/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.storage;

import java.util.ResourceBundle;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.service.localization.BundleLocalization;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.osgi.framework.Bundle;

/**
 * The implementation of the service that gets ResourceBundle objects from a
 * given bundle with a given locale.
 *
 * <p>
 * Internal class.
 * </p>
 */

public class BundleLocalizationImpl implements BundleLocalization {
	/**
	 * The getLocalization method gets a ResourceBundle object for the given locale
	 * and bundle.
	 *
	 * @return A <code>ResourceBundle</code> object for the given bundle and locale.
	 *         If null is passed for the locale parameter, the default locale is
	 *         used.
	 */
	@Override
	public ResourceBundle getLocalization(Bundle bundle, String locale) {
		Module m = ((EquinoxBundle) bundle).getModule();
		ModuleRevision r = m.getCurrentRevision();
		Generation g = (Generation) r.getRevisionInfo();
		return g.getResourceBundle(locale);
	}
}
