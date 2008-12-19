/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.link.b;

import org.osgi.framework.Bundle;
import test.link.b.params.AParam;

public class SomeAPI {
	public Long getBundleID(Bundle bundle) {
		return new Long(bundle.getBundleId());
	}

	public String getString(AParam arg0) {
		return arg0.toString();
	}
}
