/*******************************************************************************
 * Copyright (c) 1997-2011 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *    Simon Archer 		    - bug.id = 225624 
 *    Bryan Hunt 		    - bug.id = 275997 
 *    Lazar Kirchev		 	- bug.id = 320377
 *******************************************************************************/
package org.eclipse.equinox.internal.ds.model;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.eclipse.equinox.internal.ds.*;
import org.eclipse.equinox.internal.ds.impl.ReadOnlyDictionary;
import org.eclipse.equinox.internal.util.io.Externalizable;
import org.eclipse.equinox.internal.util.io.ExternalizableDictionary;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.component.*;
import org.osgi.service.log.LogService;

/**
 * This is an OO wrapper for the XML representing the components. It also caches
 * the activate and deactivate methods of the component.
 * 
 * @author Valentin Valchev
 * @author Pavlin Dobrev
 * @author Stoyan Boshev
 */
public class ServiceComponent implements Externalizable, Component {

	//constants defining possible SCR namespaces XML schemas
	public static final int NAMESPACE_1_0 = 0;
	public static final int NAMESPACE_1_1 = 1;
	public static final int NAMESPACE_1_2 = 2;

	public static final String CONF_POLICY_OPTIONAL = "optional"; //$NON-NLS-1$
	public static final String CONF_POLICY_REQUIRE = "require"; //$NON-NLS-1$
	public static final String CONF_POLICY_IGNORE = "ignore"; //$NON-NLS-1$

	public Vector componentProps = null;

	// --- begin: XML def
	public String name;
	public String factory;
	String implementation; // the class name
	Properties properties; // property or properties
	String configurationPolicy = CONF_POLICY_OPTIONAL;
	String activateMethodName = "activate"; //$NON-NLS-1$
	String deactivateMethodName = "deactivate"; //$NON-NLS-1$
	public String modifyMethodName = ""; //$NON-NLS-1$

	//Since DS 1.2
	public String configurationPID;

	// service
	public Vector serviceInterfaces; // all strings
	public String[] provides; // the same as above, but as String[]
	public boolean serviceFactory = false;

	public Vector references; // ComponentReference

	public boolean autoenable = true;
	public boolean immediate = false;
	public int namespace = NAMESPACE_1_0;
	// --- end: XML def

	// --- begin: cache
	private boolean activateCached = false;
	private boolean deactivateCached = false;
	private boolean modifyCached = false;
	private Method activateMethod;
	private Method deactivateMethod;
	private Method modifyMethod;
	// --- end: cache

	// --- begin: model
	public boolean enabled;
	public Bundle bundle;
	public BundleContext bc;
	boolean activateMethodDeclared = false;
	boolean deactivateMethodDeclared = false;
	int state = Component.STATE_UNSATISFIED;
	// --- end: model

	public HashSet componentIssues = new HashSet(1, 1);
	private ReadOnlyDictionary readOnlyProps;

	public String getComponentIssues() {
		if (!componentIssues.isEmpty()) {
			String result = ""; //$NON-NLS-1$
			Object[] issues = componentIssues.toArray();
			for (int i = 0; i < issues.length; i++) {
				result += issues[i] + "\n"; //$NON-NLS-1$
			}
			return result;
		}
		return null;
	}

	private static final Class ACTIVATE_METHODS_PARAMETERS[] = new Class[] {ComponentContext.class};

	public ServiceComponent() {
		//
	}

	private final Method getMethod(Object instance, String methodName, boolean isActivate) throws Exception {
		if (Activator.DEBUG) {
			Activator.log.debug("ServiceComponent.getMethod(): " + methodName, null); //$NON-NLS-1$
		}
		Method method = null;
		int methodPriority = Integer.MAX_VALUE;
		Class clazz = instance != null ? instance.getClass() : null;

		while (method == null && clazz != null) {
			if (isNamespaceAtLeast11()) {
				Method[] methods = clazz.getDeclaredMethods();
				for (int i = 0; i < methods.length; i++) {
					if (methods[i].getName().equals(methodName)) {
						Class[] params = methods[i].getParameterTypes();
						boolean accepted = true;
						for (int j = 0; j < params.length; j++) {
							if (params[j] == ComponentContext.class || params[j] == BundleContext.class || params[j] == Map.class) {
								// correct parameter for both activate and deactivate methods
							} else if (!isActivate && (params[j] == Integer.class || params[j] == int.class)) {
								//we are checking int/Integer as special deactivate parameters

							} else {
								//the parameter is not recognized
								accepted = false;
								break;
							}
						}
						if (accepted && SCRUtil.checkMethodAccess(instance.getClass(), clazz, methods[i], true)) {
							//check if the newly accepted method has higher priority than the previous one and use it
							int prio = getMethodPriority(params);
							if (prio < methodPriority) {
								//found a method with a higher priority
								method = methods[i];
								methodPriority = prio;
							}
						}
					}
				}
			} else {
				try {
					method = clazz.getDeclaredMethod(methodName, ACTIVATE_METHODS_PARAMETERS);
					if (method != null) {
						if (!SCRUtil.checkMethodAccess(instance.getClass(), clazz, method, false)) {
							//the method is not accessible. Stop the search
							Activator.log(bc, LogService.LOG_WARNING, NLS.bind(Messages.METHOD_UNACCESSABLE, methodName, clazz), null);
							componentIssues.add(NLS.bind(Messages.METHOD_UNACCESSABLE, methodName, clazz));
							method = null;
							break;
						}
					}
				} catch (NoSuchMethodException e) {
					// the method activate/deactivate may not exist in the component implementation class
				}
			}

			if (method != null)
				break;

			// search for the method in the parent classes!
			clazz = clazz.getSuperclass();
		}
		if (method != null) {
			int modifiers = method.getModifiers();
			if (!Modifier.isPublic(modifiers)) {
				SCRUtil.setAccessible(method);
			}
		}
		return method;
	}

	private int getMethodPriority(Class[] params) {
		int priority = Integer.MAX_VALUE;
		if (params.length == 1) {
			if (params[0] == ComponentContext.class) {
				priority = 0; //highest priority
			} else if (params[0] == BundleContext.class) {
				priority = 1;
			} else if (params[0] == Map.class) {
				priority = 2;
			} else if (params[0] == int.class) {
				priority = 3;
			} else if (params[0] == Integer.class) {
				priority = 4;
			}
		} else if (params.length >= 2) {
			priority = 5;
		} else if (params.length == 0) {
			priority = 6;
		}
		return priority;
	}

	void activate(Object instance, ComponentContext context) throws ComponentException {
		try {
			if (isNamespaceAtLeast11()) {
				if (!activateCached) {
					activateCached = true;
					activateMethod = getMethod(instance, activateMethodName, true);
				}
				// invoke the method if any
				if (activateMethod != null) {
					Class[] paramTypes = activateMethod.getParameterTypes();
					Object[] params = null;
					if (paramTypes.length == 1) {
						params = SCRUtil.getObjectArray();
					} else {
						params = new Object[paramTypes.length];
					}
					for (int i = 0; i < params.length; i++) {
						if (paramTypes[i] == ComponentContext.class) {
							params[i] = context;
						} else if (paramTypes[i] == BundleContext.class) {
							params[i] = context.getBundleContext();
						} else if (paramTypes[i] == Map.class) {
							params[i] = context.getProperties();
						}
					}

					try {
						activateMethod.invoke(instance, params);
					} finally {
						if (params.length == 1) {
							SCRUtil.release(params);
						}
					}
				} else {
					if (activateMethodName != "activate") { //$NON-NLS-1$
						//the activate method is specified in the component description XML by the user.
						//It is expected to find it in the implementation class
						componentIssues.add("Can not activate instance of component " + this.implementation + ". The specified activate method [" + activateMethodName + "] was not found."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						throw new ComponentException(NLS.bind(Messages.SPECIFIED_ACTIVATE_METHOD_NOT_FOUND, instance, this));
					}
				}
			} else {
				// retrieve the activate method from cache
				if (!activateCached) {
					activateCached = true;
					activateMethod = getMethod(instance, "activate", true); //$NON-NLS-1$
				}
				// invoke the method if any
				if (activateMethod != null) {
					Object[] params = SCRUtil.getObjectArray();
					params[0] = context;
					try {
						activateMethod.invoke(instance, params);
					} finally {
						SCRUtil.release(params);
					}
				}
			}
		} catch (Throwable t) {
			if (t instanceof ComponentException) {
				throw (ComponentException) t;
			}
			String cause = t.toString();
			if (t instanceof InvocationTargetException) {
				cause = t.getCause().toString();
			}
			componentIssues.add("Can not activate instance of component " + this.implementation + ". The activation throws: " + cause); //$NON-NLS-1$ //$NON-NLS-2$ 
			throw new ComponentException(NLS.bind(Messages.EXCEPTION_ACTIVATING_INSTANCE, instance, name), t);
			// rethrow exception so resolver is eventually notified that
			// the processed SCP is bad
		}
	}

	void modified(Object instance, ComponentContext context) throws ComponentException {
		try {
			if (isNamespaceAtLeast11()) {
				if (!modifyCached) {
					modifyCached = true;
					if (modifyMethodName != "") { //$NON-NLS-1$
						modifyMethod = getMethod(instance, modifyMethodName, true);
					}
				}
				// invoke the method if any
				if (modifyMethod != null) {
					Class[] paramTypes = modifyMethod.getParameterTypes();
					Object[] params = null;
					if (paramTypes.length == 1) {
						params = SCRUtil.getObjectArray();
					} else {
						params = new Object[paramTypes.length];
					}
					for (int i = 0; i < params.length; i++) {
						if (paramTypes[i] == ComponentContext.class) {
							params[i] = context;
						} else if (paramTypes[i] == BundleContext.class) {
							params[i] = context.getBundleContext();
						} else if (paramTypes[i] == Map.class) {
							params[i] = context.getProperties();
						}
					}

					try {
						modifyMethod.invoke(instance, params);
					} finally {
						if (params.length == 1) {
							SCRUtil.release(params);
						}
					}
				} else {
					if (modifyMethodName != "") { //$NON-NLS-1$
						//the modify method is specified in the component description XML by the user.
						//It is expected to find it in the implementation class
						throw new ComponentException(NLS.bind(Messages.CANNOT_MODIFY_INSTANCE__MODIFY_METHOD_NOT_FOUND, instance, this));
					}
				}
			}
		} catch (Throwable t) {
			if (t instanceof ComponentException) {
				throw (ComponentException) t;
			}
			Activator.log(bc, LogService.LOG_ERROR, NLS.bind(Messages.EXCEPTION_MODIFYING_COMPONENT, instance, this), t);
		}
	}

	void deactivate(Object instance, ComponentContext context, int deactivateReason) {
		try {
			if (isNamespaceAtLeast11()) {
				if (!deactivateCached) {
					deactivateCached = true;
					deactivateMethod = getMethod(instance, deactivateMethodName, false);
				}
				// invoke the method if any
				if (deactivateMethod != null) {
					Class[] paramTypes = deactivateMethod.getParameterTypes();
					Object[] params = null;
					if (paramTypes.length == 1) {
						params = SCRUtil.getObjectArray();
					} else {
						params = new Object[paramTypes.length];
					}
					for (int i = 0; i < params.length; i++) {
						if (paramTypes[i] == ComponentContext.class) {
							params[i] = context;
						} else if (paramTypes[i] == BundleContext.class) {
							params[i] = context.getBundleContext();
						} else if (paramTypes[i] == Map.class) {
							params[i] = context.getProperties();
						} else if (paramTypes[i] == int.class) {
							params[i] = new Integer(deactivateReason);
						} else if (paramTypes[i] == Integer.class) {
							params[i] = new Integer(deactivateReason);
						}
					}

					try {
						deactivateMethod.invoke(instance, params);
					} finally {
						if (params.length == 1) {
							SCRUtil.release(params);
						}
					}
				} else {
					if (deactivateMethodName != "deactivate") { //$NON-NLS-1$
						//the deactivate method is specified in the component description XML by the user.
						//It is expected to find it in the implementation class
						Activator.log(bc, LogService.LOG_ERROR, NLS.bind(Messages.SPECIFIED_DEACTIVATE_METHOD_NOT_FOUND, instance, this), null);
					}
				}
			} else {
				// retrieve the activate method from cache
				if (!deactivateCached) {
					deactivateCached = true;
					deactivateMethod = getMethod(instance, "deactivate", false); //$NON-NLS-1$
				}
				// invoke the method
				if (deactivateMethod != null) {
					Object[] params = SCRUtil.getObjectArray();
					params[0] = context;
					try {
						deactivateMethod.invoke(instance, params);
					} finally {
						SCRUtil.release(params);
					}
				}
			}
		} catch (Throwable t) {
			Activator.log(bc, LogService.LOG_ERROR, NLS.bind(Messages.ERROR_DEACTIVATING_INSTANCE, this), t);
		}
	}

	/**
	 * This method is called from the XML parser to validate the component once
	 * it is fully loaded!
	 * 
	 * @param line
	 *            the line at which the the component definition ends
	 * @param _namespace specify the namespace of the component according to XML SCR schema
	 */
	void validate(int line, int _namespace) {
		//		System.out.println("Validating component " + name + " with namespace " + (namespace11 ? "1.1" : "1.0"));
		this.namespace = _namespace;
		if (name == null) {
			if (isNamespaceAtLeast11()) {
				name = implementation;
			} else {
				throw new IllegalArgumentException(NLS.bind(Messages.NO_NAME_ATTRIBUTE, Integer.toString(line)));
			}
		}
		if (isNamespaceAtLeast11()) {
			if (!(configurationPolicy == CONF_POLICY_OPTIONAL || configurationPolicy == CONF_POLICY_REQUIRE || configurationPolicy == CONF_POLICY_IGNORE)) {
				throw new IllegalArgumentException(NLS.bind(Messages.INCORRECT_ACTIVATION_POLICY, name, Integer.toString(line)));
			}
		}

		if (isNamespaceAtLeast12()) {
			if (configurationPID == null) {
				configurationPID = name;
			}
		}

		if (implementation == null) {
			throw new IllegalArgumentException(NLS.bind(Messages.NO_IMPLEMENTATION_ATTRIBUTE, name, Integer.toString(line)));
		}

		// component factory is incompatible with service factory
		if (factory != null && serviceFactory) {
			throw new IllegalArgumentException(NLS.bind(Messages.INVALID_COMPONENT_FACTORY_AND_SERVICE_FACTORY, name));
		}

		if (immediate) {
			if (serviceFactory)
				throw new IllegalArgumentException(NLS.bind(Messages.INVALID_COMPONENT_IMMEDIATE_AND_SERVICE_FACTORY, name));
			if (factory != null)
				throw new IllegalArgumentException(NLS.bind(Messages.INVALID_COMPONENT_IMMEDIATE_AND_FACTORY, name));
		} else {
			if ((serviceInterfaces == null) && (factory == null)) {
				throw new IllegalArgumentException(NLS.bind(Messages.INVALID_COMPONENT_NO_SERVICES_NO_IMMEDIATE, name));
			}
		}

		// make sure that references are also valid
		if (references != null) {
			for (int i = 0; i < references.size(); i++) {
				ComponentReference r = (ComponentReference) references.elementAt(i);
				if (r.name == null) {
					if (isNamespaceAtLeast11()) {
						r.name = r.interfaceName;
					} else {
						throw new IllegalArgumentException(NLS.bind(Messages.COMPONENT_HAS_ILLEGAL_REFERENCE, new Object[] {name, Integer.toString(line), r}));
					}
				}
				if (r.interfaceName == null || r.name.length() == 0 || r.interfaceName.length() == 0) {
					throw new IllegalArgumentException(NLS.bind(Messages.COMPONENT_HAS_ILLEGAL_REFERENCE, new Object[] {name, Integer.toString(line), r}));
				}
				for (int j = i + 1; j < references.size(); j++) {
					ComponentReference ref2 = (ComponentReference) references.elementAt(j);
					if (r.name.equals(ref2.name)) {
						throw new IllegalArgumentException(NLS.bind(Messages.DUPLICATED_REFERENCE_NAMES, name, Integer.toString(line)));
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
			throw new ComponentException(NLS.bind(Messages.EXCEPTION_CREATING_COMPONENT_INSTANCE, this), t);
		}
	}

	/**
	 * This method will dispose everything
	 */
	// TODO : this method is not used - should be removed?
	public final void dispose() {

		activateCached = deactivateCached = false;
		activateMethod = deactivateMethod = null;

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

	public String getConfigurationPID() {
		if (isNamespaceAtLeast12()) {
			return configurationPID;
		}
		return name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public final String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Component["); //$NON-NLS-1$
		buffer.append("\n\tname = ").append(name); //$NON-NLS-1$
		if (isNamespaceAtLeast11()) {
			buffer.append("\n\tactivate = ").append(activateMethodName); //$NON-NLS-1$
			buffer.append("\n\tdeactivate = ").append(deactivateMethodName); //$NON-NLS-1$
			buffer.append("\n\tmodified = ").append(modifyMethodName); //$NON-NLS-1$
			buffer.append("\n\tconfiguration-policy = ").append(configurationPolicy); //$NON-NLS-1$
		}
		if (isNamespaceAtLeast12()) {
			buffer.append("\n\tconfiguration-pid = ").append(configurationPID); //$NON-NLS-1$
		}
		buffer.append("\n\tfactory = ").append(factory); //$NON-NLS-1$
		buffer.append("\n\tautoenable = ").append(autoenable); //$NON-NLS-1$
		buffer.append("\n\timmediate = ").append(immediate); //$NON-NLS-1$

		buffer.append("\n\timplementation = ").append(implementation); //$NON-NLS-1$
		buffer.append("\n\tstate = ").append(SCRUtil.getStateStringRepresentation(state)); //$NON-NLS-1$
		StringBuffer buf = new StringBuffer(200);
		if (properties != null) {
			buf.append('{');
			Enumeration keys = properties.keys();
			while (keys.hasMoreElements()) {
				Object key = keys.nextElement();
				buf.append(key).append('=').append(SCRUtil.getStringRepresentation(properties.get(key)));
				if (keys.hasMoreElements()) {
					buf.append(", "); //$NON-NLS-1$
				}
			}
			buf.append('}');
		}
		buffer.append("\n\tproperties = ").append(buf.toString()); //$NON-NLS-1$

		buffer.append("\n\tserviceFactory = ").append(serviceFactory); //$NON-NLS-1$
		buffer.append("\n\tserviceInterface = ").append(serviceInterfaces); //$NON-NLS-1$

		if (references == null) {
			buffer.append("\n\treferences = ").append("null"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			buffer.append("\n\treferences = {"); //$NON-NLS-1$
			for (int i = 0; i < references.size(); i++) {
				buffer.append("\n\t\t").append(references.elementAt(i)); //$NON-NLS-1$
			}
			buffer.append("\n\t}"); //$NON-NLS-1$
		}
		buffer.append("\n\tlocated in bundle = ").append(bundle); //$NON-NLS-1$
		buffer.append("\n]"); //$NON-NLS-1$
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

			out.writeInt(namespace);
			if (isNamespaceAtLeast11()) {
				if (configurationPolicy == CONF_POLICY_OPTIONAL) {
					//this is the default value. Do not write it. Just add a mark
					out.writeBoolean(false);
				} else {
					out.writeBoolean(true);
					out.writeUTF(configurationPolicy);
				}
				if (!activateMethodDeclared) {
					//this is the default value. Do not write it. Just add a mark
					out.writeBoolean(false);
				} else {
					out.writeBoolean(true);
					out.writeUTF(activateMethodName);
				}
				if (!deactivateMethodDeclared) {
					//this is the default value. Do not write it. Just add a mark
					out.writeBoolean(false);
				} else {
					out.writeBoolean(true);
					out.writeUTF(deactivateMethodName);
				}
				if (modifyMethodName == "") { //$NON-NLS-1$
					//this is the default value. Do not write it. Just add a mark
					out.writeBoolean(false);
				} else {
					out.writeBoolean(true);
					out.writeUTF(modifyMethodName);
				}
			}
			if (isNamespaceAtLeast12()) {
				if (configurationPID == name) {
					out.writeBoolean(false);
				} else {
					out.writeBoolean(true);
					out.writeUTF(configurationPID);
				}
			}
		} catch (Exception e) {
			Activator.log(null, LogService.LOG_ERROR, Messages.ERROR_WRITING_OBJECT, e);
			throw e;
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
			namespace = in.readInt();
			if (isNamespaceAtLeast11()) {
				flag = in.readBoolean();
				if (flag) {
					configurationPolicy = in.readUTF();
					if (configurationPolicy.equals(CONF_POLICY_IGNORE)) {
						configurationPolicy = CONF_POLICY_IGNORE;
					} else {
						configurationPolicy = CONF_POLICY_REQUIRE;
					}
				}
				flag = in.readBoolean();
				if (flag) {
					activateMethodName = in.readUTF();
					activateMethodDeclared = true;
				}
				flag = in.readBoolean();
				if (flag) {
					deactivateMethodName = in.readUTF();
					deactivateMethodDeclared = true;
				}
				flag = in.readBoolean();
				if (flag)
					modifyMethodName = in.readUTF();
			}
			if (isNamespaceAtLeast12()) {
				flag = in.readBoolean();
				if (flag) {
					configurationPID = in.readUTF();
				} else {
					configurationPID = name;
				}
			}
		} catch (Exception e) {
			Activator.log(null, LogService.LOG_ERROR, Messages.ERROR_READING_OBJECT, e);
			throw e;
		}
	}

	public ServiceComponentProp getComponentPropByPID(String pid) {
		if (componentProps != null) {
			for (int i = 0; i < componentProps.size(); i++) {
				ServiceComponentProp scp = (ServiceComponentProp) componentProps.elementAt(i);
				if (scp.getProperties() != null) {
					if (pid.equals(scp.getProperties().get(Constants.SERVICE_PID))) {
						return scp;
					}
				}
			}
		}
		return null;
	}

	public void addServiceComponentProp(ServiceComponentProp scp) {
		if (componentProps == null) {
			componentProps = new Vector(1);
		}
		componentProps.addElement(scp);
	}

	/**
	 * Return the ServiceComponentProp object created for this component. Note there might be more than one SCP objects. 
	 * This method will return always the first one
	 * @return the ServiceComponentProp object created for this component 
	 */
	public ServiceComponentProp getServiceComponentProp() {
		if (componentProps != null) {
			synchronized (componentProps) {
				if (!componentProps.isEmpty()) {
					return (ServiceComponentProp) componentProps.elementAt(0);
				}
			}
		}
		return null;
	}

	public boolean isNamespaceAtLeast11() {
		return namespace >= NAMESPACE_1_1;
	}

	public boolean isNamespaceAtLeast12() {
		return namespace >= NAMESPACE_1_2;
	}

	public boolean isImmediate() {
		return immediate;
	}

	public void setImmediate(boolean immediate) {
		this.immediate = immediate;
	}

	public String getConfigurationPolicy() {
		return configurationPolicy;
	}

	public void disable() {
		if (getState() == STATE_DISPOSED) {
			throw new IllegalStateException(Messages.COMPONENT_DISPOSED);
		} else if (getState() != STATE_DISABLED) {
			InstanceProcess.resolver.mgr.disableComponent(name, bundle);
		}
	}

	public void enable() {
		if (getState() == STATE_DISPOSED) {
			throw new IllegalStateException(Messages.COMPONENT_DISPOSED);
		} else if (getState() == STATE_DISABLED) {
			InstanceProcess.resolver.mgr.enableComponent(name, bundle);
		}
	}

	public String getActivate() {
		return activateMethodName;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public String getClassName() {
		return implementation;
	}

	public ComponentInstance getComponentInstance() {
		if (componentProps != null && !componentProps.isEmpty()) {
			//get the first built compoent's instance 
			Vector instances = ((ServiceComponentProp) componentProps.elementAt(0)).instances;
			if (!instances.isEmpty()) {
				return (ComponentInstance) instances.elementAt(0);
			}
		}
		//The component is not yet built
		return null;
	}

	public String getDeactivate() {
		return deactivateMethodName;
	}

	public String getFactory() {
		return factory;
	}

	public long getId() {
		if (componentProps != null && !componentProps.isEmpty()) {
			//get the first built component's ID 
			return ((Long) ((ServiceComponentProp) componentProps.elementAt(0)).properties.get(ComponentConstants.COMPONENT_ID)).longValue();
		}
		//The component is not yet given an ID by the SRC because it is not active
		return -1;
	}

	public String getModified() {
		if (!isNamespaceAtLeast11()) {
			return null;
		}
		return modifyMethodName;
	}

	public String getName() {
		return name;
	}

	public Dictionary getProperties() {
		if (readOnlyProps == null) {
			readOnlyProps = new ReadOnlyDictionary(properties);
		} else {
			// the scp properties may have been modified by configuration
			// update the instance with the current properties
			readOnlyProps.updateDelegate(properties);
		}
		return readOnlyProps;
	}

	public Reference[] getReferences() {
		if (references != null && !references.isEmpty()) {
			org.apache.felix.scr.Reference[] res = new org.apache.felix.scr.Reference[references.size()];
			references.copyInto(res);
			return res;
		}
		return null;
	}

	public String[] getServices() {
		return provides;
	}

	public int getState() {
		//check if there is at least one SCP created and return its state
		if (componentProps != null && !componentProps.isEmpty()) {
			//get the first component's state
			return ((ServiceComponentProp) componentProps.elementAt(0)).getState();
		}
		//return the current state of the component
		return state;
	}

	public boolean isActivateDeclared() {
		if (!isNamespaceAtLeast11()) {
			return false;
		}
		return activateMethodDeclared;
	}

	public boolean isDeactivateDeclared() {
		if (!isNamespaceAtLeast11()) {
			return false;
		}
		return deactivateMethodDeclared;
	}

	public boolean isDefaultEnabled() {
		return autoenable;
	}

	public boolean isServiceFactory() {
		return serviceFactory;
	}

	public void setState(int newState) {
		state = newState;
	}
}