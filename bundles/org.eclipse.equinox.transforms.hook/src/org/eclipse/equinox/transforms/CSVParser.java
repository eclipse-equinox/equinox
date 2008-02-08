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

package org.eclipse.equinox.transforms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class CSVParser {

	/**
	 * Utility method that transform providers may use to parse a CSV file
	 * containing mappings between bundle/path regular expressions and
	 * transformer urls. The resulting transforms are registered against the
	 * provided type name.
	 * 
	 * @param context
	 *            the bundle context on which to register the transforms
	 * @param transformMapURL
	 *            the url to the CSV file
	 * @param typeName
	 *            the name under which to register any transformations
	 * @return the service registration corresponding to the transforms
	 *         registered from the contents of the CSV
	 * @throws IOException
	 *             thrown if there are problems parsing the CSV file
	 */
	public static ServiceRegistration register(BundleContext context,
			URL transformMapURL, String typeName) throws IOException {
		Properties properties = new Properties();
		properties.setProperty(TransformTuple.TRANSFORMER_TYPE, typeName);
		TransformTuple[] transforms = parse(context, transformMapURL);
		return context.registerService(TransformTuple[].class.getName(),
				transforms, properties);
	}

	public static TransformTuple[] parse(BundleContext context,
			URL transformMapURL) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				transformMapURL.openStream()));
		String currentLine = null;
		List list = new ArrayList();
		while ((currentLine = reader.readLine()) != null) {
			if (currentLine.startsWith("#"))
				continue;
			currentLine = currentLine.trim();
			if (currentLine.length() == 0)
				continue;
			StringTokenizer toker = new StringTokenizer(currentLine, ",");
			try {
				String bundlePatternString = toker.nextToken().trim();
				String pathPatternString = toker.nextToken().trim();
				String transformPath = toker.nextToken().trim();
				try {
					Pattern bundlePattern = Pattern
							.compile(bundlePatternString);
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
						TransformerHook.log(
								FrameworkLogEntry.ERROR,
								"Could not add transform :" + transformerURL.toString(), e); //$NON-NLS-1$
					}
				} catch (PatternSyntaxException e) {
					TransformerHook.log(
							FrameworkLogEntry.ERROR,
							"Could not add compile transform matching regular expression", e); //$NON-NLS-1$
				}

			} catch (NoSuchElementException e) {
				TransformerHook.log(FrameworkLogEntry.ERROR, "Could not parse transform file record :" + currentLine, e);
			}
		}
		return (TransformTuple[]) list.toArray(new TransformTuple[list.size()]);
	}
}
