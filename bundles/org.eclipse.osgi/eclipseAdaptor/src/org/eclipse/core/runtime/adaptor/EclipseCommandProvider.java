/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class EclipseCommandProvider implements CommandProvider {
	private BundleContext context;
	public EclipseCommandProvider(BundleContext context) {
		this.context = context;
	}
	public String getHelp() {
		StringBuffer help = new StringBuffer(512);
		help.append(EclipseAdaptorMsg.NEW_LINE);
		help.append("---");
		help.append(EclipseAdaptorMsg.formatter.getString("ECLIPSE_CONSOLE_COMMANDS_HEADER"));
		help.append("---");
		help.append(EclipseAdaptorMsg.NEW_LINE);
		help.append("\tdiag - " + EclipseAdaptorMsg.formatter.getString("ECLIPSE_CONSOLE_HELP_DIAG_COMMAND_DESCRIPTION"));
		return help.toString();
	}
	private BundleDescription getBundleDescriptionFromToken(State state, String token) {
		try {
			long id = Long.parseLong(token);
			return state.getBundle(id);
		} catch (NumberFormatException nfe) {
			BundleDescription[] allBundles = state.getBundles(token);
			if (allBundles.length > 0)
				return allBundles[0];
		}
		return null;
	}
	public void _diag(CommandInterpreter ci) throws Exception {
		String nextArg = ci.nextArgument();
		if (nextArg == null) {
			ci.println(EclipseAdaptorMsg.formatter.getString("ECLIPSE_CONSOLE_NO_BUNDLE_SPECIFIED_ERROR"));
			return;
		}
		ServiceReference platformAdminRef = context.getServiceReference("org.eclipse.osgi.service.resolver.PlatformAdmin");
		if (platformAdminRef == null) {
			ci.print("  ");
			ci.println(EclipseAdaptorMsg.formatter.getString("ECLIPSE_CONSOLE_NO_CONSTRAINTS_NO_PLATFORM_ADMIN_MESSAGE"));
			return;
		}
		try {
			PlatformAdmin platformAdmin = (PlatformAdmin) context.getService(platformAdminRef);
			if (platformAdmin == null)
				return;
			State systemState = platformAdmin.getState(false);
			while (nextArg != null) {
				BundleDescription bundle = getBundleDescriptionFromToken(systemState, nextArg);
				if (bundle == null) {
					ci.println(EclipseAdaptorMsg.formatter.getString("ECLIPSE_CONSOLE_CANNOT_FIND_BUNDLE_ERROR", nextArg));
					nextArg = ci.nextArgument();
					continue;
				}
				ci.println(bundle.getLocation() + " [" + bundle.getBundleId() + "]");
				VersionConstraint[] unsatisfied = platformAdmin.getStateHelper().getUnsatisfiedConstraints(bundle);
				if (unsatisfied.length == 0) {
					ci.print("  ");
					ci.println(EclipseAdaptorMsg.formatter.getString("ECLIPSE_CONSOLE_NO_CONSTRAINTS"));
				}
				for (int i = 0; i < unsatisfied.length; i++) {
					ci.print("  ");
					ci.println(EclipseAdaptorMsg.getResolutionFailureMessage(unsatisfied[i]));
				}
				nextArg = ci.nextArgument();
			}
		} finally {
			context.ungetService(platformAdminRef);
		}
	}
}
