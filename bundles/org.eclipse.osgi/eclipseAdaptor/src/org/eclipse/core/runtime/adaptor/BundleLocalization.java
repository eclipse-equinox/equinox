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
import java.util.ResourceBundle;
import org.osgi.framework.Bundle;

/**
 * The implementation of the service that gets ResourceBundle objects from a given 
 * bundle with a given locale. 
 */

public class BundleLocalization implements org.eclipse.osgi.service.localization.BundleLocalization {
	/**
	 * The getLocalization method gets a ResourceBundle object for the given
	 * locale and bundle.
	 * 
	 * 
	 * @return A <code>ResourceBundle</code> object for the given bundle and locale.
	 * If null is passed for the locale parameter, the default locale is used.
	 */
	public ResourceBundle getLocalization(Bundle bundle, String locale) {
		return ((org.eclipse.osgi.framework.internal.core.Bundle) (bundle)).getResourceBundle(locale);
	}
}