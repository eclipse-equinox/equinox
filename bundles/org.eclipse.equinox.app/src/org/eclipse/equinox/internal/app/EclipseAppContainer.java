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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.service.runnable.ApplicationLauncher;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.application.*;

/*
 * A MEG application container that understands eclipse applications.  This 
 * container will discover installed eclipse applications and register the 
 * appropriate ApplicatoinDescriptor service with the service registry.
 */
public class EclipseAppContainer implements IRegistryChangeListener, SynchronousBundleListener {
	private static final String PI_RUNTIME = "org.eclipse.core.runtime"; //$NON-NLS-1$
	private static final String PT_APPLICATIONS = "applications"; //$NON-NLS-1$
	private static final String PT_APP_VISIBLE = "visible"; //$NON-NLS-1$
	private static final String PT_APP_THREAD = "thread"; //$NON-NLS-1$
	private static final String PT_APP_THREAD_MAIN = "main"; //$NON-NLS-1$
	private static final String PT_APP_THREAD_ANY = "any"; //$NON-NLS-1$
	private static final String PT_APP_CARDINALITY = "cardinality"; //$NON-NLS-1$
	private static final String PT_APP_CARDINALITY_SINGLETON_GLOBAL = "singleton-global"; //$NON-NLS-1$
	private static final String PT_APP_CARDINALITY_SINGLETON_SCOPED = "singleton-scoped"; //$NON-NLS-1$
	private static final String PT_APP_CARDINALITY_UNLIMITED = "*"; //$NON-NLS-1$
	private static final String PT_PRODUCTS = "products"; //$NON-NLS-1$
	private static final String EXT_ERROR_APP = "org.eclipse.equinox.app.error"; //$NON-NLS-1$

	private static final String PROP_PRODUCT = "eclipse.product"; //$NON-NLS-1$
	private static final String PROP_ECLIPSE_APPLICATION = "eclipse.application"; //$NON-NLS-1$
	private static final String PROP_ECLIPSE_APPLICATION_LAUNCH_DEFAULT = "eclipse.application.launchDefault"; //$NON-NLS-1$
	private static final String PROP_ECLIPSE_REGISTER_APP_DESC = "eclipse.application.registerDescriptors"; //$NON-NLS-1$

	static final int NOT_LOCKED = 0;
	static final int LOCKED_SINGLETON_GLOBAL_RUNNING = 1;
	static final int LOCKED_SINGLETON_GLOBAL_APPS_RUNNING = 2;
	static final int LOCKED_SINGLETON_SCOPED_RUNNING = 3;
	static final int LOCKED_SINGLETON_LIMITED_RUNNING = 4;
	static final int LOCKED_MAIN_THREAD_RUNNING = 5;

	BundleContext context;
	// A map of ApplicationDescriptors keyed by eclipse application ID
	private HashMap apps = new HashMap();

	private IExtensionRegistry extensionRegistry;
	private ApplicationLauncher appLauncher;
	private IBranding branding;
	private boolean missingProductReported;

	// the currently active application handles
	private Collection activeHandles = new ArrayList();
	private EclipseAppHandle activeMain;
	private EclipseAppHandle activeGlobalSingleton;
	private EclipseAppHandle activeScopedSingleton;
	private HashMap activeLimited;

	public EclipseAppContainer(BundleContext context, IExtensionRegistry extensionRegistry, ApplicationLauncher appLauncher) {
		this.context = context;
		this.extensionRegistry = extensionRegistry;
		this.appLauncher = appLauncher;
	}

	void start() {
		extensionRegistry.addRegistryChangeListener(this);
		// need to listen for system bundle stopping
		context.addBundleListener(this);
		String startDefaultProp = context.getProperty(EclipseAppContainer.PROP_ECLIPSE_APPLICATION_LAUNCH_DEFAULT);
		if (startDefaultProp == null || "true".equalsIgnoreCase(context.getProperty(EclipseAppContainer.PROP_ECLIPSE_APPLICATION_LAUNCH_DEFAULT))) { //$NON-NLS-1$
			boolean registerDescs = "true".equalsIgnoreCase(context.getProperty(EclipseAppContainer.PROP_ECLIPSE_REGISTER_APP_DESC)); //$NON-NLS-1$
			// register all the descriptors if requested to
			if (registerDescs)
				registerAppDescriptors();
			// Start the default application
			try {
				startDefaultApp();
			} catch (ApplicationException e) {
				Activator.log(new FrameworkLogEntry(Activator.PI_APP, FrameworkLogEntry.ERROR, 0, Messages.application_errorStartDefault, 0, e, null));
				if (!registerDescs)
					// if an error occurred then register all desciptors if they were not registered already.
					registerAppDescriptors();
			}
		} else {
			// we are not running the default application; we should register all applications
			registerAppDescriptors();
		}
	}

	void stop() {
		// stop all applications
		stopAllApps();
		context.removeBundleListener(this);
		extensionRegistry.removeRegistryChangeListener(this);
		// flush the apps
		apps.clear();
		branding = null;
		missingProductReported = false;
	}

	/*
	 * Only used to find the default application
	 */
	private EclipseAppDescriptor getAppDescriptor(String applicationId) {
		EclipseAppDescriptor result = null;
		synchronized (apps) {
			result = (EclipseAppDescriptor) apps.get(applicationId);
		}
		if (result == null) {
			registerAppDescriptor(applicationId); // try again just in case we are waiting for an event
			synchronized (apps) {
				result = (EclipseAppDescriptor) apps.get(applicationId);
			}
		}
		return result;
	}

	private EclipseAppDescriptor createAppDescriptor(IExtension appExtension) {
		synchronized (apps) {
			EclipseAppDescriptor appDescriptor = (EclipseAppDescriptor) apps.get(appExtension.getUniqueIdentifier());
			if (appDescriptor != null)
				return appDescriptor;
			// the appDescriptor does not exist for the app ID; create it
			IConfigurationElement[] configs = appExtension.getConfigurationElements();
			int flags = EclipseAppDescriptor.FLAG_CARD_SINGLETON_GLOGAL | EclipseAppDescriptor.FLAG_VISIBLE | EclipseAppDescriptor.FLAG_TYPE_MAIN_THREAD;
			int cardinality = 0;
			if (configs.length > 0) {
				String sVisible = configs[0].getAttribute(PT_APP_VISIBLE);
				if (sVisible != null && !Boolean.valueOf(sVisible).booleanValue())
					flags &= ~(EclipseAppDescriptor.FLAG_VISIBLE);
				String sThread = configs[0].getAttribute(PT_APP_THREAD);
				if (PT_APP_THREAD_ANY.equals(sThread)) {
					flags |= EclipseAppDescriptor.FLAG_TYPE_ANY_THREAD;
					flags &= ~(EclipseAppDescriptor.FLAG_TYPE_MAIN_THREAD);
				}
				String sCardinality = configs[0].getAttribute(PT_APP_CARDINALITY);
				if (sCardinality != null) {
					flags &= ~(EclipseAppDescriptor.FLAG_CARD_SINGLETON_GLOGAL); // clear the global bit
					if (PT_APP_CARDINALITY_SINGLETON_SCOPED.equals(sCardinality))
						flags |= EclipseAppDescriptor.FLAG_CARD_SINGLETON_SCOPED;
					else if (PT_APP_CARDINALITY_UNLIMITED.equals(sCardinality))
						flags |= EclipseAppDescriptor.FLAG_CARD_UNLIMITED;
					else if (PT_APP_CARDINALITY_SINGLETON_GLOBAL.equals(sCardinality))
						flags |= EclipseAppDescriptor.FLAG_CARD_SINGLETON_GLOGAL;
					else {
						try {
							cardinality = Integer.parseInt(sCardinality);
							flags |= EclipseAppDescriptor.FLAG_CARD_LIMITED;
						} catch (NumberFormatException e) {
							// TODO should we log this?
							// just fall back to the default
							flags |= EclipseAppDescriptor.FLAG_CARD_SINGLETON_GLOGAL;
						}
					}
				}
			}
			appDescriptor = new EclipseAppDescriptor(Activator.getBundle(appExtension.getContributor()), appExtension.getUniqueIdentifier(), flags, cardinality, this);
			// register the appDescriptor as a service
			ServiceRegistration sr = (ServiceRegistration) AccessController.doPrivileged(new RegisterService(new String[] {ApplicationDescriptor.class.getName()}, appDescriptor, appDescriptor.getServiceProperties()));
			appDescriptor.setServiceRegistration(sr);
			// save the app descriptor in the cache
			apps.put(appExtension.getUniqueIdentifier(), appDescriptor);
			return appDescriptor;
		}
	}

	private EclipseAppDescriptor removeAppDescriptor(String applicationId) {
		synchronized (apps) {
			EclipseAppDescriptor appDescriptor = (EclipseAppDescriptor) apps.remove(applicationId);
			if (appDescriptor == null)
				return null;
			appDescriptor.unregister();
			return appDescriptor;
		}
	}

	/*
	 * Gives access to the RegisterService privileged action.
	 */
	PrivilegedAction getRegServiceAction(String[] serviceClasses, Object serviceObject, Dictionary serviceProps) {
		return new RegisterService(serviceClasses, serviceObject, serviceProps);
	}

	/*
	 * PrivilegedAction used to register ApplicationDescriptor and ApplicationHandle services
	 */
	private class RegisterService implements PrivilegedAction {
		String[] serviceClasses;
		Object serviceObject;
		Dictionary serviceProps;

		RegisterService(String[] serviceClasses, Object serviceObject, Dictionary serviceProps) {
			this.serviceClasses = serviceClasses;
			this.serviceObject = serviceObject;
			this.serviceProps = serviceProps;
		}

		public Object run() {
			return context.registerService(serviceClasses, serviceObject, serviceProps);
		}
	}

	private void startDefaultApp() throws ApplicationException {
		// find the default application
		String applicationId = getApplicationId();
		EclipseAppDescriptor defaultDesc = null;
		Map args = null;
		if (applicationId == null) {
			// the application id is not set; use a descriptor that will throw an exception
			args = new HashMap(2);
			args.put(ErrorApplication.ERROR_EXCEPTION, new RuntimeException(Messages.application_noIdFound));
			defaultDesc = getAppDescriptor(EXT_ERROR_APP);
		} else {
			defaultDesc = getAppDescriptor(applicationId);
			if (defaultDesc == null) {
				// the application id is not available in the registry; use a descriptor that will throw an exception
				args = new HashMap(2);
				args.put(ErrorApplication.ERROR_EXCEPTION, new RuntimeException(NLS.bind(Messages.application_notFound, applicationId, getAvailableAppsMsg())));
				defaultDesc = getAppDescriptor(EXT_ERROR_APP);
			}
		}
		if (defaultDesc != null)
			defaultDesc.launch(args);
		else
			throw new ApplicationException(ApplicationException.APPLICATION_INTERNAL_ERROR, Messages.application_noIdFound);
	}

	/*
	 * Registers an ApplicationDescriptor service for each eclipse application
	 * available in the extension registry.
	 */
	private void registerAppDescriptors() {
		IExtension[] availableApps = getAvailableAppExtensions();
		for (int i = 0; i < availableApps.length; i++)
			createAppDescriptor(availableApps[i]);
	}

	private void registerAppDescriptor(String applicationId) {
		IExtension appExtension = getAppExtension(applicationId);
		if (appExtension != null)
			createAppDescriptor(appExtension);
	}

	/*
	 * Returns a list of all the available application IDs which are available 
	 * in the extension registry.
	 */
	private IExtension[] getAvailableAppExtensions() {
		IExtensionPoint point = extensionRegistry.getExtensionPoint(PI_RUNTIME + '.' + PT_APPLICATIONS);
		if (point == null)
			return new IExtension[0];
		return point.getExtensions();
	}

	String getAvailableAppsMsg() {
		IExtension[] availableApps = getAvailableAppExtensions();
		String availableAppsMsg = "<NONE>"; //$NON-NLS-1$
		if (availableApps.length != 0) {
			availableAppsMsg = availableApps[0].getUniqueIdentifier();
			for (int i = 1; i < availableApps.length; i++)
				availableAppsMsg = availableAppsMsg + ", " + availableApps[i].getUniqueIdentifier(); //$NON-NLS-1$
		}
		return availableAppsMsg;
	}

	/*
	 * Returns the application extension for the specified applicaiton ID.
	 * A RuntimeException is thrown if the extension does not exist for the
	 * given application ID.
	 */
	IExtension getAppExtension(String applicationId) {
		return extensionRegistry.getExtension(PI_RUNTIME, PT_APPLICATIONS, applicationId);
	}

	void launch(EclipseAppHandle appHandle) throws Exception {
		lock(appHandle);
		if (((EclipseAppDescriptor) appHandle.getApplicationDescriptor()).getThreadType() == EclipseAppDescriptor.FLAG_TYPE_MAIN_THREAD) {
			// use the ApplicationLauncher provided by the framework 
			// to ensure it is launched on the main thread
			if (appLauncher == null)
				throw new IllegalStateException();
			appLauncher.launch(appHandle, appHandle.getArguments().get(IApplicationContext.APPLICATION_ARGS));
		} else {
			AnyThreadAppLauncher.launchEclipseApplication(appHandle);
		}
	}

	public void registryChanged(IRegistryChangeEvent event) {
		processAppDeltas(event.getExtensionDeltas(PI_RUNTIME, PT_APPLICATIONS));
		processAppDeltas(event.getExtensionDeltas(Activator.PI_APP, PT_APPLICATIONS));
	}

	private void processAppDeltas(IExtensionDelta[] deltas) {
		for (int i = 0; i < deltas.length; i++) {
			switch (deltas[i].getKind()) {
				case IExtensionDelta.ADDED :
					createAppDescriptor(deltas[i].getExtension());
					break;
				case IExtensionDelta.REMOVED :
					removeAppDescriptor(deltas[i].getExtension().getUniqueIdentifier());
					break;
			}
		}
	}

	public void bundleChanged(BundleEvent event) {
		// if this is not the system bundle stopping then ignore the event
		if ((BundleEvent.STOPPING & event.getType()) == 0 || event.getBundle().getBundleId() != 0)
			return;
		// The system bundle is stopping; better stop all applications and containers now
		stopAllApps();
	}

	private void stopAllApps() {
		// get a stapshot of running applications
		try {
			ServiceReference[] runningRefs = context.getServiceReferences(ApplicationHandle.class.getName(), "(!(application.state=STOPPING))"); //$NON-NLS-1$
			if (runningRefs != null)
				for (int i = 0; i < runningRefs.length; i++) {
					ApplicationHandle handle = (ApplicationHandle) context.getService(runningRefs[i]);
					try {
						handle.destroy();
					} catch (Throwable t) {
						String message = NLS.bind(Messages.application_error_stopping, handle.getInstanceId());
						Activator.log(new FrameworkLogEntry(Activator.PI_APP, FrameworkLogEntry.WARNING, 0, message, 0, t, null));
					}
				}
		} catch (InvalidSyntaxException e) {
			// do nothing; we already tested the filter string above
		}
	}

	String getApplicationId() {
		// try commandLineProperties
		String applicationId = CommandLineArgs.getApplication();
		if (applicationId != null)
			return applicationId;

		// try bundleContext properties
		applicationId = context.getProperty(EclipseAppContainer.PROP_ECLIPSE_APPLICATION);
		if (applicationId != null)
			return applicationId;

		//Derive the application from the product information
		return getBranding() == null ? null : getBranding().getApplication();
	}

	public IBranding getBranding() {
		if (branding != null)
			return branding;
		// try commandLineProperties
		String productId = CommandLineArgs.getProduct();
		if (productId == null) {
			// try bundleContext properties
			if (context == null)
				return null;
			productId = context.getProperty(PROP_PRODUCT);
			if (productId == null)
				return null;
		}
		IConfigurationElement[] entries = extensionRegistry.getConfigurationElementsFor(PI_RUNTIME, PT_PRODUCTS, productId);
		if (entries.length > 0) {
			// There should only be one product with the given id so just take the first element
			branding = new ProductExtensionBranding(productId, entries[0]);
			return branding;
		}
		IConfigurationElement[] elements = extensionRegistry.getConfigurationElementsFor(PI_RUNTIME, PT_PRODUCTS);
		List logEntries = null;
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equalsIgnoreCase("provider")) { //$NON-NLS-1$
				try {
					Object provider = element.createExecutableExtension("run"); //$NON-NLS-1$
					Object[] products = (Object[]) EclipseAppContainer.callMethod(provider, "getProducts", null, null); //$NON-NLS-1$
					for (int j = 0; j < products.length; j++) {
						if (productId.equalsIgnoreCase((String) EclipseAppContainer.callMethod(products[j], "getId", null, null))) {
							branding = new ProviderExtensionBranding(products[j]);
							return branding;
						}
					}
				} catch (CoreException e) {
					if (logEntries == null)
						logEntries = new ArrayList(3);
					logEntries.add(new FrameworkLogEntry(Activator.PI_APP, NLS.bind(Messages.provider_invalid, element.getParent().toString()), 0, e, null));
				}
			}
		}
		if (logEntries != null)
			Activator.log(new FrameworkLogEntry(Activator.PI_APP, Messages.provider_invalid_general, 0, null, (FrameworkLogEntry[]) logEntries.toArray(new FrameworkLogEntry[logEntries.size()])));

		if (!missingProductReported) {
			Activator.log(new FrameworkLogEntry(Activator.PI_APP, NLS.bind(Messages.product_notFound, productId), 0, null, null));
			missingProductReported = true;
		}
		return null;
	}

	private void refreshAppDescriptors() {
		synchronized (apps) {
			for (Iterator allApps = apps.values().iterator(); allApps.hasNext();)
				((EclipseAppDescriptor) allApps.next()).refreshProperties();
		}
	}

	synchronized void lock(EclipseAppHandle appHandle) throws ApplicationException {
		EclipseAppDescriptor eclipseApp = (EclipseAppDescriptor) appHandle.getApplicationDescriptor();
		switch (isLocked(eclipseApp)) {
			case NOT_LOCKED :
				break;
			case LOCKED_SINGLETON_GLOBAL_RUNNING :
				throw new ApplicationException(ApplicationException.APPLICATION_INTERNAL_ERROR, NLS.bind(Messages.singleton_running, activeGlobalSingleton.getInstanceId()));
			case LOCKED_SINGLETON_GLOBAL_APPS_RUNNING :
				throw new ApplicationException(ApplicationException.APPLICATION_INTERNAL_ERROR, Messages.apps_running);
			case LOCKED_SINGLETON_SCOPED_RUNNING :
				throw new ApplicationException(ApplicationException.APPLICATION_INTERNAL_ERROR, NLS.bind(Messages.singleton_running, activeScopedSingleton.getInstanceId()));
			case LOCKED_SINGLETON_LIMITED_RUNNING :
				throw new ApplicationException(ApplicationException.APPLICATION_INTERNAL_ERROR, NLS.bind(Messages.max_running, eclipseApp.getApplicationId()));
			case LOCKED_MAIN_THREAD_RUNNING :
				throw new ApplicationException(ApplicationException.APPLICATION_INTERNAL_ERROR, NLS.bind(Messages.main_running, activeMain.getInstanceId()));
			default :
				break;
		}

		if (eclipseApp.getThreadType() == EclipseAppDescriptor.FLAG_TYPE_MAIN_THREAD) {
			if (activeMain != null)
				throw new IllegalStateException(Messages.main_running + activeMain.getInstanceId());
		}
		// ok we can now successfully lock the container
		switch (eclipseApp.getCardinalityType()) {
			case EclipseAppDescriptor.FLAG_CARD_SINGLETON_GLOGAL :
				activeGlobalSingleton = appHandle;
				break;
			case EclipseAppDescriptor.FLAG_CARD_SINGLETON_SCOPED :
				activeScopedSingleton = appHandle;
				break;
			case EclipseAppDescriptor.FLAG_CARD_LIMITED :
				if (activeLimited == null)
					activeLimited = new HashMap(3);
				ArrayList limited = (ArrayList) activeLimited.get(eclipseApp.getApplicationId());
				if (limited == null) {
					limited = new ArrayList(eclipseApp.getCardinality());
					activeLimited.put(eclipseApp.getApplicationId(), limited);
				}
				limited.add(appHandle);
				break;
			case EclipseAppDescriptor.FLAG_CARD_UNLIMITED :
				break;
			default :
				break;
		}
		if (eclipseApp.getThreadType() == EclipseAppDescriptor.FLAG_TYPE_MAIN_THREAD)
			activeMain = appHandle;
		activeHandles.add(appHandle);
		refreshAppDescriptors();
	}

	synchronized void unlock(EclipseAppHandle appHandle) {
		if (activeGlobalSingleton == appHandle)
			activeGlobalSingleton = null;
		else if (activeScopedSingleton == appHandle)
			activeScopedSingleton = null;
		else if (((EclipseAppDescriptor) appHandle.getApplicationDescriptor()).getCardinalityType() == EclipseAppDescriptor.FLAG_CARD_LIMITED) {
			if (activeLimited != null) {
				ArrayList limited = (ArrayList) activeLimited.get(((EclipseAppDescriptor) appHandle.getApplicationDescriptor()).getApplicationId());
				if (limited != null)
					limited.remove(appHandle);
			}
		}
		if (activeMain == appHandle)
			activeMain = null;
		if (activeHandles.remove(appHandle))
			refreshAppDescriptors(); // only refresh descriptors if we really unlocked something
	}

	synchronized int isLocked(EclipseAppDescriptor eclipseApp) {
		if (activeGlobalSingleton != null)
			return LOCKED_SINGLETON_GLOBAL_RUNNING;
		switch (eclipseApp.getCardinalityType()) {
			case EclipseAppDescriptor.FLAG_CARD_SINGLETON_GLOGAL :
				if (activeHandles.size() > 0)
					return LOCKED_SINGLETON_GLOBAL_APPS_RUNNING;
				break;
			case EclipseAppDescriptor.FLAG_CARD_SINGLETON_SCOPED :
				if (activeScopedSingleton != null)
					return LOCKED_SINGLETON_SCOPED_RUNNING;
				break;
			case EclipseAppDescriptor.FLAG_CARD_LIMITED :
				if (activeLimited != null) {
					ArrayList limited = (ArrayList) activeLimited.get(eclipseApp.getApplicationId());
					if (limited != null && limited.size() >= eclipseApp.getCardinality())
						return LOCKED_SINGLETON_LIMITED_RUNNING;
				}
				break;
			case EclipseAppDescriptor.FLAG_CARD_UNLIMITED :
				break;
			default :
				break;
		}
		if (eclipseApp.getThreadType() == EclipseAppDescriptor.FLAG_TYPE_MAIN_THREAD && activeMain != null)
			return LOCKED_MAIN_THREAD_RUNNING;
		return NOT_LOCKED;
	}

	static Object callMethod(Object obj, String methodName, Class[] argTypes, Object[] args) {
		Throwable error = null;
		try {
			Method method = obj.getClass().getMethod(methodName, argTypes);
			return method.invoke(obj, args);
		} catch (SecurityException e) {
			error = e;
		} catch (NoSuchMethodException e) {
			error = e;
		} catch (IllegalArgumentException e) {
			error = e;
		} catch (IllegalAccessException e) {
			error = e;
		} catch (InvocationTargetException e) {
			error = e.getCause();
		}
		if (error != null) {
			Activator.log(new FrameworkLogEntry(Activator.PI_APP, FrameworkLogEntry.ERROR, 0, "Error in invoking method.", 0, error, null));
		}
		return null;
	}
}
