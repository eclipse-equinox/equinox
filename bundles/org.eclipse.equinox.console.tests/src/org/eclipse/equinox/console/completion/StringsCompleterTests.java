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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class StringsCompleterTests {

	@Test
	public void testGetCandidates() {
		Set<String> strings = new HashSet<String>();
		strings.add("command");
		strings.add("SCOPE");
		strings.add("equinox:bundles");
		strings.add("common");
		
		StringsCompleter completer = new StringsCompleter(strings, false);
		Map<String, Integer> candidates;
		
		candidates = completer.getCandidates("sco", 3);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 1, candidates.size());
		assertNotNull("SCOPE should be in the resultset, but it is not", candidates.get("SCOPE"));
		
		candidates = completer.getCandidates("com", 3);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 2, candidates.size());
		assertNotNull("command should be in the resultset, but it is not", candidates.get("command"));
		assertNotNull("common should be in the resultset, but it is not", candidates.get("common"));
		
		candidates = completer.getCandidates("tr", 2);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 0, candidates.size());
		
		completer = new StringsCompleter(strings, true);
		
		candidates = completer.getCandidates("sco", 3);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 0, candidates.size());
		
		candidates = completer.getCandidates("SCO", 3);
		assertNotNull("Candidates null", candidates);
		assertEquals("Candidates not as expected", 1, candidates.size());
		assertNotNull("SCOPE should be in the resultset, but it is not", candidates.get("SCOPE"));
	}

}
