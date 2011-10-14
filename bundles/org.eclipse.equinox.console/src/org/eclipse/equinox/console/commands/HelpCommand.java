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

package org.eclipse.equinox.console.commands;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.eclipse.equinox.console.command.adapter.CustomCommandInterpreter;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * This class provides help for the legacy equinox commands, which are adapted to Gogo commands. 
 */
public class HelpCommand {
	private BundleContext context;
	private Set<CommandProvider> legacyCommandProviders;
	private ServiceTracker<CommandProvider, Set<CommandProvider>> commandProvidersTracker;
	
    public class CommandProviderCustomizer implements ServiceTrackerCustomizer<CommandProvider, Set<CommandProvider>> {
    	private BundleContext context;
    	public CommandProviderCustomizer(BundleContext context) {
    		this.context = context;
    	}
    	
		public Set<CommandProvider> addingService(
				ServiceReference<CommandProvider> reference) {
			if (reference.getProperty("osgi.command.function") != null) {
				// must be a gogo function already; don' track
				return null;
			}
			CommandProvider command = context.getService(reference);
			legacyCommandProviders.add(command);
			context.ungetService(reference);
			return legacyCommandProviders;
		}

		public void modifiedService(
				ServiceReference<CommandProvider> reference,
				Set<CommandProvider> service) {
			// nothing to do
		}

		public void removedService(ServiceReference<CommandProvider> reference,
				Set<CommandProvider> providers) {
			CommandProvider provider = context.getService(reference);
			providers.remove(provider);
		}
    	
    }
	
	public HelpCommand(BundleContext context) {
		this.context = context;
		legacyCommandProviders = new HashSet<CommandProvider>();
		commandProvidersTracker = new ServiceTracker<CommandProvider, Set<CommandProvider>>(context, CommandProvider.class.getName(), new CommandProviderCustomizer(context));
		commandProvidersTracker.open();
	}
	
	public void start() {
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
		props.put(CommandProcessor.COMMAND_SCOPE, "equinox");
		props.put(CommandProcessor.COMMAND_FUNCTION, new String[] {"help"});
		context.registerService(HelpCommand.class.getName(), this, props);
	}
	
	/**
	 * Provides help for the available commands. The Gogo help command, used with no arguments, prints the names
	 * of all registered commands. If a command name is passed as argument to the help command, then the help
	 * message for the particular command is displayed (if such is defined).
	 * 
	 * This method can accept an additional argument -legacy. If this option is specified, the names of all 
	 * legacy equinox commands are displayed. If -legacy is not specified, then only the Gogo help command is called.
	 * 
	 * If -legacy is displayed along with a command name, then the legacy commands are searched
	 * for a command with this name, and the help message for this command is displayed, if provided. If the 
	 * CommandProvider, which provides this command, does not provide help for individual commands, then
	 * the help for all commands in the CommandProvider is displayed. 
	 * 
	 * This method can accept an additional argument -all. If this option is specified, then both the names of the 
	 * legacy equinox commands and the Gogo commands are displayed.
	 * 
	 * @param session
	 * @param args
	 * @throws Exception
	 */
	public void help(final CommandSession session, String... args) throws Exception {
		String command = null;
		
		if (args.length > 0) {
			command = args[0];
		}
		
		if (command != null) {
			for (CommandProvider provider : legacyCommandProviders) {
				Method[] methods = provider.getClass().getMethods();
				for (Method method : methods) {
					Object retval = null;
					if (method.getName().equals("_" + command)) {
						try {
							Method helpMethod = provider.getClass().getMethod("_help", CommandInterpreter.class);
							ArrayList<Object> argsList = new ArrayList<Object>();
							argsList.add(command);
							retval = helpMethod.invoke(provider, new CustomCommandInterpreter(argsList));
						} catch (Exception e) {
							System.out.println(provider.getHelp());
							break;
						}
						
						if (retval != null && retval instanceof String) {
							System.out.println(retval);
						}
						break;
					}
				}
			}
			
			try {
				session.execute("felix:help " + command);
			} catch (IllegalArgumentException e) {
				handleCommandNotFound();
			}
			
			return;
		}

		printLegacyCommands();
		try {
			session.execute("felix:help");
		} catch (IllegalArgumentException e) {
			handleCommandNotFound();
		}

	}
	
	private void printLegacyCommands() {
		for (CommandProvider provider : legacyCommandProviders) {
			Method[] methods = provider.getClass().getMethods();
			for (Method method : methods) {
				if (method.getName().startsWith("_") && !method.getName().equals("_help")) {
					System.out.println("equinox:" + method.getName().substring(1));
				}
			}
		}
	}
	
	private boolean checkStarted(String symbolicName) {
		Bundle[] bundles = context.getBundles();
		for (Bundle bundle : bundles) {
			if (bundle.getSymbolicName().equals(symbolicName) && bundle.getState() == Bundle.ACTIVE) {
				return true;
			}
		}
		
		return false;
	}
	
	private void handleCommandNotFound() {
		if (checkStarted("org.apache.felix.gogo.command")) {
			System.out.println("Cannot find felix:help command");
		} else {
			System.out.println("Cannot find felix:help command; bundle org.apache.felix.gogo.command is not started");
		}
	}
}
