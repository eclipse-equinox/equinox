/*******************************************************************************
* Copyright (c) 2021 Indel AG and others.
* 
* This program and the accompanying materials
* are made available under the terms of the Eclipse Public License 2.0
* which accompanies this distribution, and is available at
* https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
* 
* Contributors:
*     Indel AG - initial API and implementation
*******************************************************************************/
package org.eclipse.osgi.tests.appadmin;

import java.util.Collection;
import java.util.Map;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ApplicationException;
import org.osgi.service.application.ApplicationHandle;

public class RelaunchApp implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		final Map arguments = context.getArguments();

		// Setting eclipse.allowAppRelaunch to true at runtime should allow us to launch
		// multiple applications in sequence
		ServiceReference<EnvironmentInfo> envref = OSGiTestsActivator.getContext()
				.getServiceReference(EnvironmentInfo.class);
		EnvironmentInfo env = OSGiTestsActivator.getContext().getService(envref);
		if (Boolean.valueOf(env.getProperty("eclipse.allowAppRelaunch"))) { //$NON-NLS-1$
			throw new AssertionError("eclipse.allowAppRelaunch should not be set initially"); //$NON-NLS-1$
		}
		env.setProperty("eclipse.allowAppRelaunch", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		OSGiTestsActivator.getContext().ungetService(envref);

		// Get a handle for the running application so we can wait for it to exit
		ServiceReference<ApplicationHandle> thisAppRef = OSGiTestsActivator.getContext()
				.getServiceReference(ApplicationHandle.class);
		ApplicationHandle thisAppHandle = OSGiTestsActivator.getContext().getService(thisAppRef);

		new Thread("launcher") { //$NON-NLS-1$
			public void run() {
				// Wait for this application to exit
				try {
					thisAppHandle.getExitValue(0);
				} catch (ApplicationException e) {
					// does not occur for timeout 0
				} catch (InterruptedException e) {
					// I don't think this should occur
					e.printStackTrace();
				}

				// Get the descriptor for the actual test runner application.
				// Need a test runner that runs in the main thread to avoid race conditions.
				Collection<ServiceReference<ApplicationDescriptor>> testAppRefs = null;
				try {
					testAppRefs = OSGiTestsActivator.getContext().getServiceReferences(
							org.osgi.service.application.ApplicationDescriptor.class,
							"(" + Constants.SERVICE_PID + "=org.eclipse.pde.junit.runtime.nonuithreadtestapplication)"); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (InvalidSyntaxException e) {
					// shouldn't happen, the hardcoded filter expression
					// should be syntactically correct
					e.printStackTrace();
				}
				ServiceReference<ApplicationDescriptor> testAppRef = testAppRefs.iterator().next();
				ApplicationDescriptor testAppDescriptor = OSGiTestsActivator.getContext().getService(testAppRef);

				// Launch the new application
				// If it does launch, it will run some unrelated succeeding test
				// and thereby confirm that relaunching works.
				try {
					ApplicationHandle testAppHandle;
					// There is a race condition in that the previous
					// application may not have exited far enough yet for
					// the EclipseAppLauncher to allow launching of a new
					// application: Setting the exit value happens earlier
					// in EclipseAppLauncher.runApplication() (inside
					// EclipseAppHandle.run()) than releasing runningLock.
					// Unfortunately there is no way to wait for the
					// EclipseAppLauncher to be ready, so just try again
					// after a delay when that happens.
					while (true) {
						try {
							testAppHandle = testAppDescriptor.launch(arguments);
							break;
						} catch (IllegalStateException e) {
							Thread.sleep(100);
						}
					}

					// Wait for the test application to exit
					testAppHandle.getExitValue(0);
				} catch (ApplicationException | InterruptedException e) {
					// ApplicationException "The main thread is not available to launch the
					// application" can happen when the test fails
					e.printStackTrace();
				} finally {
					OSGiTestsActivator.getContext().ungetService(thisAppRef);
					OSGiTestsActivator.getContext().ungetService(testAppRef);

					try {
						// This will not return but cause the process to terminate
						OSGiTestsActivator.getContext().getBundle(0).stop();
					} catch (BundleException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();

		// If relaunching does not work, the process will end after this and the test
		// will end with an error "Test did not run". The "launcher" thread will be
		// killed wherever its execution happens to be, which is a race condition that
		// means that there may be various exceptions printed or not. However even if it
		// successfully got past testAppDescriptor.launch(), the test runner which wants
		// to run in the main thread will never actually run, so the test cannot
		// mistakenly succeed.
		return null;
	}

	@Override
	public void stop() {
	}

}
