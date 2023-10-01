/*******************************************************************************
 * Copyright (c) 2015 Raymond Augé.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 467859
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.util;

/**
 * @author Raymond Augé
 */
public class Path {

	public Path(String path) {
		int index = path.indexOf('?');
		int semi = path.indexOf(';');

		if (semi != -1) {
			if (index > semi) {
				path = path.substring(0, semi) + path.substring(index, path.length());
			}
			else if (index == -1) {
				path = path.substring(0, semi);
			}
		}

		if (index == -1) {
			_requestURI = path;
			_queryString = null;
		}
		else {
			_requestURI = path.substring(0, index);
			_queryString = path.substring(index + 1);
		}

		index = _requestURI.lastIndexOf('.');

		if ((index == -1) || (index < _requestURI.lastIndexOf('/'))) {
			_extension = null;
		}
		else {
			_extension = _requestURI.substring(index + 1);
		}
	}

	public String getRequestURI() {
		return _requestURI;
	}

	public String getQueryString() {
		return _queryString;
	}

	public String getExtension() {
		return _extension;
	}

	private final String _requestURI;
	private final String _queryString;
	private final String _extension;

}
