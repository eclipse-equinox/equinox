/*******************************************************************************
 * Copyright (c) 2011, 2017 SAP AG
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
package org.eclipse.equinox.console.commands;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.osgi.framework.BundleContext;

public class ManCommand {
	private BundleContext context;
	
	public ManCommand(BundleContext context) {
		this.context = context;
	}
	
	public void startService() {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put(CommandProcessor.COMMAND_SCOPE, "equinox");
		props.put(CommandProcessor.COMMAND_FUNCTION, new String[] {"man"});
		context.registerService(ManCommand.class.getName(), this, props);
	}
	
	public void man(CommandSession session, String... args) throws Exception {
		StringBuilder builder = null;
		if (args.length > 0) {
			builder = new StringBuilder();
			for(String arg : args) {
				builder.append(arg);
				builder.append(" ");
			}
		}
		
		String cmdForExecution = null;
		if (builder != null) {
			cmdForExecution = "equinox:help" + " " + builder.toString().trim();
		} else {
			cmdForExecution = "equinox:help";
		}
		
		session.execute(cmdForExecution);
	}
}
