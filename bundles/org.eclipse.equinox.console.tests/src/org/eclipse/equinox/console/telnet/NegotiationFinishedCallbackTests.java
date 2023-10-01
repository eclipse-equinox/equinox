/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Lazar Kirchev, SAP AG - initial contribution
 ******************************************************************************/

package org.eclipse.equinox.console.telnet;

import org.junit.Assert;
import org.junit.Test;

public class NegotiationFinishedCallbackTests {
	
	@Test
	public void finishTest() throws Exception {
		TelnetConnection telnetConnection = null;
		telnetConnection = new TelnetConnection (null, null, null);
		NegotiationFinishedCallback callback = new NegotiationFinishedCallback(telnetConnection);
		callback.finished();
		Assert.assertTrue("Finished not called on console session", telnetConnection.isTelnetNegotiationFinished);
	}
}
