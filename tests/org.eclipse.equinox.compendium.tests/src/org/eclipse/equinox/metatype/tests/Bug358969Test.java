/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others
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
package org.eclipse.equinox.metatype.tests;

import static org.junit.Assert.assertFalse;

import org.eclipse.equinox.metatype.impl.LogTrackerMsg;
import org.eclipse.equinox.metatype.impl.MetaTypeMsg;
import org.junit.Test;

public class Bug358969Test extends AbstractTest {
	@Test
	public void test1() {
		String message = "NLS properties file not configured correctly"; //$NON-NLS-1$
		String prefix = "NLS missing message:"; //$NON-NLS-1$
		assertFalse(message, MetaTypeMsg.SERVICE_DESCRIPTION.startsWith(prefix));
		assertFalse(message, LogTrackerMsg.Debug.startsWith(prefix));
	}
}
