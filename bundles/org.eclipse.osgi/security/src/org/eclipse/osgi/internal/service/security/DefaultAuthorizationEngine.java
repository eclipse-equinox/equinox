/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.service.security;

import java.io.*;
import java.security.cert.CertificateException;
import java.util.Properties;
import org.eclipse.core.runtime.adaptor.LocationManager;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.baseadaptor.DevClassPathHelper;
import org.eclipse.osgi.internal.provisional.service.security.*;
import org.eclipse.osgi.internal.signedcontent.SignedBundleHook;
import org.eclipse.osgi.internal.signedcontent.SignedStorageHook;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.osgi.framework.*;

public class DefaultAuthorizationEngine extends AuthorizationEngine {

	private static final String VERSION_PROP = "Version"; //$NON-NLS-1$
	private static final String VERSION_NUM = "1.0"; //$NON-NLS-1$
	private static final Version VERSION_MAX = new Version(2, 0, 0);

	private final State systemState;
	private final BundleContext bundleContext;

	public static final int ENFORCE_NONE = 0x0000;
	public static final int ENFORCE_SIGNED = 0x0001;
	public static final int ENFORCE_TRUSTED = 0x0002;
	public static final int ENFORCE_VALIDITY = 0x0004;

	private static final String STR_ENFORCE_NONE = "any"; //$NON-NLS-1$
	private static final String STR_ENFORCE_SIGNED = "signed"; //$NON-NLS-1$
	private static final String STR_ENFORCE_TRUSTED = "trusted"; //$NON-NLS-1$
	private static final String STR_ENFORCE_VALIDITY = "validity"; //$NON-NLS-1$

	private static final String POLICY_NAME = "org.eclipse.equinox.security"; //$NON-NLS-1$
	private static final String POLICY_PROP = "osgi.signedcontent.authorization.engine.policy"; //$NON-NLS-1$
	private static final String FILE_LOAD_POLICY = ".loadpolicy"; //$NON-NLS-1$
	private static int enforceFlags = 0;

	private static final File policyFile;
	static {
		File osgiFile = LocationManager.getOSGiConfigurationDir();
		policyFile = new File(osgiFile.getPath() + File.separatorChar + FILE_LOAD_POLICY);

		Properties properties = null;
		// load the policy file, if not exist, create it and load it
		if (policyFile.exists()) {
			try {
				properties = new Properties();
				properties.load(new FileInputStream(policyFile));
			} catch (IOException e) {
				SignedBundleHook.log("Error loading policy file", FrameworkLogEntry.ERROR, e); //$NON-NLS-1$
			}
		}

		if (properties != null) {
			Version version = new Version(0, 0, 0);
			String versionProp = properties.getProperty(VERSION_PROP);
			if (versionProp != null)
				try {
					version = new Version(versionProp);
				} catch (IllegalArgumentException e) {
					// do nothing;
				}
			if (VERSION_MAX.compareTo(version) > 0) {
				String policy = properties.getProperty(POLICY_PROP);
				if (policy != null)
					try {
						enforceFlags = Integer.parseInt(policy);
					} catch (NumberFormatException e) {
						// do nothing;
					}
			}
		} else {
			String policy = FrameworkProperties.getProperty(POLICY_PROP);
			if (policy == null || STR_ENFORCE_NONE.equals(policy))
				enforceFlags = ENFORCE_NONE;
			else if (STR_ENFORCE_TRUSTED.equals(policy))
				enforceFlags = ENFORCE_TRUSTED | ENFORCE_SIGNED;
			else if (STR_ENFORCE_SIGNED.equals(policy))
				enforceFlags = ENFORCE_SIGNED;
			else if (STR_ENFORCE_VALIDITY.equals(policy))
				enforceFlags = ENFORCE_TRUSTED | ENFORCE_SIGNED | ENFORCE_VALIDITY;
		}

	}

	public DefaultAuthorizationEngine(BundleContext context, State systemState) {
		super(context);
		this.bundleContext = context;
		this.systemState = systemState;
	}

	protected AuthorizationEvent doAuthorize(SignedContent content, Object context) {
		boolean enabled = isEnabled(content, context);
		AuthorizationEvent event = null;
		if (context instanceof Bundle) {
			BundleDescription desc = systemState.getBundle(((Bundle) context).getBundleId());
			if (!enabled) {
				DisabledInfo info = new DisabledInfo(POLICY_NAME, null, desc); // TODO add an error message
				systemState.addDisabledInfo(info);
				event = new AuthorizationEvent(AuthorizationEvent.DENIED, content, context, 0); // TODO severity??
			} else {
				DisabledInfo info = systemState.getDisabledInfo(desc, POLICY_NAME);
				if (info != null) {
					systemState.removeDisabledInfo(info);
				}
				event = new AuthorizationEvent(AuthorizationEvent.ALLOWED, content, context, 0);
			}
		}
		return event;
	}

	private boolean isEnabled(SignedContent content, Object context) {
		if (context instanceof Bundle && DevClassPathHelper.inDevelopmentMode()) {
			String[] devClassPath = DevClassPathHelper.getDevClassPath(((Bundle) context).getSymbolicName());
			if (devClassPath != null && devClassPath.length > 0)
				return true; // always enabled bundles from workspace; they never are signed
		}
		if ((0 != (enforceFlags & ENFORCE_SIGNED)) && ((content == null) || !content.isSigned()))
			return false;

		SignerInfo[] signerInfos = content == null ? new SignerInfo[0] : content.getSignerInfos();
		for (int i = 0; i < signerInfos.length; i++) {
			if ((0 != (enforceFlags & ENFORCE_TRUSTED)) && !signerInfos[i].isTrusted())
				return false;
			if ((0 != (enforceFlags & ENFORCE_VALIDITY)))
				try {
					content.checkValidity(signerInfos[i]);
				} catch (CertificateException e) {
					return false;
				}
		}
		return true;
	}

	public int getStatus() {
		if (0 != systemState.getDisabledBundles().length) {
			return AuthorizationStatus.ERROR;
		}
		return AuthorizationStatus.OK;
	}

	public void processInstalledBundles() {
		Bundle[] bundles = bundleContext.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			BaseData baseData = (BaseData) ((AbstractBundle) bundles[i]).getBundleData();
			SignedStorageHook hook = (SignedStorageHook) baseData.getStorageHook(SignedStorageHook.KEY);
			SignedContent signedContent = hook != null ? hook.getSignedContent() : null;
			authorize(signedContent, bundles[i]);
		}
	}

	public void setLoadPolicy(int policy) {
		if ((policy | ENFORCE_SIGNED | ENFORCE_TRUSTED | ENFORCE_VALIDITY) != (ENFORCE_SIGNED | ENFORCE_TRUSTED | ENFORCE_VALIDITY))
			throw new IllegalArgumentException("Invalid policy: " + policy); //$NON-NLS-1$
		enforceFlags = policy;
		Properties properties = new Properties();
		properties.setProperty(POLICY_PROP, Integer.toString(policy));
		properties.setProperty(VERSION_PROP, VERSION_NUM); // need to act different when we have different versions
		try {
			properties.store(new FileOutputStream(policyFile), null);
		} catch (IOException e) {
			SignedBundleHook.log("Error saving load policy file", FrameworkLogEntry.ERROR, e); //$NON-NLS-1$
		}
	}

	public int getLoadPolicy() {
		return enforceFlags;
	}

}
