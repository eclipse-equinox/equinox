/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.osgi.framework.Bundle;

public class NativeCodeBundleTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(NativeCodeBundleTests.class);
	}

	public void testNativeCode01() throws Exception {
		Bundle nativetestA1 = installer.installBundle("nativetest.a1");
		nativetestA1.start();
		nativetestA1.stop();
		Object[] a1Results = simpleResults.getResults(1);

		installer.updateBundle("nativetest.a1", "nativetest.a2");
		nativetestA1.start();
		nativetestA1.stop();		
		Object[] a2Results = simpleResults.getResults(1);
		assertTrue("1.0", a1Results.length == 1);
		assertTrue("1.1", a2Results.length == 1);
		assertNotNull("1.2", a1Results[0]);
		assertNotNull("1.3", a2Results[0]);
		assertFalse("1.4", a1Results[0].equals(a2Results[0]));
	}

	public void testNativeCode02() throws Exception {
		Bundle nativetestB1 = installer.installBundle("nativetest.b1");
		nativetestB1.start();
		nativetestB1.stop();
		Object[] b1Results = simpleResults.getResults(1);

		installer.updateBundle("nativetest.b1", "nativetest.b2");
		nativetestB1.start();
		nativetestB1.stop();		
		Object[] b2Results = simpleResults.getResults(1);
		assertTrue("1.0", b1Results.length == 1);
		assertTrue("1.1", b2Results.length == 1);
		assertNotNull("1.2", b1Results[0]);
		assertNotNull("1.3", b2Results[0]);
		assertFalse("1.4", b1Results[0].equals(b2Results[0]));
	}
}
