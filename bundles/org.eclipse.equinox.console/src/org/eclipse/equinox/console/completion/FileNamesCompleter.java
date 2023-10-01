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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.console.completion.common.Completer;

/**
 * This class implements completion of file names. It provides completion both for 
 * files with absolute filenames, as well as with names, relative to the current
 * directory.
 */
public class FileNamesCompleter implements Completer {
	private static final String FILE = "file:";
	
	@Override
	public Map<String, Integer> getCandidates(String buffer, int cursor) {
		Map<String, Integer> result = new HashMap<>();
		String currentToken = CommandLineParser.getCurrentToken(buffer, cursor);
		if(currentToken == null || currentToken.equals("")) {
			return new HashMap<>();
		}
		
		// if current token contains file:, then use URL class to parse the filename
		if(currentToken.contains(FILE)) {
			String fileName = currentToken.substring(currentToken.indexOf(FILE));
			try {
				URL url = new URL(fileName);
				String canonicalFileName = url.getPath();
				File file = new File(canonicalFileName);
				File parent = file.getParentFile();
				 
				if ((file.isDirectory() && canonicalFileName.endsWith("/") )|| parent == null) {
					// the entered filename is a directory name, ending with file separator character - here 
					// all files in the directory will be returned as completion candidates; 
					// or, if parent is null, the file is in the root directory and 
					// the names of all files in this directory should be used to search for completion candidates
					return checkChildren(file, "", cursor, false);
				} else {
					// there is a filename for completion, and the names of all files in the same directory will be used
					// to search for completion candidates
					return checkChildren(parent, file.getName(), cursor, false);
				}
			} catch (MalformedURLException e) {
				return result;
			}
		}
		
		// the file name for completion is only the file separator character, so all files 
		// in the current directory will be returned as completion candidates
		if (currentToken.equals("\\\\") || currentToken.equals("/")) {
			File file = new File(".");
			return checkChildren(file, "", cursor, false);
		}
		
		// if the current token contains file separator character, then its parent directory can be extracted
		if (currentToken.contains("\\\\") || currentToken.contains("/")) {
			File file = new File(currentToken);
			File parent = file.getParentFile();
			if ((file.isDirectory() && (currentToken.endsWith("/") || currentToken.endsWith("\\\\")))
					|| parent == null) {
				// the entered filename is a directory name, ending with file separator character - here 
				// all files in the directory will be returned as completion candidates; 
				// or, if parent is null, the file is in the root directory and 
				// the names of all files in this directory should be used to search for completion candidates
				return checkChildren(file, "", cursor, false);
			} else {
				// there is a filename for completion, and the names of all files in the same directory will be used
				// to search for completion candidates
				return checkChildren(parent, file.getName(), cursor, false);
			}
		}
		
		// if the current token does not contain file separator character, 
		// then search for candidates in the current directory
		return checkChildren(new File("."), currentToken, cursor, false);
	}

	private Map<String, Integer> checkChildren(File parent, String nameToComplete, int cursor, boolean absolute) {
		Map<String, Integer> result = new HashMap<>();
		if(parent.exists()) {
			File[] children = parent.listFiles();
			for(File child : children) {
				if(child.getName().startsWith(nameToComplete)) {
					if(absolute == true) {
						result.put(child.getAbsolutePath(), cursor - nameToComplete.length());
					} else {
						result.put(child.getName(), cursor - nameToComplete.length());
					}
				}
			}
		}
		return result;
	}
}
