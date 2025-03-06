/*******************************************************************************
 * Copyright (c) 2011, 2017 SAP AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

		@Override
		public SshCommand addingService(ServiceReference<CommandProcessor> reference) {
			CommandProcessor processor = context.getService(reference);
			if (processor == null) {
				return null;
			}

			if (isFirstProcessor) {
				isFirstProcessor = false;
				sshConnection = new SshCommand(processor, context);
				sshConnection.startService();
			} else {
				sshConnection.addCommandProcessor(processor);
			}

			return sshConnection;
		}

		@Override
		public void modifiedService(ServiceReference<CommandProcessor> reference, SshCommand service) {
			// nothing
		}

		@Override
		public void removedService(ServiceReference<CommandProcessor> reference, SshCommand service) {
			CommandProcessor processor = context.getService(reference);
			service.removeCommandProcessor(processor);
		}
	}

	static BundleContext getContext() {
		return context;
	}

	@Override
	public void start(BundleContext bundleContext) throws Exception {
		context = bundleContext;
		commandProcessorTracker = new ServiceTracker<>(context, CommandProcessor.class,
				new ProcessorCustomizer(context));
		commandProcessorTracker.open();
	}

	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		commandProcessorTracker.close();

		try {
			sshConnection.ssh(new String[] { "stop" });
		} catch (Exception e) {
			// expected if the ssh server is not started
		}
	}

}
