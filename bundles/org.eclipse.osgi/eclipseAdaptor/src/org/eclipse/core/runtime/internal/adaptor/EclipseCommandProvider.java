/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.internal.adaptor;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Internal class.
 */
public class EclipseCommandProvider implements CommandProvider {
	private BundleContext context;

	public EclipseCommandProvider(BundleContext context) {
		this.context = context;
	}

	public String getHelp() {
		StringBuffer help = new StringBuffer(512);
		help.append("---"); //$NON-NLS-1$
		help.append(EclipseAdaptorMsg.ECLIPSE_CONSOLE_COMMANDS_HEADER);
		help.append("---"); //$NON-NLS-1$
		help.append(EclipseAdaptorMsg.NEW_LINE);
		help.append("\tdiag - " + EclipseAdaptorMsg.ECLIPSE_CONSOLE_HELP_DIAG_COMMAND_DESCRIPTION);//$NON-NLS-1$
		help.append(EclipseAdaptorMsg.NEW_LINE);
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
			ci.println(EclipseAdaptorMsg.ECLIPSE_CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}
		ServiceReference platformAdminRef = context.getServiceReference(PlatformAdmin.class.getName());
		if (platformAdminRef == null) {
			ci.print("  "); //$NON-NLS-1$
			ci.println(EclipseAdaptorMsg.ECLIPSE_CONSOLE_NO_CONSTRAINTS_NO_PLATFORM_ADMIN_MESSAGE);
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
					ci.println(NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONSOLE_CANNOT_FIND_BUNDLE_ERROR, nextArg));
					nextArg = ci.nextArgument();
					continue;
				}
				ci.println(bundle.getLocation() + " [" + bundle.getBundleId() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				VersionConstraint[] unsatisfied = platformAdmin.getStateHelper().getUnsatisfiedConstraints(bundle);
				ResolverError[] resolverErrors = platformAdmin.getState(false).getResolverErrors(bundle);
				for (int i = 0; i < resolverErrors.length; i++) {
					if ((resolverErrors[i].getType() & (ResolverError.MISSING_FRAGMENT_HOST | ResolverError.MISSING_GENERIC_CAPABILITY | ResolverError.MISSING_IMPORT_PACKAGE | ResolverError.MISSING_REQUIRE_BUNDLE)) != 0)
						continue;
					ci.print("  "); //$NON-NLS-1$
					ci.println(resolverErrors[i].toString());
				}

				for (int i = 0; i < unsatisfied.length; i++) {
					ci.print("  "); //$NON-NLS-1$
					ci.println(MessageHelper.getResolutionFailureMessage(unsatisfied[i]));
				}
				if (unsatisfied.length == 0 && resolverErrors.length == 0) {
					ci.print("  "); //$NON-NLS-1$
					ci.println(EclipseAdaptorMsg.ECLIPSE_CONSOLE_NO_CONSTRAINTS);
				}
				nextArg = ci.nextArgument();
			}
		} finally {
			context.ungetService(platformAdminRef);
		}
	}
}
