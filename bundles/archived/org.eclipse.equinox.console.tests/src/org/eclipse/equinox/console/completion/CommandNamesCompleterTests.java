/*******************************************************************************
 * Copyright (c) 2011, 2017 SAP AG and others.
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class CommandNamesCompleterTests {
	
	private static final String COMMANDS = ".commands";

	@Test
	public void testGetCandidates() throws Exception {
		Set<String> commands = new HashSet<>();
		commands.add("equinox:bundles");
		commands.add("equinox:diag");
		commands.add("equinox:setprop");
		commands.add("gogo:lb");
		commands.add("gogo:echo");
		commands.add("gogo:set");
		
		CommandSession session = createMock(CommandSession.class);
		expect(session.get(COMMANDS)).andReturn(commands).times(4);
		replay(session);
		
		Filter filter = createMock(Filter.class);
		replay(filter);
		
		BundleContext context = createMock(BundleContext.class);
//		expect(context.createFilter(String.format("(&(%s=*)(%s=*))", CommandProcessor.COMMAND_SCOPE, CommandProcessor.COMMAND_FUNCTION))).andReturn(filter);
		expect(context.createFilter("(objectClass=org.eclipse.equinox.console.commands.CommandsTracker)")).andReturn(filter);
		context.addServiceListener(isA(ServiceListener.class), isA(String.class));
		expect(context.getServiceReferences("org.eclipse.equinox.console.commands.CommandsTracker", null)).andReturn(new ServiceReference[]{});
		replay(context);
		
		CommandNamesCompleter completer = new CommandNamesCompleter(context, session);
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
