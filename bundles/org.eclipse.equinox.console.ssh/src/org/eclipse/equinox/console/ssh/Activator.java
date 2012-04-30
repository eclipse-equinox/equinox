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
package org.eclipse.equinox.console.ssh;

import org.apache.felix.service.command.CommandProcessor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {
	
	private static BundleContext context;
	private static SshCommand sshConnection = null;
	private static boolean isFirstProcessor = true;
	
	private ServiceTracker<CommandProcessor, SshCommand> commandProcessorTracker;
	
	public static class ProcessorCustomizer implements ServiceTrackerCustomizer<CommandProcessor, SshCommand> {

		private final BundleContext context;

		public ProcessorCustomizer(BundleContext context) {
			this.context = context;
		}

		public SshCommand addingService(ServiceReference<CommandProcessor> reference) {
			CommandProcessor processor = context.getService(reference);
			if (processor == null)
				return null;

			if (isFirstProcessor) {
				isFirstProcessor = false;
				sshConnection = new SshCommand(processor, context);
				sshConnection.startService();
			} else {
				sshConnection.addCommandProcessor(processor);
			}

			return sshConnection;
		}

		public void modifiedService(
				ServiceReference<CommandProcessor> reference,
				SshCommand service) {
			// nothing
		}

		public void removedService(ServiceReference<CommandProcessor> reference, SshCommand service) {
			CommandProcessor processor = context.getService(reference);
			service.removeCommandProcessor(processor);
		}	
	}

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		context = bundleContext;
		commandProcessorTracker = new ServiceTracker<CommandProcessor, SshCommand>(context, CommandProcessor.class, new ProcessorCustomizer(context));
		commandProcessorTracker.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		commandProcessorTracker.close();
		
		try {
			sshConnection.ssh(new String[]{"stop"});
		} catch (Exception e) {
			// expected if the ssh server is not started
		}
	}

}
