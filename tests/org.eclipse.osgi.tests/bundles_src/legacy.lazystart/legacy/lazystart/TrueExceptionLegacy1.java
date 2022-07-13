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

import org.eclipse.osgi.tests.bundles.ITestRunner;
import legacy.lazystart.b.excluded.a.BAExcluded;
import legacy.lazystart.b.excluded.b.BBExcluded;
public class TrueExceptionLegacy1 implements ITestRunner {

	@Override
	public Object testIt() throws Exception {
		new BAExcluded();
		return new BBExcluded();
	}

}
