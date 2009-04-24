/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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

	private static final String POLICY_CONSOLE = "org.eclipse.osgi.framework.console"; //$NON-NLS-1$
	private PlatformAdmin platformAdmin;
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
		help.append("\tenableBundle - " + EclipseAdaptorMsg.ECLIPSE_CONSOLE_HELP_ENABLE_COMMAND_DESCRIPTION);//$NON-NLS-1$
		help.append(EclipseAdaptorMsg.NEW_LINE);
		help.append("\tdisableBundle - " + EclipseAdaptorMsg.ECLIPSE_CONSOLE_HELP_DISABLE_COMMAND_DESCRIPTION);//$NON-NLS-1$
		help.append(EclipseAdaptorMsg.NEW_LINE);
		help.append("\tdisabledBundles - " + EclipseAdaptorMsg.ECLIPSE_CONSOLE_HELP_LD_COMMAND_DESCRIPTION);//$NON-NLS-1$
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

	private PlatformAdmin getPlatformAdmin(CommandInterpreter ci) {
		if (platformAdmin == null) {
			ServiceReference platformAdminRef = context.getServiceReference(PlatformAdmin.class.getName());
			if (platformAdminRef == null) {
				ci.print("  "); //$NON-NLS-1$
				ci.println(EclipseAdaptorMsg.ECLIPSE_CONSOLE_NO_CONSTRAINTS_NO_PLATFORM_ADMIN_MESSAGE);
				return null;
			}
			platformAdmin = (PlatformAdmin) context.getService(platformAdminRef);
		}
		return platformAdmin;
	}

	private void ungetPlatformAdmin() {
		ServiceReference platformAdminRef = context.getServiceReference(PlatformAdmin.class.getName());
		context.ungetService(platformAdminRef);
	}

	public void _diag(CommandInterpreter ci) throws Exception {
		String nextArg = ci.nextArgument();
		if (nextArg == null) {
			ci.println(EclipseAdaptorMsg.ECLIPSE_CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}
		try {
			State systemState = getPlatformAdmin(ci).getState(false);
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

				if (unsatisfied.length == 0 && resolverErrors.length == 0) {
					ci.print("  "); //$NON-NLS-1$
					ci.println(EclipseAdaptorMsg.ECLIPSE_CONSOLE_NO_CONSTRAINTS);
				}
				if (unsatisfied.length > 0) {
					ci.print("  "); //$NON-NLS-1$
					ci.println(EclipseAdaptorMsg.ECLIPSE_CONSOLE_DIRECT_CONSTRAINTS);
				}
				for (int i = 0; i < unsatisfied.length; i++) {
					ci.print("    "); //$NON-NLS-1$
					ci.println(MessageHelper.getResolutionFailureMessage(unsatisfied[i]));
				}
				VersionConstraint[] unsatisfiedLeaves = platformAdmin.getStateHelper().getUnsatisfiedLeaves(new BundleDescription[] {bundle});
				boolean foundLeaf = false;
				for (int i = 0; i < unsatisfiedLeaves.length; i++) {
					if (unsatisfiedLeaves[i].getBundle() == bundle)
						continue;
					if (!foundLeaf) {
						foundLeaf = true;
						ci.print("  "); //$NON-NLS-1$
						ci.println(EclipseAdaptorMsg.ECLIPSE_CONSOLE_LEAF_CONSTRAINTS);
					}
					ci.print("    "); //$NON-NLS-1$
					ci.println(unsatisfiedLeaves[i].getBundle().getLocation() + " [" + unsatisfiedLeaves[i].getBundle().getBundleId() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
					ci.print("      "); //$NON-NLS-1$
					ci.println(MessageHelper.getResolutionFailureMessage(unsatisfiedLeaves[i]));
				}
				nextArg = ci.nextArgument();
			}
		} finally {
			ungetPlatformAdmin();
		}
	}

	public void _enableBundle(CommandInterpreter ci) throws Exception {
		String nextArg = ci.nextArgument();
		if (nextArg == null) {
			ci.println(EclipseAdaptorMsg.ECLIPSE_CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}
		try {
			State systemState = getPlatformAdmin(ci).getState(false);
			while (nextArg != null) {
				BundleDescription bundleDesc = getBundleDescriptionFromToken(systemState, nextArg);
				if (bundleDesc == null) {
					ci.println(NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONSOLE_CANNOT_FIND_BUNDLE_ERROR, nextArg));
					nextArg = ci.nextArgument();
					continue;
				}
				DisabledInfo[] infos = systemState.getDisabledInfos(bundleDesc);
				for (int i = 0; i < infos.length; i++) {
					getPlatformAdmin(ci).removeDisabledInfo(infos[i]);
				}
				nextArg = ci.nextArgument();
			}
		} finally {
			ungetPlatformAdmin();
		}
	}

	public void _disableBundle(CommandInterpreter ci) throws Exception {
		String nextArg = ci.nextArgument();
		if (nextArg == null) {
			ci.println(EclipseAdaptorMsg.ECLIPSE_CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}
		try {
			State systemState = getPlatformAdmin(ci).getState(false);
			while (nextArg != null) {
				BundleDescription bundleDesc = getBundleDescriptionFromToken(systemState, nextArg);
				if (bundleDesc == null) {
					ci.println(NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONSOLE_CANNOT_FIND_BUNDLE_ERROR, nextArg));
					nextArg = ci.nextArgument();
					continue;
				}
				DisabledInfo info = new DisabledInfo(POLICY_CONSOLE, EclipseAdaptorMsg.ECLIPSE_CONSOLE_BUNDLE_DISABLED_MESSAGE, bundleDesc);
				getPlatformAdmin(ci).addDisabledInfo(info);
				nextArg = ci.nextArgument();
			}
		} finally {
			ungetPlatformAdmin();
		}
	}

	public void _disabledBundles(CommandInterpreter ci) throws Exception {

		try {
			State systemState = getPlatformAdmin(ci).getState(false);
			BundleDescription[] disabledBundles = systemState.getDisabledBundles();

			ci.println(NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONSOLE_DISABLED_COUNT_MESSAGE, String.valueOf(disabledBundles.length)));

			if (disabledBundles.length > 0) {
				ci.println();
			}
			for (int i = 0; i < disabledBundles.length; i++) {
				DisabledInfo[] disabledInfos = systemState.getDisabledInfos(disabledBundles[i]);

				ci.println(NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONSOLE_DISABLED_BUNDLE_HEADER, formatBundleName(disabledBundles[i]), String.valueOf(disabledBundles[i].getBundleId())));
				ci.print(NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONSOLE_DISABLED_BUNDLE_REASON1, disabledInfos[0].getMessage(), disabledInfos[0].getPolicyName()));

				for (int j = 1; j < disabledInfos.length; j++) {
					ci.print(NLS.bind(EclipseAdaptorMsg.ECLIPSE_CONSOLE_DISABLED_BUNDLE_REASON1, disabledInfos[j].getMessage(), String.valueOf(disabledInfos[j].getPolicyName())));
				}

				ci.println();
			}
		} finally {
			ungetPlatformAdmin();
		}
	}

	private String formatBundleName(BundleDescription b) {
		String label = b.getSymbolicName();
		if (label == null || label.length() == 0)
			label = b.toString();
		else
			label = label + "_" + b.getVersion(); //$NON-NLS-1$

		return label;
	}
}
