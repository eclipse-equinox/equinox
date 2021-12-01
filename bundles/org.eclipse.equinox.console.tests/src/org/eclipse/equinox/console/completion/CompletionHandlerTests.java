/*******************************************************************************
 * Copyright (c) 2011, 2021 SAP AG and others.
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.felix.service.command.CommandSession;
import org.eclipse.equinox.console.completion.common.Completer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class CompletionHandlerTests {

	private static final String COMMANDS = ".commands";
	private static final String WORK_DIR_NAME = "work";
	private static final String TESTFILE = "testfile";
	private static final String TESTOUTPUT = "testoutput";
	private static final String FILE = "file";
	
	@Before
	public void init() throws IOException {
		File currentDir = new File(".");
		File[] files = currentDir.listFiles();
		for (File file : files) {
			if(file.getName().equals(WORK_DIR_NAME)) {
				clean();
				break;
			}
		}
		
		File workDir = new File(currentDir.getAbsolutePath() + File.separator + WORK_DIR_NAME);
		workDir.mkdir();
		
		createFile(workDir, TESTFILE);
		createFile(workDir, TESTOUTPUT);
		createFile(workDir, FILE);
	}
	
	@Test
	public void testGetCandidates() throws Exception {
		Filter filter = mock(Filter.class);

		BundleContext context = mock(BundleContext.class);
		when(context.getServiceReferences(Completer.class.getName(), null)).thenReturn(null);
		when(context.createFilter("(objectClass=org.eclipse.equinox.console.commands.CommandsTracker)")).thenReturn(filter);
		context.addServiceListener(isA(ServiceListener.class), isA(String.class));
		when(context.getServiceReferences("org.eclipse.equinox.console.commands.CommandsTracker", null)).thenReturn(new ServiceReference[]{});
		
		Set<String> variables = new HashSet<>();
		variables.add("SCOPE");
		variables.add("PROMPT");
		variables.add("ECHO_ON");
		variables.add("ECHO");
		
		Set<String> commands = new HashSet<>();
		commands.add("equinox:bundles");
		commands.add("equinox:diag");
		commands.add("equinox:setprop");
		commands.add("gogo:lb");
		commands.add("gogo:echo");
		commands.add("gogo:set");
		
		CommandSession session = mock(CommandSession.class);
		when(session.get(null)).thenReturn(variables);
		when(session.get(COMMANDS)).thenReturn(commands);
		
		CompletionHandler completer = new CompletionHandler(context, session);
		Map<String, Integer> candidates;
		
		candidates = completer.getCandidates("$SC".getBytes(), 2);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 1, candidates.size());
		assertNotNull("SCOPE should be in the resultset, but it is not", candidates.get("SCOPE"));
		
		candidates = completer.getCandidates("$EC".getBytes(), 2);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 2, candidates.size());
		assertNotNull("ECHO_ON should be in the resultset, but it is not", candidates.get("ECHO_ON"));
		assertNotNull("ECHO should be in the resultset, but it is not", candidates.get("ECHO"));
		
		candidates = completer.getCandidates("$AB".getBytes(), 2);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 0, candidates.size());
		
		completer = new CompletionHandler(context, session);
		candidates = completer.getCandidates("se".getBytes(), 2);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 2, candidates.size());
		assertNotNull("set should be in the resultset, but it is not", candidates.get("set"));
		assertNotNull("setprop should be in the resultset, but it is not", candidates.get("setprop"));
		
		candidates = completer.getCandidates("equinox:bun".getBytes(), "equinox:bun".length());
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 1, candidates.size());
		assertNotNull("equinox:bundles should be in the resultset, but it is not", candidates.get("equinox:bundles"));
		
		candidates = completer.getCandidates("ec".getBytes(), 2);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 1, candidates.size());
		assertNotNull("echo should be in the resultset, but it is not", candidates.get("echo"));
		
		candidates = completer.getCandidates("head".getBytes(), "head".length());
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 0, candidates.size());
		
		completer = new CompletionHandler(context, session);
		candidates = completer.getCandidates("wor".getBytes(), "wor".length());
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 1, candidates.size());
		assertNotNull("work should be in the resultset, but it is not", candidates.get(WORK_DIR_NAME));
		
		candidates = completer.getCandidates("work/test".getBytes(), "work/test".length());
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 2, candidates.size());
		assertNotNull("testfile should be in the resultset, but it is not", candidates.get(TESTFILE));
		assertNotNull("testoutput should be in the resultset, but it is not", candidates.get(TESTOUTPUT));
		
		candidates = completer.getCandidates("work/".getBytes(), "work/".length());
		assertEquals("Candidates not as expected", 3, candidates.size());
		assertNotNull("testfile should be in the resultset, but it is not", candidates.get(TESTFILE));
		assertNotNull("testoutput should be in the resultset, but it is not", candidates.get(TESTOUTPUT));
		assertNotNull("file should be in the resultset, but it is not", candidates.get(FILE));
	}
	
	@After
	public void cleanUp() {
		clean();
	}
	
	private void clean() {
		File currentFile = new File(".");
		File workDir = new File(currentFile.getAbsolutePath() + File.separator + WORK_DIR_NAME);
		File[] files = workDir.listFiles();
		for (File file : files) {
			file.delete();
		}
		workDir.delete();
	}
	
	private void createFile(File parentDir, String filename) throws IOException {
		File file = new File(parentDir.getAbsolutePath() + File.separator + filename);
		try (PrintWriter out = new PrintWriter(new FileOutputStream(file))) {
			out.write(filename);
			out.flush();
		}
	}

}
