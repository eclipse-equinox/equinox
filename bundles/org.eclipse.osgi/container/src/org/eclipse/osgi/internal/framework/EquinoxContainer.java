/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.framework;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.osgi.framework.eventmgr.EventManager;
import org.eclipse.osgi.framework.eventmgr.ListenerQueue;
import org.eclipse.osgi.framework.internal.core.Msg;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.framework.legacy.PackageAdminImpl;
import org.eclipse.osgi.internal.framework.legacy.StartLevelImpl;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.eclipse.osgi.internal.log.EquinoxLogServices;
import org.eclipse.osgi.internal.serviceregistry.ServiceRegistry;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.storage.Storage;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

@SuppressWarnings("deprecation")
public class EquinoxContainer {
	public static final String NAME = "org.eclipse.osgi"; //$NON-NLS-1$
	private static final String CONFIG_FILE = "config.ini"; //$NON-NLS-1$

	private final EquinoxConfiguration equinoxConfig;
	private final EquinoxLocations equinoxLocations;
	private final EquinoxLogServices logServices;
	private final Storage storage;
	private final PackageAdmin packageAdmin;
	private final StartLevel startLevel;
	private final String[] bootDelegation;
	private final String[] bootDelegationStems;
	private final boolean bootDelegateAll;

	private final Object monitor = new Object();
	private EventManager eventManager;
	private EquinoxEventPublisher eventPublisher;
	private ServiceRegistry serviceRegistry;
	private ContextFinder contextFinder;
	private boolean initialized = false;

	public EquinoxContainer(Map<String, String> configuration) {
		this.equinoxConfig = new EquinoxConfiguration(configuration, new HookRegistry(this));
		this.equinoxLocations = new EquinoxLocations(this.equinoxConfig);
		loadConfig(equinoxConfig, equinoxLocations);
		this.logServices = new EquinoxLogServices(this.equinoxConfig, this.equinoxLocations.getConfigurationLocation());
		this.equinoxConfig.getHookRegistry().initialize();

		try {
			this.storage = new Storage(this);
		} catch (IOException e) {
			throw new RuntimeException("Error initializing storage.", e); //$NON-NLS-1$
		}
		this.packageAdmin = new PackageAdminImpl(storage.getModuleContainer());
		this.startLevel = new StartLevelImpl(storage.getModuleContainer());

		// set the boot delegation according to the osgi boot delegation property
		// TODO unfortunately this has to be done after constructing storage so the vm profile is loaded
		// TODO ideally this should be in equinox configuration or perhaps in storage
		String bootDelegationProp = equinoxConfig.getConfiguration(Constants.FRAMEWORK_BOOTDELEGATION);
		String[] bootPackages = ManifestElement.getArrayFromList(bootDelegationProp, ","); //$NON-NLS-1$
		List<String> exactMatch = new ArrayList<String>(bootPackages.length);
		List<String> stemMatch = new ArrayList<String>(bootPackages.length);
		boolean delegateAllValue = false;
		for (int i = 0; i < bootPackages.length; i++) {
			if (bootPackages[i].equals("*")) { //$NON-NLS-1$
				delegateAllValue = true;
				exactMatch.clear();
				stemMatch.clear();
				break;
			} else if (bootPackages[i].endsWith("*")) { //$NON-NLS-1$
				if (bootPackages[i].length() > 2 && bootPackages[i].endsWith(".*")) //$NON-NLS-1$
					stemMatch.add(bootPackages[i].substring(0, bootPackages[i].length() - 1));
			} else {
				exactMatch.add(bootPackages[i]);
			}
		}
		bootDelegateAll = delegateAllValue;
		bootDelegation = exactMatch.isEmpty() ? null : exactMatch.toArray(new String[exactMatch.size()]);
		bootDelegationStems = stemMatch.isEmpty() ? null : stemMatch.toArray(new String[stemMatch.size()]);
	}

	private static void loadConfig(EquinoxConfiguration equinoxConfig, EquinoxLocations equinoxLocations) {
		Location configArea = equinoxLocations.getConfigurationLocation();
		if (configArea == null)
			return;

		URL location = null;
		try {
			location = new URL(configArea.getURL().toExternalForm() + CONFIG_FILE);
		} catch (MalformedURLException e) {
			// its ok.  This should never happen
		}
		mergeProperties(equinoxConfig, loadProperties(location, equinoxConfig));
	}

	private static void mergeProperties(EquinoxConfiguration equinoxConfig, Properties source) {
		for (Enumeration<?> e = source.keys(); e.hasMoreElements();) {
			String key = (String) e.nextElement();
			String value = source.getProperty(key);
			if (equinoxConfig.getConfiguration(key) == null)
				equinoxConfig.setProperty(key, value);
		}
	}

	private static Properties loadProperties(URL location, EquinoxConfiguration equinoxConfig) {
		Properties result = new Properties();
		if (location == null)
			return result;
		try {
			InputStream in = location.openStream();
			try {
				result.load(in);
			} finally {
				in.close();
			}
		} catch (IOException e) {
			// its ok if there is no file.  We'll just use the defaults for everything
			// TODO but it might be nice to log something with gentle wording (i.e., it is not an error)
		}
		return substituteVars(result, equinoxConfig);
	}

	private static Properties substituteVars(Properties result, EquinoxConfiguration equinoxConfig) {
		if (result == null) {
			//nothing todo.
			return null;
		}
		for (Enumeration<Object> eKeys = result.keys(); eKeys.hasMoreElements();) {
			Object key = eKeys.nextElement();
			if (key instanceof String) {
				String value = result.getProperty((String) key);
				if (value != null)
					result.put(key, equinoxConfig.substituteVars(value));
			}
		}
		return result;
	}

	public Storage getStorage() {
		return storage;
	}

	public EquinoxConfiguration getConfiguration() {
		return equinoxConfig;
	}

	public EquinoxLocations getLocations() {
		return equinoxLocations;
	}

	public EquinoxLogServices getLogServices() {
		return logServices;
	}

	public PackageAdmin getPackageAdmin() {
		return packageAdmin;
	}

	public StartLevel getStartLevel() {
		return startLevel;
	}

	public boolean isBootDelegationPackage(String name) {
		if (bootDelegateAll)
			return true;
		if (bootDelegation != null)
			for (int i = 0; i < bootDelegation.length; i++)
				if (name.equals(bootDelegation[i]))
					return true;
		if (bootDelegationStems != null)
			for (int i = 0; i < bootDelegationStems.length; i++)
				if (name.startsWith(bootDelegationStems[i]))
					return true;
		return false;
	}

	void init() {
		synchronized (this.monitor) {
			eventManager = new EventManager("Framework Event Dispatcher"); //$NON-NLS-1$
			eventPublisher = new EquinoxEventPublisher(this);
			serviceRegistry = new ServiceRegistry(this);
			initializeContextFinder();
			initialized = true;
			startActiveThread();
		}
	}

	private void startActiveThread() {
		if (EquinoxConfiguration.FRAMEWORK_THREAD_NORMAL.equals(equinoxConfig.getConfiguration(EquinoxConfiguration.PROP_FRAMEWORK_THREAD, EquinoxConfiguration.FRAMEWORK_THREAD_NORMAL))) {
			Thread fwkThread = new Thread(new Runnable() {
				@Override
				public void run() {
					synchronized (EquinoxContainer.this.monitor) {
						while (EquinoxContainer.this.initialized)
							try {
								EquinoxContainer.this.monitor.wait(1000);
							} catch (InterruptedException e) {
								// do nothing
							}
					}
				}
			}, "Framework Active Thread"); //$NON-NLS-1$
			fwkThread.setDaemon(false);
			fwkThread.start();
		}
	}

	void close() {
		synchronized (this.monitor) {
			eventManager.close();
			eventManager = null;
			eventPublisher = null;
			serviceRegistry = null;
			storage.close();
			initialized = false;
			notifyAll();
		}
	}

	private void initializeContextFinder() {
		Thread current = Thread.currentThread();
		try {
			ClassLoader parent = null;
			// check property for specified parent
			String type = equinoxConfig.getConfiguration(EquinoxConfiguration.PROP_CONTEXTCLASSLOADER_PARENT);
			if (EquinoxConfiguration.CONTEXTCLASSLOADER_PARENT_APP.equals(type))
				parent = ClassLoader.getSystemClassLoader();
			else if (EquinoxConfiguration.CONTEXTCLASSLOADER_PARENT_BOOT.equals(type))
				parent = null;
			else if (EquinoxConfiguration.CONTEXTCLASSLOADER_PARENT_FWK.equals(type))
				parent = EquinoxContainer.class.getClassLoader();
			else if (EquinoxConfiguration.CONTEXTCLASSLOADER_PARENT_EXT.equals(type)) {
				ClassLoader appCL = ClassLoader.getSystemClassLoader();
				if (appCL != null)
					parent = appCL.getParent();
			} else { // default is ccl (null or any other value will use ccl)
				parent = current.getContextClassLoader();
			}
			contextFinder = new ContextFinder(parent);
			current.setContextClassLoader(contextFinder);
			return;
		} catch (Exception e) {
			logServices.log(NAME, FrameworkLogEntry.INFO, NLS.bind(Msg.CANNOT_SET_CONTEXTFINDER, null), e);
		}

	}

	public EquinoxEventPublisher getEventPublisher() {
		synchronized (this.monitor) {
			return eventPublisher;
		}
	}

	public ServiceRegistry getServiceRegistry() {
		synchronized (this.monitor) {
			return serviceRegistry;
		}
	}

	public ContextFinder getContextFinder() {
		synchronized (this.monitor) {
			return contextFinder;
		}
	}

	public <K, V, E> ListenerQueue<K, V, E> newListenerQueue() {
		synchronized (this.monitor) {
			return new ListenerQueue<K, V, E>(eventManager);
		}
	}

	void checkAdminPermission(Bundle bundle, String action) {
		if (bundle == null)
			return;
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(new AdminPermission(bundle, action));
	}

	public void handleRuntimeError(Throwable t) {
		// TODO need to call some hook here
	}
}
