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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.app.IContainer;
import org.eclipse.equinox.registry.*;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ApplicationHandle;
import org.osgi.util.tracker.ServiceTracker;

/*
 * A MEG application container that understands eclipse applications.  This 
 * container will discover installed eclipse applications and register the 
 * appropriate ApplicatoinDescriptor service with the service registry.
 */
public class ContainerManager implements IRegistryChangeListener, SynchronousBundleListener {
	private static final String PI_RUNTIME = "org.eclipse.core.runtime"; //$NON-NLS-1$
	private static final String PT_APPLICATIONS = "applications"; //$NON-NLS-1$
	private static final String PT_APP_TYPE = "type"; //$NON-NLS-1$
	private static final String PT_RUN = "run"; //$NON-NLS-1$
	private static final String PT_PRODUCTS = "products"; //$NON-NLS-1$
	private static final String PT_CONTAINERS = "containers"; //$NON-NLS-1$
	private static final String ATTR_APPLICATION = "application"; //$NON-NLS-1$
	static final String APP_TYPE_MAIN_SINGLETON = "main.singleton"; //$NON-NLS-1$

	public static final String PROP_PRODUCT = "eclipse.product"; //$NON-NLS-1$
	public static final String PROP_ECLIPSE_APPLICATION = "eclipse.application"; //$NON-NLS-1$
	public static final String PROP_ECLIPSE_APPLICATION_ARGS = "eclipse.application.args"; //$NON-NLS-1$
	public static final String PROP_ECLIPSE_APPLICATION_NODEFAULT = "eclipse.application.noDefault"; //$NON-NLS-1$

	BundleContext context;
	// A map of ApplicationDescriptors keyed by eclipse application ID
	private HashMap apps = new HashMap();
	// A map of containers keyed by application type
	private HashMap containers = new HashMap();
	// tracks the FrameworkLog
	private ServiceTracker frameworkLog;
	private boolean missingProductReported;
	private IExtensionRegistry extensionRegistry;

	public ContainerManager(BundleContext context, IExtensionRegistry extensionRegistry) {
		this.context = context;
		this.extensionRegistry = extensionRegistry;
	}

	void startContainer() {
		frameworkLog = new ServiceTracker(context, FrameworkLog.class.getName(), null);
		frameworkLog.open();
		getExtensionRegistry().addRegistryChangeListener(this);
		registerAppDecriptors();
		containers.put(APP_TYPE_MAIN_SINGLETON, new SingletonContainerMgr(new MainSingletonContainer(this), APP_TYPE_MAIN_SINGLETON, this));
		// need to listen for system bundle stopping
		context.addBundleListener(this);
	}

	void stopContainer() {
		// stop all applications first
		stopAllApplications();
		context.removeBundleListener(this);
		getExtensionRegistry().removeRegistryChangeListener(this);
		frameworkLog.close();
		frameworkLog = null;
		// flush the apps and containers
		apps.clear();
		containers.clear();
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
			registerAppDecriptors(); // try again just in case we are waiting for an event
			synchronized (apps) {
				result = (EclipseAppDescriptor) apps.get(applicationId);
			}
		}
		return result;
	}

	EclipseAppDescriptor[] getAppDescriptorsByType(String type) {
		ArrayList result = new ArrayList();
		synchronized (apps) {
			for (Iterator iApps = apps.values().iterator(); iApps.hasNext();) {
				EclipseAppDescriptor app = (EclipseAppDescriptor) iApps.next();
				if (type.equals(app.getType()))
					result.add(app);
			}
		}
		return (EclipseAppDescriptor[]) result.toArray(new EclipseAppDescriptor[result.size()]);
	}

	private IContainer getContainer(String type) {
		synchronized (containers) {
			IContainer container = (IContainer) containers.get(type);
			if (container != null)
				return container;
			container = createContainer(type);
			if (container != null)
				containers.put(type, container);
			return container;
		}
	}

	private EclipseAppDescriptor createAppDescriptor(IExtension appExtension) {
		synchronized (apps) {
			EclipseAppDescriptor appDescriptor = (EclipseAppDescriptor) apps.get(appExtension.getUniqueIdentifier());
			if (appDescriptor != null)
				return appDescriptor;
			// the appDescriptor does not exist for the app ID; create it
			IConfigurationElement[] configs = appExtension.getConfigurationElements();
			String type = null;
			if (configs.length > 0)
				type = configs[0].getAttribute(PT_APP_TYPE);
			appDescriptor = new EclipseAppDescriptor(appExtension.getNamespace(), appExtension.getUniqueIdentifier(), type, this);
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

	EclipseAppDescriptor findDefaultApp() {
		String applicationId = System.getProperty(PROP_ECLIPSE_APPLICATION);
		if (applicationId == null) {
			//Derive the application from the product information
			applicationId = getProductAppId();
			if (applicationId != null)
				// use the long way to set the property to compile against eeminimum
				System.getProperties().setProperty(PROP_ECLIPSE_APPLICATION, applicationId);
		}
		if (applicationId == null)
			// the application id is not set; return a descriptor that will throw an exception
			return new EclipseAppDescriptor(Activator.PI_APP, Activator.PI_APP + ".missingapp", null, this, new RuntimeException(Messages.application_noIdFound)); //$NON-NLS-1$
		EclipseAppDescriptor defaultApp = getAppDescriptor(applicationId);
		if (defaultApp == null)
			// the application id is not available in the registry; return a descriptor that will throw an exception
			return new EclipseAppDescriptor(applicationId, applicationId, null, this, new RuntimeException(NLS.bind(Messages.application_notFound, applicationId, getAvailableAppsMsg(getExtensionRegistry()))));
		return defaultApp;
	}

	/*
	 * Registers an ApplicationDescriptor service for each eclipse application
	 * available in the extension registry.
	 */
	private void registerAppDecriptors() {
		// look in the old core.runtime applications extension point
		IExtension[] availableApps = getAvailableApps(getExtensionRegistry(), PI_RUNTIME);
		for (int i = 0; i < availableApps.length; i++)
			createAppDescriptor(availableApps[i]);
		// look in the new equinox.app applications extinsion point
		availableApps = getAvailableApps(getExtensionRegistry(), Activator.PI_APP);
		for (int i = 0; i < availableApps.length; i++)
			createAppDescriptor(availableApps[i]);
	}

	private IContainer createContainer(String type) {
		IExtensionPoint extPoint = getExtensionRegistry().getExtensionPoint(Activator.PI_APP, PT_CONTAINERS);
		if (extPoint == null)
			return null;
		IExtension[] availableContainers = extPoint.getExtensions();
		for (int i = 0; i < availableContainers.length; i++) {
			IConfigurationElement[] configs = availableContainers[i].getConfigurationElements();
			if (configs.length == 0)
				return null;
			String containerType = configs[0].getAttribute(PT_APP_TYPE);
			if (type.equals(containerType))
				return createContainer(configs[0], type);
		}
		return null;
	}

	private IContainer createContainer(IConfigurationElement config, String type) {
		try {
			IContainer container = (IContainer) config.createExecutableExtension(PT_RUN);
			if (container.isSingletonContainer())
				container = new SingletonContainerMgr(container, type, this);
			return container;
		} catch (CoreException e) {
			// TODO should log this
			e.printStackTrace();
		}
		return null;
	}

	private IContainer removeContainer(IExtension containerExt) {
		IConfigurationElement[] configs = containerExt.getConfigurationElements();
		if (configs.length == 0)
			return null;
		String type = configs[0].getAttribute(PT_APP_TYPE);
		if (type == null)
			return null;
		synchronized (containers) {
			return (IContainer) containers.get(type);
		}
	}

	/*
	 * Returns a list of all the available application IDs which are available 
	 * in the extension registry.
	 */
	private IExtension[] getAvailableApps(IExtensionRegistry registry, String pi) {
		IExtensionPoint point = registry.getExtensionPoint(pi + '.' + PT_APPLICATIONS);
		if (point == null)
			return new IExtension[0];
		return point.getExtensions();
	}

	private String getAvailableAppsMsg(IExtensionRegistry registry) {
		IExtension[] availableApps = getAvailableApps(registry, PI_RUNTIME);
		String availableAppsMsg = "<NONE>"; //$NON-NLS-1$
		if (availableApps.length != 0) {
			availableAppsMsg = availableApps[0].getUniqueIdentifier();
			for (int i = 1; i < availableApps.length; i++)
				availableAppsMsg = availableAppsMsg + ", " + availableApps[i].getUniqueIdentifier(); //$NON-NLS-1$
		}
		availableApps = getAvailableApps(registry, Activator.PI_APP);
		if (availableApps.length != 0) {
			if (!availableAppsMsg.equals("<NONE>")) //$NON-NLS-1$
				availableAppsMsg = availableAppsMsg + ", "; //$NON-NLS-1$
			availableAppsMsg = availableAppsMsg + availableApps[0].getUniqueIdentifier();
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
			applicationExtension = registry.getExtension(Activator.PI_APP, PT_APPLICATIONS, applicationId);
		if (applicationExtension == null)
			throw new RuntimeException(NLS.bind(Messages.application_notFound, applicationId, getAvailableAppsMsg(registry)));
		return applicationExtension;
	}

	private IExtensionRegistry getExtensionRegistry() {
		return extensionRegistry;
	}

	private FrameworkLog getFrameworkLog() {
		return (FrameworkLog) AppManager.getService(frameworkLog);
	}

	BundleContext getBundleContext() {
		return context;
	}

	private String getProductAppId() {
		String productId = System.getProperty(PROP_PRODUCT);
		if (productId == null)
			return null;
		IConfigurationElement[] entries = getExtensionRegistry().getConfigurationElementsFor(PI_RUNTIME, PT_PRODUCTS, productId);
		if (entries.length > 0)
			// There should only be one product with the given id so just take the first element
			return entries[0].getAttribute(ATTR_APPLICATION);
		IConfigurationElement[] elements = getExtensionRegistry().getConfigurationElementsFor(PI_RUNTIME, PT_PRODUCTS);
		List logEntries = null;
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (element.getName().equalsIgnoreCase("provider")) { //$NON-NLS-1$
				try {
					Object provider = element.createExecutableExtension(PT_RUN);
					Object[] products = (Object[]) ContainerManager.execMethod(provider, "getProducts", null, null); //$NON-NLS-1$
					for (int j = 0; j < products.length; j++) {
						Object provided = products[j];
						if (productId.equalsIgnoreCase((String) ContainerManager.execMethod(provided, "getId", null, null))) //$NON-NLS-1$
							return (String) ContainerManager.execMethod(provided, "getApplication", null, null); //$NON-NLS-1$
					}
				} catch (CoreException e) {
					if (logEntries == null)
						logEntries = new ArrayList(3);
					logEntries.add(new FrameworkLogEntry(Activator.PI_APP, NLS.bind(Messages.provider_invalid, element.getParent().toString()), 0, e, null));
				}
			}
		}
		if (logEntries != null)
			getFrameworkLog().log(new FrameworkLogEntry(Activator.PI_APP, Messages.provider_invalid_general, 0, null, (FrameworkLogEntry[]) logEntries.toArray()));

		if (!missingProductReported) {
			getFrameworkLog().log(new FrameworkLogEntry(Activator.PI_APP, NLS.bind(Messages.product_notFound, productId), 0, null, null));
			missingProductReported = true;
		}
		return null;
	}

	public static Object execMethod(Object obj, String methodName, Class argType, Object arg) {
		try {
			Method method = obj.getClass().getMethod(methodName, argType == null ? null : new Class[] {argType});
			return method.invoke(obj, arg == null ? null : new Object[] {arg});
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage());
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(e.getMessage());
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	void launch(EclipseAppHandle appHandle) throws Exception {
		String type = ((EclipseAppDescriptor) appHandle.getApplicationDescriptor()).getType();
		if (type == null)
			type = APP_TYPE_MAIN_SINGLETON;
		IContainer container = getContainer(type);
		if (container != null) {
			if (container instanceof SingletonContainerMgr)
				((SingletonContainerMgr) container).lock(appHandle);
			appHandle.setApplication(container.launch(appHandle));
		} else
			throw new UnsupportedOperationException(NLS.bind(Messages.container_notFound, appHandle.getApplicationDescriptor().getApplicationId(), type));
	}

	public void registryChanged(IRegistryChangeEvent event) {
		processAppDeltas(event.getExtensionDeltas(PI_RUNTIME, PT_APPLICATIONS));
		processAppDeltas(event.getExtensionDeltas(Activator.PI_APP, PT_APPLICATIONS));
		processContainerDeltas(event.getExtensionDeltas(Activator.PI_APP, PT_CONTAINERS));
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

	private void processContainerDeltas(IExtensionDelta[] deltas) {
		for (int i = 0; i < deltas.length; i++) {
			switch (deltas[i].getKind()) {
				case IExtensionDelta.ADDED :
					// don't create containers agressively
					break;
				case IExtensionDelta.REMOVED :
					removeContainer(deltas[i].getExtension());
					break;
			}
		}
	}

	public void bundleChanged(BundleEvent event) {
		// if this is not the system bundle stopping then ignore the event
		if ((BundleEvent.STOPPING & event.getType()) == 0 || event.getBundle().getBundleId() != 0)
			return;
		// The system bundle is stopping; better stop all applications now
		stopAllApplications();
	}

	private void stopAllApplications() {
		// get a stapshot of running applications
		try {
			ServiceReference[] runningRefs = context.getServiceReferences(ApplicationHandle.class.getName(), "(!(application.state=STOPPING))"); //$NON-NLS-1$
			if (runningRefs == null)
				return;
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
}
