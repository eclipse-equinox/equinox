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

import java.util.Map;
import java.util.Set;

import org.apache.felix.service.command.CommandSession;
import org.eclipse.equinox.console.completion.common.Completer;

/**
 * This class provides completion for gogo session variables.
 */
public class VariableNamesCompleter implements Completer {

	private final CommandSession session;

	public VariableNamesCompleter(CommandSession session) {
		this.session = session;
	}

	@Override
	public Map<String, Integer> getCandidates(String buffer, int cursor) {
		// CommandSession.get(null) returns the names of all registered varialbes
		@SuppressWarnings("unchecked")
		Set<String> variableNames = (Set<String>) session.get(null);
		StringsCompleter completer = new StringsCompleter(variableNames, false);
		return completer.getCandidates(buffer, cursor);
	}

}
