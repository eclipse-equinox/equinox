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
 *    Simon Archer		 	- bug.id = 288783
 *    Lazar Kirchev		 	- bug.id = 320377
 *******************************************************************************/
package org.eclipse.equinox.internal.ds;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.ds.impl.ComponentInstanceImpl;
import org.eclipse.equinox.internal.ds.model.*;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentConstants;

/** 
 * SCRCommandProvider class provides some useful commands for managing the Service Component Runtime
 *
 * @author Stoyan Boshev
 * @version 1.0
 */
public class SCRCommandProvider implements CommandProvider {

	private Resolver resolver;
	private SCRManager scrManager;

	private int curID = 1;
	private Hashtable componentRefsIDs = null;

	protected SCRCommandProvider(SCRManager scrManager) {
		this.scrManager = scrManager;
		this.resolver = InstanceProcess.resolver;
	}

	public String getHelp() {
		return getHelp(null);
	}

	/*
	 * This method either returns the help message for a particular command, 
	 * or returns the help messages for all commands (if commandName is null)
	 */
	private String getHelp(String commandName) {
		boolean all = commandName == null;
		StringBuffer res = new StringBuffer(1000);
		if (all) {
			res.append("---").append(Messages.SCR).append("---\r\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (all || "list".equals(commandName) || "ls".equals(commandName)) { //$NON-NLS-1$ //$NON-NLS-2$
			res.append("\tlist/ls [-c] [bundle id] - ").append(Messages.LIST_ALL_COMPONENTS); //$NON-NLS-1$
			res.append("\r\n\t\t\t").append(Messages.LIST_ALL_BUNDLE_COMPONENTS); //$NON-NLS-1$
		}
		if (all || "component".equals(commandName) || "comp".equals(commandName)) { //$NON-NLS-1$ //$NON-NLS-2$
			res.append("\r\n\tcomponent/comp <component id> - ").append(Messages.PRINT_COMPONENT_INFO); //$NON-NLS-1$
			res.append("\r\n\t\t\t<component id> - ").append(Messages.COMPONENT_ID_DEFINIED_BY_LIST_COMMAND); //$NON-NLS-1$
		}
		if (all || "enable".equals(commandName) || "en".equals(commandName)) { //$NON-NLS-1$ //$NON-NLS-2$
			res.append("\r\n\tenable/en <component id> - ").append(Messages.ENABLE_COMPONENT); //$NON-NLS-1$
			res.append("\r\n\t\t\t<component id> - ").append(Messages.COMPONENT_ID_DEFINIED_BY_LIST_COMMAND); //$NON-NLS-1$
		}
		if (all || "disable".equals(commandName) || "dis".equals(commandName)) { //$NON-NLS-1$ //$NON-NLS-2$
			res.append("\n\tdisable/dis <component id> - ").append(Messages.DISABLE_COMPONENT); //$NON-NLS-1$
			res.append("\n\t\t\t<component id> - ").append(Messages.COMPONENT_ID_DEFINIED_BY_LIST_COMMAND); //$NON-NLS-1$
		}
		if (all || "enableAll".equals(commandName) || "enAll".equals(commandName)) { //$NON-NLS-1$ //$NON-NLS-2$
			res.append("\r\n\tenableAll/enAll [bundle id] - ").append(Messages.ENABLE_ALL_COMPONENTS); //$NON-NLS-1$
		}
		if (all || "disableAll".equals(commandName) || "disAll".equals(commandName)) { //$NON-NLS-1$ //$NON-NLS-2$
			res.append("\n\tdisableAll/disAll [bundle id] - ").append(Messages.DISABLE_ALL_COMPONENTS).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return res.toString();
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
				intp.println(NLS.bind(Messages.WRONG_PARAMETER, params[0]));
				return;
			}
			printComponentDetails(intp, compIndex);
			return;
		}
		intp.println(Messages.EXPECTED_PARAMETER_COMPONENT_ID);
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
				intp.println(NLS.bind(Messages.WRONG_PARAMETER, params[0]));
				return;
			}
			enableComponent(intp, compIndex);
			return;
		}
		intp.println(Messages.EXPECTED_PARAMETER_COMPONENT_ID);
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
				intp.println(NLS.bind(Messages.WRONG_PARAMETER, params[0]));
				return;
			}
			disableComponent(intp, compIndex);
			return;
		}
		intp.println(Messages.EXPECTED_PARAMETER_COMPONENT_ID);
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

	/**
	 * Handles the help command
	 * 
	 * @param intp
	 * @return description for a particular command or false if there is no command with the specified name
	 */
	public Object _help(CommandInterpreter intp) {
		String commandName = intp.nextArgument();
		if (commandName == null) {
			return Boolean.FALSE;
		}
		String help = getHelp(commandName);

		if (help.length() > 0) {
			return help;
		}
		return Boolean.FALSE;
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
			intp.println(NLS.bind(Messages.WRONG_PARAMETER2, bidString));
			return null;
		}
		Bundle b = Activator.bc.getBundle(bid);
		if (b == null) {
			intp.println(NLS.bind(Messages.BUNDLE_NOT_FOUND, bidString));
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
			intp.println(Messages.INVALID_COMPONENT_ID);
			return;
		}

		Bundle b = Activator.bc.getBundle(cRef.bid);
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
			intp.println(Messages.CANNOT_FIND_COMPONENT_BUNDLE);
		}
	}

	private void printComponentDetails(CommandInterpreter intp, ServiceComponent sc) {
		intp.println("\t" + sc.toString()); //$NON-NLS-1$
		intp.println(Messages.DYNAMIC_INFO);
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
			intp.println(Messages.COMPONENT_RESOLVED);
		} else {
			intp.println(Messages.COMPONENT_NOT_RESOLVED);
		}

		if (unresulvedReferences != null) {
			intp.println(Messages.NOT_RESOLVED_REFERENCES);
			for (int i = 0; i < unresulvedReferences.size(); i++) {
				intp.println("    " + unresulvedReferences.elementAt(i)); //$NON-NLS-1$
			}
		} else {
			intp.println(Messages.ALL_REFERENCES_RESOLVED);
		}
		intp.println(Messages.COMPONENT_CONFIGURATIONS);
		Vector enabledSCPs = (Vector) resolver.scpEnabled.clone();
		for (int i = 0; i < enabledSCPs.size(); i++) {
			ServiceComponentProp scp = (ServiceComponentProp) enabledSCPs.elementAt(i);
			if (scp.serviceComponent == sc) {
				printSCP(intp, scp);
			}
		}
		if (sc.getConfigurationPolicy() == ServiceComponent.CONF_POLICY_REQUIRE) {
			if (resolved && (sc.componentProps == null || sc.componentProps.size() == 0)) {
				intp.println(Messages.NO_BUILT_COMPONENT_CONFIGURATIONS);
			}
		}
		intp.println();
	}

	private void printSCP(CommandInterpreter intp, ServiceComponentProp scp) {
		Hashtable props = scp.properties;
		intp.println(Messages.CONFIG_PROPERTIES);
		Enumeration keys = props.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = props.get(key);
			intp.print("      " + key + " = "); //$NON-NLS-1$ //$NON-NLS-2$
			intp.print(SCRUtil.getStringRepresentation(value));
			intp.println();
		}
		intp.println("    Instances:"); //$NON-NLS-1$
		if (scp.instances.size() > 0) {
			ComponentInstanceImpl instance = null;
			for (int i = 0; i < scp.instances.size(); i++) {
				instance = (ComponentInstanceImpl) scp.instances.elementAt(i);
				intp.println("      " + instance); //$NON-NLS-1$
				if (instance.bindedServices.size() > 0) {
					intp.println("    	Bound References:"); //$NON-NLS-1$
					Enumeration refs = instance.bindedServices.keys();
					ServiceReference sr = null;
					while (refs.hasMoreElements()) {
						sr = (ServiceReference) refs.nextElement();
						Object interfaces = sr.getProperty(Constants.OBJECTCLASS);
						intp.println("        " + SCRUtil.getStringRepresentation(interfaces)); //$NON-NLS-1$
						intp.println("      		-> " + instance.bindedServices.get(sr)); //$NON-NLS-1$
					}
				}
			}
		} else {
			// there are no instances either because the references are not satisfied or because some runtime issues appear; for example the bind method was not found in the class
			String issues = scp.serviceComponent.getComponentIssues();
			if (issues != null) {
				intp.println("    No instances were created because: " + issues); //$NON-NLS-1$	
			}
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
			intp.println(NLS.bind(Messages.ENABLING_ALL_BUNDLE_COMPONENTS, getBundleRepresentationName(b)));
		} else {
			intp.println(Messages.ENABLING_ALL_COMPONENTS);
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
				Bundle[] allBundles = Activator.bc.getBundles();
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
			intp.println(NLS.bind(Messages.DISABLING_ALL_BUNDLE_COMPONENTS, getBundleRepresentationName(b)));
		} else {
			intp.println(Messages.DISABLING_ALL_COMPONENTS);
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
						resolver.disableComponents(componentsToDisable, ComponentConstants.DEACTIVATION_REASON_DISABLED);
					}
				}
			}
		} else {
			if (scrManager.bundleToServiceComponents != null) {
				Bundle[] allBundles = Activator.bc.getBundles();
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
					resolver.disableComponents(componentsToDisable, ComponentConstants.DEACTIVATION_REASON_DISABLED);
				}
			}
		}
	}

	private void enableComponent(CommandInterpreter intp, int componentIndex) {
		ComponentRef cRef = findComponentWithID(componentIndex);
		if (cRef == null) {
			intp.println(Messages.INVALID_COMPONENT_ID);
			return;
		}
		scrManager.enableComponent(cRef.name, Activator.bc.getBundle(cRef.bid));
		intp.println(NLS.bind(Messages.SENT_ENABLING_REQUEST, cRef.name));
	}

	private void disableComponent(CommandInterpreter intp, int componentIndex) {
		ComponentRef cRef = findComponentWithID(componentIndex);
		if (cRef == null) {
			intp.println(Messages.INVALID_COMPONENT_ID);
			return;
		}
		scrManager.disableComponent(cRef.name, Activator.bc.getBundle(cRef.bid));
		intp.println(NLS.bind(Messages.SENT_DISABLING_REQUEST, cRef.name));
	}

	/* commands */

	private void listComponents(CommandInterpreter intp, Bundle b, boolean completeInfo) {
		if (componentRefsIDs == null)
			componentRefsIDs = new Hashtable(101);

		if (b != null) {
			intp.println(NLS.bind(Messages.COMPONENTS_IN_BUNDLE, getBundleRepresentationName(b)));
			if (componentRefsIDs.isEmpty()) {
				initComponentRefs();
			}
		} else {
			intp.println(Messages.ALL_COMPONENTS);
		}

		if (b == null || scrManager.bundleToServiceComponents != null && scrManager.bundleToServiceComponents.get(b) != null) {
			intp.print("ID"); //$NON-NLS-1$
			if (completeInfo) {
				intp.println(Messages.COMPONENT_DETAILS);
			} else {
				intp.print(Messages.STATE);
				intp.print(Messages.COMPONENT_NAME);
				intp.println(Messages.LOCATED_IN_BUNDLE);
			}
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
							intp.print("\t" + SCRUtil.getStateStringRepresentation(sc.getState())); //$NON-NLS-1$
							intp.print("\t\t" + sc.name); //$NON-NLS-1$
							intp.println("\t\t\t" + getBundleRepresentationName(b) + "(bid=" + b.getBundleId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}
				}

				// check the bundle for issues during components resolving
				// first check for service component header in this bundle
				String dsHeader = null;
				Dictionary allHeaders = b.getHeaders(""); //$NON-NLS-1$

				if (!((dsHeader = (String) allHeaders.get(ComponentConstants.SERVICE_COMPONENT)) != null)) {
					// no component descriptions in this bundle
					intp.println("No ServiceComponent header was found in bundle " + b.toString()); //$NON-NLS-1$
					return;
				}

				// second check if the ds xml is correct
				Vector issues = parseXMLDeclaration(b, dsHeader);
				if (issues.size() > 0) {
					intp.println();
					intp.println("Issues encountered when parsing components xml declarations"); //$NON-NLS-1$
					for (int i = 0; i < issues.size(); i++) {
						intp.println("\t" + issues.get(i)); //$NON-NLS-1$
						intp.println();
					}
				}

			}

		} else {
			if (scrManager.bundleToServiceComponents != null) {
				Bundle[] allBundles = Activator.bc.getBundles();
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
								intp.print("\t" + SCRUtil.getStateStringRepresentation(sc.getState())); //$NON-NLS-1$
								intp.print("\t\t" + sc.name); //$NON-NLS-1$
								intp.println("\t\t\t" + getBundleRepresentationName(allBundles[j]) + "(bid=" + allBundles[j].getBundleId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							}
						}
					}
				}
			}
		}
	}

	private void initComponentRefs() {
		if (scrManager.bundleToServiceComponents != null) {
			Bundle[] allBundles = Activator.bc.getBundles();
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

	protected Vector parseXMLDeclaration(Bundle bundle, String dsHeader) {
		Vector componentsIssues = new Vector();
		Vector components = new Vector();
		try {
			if (dsHeader != null) {
				StringTokenizer tok = new StringTokenizer(dsHeader, ","); //$NON-NLS-1$
				DeclarationParser parser = new DeclarationParser(true);
				// process all definition file
				while (tok.hasMoreElements()) {
					String definitionFile = tok.nextToken().trim();
					int ind = definitionFile.lastIndexOf('/');
					String path = ind != -1 ? definitionFile.substring(0, ind) : "/"; //$NON-NLS-1$
					InputStream is = null;

					Enumeration urls = bundle.findEntries(path, ind != -1 ? definitionFile.substring(ind + 1) : definitionFile, false);
					if (urls == null || !urls.hasMoreElements()) {
						componentsIssues.add(NLS.bind(Messages.COMPONENT_XML_NOT_FOUND, bundle.getSymbolicName(), definitionFile));
						continue;
					}

					// illegal components are ignored, but framework event is posted for
					// them; however, it will continue and try to load any legal
					// definitions
					URL url;
					while (urls.hasMoreElements()) {
						url = (URL) urls.nextElement();
						if (Activator.DEBUG) {
							Activator.log.debug("ComponentStorage.parseXMLDeclaration(): loading " + url.toString(), null); //$NON-NLS-1$
						}
						try {
							is = url.openStream();
							if (is == null) {
								componentsIssues.add(NLS.bind(Messages.CANT_OPEN_STREAM_TO_COMPONENT_XML, url));
							} else {
								try {
									parser.parse(is, bundle, components, url.toString());
								} catch (Exception e) {
									componentsIssues.add(e.getMessage());
								}
							}
						} catch (IOException ie) {
							componentsIssues.add(NLS.bind(Messages.ERROR_OPENING_COMP_XML, url) + "\n\t" + ie); //$NON-NLS-1$
						} catch (Throwable t) {
							componentsIssues.add(NLS.bind(Messages.ILLEGAL_DEFINITION_FILE, url) + "\n\t" + t); //$NON-NLS-1$
						} finally {
							if (is != null) {
								is.close();
							}
						}
					}
				} // end while

				components = parser.components;
				// make sure the clean-up the parser cache, for the next bundle to
				// work properly!!!
				parser.components = null;
			}
		} catch (Exception e) {
			componentsIssues.add("Exception [" + e + "] while parsing DS xml definition."); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return componentsIssues;
	}

	private static class ComponentRef {
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
