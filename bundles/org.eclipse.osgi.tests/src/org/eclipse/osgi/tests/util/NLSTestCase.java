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

import junit.framework.TestCase;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleException;

public class NLSTestCase extends TestCase {

	public void testEmptyMessageBug200296() throws BundleException {
		NLS.bind("", new Integer(0));
	}

}
