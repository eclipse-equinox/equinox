/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.localization;

import java.util.ResourceBundle;
import org.osgi.framework.Bundle;

/**
 * The interface of the service that gets {@link ResourceBundle} objects from a given 
 * bundle with a given locale. 
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface BundleLocalization {

	/**
	 * Returns a <code>ResourceBundle</code> object for the given bundle and locale.
	 * @param bundle the bundle to get localization for
	 * @param locale the name of the locale to get, or <code>null</code> if
	 * the default locale is to be used
	 * 
	 * @return A <code>ResourceBundle</code> object for the given bundle and locale,
	 * or <code>null</code> is returned if no ResourceBundle object can
	 * be loaded.
	 */
	public ResourceBundle getLocalization(Bundle bundle, String locale);
}
