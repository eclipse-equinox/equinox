/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.app;

import java.util.*;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.*;
import org.osgi.service.application.*;
import org.osgi.util.tracker.ServiceTracker;

public class AppCommands implements CommandProvider {
	private final static String LAUNCHABLE_APP_FILTER = "(&(application.locked=false)(application.launchable=true)(application.visible=true))"; //$NON-NLS-1$
	private final static String ACTIVE_APP_FILTER = "(!(application.state=STOPPING))"; //$NON-NLS-1$

	private static AppCommands instance;
	private BundleContext context;
	private ServiceTracker appDescTracker;
	private ServiceTracker appHandleTracker;
	private ServiceTracker schedAppTracker;
	private Filter launchableApp;
	private Filter activeApp;
	private ServiceRegistration sr;

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
			appDescTracker = new ServiceTracker(ctx, ApplicationDescriptor.class.getName(), null);
			appDescTracker.open();
			appHandleTracker = new ServiceTracker(ctx, ApplicationHandle.class.getName(), null);
			appHandleTracker.open();
			schedAppTracker = new ServiceTracker(ctx, ScheduledApplication.class.getName(), null);
			schedAppTracker.open();
			launchableApp = ctx.createFilter(LAUNCHABLE_APP_FILTER);
			activeApp = ctx.createFilter(ACTIVE_APP_FILTER);
			sr = ctx.registerService(CommandProvider.class.getName(), this, null);
		} catch (InvalidSyntaxException e) {
			// should not happen.
		}
	}

	public void stop(BundleContext ctx) {
		sr.unregister();
		if (appDescTracker != null)
			appDescTracker.close();
		if (appHandleTracker != null)
			appHandleTracker.close();
		if (schedAppTracker != null)
			schedAppTracker.close();
	}

	public String getHelp() {
		return null;
	}

	private Dictionary getServiceProps(ServiceReference ref) {
		String[] keys = ref.getPropertyKeys();
		Hashtable props = new Hashtable(keys.length);
		for (int i = 0; i < keys.length; i++)
			props.put(keys[i], ref.getProperty(keys[i]));
		return props;
	}

	public void _apps(CommandInterpreter intp) {
		ServiceReference[] apps = appDescTracker.getServiceReferences();
		if (apps == null) {
			intp.println("No applications found."); //$NON-NLS-1$
			return;
		}
		for (int i = 0; i < apps.length; i++) {
			intp.print(apps[i].getProperty(ApplicationDescriptor.APPLICATION_PID));
			intp.print(" ["); //$NON-NLS-1$
			intp.print(launchableApp.match(getServiceProps(apps[i])) ? "enabled" : "disabled"); //$NON-NLS-1$ //$NON-NLS-2$
			intp.println("]"); //$NON-NLS-1$
		}
	}

	public void _activeApps(CommandInterpreter intp) {
		ServiceReference[] active = appHandleTracker.getServiceReferences();
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

	public void _startApp(CommandInterpreter intp) throws Exception {
		String appId = intp.nextArgument();
		ServiceReference[] apps = appDescTracker.getServiceReferences();
		if (apps != null)
			for (int i = 0; i < apps.length; i++)
				if (appId.equals(apps[i].getProperty(ApplicationDescriptor.APPLICATION_PID))) {
					if (launchableApp.match(getServiceProps(apps[i]))) {
						ApplicationDescriptor appDesc = (ApplicationDescriptor) context.getService(apps[i]);
						try {
							appDesc.launch(new HashMap(0));
							intp.println("Launched application: " + appId); //$NON-NLS-1$
						} finally {
							context.ungetService(apps[i]);
						}
					} else {
						intp.println("Application is not enabled: " + appId); //$NON-NLS-1$
					}
					return;
				}
		intp.println("No application with the id \"" + appId + "\" exists."); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void _stopApp(CommandInterpreter intp) throws Exception {
		String runningId = intp.nextArgument();
		ServiceReference[] runningApps = appHandleTracker.getServiceReferences();
		if (runningApps != null)
			for (int i = 0; i < runningApps.length; i++)
				if (runningId.equals(runningApps[i].getProperty(ApplicationHandle.APPLICATION_PID))) {
					if (activeApp.match(getServiceProps(runningApps[i]))) {
						try {
							ApplicationHandle appDesc = (ApplicationHandle) context.getService(runningApps[i]);
							appDesc.destroy();
							intp.println("Stopped application: " + runningId); //$NON-NLS-1$
						} finally {
							context.ungetService(runningApps[i]);
						}
					} else {
						intp.println("Applicationi is already stopping: " + runningId); //$NON-NLS-1$
					}
					return;
				}
		intp.println("No running application with the id \"" + runningId + "\" exists."); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void _lockApp(CommandInterpreter intp) throws Exception {
		String appId = intp.nextArgument();
		ServiceReference[] apps = appDescTracker.getServiceReferences();
		if (apps != null)
			for (int i = 0; i < apps.length; i++)
				if (appId.equals(apps[i].getProperty(ApplicationDescriptor.APPLICATION_PID))) {
					try {
						ApplicationDescriptor appDesc = (ApplicationDescriptor) context.getService(apps[i]);
						appDesc.lock();
						intp.println("Locked application: " + appId); //$NON-NLS-1$
					} finally {
						context.ungetService(apps[i]);
					}
					return;
				}
		intp.println("No application with the id \"" + appId + "\" exists."); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void _unlockApp(CommandInterpreter intp) throws Exception {
		String appId = intp.nextArgument();
		ServiceReference[] apps = appDescTracker.getServiceReferences();
		if (apps != null)
			for (int i = 0; i < apps.length; i++)
				if (appId.equals(apps[i].getProperty(ApplicationDescriptor.APPLICATION_PID))) {
					try {
						ApplicationDescriptor appDesc = (ApplicationDescriptor) context.getService(apps[i]);
						appDesc.unlock();
						intp.println("Unlocked application: " + appId); //$NON-NLS-1$
					} finally {
						context.ungetService(apps[i]);
					}
					return;
				}
		intp.println("No application with the id \"" + appId + "\" exists."); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void _schedApp(CommandInterpreter intp) throws Exception {
		String appId = intp.nextArgument();
		ServiceReference[] apps = appDescTracker.getServiceReferences();
		if (apps != null)
			for (int i = 0; i < apps.length; i++)
				if (appId.equals(apps[i].getProperty(ApplicationDescriptor.APPLICATION_PID))) {
					try {
						ApplicationDescriptor appDesc = (ApplicationDescriptor) context.getService(apps[i]);
						String filter = intp.nextArgument();
						boolean recure = Boolean.valueOf(intp.nextArgument()).booleanValue();
						appDesc.schedule(null, "org/osgi/application/timer", filter, recure); //$NON-NLS-1$
						intp.println("scheduled application: " + appId); //$NON-NLS-1$
					} finally {
						context.ungetService(apps[i]);
					}
					return;
				}
		intp.println("No application with the id \"" + appId + "\" exists."); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void _unschedApp(CommandInterpreter intp) throws Exception {
		String schedId = intp.nextArgument();
		ServiceReference[] scheds = schedAppTracker.getServiceReferences();
		if (scheds != null)
			for (int i = 0; i < scheds.length; i++) {
				ScheduledApplication schedApp = (ScheduledApplication) context.getService(scheds[i]);
				try {
					if (schedId.equals(schedApp.getApplicationDescriptor().getApplicationId()))
						schedApp.remove();
				} finally {
					context.ungetService(scheds[i]);
				}
			}
		
	}
}
