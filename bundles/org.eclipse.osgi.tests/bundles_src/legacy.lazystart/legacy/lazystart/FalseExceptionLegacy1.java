/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package legacy.lazystart;

import legacy.lazystart.c.excluded.a.CAExcluded;
import legacy.lazystart.c.excluded.b.CBExcluded;
import org.eclipse.osgi.tests.bundles.ITestRunner;

public class FalseExceptionLegacy1 implements ITestRunner {

	@Override
	public Object testIt() throws Exception {
		new CAExcluded();
		return new CBExcluded();
	}

}
