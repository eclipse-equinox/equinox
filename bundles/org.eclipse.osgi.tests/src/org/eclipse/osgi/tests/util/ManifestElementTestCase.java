/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.osgi.tests.util;

import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;

import junit.framework.TestCase;

public class ManifestElementTestCase extends TestCase {

	public void testSpacesInValues() throws BundleException {
		ManifestElement[] elements = ManifestElement.parseHeader("test-spaces", "\"comp 1\";\"comp 2\";\"comp 3\";attr=\"val 1\";dir:=\"val 2\"");
		assertNotNull("1.0", elements);
		assertEquals("1.1", elements.length, 1);
		String[] components = elements[0].getValueComponents();
		assertEquals("1.2", components.length, 3);

		assertEquals("2.0", components[0], "comp 1");
		assertEquals("2.1", components[1], "comp 2");
		assertEquals("2.2", components[2], "comp 3");

		assertEquals("3.0", elements[0].getAttribute("attr"), "val 1");
		assertEquals("3.1", elements[0].getDirective("dir"), "val 2");
	}

}
