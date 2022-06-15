/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package test.link.a;

import org.osgi.framework.Bundle;
import test.link.a.params.AParam;

public class SomeAPI {
	public Long getBundleID(Bundle bundle) {
		return Long.valueOf(bundle.getBundleId());
	}

	public String getString(AParam arg0) {
		return arg0.toString();
	}
}
