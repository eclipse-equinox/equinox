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

/**
 *  This class determines the last token in the command line. Completion should be made for this token. 
 */
public class CommandLineParser {
	private static final char[] delimiters = new char[] {'|', ';', '=', '{', '}', '(', ')', '$', '>', ']', ' '};
	private static final String COMMENT_CHAR = "#";
	private static final String HASH_MAP_DEF_START_CHAR = "<";
	private static final String HASH_MAP_DEF_END_CHAR = ">";
	private static final String LIST_DEF_START_CHAR = "[";
	/**
	 * Determine the last token in the command line. The last token is the substring, starting from 
	 * one of the characters, considered as delimiters
	 * 
	 * @param commandLine whole command line
	 * @param cursor current position in the command line
	 * @return the current token
	 */
	public static String getCurrentToken(String commandLine, int cursor) {
		String current = commandLine.substring(0, cursor);
	
		int currentStartIdx = -1;
		// determine the positioin of the last delimiter
		for(char delimiter : delimiters) {
			int idx = current.lastIndexOf(delimiter);
			if (delimiter == '=' && idx > -1) {
				// hash map is defined in a command with the syntax <key=value>; within this definition we do not want to 
				// make completion; determine if we are in such case
				int startAngleBraceIdx = current.substring(0, idx).lastIndexOf(HASH_MAP_DEF_START_CHAR);
				int endAngleBraceBeforeAssignmentIdx = current.substring(0, idx).lastIndexOf(HASH_MAP_DEF_END_CHAR);
				int endAngleBraceAfterAssignmentIdx = current.substring(idx + 1).indexOf(HASH_MAP_DEF_END_CHAR);
				if (startAngleBraceIdx > -1 && startAngleBraceIdx < idx && endAngleBraceBeforeAssignmentIdx == -1 && endAngleBraceAfterAssignmentIdx == -1) {
					return null;
				}
			}
			if (idx > currentStartIdx) {
				currentStartIdx = idx;
			}
		}
		
		if (currentStartIdx + 1 == current.length()) {
			return "";
		}
		
		if (currentStartIdx + 1 > current.length()) {
			return null;
		}
		
		String currentToken = current.substring(currentStartIdx + 1, current.length());
		
		// if the current position is after the comment character, or within a hash map or list definition, do not do command completion
		if (currentToken.contains(COMMENT_CHAR) || currentToken.contains(HASH_MAP_DEF_START_CHAR) || currentToken.contains(LIST_DEF_START_CHAR)) {
			return null;
		}
		
		return currentToken;
	}
}
