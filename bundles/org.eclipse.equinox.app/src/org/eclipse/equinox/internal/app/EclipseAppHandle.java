/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Josh Arnold - Bug 180080 Equinox Application Admin spec violations
 *******************************************************************************/

package org.eclipse.equinox.internal.app;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.runnable.ApplicationRunnable;
import org.eclipse.osgi.service.runnable.StartupMonitor;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.application.ApplicationException;
import org.osgi.service.application.ApplicationHandle;

/*
 * An ApplicationHandle that represents a single instance of a running eclipse application.
 */
public class EclipseAppHandle extends ApplicationHandle implements ApplicationRunnable, IApplicationContext {
	// Indicates the application is starting
	private static final int FLAG_STARTING = 0x01;
	// Indicates the application is active
	private static final int FLAG_ACTIVE = 0x02;
	// Indicates the application is stopping
	private static final int FLAG_STOPPING = 0x04;
	// Indicates the application is stopped
	private static final int FLAG_STOPPED = 0x08;
	private static final String STARTING = "org.eclipse.equinox.app.starting"; //$NON-NLS-1$
	private static final String STOPPED = "org.eclipse.equinox.app.stopped"; //$NON-NLS-1$
	private static final String PROP_ECLIPSE_EXITCODE = "eclipse.exitcode"; //$NON-NLS-1$
	private static final Object NULL_RESULT = new Object();

	private volatile ServiceRegistration handleRegistration;
	private int status = EclipseAppHandle.FLAG_STARTING;
	private final Map arguments;
	private Object application;
	private final Boolean defaultAppInstance;
	private Object result;
	private boolean setResult = false;
	private boolean setAsyncResult = false;
	private final boolean[] registrationLock = new boolean[] {true};

	/*
	 * Constructs a handle for a single running instance of a eclipse application.
	 */
	EclipseAppHandle(String instanceId, Map arguments, EclipseAppDescriptor descriptor) {
		super(instanceId, descriptor);
		defaultAppInstance = arguments == null || arguments.get(EclipseAppDescriptor.APP_DEFAULT) == null ? Boolean.FALSE : (Boolean) arguments.remove(EclipseAppDescriptor.APP_DEFAULT);
		if (arguments == null)
			this.arguments = new HashMap(2);
		else
			this.arguments = new HashMap(arguments);
	}

	synchronized public String getState() {
		switch (status) {
			case FLAG_STARTING :
				return STARTING;
			case FLAG_ACTIVE :
				return ApplicationHandle.RUNNING;
			case FLAG_STOPPING :
				return ApplicationHandle.STOPPING;
			case FLAG_STOPPED :
			default :
				// must only check this if the status is STOPPED; otherwise we throw exceptions before we have set the registration.
				if (getServiceRegistration() == null)
					throw new IllegalStateException(NLS.bind(Messages.application_error_state_stopped, getInstanceId()));
				return STOPPED;
		}
	}

	protected void destroySpecific() {
		// when this method is called we must force the application to exit.
		// first set the status to stopping
		setAppStatus(EclipseAppHandle.FLAG_STOPPING);
		// now force the application to stop
		IApplication app = getApplication();
		if (app != null)
			app.stop();
		// make sure the app status is stopped
		setAppStatus(EclipseAppHandle.FLAG_STOPPED);
	}

	void setServiceRegistration(ServiceRegistration sr) {
		synchronized (registrationLock) {
			this.handleRegistration = sr;
			registrationLock[0] = sr != null;
			registrationLock.notifyAll();
		}
	}

	private ServiceRegistration getServiceRegistration() {
		synchronized (registrationLock) {
			if (handleRegistration == null && registrationLock[0]) {
				try {
					registrationLock.wait(1000); // timeout after 1 second
				} catch (InterruptedException e) {
					// nothing
				}
			}
			return handleRegistration;
		}
	}

	ServiceReference getServiceReference() {
		ServiceRegistration reg = getServiceRegistration();
		if (reg == null)
			return null;
		try {
			return reg.getReference();
		} catch (IllegalStateException e) {
			return null; // this will happen if the service has been unregistered already
		}
	}

	/*
	 * Gets a snapshot of the current service properties.
	 */
	Dictionary getServiceProperties() {
		Dictionary props = new Hashtable(6);
		props.put(ApplicationHandle.APPLICATION_PID, getInstanceId());
		props.put(ApplicationHandle.APPLICATION_STATE, getState());
		props.put(ApplicationHandle.APPLICATION_DESCRIPTOR, getApplicationDescriptor().getApplicationId());
		props.put(EclipseAppDescriptor.APP_TYPE, ((EclipseAppDescriptor) getApplicationDescriptor()).getThreadTypeString());
		props.put(ApplicationHandle.APPLICATION_SUPPORTS_EXITVALUE, Boolean.TRUE);
		if (defaultAppInstance.booleanValue())
			props.put(EclipseAppDescriptor.APP_DEFAULT, defaultAppInstance);
		return props;
	}

	/*
	 * Changes the status of this handle.  This method will properly transition
	 * the state of this handle and will update the service registration accordingly.
	 */
	private synchronized void setAppStatus(int status) {
		if (this.status == status)
			return;
		if ((status & EclipseAppHandle.FLAG_STARTING) != 0)
			throw new IllegalArgumentException("Cannot set app status to starting"); //$NON-NLS-1$
		// if status is stopping and the context is already stopping then return
		if ((status & EclipseAppHandle.FLAG_STOPPING) != 0)
			if ((this.status & (EclipseAppHandle.FLAG_STOPPING | EclipseAppHandle.FLAG_STOPPED)) != 0)
				return;
		// change the service properties to reflect the state change.
		this.status = status;
		ServiceRegistration handleReg = getServiceRegistration();
		if (handleReg == null)
			return;
		handleReg.setProperties(getServiceProperties());
		// if the status is stopped then unregister the service
		if ((this.status & EclipseAppHandle.FLAG_STOPPED) != 0) {
			((EclipseAppDescriptor) getApplicationDescriptor()).getContainerManager().unlock(this);
			handleReg.unregister();
			setServiceRegistration(null);
		}
	}

	public Map getArguments() {
		return arguments;
	}

	public Object run(Object context) throws Exception {
		if (context != null) {
			// always force the use of the context if it is not null
			arguments.put(IApplicationContext.APPLICATION_ARGS, context);
		} else {
			// get the context from the arguments
			context = arguments.get(IApplicationContext.APPLICATION_ARGS);
			if (context == null) {
				// if context is null then use the args from CommandLineArgs
				context = CommandLineArgs.getApplicationArgs();
				arguments.put(IApplicationContext.APPLICATION_ARGS, context);
			}
		}
		Object tempResult = null;
		try {
			Object app;
			synchronized (this) {
				if ((status & (EclipseAppHandle.FLAG_STARTING | EclipseAppHandle.FLAG_STOPPING)) == 0)
					throw new ApplicationException(ApplicationException.APPLICATION_INTERNAL_ERROR, NLS.bind(Messages.application_instance_stopped, getInstanceId()));
				application = getConfiguration().createExecutableExtension("run"); //$NON-NLS-1$
				app = application;
				notifyAll();
			}
			if (app instanceof IApplication)
				tempResult = ((IApplication) app).start(this);
			else
				tempResult = EclipseAppContainer.callMethodWithException(app, "run", new Class[] {Object.class}, new Object[] {context}); //$NON-NLS-1$
			if (tempResult == null)
				tempResult = NULL_RESULT;
		} finally {
			tempResult = setInternalResult(tempResult, false, null);
		}

		if (Activator.DEBUG)
			System.out.println(NLS.bind(Messages.application_returned, (new String[] {getApplicationDescriptor().getApplicationId(), tempResult == null ? "null" : tempResult.toString()}))); //$NON-NLS-1$
		return tempResult;
	}

	private synchronized Object setInternalResult(Object result, boolean isAsync, IApplication tokenApp) {
		if (setResult)
			throw new IllegalStateException("The result of the application is already set."); //$NON-NLS-1$
		if (isAsync) {
			if (!setAsyncResult)
				throw new IllegalStateException("The application must return IApplicationContext.EXIT_ASYNC_RESULT to set asynchronous results."); //$NON-NLS-1$
			if (application != tokenApp)
				throw new IllegalArgumentException("The application is not the correct instance for this application context."); //$NON-NLS-1$
		} else {
			if (result == IApplicationContext.EXIT_ASYNC_RESULT) {
				setAsyncResult = true;
				return NULL_RESULT; // the result well be set with setResult
			}
		}
		this.result = result;
		setResult = true;
		application = null;
		notifyAll();
		// The application exited itself; notify the app context
		setAppStatus(EclipseAppHandle.FLAG_STOPPING); // must ensure the STOPPING event is fired
		setAppStatus(EclipseAppHandle.FLAG_STOPPED);
		// only set the exit code property if this is the default application
		// (bug 321386) only set the exit code if the result != null; when result == null we assume an exception was thrown
		if (isDefault() && result != null) {
			int exitCode = result instanceof Integer ? ((Integer) result).intValue() : 0;
			// Use the EnvironmentInfo Service to set properties
			Activator.setProperty(PROP_ECLIPSE_EXITCODE, Integer.toString(exitCode));
		}
		return result;
	}

	public void stop() {
		try {
			destroy();
		} catch (IllegalStateException e) {
			// Do nothing; we don't care that the application was already stopped
			// return with no error
		}

	}

	public void applicationRunning() {
		// first set the application handle status to running
		setAppStatus(EclipseAppHandle.FLAG_ACTIVE);
		// now run the splash handler
		final ServiceReference[] monitors = getStartupMonitors();
		if (monitors == null)
			return;
		SafeRunner.run(new ISafeRunnable() {
			public void handleException(Throwable e) {
				// just continue ... the exception has already been logged by
				// handleException(ISafeRunnable)
			}

			public void run() throws Exception {
				for (int i = 0; i < monitors.length; i++) {
					StartupMonitor monitor = (StartupMonitor) Activator.getContext().getService(monitors[i]);
					if (monitor != null) {
						monitor.applicationRunning();
						Activator.getContext().ungetService(monitors[i]);
					}
				}
			}
		});
	}

	private ServiceReference[] getStartupMonitors() {
		// assumes theStartupMonitor is available as a service
		// see EclipseStarter.publishSplashScreen
		ServiceReference[] refs = null;
		try {
			refs = Activator.getContext().getServiceReferences(StartupMonitor.class.getName(), null);
		} catch (InvalidSyntaxException e) {
			// ignore; this cannot happen
		}
		if (refs == null || refs.length == 0)
			return null;
		// Implement our own Comparator to sort services
		Arrays.sort(refs, new Comparator() {
			public int compare(Object o1, Object o2) {
				// sort in descending order
				// sort based on service ranking first; highest rank wins
				ServiceReference ref1 = (ServiceReference) o1;
				ServiceReference ref2 = (ServiceReference) o2;
				Object property = ref1.getProperty(Constants.SERVICE_RANKING);
				int rank1 = (property instanceof Integer) ? ((Integer) property).intValue() : 0;
				property = ref2.getProperty(Constants.SERVICE_RANKING);
				int rank2 = (property instanceof Integer) ? ((Integer) property).intValue() : 0;
				if (rank1 != rank2)
					return rank1 > rank2 ? -1 : 1;
				// rankings are equal; sort by id, lowest id wins
				long id1 = ((Long) (ref1.getProperty(Constants.SERVICE_ID))).longValue();
				long id2 = ((Long) (ref2.getProperty(Constants.SERVICE_ID))).longValue();
				return id2 > id1 ? -1 : 1;
			}
		});
		return refs;
	}

	private synchronized IApplication getApplication() {
		if (handleRegistration != null && application == null)
			// the handle has been initialized by the container but the launcher has not
			// gotten around to creating the application object and starting it yet.
			try {
				wait(5000); // timeout after a while in case there was an internal error and there will be no application created
			} catch (InterruptedException e) {
				// do nothing
			}
		return (IApplication) ((application instanceof IApplication) ? application : null);
	}

	private IConfigurationElement getConfiguration() {
		IExtension applicationExtension = ((EclipseAppDescriptor) getApplicationDescriptor()).getContainerManager().getAppExtension(getApplicationDescriptor().getApplicationId());
		if (applicationExtension == null)
			throw new RuntimeException(NLS.bind(Messages.application_notFound, getApplicationDescriptor().getApplicationId(), ((EclipseAppDescriptor) getApplicationDescriptor()).getContainerManager().getAvailableAppsMsg()));
		IConfigurationElement[] configs = applicationExtension.getConfigurationElements();
		if (configs.length == 0)
			throw new RuntimeException(NLS.bind(Messages.application_invalidExtension, getApplicationDescriptor().getApplicationId()));
		return configs[0];
	}

	public String getBrandingApplication() {
		IBranding branding = ((EclipseAppDescriptor) getApplicationDescriptor()).getContainerManager().getBranding();
		return branding == null ? null : branding.getApplication();
	}

	public Bundle getBrandingBundle() {
		IBranding branding = ((EclipseAppDescriptor) getApplicationDescriptor()).getContainerManager().getBranding();
		return branding == null ? null : branding.getDefiningBundle();

	}

	public String getBrandingDescription() {
		IBranding branding = ((EclipseAppDescriptor) getApplicationDescriptor()).getContainerManager().getBranding();
		return branding == null ? null : branding.getDescription();

	}

	public String getBrandingId() {
		IBranding branding = ((EclipseAppDescriptor) getApplicationDescriptor()).getContainerManager().getBranding();
		return branding == null ? null : branding.getId();
	}

	public String getBrandingName() {
		IBranding branding = ((EclipseAppDescriptor) getApplicationDescriptor()).getContainerManager().getBranding();
		return branding == null ? null : branding.getName();

	}

	public String getBrandingProperty(String key) {
		IBranding branding = ((EclipseAppDescriptor) getApplicationDescriptor()).getContainerManager().getBranding();
		return branding == null ? null : branding.getProperty(key);
	}

	boolean isDefault() {
		return defaultAppInstance.booleanValue();
	}

	public synchronized Object waitForResult(int timeout) {
		try {
			return getExitValue(timeout);
		} catch (ApplicationException e) {
			// return null
		} catch (InterruptedException e) {
			// return null
		}
		return null;
	}

	public synchronized Object getExitValue(long timeout) throws ApplicationException, InterruptedException {
		if (handleRegistration == null && application == null)
			return result;
		long startTime = System.currentTimeMillis();
		long delay = timeout;
		while (!setResult && (delay > 0 || timeout == 0)) {
			wait(delay); // only wait for the specified amount of time
			if (timeout > 0)
				delay -= (System.currentTimeMillis() - startTime);
		}
		if (result == null)
			throw new ApplicationException(ApplicationException.APPLICATION_EXITVALUE_NOT_AVAILABLE);
		if (result == NULL_RESULT)
			return null;
		return result;
	}

	public void setResult(Object result, IApplication application) {
		setInternalResult(result == null ? NULL_RESULT : result, true, application);
	}
}
