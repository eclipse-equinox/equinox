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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.felix.service.command.CommandSession;
import org.eclipse.equinox.console.completion.common.Completer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * This class aggregates the different types of completers - variable, command
 * and file completers. It also searches for registered custom completers and if
 * available uses them too. It call all completers and finally returns the
 * completion candidates returned from all of them.
 */
public class CompletionHandler {

	private final BundleContext context;
	private final CommandSession session;
	Set<Completer> completers;
	private static final String FILE = "file";
	private static final char VARIABLE_PREFIX = '$';

	public CompletionHandler(BundleContext context, CommandSession session) {
		this.context = context;
		this.session = session;
		completers = new HashSet<>();
	}

	public Map<String, Integer> getCandidates(byte[] buf, int cursor) {
		String currentInput = new String(buf);
		String currentToken = CommandLineParser.getCurrentToken(currentInput, cursor);
		if (currentToken == null) {
			return new HashMap<>();
		}
		if (currentToken.contains(FILE) == true) {
			completers.add(new FileNamesCompleter());
		} else {
			if ((cursor - currentToken.length() > 0) && (buf[cursor - currentToken.length() - 1] == VARIABLE_PREFIX)) {
				completers.add(new VariableNamesCompleter(session));
			} else {
				completers.add(new CommandNamesCompleter(context, session));
				completers.add(new FileNamesCompleter());
			}
		}
		lookupCustomCompleters();
		Map<String, Integer> candidates = new TreeMap<>();
		for (Completer completer : completers) {
			candidates.putAll(completer.getCandidates(currentInput, cursor));
		}

		return candidates;
	}

	@SuppressWarnings("unchecked")
	private void lookupCustomCompleters() {
		ServiceReference<Completer>[] completersRefs = null;
		try {
			completersRefs = (ServiceReference<Completer>[]) context.getServiceReferences(Completer.class.getName(),
					null);
		} catch (InvalidSyntaxException e) {
			// do nothing
		}

		if (completersRefs != null) {
			for (ServiceReference<Completer> ref : completersRefs) {
				Completer completer = context.getService(ref);
				if (completer != null) {
					completers.add(completer);
				}
			}
		}
	}

}
