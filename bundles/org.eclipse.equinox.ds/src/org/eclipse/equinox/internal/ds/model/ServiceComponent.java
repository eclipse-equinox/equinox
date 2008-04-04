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
 *    Simon Archer 		    - bug.id = 225624 
 *******************************************************************************/
package org.eclipse.equinox.internal.ds.model;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import org.eclipse.equinox.internal.ds.Activator;
import org.eclipse.equinox.internal.ds.SCRUtil;
import org.eclipse.equinox.internal.util.io.Externalizable;
import org.eclipse.equinox.internal.util.io.ExternalizableDictionary;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;

/**
 * This is an OO wrapper for the XML representing the components. It also caches
 * the (de)activate & (un)bind methods (reflection) of the component.
 * 
 * @author Valentin Valchev
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class ServiceComponent implements Externalizable {

	public Vector componentProps = null;

	// --- begin: XML def
	public String name;
	public String factory;
	String implementation; // the class name
	Properties properties; // property or properties

	// service
	public Vector serviceInterfaces; // all strings
	public String[] provides; // the same as above, but as String[]
	public boolean serviceFactory = false;

	public Vector references; // ComponentReference

	public boolean autoenable = true;
	public boolean immediate = false;
	// --- end: XML def

	// --- begin: cache
	private boolean activateCached = false;
	private boolean deactivateCached = false;
	private Method activate; // this is optional!
	private Method deactivate;
	// --- end: cache

	// --- begin: model
	public boolean enabled;
	public Bundle bundle;
	public BundleContext bc;
	// --- end: model

	private static final Class ACTIVATE_METHODS_PARAMETERS[] = new Class[] {ComponentContext.class};

	public ServiceComponent() {
	}

	private final Method getMethod(Object instance, String methodName) throws Exception {
		if (Activator.DEBUG) {
			Activator.log.debug(0, 10034, methodName, null, false);
			// //Activator.log.debug("getMethod() " + name, null);
		}
		Method method = null;
		Class clazz = instance != null ? instance.getClass() : null;

		while (method == null && clazz != null) {
			try {
				method = clazz.getDeclaredMethod(methodName, ACTIVATE_METHODS_PARAMETERS);
			} catch (NoSuchMethodException e) {
				// the method activate/deactivate may not exist in the component implementation class
			}
			// search for the method in the parent classes!
			clazz = clazz.getSuperclass();
		}
		if (method != null) {
			int modifiers = method.getModifiers();
			if (Modifier.isProtected(modifiers)) {
				SCRUtil.setAccessible(method);
			} else if (!Modifier.isPublic(modifiers)) {
				// not protected neither public
				Activator.log.warning("[SCR] Method '" + methodName + "' is not public or protected and cannot be executed! The method is located in the implementation class of component: " + this, null);
				method = null;
			}
		}

		return method;
	}

	void activate(Object instance, ComponentContext context) throws ComponentException {
		try {
			// retrieve the activate method from cache
			if (!activateCached) {
				activateCached = true;
				activate = getMethod(instance, "activate");
			}
			// invoke the method if any
			if (activate != null) {
				Object[] params = SCRUtil.getObjectArray();
				params[0] = context;
				try {
					activate.invoke(instance, params);
				} finally {
					SCRUtil.release(params);
				}
			}
		} catch (Throwable t) {
			Activator.log.error("[SCR] Cannot activate instance " + instance + " of component " + this, null);
			throw new ComponentException("[SCR] Exception while activating instance " + instance + " of component " + name, t);
			// rethrow exception so resolver is eventually notified that
			// the processed SCP is bad
		}
	}

	void deactivate(Object instance, ComponentContext context) {
		try {
			// retrieve the activate method from cache
			if (!deactivateCached) {
				deactivateCached = true;
				deactivate = getMethod(instance, "deactivate");
			}
			// invoke the method
			if (deactivate != null) {
				Object[] params = SCRUtil.getObjectArray();
				params[0] = context;
				try {
					deactivate.invoke(instance, params);
				} finally {
					SCRUtil.release(params);
				}
			}
		} catch (Throwable t) {
			Activator.log.error("[SCR] Error while attempting to deactivate instance of component " + this, t);
		}
	}

	/**
	 * this method is called from the xml parser to validate the component once
	 * it is fully loaded!
	 * 
	 * @param line
	 *            the line at which the the component definition ends
	 */
	void validate(int line) {
		// name & implementations are required
		if (name == null) {
			throw new IllegalArgumentException("The component definition misses 'name' attribute, line " + line);
		}
		if (implementation == null) {
			throw new IllegalArgumentException("The component '" + name + "' misses 'implementation' attribute, line " + line);
		}

		// component factory is incompatible with service factory
		if (factory != null && serviceFactory) {
			throw new IllegalArgumentException("The component '" + name + "' invalid specifies both ComponentFactory and ServiceFactory");
		}

		if (immediate) {
			if (serviceFactory)
				throw new IllegalArgumentException("The component '" + name + "' is invalid specified both as immediate and ServiceFactory");
			if (factory != null)
				throw new IllegalArgumentException("The component '" + name + "' is invalid specified both as immediate and ComponentFactory");
		} else {
			if ((serviceInterfaces == null) && (factory == null)) {
				throw new IllegalArgumentException("The component '" + name + "' is invalid specifying immediate to false and providing no Services");
			}
		}

		// make sure that references are also valid
		if (references != null) {
			for (int i = 0; i < references.size(); i++) {
				ComponentReference r = (ComponentReference) references.elementAt(i);
				if (r.name == null || r.interfaceName == null || r.name.equals("") || r.interfaceName.equals("")) {
					throw new IllegalArgumentException("The component '" + name + "' defined at line " + line + " contains illegal reference " + r);
				}
				for (int j = i + 1; j < references.size(); j++) {
					ComponentReference ref2 = (ComponentReference) references.elementAt(j);
					if (r.name.equals(ref2.name)) {
						throw new IllegalArgumentException("The component '" + name + "' defined at line " + line + " contains references with duplicate names");
					}
				}
			}
		}

		// cache the service interfaces as String[] too.
		if (serviceInterfaces != null && !serviceInterfaces.isEmpty()) {
			provides = new String[serviceInterfaces.size()];
			serviceInterfaces.copyInto(provides);
		}

		// make sure that the component will get automatically enabled!
		enabled = autoenable;
	}

	/**
	 * This method will instantiate the implementation class!
	 * 
	 * @return instance of the component implementation class. If the components
	 *         exports some services, the implementation must implement all of
	 *         them
	 * @throws Exception
	 *             is thrown if the implementation cannot be instantiated for
	 *             some reasons.
	 */
	final Object createInstance() throws Exception {
		try {
			return bundle.loadClass(implementation).newInstance();
		} catch (Throwable t) {
			throw new ComponentException("Exception occured while creating new instance of component " + this, t);
		}
	}

	/**
	 * This method will dispose everything
	 */
	// TODO : this method is not used - should be removed?
	public final void dispose() {

		activateCached = deactivateCached = false;
		activate = deactivate = null;

		enabled = false;
		bundle = null;
		// bc = null;

		if (references != null) {
			for (int i = 0; i < references.size(); i++) {
				ComponentReference ref = (ComponentReference) references.elementAt(i);
				ref.dispose();
			}
			references.removeAllElements();
			references = null;
		}

		if (properties != null) {
			properties.clear();
			properties = null;
		}

		if (serviceInterfaces != null) {
			serviceInterfaces.removeAllElements();
			serviceInterfaces = null;
		}
	}

	public boolean provides(String interfaceName) {
		return serviceInterfaces != null && serviceInterfaces.contains(interfaceName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public final String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Component[");
		buffer.append("\n\tname = ").append(name);
		buffer.append("\n\tautoenable = ").append(autoenable);
		buffer.append("\n\tfactory = ").append(factory);
		buffer.append("\n\timmediate = ").append(immediate);

		buffer.append("\n\timplementation = ").append(implementation);
		buffer.append("\n\tproperties = ").append(properties);

		buffer.append("\n\tserviceFactory = ").append(serviceFactory);
		buffer.append("\n\tserviceInterface = ").append(serviceInterfaces);

		if (references == null) {
			buffer.append("\n\treferences = ").append("null");
		} else {
			buffer.append("\n\treferences = {");
			for (int i = 0; i < references.size(); i++) {
				buffer.append("\n\t\t").append(references.elementAt(i));
			}
			buffer.append("\n\t}");
		}
		buffer.append("\n\tlocated in bundle = ").append(bundle);
		buffer.append("\n]");
		return buffer.toString();
	}

	public synchronized void writeObject(OutputStream s) throws Exception {

		try {

			DataOutputStream out;
			if (s instanceof DataOutputStream) {
				out = (DataOutputStream) s;
			} else {
				out = new DataOutputStream(s);
			}

			boolean flag;
			int count;

			out.writeUTF(name);
			out.writeUTF(implementation);
			out.writeBoolean(serviceFactory);
			out.writeBoolean(autoenable);
			out.writeBoolean(immediate);

			flag = factory != null;
			out.writeBoolean(flag);
			if (flag)
				out.writeUTF(factory);

			count = serviceInterfaces == null ? 0 : serviceInterfaces.size();
			out.writeInt(count);
			for (int i = 0; i < count; i++) {
				out.writeUTF(serviceInterfaces.elementAt(i).toString());
			}

			count = references == null ? 0 : references.size();
			out.writeInt(count);
			for (int i = 0; i < count; i++) {
				ComponentReference ref = (ComponentReference) references.elementAt(i);
				ref.writeObject(out);
			}

			flag = properties != null && !properties.isEmpty();
			out.writeBoolean(flag);
			if (flag) {
				ExternalizableDictionary dictionary = new ExternalizableDictionary();
				dictionary.copyFrom(properties);
				dictionary.writeObject(out);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Don't forget to set the bundle & the bc attributes!!!!!!
	 * 
	 * @param s
	 *            the input stream from which to read the object
	 * @throws Exception
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
			int count;

			name = in.readUTF();
			implementation = in.readUTF();
			serviceFactory = in.readBoolean();
			autoenable = in.readBoolean();
			immediate = in.readBoolean();

			flag = in.readBoolean();
			if (flag)
				factory = in.readUTF();

			count = in.readInt();
			if (count > 0) {
				serviceInterfaces = new Vector(count);
				provides = new String[count];
				for (int i = 0; i < count; i++) {
					String entry = in.readUTF();
					serviceInterfaces.addElement(entry);
					provides[i] = entry;
				}
			}

			count = in.readInt();
			if (count > 0) {
				references = new Vector(count);
				for (int i = 0; i < count; i++) {
					ComponentReference ref = new ComponentReference(this);
					ref.readObject(in);
				}
			}

			flag = in.readBoolean();
			if (flag) {
				ExternalizableDictionary dictionary = new ExternalizableDictionary();
				dictionary.readObject(in);
				Properties props = new Properties();
				for (Enumeration keys = dictionary.keys(); keys.hasMoreElements();) {
					String key = (String) keys.nextElement();
					props.put(key, dictionary.get(key));
				}
				properties = props;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public ServiceComponentProp getComponentPropByPID(String pid) {
		for (int i = 0; i < componentProps.size(); i++) {
			ServiceComponentProp scp = (ServiceComponentProp) componentProps.elementAt(i);
			if (scp.getProperties() != null) {
				if (pid.equals(scp.getProperties().get(Constants.SERVICE_PID))) {
					return scp;
				}
			}
		}
		return null;
	}

	public void addServiceComponentProp(ServiceComponentProp scp) {
		if (componentProps == null) {
			componentProps = new Vector(2);
		}
		if (!componentProps.contains(scp)) {
			componentProps.addElement(scp);
		}
	}

	public boolean isImmediate() {
		return immediate;
	}

	public void setImmediate(boolean immediate) {
		this.immediate = immediate;
	}
}
