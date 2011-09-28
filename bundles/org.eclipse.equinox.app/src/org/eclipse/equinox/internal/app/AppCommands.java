/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alex Blewitt (bug 196071)
 *******************************************************************************/
package org.eclipse.equinox.internal.app;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.*;
import org.osgi.service.application.*;
import org.osgi.util.tracker.ServiceTracker;

public class AppCommands implements CommandProvider {
	private final static String LAUNCHABLE_APP_FILTER = "(&(application.locked=false)(application.launchable=true)(application.visible=true))"; //$NON-NLS-1$
	private final static String ACTIVE_APP_FILTER = "(!(application.state=STOPPING))"; //$NON-NLS-1$
	private final static String LOCKED_APP_FILTER = "(application.locked=true)"; //$NON-NLS-1$
	private final static String NEW_LINE = "\r\n"; //$NON-NLS-1$
	private final static String TAB = "\t"; //$NON-NLS-1$

	// holds the mappings from command name to command arguments and command description
	private Map commandsHelp = null;

	private static AppCommands instance;
	private BundleContext context;
	private ServiceTracker applicationDescriptors;
	private ServiceTracker applicationHandles;
	private ServiceTracker scheduledApplications;
	private Filter launchableApp;
	private Filter activeApp;
	private Filter lockedApp;
	private ServiceRegistration providerRegistration;

	static synchronized void create(BundleContext context) {
		if (instance != null)
			return;
		instance = new AppCommands();
		instance.start(context);
	}

	static synchronized void destroy(BundleContext context) {
		if (instance == null)
			return;
		instance.stop(context);
		instance = null;
	}

	protected AppCommands() {
		// empty
	}

	public void start(BundleContext ctx) {
		this.context = ctx;
		try {
			applicationDescriptors = new ServiceTracker(ctx, ApplicationDescriptor.class.getName(), null);
			applicationDescriptors.open();
			applicationHandles = new ServiceTracker(ctx, ApplicationHandle.class.getName(), null);
			applicationHandles.open();
			scheduledApplications = new ServiceTracker(ctx, ScheduledApplication.class.getName(), null);
			scheduledApplications.open();
			launchableApp = ctx.createFilter(LAUNCHABLE_APP_FILTER);
			activeApp = ctx.createFilter(ACTIVE_APP_FILTER);
			lockedApp = ctx.createFilter(LOCKED_APP_FILTER);
			providerRegistration = ctx.registerService(CommandProvider.class.getName(), this, null);
		} catch (InvalidSyntaxException e) {
			// should not happen.
		}
	}

	public void stop(BundleContext ctx) {
		providerRegistration.unregister();
		if (applicationDescriptors != null)
			applicationDescriptors.close();
		if (applicationHandles != null)
			applicationHandles.close();
		if (scheduledApplications != null)
			scheduledApplications.close();
	}

	public String getHelp() {
		return getHelp(null);
	}

	/*
	 * This method either returns the help message for a particular command, 
	 * or returns the help messages for all commands (if commandName is null)
	 */
	private String getHelp(String commandName) {
		StringBuffer sb = new StringBuffer();

		if (commandsHelp == null) {
			initializeCommandsHelp();
		}

		if (commandName != null) {
			if (commandsHelp.containsKey(commandName)) {
				addCommand(commandName, (String[]) commandsHelp.get(commandName), sb);
			}
			return sb.toString();
		}

		addHeader(Messages.console_help_app_commands_header, sb);
		Iterator i = commandsHelp.entrySet().iterator();
		while (i.hasNext()) {
			Entry entry = (Entry) i.next();
			String command = (String) entry.getKey();
			String[] attributes = (String[]) entry.getValue();
			addCommand(command, attributes, sb);
		}

		return sb.toString();
	}

	private void initializeCommandsHelp() {
		commandsHelp = new LinkedHashMap();
		commandsHelp.put("activeApps", new String[] {Messages.console_help_activeapps_description}); //$NON-NLS-1$
		commandsHelp.put("apps", new String[] {Messages.console_help_apps_description}); //$NON-NLS-1$
		commandsHelp.put("lockApp", new String[] {Messages.console_help_arguments, Messages.console_help_lockapp_description}); //$NON-NLS-1$
		commandsHelp.put("schedApp", new String[] {Messages.console_help_schedapp_arguments, Messages.console_help_schedapp_description}); //$NON-NLS-1$
		commandsHelp.put("startApp", new String[] {Messages.console_help_arguments, Messages.console_help_startapp_description}); //$NON-NLS-1$
		commandsHelp.put("stopApp", new String[] {Messages.console_help_arguments, Messages.console_help_stopapp_description}); //$NON-NLS-1$
		commandsHelp.put("unlockApp", new String[] {Messages.console_help_arguments, Messages.console_help_unlockapp_description}); //$NON-NLS-1$
		commandsHelp.put("unschedApp", new String[] {Messages.console_help_arguments, Messages.console_help_unschedapp_description}); //$NON-NLS-1$
	}

	/** Private helper method for getHelp.  Formats the help headers. */
	private void addHeader(String header, StringBuffer help) {
		help.append("---"); //$NON-NLS-1$
		help.append(header);
		help.append("---"); //$NON-NLS-1$
		help.append(NEW_LINE);
	}

	/** Private helper method for getHelp.  Formats the command descriptions. */
	private void addCommand(String command, String description, StringBuffer help) {
		help.append(TAB);
		help.append(command);
		help.append(" - "); //$NON-NLS-1$
		help.append(description);
		help.append(NEW_LINE);
	}

	/** Private helper method for getHelp.  Formats the command descriptions with command arguments. */
	private void addCommand(String command, String parameters, String description, StringBuffer help) {
		help.append(TAB);
		help.append(command);
		help.append(" "); //$NON-NLS-1$
		help.append(parameters);
		help.append(" - "); //$NON-NLS-1$
		help.append(description);
		help.append(NEW_LINE);
	}

	/** Private helper method for getHelp. According to its arguments chooses which one of the above addCommand methods to use. */
	private void addCommand(String command, String[] attributes, StringBuffer help) {
		if (attributes.length == 1) {
			addCommand(command, attributes[0], help);
		} else if (attributes.length == 2) {
			addCommand(command, attributes[0], attributes[1], help);
		}
	}

	private Dictionary getServiceProps(ServiceReference ref) {
		String[] keys = ref.getPropertyKeys();
		Hashtable props = new Hashtable(keys.length);
		for (int i = 0; i < keys.length; i++)
			props.put(keys[i], ref.getProperty(keys[i]));
		return props;
	}

	public void _apps(CommandInterpreter intp) {
		ServiceReference[] apps = applicationDescriptors.getServiceReferences();
		if (apps == null) {
			intp.println("No applications found."); //$NON-NLS-1$
			return;
		}
		for (int i = 0; i < apps.length; i++) {
			String application = (String) apps[i].getProperty(ApplicationDescriptor.APPLICATION_PID);
			intp.print(application);

			if (getApplication(applicationHandles.getServiceReferences(), application, ApplicationHandle.APPLICATION_DESCRIPTOR, true) != null)
				intp.print(" [running]"); //$NON-NLS-1$ 

			if (getApplication(scheduledApplications.getServiceReferences(), application, ScheduledApplication.APPLICATION_PID, true) != null)
				intp.print(" [scheduled]"); //$NON-NLS-1$ 

			if (!launchableApp.match(getServiceProps(apps[i])))
				intp.print(" [not launchable]"); //$NON-NLS-1$ 
			else
				intp.print(" [launchable]"); //$NON-NLS-1$ 

			if (lockedApp.match(getServiceProps(apps[i])))
				intp.print(" [locked]"); //$NON-NLS-1$ 
			intp.println();
		}
	}

	public void _activeApps(CommandInterpreter intp) {
		ServiceReference[] active = applicationHandles.getServiceReferences();
		if (active == null) {
			intp.println("No active applications found"); //$NON-NLS-1$
			return;
		}
		for (int i = 0; i < active.length; i++) {
			intp.print(active[i].getProperty(ApplicationHandle.APPLICATION_PID));
			intp.print(" ["); //$NON-NLS-1$
			intp.print(activeApp.match(getServiceProps(active[i])) ? "running" : "stopping"); //$NON-NLS-1$ //$NON-NLS-2$
			intp.println("]"); //$NON-NLS-1$
		}
	}

	private ServiceReference getApplication(ServiceReference[] apps, String targetId, String idKey, boolean perfectMatch) {
		if (apps == null || targetId == null)
			return null;

		ServiceReference result = null;
		boolean ambigous = false;
		for (int i = 0; i < apps.length; i++) {
			String id = (String) apps[i].getProperty(idKey);
			if (targetId.equals(id))
				return apps[i]; // always return a perfect match
			if (perfectMatch)
				continue;
			if (id.indexOf(targetId) >= 0) {
				if (result != null)
					ambigous = true;
				result = apps[i];
			}
		}
		return ambigous ? null : result;
	}

	public void _startApp(CommandInterpreter intp) throws Exception {
		String appId = intp.nextArgument();
		ServiceReference application = getApplication(applicationDescriptors.getServiceReferences(), appId, ApplicationDescriptor.APPLICATION_PID, false);
		if (application == null)
			intp.println("\"" + appId + "\" does not exist or is ambigous."); //$NON-NLS-1$ //$NON-NLS-2$
		else {
			ArrayList argList = new ArrayList();
			String arg = null;
			while ((arg = intp.nextArgument()) != null)
				argList.add(arg);
			String[] args = argList.size() == 0 ? null : (String[]) argList.toArray(new String[argList.size()]);
			try {
				HashMap launchArgs = new HashMap(1);
				if (args != null)
					launchArgs.put(IApplicationContext.APPLICATION_ARGS, args);
				ApplicationDescriptor appDesc = ((ApplicationDescriptor) context.getService(application));
				ApplicationHandle handle = appDesc.launch(launchArgs);
				intp.println("Launched application instance: " + handle.getInstanceId()); //$NON-NLS-1$
			} finally {
				context.ungetService(application);
			}
			return;
		}
	}

	public void _stopApp(CommandInterpreter intp) throws Exception {
		String appId = intp.nextArgument();
		// first search for the application instance id
		ServiceReference application = getApplication(applicationHandles.getServiceReferences(), appId, ApplicationHandle.APPLICATION_PID, false);
		if (application == null)
			application = getApplication(applicationHandles.getServiceReferences(), appId, ApplicationHandle.APPLICATION_DESCRIPTOR, false);
		if (application == null)
			intp.println("\"" + appId + "\" does not exist, is not running or is ambigous."); //$NON-NLS-1$ //$NON-NLS-2$
		else {
			if (activeApp.match(getServiceProps(application))) {
				try {
					ApplicationHandle appHandle = (ApplicationHandle) context.getService(application);
					appHandle.destroy();
					intp.println("Stopped application instance: " + appHandle.getInstanceId()); //$NON-NLS-1$
				} finally {
					context.ungetService(application);
				}
			} else {
				intp.println("Application instance is already stopping: " + application.getProperty(ApplicationHandle.APPLICATION_PID)); //$NON-NLS-1$
			}
			return;
		}
	}

	public void _lockApp(CommandInterpreter intp) throws Exception {
		String appId = intp.nextArgument();
		ServiceReference application = getApplication(applicationDescriptors.getServiceReferences(), appId, ApplicationDescriptor.APPLICATION_PID, false);
		if (application == null)
			intp.println("\"" + appId + "\" does not exist or is ambigous."); //$NON-NLS-1$ //$NON-NLS-2$
		else {
			try {
				ApplicationDescriptor appDesc = (ApplicationDescriptor) context.getService(application);
				appDesc.lock();
				intp.println("Locked application: " + appDesc.getApplicationId()); //$NON-NLS-1$
			} finally {
				context.ungetService(application);
			}
			return;
		}
	}

	public void _unlockApp(CommandInterpreter intp) throws Exception {
		String appId = intp.nextArgument();
		ServiceReference application = getApplication(applicationDescriptors.getServiceReferences(), appId, ApplicationDescriptor.APPLICATION_PID, false);
		if (application == null)
			intp.println("\"" + appId + "\" does not exist or is ambigous."); //$NON-NLS-1$ //$NON-NLS-2$
		else {
			try {
				ApplicationDescriptor appDesc = (ApplicationDescriptor) context.getService(application);
				appDesc.unlock();
				intp.println("Unlocked application: " + appDesc.getApplicationId()); //$NON-NLS-1$
			} finally {
				context.ungetService(application);
			}
			return;
		}
	}

	public void _schedApp(CommandInterpreter intp) throws Exception {
		String appId = intp.nextArgument();
		ServiceReference application = getApplication(applicationDescriptors.getServiceReferences(), appId, ApplicationDescriptor.APPLICATION_PID, false);
		if (application == null)
			intp.println("\"" + appId + "\" does not exist or is ambigous."); //$NON-NLS-1$ //$NON-NLS-2$
		else {
			try {
				ApplicationDescriptor appDesc = (ApplicationDescriptor) context.getService(application);
				String filter = intp.nextArgument();
				boolean recure = Boolean.valueOf(intp.nextArgument()).booleanValue();
				appDesc.schedule(null, null, "org/osgi/application/timer", filter, recure); //$NON-NLS-1$
				intp.println("Scheduled application: " + appDesc.getApplicationId()); //$NON-NLS-1$
			} finally {
				context.ungetService(application);
			}
			return;
		}
	}

	public void _unschedApp(CommandInterpreter intp) throws Exception {
		String appId = intp.nextArgument();
		ServiceReference application = getApplication(scheduledApplications.getServiceReferences(), appId, ScheduledApplication.APPLICATION_PID, false);
		if (application == null)
			intp.println("\"" + appId + "\" does not exist or is ambigous."); //$NON-NLS-1$ //$NON-NLS-2$
		else {
			try {
				ScheduledApplication schedApp = (ScheduledApplication) context.getService(application);
				schedApp.remove();
				intp.println("Unscheduled application: " + application.getProperty(ApplicationDescriptor.APPLICATION_PID)); //$NON-NLS-1$
			} finally {
				context.ungetService(application);
			}
		}
	}

	/**
	 * Handles the help command
	 * 
	 * @param intp
	 * @return description for a particular command or false if there is no command with the specified name
	 */
	public Object _help(CommandInterpreter intp) {
		String commandName = intp.nextArgument();
		if (commandName == null) {
			return Boolean.FALSE;
		}
		String help = getHelp(commandName);

		if (help.length() > 0) {
			return help;
		}
		return Boolean.FALSE;
	}
}
