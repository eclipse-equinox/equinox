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
package org.eclipse.osgi.internal.signedcontent;

import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.framework.internal.core.AbstractBundle;
import org.eclipse.osgi.internal.provisional.service.security.AuthorizationEngine;
import org.eclipse.osgi.signedcontent.SignedContent;
import org.osgi.framework.*;

public class BundleInstallListener implements SynchronousBundleListener {

	public void bundleChanged(BundleEvent event) {
		Bundle bundle = event.getBundle();
		switch (event.getType()) {
			case BundleEvent.UPDATED :
				// fall through to INSTALLED
			case BundleEvent.INSTALLED :
				TrustEngineListener listener = TrustEngineListener.getInstance();
				AuthorizationEngine authEngine = listener == null ? null : listener.getAuthorizationEngine();
				if (authEngine != null) {
					BaseData baseData = (BaseData) ((AbstractBundle) bundle).getBundleData();
					SignedStorageHook hook = (SignedStorageHook) baseData.getStorageHook(SignedStorageHook.KEY);
					SignedContent signedContent = hook != null ? hook.signedContent : null;
					authEngine.authorize(signedContent, bundle);
				}
				break;
			default :
				break;
		}
	}
}
