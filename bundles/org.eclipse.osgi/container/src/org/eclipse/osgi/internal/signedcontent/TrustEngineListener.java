/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.internal.signedcontent;

import java.security.cert.Certificate;
import java.util.*;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.provisional.service.security.AuthorizationEngine;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.osgi.framework.*;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class TrustEngineListener {
	// this is a singleton listener; see SignedBundleHook for initialization
	private volatile static TrustEngineListener instance;
	private final BundleContext context;
	private final ServiceTracker<AuthorizationEngine, AuthorizationEngine> authorizationTracker;

	TrustEngineListener(BundleContext context) {
		this.context = context;
		// read the trust provider security property
		String authEngineProp = FrameworkProperties.getProperty(SignedContentConstants.AUTHORIZATION_ENGINE);
		Filter filter = null;
		if (authEngineProp != null)
			try {
				filter = FilterImpl.newInstance("(&(" + Constants.OBJECTCLASS + "=" + AuthorizationEngine.class.getName() + ")(" + SignedContentConstants.AUTHORIZATION_ENGINE + "=" + authEngineProp + "))"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$//$NON-NLS-5$
			} catch (InvalidSyntaxException e) {
				SignedBundleHook.log("Invalid authorization filter", FrameworkLogEntry.WARNING, e); //$NON-NLS-1$
			}
		if (filter != null)
			authorizationTracker = new ServiceTracker<AuthorizationEngine, AuthorizationEngine>(context, filter, null);
		else
			authorizationTracker = new ServiceTracker<AuthorizationEngine, AuthorizationEngine>(context, AuthorizationEngine.class.getName(), null);
		authorizationTracker.open();
		instance = this;
	}

	public static TrustEngineListener getInstance() {
		return instance;
	}

	void stopTrustEngineListener() {
		authorizationTracker.close();
		instance = null;
	}

	public void addedTrustAnchor(Certificate anchor) {
		// find any SignedContent with SignerInfos that do not have an anchor;
		// re-evaluate trust and check authorization for these SignedContents
		Bundle[] bundles = context.getBundles();
		Set<Bundle> unresolved = new HashSet<Bundle>();
		for (int i = 0; i < bundles.length; i++) {
			SignedContentImpl signedContent = getSignedContent(bundles[i]);
			if (signedContent != null && signedContent.isSigned()) {
				// check the SignerInfos for this content
				SignerInfo[] infos = signedContent.getSignerInfos();
				for (int j = 0; j < infos.length; j++) {
					if (infos[j].getTrustAnchor() == null)
						// one of the signers is not trusted
						unresolved.add(bundles[i]);
					SignerInfo tsa = signedContent.getTSASignerInfo(infos[j]);
					if (tsa != null && tsa.getTrustAnchor() == null)
						// one of the tsa signers is not trusted
						unresolved.add(bundles[i]);
				}
			}
			if (unresolved.contains(bundles[i])) {
				// found an untrusted signer for this bundle re-evaluate trust
				SignedBundleFile.determineTrust(signedContent, SignedBundleHook.VERIFY_TRUST);
				// now check the authorization handler
				checkAuthorization(signedContent, bundles[i]);
			}
		}
		// try to resolve
		if (unresolved.size() > 0)
			resolveBundles(unresolved.toArray(new Bundle[unresolved.size()]), false);
	}

	private void checkAuthorization(SignedContentImpl signedContent, Bundle bundle) {
		AuthorizationEngine authEngine = getAuthorizationEngine();
		if (authEngine != null)
			authEngine.authorize(signedContent, bundle);
	}

	AuthorizationEngine getAuthorizationEngine() {
		return authorizationTracker.getService();
	}

	private void resolveBundles(Bundle[] bundles, boolean refresh) {
		ServiceReference<?> ref = context.getServiceReference(PackageAdmin.class.getName());
		if (ref == null)
			return;
		PackageAdmin pa = (PackageAdmin) context.getService(ref);
		if (pa == null)
			return;
		try {
			if (refresh)
				pa.refreshPackages(bundles);
			else
				pa.resolveBundles(bundles);
		} finally {
			context.ungetService(ref);
		}
	}

	public void removedTrustAnchor(Certificate anchor) {
		// find any signed content that has signerinfos with the supplied anchor
		// re-evaluate trust and check authorization again.
		Bundle[] bundles = context.getBundles();
		Set<Bundle> usingAnchor = new HashSet<Bundle>();
		Set<SignerInfo> untrustedSigners = new HashSet<SignerInfo>();
		for (int i = 0; i < bundles.length; i++) {
			SignedContentImpl signedContent = getSignedContent(bundles[i]);
			if (signedContent != null && signedContent.isSigned()) {
				// check signer infos for this content
				SignerInfo[] infos = signedContent.getSignerInfos();
				for (int j = 0; j < infos.length; j++) {
					if (anchor.equals(infos[j].getTrustAnchor())) {
						// one of the signers uses this anchor
						untrustedSigners.add(infos[j]);
						usingAnchor.add(bundles[i]);
					}
					SignerInfo tsa = signedContent.getTSASignerInfo(infos[j]);
					if (tsa != null && anchor.equals(tsa.getTrustAnchor())) {
						// one of the tsa signers uses this anchor
						usingAnchor.add(bundles[i]);
						untrustedSigners.add(tsa);
					}
				}
			}
		}
		// remove trust anchors from untrusted signers
		for (Iterator<SignerInfo> untrusted = untrustedSigners.iterator(); untrusted.hasNext();)
			((SignerInfoImpl) untrusted.next()).setTrustAnchor(null);
		// re-establish trust and check authorization
		for (Iterator<Bundle> untrustedBundles = usingAnchor.iterator(); untrustedBundles.hasNext();) {
			Bundle bundle = untrustedBundles.next();
			SignedContentImpl signedContent = getSignedContent(bundle);
			// found an signer using the anchor for this bundle re-evaluate trust
			SignedBundleFile.determineTrust(signedContent, SignedBundleHook.VERIFY_TRUST);
			// now check the authorization handler
			checkAuthorization(signedContent, bundle);
		}
		// TODO an optimization here would be to check for real DisabledInfo objects for each bundle
		// try to refresh
		if (usingAnchor.size() > 0)
			resolveBundles(usingAnchor.toArray(new Bundle[usingAnchor.size()]), true);
	}

	private SignedContentImpl getSignedContent(Bundle bundle) {
		BaseData data = (BaseData) ((AbstractBundle) bundle).getBundleData();
		SignedStorageHook hook = (SignedStorageHook) data.getStorageHook(SignedStorageHook.KEY);
		if (hook == null)
			return null;
		return (SignedContentImpl) hook.getSignedContent();
	}
}
