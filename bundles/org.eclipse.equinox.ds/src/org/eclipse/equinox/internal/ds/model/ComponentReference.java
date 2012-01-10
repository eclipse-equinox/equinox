/*******************************************************************************
 * Copyright (c) 1997-2010 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *    Jeremy Volkman 		- bug.id = 182560
 *    Simon Archer 		    - bug.id = 223454
 *    Lazar Kirchev		 	- bug.id = 320377
 *******************************************************************************/
package org.eclipse.equinox.internal.ds.model;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.eclipse.equinox.internal.ds.*;
import org.eclipse.equinox.internal.ds.impl.ComponentInstanceImpl;
import org.eclipse.equinox.internal.util.io.Externalizable;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.log.LogService;

/**
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 */
public class ComponentReference implements Externalizable, org.apache.felix.scr.Reference {

	public static final int CARDINALITY_0_1 = 0;
	public static final int CARDINALITY_0_N = 1;
	public static final int CARDINALITY_1_1 = 2;
	public static final int CARDINALITY_1_N = 3;
	public static final int POLICY_STATIC = 0;
	public static final int POLICY_DYNAMIC = 1;

	public static final int POLICY_OPTION_RELUCTANT = 0; // default
	public static final int POLICY_OPTION_GREEDY = 1;

	// --- begin: XML def
	public String name;
	public String interfaceName; // required
	public int cardinality = CARDINALITY_1_1;
	public int policy = POLICY_STATIC;
	public String target;
	public String bind;
	public String unbind;
	//defined by DS specification v1.2 - schema v1.2.0
	public String updated;
	public int policy_option = POLICY_OPTION_RELUCTANT;
	// --- end: XML def
	public ServiceComponent component;

	// --- begin: cache
	private boolean bindCached;
	private boolean unbindCached;
	private boolean updatedCached;
	Method bindMethod;
	Method unbindMethod;
	Method updatedMethod;
	// --- end: cache

	// --- begin: model

	// Contains mapping of ServiceReference to ComponentInstance or Vector of ComponentInstances.
	// The service reference is bound to the ComponentInstance or to each of the ComponentInstances in the Vector 
	public Hashtable serviceReferences = new Hashtable(3);

	// avoids recursive calling of unbind method for one and the same instance 
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
				//
			} catch (NoClassDefFoundError err) {
				// this may happen on skelmir VM or in case of class loading problems
				logWarning(NLS.bind(Messages.EXCEPTION_GETTING_METHOD, methodName, consumerClass.getName()), err, reference);
			}

			if (method != null && SCRUtil.checkMethodAccess(componentInstance.getInstance().getClass(), consumerClass, method, component.isNamespaceAtLeast11()))
				break;

			// we need a serviceObject to keep looking, create one if necessary
			if (serviceObject == null) {
				serviceObject = componentInstance.bindedServices.get(serviceReference);
				if (serviceObject == null) {
					serviceObject = InstanceProcess.staticRef.getService(reference, serviceReference);
				}
				if (serviceObject == null) {
					// we could not create a serviceObject because of circularity or the BundleContext.getService(ServiceReference) returned null
					logWarning(Messages.CANT_GET_SERVICE_OBJECT, null, reference);
					return null;
				}
				componentInstance.bindedServices.put(serviceReference, serviceObject);
				serviceObjectClass = serviceObject.getClass();

				// figure out the interface class - this is guaranteed to succeed or else
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
				//
			} catch (NoClassDefFoundError err) {
				// this may happen on skelmir VM or in case of class loading problems
				logWarning(NLS.bind(Messages.EXCEPTION_GETTING_METHOD, methodName, consumerClass.getName()), err, reference);
			}
			if (method != null && SCRUtil.checkMethodAccess(componentInstance.getInstance().getClass(), consumerClass, method, component.isNamespaceAtLeast11()))
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
			if (method != null && SCRUtil.checkMethodAccess(componentInstance.getInstance().getClass(), consumerClass, method, component.isNamespaceAtLeast11()))
				break;

			//implement search for bind/unbind methods according to schema v1.1.0
			if (component.isNamespaceAtLeast11()) {
				for (int i = 0; i < methods.length; i++) {
					Class[] params = methods[i].getParameterTypes();
					if (params.length == 2 && methods[i].getName().equals(methodName) && params[0] == interfaceClass && params[1] == Map.class) {
						method = methods[i];
						break;
					}
				}
				if (method != null && SCRUtil.checkMethodAccess(componentInstance.getInstance().getClass(), consumerClass, method, true))
					break;

				for (int i = 0; i < methods.length; i++) {
					Class[] params = methods[i].getParameterTypes();
					if (params.length == 2 && methods[i].getName().equals(methodName) && params[0].isAssignableFrom(serviceObjectClass) && params[1] == Map.class) {
						method = methods[i];
						break;
					}
				}
			}
			if (method != null)
				break;

			// we couldn't find the method - try the superclass
			consumerClass = consumerClass.getSuperclass();
		}

		if (method == null) {
			logMethodNotFoundError(componentInstance, reference, methodName);
			return null;
		}

		if (!SCRUtil.checkMethodAccess(componentInstance.getInstance().getClass(), consumerClass, method, component.isNamespaceAtLeast11())) {
			// if method is not visible, log error message
			logMethodNotVisible(componentInstance, reference, methodName, method.getParameterTypes());
			return null;
		}

		int modifier = method.getModifiers();
		if (!Modifier.isPublic(modifier)) {
			SCRUtil.setAccessible(method);
		}

		return method;
	}

	private void logMethodNotVisible(ComponentInstanceImpl componentInstance, Reference reference, String methodName, Class[] param_interfaceClasses) {
		StringBuffer buffer = createBuffer();
		buffer.append("[SCR] Method "); //$NON-NLS-1$
		buffer.append(methodName);
		buffer.append('(');
		for (int i = 0; i < param_interfaceClasses.length; i++) {
			buffer.append(param_interfaceClasses[i].getName());
			if (i < param_interfaceClasses.length - 1) {
				buffer.append(',');
			}
		}
		buffer.append(')');
		buffer.append(" is not accessible!"); //$NON-NLS-1$
		component.componentIssues.add("Method [" + methodName + "]  is not accessible in " + componentInstance.getInstance().getClass());
		appendDetails(buffer, reference);
		String message = buffer.toString();
		Activator.log(reference.reference.component.bc, LogService.LOG_ERROR, message, null);
	}

	private void logMethodNotFoundError(ComponentInstanceImpl componentInstance, Reference reference, String methodName) {
		StringBuffer buffer = createBuffer();
		buffer.append("[SCR] Method was not found: "); //$NON-NLS-1$
		buffer.append(methodName);
		buffer.append('(');
		buffer.append("..."); //$NON-NLS-1$
		buffer.append(')');
		component.componentIssues.add("Method [" + methodName + "] was not found in " + componentInstance.getInstance().getClass());
		appendDetails(buffer, reference);
		String message = buffer.toString();
		Activator.log(reference.reference.component.bc, LogService.LOG_ERROR, message, null);
	}

	private void appendDetails(StringBuffer buffer, Reference reference) {
		try {
			String indent = "\n\t"; //$NON-NLS-1$
			buffer.append(indent);
			buffer.append("Details:"); //$NON-NLS-1$
			buffer.append(indent);
			buffer.append("Problematic reference = " + reference.reference); //$NON-NLS-1$
			buffer.append(indent);
			buffer.append("of service component = "); //$NON-NLS-1$
			buffer.append(reference.reference.component.name);
			buffer.append(indent);
			buffer.append("component implementation class = "); //$NON-NLS-1$
			buffer.append(reference.reference.component.implementation);
			buffer.append(indent);
			buffer.append("located in bundle with symbolic name = "); //$NON-NLS-1$
			buffer.append(component.bc.getBundle().getSymbolicName());
			buffer.append(indent);
			buffer.append("bundle location = "); //$NON-NLS-1$
			buffer.append(component.bc.getBundle().getLocation());
		} catch (Throwable t) {
			//prevent possible exceptions in case the component's bundle becomes uninstalled 
		}
	}

	private void logWarning(String message, Throwable t, Reference reference) {
		StringBuffer buffer = createBuffer();
		buffer.append(message);
		appendDetails(buffer, reference);
		Activator.log(reference.reference.component.bc, LogService.LOG_WARNING, buffer.toString(), t);
	}

	private void logError(String message, Throwable t, Reference reference) {
		StringBuffer buffer = createBuffer();
		buffer.append(message);
		String cause = t.toString();
		if (t instanceof InvocationTargetException) {
			cause = "The called method throws: " + t.getCause().toString();
		}
		component.componentIssues.add(message + " Root Cause [" + cause + "]");
		appendDetails(buffer, reference);
		Activator.log(reference.reference.component.bc, LogService.LOG_ERROR, buffer.toString(), t);
	}

	private StringBuffer createBuffer() {
		return new StringBuffer(400);
	}

	final boolean bind(Reference reference, ComponentInstance instance, ServiceReference serviceReference) throws Exception {
		if (bind != null) {
			boolean bound = false;
			// DON'T rebind the same object again
			synchronized (serviceReferences) {
				Vector instances = (Vector) serviceReferences.get(serviceReference);
				if (instances == null) {
					instances = new Vector(1);
					instances.addElement(instance);
					serviceReferences.put(serviceReference, instances);
				} else if (instances.contains(instance)) {
					if (reference.isUnary()) {
						logWarning(NLS.bind(Messages.SERVICE_REFERENCE_ALREADY_BOUND, serviceReference, instance), null, reference);
					}
					return false;
				} else {
					instances.addElement(instance);
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
				Class[] paramTypes = bindMethod.getParameterTypes();
				if (paramTypes.length == 1 && paramTypes[0].equals(ServiceReference.class)) {
					methodParam = serviceReference;
				} else {
					// bindedServices is filled by the getMethod function
					methodParam = ((ComponentInstanceImpl) instance).bindedServices.get(serviceReference);
					if (methodParam == null) {
						methodParam = InstanceProcess.staticRef.getService(reference, serviceReference);
						if (methodParam != null) {
							((ComponentInstanceImpl) instance).bindedServices.put(serviceReference, methodParam);
						}
					}
					if (methodParam == null) {
						// cannot get serviceObject because of circularity

						//remove the component instance marked as bound
						removeServiceReference(serviceReference, instance);
						return false;
					}
				}

				Object[] params = null;
				if (paramTypes.length == 1) {
					params = SCRUtil.getObjectArray();
					params[0] = methodParam;
				} else {
					//this is the case where we have 2 parameters: a service object and a Map, holding the service properties
					HashMap map = new HashMap();
					String[] keys = serviceReference.getPropertyKeys();
					for (int i = 0; i < keys.length; i++) {
						map.put(keys[i], serviceReference.getProperty(keys[i]));
					}
					params = new Object[] {methodParam, map};
				}

				try {
					bindMethod.invoke(instance.getInstance(), params);
					bound = true;
				} catch (Throwable t) {
					logError(NLS.bind(Messages.ERROR_BINDING_REFERENCE, this), t, reference);
					//remove the component instance marked as bound
					removeServiceReference(serviceReference, instance);
				} finally {
					if (params.length == 1) {
						SCRUtil.release(params);
					}
				}
			} else {
				//remove the component instance marked as bound
				removeServiceReference(serviceReference, instance);
				// could be also circularity break
				logWarning(NLS.bind(Messages.BIND_METHOD_NOT_FOUND_OR_NOT_ACCESSIBLE, bind), null, reference);
			}
			return bound;
		}
		return false;
	}

	private void removeServiceReference(ServiceReference serviceReference, ComponentInstance instance) {
		Vector instances = (Vector) serviceReferences.get(serviceReference);
		instances.removeElement(instance);
		if (instances.isEmpty()) {
			serviceReferences.remove(serviceReference);
		}
	}

	public final void unbind(Reference reference, ComponentInstance instance, ServiceReference serviceReference) {
		// don't unbind an object that wasn't bound
		boolean referenceExists = true;
		synchronized (serviceReferences) {
			Vector instances = (Vector) serviceReferences.get(serviceReference);
			if (instances == null) {
				referenceExists = false;
			} else {
				if (!instances.contains(instance)) {
					logWarning(NLS.bind(Messages.INSTANCE_NOT_BOUND, instance), null, reference);
					return;
				}
			}
			if (referenceExists) {
				Vector instancesToUnbind = (Vector) serviceReferencesToUnbind.get(serviceReference);
				if (instancesToUnbind != null && instancesToUnbind.contains(instance)) {
					//the service reference is already in process of unbinding
					return;
				}
				if (instancesToUnbind == null) {
					instancesToUnbind = new Vector(1);
					serviceReferencesToUnbind.put(serviceReference, instancesToUnbind);
				}
				instancesToUnbind.addElement(instance);
			}
		}
		if (!referenceExists) {
			logWarning(NLS.bind(Messages.INVALID_SERVICE_REFERENCE, serviceReference), null, reference);
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
					Class[] paramTypes = unbindMethod.getParameterTypes();
					if (paramTypes.length == 1 && paramTypes[0].equals(ServiceReference.class)) {
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

					Object[] params = null;
					if (paramTypes.length == 1) {
						params = SCRUtil.getObjectArray();
						params[0] = methodParam;
					} else {
						//this is the case where we have 2 parameters: a service object and a Map, holding the service properties
						HashMap map = new HashMap();
						String[] keys = serviceReference.getPropertyKeys();
						for (int i = 0; i < keys.length; i++) {
							map.put(keys[i], serviceReference.getProperty(keys[i]));
						}
						params = new Object[] {methodParam, map};
					}
					try {
						unbindMethod.invoke(instance.getInstance(), params);
					} catch (Throwable t) {
						logError(NLS.bind(Messages.EXCEPTION_UNBINDING_REFERENCE, this), t, reference);
					} finally {
						if (params.length == 1) {
							SCRUtil.release(params);
						}
					}
				}
			}
		} finally {
			synchronized (serviceReferences) {
				Vector instances = (Vector) serviceReferences.get(serviceReference);
				instances.removeElement(instance);
				if (instances.isEmpty()) {
					serviceReferences.remove(serviceReference);
				}

				instances = (Vector) serviceReferencesToUnbind.get(serviceReference);
				instances.removeElement(instance);
				if (instances.isEmpty()) {
					serviceReferencesToUnbind.remove(serviceReference);
				}
			}
		}
		if (((ComponentInstanceImpl) instance).bindedServices.remove(serviceReference) != null) {
			component.bc.ungetService(serviceReference);
		}
	}

	final void updated(Reference reference, ComponentInstance instance, ServiceReference serviceReference) {
		if (updated != null) {
			synchronized (serviceReferences) {
				Vector instances = (Vector) serviceReferences.get(serviceReference);
				if (instances == null || !instances.contains(instance)) {
					//this instance is not bound to the passed service reference
					return;
				}
			}
			// retrieve the method from cache
			if (!updatedCached) {
				updatedMethod = getMethod((ComponentInstanceImpl) instance, reference, updated, serviceReference);
				// updatedMethod can be null in case of circularity
				if (updatedMethod != null) {
					updatedCached = true;
				}
			}
			// invoke the method
			if (updatedMethod != null) {
				Object methodParam = null;
				Class[] paramTypes = updatedMethod.getParameterTypes();
				if (paramTypes.length == 1 && paramTypes[0].equals(ServiceReference.class)) {
					methodParam = serviceReference;
				} else {
					// bindedServices is filled by the getMethod function
					methodParam = ((ComponentInstanceImpl) instance).bindedServices.get(serviceReference);
					if (methodParam == null) {
						methodParam = InstanceProcess.staticRef.getService(reference, serviceReference);
						if (methodParam != null) {
							((ComponentInstanceImpl) instance).bindedServices.put(serviceReference, methodParam);
						}
					}
					if (methodParam == null) {
						// cannot get serviceObject because of circularity
						Activator.log(null, LogService.LOG_WARNING, NLS.bind(Messages.UPDATED_METHOD_NOT_CALLED, name, component.name), null);
						return;
					}
				}

				Object[] params = null;
				if (paramTypes.length == 1) {
					params = SCRUtil.getObjectArray();
					params[0] = methodParam;
				} else {
					//this is the case where we have 2 parameters: a service object and a Map, holding the service properties
					HashMap map = new HashMap();
					String[] keys = serviceReference.getPropertyKeys();
					for (int i = 0; i < keys.length; i++) {
						map.put(keys[i], serviceReference.getProperty(keys[i]));
					}
					params = new Object[] {methodParam, map};
				}

				try {
					updatedMethod.invoke(instance.getInstance(), params);
				} catch (Throwable t) {
					logError(NLS.bind(Messages.ERROR_UPDATING_REFERENCE, this, instance.getInstance()), t, reference);
				} finally {
					if (params.length == 1) {
						SCRUtil.release(params);
					}
				}
			} else {
				// could be also circularity break
				logWarning(NLS.bind(Messages.UPDATED_METHOD_NOT_FOUND_OR_NOT_ACCESSIBLE, updated), null, reference);
			}
		}
	}

	public final void dispose() {
		bindCached = unbindCached = updatedCached = false;
		bindMethod = unbindMethod = updatedMethod = null;
		serviceReferences = null;
	}

	public final String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Reference["); //$NON-NLS-1$
		buffer.append("name = ").append(name); //$NON-NLS-1$
		buffer.append(", interface = ").append(interfaceName); //$NON-NLS-1$
		buffer.append(", policy = "); //$NON-NLS-1$
		switch (policy) {
			case POLICY_DYNAMIC :
				buffer.append("dynamic"); //$NON-NLS-1$
				break;
			case POLICY_STATIC :
				buffer.append("static"); //$NON-NLS-1$
		}
		if (component.isNamespaceAtLeast12()) {
			buffer.append(", policy-option = "); //$NON-NLS-1$
			switch (policy_option) {
				case POLICY_OPTION_RELUCTANT :
					buffer.append("reluctant"); //$NON-NLS-1$
					break;
				case POLICY_OPTION_GREEDY :
					buffer.append("greedy"); //$NON-NLS-1$
			}
		}

		buffer.append(", cardinality = "); //$NON-NLS-1$
		switch (cardinality) {
			case CARDINALITY_0_1 :
				buffer.append("0..1"); //$NON-NLS-1$
				break;
			case CARDINALITY_0_N :
				buffer.append("0..n"); //$NON-NLS-1$
				break;
			case CARDINALITY_1_1 :
				buffer.append("1..1"); //$NON-NLS-1$
				break;
			case CARDINALITY_1_N :
				buffer.append("1..n"); //$NON-NLS-1$
		}
		buffer.append(", target = ").append(target); //$NON-NLS-1$
		buffer.append(", bind = ").append(bind); //$NON-NLS-1$
		buffer.append(", unbind = ").append(unbind); //$NON-NLS-1$
		if (component.isNamespaceAtLeast12()) {
			buffer.append(", updated = ").append(updated); //$NON-NLS-1$
		}
		buffer.append("]"); //$NON-NLS-1$
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

			// DS 1.2 support
			out.writeBoolean(component.isNamespaceAtLeast12());
			if (component.isNamespaceAtLeast12()) {
				flag = updated != null;
				out.writeBoolean(flag);
				if (flag)
					out.writeUTF(updated);

				out.writeInt(policy_option);
			}
		} catch (Exception e) {
			Activator.log(null, LogService.LOG_ERROR, Messages.ERROR_WRITING_OBJECT, e);
			throw e;
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

			// DS 1.2 support
			flag = in.readBoolean();
			if (flag) {
				//This is a DS 1.2 component
				flag = in.readBoolean();
				if (flag)
					updated = in.readUTF();
				policy_option = in.readInt();
			}
		} catch (Exception e) {
			Activator.log(null, LogService.LOG_ERROR, Messages.ERROR_READING_OBJECT, e);
			throw e;
		}
	}

	public String getBindMethodName() {
		return bind;
	}

	public String getName() {
		return name;
	}

	public String getServiceName() {
		return interfaceName;
	}

	public ServiceReference[] getServiceReferences() {
		Vector result = null;
		if (bind != null) {
			if (!serviceReferences.isEmpty()) {
				result = new Vector(2);
				Enumeration keys = serviceReferences.keys();
				while (keys.hasMoreElements()) {
					result.add(keys.nextElement());
				}
			}
		}
		if (result != null && !result.isEmpty()) {
			ServiceReference[] finalResult = new ServiceReference[result.size()];
			result.copyInto(finalResult);
			return finalResult;
		}
		return null;
	}

	public String getTarget() {
		return target;
	}

	public String getUnbindMethodName() {
		return unbind;
	}

	public boolean isMultiple() {
		return cardinality == ComponentReference.CARDINALITY_0_N || cardinality == ComponentReference.CARDINALITY_1_N;
	}

	public boolean isOptional() {
		return cardinality == ComponentReference.CARDINALITY_0_1 || cardinality == ComponentReference.CARDINALITY_0_N;
	}

	public boolean isSatisfied() {
		if (isOptional()) {
			return true;
		}
		try {
			ServiceReference[] _serviceReferences = component.bc.getServiceReferences(interfaceName, target);
			if (_serviceReferences != null) {
				return true;
			}
		} catch (InvalidSyntaxException e) {
			// do nothing
		}
		return false;
	}

	public boolean isStatic() {
		return policy == ComponentReference.POLICY_STATIC;
	}

	/* (non-Javadoc)
	 * @see org.apache.felix.scr.Reference#getUpdatedMethodName()
	 */
	public String getUpdatedMethodName() {
		if (component.isNamespaceAtLeast12()) {
			return updated;
		}
		return null;
	}

}
