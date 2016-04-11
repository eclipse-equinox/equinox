/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.completion;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.service.command.CommandSession;
import org.junit.Test;

public class VariableNamesCompleterTests {

	@Test
	public void testGetCandidates() {
		Set<String> variables = new HashSet<String>();
		variables.add("SCOPE");
		variables.add("PROMPT");
		variables.add("ECHO_ON");
		variables.add("ECHO");
		
		CommandSession session = createMock(CommandSession.class);
		expect(session.get(null)).andReturn(variables).times(3);
		replay(session);
		
		VariableNamesCompleter completer = new VariableNamesCompleter(session);
		Map<String, Integer> candidates;
		
		candidates = completer.getCandidates("SC", 2);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 1, candidates.size());
		assertNotNull("SCOPE should be in the resultset, but it is not", candidates.get("SCOPE"));
		
		candidates = completer.getCandidates("EC", 2);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 2, candidates.size());
		assertNotNull("ECHO_ON should be in the resultset, but it is not", candidates.get("ECHO_ON"));
		assertNotNull("ECHO should be in the resultset, but it is not", candidates.get("ECHO"));
		
		candidates = completer.getCandidates("AB", 2);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 0, candidates.size());
		
		verify(session);
	}

}
