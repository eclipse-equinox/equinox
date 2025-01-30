/*******************************************************************************
 * Copyright (c) 2011, 2017 SAP AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.completion;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.equinox.console.completion.common.Completer;

/**
 * This class provides completion for arbitrary strings
 */
public class StringsCompleter implements Completer {

	private final Set<String> strings;
	private final boolean isCaseSensitive;

	public StringsCompleter(Set<String> strings, boolean isCaseSensitive) {
		this.strings = strings;
		this.isCaseSensitive = isCaseSensitive;
	}

	@Override
	public Map<String, Integer> getCandidates(String buffer, int cursor) {
		String currentToken = CommandLineParser.getCurrentToken(buffer, cursor);
		if (currentToken == null) {
			return new HashMap<>();
		}
		if (!isCaseSensitive) {
			currentToken = currentToken.toLowerCase();
		}

		int startIndex = cursor - currentToken.length();

		// if currentToken is empty string, then there is nothing to complete
		// the only exception is if the previous character is $, which signifies
		// that a variable name is expected; in this case all strings will be
		// returned as candidates
		if (currentToken.equals("") && buffer.charAt(startIndex - 1) != '$') {
			return new HashMap<>();
		}

		Map<String, Integer> result = new HashMap<>();

		for (String candidate : strings) {
			if (isCaseSensitive) {
				if (candidate.startsWith(currentToken)) {
					result.put(candidate, startIndex);
				}
			} else {
				if (candidate.toLowerCase().startsWith(currentToken)) {
					result.put(candidate, startIndex);
				}
			}
		}

		return result;
	}

}
