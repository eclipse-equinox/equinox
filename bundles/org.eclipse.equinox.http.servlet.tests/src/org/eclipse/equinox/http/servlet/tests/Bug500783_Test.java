/*******************************************************************************
 * Copyright (c) Sept. 16, 2019 Liferay, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Liferay, Inc. - tests
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Bug500783_Test extends BaseTest {

	@SuppressWarnings("serial")
	@Test
	public void test() throws Exception {
		BundleContext context = getBundleContext();

		HttpServlet myServlet = new HttpServlet() {
			@Override
			protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				int contentLength = req.getContentLength();

				InputStream in = req.getInputStream();
				// Read the input stream
				int bytesRead = 0;
				byte[] buffer = new byte[1024];
				int bytes = 0;
				while (bytes != -1) {
					bytes = in.read(buffer, 0, buffer.length);
					if (bytes != -1) {
						bytesRead += bytes;
					}
				}
				in.close();
				StringBuffer sb = new StringBuffer().append(contentLength);

				for (Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
					sb.append("|").append(entry.getKey()).append("=").append(Arrays.toString(entry.getValue()));
				}

				sb.append("|").append(bytesRead);
				Writer out = resp.getWriter();
				Reader rdr = new StringReader(sb.toString());

				while (true) {
					int ch = rdr.read();
					if (ch == -1) {
						break;
					}
					out.write(ch);
				}
				out.close();
				resp.setStatus(200);
			}
		};

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN, "/MyServlet");
		registrations.add(context.registerService(Servlet.class, myServlet, properties));

		Map<String, List<Object>> map = new HashMap<>();
		map.put("method", Arrays.<Object>asList("POST"));
		map.put("x-www-form-urlencoded", Arrays.<Object>asList("fielda=foo&fieldb=bar"));

		Map<String, List<String>> response = requestAdvisor.upload("MyServlet", map);
		assertEquals("21|21", response.get("responseBody").get(0));
		assertEquals(HttpServletResponse.SC_OK + "", response.get("responseCode").get(0));
	}

}
