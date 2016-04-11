/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
