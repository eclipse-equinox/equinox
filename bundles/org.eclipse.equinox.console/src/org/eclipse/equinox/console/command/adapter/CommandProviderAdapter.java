/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation, SAP AG.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Thomas Watson, IBM Corporation - initial API and implementation
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.command.adapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.felix.service.command.CommandSession;
import org.eclipse.osgi.framework.console.CommandProvider;

/**
 * This adapter class provides for execution of legacy Equinox commands from 
 * the Gogo shell. The commands are executed through the main method of the
 * adapter. It finds the appropriate Equinox command and executes
 * it with the proper argument.
 *
 */
public class CommandProviderAdapter {

	private final CommandProvider commandProvider;
	private final Method[] commands;
	

	public CommandProviderAdapter(CommandProvider commandProvider, Method[] commands) {
		this.commandProvider = commandProvider;
		this.commands = commands;
	}

	public Object main(CommandSession commandSession, Object[] args) throws Exception {
		try {
			// first argument is the command
			Method command = findCommand("_" + args[0]);
			ArrayList<Object> argList = new ArrayList<>();
			for (int i = 1; i < args.length; i++)
				argList.add(args[i]);
			return command.invoke(commandProvider, new CustomCommandInterpreter(commandSession, argList));
		} catch (InvocationTargetException e) {
			if (e.getTargetException() instanceof Exception)
				throw (Exception) e.getTargetException();
			throw (Error) e.getTargetException();
		}
	}

	private Method findCommand(Object commandName) {
		for (Method command : commands) {
			if (command.getName().equalsIgnoreCase(commandName.toString()))
				return command;
		}
		throw new IllegalArgumentException("Cannot find the command method for: " + commandName);
	}

	// TODO Felix gogo seems to search for _main
	public Object _main(CommandSession commandSession, Object[] args) throws Exception {
		return main(commandSession, args);
	}
}
