/*******************************************************************************
 * Copyright (c) 2014 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.util;

import java.util.*;

/**
 * @author Raymond Augé
 */
public class StringPlus {

	@SuppressWarnings("unchecked")
	public static List<String> from(Object object) {
		if (String.class.isInstance(object)) {
			return Collections.singletonList((String)object);
		}
		else if (String[].class.isInstance(object)) {
			return Arrays.asList((String[])object);
		}
		else if (Collection.class.isInstance(object)) {
			Collection<?> collection = (Collection<?>)object;

			if (!collection.isEmpty() &&
				String.class.isInstance(collection.iterator().next())) {

				return new ArrayList<>((Collection<String>)object);
			}
		}

		return Collections.emptyList();
	}

}
