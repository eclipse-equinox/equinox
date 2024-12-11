/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *     Sergey Prigogin (Google) - use parameterized types (bug 442021)
 *******************************************************************************/
package org.eclipse.equinox.api.internal;

import java.lang.reflect.Field;

import org.osgi.framework.Bundle;

/**
 * Helper methods related to string localization.
 * 
 * @since org.eclipse.equinox.common 3.3
 */
public class LocalizationUtils {
	/**
	 * This method can be used in the absence of NLS class. The method tries to use
	 * the NLS-based translation routine. If it falls, the method returns the
	 * original non-translated key.
	 * 
	 * @param key case-sensitive name of the filed in the translation file
	 *            representing the string to be translated
	 * @return The localized message or the non-translated key
	 */
	static public String safeLocalize(String key) {
		try {
			Class<?> messageClass = getMessageClass(); // $NON-NLS-1$
			if (messageClass == null)
				return key;
			Field field = messageClass.getDeclaredField(key);
			if (field == null)
				return key;
			Object value = field.get(null);
			if (value instanceof String)
				return (String) value;
		} catch (ClassNotFoundException | NoClassDefFoundError | NoSuchFieldException
				| IllegalAccessException | RuntimeException e) {
			// eat exception and fall through
		}
		return key;
	}

	protected static Class<?> getMessageClass() throws ClassNotFoundException {
		Bundle bundle = APISupport.equinoxCommonBundle;
		if (bundle != null) {
			return bundle.loadClass("org.eclipse.core.internal.runtime.CommonMessages");
		}
		return Class.forName("org.eclipse.core.internal.runtime.CommonMessages");
	}
}
