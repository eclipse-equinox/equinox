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
	private static final String PT_PRODUCTS = "products"; //$NON-NLS-1$
	private static final String EXT_ERROR_APP = "org.eclipse.equinox.app.error"; //$NON-NLS-1$

	private static final String PROP_PRODUCT = "eclipse.product"; //$NON-NLS-1$
	private static final String PROP_ECLIPSE_APPLICATION = "eclipse.application"; //$NON-NLS-1$
	private static final String PROP_ECLIPSE_APPLICATION_NODEFAULT = "eclipse.application.noDefault"; //$NON-NLS-1$

	BundleContext context;
	// A map of ApplicationDescriptors keyed by eclipse application ID
	private HashMap apps = new HashMap();

	private IExtensionRegistry extensionRegistry;
	private ApplicationLauncher appLauncher;
	private IProduct product;
	private boolean missingProductReported;

	// the currently lauched application handle
	private EclipseAppHandle curHandle;
	
	public EclipseAppContainer(BundleContext context, IExtensionRegistry extensionRegistry, ApplicationLauncher appLauncher) {
		this.context = context;
		this.extensionRegistry = extensionRegistry;
		this.appLauncher = appLauncher;
	}

	void start() {
		getExtensionRegistry().addRegistryChangeListener(this);
		// registerAppDecriptors(null);
		// need to listen for system bundle stopping
		context.addBundleListener(this);
		// Start the default application
		try {
			startDefaultApp();
		} catch (ApplicationException e) {
			// TODO Log this
			e.printStackTrace();
		}
	}

	void stop() {
		// stop all applications
		stopAllApps();
		context.removeBundleListener(this);
		getExtensionRegistry().removeRegistryChangeListener(this);
		// flush the apps
		apps.clear();
		product = null;
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
			registerAppDecriptors(applicationId); // try again just in case we are waiting for an event
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
			boolean visible = true;
			if (configs.length > 0) {
				String sVisible = configs[0].getAttribute(PT_APP_VISIBLE);
				visible = sVisible == null ? true : Boolean.valueOf(sVisible).booleanValue();
			}
			appDescriptor = new EclipseAppDescriptor(appExtension.getContributor().getName(), appExtension.getUniqueIdentifier(), visible, this);
			// register the appDescriptor as a service
			ServiceRegistration sr = (ServiceRegistration) AccessController.doPrivileged(new RegisterService(ApplicationDescriptor.class.getName(), appDescriptor, appDescriptor.getServiceProperties()));
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
	PrivilegedAction getRegServiceAction(String serviceClass, Object serviceObject, Dictionary serviceProps) {
		return new RegisterService(serviceClass, serviceObject, serviceProps);
	}

	/*
	 * PrivilegedAction used to register ApplicationDescriptor and ApplicationHandle services
	 */
	private class RegisterService implements PrivilegedAction {
		String serviceClass;
		Object serviceObject;
		Dictionary serviceProps;

		RegisterService(String serviceClass, Object serviceObject, Dictionary serviceProps) {
			this.serviceClass = serviceClass;
			this.serviceObject = serviceObject;
			this.serviceProps = serviceProps;
		}

		public Object run() {
			return context.registerService(serviceClass, serviceObject, serviceProps);
		}
	}

	private void startDefaultApp() throws ApplicationException {
		if (Boolean.getBoolean(EclipseAppContainer.PROP_ECLIPSE_APPLICATION_NODEFAULT)) {
			// we are not running the default application; we should register all applications
			registerAppDecriptors(null);
		} else {
			// find the default application
			EclipseAppDescriptor defaultDesc = findDefaultApp();
			if (defaultDesc != null)
				defaultDesc.launch(null);
		}
	}

	private EclipseAppDescriptor findDefaultApp() {
		String applicationId = getApplicationId();
		if (applicationId == null) {
			// the application id is not set; return a descriptor that will throw an exception
			// return new EclipseAppDescriptor(Activator.PI_APP, Activator.PI_APP + ".missingapp", null, false, this, new RuntimeException(Messages.application_noIdFound)); //$NON-NLS-1$
			ErrorApplication.setError(new RuntimeException(Messages.application_noIdFound));
			return getAppDescriptor(EXT_ERROR_APP);
		}
		EclipseAppDescriptor defaultApp = getAppDescriptor(applicationId);
		if (defaultApp == null) {
			// the application id is not available in the registry; return a descriptor that will throw an exception
			//return new EclipseAppDescriptor(applicationId, applicationId, null, false, this, new RuntimeException(NLS.bind(Messages.application_notFound, applicationId, getAvailableAppsMsg(getExtensionRegistry()))));
			ErrorApplication.setError(new RuntimeException(NLS.bind(Messages.application_notFound, applicationId, getAvailableAppsMsg(getExtensionRegistry()))));
			return getAppDescriptor(EXT_ERROR_APP);
		}
		return defaultApp;
	}

	/*
	 * Registers an ApplicationDescriptor service for each eclipse application
	 * available in the extension registry.
	 */
	private void registerAppDecriptors(String applicationId) {
		// look in the old core.runtime applications extension point
		IExtension[] availableApps = getAvailableApps(getExtensionRegistry(), PI_RUNTIME, applicationId);
		for (int i = 0; i < availableApps.length; i++)
			createAppDescriptor(availableApps[i]);
	}

	/*
	 * Returns a list of all the available application IDs which are available 
	 * in the extension registry.
	 */
	private IExtension[] getAvailableApps(IExtensionRegistry registry, String pi, String applicationId) {
		if (applicationId != null) {
			IExtension appExt = registry.getExtension(pi + '.' + PT_APPLICATIONS, applicationId);
			return appExt == null ? new IExtension[0] : new IExtension[] {appExt};
		}
		IExtensionPoint point = registry.getExtensionPoint(pi + '.' + PT_APPLICATIONS);
		if (point == null)
			return new IExtension[0];
		return point.getExtensions();
	}

	private String getAvailableAppsMsg(IExtensionRegistry registry) {
		IExtension[] availableApps = getAvailableApps(registry, PI_RUNTIME, null);
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
		IExtensionRegistry registry = getExtensionRegistry();
		IExtension applicationExtension = registry.getExtension(PI_RUNTIME, PT_APPLICATIONS, applicationId);
		if (applicationExtension == null)
			throw new RuntimeException(NLS.bind(Messages.application_notFound, applicationId, getAvailableAppsMsg(registry)));
		return applicationExtension;
	}

	private IExtensionRegistry getExtensionRegistry() {
		return extensionRegistry;
	}

	void launch(EclipseAppHandle appHandle) throws Exception {
		lock(appHandle);
		// use the ApplicationLauncher provided by the framework 
		// to ensure it is launched on the main thread
		if (appLauncher == null)
			throw new IllegalStateException();
		MainThreadRunnable app = new MainThreadRunnable(appHandle);
		appLauncher.launch(app, appHandle.getArguments() == null ? null : appHandle.getArguments().get(IApplicationContext.APPLICATION_ARGS));
		appHandle.setAppRunnable(app);
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
						// TODO should log this
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
		return getProduct() == null ? null : getProduct().getApplication();
	}

	public IProduct getProduct() {
		if (product != null)
			return product;
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
		IConfigurationElement[] entries = getExtensionRegistry().getConfigurationElementsFor(PI_RUNTIME, PT_PRODUCTS, productId);
		if (entries.length > 0) {
			// There should only be one product with the given id so just take the first element
			product = new Product(productId, entries[0]);
			return product;
		}
		IConfigurationElement[] elements = getExtensionRegistry().getConfigurationElementsFor(PI_RUNTIME, PT_PRODUCTS);
		List logEntries = null;
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equalsIgnoreCase("provider")) { //$NON-NLS-1$
				try {
					IProductProvider provider = (IProductProvider) element.createExecutableExtension("run"); //$NON-NLS-1$
					IProduct[] products = provider.getProducts();
					for (int j = 0; j < products.length; j++) {
						IProduct provided = products[j];
						if (productId.equalsIgnoreCase(provided.getId())) {
							product = provided;
							return product;
						}
					}
				} catch (CoreException e) {
					if (logEntries == null)
						logEntries = new ArrayList(3);
					logEntries.add(new FrameworkLogEntry(Activator.PI_APP, NLS.bind(Messages.provider_invalid, element.getParent().toString()), 0, e, null));
				}
			}
		}
		if (logEntries != null && Activator.getFrameworkLog() != null)
			Activator.getFrameworkLog().log(new FrameworkLogEntry(Activator.PI_APP, Messages.provider_invalid_general, 0, null, (FrameworkLogEntry[]) logEntries.toArray(new FrameworkLogEntry[logEntries.size()])));

		if (!missingProductReported && Activator.getFrameworkLog() != null) {
			Activator.getFrameworkLog().log(new FrameworkLogEntry(Activator.PI_APP, NLS.bind(Messages.product_notFound, productId), 0, null, null));
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

	synchronized void lock(EclipseAppHandle appHandle) {
		if (curHandle != null)
			throw new IllegalStateException("Only one application of is allowed to run at a time");
		curHandle = appHandle;
		refreshAppDescriptors();
	}

	synchronized void unlock() {
		curHandle = null;
		refreshAppDescriptors();
	}

	synchronized boolean isLocked() {
		return curHandle != null;
	}
}
