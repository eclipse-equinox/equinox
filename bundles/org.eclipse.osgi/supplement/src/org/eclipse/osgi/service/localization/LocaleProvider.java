/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.localization;

import java.util.Locale;

/**
 * A service that is used to determine what the current locale is for a 
 * particular context or session.  If no <code>LocaleProvider</code> 
 * service is available then the locale must be determined by other 
 * means, for example, by calling {@link Locale#getDefault()}.
 * <p>
 * More advanced environments can support multiple locales within a 
 * single system.  For example, a server may support multiple users, 
 * each needing a different locale.  In such an environment a 
 * <code>LocaleProvider</code> service must be registered that can 
 * determine the current locale for the context of the call to the 
 * {@link #getLocale()} method.
 * </p>
 * @since 1.1
 */
public interface LocaleProvider {

	/**
	 * Determines the current locale for the context of the call to 
	 * this method.  For environments that support a single system wide 
	 * locale, this is equivalent to calling {@link Locale#getDefault()}.
	 * <p>
	 * The result of this method should not be retained or passed to other 
	 * threads.  The current locale can change any time and may be 
	 * different for each thread.
	 * </p>
	 * @return The current locale.
	 */
	public Locale getLocale();

}
