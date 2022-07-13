/*******************************************************************************
 * Copyright (C) 2020, Thomas Wolf <thomas.wolf@paranor.ch> and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.equinox.common.tests.text;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

@RunWith(Parameterized.class)
public class AbstractStringMatcherTestBase {

	/** Marker in test inputs for empty strings. */
	private static final String EMPTY = "<empty>";

	protected static class TestData {

		int line;

		boolean caseInsensitive;

		String pattern;

		String text;

		boolean expected;

		@Override
		public String toString() {
			return "line " + line + ": " + (expected ? "OK" : "NOK") + " pattern=" + pattern + ", text=" + text;
		}
	}

	public static TestData[] getTestData(String fileName) throws IOException {
		List<TestData> data = new ArrayList<>();
		BundleContext context = FrameworkUtil.getBundle(AbstractStringMatcherTestBase.class).getBundleContext();
		URL url = context.getBundle().getEntry(
				"resources/" + AbstractStringMatcherTestBase.class.getPackage().getName().replace('.', '/') + '/'
						+ fileName);
		url = FileLocator.toFileURL(url);
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
			String line;
			for (int i = 1; (line = reader.readLine()) != null; i++) {
				String l = line.trim();
				if (l.isEmpty() || l.charAt(0) == '#') {
					continue;
				}
				String[] parts = line.split("\t");
				if (parts.length < 4) {
					fail("File " + fileName + ": invalid test input line " + i + ": " + line);
				}
				TestData item = new TestData();
				item.line = i;
				item.caseInsensitive = Boolean.parseBoolean(parts[0]);
				if (EMPTY.equals(parts[1])) {
					parts[1] = "";
				}
				item.pattern = parts[1];
				if (EMPTY.equals(parts[2])) {
					parts[2] = "";
				}
				item.text = parts[2];
				item.expected = Boolean.parseBoolean(parts[3]);
				data.add(item);
			}
		}
		return data.toArray(new TestData[0]);
	}

	/** Injected by JUnit. */
	@Parameter
	public TestData data;

}
