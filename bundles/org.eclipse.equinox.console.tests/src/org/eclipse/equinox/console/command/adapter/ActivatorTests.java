/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.command.adapter;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.junit.Test;

public class ActivatorTests {
	
	private static final String SCOPE_PROPERTY_NAME = "osgi.command.scope";
	private static final String FUNCTION_PROPERTY_NAME = "osgi.command.function";
	private static final String EQUINOX_SCOPE = "equinox";

	@Test
	public void testGetCommandMethods() {
		Set<String> commandNames = new HashSet<String>();
		commandNames.add("_testMethod1");
		commandNames.add("_testMethod2");
		commandNames.add("_testMethod3");
		
		Activator activator = new Activator();
		CommandProvider command = new TestCommandProvider();
		Method[] methods = activator.getCommandMethods(command);
		
		assertEquals("Command methods not as expected", 3, methods.length);
		for (Method method : methods) {
			assertTrue("Command methods should not include " + method.getName(), commandNames.contains(method.getName()));
		}
		
		Dictionary<String, Object> props = activator.getAttributes(methods);
		assertTrue("Attributes should contain property " + SCOPE_PROPERTY_NAME + " with value " + EQUINOX_SCOPE, EQUINOX_SCOPE.equals(props.get(SCOPE_PROPERTY_NAME)));
		String[] methodNames = (String[])props.get(FUNCTION_PROPERTY_NAME);
		assertEquals("Methods number not as expected", methods.length, methodNames.length);
		
		for(int i = 0; i < methods.length; i++) {
			assertEquals("Wrong method name", methods[i].getName().substring(1), methodNames[i]);
		}
	}

	class TestCommandProvider implements CommandProvider {
		public void _testMethod1(CommandInterpreter i) {
			
		}
		
		public void _testMethod2(CommandInterpreter i) {
			
		}
		
		public void _testMethod3(CommandInterpreter i) {
			
		}
		
		private void _method(CommandInterpreter i) {
			
		}
		
		@Override
		public String getHelp() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}

}
