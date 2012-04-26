/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation, SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Thomas Watson, IBM Corporation - initial API and implementation
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.command.adapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

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

	public Object main(Object[] args) throws Exception {
		try {
			// first argument is the command
			Method command = findCommand("_" + args[0]);
			ArrayList<Object> argList = new ArrayList<Object>();
			for (int i = 1; i < args.length; i++)
				argList.add(args[i]);
			return command.invoke(commandProvider, new CustomCommandInterpreter(argList));
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
	public Object _main(Object[] args) throws Exception {
		return main(args);
	}
}
