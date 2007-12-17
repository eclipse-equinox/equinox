/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.service.security;

import java.security.cert.CertificateException;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.internal.baseadaptor.DevClassPathHelper;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.service.security.AuthorizationEngine;
import org.eclipse.osgi.service.security.AuthorizationEvent;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class DefaultAuthorizationEngine extends AuthorizationEngine {

	private final State systemState;

	public DefaultAuthorizationEngine(BundleContext context, State systemState) {
		super(context);
		this.systemState = systemState;
	}

	private static final int ENFORCE_NONE = 0x0000;
	private static final int ENFORCE_SIGNED = 0x0001;
	private static final int ENFORCE_TRUSTED = 0x0002;
	private static final int ENFORCE_VALIDITY = 0x0004;

	private static final String STR_ENFORCE_NONE = "any"; //$NON-NLS-1$
	private static final String STR_ENFORCE_SIGNED = "signed"; //$NON-NLS-1$
	private static final String STR_ENFORCE_TRUSTED = "trusted"; //$NON-NLS-1$
	private static final String STR_ENFORCE_VALIDITY = "validity"; //$NON-NLS-1$

	private static final String POLICY_NAME = "org.eclipse.equinox.security"; //$NON-NLS-1$
	private static final String POLICY = "osgi.signedcontent.authorization.engine.policy"; //$NON-NLS-1$
	private static int enforceFlags = 0;

	static {
		String policy = FrameworkProperties.getProperty(POLICY);
		if (policy == null || STR_ENFORCE_NONE.equals(policy))
			enforceFlags = ENFORCE_NONE;
		else if (STR_ENFORCE_TRUSTED.equals(policy))
			enforceFlags = ENFORCE_TRUSTED | ENFORCE_SIGNED;
		else if (STR_ENFORCE_SIGNED.equals(policy))
			enforceFlags = ENFORCE_SIGNED;
		else if (STR_ENFORCE_VALIDITY.equals(policy))
			enforceFlags = ENFORCE_TRUSTED | ENFORCE_SIGNED | ENFORCE_VALIDITY;
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

	protected int doGetSeverity() {
		return 0;
	}
}
