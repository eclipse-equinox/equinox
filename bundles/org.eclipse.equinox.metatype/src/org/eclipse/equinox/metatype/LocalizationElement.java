/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class LocalizationElement {

	public static final char KEY_SIGN = '%';
	String _localization = null;
	ResourceBundle _rb;

	/**
	 * Internal method
	 */
	void setResourceBundle(ResourceBundle rb) {
		this._rb = rb;
	}

	/**
	 * Method to get the localized text of inputed String.
	 */
	String getLocalized(String key) {

		if (key == null) {
			return null;
		}

		if ((key.charAt(0) == KEY_SIGN) && (key.length() > 1)) {
			if (_rb != null) {
				try {
					String transfered = _rb.getString(key.substring(1));
					if (transfered != null) {
						return transfered;
					}
				} catch (MissingResourceException mre) {
					// Nothing found for this key.
				}
			}
			// If no localization file available or no localized value found
			// for the key, then return the raw data without the key-sign.
			return key.substring(1);
		}
		return key;
	}
}
