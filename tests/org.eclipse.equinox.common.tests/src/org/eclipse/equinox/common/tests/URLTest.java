/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.common.tests;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.tests.harness.CoreTest;

public class URLTest extends CoreTest {

	public URLTest(String name) {
		super(name);
	}

	public void testPlatformPlugin() throws IOException {
		URL url = new URL("platform:/plugin/org.eclipse.equinox.common.tests/test.xml");
		InputStream is = url.openStream();
		is.close();
	}
}
