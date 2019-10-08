/*******************************************************************************
 * Copyright (c) 2016 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - initial implementation
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.tests.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class EventHandler {

	public void close() {
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void handle(Map<String, String> eventMap) {
		System.out.println("==event==\n" + eventMap.get("data"));
	}

	public void open(final InputStream inputStream) {
		Runnable streamProcessorThread = new Runnable() {

			@Override
			public void run() {
				System.out.println("==event stream opened==");

				// Ref: https://html.spec.whatwg.org/multipage/comms.html#server-sent-events

				Map<String, String> eventMap = new HashMap<>();

				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

				String current;

				try {
					while ((current = reader.readLine()) != null) {
						if (current.length() == 0) {
							handle(eventMap);

							eventMap = new HashMap<>();

							continue;
						}

						int colon = current.indexOf('\u003A');

						if (colon == 0) {
							// ignore comment lines

							continue;
						}
						else if (colon < 0) {
							// No colon? Entire line must be treated as the key with blank value

							eventMap.put(current, "");

							continue;
						}

						String key = current.substring(0, colon);
						String value = current.substring(colon + 1);

						if (value.startsWith("\u0020")) {
							value = value.substring(1);
						}

						if (eventMap.containsKey(key)) {
							String currentValue = eventMap.get(key);

							value = currentValue + '\n' + value;
						}

						eventMap.put(key, value);
					}
				}
				catch (IOException e) {
					e.printStackTrace();
				}
				finally {
					try {
						inputStream.close();
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}

				// Ignore remaining content which is not a well formed event

				System.out.println("==event stream closed==");
			}

		};

		thread = new Thread(streamProcessorThread);

		thread.start();
	}

	private Thread thread;

}