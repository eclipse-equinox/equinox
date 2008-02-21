/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.transforms;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;

/**
 * This class is used by the transformer hook to parse urls provided by transform developers that specifies the particular transforms that should be utilized for a particular transformer.  
 * TODO: factor this out into a new type of service the transformer uses.  Then there could be CSV transforms, programatic transforms, etc.
 */
public class CSVParser {

	/**
	 * Parse the given url as a CSV file containing transform tuples.  The tuples have the form:
	 * <pre>
	 * bundleRegex,pathRegex,transformerResource
	 * </pre> 
	 * @param transformMapURL the map url
	 * @return an array of tuples derived from the contents of the file
	 * @throws IOException thrown if there are issues parsing the file
	 */
	public static TransformTuple[] parse(URL transformMapURL) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(transformMapURL.openStream()));
		String currentLine = null;
		List list = new ArrayList();
		while ((currentLine = reader.readLine()) != null) {
			if (currentLine.startsWith("#")) { //$NON-NLS-1$
				continue;
			}
			currentLine = currentLine.trim();
			if (currentLine.length() == 0)
				continue;
			StringTokenizer toker = new StringTokenizer(currentLine, ","); //$NON-NLS-1$
			try {
				String bundlePatternString = toker.nextToken().trim();
				String pathPatternString = toker.nextToken().trim();
				String transformPath = toker.nextToken().trim();
				try {
					Pattern bundlePattern = Pattern.compile(bundlePatternString);
					Pattern pathPattern = Pattern.compile(pathPatternString);
					URL transformerURL = new URL(transformMapURL, transformPath);
					try {
						transformerURL.openStream();
						TransformTuple tuple = new TransformTuple();
						tuple.bundlePattern = bundlePattern;
						tuple.pathPattern = pathPattern;
						tuple.transformerUrl = transformerURL;
						list.add(tuple);
					} catch (IOException e) {
						TransformerHook.log(FrameworkLogEntry.ERROR, "Could not add transform :" + transformerURL.toString(), e); //$NON-NLS-1$
					}
				} catch (PatternSyntaxException e) {
					TransformerHook.log(FrameworkLogEntry.ERROR, "Could not add compile transform matching regular expression", e); //$NON-NLS-1$
				}

			} catch (NoSuchElementException e) {
				TransformerHook.log(FrameworkLogEntry.ERROR, "Could not parse transform file record :" + currentLine, e); //$NON-NLS-1$
			}
		}
		return (TransformTuple[]) list.toArray(new TransformTuple[list.size()]);
	}
}
