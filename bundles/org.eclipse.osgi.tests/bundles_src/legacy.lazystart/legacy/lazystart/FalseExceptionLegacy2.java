/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package legacy.lazystart;

import org.eclipse.osgi.tests.bundles.ITestRunner;
import legacy.lazystart.c.CTest;
public class FalseExceptionLegacy2 implements ITestRunner {

	public Object testIt() throws Exception {
		return new CTest();
	}

}
