/*******************************************************************************
 * Copyright (c) 2014 Raymond Augé and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.registration;

import org.eclipse.equinox.http.servlet.internal.servlet.Match;
import org.osgi.dto.DTO;

/**
 * @author Raymond Augé
 */
public abstract class MatchableRegistration<T, D extends DTO>
	extends Registration<T, D> {

	public MatchableRegistration(T t, D d) {
		super(t, d);
	}

	public abstract String match(
		String name, String servletPath, String pathInfo, String extension,
		Match match);

	protected boolean isPathWildcardMatch(
		String pattern, String servletPath, String pathInfo) {

		int cpl = pattern.length() - 2;

		if (pattern.endsWith("/*") && servletPath.regionMatches(0, pattern, 0, cpl)) { //$NON-NLS-1$
			if ((pattern.length() > 2) && !pattern.startsWith(servletPath)) {
				return false;
			}

			if (servletPath.length() == cpl) {
				return true;
			}
		}

		return false;
	}

	protected boolean doMatch(
			String pattern, String servletPath, String pathInfo,
			String extension, Match match)
		throws IllegalArgumentException {

		if (match == Match.EXACT) {
			return pattern.equals(servletPath);
		}

		if (pattern.indexOf("/*.") == 0) { //$NON-NLS-1$
			pattern = pattern.substring(1);
		}

		if (pattern.charAt(0) == '/') {
			if ((match == Match.DEFAULT_SERVLET) && (pattern.length() == 1)) {
				return true;
			}

			if ((match == Match.REGEX) && isPathWildcardMatch(
					pattern, servletPath, pathInfo)) {

				return true;
			}
		}

		if ((match == Match.EXTENSION) && (pattern.indexOf("*.") == 0)) { //$NON-NLS-1$
			return pattern.substring(2).equals(extension);
		}

		return false;
	}

}