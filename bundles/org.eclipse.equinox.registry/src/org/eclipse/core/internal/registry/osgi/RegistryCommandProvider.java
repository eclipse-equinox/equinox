/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Muskalla - fix for the console help text (bug 240607) 
 *******************************************************************************/

package org.eclipse.core.internal.registry.osgi;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

public class RegistryCommandProvider implements CommandProvider {

	private final static String NEW_LINE = "\r\n"; //$NON-NLS-1$

	private static final String indent = "   "; //$NON-NLS-1$

	private boolean verbose = false; // is command run in a "verbose" mode?

	public String getHelp() {
		return getHelp(null);
	}

	/*
	 * This method either returns the help message for a particular command, 
	 * or returns the help messages for all commands (if commandName is null)
	 */
	private String getHelp(String commandName) {
		boolean all = commandName == null;
		StringBuffer sb = new StringBuffer();
		if (all) {
			sb.append("---Extension Registry Commands---"); //$NON-NLS-1$
			sb.append(NEW_LINE);
		}
		if (all || "ns".equals(commandName)) { //$NON-NLS-1$
			sb.append("\tns [-v] [name] - display extension points in the namespace; add -v to display extensions"); //$NON-NLS-1$
			sb.append(NEW_LINE);
		}
		if (all || "pt".equals(commandName)) { //$NON-NLS-1$
			sb.append("\tpt [-v] uniqueExtensionPointId - display the extension point and extensions; add -v to display config elements"); //$NON-NLS-1$
			sb.append(NEW_LINE);
		}
		return sb.toString();
	}

	public void _ns(CommandInterpreter ci) throws Exception {
		String namespace = getArgument(ci);
		if (namespace == null) {
			String[] namespaces = RegistryFactory.getRegistry().getNamespaces();
			ci.println("Namespace(s):"); //$NON-NLS-1$
			ci.println("-------------------"); //$NON-NLS-1$
			for (int i = 0; i < namespaces.length; i++)
				ci.println(namespaces[i]);
			return;
		}

		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint[] extpts = registry.getExtensionPoints(namespace);
		ci.println("Extension point(s):"); //$NON-NLS-1$
		ci.println("-------------------"); //$NON-NLS-1$
		for (int i = 0; i < extpts.length; i++)
			displayExtensionPoint(extpts[i], ci);

		if (verbose) {
			ci.println("\nExtension(s):"); //$NON-NLS-1$
			ci.println("-------------------"); //$NON-NLS-1$
			IExtension[] exts = RegistryFactory.getRegistry().getExtensions(namespace);
			for (int j = 0; j < exts.length; j++)
				displayExtension(exts[j], ci, true /*full*/);
		}
	}

	public void _pt(CommandInterpreter ci) throws Exception {
		String extensionPointId = getArgument(ci);
		if (extensionPointId == null)
			return;
		IExtensionPoint extpt = RegistryFactory.getRegistry().getExtensionPoint(extensionPointId);
		if (extpt == null)
			return;
		ci.print("Extension point: "); //$NON-NLS-1$
		displayExtensionPoint(extpt, ci);
		IExtension[] exts = extpt.getExtensions();
		ci.println("\nExtension(s):"); //$NON-NLS-1$
		ci.println("-------------------"); //$NON-NLS-1$
		for (int i = 0; i < exts.length; i++) {
			displayExtension(exts[i], ci, false /*short*/);
			if (verbose) {
				IConfigurationElement[] ce = exts[i].getConfigurationElements();
				for (int j = 0; j < ce.length; j++)
					displayConfigElement(ci, ce[j], 1);
				ci.println();
			}
		}
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

	// This method has a side effect of setting the verbose flag
	private String getArgument(CommandInterpreter ci) {
		String firstParm = ci.nextArgument();
		if ("-v".equals(firstParm)) { //$NON-NLS-1$
			verbose = true;
			return ci.nextArgument();
		}

		verbose = false;
		return firstParm;
	}

	private void displayExtensionPoint(IExtensionPoint extentionPoint, CommandInterpreter ci) {
		if (extentionPoint == null)
			return;
		ci.println(extentionPoint.getUniqueIdentifier() + " [from " + extentionPoint.getContributor().getName() + ']'); //$NON-NLS-1$
	}

	private void displayExtension(IExtension extention, CommandInterpreter ci, boolean full) {
		if (extention == null)
			return;
		if (full) {
			ci.print("Id: " + extention.getUniqueIdentifier()); //$NON-NLS-1$
			ci.print(" PointId: " + extention.getExtensionPointUniqueIdentifier()); //$NON-NLS-1$
			ci.println(" [from " + extention.getContributor().getName() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			ci.println(extention.getUniqueIdentifier() + " [from " + extention.getContributor().getName() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void displayConfigElement(CommandInterpreter ci, IConfigurationElement ce, int level) throws Exception {
		String spacing = spacing(ci, level);
		ci.println(spacing + '<' + ce.getName() + '>');
		String[] attrs = ce.getAttributeNames();
		for (int k = 0; k < attrs.length; k++)
			ci.println(indent + spacing + attrs[k] + " = " + ce.getAttribute(attrs[k])); //$NON-NLS-1$
		String value = ce.getValue();
		if (value != null)
			ci.println(indent + spacing + value);
		IConfigurationElement[] children = ce.getChildren();
		for (int z = 0; z < children.length; z++)
			displayConfigElement(ci, children[z], level + 1);
		ci.println(spacing + "</" + ce.getName() + '>'); //$NON-NLS-1$
	}

	private String spacing(CommandInterpreter ci, int level) {
		StringBuffer b = new StringBuffer();
		for (int i = 0; i < level; i++)
			b.append(indent);
		return b.toString();
	}
}
