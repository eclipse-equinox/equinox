/*******************************************************************************
 * Copyright (c) 2014, 2020 Raymond Augé and others.
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

package org.eclipse.equinox.http.servlet.internal.registration;

import org.eclipse.equinox.http.servlet.internal.servlet.Match;
import org.eclipse.equinox.http.servlet.internal.util.Const;
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

	private boolean isPathWildcardMatch(
		String pattern, String servletPath) {

		int cpl = pattern.length() - 2;

		if (pattern.endsWith(Const.SLASH_STAR) && servletPath.regionMatches(0, pattern, 0, cpl)) {
			if ((pattern.length() > 2) && !pattern.startsWith(servletPath)) {
				return false;
			}

			if (servletPath.length() == cpl) {
				return true;
			}
		}

		return false;
	}

	final protected boolean doMatch(
			String pattern, String servletPath, String pathInfo,
			String extension, Match match)
		throws IllegalArgumentException {
		if (match == Match.EXACT) {
			return pattern.equals(servletPath);
		}
		if ((match == Match.CONTEXT_ROOT) && Const.BLANK.equals(pattern)) {
			return  Const.BLANK.equals(servletPath) && Const.SLASH.equals(pathInfo);
		}
		if ((match == Match.DEFAULT_SERVLET) && Const.SLASH.equals(pattern)) {
			return !servletPath.isEmpty() && pathInfo == null;
		}

		if (pattern.indexOf(Const.SLASH_STAR_DOT) == 0) {
			pattern = pattern.substring(1);
		}

		if (!pattern.isEmpty() && pattern.charAt(0) == '/') {
			if ((match == Match.DEFAULT_SERVLET) && (pattern.length() == 1)) {
				return true;
			}

			if ((match == Match.REGEX) && isPathWildcardMatch(
					pattern, servletPath)) {

				return true;
			}
		}

		if (match == Match.EXTENSION) {
			int index = pattern.lastIndexOf(Const.STAR_DOT);
			String patterPrefix = Const.BLANK;

			if (index > 0) {
				patterPrefix = pattern.substring(0, index - 1);
			}

			if ((index != -1) && (servletPath.equals(patterPrefix))) {
				return pattern.endsWith(Const.DOT + extension);
			}
		}

		return false;
	}
}
