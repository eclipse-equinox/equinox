/*******************************************************************************
 * Copyright (c) 2011, 2012 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
		Dictionary<String, Object> props = new Hashtable<String, Object>();
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
