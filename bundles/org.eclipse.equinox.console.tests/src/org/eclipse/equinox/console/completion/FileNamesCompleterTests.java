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

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileNamesCompleterTests {
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
	public void testGetCandidates() {
		FileNamesCompleter completer = new FileNamesCompleter();
		
		Map<String, Integer> candidates;
		
		candidates = completer.getCandidates("wor", "wor".length());
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 1, candidates.size());
		assertNotNull("work should be in the resultset, but it is not", candidates.get(WORK_DIR_NAME));
		
		candidates = completer.getCandidates("work/test", "work/test".length());
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 2, candidates.size());
		assertNotNull("testfile should be in the resultset, but it is not", candidates.get(TESTFILE));
		assertNotNull("testoutput should be in the resultset, but it is not", candidates.get(TESTOUTPUT));
		
		candidates = completer.getCandidates(WORK_DIR_NAME + "/", 5);
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
