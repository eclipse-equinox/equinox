/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.core.internal.preferences.exchange;

import java.util.Properties;

/**
 * A product can customize preferences by implementing this service.
 *
 * This interface is likely going to change as the application model is
 * introduced in which point it might be kept for backward compatibility or
 * removed entirely.
 *
 * @since org.eclipse.equinox.common 3.2
 */
public interface IProductPreferencesService {

	/**
	 * Returns properties specified in the product customization file.
	 *
	 * @return default preferences specified by the product.
	 */
	public Properties getProductCustomization();

	/**
	 * Returns translations for the customized properties.
	 *
	 * @return translation table for default preferences
	 */
	public Properties getProductTranslation();
}
