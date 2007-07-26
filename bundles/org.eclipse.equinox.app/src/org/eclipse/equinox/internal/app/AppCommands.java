/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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
		StringBuffer sb = new StringBuffer();
		sb.append("\n---Application Admin Commands---\n"); //$NON-NLS-1$
		sb.append("\tactiveApps - lists all running application IDs\n"); //$NON-NLS-1$
		sb.append("\tapps - lists all installed application IDs\n"); //$NON-NLS-1$
		sb.append("\tlockApp <application id> - locks the specified application ID\n"); //$NON-NLS-1$
		sb.append("\tschedApp <application id> <time filter> [true|false] - schedules the specified application id to launch at the specified time filter.  Can optionally make the schedule recurring.\n"); //$NON-NLS-1$
		sb.append("\tstartApp <application id> - starts the specified application ID\n"); //$NON-NLS-1$
		sb.append("\tstopApp <application id> - stops the specified running application ID\n"); //$NON-NLS-1$
		sb.append("\tunlockApp <application id> - unlocks the specified application ID\n"); //$NON-NLS-1$
		sb.append("\tunschedApp <application id> - unschedules all scheduled applications with the specified application ID\n"); //$NON-NLS-1$
		return sb.toString();
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
}
