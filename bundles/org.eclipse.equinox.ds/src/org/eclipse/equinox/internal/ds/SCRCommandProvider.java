/*******************************************************************************
 * Copyright (c) 1997-2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds;

import java.util.*;
import org.eclipse.equinox.internal.ds.model.*;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.*;

/** 
 * SCRCommandProvider class provides some useful commands for managing the Service Component Runtime
 *
 * @author Stoyan Boshev
 * @version 1.0
 */
public class SCRCommandProvider implements CommandProvider {

	private final static String helpText = "---Service Component Runtime---\n" + //$NON-NLS-1$
			"\tlist/ls [-c] [bundle id] - Lists all components; add -c to display the complete info for each component;" + "\n\t\t\tuse [bundle id] to list the components of the specified bundle\n" + //$NON-NLS-1$ //$NON-NLS-2$
			"\tcomponent/comp <component id> - Prints all available information about the specified component;" + "\n\t\t\t<component id> - The ID of the component as displayed by the list command\n" + //$NON-NLS-1$ //$NON-NLS-2$
			"\tenable/en <component id> - Enables the specified component; " + "\n\t\t\t<component id> - The ID of the component as displayed by the list command\n" + //$NON-NLS-1$ //$NON-NLS-2$
			"\tdisable/dis <component id> - Disables the specified component; " + "\n\t\t\t<component id> - The ID of the component as displayed by the list command\n" + //$NON-NLS-1$ //$NON-NLS-2$
			"\tenableAll/enAll [bundle id] - Enables all components; use [bundle id] to enable all components of the specified bundle\n" + //$NON-NLS-1$
			"\tdisableAll/disAll [bundle id] - Disables all components; use [bundle id] to disable all components of the specified bundle\n"; //$NON-NLS-1$

	private Resolver resolver;
	private SCRManager scrManager;

	private int curID = 1;
	private Hashtable componentRefsIDs = null;

	protected SCRCommandProvider(SCRManager scrManager) {
		this.scrManager = scrManager;
		this.resolver = InstanceProcess.resolver;
	}

	public String getHelp() {
		return helpText;
	}

	/**
	 *  Handle the list command
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _list(CommandInterpreter intp) throws Exception {
		boolean completeInfo = false;
		Bundle b = null;
		String[] params = getParams(intp);
		if (params.length > 0) {
			if (params[0].equals("-c")) { //$NON-NLS-1$
				completeInfo = true;
				if (params.length > 1) {
					b = getBundle(intp, params[1]);
					if (b == null) {
						return;
					}
				}
			} else {
				b = getBundle(intp, params[0]);
				if (b == null) {
					return;
				}
			}
		}
		listComponents(intp, b, completeInfo);
	}

	/**
	 *  Shortcut to list command
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _ls(CommandInterpreter intp) throws Exception {
		_list(intp);
	}

	/**
	 *  Handle the component command
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _component(CommandInterpreter intp) throws Exception {
		String[] params = getParams(intp);
		if (params.length > 0) {
			int compIndex = -1;
			try {
				compIndex = Integer.parseInt(params[0]);
			} catch (NumberFormatException nfe) {
				intp.println("Wrong parameter " + params[0] + " ! It is not a number!"); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}
			printComponentDetails(intp, compIndex);
			return;
		}
		intp.println("This command expects a component ID as parameter!"); //$NON-NLS-1$
	}

	/**
	 *  Shortcut to component command
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _comp(CommandInterpreter intp) throws Exception {
		_component(intp);
	}

	/**
	 *  Handle the enable command
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _enable(CommandInterpreter intp) throws Exception {
		String[] params = getParams(intp);
		if (params.length > 0) {
			int compIndex = -1;
			try {
				compIndex = Integer.parseInt(params[0]);
			} catch (NumberFormatException nfe) {
				intp.println("Wrong parameter " + params[0] + " ! It is not a number!"); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}
			enableComponent(intp, compIndex);
			return;
		}
		intp.println("This command expects a component ID as parameter!"); //$NON-NLS-1$
	}

	/**
	 *  Shortcut to enable command
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _en(CommandInterpreter intp) throws Exception {
		_enable(intp);
	}

	/**
	 *  Handle the disable command
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _disable(CommandInterpreter intp) throws Exception {
		String[] params = getParams(intp);
		if (params.length > 0) {
			int compIndex = -1;
			try {
				compIndex = Integer.parseInt(params[0]);
			} catch (NumberFormatException nfe) {
				intp.println("Wrong parameter " + params[0] + " ! It is not a number!"); //$NON-NLS-1$ //$NON-NLS-2$
				return;
			}
			disableComponent(intp, compIndex);
			return;
		}
		intp.println("This command expects a component ID as parameter!"); //$NON-NLS-1$
	}

	/**
	 *  Shortcut to disable command
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _dis(CommandInterpreter intp) throws Exception {
		_disable(intp);
	}

	/**
	 *  Handle the enableAll command
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _enableAll(CommandInterpreter intp) throws Exception {
		String[] params = getParams(intp);
		Bundle b = null;
		if (params.length > 0) {
			b = getBundle(intp, params[0]);
			if (b == null) {
				return;
			}
		}
		enableAll(intp, b);
	}

	/**
	 *  Shortcut to enableAll command
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _enAll(CommandInterpreter intp) throws Exception {
		_enableAll(intp);
	}

	/**
	 *  Handle the disableAll command
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _disableAll(CommandInterpreter intp) throws Exception {
		String[] params = getParams(intp);
		Bundle b = null;
		if (params.length > 0) {
			b = getBundle(intp, params[0]);
			if (b == null) {
				return;
			}
		}
		disableAll(intp, b);
	}

	/**
	 *  Shortcut to disableAll command
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _disAll(CommandInterpreter intp) throws Exception {
		_disableAll(intp);
	}

	private String[] getParams(CommandInterpreter intp) {
		Vector arguments = new Vector();
		String arg = intp.nextArgument();
		while (arg != null) {
			if (arg != null) {
				arguments.addElement(arg);
			}
			arg = intp.nextArgument();
		}
		String[] res = new String[arguments.size()];
		arguments.copyInto(res);
		return res;
	}

	private Bundle getBundle(CommandInterpreter intp, String bidString) {
		long bid = -1;
		try {
			bid = Long.parseLong(bidString);
		} catch (NumberFormatException nfe) {
			intp.println("Wrong parameter " + bidString); //$NON-NLS-1$
			return null;
		}
		Bundle b = scrManager.bc.getBundle(bid);
		if (b == null) {
			intp.println("Bundle with ID " + bidString + " was not found!"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return b;
	}

	private ComponentRef findComponentWithID(int compID) {
		if (componentRefsIDs != null) {
			Enumeration keys = componentRefsIDs.keys();
			while (keys.hasMoreElements()) {
				ComponentRef key = (ComponentRef) keys.nextElement();
				if (key.id == compID) {
					return key;
				}
			}
		}
		return null;
	}

	private void printComponentDetails(CommandInterpreter intp, int componentIndex) {
		ComponentRef cRef = findComponentWithID(componentIndex);
		if (cRef == null) {
			intp.println("Invalid component ID!"); //$NON-NLS-1$
			return;
		}

		Bundle b = scrManager.bc.getBundle(cRef.bid);
		if (b != null) {
			Vector components = (Vector) scrManager.bundleToServiceComponents.get(b);
			if (components != null) {
				for (int i = 0; i < components.size(); i++) {
					ServiceComponent sc = (ServiceComponent) components.elementAt(i);
					if (sc.name.equals(cRef.name)) {
						printComponentDetails(intp, sc);
						break;
					}
				}
			}
		} else {
			intp.println("Could not find the bundle of the specified component! It is possibly uninstalled."); //$NON-NLS-1$
		}
	}

	private void printComponentDetails(CommandInterpreter intp, ServiceComponent sc) {
		intp.println("\t" + sc.toString()); //$NON-NLS-1$
		intp.println("Dynamic information :"); //$NON-NLS-1$
		Vector unresulvedReferences = getUnresolvedReferences(sc);
		boolean resolved = true;
		if (unresulvedReferences != null) {
			for (int i = 0; i < unresulvedReferences.size(); i++) {
				if (isMandatory((ComponentReference) unresulvedReferences.elementAt(i))) {
					resolved = false;
					break;
				}
			}
		}
		if (resolved) {
			intp.println("  The component is resolved"); //$NON-NLS-1$
		} else {
			intp.println("  *The component is NOT resolved"); //$NON-NLS-1$
		}

		if (unresulvedReferences != null) {
			intp.println("  The following references are not resolved:"); //$NON-NLS-1$
			for (int i = 0; i < unresulvedReferences.size(); i++) {
				intp.println("    " + unresulvedReferences.elementAt(i)); //$NON-NLS-1$
			}
		} else {
			intp.println("  All component references are resolved"); //$NON-NLS-1$
		}
		intp.println("  Component configurations :"); //$NON-NLS-1$
		Vector enabledSCPs = (Vector) resolver.scpEnabled.clone();
		for (int i = 0; i < enabledSCPs.size(); i++) {
			ServiceComponentProp scp = (ServiceComponentProp) enabledSCPs.elementAt(i);
			if (scp.serviceComponent == sc) {
				printSCP(intp, scp);
			}
		}
		intp.println();
	}

	private void printSCP(CommandInterpreter intp, ServiceComponentProp scp) {
		Hashtable props = scp.properties;
		intp.println("    Configuration properties:"); //$NON-NLS-1$
		Enumeration keys = props.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = props.get(key);
			intp.print("      " + key + " = "); //$NON-NLS-1$ //$NON-NLS-2$
			printPropertyValue(intp, value);
			intp.println();
		}
		intp.println("    Instances:"); //$NON-NLS-1$
		for (int i = 0; i < scp.instances.size(); i++) {
			intp.println("      " + scp.instances.elementAt(i)); //$NON-NLS-1$
		}
	}

	private void printPropertyValue(CommandInterpreter intp, Object value) {
		if (value instanceof Object[]) {
			intp.print("Object["); //$NON-NLS-1$
			Object[] arr = (Object[]) value;
			for (int i = 0; i < arr.length; i++) {
				printPropertyValue(intp, arr[i]);
				if (i != arr.length - 1) {
					intp.print(","); //$NON-NLS-1$
				}
			}
			intp.print("]"); //$NON-NLS-1$
		} else if (value instanceof String[]) {
			intp.print("String["); //$NON-NLS-1$
			String[] arr = (String[]) value;
			for (int i = 0; i < arr.length; i++) {
				intp.print(arr[i]);
				if (i != arr.length - 1) {
					intp.print(","); //$NON-NLS-1$
				}
			}
			intp.print("]"); //$NON-NLS-1$
		} else if (value instanceof int[]) {
			intp.print("int["); //$NON-NLS-1$
			int[] arr = (int[]) value;
			for (int i = 0; i < arr.length; i++) {
				intp.print(arr[i] + ""); //$NON-NLS-1$
				if (i != arr.length - 1) {
					intp.print(","); //$NON-NLS-1$
				}
			}
			intp.print("]"); //$NON-NLS-1$
		} else if (value instanceof long[]) {
			intp.print("long["); //$NON-NLS-1$
			long[] arr = (long[]) value;
			for (int i = 0; i < arr.length; i++) {
				intp.print(arr[i] + ""); //$NON-NLS-1$
				if (i != arr.length - 1) {
					intp.print(","); //$NON-NLS-1$
				}
			}
			intp.print("]"); //$NON-NLS-1$
		} else if (value instanceof char[]) {
			intp.print("char["); //$NON-NLS-1$
			char[] arr = (char[]) value;
			for (int i = 0; i < arr.length; i++) {
				intp.print(arr[i] + ""); //$NON-NLS-1$
				if (i != arr.length - 1) {
					intp.print(","); //$NON-NLS-1$
				}
			}
			intp.print("]"); //$NON-NLS-1$
		} else if (value instanceof boolean[]) {
			intp.print("boolean["); //$NON-NLS-1$
			boolean[] arr = (boolean[]) value;
			for (int i = 0; i < arr.length; i++) {
				intp.print(arr[i] + ""); //$NON-NLS-1$
				if (i != arr.length - 1) {
					intp.print(","); //$NON-NLS-1$
				}
			}
			intp.print("]"); //$NON-NLS-1$
		} else if (value instanceof double[]) {
			intp.print("double["); //$NON-NLS-1$
			double[] arr = (double[]) value;
			for (int i = 0; i < arr.length; i++) {
				intp.print(arr[i] + ""); //$NON-NLS-1$
				if (i != arr.length - 1) {
					intp.print(","); //$NON-NLS-1$
				}
			}
			intp.print("]"); //$NON-NLS-1$
		} else if (value instanceof float[]) {
			intp.print("float["); //$NON-NLS-1$
			float[] arr = (float[]) value;
			for (int i = 0; i < arr.length; i++) {
				intp.print(arr[i] + ""); //$NON-NLS-1$
				if (i != arr.length - 1) {
					intp.print(","); //$NON-NLS-1$
				}
			}
			intp.print("]"); //$NON-NLS-1$
		} else {
			intp.print(value.toString());
		}
	}

	private Vector getUnresolvedReferences(ServiceComponent sc) {
		Vector unresolved = new Vector();
		if (sc.references != null) {
			for (int i = 0; i < sc.references.size(); i++) {
				ComponentReference ref = (ComponentReference) sc.references.elementAt(i);
				if (!hasProviders(ref)) {
					unresolved.addElement(ref);
				}
			}
		}

		return unresolved.isEmpty() ? null : unresolved;
	}

	private boolean hasProviders(ComponentReference ref) {
		////check whether the component's bundle has service GET permission
		if (System.getSecurityManager() != null && !ref.component.bc.getBundle().hasPermission(new ServicePermission(ref.interfaceName, ServicePermission.GET))) {
			return false;
		}
		//// Get all service references for this target filter
		try {
			ServiceReference[] serviceReferences = null;
			serviceReferences = ref.component.bc.getServiceReferences(ref.interfaceName, ref.target);
			if (serviceReferences != null) {
				return true;
			}
		} catch (InvalidSyntaxException e) {
			//do nothing
		}
		return false;
	}

	private boolean isMandatory(ComponentReference ref) {
		return ref.cardinality == ComponentReference.CARDINALITY_1_1 || ref.cardinality == ComponentReference.CARDINALITY_1_N;
	}

	private void enableAll(CommandInterpreter intp, Bundle b) {
		Vector componentsToEnable = new Vector();
		if (b != null) {
			intp.println("Enabling all components in bundle " + getBundleRepresentationName(b)); //$NON-NLS-1$
		} else {
			intp.println("Enabling all components"); //$NON-NLS-1$
		}
		if (b != null) {
			if (scrManager.bundleToServiceComponents != null) {
				Vector components = (Vector) scrManager.bundleToServiceComponents.get(b);
				if (components != null) {
					for (int i = 0; i < components.size(); i++) {
						ServiceComponent sc = (ServiceComponent) components.elementAt(i);
						if (!sc.enabled) {
							componentsToEnable.addElement(sc);
							sc.enabled = true;
						}
					}
					if (!componentsToEnable.isEmpty()) {
						resolver.enableComponents(componentsToEnable);
					}
				}
			}
		} else {
			if (scrManager.bundleToServiceComponents != null) {
				Bundle[] allBundles = scrManager.bc.getBundles();
				for (int j = 0; j < allBundles.length; j++) {
					Vector components = (Vector) scrManager.bundleToServiceComponents.get(allBundles[j]);
					if (components != null) {
						for (int i = 0; i < components.size(); i++) {
							ServiceComponent sc = (ServiceComponent) components.elementAt(i);
							if (!sc.enabled) {
								componentsToEnable.addElement(sc);
								sc.enabled = true;
							}
						}
					}
				}
				if (!componentsToEnable.isEmpty()) {
					resolver.enableComponents(componentsToEnable);
				}
			}
		}
	}

	private void disableAll(CommandInterpreter intp, Bundle b) {
		Vector componentsToDisable = new Vector();
		if (b != null) {
			intp.println("Disabling all components in bundle " + getBundleRepresentationName(b)); //$NON-NLS-1$
		} else {
			intp.println("Disabling all components"); //$NON-NLS-1$
		}
		if (b != null) {
			if (scrManager.bundleToServiceComponents != null) {
				Vector components = (Vector) scrManager.bundleToServiceComponents.get(b);
				if (components != null) {
					for (int i = 0; i < components.size(); i++) {
						ServiceComponent sc = (ServiceComponent) components.elementAt(i);
						if (sc.enabled) {
							componentsToDisable.addElement(sc);
							sc.enabled = false;
						}
					}
					if (!componentsToDisable.isEmpty()) {
						resolver.disableComponents(componentsToDisable);
					}
				}
			}
		} else {
			if (scrManager.bundleToServiceComponents != null) {
				Bundle[] allBundles = scrManager.bc.getBundles();
				for (int j = 0; j < allBundles.length; j++) {
					Vector components = (Vector) scrManager.bundleToServiceComponents.get(allBundles[j]);
					if (components != null) {
						for (int i = 0; i < components.size(); i++) {
							ServiceComponent sc = (ServiceComponent) components.elementAt(i);
							if (sc.enabled) {
								componentsToDisable.addElement(sc);
								sc.enabled = false;
							}
						}
					}
				}
				if (!componentsToDisable.isEmpty()) {
					resolver.disableComponents(componentsToDisable);
				}
			}
		}
	}

	private void enableComponent(CommandInterpreter intp, int componentIndex) {
		ComponentRef cRef = findComponentWithID(componentIndex);
		if (cRef == null) {
			intp.println("Invalid component ID!"); //$NON-NLS-1$
			return;
		}
		scrManager.enableComponent(cRef.name, scrManager.bc.getBundle(cRef.bid));
		intp.println("Sent request for enabling component " + cRef.name); //$NON-NLS-1$
	}

	private void disableComponent(CommandInterpreter intp, int componentIndex) {
		ComponentRef cRef = findComponentWithID(componentIndex);
		if (cRef == null) {
			intp.println("Invalid component ID!"); //$NON-NLS-1$
			return;
		}
		scrManager.disableComponent(cRef.name, scrManager.bc.getBundle(cRef.bid));
		intp.println("Sent request for disabling component " + cRef.name); //$NON-NLS-1$
	}

	/* commands */

	private void listComponents(CommandInterpreter intp, Bundle b, boolean completeInfo) {
		if (componentRefsIDs == null)
			componentRefsIDs = new Hashtable(101);

		if (b != null) {
			intp.println("Components in bundle " + getBundleRepresentationName(b) + " :"); //$NON-NLS-1$ //$NON-NLS-2$
			if (componentRefsIDs.isEmpty()) {
				initComponentRefs();
			}
		} else {
			intp.println("All Components:"); //$NON-NLS-1$
		}
		intp.print("ID"); //$NON-NLS-1$
		if (completeInfo) {
			intp.println("\tComponent details"); //$NON-NLS-1$
		} else {
			intp.print("\tState"); //$NON-NLS-1$
			intp.print("\t\tComponent Name"); //$NON-NLS-1$
			intp.println("\t\tLocated in bundle"); //$NON-NLS-1$
		}

		if (b != null) {
			if (scrManager.bundleToServiceComponents != null) {
				Vector components = (Vector) scrManager.bundleToServiceComponents.get(b);
				if (components != null) {
					for (int i = 0; i < components.size(); i++) {
						ServiceComponent sc = (ServiceComponent) components.elementAt(i);
						ComponentRef aRef = new ComponentRef(b.getBundleId(), sc.name);
						ComponentRef ref = (ComponentRef) componentRefsIDs.get(aRef);
						if (ref == null) {
							ref = aRef;
							ref.id = generateID();
							componentRefsIDs.put(ref, ref);
						}
						if (completeInfo) {
							intp.print(ref.id + ""); //$NON-NLS-1$
							printComponentDetails(intp, sc);
						} else {
							////print short info
							intp.print("" + ref.id); //$NON-NLS-1$
							intp.print(sc.enabled ? "\tEnabled" : "\tDisabled"); //$NON-NLS-1$ //$NON-NLS-2$
							intp.print("\t\t" + sc.name); //$NON-NLS-1$
							intp.println("\t\t" + getBundleRepresentationName(b) + "(bid=" + b.getBundleId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}
				}
			}
		} else {
			if (scrManager.bundleToServiceComponents != null) {
				Bundle[] allBundles = scrManager.bc.getBundles();
				for (int j = 0; j < allBundles.length; j++) {
					Vector components = (Vector) scrManager.bundleToServiceComponents.get(allBundles[j]);
					if (components != null) {
						for (int i = 0; i < components.size(); i++) {
							ServiceComponent sc = (ServiceComponent) components.elementAt(i);
							ComponentRef aRef = new ComponentRef(allBundles[j].getBundleId(), sc.name);
							ComponentRef ref = (ComponentRef) componentRefsIDs.get(aRef);
							if (ref == null) {
								ref = aRef;
								ref.id = generateID();
								componentRefsIDs.put(ref, ref);
							}

							if (completeInfo) {
								intp.print(ref.id + ""); //$NON-NLS-1$
								printComponentDetails(intp, sc);
							} else {
								////print short info
								intp.print("" + ref.id); //$NON-NLS-1$
								intp.print(sc.enabled ? "\tEnabled" : "\tDisabled"); //$NON-NLS-1$ //$NON-NLS-2$
								intp.print("\t\t" + sc.name); //$NON-NLS-1$
								intp.println("\t\t" + getBundleRepresentationName(allBundles[j]) + "(bid=" + allBundles[j].getBundleId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							}
						}
					}
				}
			}
		}
	}

	private void initComponentRefs() {
		if (scrManager.bundleToServiceComponents != null) {
			Bundle[] allBundles = scrManager.bc.getBundles();
			for (int j = 0; j < allBundles.length; j++) {
				Vector components = (Vector) scrManager.bundleToServiceComponents.get(allBundles[j]);
				if (components != null) {
					for (int i = 0; i < components.size(); i++) {
						ServiceComponent sc = (ServiceComponent) components.elementAt(i);
						ComponentRef aRef = new ComponentRef(allBundles[j].getBundleId(), sc.name);
						ComponentRef ref = (ComponentRef) componentRefsIDs.get(aRef);
						if (ref == null) {
							ref = aRef;
							ref.id = generateID();
							componentRefsIDs.put(ref, ref);
						}
					}
				}
			}
		}
	}

	private String getBundleRepresentationName(Bundle b) {
		String res = b.getSymbolicName();
		if (res == null) {
			res = "with ID " + b.getBundleId(); //$NON-NLS-1$
		}
		return res;
	}

	private synchronized int generateID() {
		return curID++;
	}

	public class ComponentRef {
		////the ID of the bundle holding this service component
		long bid;
		////the name of the service component
		String name;
		////the temporary id given to this reference. It will be used by certain console commands
		int id = -1;

		public ComponentRef(long bid, String name) {
			this.bid = bid;
			this.name = name;
		}

		public boolean equals(Object o) {
			if (o instanceof ComponentRef) {
				ComponentRef obj = (ComponentRef) o;
				return (obj.bid == bid && name.equals(obj.name));
			}
			return false;
		}

		public int hashCode() {
			return name.hashCode();
		}

	}

}
