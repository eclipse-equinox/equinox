/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.internal.signedcontent;

import java.security.cert.Certificate;
import java.util.*;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.internal.signedcontent.SignedStorageHook.StorageHookImpl;
import org.eclipse.osgi.signedcontent.SignerInfo;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class TrustEngineListener {
	private final BundleContext context;
	private final SignedBundleHook signedBundleHook;

	TrustEngineListener(BundleContext context, SignedBundleHook signedBundleHook) {
		this.context = context;
		this.signedBundleHook = signedBundleHook;
	}

	public void addedTrustAnchor(Certificate anchor) {
		// find any SignedContent with SignerInfos that do not have an anchor;
		// re-evaluate trust and check authorization for these SignedContents
		Bundle[] bundles = context.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			SignedContentImpl signedContent = getSignedContent(bundles[i]);
			if (signedContent != null && signedContent.isSigned()) {
				// check the SignerInfos for this content
				SignerInfo[] infos = signedContent.getSignerInfos();
				for (int j = 0; j < infos.length; j++) {
					if (infos[j].getTrustAnchor() == null) {
						// one of the signers is not trusted
						signedBundleHook.determineTrust(signedContent, SignedBundleHook.VERIFY_TRUST);
					} else {
						SignerInfo tsa = signedContent.getTSASignerInfo(infos[j]);
						if (tsa != null && tsa.getTrustAnchor() == null)
							// one of the tsa signers is not trusted
							signedBundleHook.determineTrust(signedContent, SignedBundleHook.VERIFY_TRUST);
					}
				}
			}
		}
	}

	public void removedTrustAnchor(Certificate anchor) {
		// find any signed content that has signerinfos with the supplied anchor
		// re-evaluate trust and check authorization again.
		Bundle[] bundles = context.getBundles();
		Set<Bundle> usingAnchor = new HashSet<>();
		Set<SignerInfo> untrustedSigners = new HashSet<>();
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
		// re-establish trust
		for (Iterator<Bundle> untrustedBundles = usingAnchor.iterator(); untrustedBundles.hasNext();) {
			Bundle bundle = untrustedBundles.next();
			SignedContentImpl signedContent = getSignedContent(bundle);
			// found an signer using the anchor for this bundle re-evaluate trust
			signedBundleHook.determineTrust(signedContent, SignedBundleHook.VERIFY_TRUST);
		}
	}

	private SignedContentImpl getSignedContent(Bundle bundle) {
		Generation generation = (Generation) ((EquinoxBundle) bundle).getModule().getCurrentRevision().getRevisionInfo();
		StorageHookImpl hook = generation.getStorageHook(SignedStorageHook.class);
		return hook != null ? hook.signedContent : null;
	}
}
