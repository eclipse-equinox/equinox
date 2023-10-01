/*******************************************************************************
 * Copyright (c) 2011 SAP AG
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

import org.junit.Test;

public class CommandLineParserTests {
	
	private static final String PIPE_TEST_INPUT = "command1|comm";
	private static final String CONSECUTIVE_COMMANDS_TEST_INPUT = "command1;comm";
	private static final String ASSIGNMENT_TEST_INPUT = "var=val";
	private static final String START_CLOSURE_TEST_INPUT = "${comm";
	private static final String END_CLOSURE_TEST_INPUT = "${command}arg1";
	private static final String START_MACRO_TEST_INPUT = "$(macr";
	private static final String END_MACRO_TEST_INPUT = "$(macro)val";
	private static final String VARIABLE_TEST_INPUT = "$VAR";
	private static final String START_MAP_TEST_INPUT = "<key=val";
	private static final String END_MAP_TEST_INPUT = "<key=val>other";
	private static final String START_LIST_TEST_INPUT = "[elem1,elem2,el";
	private static final String LIST_TEST_INPUT = "[elem1, elem2, elem3]other";
	private static final String COMMAND_ARGUMENTS_TEST_INPUT = "command argument1 argum";
	private static final String COMMAND_NAME_TEST_INPUT = "com";
	private static final String COMMENT_TEST_INPUT="command#comment";

	@Test
	public void testGetCurrentToken() {
		String token;
		
		token = CommandLineParser.getCurrentToken(PIPE_TEST_INPUT, PIPE_TEST_INPUT.length());
		assertEquals("Pipe not parsed correctly", "comm", token);
		
		token = CommandLineParser.getCurrentToken(CONSECUTIVE_COMMANDS_TEST_INPUT, CONSECUTIVE_COMMANDS_TEST_INPUT.length());
		assertEquals("Consequtive commands not parsed correctly", "comm", token);
		
		token = CommandLineParser.getCurrentToken(ASSIGNMENT_TEST_INPUT, ASSIGNMENT_TEST_INPUT.length());
		assertEquals("Assignment not parsed correctly", "val", token);
		
		token = CommandLineParser.getCurrentToken(START_CLOSURE_TEST_INPUT, START_CLOSURE_TEST_INPUT.length());
		assertEquals("Start closure not parsed correctly", "comm", token);
		
		token = CommandLineParser.getCurrentToken(END_CLOSURE_TEST_INPUT, END_CLOSURE_TEST_INPUT.length());
		assertEquals("End closure not parsed correctly", "arg1", token);
		
		token = CommandLineParser.getCurrentToken(START_MACRO_TEST_INPUT, START_MACRO_TEST_INPUT.length());
		assertEquals("Start macro not parsed correctly", "macr", token);
		
		token = CommandLineParser.getCurrentToken(END_MACRO_TEST_INPUT, END_MACRO_TEST_INPUT.length());
		assertEquals("End macro not parsed correctly", "val", token);
		
		token = CommandLineParser.getCurrentToken(VARIABLE_TEST_INPUT, VARIABLE_TEST_INPUT.length());
		assertEquals("Variable name not parsed correctly", "VAR", token);
		
		token = CommandLineParser.getCurrentToken(START_MAP_TEST_INPUT, START_MAP_TEST_INPUT.length());
		assertNull("Start map not parsed correctly", token);
		
		token = CommandLineParser.getCurrentToken(END_MAP_TEST_INPUT, END_MAP_TEST_INPUT.length());
		assertEquals("End map not parsed correctly", "other", token);
		
		token = CommandLineParser.getCurrentToken(START_LIST_TEST_INPUT, START_LIST_TEST_INPUT.length());
		assertNull("Start list not parsed correctly", token);
		
		token = CommandLineParser.getCurrentToken(LIST_TEST_INPUT, LIST_TEST_INPUT.length());
		assertEquals("List not parsed correctly", "other", token);
		
		token = CommandLineParser.getCurrentToken(COMMAND_ARGUMENTS_TEST_INPUT, COMMAND_ARGUMENTS_TEST_INPUT.length());
		assertEquals("Command with arguments not parsed correctly", "argum", token);
		
		token = CommandLineParser.getCurrentToken(COMMAND_NAME_TEST_INPUT, COMMAND_NAME_TEST_INPUT.length());
		assertEquals("Command name not parsed correctly", "com", token);
		
		token = CommandLineParser.getCurrentToken(COMMENT_TEST_INPUT, COMMENT_TEST_INPUT.length());
		assertNull("Comment not parsed correctly", token);
	}

}
