/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation, SAP AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * 	   Thomas Watson, IBM Corporation - initial API and implementation
 *     Lazar Kirchev, SAP AG - initial API and implementation   
 *******************************************************************************/

package org.eclipse.equinox.console.command.adapter;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.eclipse.equinox.console.commands.CommandsTracker;
import org.eclipse.equinox.console.commands.DisconnectCommand;
import org.eclipse.equinox.console.commands.EquinoxCommandProvider;
import org.eclipse.equinox.console.commands.HelpCommand;
import org.eclipse.equinox.console.commands.ManCommand;
import org.eclipse.equinox.console.telnet.TelnetCommand;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.framework.console.ConsoleSession;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator implements BundleActivator {
	private enum GOGO {
		RUNTIME("org.apache.felix.gogo.runtime", "runtime.implementation", true),
		SHELL("org.apache.felix.gogo.shell", "shell.implementation", true),
		COMMAND("org.apache.felix.gogo.command", "command.implementation", false);

		private final GogoRequirement identityRequirement;
		private final GogoRequirement implRequirement;
		private final boolean required;
		GOGO(String bsn, String implName, boolean required) {
			this.identityRequirement = new GogoRequirement(IdentityNamespace.IDENTITY_NAMESPACE, bsn);
			this.implRequirement = new GogoRequirement("org.apache.felix.gogo", implName);
			this.required = required;
		}

		void start(FrameworkWiring frameworkWiring) throws BundleException {
			Collection<BundleCapability> found = frameworkWiring.findProviders(implRequirement);
			if (found.isEmpty()) {
				found = frameworkWiring.findProviders(identityRequirement);
			}

			if (found.isEmpty()) {
				if (required) {
					throw new BundleException("Missing required gogo bundle: " + identityRequirement.getFilter());
				}
			} else {
				found.iterator().next().getRevision().getBundle().start(Bundle.START_TRANSIENT);
			}
		}

		class GogoRequirement implements Requirement {
			private final String namespace;
			private final String filter;
			GogoRequirement(String namespace, String value) {
				this.namespace = namespace;
				this.filter = "("+ getNamespace() + "=" + value + ")";
			}
			@Override
			public String getNamespace() {
				return namespace;
			}

			@Override
			public Map<String, String> getDirectives() {
				return Collections.singletonMap("filter", filter);
			}

			@Override
			public Map<String, Object> getAttributes() {
				return Collections.emptyMap();
			}

			@Override
			public Resource getResource() {
				return null;
			}

			String getFilter() {
				return filter;
			}
		}
	}
	private ServiceTracker<ConditionalPermissionAdmin, ConditionalPermissionAdmin> condPermAdminTracker;
	private ServiceTracker<PermissionAdmin, PermissionAdmin> permissionAdminTracker;
	private FrameworkStartLevel frameworkStartLevel;
	private FrameworkWiring frameworkWiring;
	@SuppressWarnings("deprecation")
	private ServiceTracker<PackageAdmin, PackageAdmin> packageAdminTracker;
	private static boolean isFirstProcessor = true;
	private static TelnetCommand telnetConnection = null;
	
	private ServiceTracker<CommandProcessor, ServiceTracker<ConsoleSession, CommandSession>> commandProcessorTracker;
	// Tracker for Equinox CommandProviders
	private ServiceTracker<CommandProvider, List<ServiceRegistration<?>>> commandProviderTracker;
	
	private EquinoxCommandProvider equinoxCmdProvider;

	public static class ProcessorCustomizer implements
			ServiceTrackerCustomizer<CommandProcessor, ServiceTracker<ConsoleSession, CommandSession>> {

		private final BundleContext context;

		public ProcessorCustomizer(BundleContext context) {
			this.context = context;
		}

		@Override
		public ServiceTracker<ConsoleSession, CommandSession> addingService(
				ServiceReference<CommandProcessor> reference) {
			CommandProcessor processor = context.getService(reference);
			if (processor == null)
				return null;
			
			if (isFirstProcessor) {
				isFirstProcessor = false;
				telnetConnection = new TelnetCommand(processor, context);
				telnetConnection.startService();
			} else {
				telnetConnection.addCommandProcessor(processor);
			}
			
			ServiceTracker<ConsoleSession, CommandSession> tracker = new ServiceTracker<>(context, ConsoleSession.class, new SessionCustomizer(context, processor));
			tracker.open();
			return tracker;
		}

		@Override
		public void modifiedService(
			ServiceReference<CommandProcessor> reference,
			ServiceTracker<ConsoleSession, CommandSession> service) {
			// nothing
		}

		@Override
		public void removedService(
			ServiceReference<CommandProcessor> reference,
			ServiceTracker<ConsoleSession, CommandSession> tracker) {
			tracker.close();
			CommandProcessor processor = context.getService(reference);
			telnetConnection.removeCommandProcessor(processor);
		}	
	}

	// Provides support for Equinox ConsoleSessions
	public static class SessionCustomizer implements
			ServiceTrackerCustomizer<ConsoleSession, CommandSession> {
		private final BundleContext context;
		final CommandProcessor processor;
		
		public SessionCustomizer(BundleContext context, CommandProcessor processor) {
			this.context = context;
			this.processor = processor;
		}

		@Override
		public CommandSession addingService(
				ServiceReference<ConsoleSession> reference) {
			final ConsoleSession equinoxSession = context.getService(reference);
			if (equinoxSession == null)
				return null;
			PrintStream output = new PrintStream(equinoxSession.getOutput());
			final CommandSession gogoSession = processor.createSession(equinoxSession.getInput(), output, output);
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						gogoSession.put("SCOPE", "equinox:*");
						gogoSession.put("prompt", "osgi> ");
						gogoSession.execute("gosh --login --noshutdown");
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					finally {
						gogoSession.close();
						equinoxSession.close();
					}
				}
			}, "Equinox Console Session").start();
			return null;
		}

		@Override
		public void modifiedService(ServiceReference<ConsoleSession> reference,
				CommandSession service) {
			// nothing
		}

		@Override
		public void removedService(ServiceReference<ConsoleSession> reference,
				CommandSession session) {
			session.close();
		}
	}

	// All commands, provided by an Equinox CommandProvider, are registered as provided by a CommandProviderAdapter.
	public class CommandCustomizer implements
			ServiceTrackerCustomizer<CommandProvider, List<ServiceRegistration<?>>> {

		private BundleContext context;
		public CommandCustomizer(BundleContext context) {
			this.context = context;
		}

		@Override
		public List<ServiceRegistration<?>> addingService(ServiceReference<CommandProvider> reference) {
			if (reference.getProperty("osgi.command.function") != null) {
				// must be a gogo function already; don' track
				return null;
			}
			CommandProvider command = context.getService(reference);
			try {
				Method[] commandMethods = getCommandMethods(command);

				if (commandMethods.length > 0) {
					List<ServiceRegistration<?>> registrations = new ArrayList<>();
					Dictionary<String, Object> attributes = getAttributes(commandMethods);
					Object serviceRanking = reference.getProperty(Constants.SERVICE_RANKING);
					if (serviceRanking != null) {
					    attributes.put(Constants.SERVICE_RANKING, serviceRanking);
					}
					registrations.add(context.registerService(Object.class, new CommandProviderAdapter(command, commandMethods), attributes));
					return registrations;
				} else {
					context.ungetService(reference);
					return null;
				}
			} catch (Exception e) {
				context.ungetService(reference);
				return null;
			}
		}


		@Override
		public void modifiedService(ServiceReference<CommandProvider> reference, List<ServiceRegistration<?>> service) {
			// Nothing to do.
		}

		@Override
		public void removedService(ServiceReference<CommandProvider> reference, List<ServiceRegistration<?>> registrations) {
			for (ServiceRegistration<?> serviceRegistration : registrations) {
				serviceRegistration.unregister();
			}
		}

	}

	@SuppressWarnings("deprecation")
	@Override
	public void start(BundleContext context) throws Exception {
		Bundle systemBundle = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
		frameworkStartLevel = systemBundle.adapt(FrameworkStartLevel.class);
		frameworkWiring = systemBundle.adapt(FrameworkWiring.class);

		commandProviderTracker = new ServiceTracker<>(context, CommandProvider.class, new CommandCustomizer(context));
		commandProviderTracker.open();
		commandProcessorTracker = new ServiceTracker<>(context, CommandProcessor.class, new ProcessorCustomizer(context));
		commandProcessorTracker.open();
		
		condPermAdminTracker = new ServiceTracker<>(context, ConditionalPermissionAdmin.class, null);
		condPermAdminTracker.open();

		// grab permission admin
		permissionAdminTracker = new ServiceTracker<>(context, PermissionAdmin.class, null);
		permissionAdminTracker.open();

		packageAdminTracker = new ServiceTracker<>(context, PackageAdmin.class, null);
		packageAdminTracker.open();
		
		equinoxCmdProvider = new EquinoxCommandProvider(context, this);
		equinoxCmdProvider.startService();
		
		HelpCommand helpCommand = new HelpCommand(context); 
		helpCommand.startService();
		
		ManCommand manCommand = new ManCommand(context);
		manCommand.startService();
		
		DisconnectCommand disconnectCommand = new DisconnectCommand(context);
		disconnectCommand.startService();
		
		CommandsTracker commandsTracker = new CommandsTracker(context);
		context.registerService(CommandsTracker.class.getName(), commandsTracker, null);

		GOGO.RUNTIME.start(frameworkWiring);
		GOGO.SHELL.start(frameworkWiring);
		GOGO.COMMAND.start(frameworkWiring);
	}

	public FrameworkStartLevel getStartLevel() {
		return frameworkStartLevel;
	}

	public PermissionAdmin getPermissionAdmin() {
		return getServiceFromTracker(permissionAdminTracker, PermissionAdmin.class);
	}

	public ConditionalPermissionAdmin getConditionalPermissionAdmin() {
		return getServiceFromTracker(condPermAdminTracker, ConditionalPermissionAdmin.class);
	}

	@SuppressWarnings("deprecation")
	public PackageAdmin getPackageAdmin() {
		return getServiceFromTracker(packageAdminTracker, PackageAdmin.class);
	}

	private static <T> T getServiceFromTracker(ServiceTracker<?, T> tracker, Class<T> serviceClass) {
		if (tracker == null)
			throw new IllegalStateException("Missing service: " + serviceClass);
		T result = tracker.getService();
		if (result == null)
			throw new IllegalStateException("Missing service: " + serviceClass);
		return result;
	}

	Method[] getCommandMethods(Object command) {
		ArrayList<Method> names = new ArrayList<>();
		Class<?> c = command.getClass();
		Method[] methods = c.getDeclaredMethods();
		for (Method method : methods) {
			if (method.getName().startsWith("_")
					&& method.getModifiers() == Modifier.PUBLIC && !method.getName().equals("_help")) {
				Type[] types = method.getGenericParameterTypes();
				if (types.length == 1
						&& types[0].equals(CommandInterpreter.class)) {
					names.add(method);
				}
			}
		}
		return names.toArray(new Method[names.size()]);
	}

	Dictionary<String, Object> getAttributes(Method[] commandMethods) {
		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put("osgi.command.scope", "equinox");
		String[] methodNames = new String[commandMethods.length];
		for (int i = 0; i < commandMethods.length; i++) {
			String methodName = commandMethods[i].getName().substring(1);
			methodNames[i] = methodName;
		}

		dict.put("osgi.command.function", methodNames);
		return dict;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		commandProviderTracker.close();
		commandProcessorTracker.close();
		if (equinoxCmdProvider != null) {
			equinoxCmdProvider.stopService();
		}

		try {
			telnetConnection.telnet(new String[]{"stop"});
		} catch (Exception e) {
			// expected if the telnet server is not started
		}

	}
}
