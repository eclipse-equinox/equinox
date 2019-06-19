/*******************************************************************************
 * Copyright (c) 2011, 2017 SAP AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.command.adapter;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.service.command.CommandSession;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.junit.Test;
import org.easymock.EasyMock;

public class CommandProviderAdapterTest {

	@Test
	public void testMain() throws Exception {
		CommandProvider provider = new TestCommandProvider();
		Method[] methods = TestCommandProvider.class.getMethods();
		Set<Method> m = new HashSet<>();
		for (Method method : methods) {
			if (method.getName().startsWith("_")) {
				m.add(method);
			}
		}
		CommandProviderAdapter providerAdapter = new CommandProviderAdapter(provider, m.toArray(new Method[0]));
		CommandSession session = EasyMock.createMock(CommandSession.class);
		
		String result = (String) providerAdapter.main(session, new Object[] {"test"});
		assertEquals("Result should be test", "test", result);
		
		result = (String) providerAdapter.main(session, new Object[] {"echo", "hello"});
		assertEquals("Result should be hello", "hello", result);
	}

	class TestCommandProvider implements CommandProvider {
		public String _test(CommandInterpreter i) {
			return "test";
		}
		
		public String _echo(CommandInterpreter i) {
			return i.nextArgument();
		}
		
		@Override
		public String getHelp() {
			return "this is a test command provider";
		}
		
	}
}
