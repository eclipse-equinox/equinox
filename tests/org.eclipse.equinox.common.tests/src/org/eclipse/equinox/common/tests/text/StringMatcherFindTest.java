package org.eclipse.equinox.common.tests.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.text.StringMatcher;
import org.eclipse.core.text.StringMatcher.Position;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

@RunWith(Parameterized.class)
public class StringMatcherFindTest {

	/** Marker in test inputs for empty strings. */
	private static final String EMPTY = "<empty>";

	private static final String TEST_DATA_FILE = "StringMatcherFindTest.txt";

	protected static class TestData {

		int line;

		String pattern;

		String text;

		int from;

		int to;

		int start;

		int end;

		@Override
		public String toString() {
			return "line " + line + ": " + (start >= 0 ? "OK" : "NOK") + " pattern=" + pattern + ", text=" + text + '['
					+ from + ',' + to + ']';
		}
	}

	@Parameters(name = "{0}")
	public static TestData[] getTestData() throws IOException {
		List<TestData> data = new ArrayList<>();
		BundleContext context = FrameworkUtil.getBundle(StringMatcherFindTest.class).getBundleContext();
		URL url = context.getBundle().getEntry("resources/"
				+ StringMatcherFindTest.class.getPackage().getName().replace('.', '/') + '/' + TEST_DATA_FILE);
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
				if (parts.length < 6) {
					fail("File " + TEST_DATA_FILE + ": invalid test input line " + i + ": " + line);
				}
				TestData item = new TestData();
				item.line = i;
				if (EMPTY.equals(parts[0])) {
					parts[0] = "";
				}
				item.pattern = parts[0];
				if (EMPTY.equals(parts[1])) {
					parts[1] = "";
				}
				item.text = parts[1];
				item.from = Integer.parseInt(parts[2]);
				item.to = Integer.parseInt(parts[3]);
				item.start = Integer.parseInt(parts[4]);
				item.end = Integer.parseInt(parts[5]);
				data.add(item);
			}
		}
		return data.toArray(new TestData[0]);
	}

	/** Injected by JUnit. */
	@Parameter
	public TestData data;

	@Test
	public void testFind() throws Exception {
		StringMatcher matcher = new StringMatcher(data.pattern, true, false);
		Position position = matcher.find(data.text, data.from, data.to);
		if (data.start < 0) {
			assertNull("No match expected", position);
		} else {
			assertEquals("Unexpected match result", new Position(data.start, data.end), position);
		}
	}
}
