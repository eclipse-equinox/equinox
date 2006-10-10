/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.service.runnable.ApplicationRunnable;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
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
	private static final String STARTING = "starting"; //$NON-NLS-1$
	private static final String STOPPED = "stopped"; //$NON-NLS-1$
	private static final String PROP_ECLIPSE_EXITCODE = "eclipse.exitcode"; //$NON-NLS-1$

	private ServiceRegistration handleRegistration;
	private int status = EclipseAppHandle.FLAG_STARTING;
	private Map arguments;
	private Object application;

	/*
	 * Constructs a handle for a single running instance of a eclipse application.
	 */
	EclipseAppHandle(String instanceId, Map arguments, EclipseAppDescriptor descriptor) {
		super(instanceId, descriptor);
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
				return STOPPED;
		}
	}

	protected void destroySpecific() {
		// when this method is called we must force the application to exit.
		// first set the status to stopping
		setAppStatus(EclipseAppHandle.FLAG_STOPPING);
		// now force the appliction to stop
		IApplication app = getApplication();
		if (app != null)
			app.stop();
		// make sure the app status is stopped
		setAppStatus(EclipseAppHandle.FLAG_STOPPED);
	}

	void setServiceRegistration(ServiceRegistration sr) {
		this.handleRegistration = sr;
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
			throw new IllegalArgumentException("Cannot set app status to ACTIVE"); //$NON-NLS-1$
		// if status is stopping and the context is already stopping then return
		if ((status & EclipseAppHandle.FLAG_STOPPING) != 0)
			if ((this.status & (EclipseAppHandle.FLAG_STOPPING | EclipseAppHandle.FLAG_STOPPED)) != 0)
				return;
		// change the service properties to reflect the state change.
		this.status = status; 
		handleRegistration.setProperties(getServiceProperties());
		// if the status is stopped then unregister the service
		if ((this.status & EclipseAppHandle.FLAG_STOPPED) != 0) {
			handleRegistration.unregister();
			((EclipseAppDescriptor) getApplicationDescriptor()).getContainerManager().unlock(this);
		}
	}

	public Map getArguments() {
		return arguments;
	}

	public IProduct getProduct() {
		return ((EclipseAppDescriptor) getApplicationDescriptor()).getContainerManager().getProduct();
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
		Object result;
		try {
			Object app;
			synchronized (this) {
				application = getConfiguration().createExecutableExtension("run"); //$NON-NLS-1$
				app = application;
			}
			if (app instanceof IApplication)
				result = ((IApplication) app).start(this);
			else
				result = ((IPlatformRunnable) app).run(context);
		} finally {
			synchronized (this) {
				application = null;
			}
			// The application exited itself; notify the app context
			setAppStatus(EclipseAppHandle.FLAG_STOPPED);
		}
		int exitCode = result instanceof Integer ? ((Integer) result).intValue() : 0;
		// use the long way to set the property to compile against eeminimum
		System.getProperties().setProperty(PROP_ECLIPSE_EXITCODE, Integer.toString(exitCode));
		if (Activator.DEBUG)
			System.out.println(NLS.bind(Messages.application_returned, (new String[] {getApplicationDescriptor().getApplicationId(), result == null ? "null" : result.toString()}))); //$NON-NLS-1$
		return result;
	}

	public void stop() {
		destroy();
	}

	public void applicationRunning() {
		// first set the application handle status to running
		setAppStatus(EclipseAppHandle.FLAG_ACTIVE);
		// now run the splash handler
		final Runnable handler = getSplashHandler();
		if (handler == null)
			return;
		SafeRunner.run(new ISafeRunnable() {
			public void handleException(Throwable e) {
				// just continue ... the exception has already been logged by
				// handleException(ISafeRunnable)
			}

			public void run() throws Exception {
				handler.run();
			}
		});
	}

	private Runnable getSplashHandler() {
		ServiceReference[] ref;
		try {
			ref = Activator.getContext().getServiceReferences(Runnable.class.getName(), "(name=splashscreen)"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			return null;
		}
		if (ref == null)
			return null;
		// assumes the endInitializationHandler is available as a service
		// see EclipseStarter.publishSplashScreen
		for (int i = 0; i < ref.length; i++) {
			Runnable result = (Runnable) Activator.getContext().getService(ref[i]);
			if (result != null) {
				Activator.getContext().ungetService(ref[i]); // immediately unget the service because we are not using it long
				return result;
			}
		}
		return null;
	}

	private IApplication getApplication() {
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
}
