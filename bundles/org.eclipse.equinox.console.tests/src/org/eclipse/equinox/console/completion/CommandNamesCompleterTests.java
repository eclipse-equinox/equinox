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

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.service.command.CommandSession;
import org.junit.Test;

public class CommandNamesCompleterTests {
	
	private static final String COMMANDS = ".commands";

	@Test
	public void testGetCandidates() {
		Set<String> commands = new HashSet<String>();
		commands.add("equinox:bundles");
		commands.add("equinox:diag");
		commands.add("equinox:setprop");
		commands.add("gogo:lb");
		commands.add("gogo:echo");
		commands.add("gogo:set");
		
		CommandSession session = createMock(CommandSession.class);
		expect(session.get(COMMANDS)).andReturn(commands).times(4);
		replay(session);
		
		CommandNamesCompleter completer = new CommandNamesCompleter(session);
		Map<String, Integer> candidates;
		
		candidates = completer.getCandidates("se", 2);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 2, candidates.size());
		assertNotNull("set should be in the resultset, but it is not", candidates.get("set"));
		assertNotNull("setprop should be in the resultset, but it is not", candidates.get("setprop"));
		
		candidates = completer.getCandidates("equinox:bun", "equinox:bun".length());
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 1, candidates.size());
		assertNotNull("equinox:bundles should be in the resultset, but it is not", candidates.get("equinox:bundles"));
		
		candidates = completer.getCandidates("ec", 2);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 1, candidates.size());
		assertNotNull("echo should be in the resultset, but it is not", candidates.get("echo"));
		
		candidates = completer.getCandidates("head", 4);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 0, candidates.size());
		
		verify(session);
	}

}
