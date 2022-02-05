/*******************************************************************************
 * Copyright (c) 2015, 2022 IBM Corporation and others.
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
package org.eclipse.osgi.tests.bundles;

import java.awt.image.ImageProducer;
import java.io.IOException;
import java.net.URL;
import org.osgi.framework.Bundle;

public class URLHandlerTests extends AbstractBundleTests {

	public void testURLHandlerUnregister() throws Exception {
		Bundle test = installer.installBundle("test.protocol.handler"); //$NON-NLS-1$
		test.start();
		URL testURL = new URL("testing1://test");
		testURL.openConnection().connect();
		test.stop();
		test.start();
		testURL = new URL("testing1://test");
		testURL.openConnection().connect();
	}

	public void testImageProducer() throws IOException {
		URL testImage = getClass().getResource("debug.gif");
		Object content = testImage.getContent();
		assertTrue("Wrong content type: " + content.getClass().getName(), content instanceof ImageProducer);
	}

}
