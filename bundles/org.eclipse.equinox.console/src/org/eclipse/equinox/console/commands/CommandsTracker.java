/*******************************************************************************
 * Copyright (c) 2013, 2017 SAP AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * 	   Lazar Kirchev, SAP AG - initial API and implementation   
 *******************************************************************************/
package org.eclipse.equinox.console.commands;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class CommandsTracker {
	private Set<String> commandNames;
	private ServiceTracker<Object, Set<String>> commandsTracker = null;
	private static final Object lock = new Object(); 
	
	public CommandsTracker(BundleContext bundleContext) {
		commandNames = Collections.synchronizedSet(new HashSet<String>());
		try {
			Filter filter = bundleContext.createFilter(String.format("(&(%s=*)(%s=*))", CommandProcessor.COMMAND_SCOPE, CommandProcessor.COMMAND_FUNCTION));
			commandsTracker = new ServiceTracker<>(bundleContext, filter, new CommandsTrackerCustomizer());
			commandsTracker.open();
		} catch (InvalidSyntaxException e) {
			//do nothing;
		}
	}
	
	public Set<String> getCommands() {
		synchronized (lock) {
			return new HashSet<>(commandNames);
		}
	}
	
	class CommandsTrackerCustomizer implements ServiceTrackerCustomizer<Object, Set<String>> {
		@Override
		public Set<String> addingService(ServiceReference<Object> reference) {
			Object scope = reference.getProperty(CommandProcessor.COMMAND_SCOPE);
			Object function = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);

			if (scope != null && function != null) {
				synchronized (lock) {
					if (function.getClass().isArray()) {
						for (Object func : ((Object[]) function)) {
							commandNames.add(scope + ":" + func);
						}
					} else {
						commandNames.add(scope + ":" + function);
					}
					return commandNames;
				}
			}
			return null;
		}

		@Override
		public void modifiedService(ServiceReference<Object> reference, Set<String> commandNames) {
			// nothing to do
		}

		@Override
		public void removedService(ServiceReference<Object> reference, Set<String> commandNames) {
			Object scope = reference.getProperty(CommandProcessor.COMMAND_SCOPE);
			Object function = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);

			if (scope != null && function != null) {
				if (!function.getClass().isArray()) {
					commandNames.remove(scope + ":" + function);
				} else {
					for (Object func : (Object[]) function) {
						commandNames.remove(scope + ":" + func);
					}
				}
			}
		}
		
	}
}
