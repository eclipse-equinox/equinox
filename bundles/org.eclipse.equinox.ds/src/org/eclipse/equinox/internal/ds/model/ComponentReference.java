/*******************************************************************************
 * Copyright (c) 1997-2007 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *    Jeremy Volkman 		- bug.id = 182560
 *******************************************************************************/
package org.eclipse.equinox.internal.ds.model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.equinox.internal.ds.*;
import org.eclipse.equinox.internal.ds.impl.ComponentInstanceImpl;
import org.eclipse.equinox.internal.ds.model.ServiceComponent;
import org.eclipse.equinox.internal.util.io.Externalizable;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentInstance;


/**
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 * @version 1.1
 */

public class ComponentReference implements Externalizable {

	public static final int CARDINALITY_0_1 = 0;
	public static final int CARDINALITY_0_N = 1;
	public static final int CARDINALITY_1_1 = 2;
	public static final int CARDINALITY_1_N = 3;
	public static final int POLICY_STATIC = 0;
	public static final int POLICY_DYNAMIC = 1;

	// --- begin: XML def
	public String name; // required
	public String interfaceName; // required
	public int cardinality = CARDINALITY_1_1;
	public int policy = POLICY_STATIC;
	public String target;
	public String bind;
	public String unbind;
	ServiceComponent component;
	// --- end: XML def

	// --- begin: cache
	private boolean bindCached;
	private boolean unbindCached;
	private Method bindMethod;
	private Method unbindMethod;
	// --- end: cache

	// --- begin: model

	// ServiceReferences binded to this reference
	public Vector serviceReferences = new Vector(2);

	private Hashtable serviceReferencesToUnbind = new Hashtable(3);

	static final Class[] SERVICE_REFERENCE = new Class[] {ServiceReference.class};

	// --- end: model;

	ComponentReference(ServiceComponent component) {
		this.component = component;
		if (this.component.references == null) {
			this.component.references = new Vector(2);
		}
		this.component.references.addElement(this);
	}

	Method getMethod(ComponentInstanceImpl componentInstance, Reference reference, String methodName, ServiceReference serviceReference) {

		Class consumerClass = componentInstance.getInstance().getClass();
		Object serviceObject = null;
		Class serviceObjectClass = null;
		Class interfaceClass = null;
		Class[] param_interfaceClass = null;
		Method method = null;
		while (consumerClass != null) {

			// search this class' methods
			// look for various forms of bind methods

			// 1) check for bind(ServiceReference) method
			try {
				method = consumerClass.getDeclaredMethod(methodName, SERVICE_REFERENCE);
			} catch (NoSuchMethodException e) {
			} catch (NoClassDefFoundError err) {
				// this may happen on skelmir VM
			}

			if (method != null)
				break;

			// we need a serviceObject to keep looking, create one if necessary
			if (serviceObject == null) {
				serviceObject = componentInstance.bindedServices.get(serviceReference);
				if (serviceObject == null) {
					serviceObject = InstanceProcess.staticRef.getService(reference, serviceReference);
				}
				if (serviceObject == null) {
					// we could not create a serviceObject because of
					// circularity
					return null;
				}
				componentInstance.bindedServices.put(serviceReference, serviceObject);
				serviceObjectClass = serviceObject.getClass();

				// figure out the interface class - this is guaranteed to
				// succeed or else
				// the framework would not have let us have the service object
				Class searchForInterfaceClass = serviceObjectClass;
				while (searchForInterfaceClass != null) {
					// first look through interfaces
					Class[] interfaceClasses = searchForInterfaceClass.getInterfaces();
					for (int i = 0; i < interfaceClasses.length; i++) {
						if (interfaceClasses[i].getName().equals(interfaceName)) {
							interfaceClass = interfaceClasses[i];
							break;
						}
					}
					if (interfaceClass != null) {
						break;
					}

					// also check the class itself
					if (searchForInterfaceClass.getName().equals(interfaceName)) {
						interfaceClass = searchForInterfaceClass;
						break;
					}

					// advance up the superclasses
					searchForInterfaceClass = searchForInterfaceClass.getSuperclass();
				}

				param_interfaceClass = new Class[] {interfaceClass};

			} // end if(serviceObject == null)

			// 2) check for bind(Service interface) method
			try {
				method = consumerClass.getDeclaredMethod(methodName, param_interfaceClass);
			} catch (NoSuchMethodException e) {
			} catch (NoClassDefFoundError err) {
				// this may happen on skelmir VM
			}
			if (method != null)
				break;

			// 3) check for bind(class.isAssignableFrom(serviceObjectClass))
			// method
			Method[] methods = consumerClass.getDeclaredMethods();
			for (int i = 0; i < methods.length; i++) {
				Class[] params = methods[i].getParameterTypes();
				if (params.length == 1 && methods[i].getName().equals(methodName) && params[0].isAssignableFrom(serviceObjectClass)) {

					method = methods[i];
					break;
				}
			}
			if (method != null)
				break;
			// we couldn't find the method - try the superclass
			consumerClass = consumerClass.getSuperclass();
		}

		if (method == null) {
			Activator.log.error("[SCR] Method was not found: " + methodName, null);
			return null;
		}

		// if method is not protected or public, log error message
		int modifier = method.getModifiers();
		if (!(Modifier.isProtected(modifier) || Modifier.isPublic(modifier))) {
			Activator.log.error("[SCR] Method " + methodName + " is not protected or public.", null);
			return null;
		}

		if (Modifier.isProtected(modifier)) {
			SCRUtil.setAccessible(method);
		}

		return method;
	}

	final void bind(Reference reference, ComponentInstance instance, ServiceReference serviceReference) throws Exception {
		if (bind != null) {
			// DON'T rebind the same object again
			synchronized (serviceReferences) {
				if (serviceReferences.contains(serviceReference)) {
					Activator.log.warning("ComponentReference.bind(): service reference already bound: " + serviceReference, null);
					return;
				} else {
					serviceReferences.addElement(serviceReference);
				}
			}
			// retrieve the method from cache
			if (!bindCached) {
				bindMethod = getMethod((ComponentInstanceImpl) instance, reference, bind, serviceReference);
				// bindMethod can be null in case of circularity
				if (bindMethod != null) {
					bindCached = true;
				}
			}
			// invoke the method
			if (bindMethod != null) {
				Object methodParam = null;
				if (bindMethod.getParameterTypes()[0].equals(ServiceReference.class)) {
					methodParam = serviceReference;
				} else {
					// bindedServices is filled by the getMethod function
					methodParam = ((ComponentInstanceImpl) instance).bindedServices.get(serviceReference);
					if (methodParam == null) {
						methodParam = InstanceProcess.staticRef.getService(reference, serviceReference);
					}
					if (methodParam == null) {
						// cannot get serviceObject because of circularity

						//remove the service reference marked as bind
						serviceReferences.remove(serviceReference);
						return;
					}
				}

				Object[] params = SCRUtil.getObjectArray();
				params[0] = methodParam;
				try {
					bindMethod.invoke(instance.getInstance(), params);
				} catch (Throwable t) {
					Activator.log.error("[SCR] Error while trying to bind reference " + this, t);
					// rethrow exception so resolver is eventually notified that
					// this component is bad
					// throw t;
				} finally {
					SCRUtil.release(params);
				}
			} else {
				//remove the service reference marked as bind
				serviceReferences.remove(serviceReference);

				// could be also circularity break
				Activator.log.warning("ComponentReference.bind(): bind method " + bind + " is not accessible!", null);
			}
		}
	}

	public final void unbind(Reference reference, ComponentInstance instance, ServiceReference serviceReference) {
		// don't unbind an object that wasn't bound
		int index;
		synchronized (serviceReferences) {
			index = serviceReferences.indexOf(serviceReference);
			if (index >= 0) {
				if (serviceReferencesToUnbind.containsKey(serviceReference)) {
					//the service reference is already in process of unbinding
					return;
				} else {
					serviceReferencesToUnbind.put(serviceReference, "");
				}
			}
		}
		if (index == -1) {
			Activator.log.warning("ComponentReference.unbind(): invalid service reference " + serviceReference, null);
			return;
		}
		try {
			if (unbind != null) {
				// retrieve the unbind method from cache
				if (!unbindCached) {
					unbindCached = true;
					unbindMethod = getMethod((ComponentInstanceImpl) instance, reference, unbind, serviceReference);
				}
				// invoke the method
				if (unbindMethod != null) {
					Object methodParam = null;
					if (unbindMethod.getParameterTypes()[0].equals(ServiceReference.class)) {
						methodParam = serviceReference;
					} else {
						// bindedServices is filled by the getMethod function
						methodParam = ((ComponentInstanceImpl) instance).bindedServices.get(serviceReference);
						if (methodParam == null) {
							methodParam = InstanceProcess.staticRef.getService(reference, serviceReference);
						}
						if (methodParam == null) {
							// probably cannot get serviceObject because of
							// circularity
							return;
						}
					}

					Object[] params = SCRUtil.getObjectArray();
					params[0] = methodParam;
					try {
						unbindMethod.invoke(instance.getInstance(), params);
					} catch (Throwable t) {
						Activator.log.error("Exception occured while unbining reference " + this, t);
						// Activator.fwAccess.postFrameworkEvent(FrameworkEvent.ERROR,
						// component.bundle, t);
					} finally {
						SCRUtil.release(params);
					}
				}
			}
		} finally {
			synchronized (serviceReferences) {
				serviceReferences.removeElementAt(index);
				serviceReferencesToUnbind.remove(serviceReference);
			}
		}
		if (((ComponentInstanceImpl) instance).bindedServices.remove(serviceReference) != null) {
			component.bc.ungetService(serviceReference);
		}
	}

	public final void dispose() {
		bindCached = unbindCached = false;
		bindMethod = unbindMethod = null;
		serviceReferences = null;
	}

	public final String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Reference[");
		buffer.append("name = ").append(name);
		buffer.append(", interface = ").append(interfaceName);
		buffer.append(", policy = ");
		switch (policy) {
			case POLICY_DYNAMIC :
				buffer.append("dynamic");
				break;
			case POLICY_STATIC :
				buffer.append("static");
		}

		buffer.append(", cardinality = ");
		switch (cardinality) {
			case CARDINALITY_0_1 :
				buffer.append("0..1");
				break;
			case CARDINALITY_0_N :
				buffer.append("0..n");
				break;
			case CARDINALITY_1_1 :
				buffer.append("1..1");
				break;
			case CARDINALITY_1_N :
				buffer.append("1..n");
		}
		buffer.append(", target = ").append(target);
		buffer.append(", bind = ").append(bind);
		buffer.append(", unbind = ").append(unbind);
		buffer.append("]");
		return buffer.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.util.io.Externalizable#writeObject(java.io.OutputStream)
	 */
	public synchronized void writeObject(OutputStream o) throws Exception {
		try {

			DataOutputStream out;
			if (o instanceof DataOutputStream) {
				out = (DataOutputStream) o;
			} else {
				out = new DataOutputStream(o);
			}
			boolean flag;
			out.writeUTF(name);
			out.writeUTF(interfaceName);
			out.writeInt(cardinality);
			out.writeInt(policy);

			flag = target != null;
			out.writeBoolean(flag);
			if (flag)
				out.writeUTF(target);

			flag = bind != null;
			out.writeBoolean(flag);
			if (flag)
				out.writeUTF(bind);

			flag = unbind != null;
			out.writeBoolean(flag);
			if (flag)
				out.writeUTF(unbind);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.internal.util.io.Externalizable#readObject(java.io.InputStream)
	 */
	public synchronized void readObject(InputStream s) throws Exception {
		try {

			DataInputStream in;
			if (s instanceof DataInputStream) {
				in = (DataInputStream) s;
			} else {
				in = new DataInputStream(s);
			}
			boolean flag;
			name = in.readUTF();
			interfaceName = in.readUTF();
			cardinality = in.readInt();
			policy = in.readInt();
			flag = in.readBoolean();
			if (flag)
				target = in.readUTF();

			flag = in.readBoolean();
			if (flag)
				bind = in.readUTF();

			flag = in.readBoolean();
			if (flag)
				unbind = in.readUTF();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
