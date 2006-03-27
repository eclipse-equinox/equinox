/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.preferences;

import java.util.Properties;

/**
 * A product can customize preferences by implementing this service.
 * 
 * @since org.eclipse.equinox.preferences 3.2
 */
public interface IProductPreferencesService {
	/**
	 * @return default preferences specified by the product. 
	 */
	public Properties getProductCustomization();

	/**
	 * @return translation table for default preferences
	 */
	public Properties getProductTranslation();
}
