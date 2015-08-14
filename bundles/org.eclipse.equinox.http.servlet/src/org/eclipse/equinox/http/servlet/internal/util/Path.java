/*******************************************************************************
 * Copyright (c) 2015 Raymond Augé.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 467859
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.util;

/**
 * @author Raymond Augé
 */
public class Path {

	public static String findExtension(String path) {
		path = stripQueryString(path);

		String lastSegment = path.substring(path.lastIndexOf('/') + 1);

		int dot = lastSegment.lastIndexOf('.');

		if (dot == -1) {
			return null;
		}

		return lastSegment.substring(dot + 1);
	}

	public static String findQueryString(String path) {
		String queryString = null;

		int index = path.indexOf('?');

		if (index != -1) {
			queryString = path.substring(index + 1);
		}

		return queryString;
	}

	public static String stripQueryString(String path) {
		int index = path.indexOf('?');

		if (index != -1) {
			path = path.substring(0, index);
		}

		return path;
	}

}