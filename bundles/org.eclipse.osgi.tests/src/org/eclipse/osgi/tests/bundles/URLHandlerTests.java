/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.net.URL;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.osgi.framework.Bundle;

public class URLHandlerTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(URLHandlerTests.class);
	}

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

}
