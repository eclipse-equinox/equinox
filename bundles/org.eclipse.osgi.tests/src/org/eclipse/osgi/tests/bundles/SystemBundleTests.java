/*******************************************************************************
 * Copyright (c) 2008, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.write;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.nio.file.Path;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.net.SocketFactory;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.equinox.log.ExtendedLogService;
import org.eclipse.equinox.log.test.TestListener;
import org.eclipse.equinox.log.test.TestListener2;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.framework.util.FilePath;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.eclipse.osgi.launch.Equinox;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.eclipse.osgi.storage.url.reference.Handler;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.security.BaseSecurityTest;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.Version;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

public class SystemBundleTests extends AbstractBundleTests {
	public static Test suite() {
		return new TestSuite(SystemBundleTests.class);
	}

	public void testSystemBundle01() {
		// simple test to create an embedded framework
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle01"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("SystemBundle context is null", systemContext); //$NON-NLS-1$

		ServiceReference[] refs = null;
		try {
			refs = systemContext.getServiceReferences(Location.class.getName(), "(type=osgi.configuration.area)"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("Unexpected syntax error", e); //$NON-NLS-1$
		}
		assertNotNull("Configuration Location refs is null", refs); //$NON-NLS-1$
		assertEquals("config refs length is wrong", 1, refs.length); //$NON-NLS-1$
		Location configLocation = (Location) systemContext.getService(refs[0]);
		URL configURL = configLocation.getURL();
		assertTrue("incorrect configuration location", configURL.toExternalForm().endsWith("testSystemBundle01/")); //$NON-NLS-1$ //$NON-NLS-2$

		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle02() {
		// create/start/stop/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle02"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);

		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle03() {
		// create/stop/ test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle03"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		String configArea = systemContext.getProperty("osgi.configuration.area"); //$NON-NLS-1$
		assertNotNull("config property is null", configArea); //$NON-NLS-1$
		assertTrue("Wrong configuration area", configArea.endsWith("testSystemBundle03/")); //$NON-NLS-1$ //$NON-NLS-2$
		// don't do anything; just put the framework back to the RESOLVED state
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle04() {
		// create/start/stop/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle04"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// try installing a bundle before starting
		Bundle substitutesA = null;
		try {
			substitutesA = systemContext.installBundle(installer.getBundleLocation("substitutes.a")); //$NON-NLS-1$
		} catch (BundleException e1) {
			fail("failed to install a bundle", e1); //$NON-NLS-1$
		}
		try {
			substitutesA.start();
		} catch (BundleException e) {
			fail("Unexpected bundle exception", e); //$NON-NLS-1$
		}

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		assertEquals("Wrong state for installed bundle", Bundle.ACTIVE, substitutesA.getState()); //$NON-NLS-1$
		// put the framework back to the RESOLVED state
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle05_1() {
		// create/install/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle05_1"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// try installing a bundle before starting
		Bundle substitutesA = null;
		try {
			substitutesA = systemContext.installBundle(installer.getBundleLocation("substitutes.a")); //$NON-NLS-1$
		} catch (BundleException e1) {
			fail("failed to install a bundle", e1); //$NON-NLS-1$
		}
		// start framework first
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		assertEquals("Wrong state for installed bundle", Bundle.INSTALLED, substitutesA.getState()); //$NON-NLS-1$
		try {
			substitutesA.start();
		} catch (BundleException e1) {
			fail("Failed to start a bundle", e1); //$NON-NLS-1$
		}
		assertEquals("Wrong state for active bundle", Bundle.ACTIVE, substitutesA.getState()); //$NON-NLS-1$
		// put the framework back to the RESOLVED state
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle05_2() {
		// create/install/start/stop/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle05_2"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// try installing a bundle before starting
		Bundle substitutesA = null;
		try {
			substitutesA = systemContext.installBundle(installer.getBundleLocation("substitutes.a")); //$NON-NLS-1$
		} catch (BundleException e1) {
			fail("failed to install a bundle", e1); //$NON-NLS-1$
		}
		// start framework first
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		assertEquals("Wrong state for installed bundle", Bundle.INSTALLED, substitutesA.getState()); //$NON-NLS-1$
		try {
			substitutesA.start();
		} catch (BundleException e1) {
			fail("Failed to start a bundle", e1); //$NON-NLS-1$
		}
		assertEquals("Wrong state for active bundle", Bundle.ACTIVE, substitutesA.getState()); //$NON-NLS-1$
		// put the framework back to the RESOLVED state
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		Bundle substitutesA2 = null;
		try {
			substitutesA2 = equinox.getBundleContext().installBundle(installer.getBundleLocation("substitutes.a")); //$NON-NLS-1$
		} catch (BundleException e) {
			fail("Unexpected exception installing", e); //$NON-NLS-1$
		}
		// assert the same bundle ID
		assertEquals("Bundle ids are not the same", substitutesA.getBundleId(), substitutesA2.getBundleId()); //$NON-NLS-1$
		// no need to start the bundle again it should have been persistently started
		assertEquals("Wrong state for active bundle", Bundle.ACTIVE, substitutesA2.getState()); //$NON-NLS-1$
		// put the framework back to the RESOLVED state
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$

	}

	public void testSystemBundle06() {
		// create multiple instances test
		File config1 = OSGiTestsActivator.getContext().getDataFile("testSystemBundle06_1"); //$NON-NLS-1$
		Map<String, Object> configuration1 = new HashMap<>();
		configuration1.put(Constants.FRAMEWORK_STORAGE, config1.getAbsolutePath());
		Equinox equinox1 = new Equinox(configuration1);
		try {
			equinox1.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox1.getState()); //$NON-NLS-1$

		File config2 = OSGiTestsActivator.getContext().getDataFile("testSystemBundle06_2"); //$NON-NLS-1$
		Map<String, Object> configuration2 = new HashMap<>();
		configuration2.put(Constants.FRAMEWORK_STORAGE, config2.getAbsolutePath());
		Equinox equinox2 = new Equinox(configuration2);
		try {
			equinox2.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox2.getState()); //$NON-NLS-1$

		BundleContext systemContext1 = equinox1.getBundleContext();
		assertNotNull("System context is null", systemContext1); //$NON-NLS-1$
		BundleContext systemContext2 = equinox2.getBundleContext();
		assertNotNull("System context is null", systemContext2); //$NON-NLS-1$

		assertNotSame(systemContext1, systemContext2);

		// start framework 1 first
		try {
			equinox1.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox1.getState()); //$NON-NLS-1$
		// start framework 2 first
		try {
			equinox2.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox2.getState()); //$NON-NLS-1$

		// put the framework 1 back to the RESOLVED state
		stop(equinox1);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox1.getState()); //$NON-NLS-1$

		// put the framework 2 back to the RESOLVED state
		stop(equinox2);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox2.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle07() {
		// test init twice
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle07_01"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		try {
			equinox.init();
		} catch (Exception e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("SystemBundle context is null", systemContext); //$NON-NLS-1$

		try {
			equinox.init();
		} catch (Exception e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}

		ServiceReference[] refs = null;
		try {
			refs = systemContext.getServiceReferences(Location.class.getName(), "(type=osgi.configuration.area)"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("Unexpected syntax error", e); //$NON-NLS-1$
		}
		assertNotNull("Configuration Location refs is null", refs); //$NON-NLS-1$
		assertEquals("config refs length is wrong", 1, refs.length); //$NON-NLS-1$
		Location configLocation = (Location) systemContext.getService(refs[0]);
		URL configURL = configLocation.getURL();
		assertTrue("incorrect configuration location", configURL.toExternalForm().endsWith("testSystemBundle07_01/")); //$NON-NLS-1$ //$NON-NLS-2$

		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle08() {
		// create/start/stop/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle08_1"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$

		config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle08_2"); //$NON-NLS-1$
		configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		equinox = new Equinox(configuration);
		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		ServiceReference[] refs = null;
		try {
			refs = equinox.getBundleContext().getServiceReferences(Location.class.getName(), "(type=osgi.configuration.area)"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("Unexpected syntax error", e); //$NON-NLS-1$
		}
		assertNotNull("Configuration Location refs is null", refs); //$NON-NLS-1$
		assertEquals("config refs length is wrong", 1, refs.length); //$NON-NLS-1$
		Location configLocation = (Location) equinox.getBundleContext().getService(refs[0]);
		URL configURL = configLocation.getURL();
		assertTrue("incorrect configuration location", configURL.toExternalForm().endsWith("testSystemBundle08_2/")); //$NON-NLS-1$ //$NON-NLS-2$

		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle09() {
		// test FrameworkUtil.createFilter
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle09"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		Bundle testFilterA = null;
		try {
			testFilterA = equinox.getBundleContext().installBundle(installer.getBundleLocation("test.filter.a")); //$NON-NLS-1$
		} catch (BundleException e) {
			fail("Unexpected exception installing", e); //$NON-NLS-1$
		}
		try {
			testFilterA.start();
		} catch (BundleException e) {
			fail("Unexpected exception starting test bundle", e); //$NON-NLS-1$
		}
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle10() {
		// create/start/update/stop test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle10"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		final Equinox equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		FrameworkEvent success = update(equinox);
		assertEquals("Wait for stop event type is wrong", FrameworkEvent.STOPPED_UPDATE, success.getType()); //$NON-NLS-1$
		// TODO delay hack to allow the framework to get started again
		for (int i = 0; i < 5 && Bundle.ACTIVE != equinox.getState(); i++)
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// nothing
			}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle11() {
		// test extra packages property
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle11"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "test.pkg1, test.pkg2"); //$NON-NLS-1$
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("SystemBundle context is null", systemContext); //$NON-NLS-1$

		PackageAdmin pa = (PackageAdmin) equinox.getBundleContext().getService(equinox.getBundleContext().getServiceReference(PackageAdmin.class.getName()));
		ExportedPackage[] pkg1 = pa.getExportedPackages("test.pkg1"); //$NON-NLS-1$
		assertNotNull(pkg1);
		assertEquals("Wrong number of exports", 1, pkg1.length); //$NON-NLS-1$
		assertEquals("Wrong package name", "test.pkg1", pkg1[0].getName()); //$NON-NLS-1$ //$NON-NLS-2$
		ExportedPackage[] pkg2 = pa.getExportedPackages("test.pkg2"); //$NON-NLS-1$
		assertNotNull(pkg2);
		assertEquals("Wrong number of exports", 1, pkg2.length); //$NON-NLS-1$
		assertEquals("Wrong package name", "test.pkg2", pkg2[0].getName()); //$NON-NLS-1$ //$NON-NLS-2$

		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle12() {
		// Test stop FrameworkEvent
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle12"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);

		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		// test timeout waiting for framework stop
		FrameworkEvent stopEvent = null;
		try {
			stopEvent = equinox.waitForStop(1000);
		} catch (InterruptedException e) {
			fail("Unexpected interrupted exception", e); //$NON-NLS-1$
		}
		assertNotNull("Stop event is null", stopEvent); //$NON-NLS-1$
		assertEquals("Wrong stopEvent", FrameworkEvent.WAIT_TIMEDOUT, stopEvent.getType()); //$NON-NLS-1$

		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		stopEvent = stop(equinox);
		assertNotNull("Stop event is null", stopEvent); //$NON-NLS-1$
		assertEquals("Wrong stopEvent", FrameworkEvent.STOPPED, stopEvent.getType()); //$NON-NLS-1$
	}

	public void testSystemBundle13() {
		// create/install/start/stop clean test
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle13"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// try installing a bundle before starting
		Bundle substitutesA = null;
		try {
			substitutesA = systemContext.installBundle(installer.getBundleLocation("substitutes.a")); //$NON-NLS-1$
		} catch (BundleException e1) {
			fail("failed to install a bundle", e1); //$NON-NLS-1$
		}
		// start framework first
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		assertEquals("Wrong state for installed bundle", Bundle.INSTALLED, substitutesA.getState()); //$NON-NLS-1$
		try {
			substitutesA.start();
		} catch (BundleException e1) {
			fail("Failed to start a bundle", e1); //$NON-NLS-1$
		}
		assertEquals("Wrong state for active bundle", Bundle.ACTIVE, substitutesA.getState()); //$NON-NLS-1$
		// put the framework back to the RESOLVED state
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$

		// initialize the framework again to the same configuration
		configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		substitutesA = equinox.getBundleContext().getBundle(1);

		// make sure the bundle is there
		assertNotNull("missing installed bundle", substitutesA); //$NON-NLS-1$
		assertEquals("Unexpected symbolic name", "substitutes.a", substitutesA.getSymbolicName()); //$NON-NLS-1$ //$NON-NLS-2$
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$

		// initialize the framework again to the same configuration but use clean option
		configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
		equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		substitutesA = equinox.getBundleContext().getBundle(1);

		// make sure the bundle is there
		assertNull("Unexpected bundle is installed", substitutesA); //$NON-NLS-1$
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testSystemBundle14() {
		// Test startlevel property
		File config = OSGiTestsActivator.getContext().getDataFile("testSystemBundle14"); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_BEGINNING_STARTLEVEL, "10"); //$NON-NLS-1$
		Equinox equinox = new Equinox(configuration);

		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}

		StartLevel st = (StartLevel) equinox.getBundleContext().getService(equinox.getBundleContext().getServiceReference(StartLevel.class.getName()));
		assertNotNull("StartLevel service is null", st); //$NON-NLS-1$
		assertEquals("Unexpected start level", 10, st.getStartLevel()); //$NON-NLS-1$
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		FrameworkEvent stopEvent = stop(equinox);
		assertNotNull("Stop event is null", stopEvent); //$NON-NLS-1$
		assertEquals("Wrong stopEvent", FrameworkEvent.STOPPED, stopEvent.getType()); //$NON-NLS-1$
	}

	public void testSystemBundle16() {
		// test parent boot
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_BOOT);
		checkParentClassLoader(configuration);
	}

	public void testSystemBundle17() {
		// test parent app
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_APP);
		checkParentClassLoader(configuration);
	}

	public void testSystemBundle18() {
		// test parent ext
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_EXT);
		checkParentClassLoader(configuration);
	}

	public void testSystemBundle19() {
		// test parent framework
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_BUNDLE_PARENT, Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK);
		checkParentClassLoader(configuration);
	}

	private void checkParentClassLoader(Map<String, Object> configuration) {
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}

		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		Bundle test = null;
		try {
			test = equinox.getBundleContext().installBundle(installer.getBundleLocation("substitutes.a")); //$NON-NLS-1$
		} catch (BundleException e) {
			fail("Failed to install bundle", e); //$NON-NLS-1$
		}
		try {
			Class activatorClazz = test.loadClass("substitutes.x.Ax"); //$NON-NLS-1$
			ClassLoader parentCL = activatorClazz.getClassLoader().getParent();
			String configParent = (String) configuration.get(Constants.FRAMEWORK_BUNDLE_PARENT);
			if (Constants.FRAMEWORK_BUNDLE_PARENT_APP.equals(configParent))
				assertTrue("Wrong parent", parentCL == ClassLoader.getSystemClassLoader()); //$NON-NLS-1$
			else if (Constants.FRAMEWORK_BUNDLE_PARENT_EXT.equals(configParent))
				assertTrue("Wrong parent", parentCL == ClassLoader.getSystemClassLoader().getParent()); //$NON-NLS-1$
			else if (Constants.FRAMEWORK_BUNDLE_PARENT_FRAMEWORK.equals(configParent))
				assertTrue("Wrong parent", parentCL == equinox.getClass().getClassLoader()); //$NON-NLS-1$
		} catch (ClassNotFoundException e) {
			fail("failed to load class", e); //$NON-NLS-1$
		}
		stop(equinox, FrameworkEvent.STOPPED);
	}

	public void testChangeEE() throws IOException, BundleException {
		URL javaSE8Profile = OSGiTestsActivator.getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getEntry("JavaSE-1.8.profile");
		URL javaSE9Profile = OSGiTestsActivator.getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getEntry("JavaSE-9.profile");

		// configure equinox for javaSE 9
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put("osgi.java.profile", javaSE9Profile.toExternalForm()); //$NON-NLS-1$

		Equinox equinox = new Equinox(configuration);
		equinox.start();

		// install a bundle that requires java 9
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		Map<String, String> testHeaders = new HashMap<>();
		testHeaders.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		testHeaders.put(Constants.BUNDLE_SYMBOLICNAME, getName());
		testHeaders.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, "JavaSE-9");
		File testBundle = createBundle(config, getName(), testHeaders);
		Bundle b = systemContext.installBundle("reference:file:///" + testBundle.getAbsolutePath()); //$NON-NLS-1$
		long bid = b.getBundleId();

		// should resolve fine
		Assert.assertTrue("Could not resolve bundle.", equinox.adapt(FrameworkWiring.class).resolveBundles(Collections.singleton(b)));

		// put the framework back to the RESOLVED state
		stop(equinox);

		// configure equinox for java 8
		configuration.put("osgi.java.profile", javaSE8Profile.toExternalForm());
		equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		// bundle should fail to resolve
		b = equinox.getBundleContext().getBundle(bid);
		Assert.assertFalse("Could resolve bundle.", equinox.adapt(FrameworkWiring.class).resolveBundles(Collections.singleton(b)));

		// put the framework back to the RESOLVED state
		stop(equinox);

		// move back to java 9
		configuration.put("osgi.java.profile", javaSE9Profile.toExternalForm());
		equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		// bundle should succeed to resolve again
		b = equinox.getBundleContext().getBundle(bid);
		Assert.assertTrue("Could not resolve bundle.", equinox.adapt(FrameworkWiring.class).resolveBundles(Collections.singleton(b)));

		// put the framework back to the RESOLVED state
		stop(equinox);
	}

	public void testMRUBundleFileList() {
		doMRUBundleFileList(10);
	}

	//	public void testMRUBundleFileListExpectedToFail() {
	//		doMRUBundleFileList(0);
	//	}

	private void doMRUBundleFileList(int limit) {
		// create/start/stop/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put("osgi.bundlefile.limit", Integer.toString(limit)); //$NON-NLS-1$//$NON-NLS-2$

		final Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		File[] testBundles = null;
		try {
			testBundles = createBundles(new File(config, "bundles"), 3000); //$NON-NLS-1$
		} catch (IOException e) {
			fail("Unexpected error creating budnles", e); //$NON-NLS-1$
		}
		for (File testBundle : testBundles) {
			try {
				systemContext.installBundle("reference:file:///" + testBundle.getAbsolutePath()); //$NON-NLS-1$
			} catch (BundleException e) {
				fail("Unexpected install error", e); //$NON-NLS-1$
			}
		}
		// put the framework back to the RESOLVED state
		stop(equinox);

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}

		openAllBundleFiles(equinox.getBundleContext());

		update(equinox);

		// we can either have a hack here that waits until the system bundle is active
		// or we can just try to start it and race with the update() call above
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}

		openAllBundleFiles(equinox.getBundleContext());

		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	private void openAllBundleFiles(BundleContext context) {
		Bundle[] bundles = context.getBundles();
		// get an entry from each bundle to ensure each one gets opened.
		try {
			for (Bundle bundle : bundles) {
				assertNotNull("No manifest for: " + bundle, bundle.getEntry("/META-INF/MANIFEST.MF"));
			}
		} catch (Throwable t) {
			// An exception used to get thrown here when we tried to close
			// the least used bundle file
			fail("Failed to get bundle entries", t);
		}
	}

	public void testURLExternalFormat01() {
		// create multiple instances test
		File config1 = OSGiTestsActivator.getContext().getDataFile("testURLExternalFormat01_1"); //$NON-NLS-1$
		Map<String, Object> configuration1 = new HashMap<>();
		configuration1.put(Constants.FRAMEWORK_STORAGE, config1.getAbsolutePath());
		Equinox equinox1 = new Equinox(configuration1);
		try {
			equinox1.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox1.getState()); //$NON-NLS-1$

		File config2 = OSGiTestsActivator.getContext().getDataFile("testURLExternalFormat01_2"); //$NON-NLS-1$
		Map<String, Object> configuration2 = new HashMap<>();
		configuration2.put(Constants.FRAMEWORK_STORAGE, config2.getAbsolutePath());
		Equinox equinox2 = new Equinox(configuration2);
		try {
			equinox2.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox2.getState()); //$NON-NLS-1$

		BundleContext systemContext1 = equinox1.getBundleContext();
		assertNotNull("System context is null", systemContext1); //$NON-NLS-1$
		BundleContext systemContext2 = equinox2.getBundleContext();
		assertNotNull("System context is null", systemContext2); //$NON-NLS-1$

		assertNotSame(systemContext1, systemContext2);

		Bundle test1 = null;
		Bundle test2 = null;
		try {
			test1 = systemContext1.installBundle(installer.getBundleLocation("test"));//$NON-NLS-1$
			test2 = systemContext2.installBundle(installer.getBundleLocation("test"));//$NON-NLS-1$
		} catch (BundleException e) {
			fail("Unexpected error installing bundle", e);//$NON-NLS-1$
		}
		URL entry1 = test1.getEntry("data/resource1"); //$NON-NLS-1$
		assertNotNull("entry1", entry1); //$NON-NLS-1$
		URL entry2 = test2.getEntry("data/resource1"); //$NON-NLS-1$
		assertNotNull("entry2", entry2); //$NON-NLS-1$
		assertFalse("External form is equal: " + entry1.toExternalForm(), entry1.toExternalForm().equals(entry2.toExternalForm())); //$NON-NLS-1$
		assertFalse("Host is equal: " + entry1.getHost(), entry1.getHost().equals(entry2.getHost())); //$NON-NLS-1$
		assertFalse("URL is equal: " + entry1.toExternalForm(), entry1.equals(entry2)); //$NON-NLS-1$

		Bundle substitutes1 = null;
		Bundle substitutes2 = null;
		try {
			substitutes1 = systemContext1.installBundle(installer.getBundleLocation("substitutes.a"));//$NON-NLS-1$
			substitutes2 = systemContext2.installBundle(installer.getBundleLocation("substitutes.a"));//$NON-NLS-1$
		} catch (BundleException e) {
			fail("Unexpected error installing bundle", e);//$NON-NLS-1$
		}

		entry1 = substitutes1.getResource("data/resource1"); //$NON-NLS-1$
		assertNotNull("entry1", entry1); //$NON-NLS-1$
		entry2 = substitutes2.getResource("data/resource1"); //$NON-NLS-1$
		assertNotNull("entry2", entry2); //$NON-NLS-1$
		assertFalse("External form is equal: " + entry1.toExternalForm(), entry1.toExternalForm().equals(entry2.toExternalForm())); //$NON-NLS-1$
		assertFalse("Host is equal: " + entry1.getHost(), entry1.getHost().equals(entry2.getHost())); //$NON-NLS-1$
		assertFalse("URL is equal: " + entry1.toExternalForm(), entry1.equals(entry2)); //$NON-NLS-1$

		// put the framework 1 back to the RESOLVED state
		stop(equinox1);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox1.getState()); //$NON-NLS-1$

		// put the framework 2 back to the RESOLVED state
		stop(equinox2);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox2.getState()); //$NON-NLS-1$
	}

	class TestHandler extends AbstractURLStreamHandlerService {

		public URLConnection openConnection(URL u) throws IOException {
			throw new IOException();
		}

		@Override
		public URLConnection openConnection(URL u, Proxy p) throws IOException {
			throw new IOException();
		}

	}

	public void testURLMultiplexing01() throws BundleException {
		// create multiple instances of Equinox to test
		File config1 = OSGiTestsActivator.getContext().getDataFile(getName() + "_1");
		Map<String, Object> configuration1 = new HashMap<>();
		configuration1.put(Constants.FRAMEWORK_STORAGE, config1.getAbsolutePath());
		Equinox equinox1 = new Equinox(configuration1);
		try {
			equinox1.start();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox1.getState()); //$NON-NLS-1$

		File config2 = OSGiTestsActivator.getContext().getDataFile(getName() + "_2"); //$NON-NLS-1$
		Map<String, Object> configuration2 = new HashMap<>();
		configuration2.put(Constants.FRAMEWORK_STORAGE, config2.getAbsolutePath());
		Equinox equinox2 = new Equinox(configuration2);
		try {
			equinox2.start();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox2.getState()); //$NON-NLS-1$

		BundleContext systemContext1 = equinox1.getBundleContext();
		assertNotNull("System context is null", systemContext1); //$NON-NLS-1$
		BundleContext systemContext2 = equinox2.getBundleContext();
		assertNotNull("System context is null", systemContext2); //$NON-NLS-1$

		assertNotSame(systemContext1, systemContext2);

		// register a protocol hander in the "root" framework
		Dictionary props = new Hashtable();
		props.put(URLConstants.URL_HANDLER_PROTOCOL, getName().toLowerCase());
		ServiceRegistration handlerReg = OSGiTestsActivator.getContext().registerService(URLStreamHandlerService.class, new TestHandler(), props);
		try {
			URL baseTestUrl = new URL(getName().toLowerCase(), "", "/test/url");
			System.getProperties().put("test.url", baseTestUrl);
			System.setProperty("test.url.spec", baseTestUrl.toExternalForm());
		} catch (MalformedURLException e) {
			fail("Unexpected url exception.", e);
		}

		Bundle geturlBundle = systemContext1.installBundle(installer.getBundleLocation("geturl"));
		geturlBundle.start();
		PrivilegedAction geturlAction = systemContext1.getService(systemContext1.getServiceReference(PrivilegedAction.class));
		try {
			geturlAction.run();
		} catch (Exception e) {
			fail("Unexpected exception", e);
		}

		// put the framework 1 back to the RESOLVED state
		stop(equinox1);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox1.getState()); //$NON-NLS-1$

		// put the framework 2 back to the RESOLVED state
		stop(equinox2);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox2.getState()); //$NON-NLS-1$
		handlerReg.unregister();
		System.getProperties().remove("test.url");
	}

	public void testUUID() {
		File config1 = OSGiTestsActivator.getContext().getDataFile(getName() + "_1"); //$NON-NLS-1$
		Map configuration1 = new HashMap();
		configuration1.put(Constants.FRAMEWORK_STORAGE, config1.getAbsolutePath());
		Equinox equinox1 = new Equinox(configuration1);
		try {
			equinox1.init();
		} catch (BundleException e) {
			fail("Failed init", e);
		}
		String uuid1_1 = equinox1.getBundleContext().getProperty(Constants.FRAMEWORK_UUID);
		verifyUUID(uuid1_1);

		File config2 = OSGiTestsActivator.getContext().getDataFile(getName() + "_2"); //$NON-NLS-1$
		Map configuration2 = new HashMap();
		configuration2.put(Constants.FRAMEWORK_STORAGE, config2.getAbsolutePath());
		Equinox equinox2 = new Equinox(configuration1);
		try {
			equinox2.init();
		} catch (BundleException e) {
			fail("Failed init", e);
		}
		String uuid2_1 = equinox2.getBundleContext().getProperty(Constants.FRAMEWORK_UUID);
		verifyUUID(uuid2_1);

		assertFalse("UUIDs are the same: " + uuid1_1, uuid1_1.equals(uuid2_1));

		stop(equinox1);
		stop(equinox2);

		try {
			equinox1.init();
			equinox2.init();
		} catch (BundleException e) {
			fail("Failed to re-init frameworks.", e);
		}

		String uuid1_2 = equinox1.getBundleContext().getProperty(Constants.FRAMEWORK_UUID);
		verifyUUID(uuid1_2);
		String uuid2_2 = equinox2.getBundleContext().getProperty(Constants.FRAMEWORK_UUID);
		verifyUUID(uuid2_2);
		assertFalse("UUIDs are the same: " + uuid1_1, uuid1_1.equals(uuid1_2));
		assertFalse("UUIDs are the same: " + uuid1_2, uuid1_2.equals(uuid2_2));
		assertFalse("UUIDs are the same: " + uuid2_1, uuid2_1.equals(uuid2_2));

		stop(equinox1);
		stop(equinox2);
	}

	private void verifyUUID(String uuid) {
		assertNotNull("Null uuid.", uuid);
		StringTokenizer st = new StringTokenizer(uuid, "-");
		String[] uuidSections = new String[5];
		// All UUIDs must have 5 sections
		for (int i = 0; i < uuidSections.length; i++) {
			try {
				uuidSections[i] = "0x" + st.nextToken();
			} catch (NoSuchElementException e) {
				fail("Wrong number of uuid sections: " + uuid, e);
			}
		}
		// make sure there is not an extra section.
		try {
			st.nextToken();
			fail("Too many sections in uuid: " + uuid);
		} catch (NoSuchElementException e) {
			// expected
		}
		// now verify each section of the UUID can be decoded as a hex string and is the correct size
		for (int i = 0; i < uuidSections.length; i++) {
			int limit = 0;
			switch (i) {
				case 0 : {
					limit = 10; // "0x" + 4*<hexOctet> == 10 len
					break;
				}
				case 1 :
				case 2 :
				case 3 : {
					limit = 6; // "0x" + 2*<hexOctet> == 6 len
					break;
				}
				case 4 : {
					limit = 14; // "0x" + 6*<hexOctet> == 14 len
					break;
				}
				default :
					break;
			}
			assertTrue("UUISection is too big: " + uuidSections[i], uuidSections[i].length() <= limit);
			try {
				Long.decode(uuidSections[i]);
			} catch (NumberFormatException e) {
				fail("Invalid section: " + uuidSections[i], e);
			}
		}
	}

	public void testBug304213() {
		// test installing bundle with empty manifest
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());

		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		File bundleFile = null;
		try {
			File baseDir = new File(config, "bundles");
			baseDir.mkdirs();
			bundleFile = createBundle(baseDir, getName(), true, true);
		} catch (IOException e) {
			fail("Unexpected error creating bundles.", e);
		}
		try {
			systemContext.installBundle("reference:file:///" + bundleFile.getAbsolutePath()); //$NON-NLS-1$
		} catch (BundleException e) {
			fail("Unexpected install error", e); //$NON-NLS-1$
		}

		// put the framework back to the RESOLVED state
		stop(equinox);

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		// remove manifest for testing
		new File(bundleFile, "META-INF/MANIFEST.MF").delete();
		systemContext = equinox.getBundleContext();
		Bundle[] bundles = systemContext.getBundles();
		// get the headers from each bundle
		try {
			for (Bundle bundle : bundles) {
				bundle.getHeaders(); //$NON-NLS-1$
			}
		} catch (Throwable t) {
			// An exception used to get thrown here when we tried to close
			// the least used bundle file
			fail("Failed to get bundle entries", t);
		}

		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testBug351083DevClassPath() throws InvalidSyntaxException {
		// create/start/stop/start/stop test
		BundleInstaller testBundleInstaller = new BundleInstaller("test_files/devCPTests", OSGiTestsActivator.getContext());

		try {
			File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
			Map<String, Object> configuration = new HashMap<>();
			configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
			configuration.put("osgi.dev", "../devCP");
			Equinox equinox = new Equinox(configuration);
			try {
				equinox.start();
			} catch (BundleException e) {
				fail("Unexpected exception in init()", e); //$NON-NLS-1$
			}
			BundleContext systemContext = equinox.getBundleContext();
			assertNotNull("System context is null", systemContext); //$NON-NLS-1$
			// try installing a bundle before starting
			Bundle tb1 = null;
			try {
				tb1 = systemContext.installBundle(testBundleInstaller.getBundleLocation("tb1")); //$NON-NLS-1$
			} catch (BundleException e1) {
				fail("failed to install a bundle", e1); //$NON-NLS-1$
			}
			URL resource = tb1.getResource("tb1/resource.txt");
			assertNotNull("Resource is null", resource);

			stop(equinox);
			assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
		} finally {
			testBundleInstaller.shutdown();
		}
	}

	public void testBug352275() {
		// simple test to create an embedded framework
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "");
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void disabledTestBug351519RefreshEnabled() {
		// TODO this is expected to fail.  Not sure we should implement this
		doTestBug351519Refresh(Boolean.TRUE);
	}

	public void testBug351519RefreshDisabled() {
		doTestBug351519Refresh(Boolean.FALSE);
	}

	public void testBug351519RefreshDefault() {
		// Note that for the unity framework this defaults to false
		doTestBug351519Refresh(null);
	}

	public void testWeavingPersistence() {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Unexpected exception in start()", e); //$NON-NLS-1$
		}

		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$

		Bundle test1 = null;
		try {
			test1 = systemContext.installBundle(installer.getBundleLocation("substitutes.a"));
		} catch (BundleException e) {
			fail("Unexpected error installing bundle", e);//$NON-NLS-1$
		}
		long testID1 = test1.getBundleId();

		final Bundle testFinal1 = test1;
		ServiceRegistration reg = systemContext.registerService(WeavingHook.class, new WeavingHook() {
			public void weave(WovenClass wovenClass) {
				if (!testFinal1.equals(wovenClass.getBundleWiring().getBundle()))
					return;
				if (!"substitutes.x.Ax".equals(wovenClass.getClassName()))
					return;
				List dynamicImports = wovenClass.getDynamicImports();
				dynamicImports.add("*");
			}
		}, null);

		try {
			testFinal1.loadClass("substitutes.x.Ax");
			testFinal1.loadClass("org.osgi.framework.hooks.bundle.FindHook");
		} catch (Throwable t) {
			fail("Unexpected testing bundle", t);
		} finally {
			reg.unregister();
		}
		// put the framework back to the RESOLVED state
		stop(equinox);

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Unexpected exception in start()", e); //$NON-NLS-1$
		}

		systemContext = equinox.getBundleContext();
		test1 = systemContext.getBundle(testID1);

		Bundle test2 = null;
		try {
			test2 = systemContext.installBundle(installer.getBundleLocation("exporter.importer1"));
		} catch (BundleException e) {
			fail("Unexpected error installing bundle", e);//$NON-NLS-1$
		}
		long testID2 = test2.getBundleId();

		final Bundle testFinal2 = test2;
		reg = systemContext.registerService(WeavingHook.class, new WeavingHook() {
			public void weave(WovenClass wovenClass) {
				if (!testFinal2.equals(wovenClass.getBundleWiring().getBundle()))
					return;
				if (!"exporter.importer.test.Test1".equals(wovenClass.getClassName()))
					return;
				List dynamicImports = wovenClass.getDynamicImports();
				dynamicImports.add("*");
			}
		}, null);

		try {
			testFinal2.loadClass("exporter.importer.test.Test1");
			testFinal2.loadClass("org.osgi.framework.hooks.service.FindHook");
		} catch (Throwable t) {
			fail("Unexpected testing bundle", t);
		} finally {
			reg.unregister();
		}

		// put the framework back to the RESOLVED state
		stop(equinox);

		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Unexpected exception in start()", e); //$NON-NLS-1$
		}

		systemContext = equinox.getBundleContext();
		test1 = systemContext.getBundle(testID1);
		test2 = systemContext.getBundle(testID2);

		BundleRevision rev1 = test1.adapt(BundleRevision.class);
		BundleRevision rev2 = test2.adapt(BundleRevision.class);
		BundleWiring wiring1 = rev1.getWiring();
		BundleWiring wiring2 = rev2.getWiring();

		assertNotNull("wiring1 is null", wiring1);
		assertNotNull("wiring2 is null", wiring2);

		List packages1 = wiring1.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);
		List packages2 = wiring2.getRequiredWires(BundleRevision.PACKAGE_NAMESPACE);

		// could make this a more complete check, but with the bug the dynamic wires
		// are missing altogether because we fail to save the resolver state cache.
		assertEquals("Wrong number of wires for wiring1", 1, packages1.size());
		assertEquals("Wrong number of wires for wiring2", 1, packages2.size());

		stop(equinox);
	}

	private void doTestBug351519Refresh(Boolean refreshDuplicates) {
		// Create a framework with equinox.refresh.duplicate.bsn=false configuration
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		if (refreshDuplicates != null) {
			configuration.put("equinox.refresh.duplicate.bsn", refreshDuplicates.toString());
		} else {
			// we default to false now
			refreshDuplicates = Boolean.FALSE;
		}
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		BundleContext systemContext = equinox.getBundleContext();

		systemContext.registerService(ResolverHookFactory.class, new ResolverHookFactory() {
			public ResolverHook begin(Collection triggers) {
				return new ResolverHook() {
					public void filterResolvable(Collection candidates) {
						// nothing
					}

					public void filterSingletonCollisions(BundleCapability singleton, Collection collisionCandidates) {
						// resolve all singletons
						collisionCandidates.clear();
					}

					public void filterMatches(BundleRequirement requirement, Collection candidates) {
						// nothing
					}

					public void end() {
						// nothing
					}
				};
			}
		}, null);

		BundleInstaller testBundleInstaller = null;
		BundleInstaller testBundleResolver = null;
		try {
			testBundleResolver = new BundleInstaller(OSGiTestsActivator.TEST_FILES_ROOT + "wiringTests/bundles", systemContext);
			testBundleInstaller = new BundleInstaller(OSGiTestsActivator.TEST_FILES_ROOT + "wiringTests/bundles", getContext());
		} catch (InvalidSyntaxException e) {
			fail("Failed to create installers.", e);
		}
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// try installing a bundle before starting
		Bundle tb1v1 = null, tb1v2 = null;
		try {
			tb1v1 = systemContext.installBundle(testBundleInstaller.getBundleLocation("singleton.tb1v1")); //$NON-NLS-1$
			tb1v2 = systemContext.installBundle(testBundleInstaller.getBundleLocation("singleton.tb1v2")); //$NON-NLS-1$
		} catch (BundleException e1) {
			fail("failed to install a bundle", e1); //$NON-NLS-1$
		}

		assertTrue("Could not resolve test bundles", testBundleResolver.resolveBundles(new Bundle[] {tb1v1, tb1v2}));
		Bundle[] refreshed = testBundleResolver.refreshPackages(new Bundle[] {tb1v1});
		if (refreshDuplicates) {
			List refreshedList = Arrays.asList(refreshed);
			assertEquals("Wrong number of refreshed bundles", 2, refreshed.length);
			assertTrue("Refreshed bundles does not include v1", refreshedList.contains(tb1v1));
			assertTrue("Refreshed bundles does not include v2", refreshedList.contains(tb1v2));
		} else {
			assertEquals("Wrong number of refreshed bundles", 1, refreshed.length);
			assertEquals("Refreshed bundles does not include v1", refreshed[0], tb1v1);
		}

		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testBug375784() {
		// Create a framework with osgi.context.bootdelegation=true configuration
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put("osgi.context.bootdelegation", "true");
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$

		// try installing a bundle before starting
		Bundle tb1;
		try {
			tb1 = systemContext.installBundle(installer.getBundleLocation("test.bug375784")); //$NON-NLS-1$
			tb1.start();
		} catch (BundleException e1) {
			fail("failed to install and start test bundle", e1); //$NON-NLS-1$
		}

		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testBug258209_1() throws BundleException {
		// create a framework to test thread context class loaders
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());

		ClassLoader current = Thread.currentThread().getContextClassLoader();
		Equinox equinox = new Equinox(configuration);
		equinox.init();
		Thread.currentThread().setContextClassLoader(current);

		BundleContext systemContext = equinox.getBundleContext();
		Bundle testTCCL = systemContext.installBundle(installer.getBundleLocation("test.tccl")); //$NON-NLS-1$
		equinox.adapt(FrameworkWiring.class).resolveBundles(Arrays.asList(testTCCL));
		try {
			testTCCL.start();
		} catch (BundleException e) {
			fail("Unexpected exception starting bundle", e); //$NON-NLS-1$
		}

		assertEquals("Unexpected state", Bundle.RESOLVED, testTCCL.getState()); //$NON-NLS-1$
		// this will start the framework on the current thread; test that the correct tccl is used
		equinox.start();
		assertEquals("Unexpected state", Bundle.ACTIVE, testTCCL.getState()); //$NON-NLS-1$

		update(equinox);

		checkActive(testTCCL);

		systemContext = equinox.getBundleContext();
		assertEquals("Unexpected state", Bundle.ACTIVE, testTCCL.getState()); //$NON-NLS-1$

		// test that the correct tccl is used for refresh packages
		equinox.adapt(FrameworkWiring.class).refreshBundles(Arrays.asList(testTCCL));
		checkActive(testTCCL);
		assertEquals("Unexpected state", Bundle.ACTIVE, testTCCL.getState()); //$NON-NLS-1$

		// use the tccl service to start the test bundle.
		ClassLoader serviceTCCL = null;
		try {
			serviceTCCL = (ClassLoader) systemContext.getService(systemContext.getServiceReferences(ClassLoader.class.getName(), "(equinox.classloader.type=contextClassLoader)")[0]);//$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("Unexpected", e);//$NON-NLS-1$
		}
		current = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(serviceTCCL);
		try {
			testTCCL.stop();
			testTCCL.start();
		} catch (BundleException e) {
			fail("Unepected", e); //$NON-NLS-1$
		} finally {
			Thread.currentThread().setContextClassLoader(current);
		}
		stop(equinox);
	}

	private void checkActive(Bundle b) {
		try {
			// just a hack to make sure we are restarted
			Thread.sleep(500);
			if (b.getState() != Bundle.ACTIVE)
				Thread.sleep(500);
		} catch (Exception e) {
			fail("Unexpected exception", e); //$NON-NLS-1$
		}
	}

	public void testBug413879() {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Unexpected exception in start()", e); //$NON-NLS-1$
		}

		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$

		Bundle test1 = null;
		try {
			test1 = systemContext.installBundle(installer.getBundleLocation("substitutes.a"));
		} catch (BundleException e) {
			fail("Unexpected error installing bundle", e);//$NON-NLS-1$
		}

		final Bundle testFinal1 = test1;
		ServiceRegistration reg = systemContext.registerService(WeavingHook.class, new WeavingHook() {
			public void weave(WovenClass wovenClass) {
				if (!testFinal1.equals(wovenClass.getBundleWiring().getBundle()))
					return;
				if (!"substitutes.x.Ax".equals(wovenClass.getClassName()))
					return;
				List dynamicImports = wovenClass.getDynamicImports();
				dynamicImports.add("*");
			}
		}, null);

		ServiceRegistration<ResolverHookFactory> resolverHookReg = systemContext.registerService(ResolverHookFactory.class, new ResolverHookFactory() {
			@Override
			public ResolverHook begin(Collection<BundleRevision> triggers) {
				// just trying to delay the resolve so that we get two threads trying to apply off the same snapshot
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}
				return null;
			}
		}, null);

		final Set<Throwable> errors = Collections.newSetFromMap(new ConcurrentHashMap<Throwable, Boolean>());
		try {
			Runnable dynamicLoadClass = new Runnable() {
				@Override
				public void run() {
					try {
						testFinal1.loadClass("substitutes.x.Ax");
						testFinal1.loadClass("org.osgi.framework.hooks.bundle.FindHook");
					} catch (Throwable t) {
						errors.add(t);
					}
				}
			};
			Thread t1 = new Thread(dynamicLoadClass, getName() + "-1");
			Thread t2 = new Thread(dynamicLoadClass, getName() + "-2");
			t1.start();
			t2.start();
			t1.join();
			t2.join();
		} catch (Throwable t) {
			fail("Unexpected testing bundle", t);
		} finally {
			reg.unregister();
			resolverHookReg.unregister();
		}
		// put the framework back to the RESOLVED state
		stop(equinox);

		if (!errors.isEmpty()) {
			fail("Failed to resolve dynamic", errors.iterator().next());
		}
	}

	public void testBug414070() throws BundleException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		equinox.init();

		BundleContext systemContext = equinox.getBundleContext();
		Bundle systemBundle = systemContext.getBundle();

		Bundle chainTest = systemContext.installBundle(installer.getBundleLocation("chain.test")); //$NON-NLS-1$

		final Bundle chainTestD = systemContext.installBundle(installer.getBundleLocation("chain.test.d")); //$NON-NLS-1$
		Bundle chainTestA = systemContext.installBundle(installer.getBundleLocation("chain.test.a")); //$NON-NLS-1$
		Bundle chainTestB = systemContext.installBundle(installer.getBundleLocation("chain.test.b")); //$NON-NLS-1$
		Bundle chainTestC = systemContext.installBundle(installer.getBundleLocation("chain.test.c")); //$NON-NLS-1$
		systemContext.registerService(WeavingHook.class, new WeavingHook() {
			public void weave(WovenClass wovenClass) {
				if (!chainTestD.equals(wovenClass.getBundleWiring().getBundle()))
					return;
				if (!"chain.test.d.DMultipleChain1".equals(wovenClass.getClassName()))
					return;
				List dynamicImports = wovenClass.getDynamicImports();
				dynamicImports.add("*");
			}
		}, null);

		equinox.start();

		chainTest.loadClass("chain.test.TestMultiChain").newInstance(); //$NON-NLS-1$
		// force a dynamic wire to cause a cycle
		chainTestD.loadClass("chain.test.a.AMultiChain1");

		// make sure all bundles are active now
		assertEquals("A is not active.", Bundle.ACTIVE, chainTestA.getState());
		assertEquals("B is not active.", Bundle.ACTIVE, chainTestB.getState());
		assertEquals("C is not active.", Bundle.ACTIVE, chainTestC.getState());
		assertEquals("D is not active.", Bundle.ACTIVE, chainTestD.getState());
		// record STOPPING order
		final List<Bundle> stoppingOrder = new ArrayList<>();
		systemContext.addBundleListener(new SynchronousBundleListener() {
			@Override
			public void bundleChanged(BundleEvent event) {
				if (event.getType() == BundleEvent.STOPPING) {
					stoppingOrder.add(event.getBundle());
				}
			}
		});
		stop(equinox);

		List<Bundle> expectedOrder = Arrays.asList(systemBundle, chainTest, chainTestA, chainTestB, chainTestC, chainTestD);
		assertEquals("Wrong stopping order", expectedOrder.toArray(), stoppingOrder.toArray());
	}

	public void testBug412228() throws BundleException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		equinox.init();

		BundleContext systemContext = equinox.getBundleContext();

		Bundle b = systemContext.installBundle(installer.getBundleLocation("test.bug412228"));
		b.start();
		equinox.start();

		long startTime = System.currentTimeMillis();
		stop(equinox, true, 500);
		long stopTime = System.currentTimeMillis() - startTime;
		if (stopTime > 2000) {
			fail("waitForStop time took too long: " + stopTime);
		}

	}

	public void testBug432632() throws BundleException, IOException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		config.mkdirs();
		// create a config.ini with some system property substitutes
		Properties configIni = new Properties();
		configIni.setProperty("test.substitute1", "Some.$test.prop1$.test");
		configIni.setProperty("test.substitute2", "Some.$test.prop2$.test");
		configIni.store(new FileOutputStream(new File(config, "config.ini")), "Test config.ini");
		// Only provide substitution for the first prop
		System.setProperty("test.prop1", "PASSED");
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		equinox.init();

		BundleContext systemContext = equinox.getBundleContext();
		// check for substitution
		assertEquals("Wrong value for test.substitute1", "Some.PASSED.test", systemContext.getProperty("test.substitute1"));
		// check that non-substitution keeps $ delimiters.
		assertEquals("Wrong value for test.substitute2", "Some.$test.prop2$.test", systemContext.getProperty("test.substitute2"));
		stop(equinox);
	}

	public void testDynamicSecurityManager() throws BundleException {
		SecurityManager sm = System.getSecurityManager();
		assertNull("SecurityManager must be null to test.", sm);
		try {
			File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
			Map<String, Object> configuration = new HashMap<>();
			configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
			Equinox equinox = new Equinox(configuration);
			try {
				equinox.start();
			} catch (BundleException e) {
				fail("Unexpected exception in start()", e); //$NON-NLS-1$
			}
			Bundle substitutesA = null;
			try {
				substitutesA = equinox.getBundleContext().installBundle(installer.getBundleLocation("substitutes.a")); //$NON-NLS-1$
			} catch (BundleException e1) {
				fail("failed to install a bundle", e1); //$NON-NLS-1$
			}
			assertTrue("BundleCould not resolve.", equinox.adapt(FrameworkWiring.class).resolveBundles(Collections.singleton(substitutesA)));
			substitutesA.adapt(BundleWiring.class).findEntries("/", null, 0);
			// set security manager after resolving
			System.setSecurityManager(new SecurityManager() {

				@Override
				public void checkPermission(Permission perm, Object context) {
					// do nothing
				}

				@Override
				public void checkPermission(Permission perm) {
					// do nothing
				}
			});
			stop(equinox);
			assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
		} finally {
			System.setSecurityManager(null);
		}
	}

	static final String nullTest = "null.test";

	public void testNullConfigurationValue() throws BundleException {
		System.setProperty(nullTest, "system");
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(nullTest, null);
		Equinox equinox = new Equinox(configuration);
		equinox.start();
		String nullValue = equinox.getBundleContext().getProperty(nullTest);
		assertNull(nullTest + " is not null: " + nullValue, nullValue);
		String systemNullValue = System.getProperty(nullTest);
		assertEquals("Wrong system null value.", "system", systemNullValue);
		equinox.stop();
	}

	public void testNullConfigurationValueRequiredProperty() throws BundleException {
		final String systemProcessor = System.getProperty(Constants.FRAMEWORK_PROCESSOR);
		assertNotNull(systemProcessor);
		try {
			System.setProperty(Constants.FRAMEWORK_PROCESSOR, "hyperflux");
			File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
			Map<String, Object> configuration = new HashMap<>();
			configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
			configuration.put(Constants.FRAMEWORK_PROCESSOR, null);
			Equinox equinox = new Equinox(configuration);
			equinox.start();
			String processor = equinox.getBundleContext().getProperty(Constants.FRAMEWORK_PROCESSOR);
			assertEquals("Wrong " + Constants.FRAMEWORK_PROCESSOR, systemProcessor, processor);
			String systemValue = System.getProperty(Constants.FRAMEWORK_PROCESSOR);
			assertEquals("Wrong system value.", "hyperflux", systemValue);
			equinox.stop();
		} finally {
			System.setProperty(Constants.FRAMEWORK_PROCESSOR, systemProcessor);
		}
	}

	public void testAllNullConfigurationValues() throws BundleException {
		Collection<String> requiredProperties = Arrays.asList( // prevent bad formatting...
				Constants.FRAMEWORK_EXECUTIONENVIRONMENT, //
				Constants.FRAMEWORK_LANGUAGE, //
				Constants.FRAMEWORK_OS_NAME, //
				Constants.FRAMEWORK_OS_VERSION, //
				Constants.FRAMEWORK_PROCESSOR, //
				Constants.FRAMEWORK_STORAGE, //
				Constants.FRAMEWORK_SYSTEMCAPABILITIES, //
				Constants.FRAMEWORK_SYSTEMPACKAGES, //
				Constants.FRAMEWORK_UUID, //
				Constants.FRAMEWORK_VENDOR, //
				Constants.FRAMEWORK_VERSION, //
				Constants.SUPPORTS_FRAMEWORK_EXTENSION, //
				Constants.SUPPORTS_FRAMEWORK_FRAGMENT, //
				Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE, //
				EquinoxConfiguration.PROP_FRAMEWORK, //
				EquinoxConfiguration.PROP_OSGI_ARCH, //
				EquinoxConfiguration.PROP_OSGI_OS, //
				EquinoxConfiguration.PROP_OSGI_WS, //
				EquinoxConfiguration.PROP_OSGI_WS, //
				EquinoxConfiguration.PROP_OSGI_NL, //
				EquinoxConfiguration.PROP_STATE_SAVE_DELAY_INTERVAL, //
				EquinoxConfiguration.PROP_INIT_UUID, //
				"gosh.args", //
				EquinoxLocations.PROP_HOME_LOCATION_AREA, //
				EquinoxLocations.PROP_CONFIG_AREA, //
				EquinoxLocations.PROP_INSTALL_AREA, //
				EclipseStarter.PROP_LOGFILE //
		);
		Properties systemProperties = (Properties) System.getProperties().clone();
		Map<String, Object> configuration = new HashMap<>();
		for (Object key : systemProperties.keySet()) {
			configuration.put((String) key, null);
		}
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		equinox.start();
		for (Object key : systemProperties.keySet()) {
			String property = (String) key;
			String value = equinox.getBundleContext().getProperty(property);
			if (requiredProperties.contains(property)) {
				assertNotNull(property + " is null", value);
			} else {
				assertNull(property + " is not null", value);
			}
			String systemValue = System.getProperty(property);
			assertEquals("Wrong system value for " + property, systemProperties.getProperty(property), systemValue);
		}
		assertEquals(systemProperties, System.getProperties());
		equinox.stop();
	}

	public void testNullConfigurationValueSystemProperties() throws BundleException {
		System.setProperty(nullTest, "system");
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put("osgi.framework.useSystemProperties", "true");
		configuration.put(nullTest, null);
		Equinox equinox = new Equinox(configuration);
		equinox.start();

		String nullValue = equinox.getBundleContext().getProperty(nullTest);
		assertNull(nullTest + " is not null: " + nullValue, nullValue);
		assertNull("Did not get null system value.", System.getProperties().get(nullTest));

		// also test EnvironmentInfo effects on system properties
		ServiceReference<EnvironmentInfo> envRef = equinox.getBundleContext().getServiceReference(EnvironmentInfo.class);
		EnvironmentInfo envInfo = equinox.getBundleContext().getService(envRef);
		envInfo.setProperty(getName(), getName());
		assertEquals("Got wrong value from system properties.", System.getProperty(getName()), getName());
		envInfo.setProperty(getName(), null);
		assertNull("Did not get null system value.", System.getProperties().get(getName()));
		equinox.stop();
	}

	public void testBackedBySystemReplaceSystemProperties() throws BundleException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put("osgi.framework.useSystemProperties", "true");
		Equinox equinox = new Equinox(configuration);
		equinox.start();
		ServiceReference<EnvironmentInfo> envRef = equinox.getBundleContext().getServiceReference(EnvironmentInfo.class);
		EnvironmentInfo envInfo = equinox.getBundleContext().getService(envRef);

		// replace the system properties with a copy
		Properties copy = new Properties();
		copy.putAll(System.getProperties());
		System.setProperties(copy);

		// set a system prop for this test after replacement of the properties object
		String systemKey = getName() + ".system";
		System.setProperty(systemKey, getName());

		// make sure context properties reflect the new test prop
		assertEquals("Wrong context value", getName(), equinox.getBundleContext().getProperty(systemKey));

		// also test EnvironmentInfo properties
		assertEquals("Wrong context value", getName(), envInfo.getProperty(systemKey));
		assertEquals("Wrong EquinoxConfiguration config value", getName(), ((EquinoxConfiguration) envInfo).getConfiguration(systemKey));

		// set environment info prop
		String envKey = getName() + ".env";
		envInfo.setProperty(envKey, getName());

		// make sure the system properties reflect the new test prop
		assertEquals("Wrong system value", getName(), System.getProperty(envKey));
		equinox.stop();
	}

	public void testLocalConfigReplaceSystemProperties() throws BundleException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		equinox.start();
		ServiceReference<EnvironmentInfo> envRef = equinox.getBundleContext().getServiceReference(EnvironmentInfo.class);
		EnvironmentInfo envInfo = equinox.getBundleContext().getService(envRef);

		// replace the system properties with a copy
		Properties copy = new Properties();
		copy.putAll(System.getProperties());
		System.setProperties(copy);

		// set a system prop for this test after replacement of the properties object
		String systemKey = getName() + ".system";
		System.setProperty(systemKey, getName());

		// make sure context properties reflect the new system test prop.
		// remember context properties are backed by system properties
		assertEquals("Wrong context value", getName(), equinox.getBundleContext().getProperty(systemKey));

		// also test EnvironmentInfo properties.
		// remember the getProperty method is backed by system properties
		assertEquals("Wrong context value", getName(), envInfo.getProperty(systemKey));
		// config options are not backed by system properties when framework is not using system properties for configuration
		assertNull("Wrong EquinoxConfiguration config value", ((EquinoxConfiguration) envInfo).getConfiguration(systemKey));

		// set environment info prop
		String envKey = getName() + ".env";
		envInfo.setProperty(envKey, getName());

		// make sure the system properties does NOT reflect the new test prop
		assertNull("Wrong system value", System.getProperty(envKey));
		equinox.stop();
	}

	public void testSystemNLFragment() throws BundleException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put("osgi.nl", "zh");
		Equinox equinox = new Equinox(configuration);
		equinox.start();
		equinox.getHeaders();
		BundleContext systemContext = equinox.getBundleContext();
		Bundle systemNLS = systemContext.installBundle(installer.getBundleLocation("test.system.nls"));
		equinox.adapt(FrameworkWiring.class).resolveBundles(Collections.singleton(systemNLS));
		assertEquals("Wrong fragment state", Bundle.RESOLVED, systemNLS.getState());
		assertEquals("Wrong header value.", "TEST NAME", equinox.getHeaders().get(Constants.BUNDLE_NAME));
		equinox.stop();
	}

	public void testNullConfiguration() {
		new Equinox(null);
	}

	public void testNullStorageArea() throws BundleException {
		File install = getContext().getDataFile(getName());
		install.mkdirs();
		Equinox equinox = new Equinox(Collections.singletonMap("osgi.install.area", install.getAbsolutePath()));
		try {
			equinox.init();
			String storageArea = equinox.getBundleContext().getProperty(Constants.FRAMEWORK_STORAGE);
			assertNotNull("No storage area set.", storageArea);
		} finally {
			equinox.stop();
		}
	}

	public void testOSGiDevSetsCheckConfiguration() throws BundleException {
		String originalCheckConfiguration = System.clearProperty(EquinoxConfiguration.PROP_CHECK_CONFIGURATION);
		try {
			File config = OSGiTestsActivator.getContext().getDataFile(getName());
			Map<String, Object> configuration = new HashMap<>();
			configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
			configuration.put("osgi.dev", "true");
			Equinox equinox = new Equinox(configuration);
			equinox.start();
			BundleContext systemContext = equinox.getBundleContext();
			assertEquals("Wrong value for: " + EquinoxConfiguration.PROP_CHECK_CONFIGURATION, "true", systemContext.getProperty(EquinoxConfiguration.PROP_CHECK_CONFIGURATION));
			equinox.stop();
		} finally {
			if (originalCheckConfiguration != null) {
				System.setProperty(EquinoxConfiguration.PROP_CHECK_CONFIGURATION, originalCheckConfiguration);
			}
		}
	}

	public void testProvideOSGiEEandNative() throws BundleException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put("osgi.equinox.allow.restricted.provides", "true");

		Equinox equinox = new Equinox(configuration);
		equinox.start();
		BundleContext systemContext = equinox.getBundleContext();
		Bundle testBundle = systemContext.installBundle(installer.getBundleLocation("test.bug449484"));
		equinox.adapt(FrameworkWiring.class).resolveBundles(Collections.singleton(testBundle));
		assertEquals("Wrong bundle state", Bundle.RESOLVED, testBundle.getState());
		testBundle.uninstall();
		equinox.stop();

		configuration.remove("osgi.equinox.allow.restricted.provides");
		equinox = new Equinox(configuration);
		equinox.start();
		systemContext = equinox.getBundleContext();
		try {
			testBundle = systemContext.installBundle(installer.getBundleLocation("test.bug449484"));
			testBundle.uninstall();
			fail("Expected to fail to install bundle with restricted provide capabilities.");
		} catch (BundleException e) {
			// expected
		}
		equinox.stop();

		configuration.put("osgi.equinox.allow.restricted.provides", "false");
		equinox = new Equinox(configuration);
		equinox.start();
		systemContext = equinox.getBundleContext();
		try {
			testBundle = systemContext.installBundle(installer.getBundleLocation("test.bug449484"));
			testBundle.uninstall();
			fail("Expected to fail to install bundle with restricted provide capabilities.");
		} catch (BundleException e) {
			// expected
		}
		equinox.stop();
	}

	public void testBootDelegationConfigIni() throws BundleException, IOException, InterruptedException {
		String compatBootDelegate = "osgi.compatibility.bootdelegation";
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		config.mkdirs();
		Properties configIni = new Properties();
		// use config.ini to override the default for the embedded case.
		// note that the embedded case the default for this setting is false
		configIni.setProperty(compatBootDelegate, "true");
		configIni.store(new FileWriter(new File(config, "config.ini")), null);
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		equinox.start();
		BundleContext systemContext = equinox.getBundleContext();
		assertEquals("Wrong value for: " + compatBootDelegate, "true", systemContext.getProperty(compatBootDelegate));

		File bundleFile = null;
		try {
			File baseDir = new File(config, "bundles");
			baseDir.mkdirs();
			bundleFile = createBundle(baseDir, getName(), true, true);
		} catch (IOException e) {
			fail("Unexpected error creating bundles.", e);
		}
		Bundle b = null;
		try {
			b = systemContext.installBundle("reference:file:///" + bundleFile.getAbsolutePath()); //$NON-NLS-1$
		} catch (BundleException e) {
			fail("Unexpected install error", e); //$NON-NLS-1$
		}
		try {
			b.loadClass(SocketFactory.class.getName());
		} catch (ClassNotFoundException e) {
			fail("Expected to be able to load the class from boot.", e);
		}
		long bId = b.getBundleId();
		stop(equinox);

		// remove the setting to ensure false is used for the embedded case
		configIni.remove(compatBootDelegate);
		configIni.store(new FileWriter(new File(config, "config.ini")), null);
		equinox = new Equinox(configuration);
		equinox.start();

		systemContext = equinox.getBundleContext();
		b = systemContext.getBundle(bId);
		try {
			b.loadClass(SocketFactory.class.getName());
			fail("Expected to fail to load the class from boot.");
		} catch (ClassNotFoundException e) {
			// expected
		}
		equinox.stop();
	}

	public void testSystemBundleListener() throws BundleException, InterruptedException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		config.mkdirs();
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		equinox.start();
		BundleContext systemContext = equinox.getBundleContext();

		final AtomicInteger stoppingEvent = new AtomicInteger();
		final AtomicInteger stoppedEvent = new AtomicInteger();

		BundleListener systemBundleListener = new SynchronousBundleListener() {

			@Override
			public void bundleChanged(BundleEvent event) {
				if (event.getBundle().getBundleId() == 0) {
					switch (event.getType()) {
						case BundleEvent.STOPPING :
							stoppingEvent.incrementAndGet();
							break;
						case BundleEvent.STOPPED :
							stoppedEvent.incrementAndGet();
						default :
							break;
					}
				}
			}
		};
		systemContext.addBundleListener(systemBundleListener);

		stop(equinox);
		assertEquals("Wrong number of STOPPING events", 1, stoppingEvent.get());
		assertEquals("Wrong number of STOPPED events", 1, stoppedEvent.get());
	}

	public void testContextBootDelegation() throws BundleException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		config.mkdirs();
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);

		equinox.start();
		BundleContext systemContext = equinox.getBundleContext();
		try {
			Bundle b = systemContext.installBundle(installer.getBundleLocation("test.bug471551"));
			b.start();
		} catch (BundleException e) {
			fail("Unexpected error", e); //$NON-NLS-1$
		}

		equinox.stop();
	}

	public void testExtraSystemBundleHeaders() throws BundleException, InterruptedException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		config.mkdirs();
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SYSTEMCAPABILITIES, "osgi.ee; osgi.ee=JavaSE; version:Version=1.8, something.system");
		configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES, "something.system");
		configuration.put(Constants.FRAMEWORK_SYSTEMCAPABILITIES_EXTRA, "something.extra");
		configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "something.extra");

		Equinox equinox = new Equinox(configuration);
		equinox.start();
		Dictionary<String, String> headers = equinox.getHeaders();
		String provideCapability = headers.get(Constants.PROVIDE_CAPABILITY);
		String exportPackage = headers.get(Constants.EXPORT_PACKAGE);
		assertTrue("Unexpected Provide-Capability header: " + provideCapability, provideCapability.contains("something.system"));
		assertTrue("Unexpected Export-Package header: " + exportPackage, exportPackage.contains("something.system"));
		assertTrue("Unexpected Provide-Capability header: " + provideCapability, provideCapability.contains("something.extra"));
		assertTrue("Unexpected Export-Package header: " + exportPackage, exportPackage.contains("something.extra"));
		stop(equinox);

		configuration.put(EquinoxConfiguration.PROP_SYSTEM_PROVIDE_HEADER, EquinoxConfiguration.SYSTEM_PROVIDE_HEADER_ORIGINAL);
		equinox = new Equinox(configuration);
		equinox.start();
		headers = equinox.getHeaders();
		provideCapability = headers.get(Constants.PROVIDE_CAPABILITY);
		exportPackage = headers.get(Constants.EXPORT_PACKAGE);
		assertFalse("Unexpected Provide-Capability header: " + provideCapability, provideCapability.contains("something.system"));
		assertFalse("Unexpected Export-Package header: " + exportPackage, exportPackage.contains("something.system"));
		assertFalse("Unexpected Provide-Capability header: " + provideCapability, provideCapability.contains("something.extra"));
		assertFalse("Unexpected Export-Package header: " + exportPackage, exportPackage.contains("something.extra"));
		stop(equinox);

		configuration.put(EquinoxConfiguration.PROP_SYSTEM_PROVIDE_HEADER, EquinoxConfiguration.SYSTEM_PROVIDE_HEADER_SYSTEM);
		equinox = new Equinox(configuration);
		equinox.start();
		headers = equinox.getHeaders();
		provideCapability = headers.get(Constants.PROVIDE_CAPABILITY);
		exportPackage = headers.get(Constants.EXPORT_PACKAGE);
		assertTrue("Unexpected Provide-Capability header: " + provideCapability, provideCapability.contains("something.system"));
		assertTrue("Unexpected Export-Package header: " + exportPackage, exportPackage.contains("something.system"));
		assertFalse("Unexpected Provide-Capability header: " + provideCapability, provideCapability.contains("something.extra"));
		assertFalse("Unexpected Export-Package header: " + exportPackage, exportPackage.contains("something.extra"));
		stop(equinox);

		configuration.put(EquinoxConfiguration.PROP_SYSTEM_PROVIDE_HEADER, EquinoxConfiguration.SYSTEM_PROVIDE_HEADER_SYSTEM_EXTRA);
		equinox = new Equinox(configuration);
		equinox.start();
		headers = equinox.getHeaders();
		provideCapability = headers.get(Constants.PROVIDE_CAPABILITY);
		exportPackage = headers.get(Constants.EXPORT_PACKAGE);
		assertTrue("Unexpected Provide-Capability header: " + provideCapability, provideCapability.contains("something.system"));
		assertTrue("Unexpected Export-Package header: " + exportPackage, exportPackage.contains("something.system"));
		assertTrue("Unexpected Provide-Capability header: " + provideCapability, provideCapability.contains("something.extra"));
		assertTrue("Unexpected Export-Package header: " + exportPackage, exportPackage.contains("something.extra"));
		stop(equinox);
	}

	public void testSystemBundleLoader() {
		Bundle systemBundle = OSGiTestsActivator.getContext().getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
		BundleWiring wiring = systemBundle.adapt(BundleWiring.class);
		ClassLoader cl = wiring.getClassLoader();
		assertNotNull("No system bundle class loader.", cl);
	}

	public void testJavaProfile() throws IOException {
		boolean isRunningJava9OrGreater = isJavaVersionGreaterThanOrEqualTo9();
		String originalSpecVersion = System.getProperty("java.specification.version");
		String originalJavaHome = System.getProperty("java.home");
		try {
			doTestJavaProfile("9.3.1", isRunningJava9OrGreater ? "JavaSE-9.3" : "JavaSE-9", null);
			doTestJavaProfile("9", "JavaSE-9", null);
			doTestJavaProfile("8.4", "JavaSE-1.8", null);
			doTestJavaProfile("1.10.1", "JavaSE-1.8", null);
			doTestJavaProfile("1.9", "JavaSE-1.8", null);
			doTestJavaProfile("1.8", "JavaSE-1.8", null);
			doTestJavaProfile("1.8", "JavaSE/compact3-1.8", "compact3");
			doTestJavaProfile("1.8", "JavaSE/compact3-1.8", "\"compact3\"");
			doTestJavaProfile("1.8", "JavaSE/compact3-1.8", " \"compact3\" ");
			doTestJavaProfile("1.8", "JavaSE/compact3-1.8", " compact3 ");
			doTestJavaProfile("1.8", "JavaSE-1.8", "\"compact4\"");
			if (!isRunningJava9OrGreater) {
				doTestJavaProfile("9", "JavaSE/compact3-1.8", "compact3");
				doTestJavaProfile("9", "JavaSE/compact3-1.8", "\"compact3\"");
				doTestJavaProfile("9", "JavaSE-9", "\"compact4\"");
			}
		} finally {
			System.setProperty("java.specification.version", originalSpecVersion);
			System.setProperty("java.home", originalJavaHome);
		}
	}

	private Boolean isJavaVersionGreaterThanOrEqualTo9() {
		// default to Java 7 since that is our min
		Version javaVersion = Version.valueOf("1.7"); //$NON-NLS-1$
		// set the profile and EE based off of the java.specification.version
		String javaSpecVersionProp = System.getProperty(EquinoxConfiguration.PROP_JVM_SPEC_VERSION);
		StringTokenizer st = new StringTokenizer(javaSpecVersionProp, " _-"); //$NON-NLS-1$
		javaSpecVersionProp = st.nextToken();
		try {
			String[] vComps = javaSpecVersionProp.split("\\."); //$NON-NLS-1$
			// only pay attention to the first three components of the version
			int major = vComps.length > 0 ? Integer.parseInt(vComps[0]) : 0;
			int minor = vComps.length > 1 ? Integer.parseInt(vComps[1]) : 0;
			int micro = vComps.length > 2 ? Integer.parseInt(vComps[2]) : 0;
			javaVersion = new Version(major, minor, micro);
		} catch (IllegalArgumentException e) {
			// do nothing
		}
		return javaVersion.compareTo(new Version(9, 0, 0)) >= 0;
	}

	private void doTestJavaProfile(String javaSpecVersion, String expectedEEName, String releaseName) throws FileNotFoundException, IOException {
		if (releaseName != null) {
			File release = getContext().getDataFile("jre/release");
			release.getParentFile().mkdirs();
			Properties props = new Properties();
			props.put("JAVA_PROFILE", releaseName);
			FileOutputStream propStream = new FileOutputStream(release);
			try {
				props.store(propStream, null);
			} finally {
				propStream.close();
			}
			System.setProperty("java.home", release.getParentFile().getAbsolutePath());
		}
		System.setProperty("java.specification.version", javaSpecVersion);
		// create/stop/ test
		File config = OSGiTestsActivator.getContext().getDataFile(getName() + javaSpecVersion);
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		@SuppressWarnings("deprecation")
		String osgiEE = equinox.getBundleContext().getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
		// don't do anything; just put the framework back to the RESOLVED state
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$

		assertTrue("Wrong osgi EE: expected: " + expectedEEName + " but was: " + osgiEE, osgiEE.endsWith(expectedEEName));
	}

	private static File[] createBundles(File outputDir, int bundleCount) throws IOException {
		outputDir.mkdirs();

		File[] bundles = new File[bundleCount];

		for (int i = 0; i < bundleCount; i++)
			bundles[i] = createBundle(outputDir, "-b" + i, false, false); //$NON-NLS-1$

		return bundles;
	}

	private static File[] createBundles(File outputDir, int bundleCount, Map<String, String> extraHeaders) throws IOException {
		outputDir.mkdirs();

		File[] bundles = new File[bundleCount];

		for (int i = 0; i < bundleCount; i++) {
			Map<String, String> headers = new HashMap<>(extraHeaders);
			headers.put(Constants.BUNDLE_SYMBOLICNAME, "-b" + i);
			bundles[i] = createBundle(outputDir, "-b" + i, headers);
		}

		return bundles;
	}

	public static File createBundle(File outputDir, String id, boolean emptyManifest, boolean dirBundle) throws IOException {
		File file = new File(outputDir, "bundle" + id + (dirBundle ? "" : ".jar")); //$NON-NLS-1$ //$NON-NLS-2$
		if (!dirBundle) {
			JarOutputStream jos = new JarOutputStream(new FileOutputStream(file), createManifest(id, emptyManifest));
			jos.flush();
			jos.close();
		} else {
			File manifest = new File(file, "META-INF/MANIFEST.MF");
			manifest.getParentFile().mkdirs();
			FileOutputStream out = new FileOutputStream(manifest);
			createManifest(id, emptyManifest).write(out);
			out.close();
		}
		return file;
	}

	private static Manifest createManifest(String id, boolean emptyManifest) {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0"); //$NON-NLS-1$ //$NON-NLS-2$
		if (!emptyManifest) {
			attributes.putValue("Bundle-ManifestVersion", "2"); //$NON-NLS-1$ //$NON-NLS-2$
			attributes.putValue("Bundle-SymbolicName", "bundle" + id); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return manifest;
	}

	static File createBundle(File outputDir, String bundleName, Map<String, String> headers, Map<String, String>... entries) throws IOException {
		Manifest m = new Manifest();
		Attributes attributes = m.getMainAttributes();
		attributes.putValue("Manifest-Version", "1.0");
		for (Map.Entry<String, String> entry : headers.entrySet()) {
			attributes.putValue(entry.getKey(), entry.getValue());
		}
		File file = new File(outputDir, "bundle" + bundleName + ".jar"); //$NON-NLS-1$ //$NON-NLS-2$
		JarOutputStream jos = new JarOutputStream(new FileOutputStream(file), m);
		if (entries != null) {
			for (Map<String, String> entryMap : entries) {
				for (Map.Entry<String, String> entry : entryMap.entrySet()) {
					jos.putNextEntry(new JarEntry(entry.getKey()));
					if (entry.getValue() != null) {
						jos.write(entry.getValue().getBytes());
					}
					jos.closeEntry();
				}
			}
		}
		jos.flush();
		jos.close();
		return file;
	}

	public void testBug405919() throws Exception {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		config.mkdirs();
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put("osgi.framework", "boo");
		// Initialize and start a framework specifying an invalid osgi.framework configuration value.
		Equinox equinox = null;
		try {
			equinox = new Equinox(configuration);
			equinox.start();
		} catch (NullPointerException e) {
			fail("failed to accept an invalid value for osgi.framework", e);
		}
		try {
			// Make sure the framework can install and start a bundle.
			BundleContext systemContext = equinox.getBundleContext();
			try {
				Bundle tb1 = systemContext.installBundle(installer.getBundleLocation("test.bug375784"));
				tb1.start();
			} catch (BundleException e) {
				fail("failed to install and start test bundle", e);
			}
			// Check the capabilities and requirements of the system bundle.
			BundleRevision inner = systemContext.getBundle().adapt(BundleRevision.class);
			BundleRevision outer = getContext().getBundle(0).adapt(BundleRevision.class);
			// Capabilities.
			List<Capability> innerCaps = inner.getCapabilities(null);
			List<Capability> outerCaps = outer.getCapabilities(null);
			assertEquals("Number of capabilities differ", outerCaps.size(), innerCaps.size());
			for (int i = 0; i < innerCaps.size(); i++) {
				Capability innerCap = innerCaps.get(i);
				Capability outerCap = innerCaps.get(i);
				assertEquals("Capability namespaces differ: " + outerCap.getNamespace(), outerCap.getNamespace(), innerCap.getNamespace());
				String namespace = outerCap.getNamespace();
				if (NativeNamespace.NATIVE_NAMESPACE.equals(namespace) || "eclipse.platform".equals(namespace)) {
					// Ignore these because they are known to differ.
					continue;
				}
				assertEquals("Capability attributes differ: " + outerCap.getNamespace(), outerCap.getAttributes(), innerCap.getAttributes());
				assertEquals("Capability directives differ: " + outerCap.getNamespace(), outerCap.getDirectives(), innerCap.getDirectives());
			}
			// Requirements.
			List<Requirement> innerReqs = inner.getRequirements(null);
			List<Requirement> outerReqs = outer.getRequirements(null);
			assertEquals("Number of requirements differ", outerReqs.size(), innerReqs.size());
			for (int i = 0; i < innerReqs.size(); i++) {
				Requirement innerReq = innerReqs.get(i);
				Requirement outerReq = innerReqs.get(i);
				assertEquals("Requirement namespaces differ: " + outerReq.getNamespace(), outerReq.getNamespace(), innerReq.getNamespace());
				assertEquals("Requirement attributes differ: " + outerReq.getNamespace(), outerReq.getAttributes(), innerReq.getAttributes());
				assertEquals("Requirement directives differ: " + outerReq.getNamespace(), outerReq.getDirectives(), innerReq.getDirectives());
			}
		} finally {
			stop(equinox);
		}
	}

	public void testInitialBundleUpdate() throws IOException {
		// test installing bundle with empty manifest
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());

		Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		File bundleFile = null;
		try {
			File baseDir = new File(config, "bundles");
			baseDir.mkdirs();
			bundleFile = createBundle(baseDir, getName(), true, true);
		} catch (IOException e) {
			fail("Unexpected error creating bundles.", e);
		}
		Bundle testBundle = null;
		try {
			String location = "reference:file:///" + bundleFile.getAbsolutePath();
			URL url = new URL(null, location, new Handler(systemContext.getProperty(EquinoxLocations.PROP_INSTALL_AREA)));
			testBundle = systemContext.installBundle("initial@" + location, url.openStream()); //$NON-NLS-1$
		} catch (BundleException e) {
			fail("Unexpected install error", e); //$NON-NLS-1$
		}

		try {
			testBundle.update();
		} catch (BundleException e) {
			fail("Unexpected update error", e); //$NON-NLS-1$
		}

		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testDaemonActiveThread() throws BundleException, InterruptedException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName());
		config.mkdirs();
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());

		// test setting to anything other than 'normal'
		// should result in a daemon thread
		configuration.put(EquinoxConfiguration.PROP_ACTIVE_THREAD_TYPE, "daemon");
		Equinox equinox = new Equinox(configuration);
		equinox.start();
		checkActiveThreadType(equinox, true);
		stop(equinox);

		// test setting to 'normal'
		// should result in a non-daemon thread
		configuration.put(EquinoxConfiguration.PROP_ACTIVE_THREAD_TYPE, EquinoxConfiguration.ACTIVE_THREAD_TYPE_NORMAL);
		equinox = new Equinox(configuration);
		equinox.start();
		checkActiveThreadType(equinox, false);
		stop(equinox);

		// test setting to null (default)
		// should result in a non-daemon thread
		configuration.remove(EquinoxConfiguration.PROP_ACTIVE_THREAD_TYPE);
		equinox = new Equinox(configuration);
		equinox.start();
		checkActiveThreadType(equinox, false);
		equinox.stop();
	}

	public void testLazyTriggerOnLoadError() throws InterruptedException, BundleException {
		// create/start/stop/start/stop test
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(EquinoxConfiguration.PROP_COMPATIBILITY_START_LAZY_ON_FAIL_CLASSLOAD, "true");
		Equinox equinox = new Equinox(configuration);

		equinox.start();

		BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		// install a lazy activated bundle
		Bundle b = systemContext.installBundle(installer.getBundleLocation("chain.test.d")); //$NON-NLS-1$
		b.start(Bundle.START_ACTIVATION_POLICY);
		assertEquals("Wrong state of bundle.", Bundle.STARTING, b.getState());
		try {
			// trigger the start by loading non existing class
			b.loadClass("does.not.exist.Clazz");
			fail("Expected class load error");
		} catch (ClassNotFoundException e) {
			// expected
		}
		// should be active now
		assertEquals("Wrong state of bundle.", Bundle.ACTIVE, b.getState());
		// put the framework back to the RESOLVED state
		stop(equinox);

		// revert back to default behavior
		configuration.remove(EquinoxConfiguration.PROP_COMPATIBILITY_START_LAZY_ON_FAIL_CLASSLOAD);
		equinox = new Equinox(configuration);
		equinox.start();
		b = equinox.getBundleContext().getBundle(b.getBundleId());
		assertEquals("Wrong state of bundle.", Bundle.STARTING, b.getState());

		try {
			// loading non-existing class should NOT trigger lazy activation now
			b.loadClass("does.not.exist.Clazz");
			fail("Expected class load error");
		} catch (ClassNotFoundException e) {
			// expected
		}
		// should still be in STARTING state.
		assertEquals("Wrong state of bundle.", Bundle.STARTING, b.getState());

		equinox.stop();
	}

	public void testConfigPercentChar() throws BundleException, IOException {
		doTestConfigSpecialChar('%');
	}

	public void testConfigSpaceChar() throws BundleException, IOException {
		doTestConfigSpecialChar(' ');
	}

	public void testConfigPlusChar() throws BundleException, IOException {
		doTestConfigSpecialChar('+');
	}

	private void doTestConfigSpecialChar(char c) throws BundleException, IOException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName() + c + "config");
		config.mkdirs();
		// create a config.ini with some system property substitutes
		Properties configIni = new Properties();
		configIni.setProperty("test.config", getName());
		configIni.store(new FileOutputStream(new File(config, "config.ini")), "Test config.ini");

		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = new Equinox(configuration);
		equinox.init();

		BundleContext systemContext = equinox.getBundleContext();
		// check for substitution
		assertEquals("Wrong value for test.config", getName(), systemContext.getProperty("test.config"));

		stop(equinox);
	}

	void checkActiveThreadType(Equinox equinox, boolean expectIsDeamon) {
		String uuid = equinox.getBundleContext().getProperty(Constants.FRAMEWORK_UUID);
		ThreadGroup topGroup = Thread.currentThread().getThreadGroup();
		if (topGroup != null) {
			while (topGroup.getParent() != null) {
				topGroup = topGroup.getParent();
			}
		}
		Thread[] threads = new Thread[topGroup.activeCount()];
		topGroup.enumerate(threads);
		Thread found = null;
		for (Thread t : threads) {
			String name = t.getName();
			if (("Active Thread: Equinox Container: " + uuid).equals(name)) {
				found = t;
				break;
			}
		}
		assertNotNull("No framework active thread for \"" + uuid + "\" found in : " + Arrays.toString(threads), found);
		assertEquals("Wrong daemon type.", expectIsDeamon, found.isDaemon());
	}

	public void testWindowsAlias() {
		String origOS = System.getProperty("os.name");
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map configuration = new HashMap();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		System.setProperty("os.name", "Windows 5000");
		Equinox equinox = null;
		try {
			equinox = new Equinox(configuration);
			equinox.init();
			Assert.assertEquals("Wrong framework os name value", "win32", equinox.getBundleContext().getProperty(Constants.FRAMEWORK_OS_NAME));
		} catch (BundleException e) {
			fail("Failed init", e);
		} finally {
			System.setProperty("os.name", origOS);
			stop(equinox);
		}
	}

	public void testOverrideEquinoxConfigAreaProp() {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map configuration = new HashMap();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(EquinoxLocations.PROP_CONFIG_AREA, config.getAbsolutePath() + "-override");
		configuration.put(EquinoxConfiguration.PROP_LOG_HISTORY_MAX, "10");
		Equinox equinox = null;
		try {
			equinox = new Equinox(configuration);
			equinox.init();
			BundleContext bc = equinox.getBundleContext();
			LogReaderService logReader = bc.getService(bc.getServiceReference(LogReaderService.class));
			Enumeration<LogEntry> logs = logReader.getLog();
			assertTrue("No logs found.", logs.hasMoreElements());
			LogEntry entry = logs.nextElement();
			assertEquals("Wrong log level.", LogLevel.WARN, entry.getLogLevel());
			assertTrue("Wrong message found: " + entry.getMessage(), entry.getMessage().contains(EquinoxLocations.PROP_CONFIG_AREA));
		} catch (BundleException e) {
			fail("Failed init", e);
		} finally {
			stop(equinox);
		}
	}

	public void testLogOrderMultipleListeners() throws InterruptedException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map configuration = new HashMap();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = null;

		try {
			equinox = new Equinox(configuration);
			equinox.start();
			doLoggingOnMultipleListeners(equinox);
			stop(equinox);
			equinox.start();
			doLoggingOnMultipleListeners(equinox);

		} catch (BundleException e) {
			fail("Failed init", e);
		} finally {
			stop(equinox);
		}
	}

	static void doLoggingOnMultipleListeners(Equinox equinox) throws InterruptedException {
		int listenersSize = 100;
		int logSize = 10000;
		BundleContext bc = equinox.getBundleContext();
		ExtendedLogReaderService logReader = bc.getService(bc.getServiceReference(ExtendedLogReaderService.class));
		ExtendedLogService log = bc.getService(bc.getServiceReference(ExtendedLogService.class));
		ArrayList<TestListener2> listeners = new ArrayList<>();
		CountDownLatch latch = new CountDownLatch(logSize * listenersSize);

		for (int i = 0; i < listenersSize; i++) {
			TestListener2 listener = new TestListener2(latch);
			listeners.add(listener);
			logReader.addLogListener(listener);
		}

		for (int i = 0; i < logSize; i++) {
			log.warn(String.valueOf(i));
		}

		latch.await(10, TimeUnit.SECONDS);
		assertEquals("Failed to log all entries", 0, latch.getCount());

		int expected = 0;
		for (String msg : listeners.get(0).getLogs()) {
			assertEquals("Unexpected log found.", expected, Integer.parseInt(msg));
			expected++;
		}

		for (int i = 1; i < listenersSize; i++) {
			assertTrue(listeners.get(i).getLogs().equals(listeners.get(0).getLogs()));
		}
	}

	public void testCaptureLogEntryLocation() throws BundleException, InterruptedException {
		doTestCaptureLogEntryLocation(true);
		doTestCaptureLogEntryLocation(false);
	}

	private void doTestCaptureLogEntryLocation(boolean captureLocation) throws BundleException, InterruptedException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map configuration = new HashMap();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		if (!captureLocation) {
			// the default is true; only set for false
			configuration.put(EquinoxConfiguration.PROP_LOG_CAPTURE_ENTRY_LOCATION, Boolean.toString(captureLocation));
		}
		Equinox equinox = new Equinox(configuration);
		try {
			equinox.start();

			BundleContext bc = equinox.getBundleContext();
			LogReaderService logReader = bc.getService(bc.getServiceReference(LogReaderService.class));
			LogService logService = bc.getService(bc.getServiceReference(LogService.class));

			TestListener listener = new TestListener(Constants.SYSTEM_BUNDLE_LOCATION);
			logReader.addLogListener(listener);

			final String testMsg = "TEST MESSAGE";
			logService.getLogger(this.getClass()).error(testMsg);

			LogEntry logEntry = listener.getEntryX();
			assertEquals("Wrong message.", testMsg, logEntry.getMessage());
			if (captureLocation) {
				assertNotNull("No location found.", logEntry.getLocation());
			} else {
				assertNull("Found location.", logEntry.getLocation());
			}
		} finally {
			stop(equinox);
		}
	}

	public void testSystemCapabilitiesBug522125() throws URISyntaxException, FileNotFoundException, IOException, BundleException, InterruptedException {
		String frameworkLocation = OSGiTestsActivator.getContext().getProperty(EquinoxConfiguration.PROP_FRAMEWORK);
		URI uri = new URI(frameworkLocation);
		File f = new File(uri);
		if (!f.isFile()) {
			System.out.println("Cannot test when framework location is a directory: " + f.getAbsolutePath());
			return;
		}
		File testDestination = OSGiTestsActivator.getContext().getDataFile(getName() + ".framework.jar");
		BaseSecurityTest.copy(new FileInputStream(f), testDestination);
		FilePath userDir = new FilePath(System.getProperty("user.dir"));
		FilePath testPath = new FilePath(testDestination);
		String relative = userDir.makeRelative(testPath);
		System.out.println(relative);
		URL relativeURL = new URL("file:" + relative);
		relativeURL.openStream().close();
		final ClassLoader osgiClassLoader = getClass().getClassLoader();
		URLClassLoader cl = new URLClassLoader(new URL[] {relativeURL}) {

			@Override
			protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
				if (name.startsWith("org.osgi.")) {
					return osgiClassLoader.loadClass(name);
				}
				return super.loadClass(name, resolve);
			}

		};

		ServiceLoader<FrameworkFactory> sLoader = ServiceLoader.load(FrameworkFactory.class, cl);
		FrameworkFactory factory = sLoader.iterator().next();

		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, String> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(EquinoxConfiguration.PROP_FRAMEWORK, relativeURL.toExternalForm());

		Framework framework = factory.newFramework(configuration);
		framework.init();
		stop(framework);

		BundleRevision systemRevision1 = framework.adapt(BundleRevision.class);
		int capCount1 = systemRevision1.getCapabilities(null).size();

		framework = factory.newFramework(configuration);
		framework.init();
		stop(framework);

		BundleRevision systemRevision2 = framework.adapt(BundleRevision.class);
		int capCount2 = systemRevision2.getCapabilities(null).size();

		Assert.assertEquals("Wrong number of capabilities", capCount1, capCount2);
	}

	public void testStartLevelSorting() throws IOException, InterruptedException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map configuration = new HashMap();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = null;
		final int numBundles = 200;
		final File[] testBundleFiles = createBundles(new File(config, "testBundles"), numBundles);
		try {
			equinox = new Equinox(configuration);
			equinox.start();
			final List<Bundle> testBundles = Collections.synchronizedList(new ArrayList<Bundle>());
			final List<Bundle> startedBundles = Collections.synchronizedList(new ArrayList<Bundle>());
			for (int i = 0; i < numBundles / 2; i++) {
				Bundle b = equinox.getBundleContext().installBundle("reference:file:///" + testBundleFiles[i].getAbsolutePath());
				b.adapt(BundleStartLevel.class).setStartLevel(i + 2);
				testBundles.add(b);
				b.start();
			}
			final Equinox finalEquinox = equinox;

			BundleListener initialListener = new SynchronousBundleListener() {
				AtomicBoolean reverseStartLevel = new AtomicBoolean();
				AtomicBoolean installBundles = new AtomicBoolean();

				@Override
				public void bundleChanged(BundleEvent event) {
					Bundle eBundle = event.getBundle();
					String bsn = eBundle.getSymbolicName();
					if (!bsn.startsWith("bundle-b") || event.getType() != BundleEvent.STARTED) {
						return;
					}
					startedBundles.add(eBundle);
					if (reverseStartLevel.compareAndSet(false, true)) {
						for (int i = numBundles / 4, j = numBundles; i < numBundles / 2; i++, j--) {
							BundleStartLevel tbsl = testBundles.get(i).adapt(BundleStartLevel.class);
							tbsl.setStartLevel(j + 2 + numBundles);
						}
					} else if (installBundles.compareAndSet(false, true)) {
						for (int i = numBundles / 2; i < numBundles; i++) {
							try {
								Bundle b = finalEquinox.getBundleContext().installBundle("reference:file:///" + testBundleFiles[i].getAbsolutePath());
								b.adapt(BundleStartLevel.class).setStartLevel(i + 2);
								testBundles.add(b);
								b.start();
							} catch (BundleException e) {
								// do nothing
							}
						}
					}
				}
			};

			equinox.getBundleContext().addBundleListener(initialListener);
			long startTime = System.currentTimeMillis();
			final CountDownLatch waitForStartLevel = new CountDownLatch(1);
			equinox.adapt(FrameworkStartLevel.class).setStartLevel(numBundles * 3, new FrameworkListener() {

				@Override
				public void frameworkEvent(FrameworkEvent event) {
					waitForStartLevel.countDown();
				}
			});
			waitForStartLevel.await(20, TimeUnit.SECONDS);
			System.out.println("Start time: " + (System.currentTimeMillis() - startTime));

			assertEquals("Did not finish start level setting.", 0, waitForStartLevel.getCount());
			assertEquals("Did not install all the expected bundles.", numBundles, testBundles.size());

			List<Bundle> expectedStartOrder = new ArrayList<>();
			for (int i = 0; i < numBundles / 4; i++) {
				expectedStartOrder.add(testBundles.get(i));
			}
			for (int i = numBundles / 2; i < numBundles; i++) {
				expectedStartOrder.add(testBundles.get(i));
			}
			for (int i = (numBundles / 2) - 1; i >= numBundles / 4; i--) {
				expectedStartOrder.add(testBundles.get(i));
			}

			assertEquals("Size on expected order is wrong.", numBundles, expectedStartOrder.size());
			for (int i = 0; i < numBundles; i++) {
				assertEquals("Wrong bundle at: " + i, expectedStartOrder.get(i), startedBundles.get(i));
			}

			equinox.getBundleContext().removeBundleListener(initialListener);

			final List<Bundle> stoppedBundles = Collections.synchronizedList(new ArrayList<Bundle>());
			BundleListener shutdownListener = new SynchronousBundleListener() {
				AtomicBoolean reverseStartLevel = new AtomicBoolean();
				AtomicBoolean uninstallBundles = new AtomicBoolean();
				AtomicBoolean inUninstall = new AtomicBoolean();

				@Override
				public void bundleChanged(BundleEvent event) {
					if (inUninstall.get()) {
						return;
					}
					Bundle eBundle = event.getBundle();
					String bsn = eBundle.getSymbolicName();
					if (!bsn.startsWith("bundle-b") || event.getType() != BundleEvent.STOPPED) {
						return;
					}
					stoppedBundles.add(eBundle);
					if (reverseStartLevel.compareAndSet(false, true)) {
						for (int i = numBundles / 2, j = numBundles - 1; i < numBundles; i++, j--) {
							BundleStartLevel tbsl = testBundles.get(i).adapt(BundleStartLevel.class);
							tbsl.setStartLevel(j + 2);
						}
					} else if (uninstallBundles.compareAndSet(false, true)) {
						for (int i = 0; i < numBundles / 4; i++) {
							try {
								inUninstall.set(true);
								testBundles.get(i).uninstall();
							} catch (BundleException e) {
								// do nothing
							} finally {
								inUninstall.set(false);
							}
						}
					}
				}
			};
			equinox.getBundleContext().addBundleListener(shutdownListener);

			List<Bundle> expectedStopOrder = new ArrayList<>(expectedStartOrder);
			Collections.reverse(expectedStopOrder);
			Collections.reverse(expectedStopOrder.subList(numBundles / 4, 3 * (numBundles / 4)));
			expectedStopOrder = new ArrayList(expectedStopOrder.subList(0, 3 * (numBundles / 4)));

			long stopTime = System.currentTimeMillis();
			stop(equinox, false, 20000);
			System.out.println("Stop time: " + (System.currentTimeMillis() - stopTime));

			assertEquals("Size on expected order is wrong.", expectedStopOrder.size(), stoppedBundles.size());
			for (int i = 0; i < expectedStopOrder.size(); i++) {
				assertEquals("Wrong bundle at: " + i, expectedStopOrder.get(i), stoppedBundles.get(i));
			}
		} catch (BundleException e) {
			fail("Failed init", e);
		} finally {
			stop(equinox);
		}
	}

	public void testStartLevelSingleThread() throws IOException, InterruptedException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map configuration = new HashMap();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = null;
		final int numBundles = 20;
		final File[] testBundleFiles = createBundles(new File(config, "testBundles"), numBundles);
		try {
			equinox = new Equinox(configuration);
			equinox.start();
			for (int i = 0; i < numBundles; i++) {
				Bundle b = equinox.getBundleContext().installBundle("reference:file:///" + testBundleFiles[i].getAbsolutePath());
				b.adapt(BundleStartLevel.class).setStartLevel(5);
				b.start();
			}

			final Set<Thread> startingThreads = Collections.synchronizedSet(new HashSet<Thread>());
			equinox.getBundleContext().addBundleListener(new SynchronousBundleListener() {
				@Override
				public void bundleChanged(BundleEvent event) {
					if (event.getType() == BundleEvent.STARTING) {
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							// nothing
						}
						startingThreads.add(Thread.currentThread());
					}
				}
			});

			final CountDownLatch waitForStartLevel = new CountDownLatch(1);
			equinox.adapt(FrameworkStartLevel.class).setStartLevel(5, new FrameworkListener() {
				@Override
				public void frameworkEvent(FrameworkEvent event) {
					waitForStartLevel.countDown();
				}
			});
			waitForStartLevel.await(10, TimeUnit.SECONDS);

			assertEquals("Did not finish start level setting.", 0, waitForStartLevel.getCount());
			assertEquals("Wrong number of start threads.", 1, startingThreads.size());

		} catch (BundleException e) {
			fail("Failed init", e);
		} finally {
			stop(equinox);
		}
	}

	public void testStartLevelMultiThreadExplicit4() throws IOException, InterruptedException {
		doTestStartLevelMultiThread(4, false);
	}

	public void testStartLevelMultiThreadExplicit1() throws IOException, InterruptedException {
		doTestStartLevelMultiThread(1, false);
	}

	public void testStartLevelMultiThreadAvailableProcessors() throws IOException, InterruptedException {
		doTestStartLevelMultiThread(0, false);
	}

	public void testStartLevelRestrictMultiThreadExplicit4() throws IOException, InterruptedException {
		doTestStartLevelMultiThread(4, true);
	}

	private void doTestStartLevelMultiThread(int expectedCount, final boolean restrictParallel) throws IOException, InterruptedException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, String> configuration = new HashMap();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(EquinoxConfiguration.PROP_EQUINOX_START_LEVEL_THREAD_COUNT, String.valueOf(expectedCount));
		if (restrictParallel) {
			configuration.put(EquinoxConfiguration.PROP_EQUINOX_START_LEVEL_RESTRICT_PARALLEL, "true");
		}
		if (expectedCount <= 0) {
			expectedCount = Runtime.getRuntime().availableProcessors();
		}
		Equinox equinox = null;
		final int numBundles = 60;
		final File[] testBundleFiles = createBundles(new File(config, "testBundles"), numBundles);
		try {
			equinox = new Equinox(configuration);
			equinox.start();
			FrameworkWiring fwkWiring = equinox.adapt(FrameworkWiring.class);
			for (int i = 0; i < numBundles; i++) {
				Bundle b = equinox.getBundleContext().installBundle("reference:file:///" + testBundleFiles[i].getAbsolutePath());
				if (i < 30) {
					b.adapt(BundleStartLevel.class).setStartLevel(5);
				} else {
					b.adapt(BundleStartLevel.class).setStartLevel(10);
				}
				Module m = b.adapt(Module.class);
				assertFalse("Wrong initial value for parallelActivation", m.isParallelActivated());
				if (b.getBundleId() % 2 == 0) {
					b.adapt(Module.class).setParallelActivation(true);
					assertTrue("Wrong value for parallelActivation", m.isParallelActivated());
				}
				fwkWiring.resolveBundles(Collections.singleton(b));
				b.start();
			}

			final Set<Thread> startingThreads = Collections.synchronizedSet(new HashSet<Thread>());
			final List<Bundle> startingBundles = Collections.synchronizedList(new ArrayList<Bundle>());
			equinox.getBundleContext().addBundleListener(new SynchronousBundleListener() {
				@Override
				public void bundleChanged(BundleEvent event) {
					if (event.getType() == BundleEvent.STARTING) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// nothing
						}
						startingBundles.add(event.getBundle());
						startingThreads.add(Thread.currentThread());
					}
				}
			});

			final CountDownLatch waitForStartLevel = new CountDownLatch(1);
			equinox.adapt(FrameworkStartLevel.class).setStartLevel(10, new FrameworkListener() {
				@Override
				public void frameworkEvent(FrameworkEvent event) {
					waitForStartLevel.countDown();
				}
			});
			waitForStartLevel.await(20, TimeUnit.SECONDS);

			assertEquals("Did not finish start level setting.", 0, waitForStartLevel.getCount());
			if (restrictParallel && expectedCount > 1) {
				// when restricting parallel start the restricted bundles will start on
				// the dispatching thread adding one more thread
				assertEquals("Wrong number of start threads.", expectedCount + 1, startingThreads.size());
			} else {
				assertEquals("Wrong number of start threads.", expectedCount, startingThreads.size());
			}
			assertEquals("Wrong number of started bundles.", numBundles, startingBundles.size());

			ListIterator<Bundle> itr = startingBundles.listIterator();
			while (itr.hasNext()) {
				if (itr.hasPrevious()) {
					Bundle b1 = itr.previous();
					itr.next();
					Bundle b2 = itr.next();

					int b1sl = b1.adapt(BundleStartLevel.class).getStartLevel();
					int b2sl = b2.adapt(BundleStartLevel.class).getStartLevel();
					assertTrue("Wrong order to start bundles: " + b1 + " - " + b2, b1sl <= b2sl);
					if (restrictParallel && b1sl == b2sl) {
						boolean b1pa = b1.adapt(Module.class).isParallelActivated();
						boolean b2pa = b2.adapt(Module.class).isParallelActivated();
						assertTrue("Wrong order to start bundles: " + b1 + " - " + b2, b1pa || (!b1pa && !b2pa));
						if (!b1pa) {
							assertTrue("Wrong order to start bundles: " + b1 + " - " + b2, b1.getBundleId() < b2.getBundleId());
						}
					}
				} else {
					itr.next();
				}
			}
		} catch (BundleException e) {
			fail("Failed init", e);
		} finally {
			stop(equinox);
		}
	}

	public void testParallelActivationPersistence() throws IOException, BundleException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, String> configuration = new HashMap();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		final int numBundles = 4;
		final File[] testBundleFiles = createBundles(new File(config, "testParallelPersistence"), numBundles);
		Equinox equinox = null;
		try {
			equinox = new Equinox(configuration);
			equinox.start();
			FrameworkWiring fwkWiring = equinox.adapt(FrameworkWiring.class);
			for (int i = 0; i < numBundles; i++) {
				Bundle b = equinox.getBundleContext().installBundle("reference:file:///" + testBundleFiles[i].getAbsolutePath());
				Module m = b.adapt(Module.class);
				assertFalse("Wrong initial value for parallelActivation", m.isParallelActivated());
				if (b.getBundleId() % 2 == 0) {
					b.adapt(Module.class).setParallelActivation(true);
					assertTrue("Wrong value for parallelActivation", m.isParallelActivated());
				}
				fwkWiring.resolveBundles(Collections.singleton(b));
				b.start();
			}
			stop(equinox);
			equinox = null;
			equinox = new Equinox(configuration);
			equinox.start();
			Bundle[] bundles = equinox.getBundleContext().getBundles();
			assertEquals("Wrong number of bundles on restart.", numBundles + 1, bundles.length);
			for (Bundle b : bundles) {
				if (b.getBundleId() == 0) {
					continue;
				}
				if (b.getBundleId() % 2 == 0) {
					assertTrue("Wrong value for parallelActivation", b.adapt(Module.class).isParallelActivated());
				} else {
					assertFalse("Wrong value for parallelActivation", b.adapt(Module.class).isParallelActivated());
				}
			}
		} finally {
			stop(equinox);
		}
	}

	public void testBundleIDLock() {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(EquinoxConfiguration.PROP_FILE_LIMIT, "10");
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());

		final Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		final BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		final int numBundles = 5000;
		final File[] testBundles;
		try {
			testBundles = createBundles(new File(config, "bundles"), numBundles); //$NON-NLS-1$
		} catch (IOException e) {
			fail("Unexpected error creating budnles", e); //$NON-NLS-1$
			throw new RuntimeException();
		}

		ExecutorService executor = Executors.newFixedThreadPool(50);
		final List<Throwable> errors = new CopyOnWriteArrayList<>();
		try {
			for (final File testBundleFile : testBundles) {
				executor.execute(new Runnable() {

					@Override
					public void run() {
						try {
							systemContext.installBundle("file:///" + testBundleFile.getAbsolutePath());
						} catch (BundleException e) {
							e.printStackTrace();
							errors.add(e);
						}
					}
				});
			}
		} finally {
			executor.shutdown();
			try {
				executor.awaitTermination(600, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				fail("Interrupted.", e);
			}
		}

		Assert.assertEquals("Errors found.", Collections.emptyList(), errors);
		Assert.assertEquals("Wrong number of bundles.", numBundles + 1, systemContext.getBundles().length);
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$
	}

	public void testMRUBundleFileListOverflow() throws BundleException, FileNotFoundException, IOException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		final int numBundles = 5000;
		final File[] testBundles;
		try {
			testBundles = createBundles(new File(config, "bundles"), numBundles); //$NON-NLS-1$
		} catch (IOException e) {
			fail("Unexpected error creating budnles", e); //$NON-NLS-1$
			throw new RuntimeException();
		}

		File debugOptions = new File(config, "debugOptions");
		Properties debugProps = new Properties();
		debugProps.setProperty(Debug.OPTION_DEBUG_BUNDLE_FILE, "true");
		FileOutputStream debugOut = new FileOutputStream(debugOptions);
		debugProps.store(debugOut, "Equinox Debug Options");
		debugOut.close();

		Map<String, Object> configuration = new HashMap<>();
		configuration.put(EquinoxConfiguration.PROP_FILE_LIMIT, "10");
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		//configuration.put(EquinoxConfiguration.PROP_DEBUG, debugOptions.getAbsolutePath());

		final Equinox equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		final BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$

		final List<Bundle> bundles = new ArrayList<>();
		for (File testBundleFile : testBundles) {
			bundles.add(systemContext.installBundle("file:///" + testBundleFile.getAbsolutePath()));
		}

		// Note that this test uses wall clock timing which is not always consistent
		// across testing environments, but we use a limit that should be well within
		// reason for the test data size
		long startTime = System.nanoTime();
		ExecutorService executor = Executors.newFixedThreadPool(10);
		try {
			for (int i = 0; i < 10; i++) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						List<Bundle> shuffled = new ArrayList<>(bundles);
						Collections.shuffle(shuffled);
						for (Bundle bundle : shuffled) {
							bundle.getEntry("does/not/exist");
						}
					}
				});
			}
		} finally {
			executor.shutdown();
			try {
				executor.awaitTermination(600, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				fail("Interrupted.", e);
			}
		}

		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$

		long timeTaken = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime);
		System.out.println(getName() + " time taken: " + timeTaken);
		assertTrue("Test took too long: " + timeTaken, timeTaken < 30);
	}

	public void testZipBundleFileOpenLock() throws IOException, BundleException, InvalidSyntaxException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		config.mkdirs();

		Map<String, String> bundleHeaders = new HashMap<>();
		bundleHeaders.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		bundleHeaders.put(Constants.BUNDLE_SYMBOLICNAME, getName());
		Map<String, String> bundleEntries = new LinkedHashMap<>();
		bundleEntries.put("dirA/", null);
		bundleEntries.put("dirA/fileA", "fileA");
		bundleEntries.put("dirA/dirB/", null);
		bundleEntries.put("dirA/dirB/fileB", "fileB");
		// file in a directory with no directory entry
		bundleEntries.put("dirA/dirC/fileC", "fileC");
		File testBundleFile = SystemBundleTests.createBundle(config, getName(), bundleHeaders, bundleEntries);

		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());

		final Equinox equinox = new Equinox(configuration);
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$
		final BundleContext systemContext = equinox.getBundleContext();

		String converterFilter = "(&(objectClass=" + URLConverter.class.getName() + ")(protocol=bundleentry))";
		final URLConverter converter = systemContext.getService(systemContext.getServiceReferences(URLConverter.class, converterFilter).iterator().next());

		final Bundle testBundle = systemContext.installBundle("file:///" + testBundleFile.getAbsolutePath());
		testBundle.start();

		final List<FrameworkEvent> errorsAndWarnings = new CopyOnWriteArrayList<>();
		FrameworkListener fwkListener = new FrameworkListener() {
			@Override
			public void frameworkEvent(FrameworkEvent event) {
				int type = event.getType();
				if (type == FrameworkEvent.ERROR || type == FrameworkEvent.WARNING) {
					errorsAndWarnings.add(event);
				}
			}
		};
		systemContext.addFrameworkListener(fwkListener);

		Runnable asyncTest = new Runnable() {
			@Override
			public void run() {
				try {
					assertNotNull("Entry not found.", testBundle.getEntry("dirA/fileA"));
					assertNotNull("Entry not found.", testBundle.getEntry("dirA/dirB/fileB"));
					assertNotNull("Entry not found.", testBundle.getEntry("dirA/dirC/fileC"));
					assertNotNull("Entry not found.", testBundle.getEntry("dirA/dirC/"));
					URL dirBURL = converter.toFileURL(testBundle.getEntry("dirA/dirB/"));
					assertNotNull("Failed to convert to file URL", dirBURL);
					URL dirAURL = converter.toFileURL(testBundle.getEntry("dirA/"));
					assertNotNull("Failed to convert to file URL", dirAURL);
					List<URL> allEntries = testBundle.adapt(BundleWiring.class).findEntries("/", "*", BundleWiring.FINDENTRIES_RECURSE);
					assertEquals("Wrong number of entries: " + allEntries, 8, allEntries.size());
				} catch (IOException e) {
					throw new AssertionFailedError(e.getMessage());
				}
			}
		};

		// do test with two threads to make sure open lock is not held by a thread
		ExecutorService executor1 = Executors.newFixedThreadPool(1);
		ExecutorService executor2 = Executors.newFixedThreadPool(1);
		try {
			executor1.submit(asyncTest).get();
			executor2.submit(asyncTest).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			fail("Interrupted.", e);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof Error) {
				throw (Error) e.getCause();
			}
			throw (RuntimeException) e.getCause();
		} finally {
			executor1.shutdown();
			executor2.shutdown();
		}
		stop(equinox);
		assertEquals("Wrong state for SystemBundle", Bundle.RESOLVED, equinox.getState()); //$NON-NLS-1$

		if (!errorsAndWarnings.isEmpty()) {
			StringWriter errorStackTraces = new StringWriter();
			PrintWriter writer = new PrintWriter(errorStackTraces);
			for (FrameworkEvent frameworkEvent : errorsAndWarnings) {
				if (frameworkEvent.getThrowable() != null) {
					frameworkEvent.getThrowable().printStackTrace(writer);
				}
			}
			writer.close();
			fail("Found errors or warnings: " + errorsAndWarnings.size() + " - " + errorStackTraces.toString());
		}
	}

	public void testContextFinderGetResource() throws IOException, InvalidSyntaxException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map configuration = new HashMap();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(EquinoxConfiguration.PROP_CONTEXTCLASSLOADER_PARENT, EquinoxConfiguration.CONTEXTCLASSLOADER_PARENT_FWK);
		Equinox equinox = null;
		try {
			equinox = new Equinox(configuration);
			equinox.init();
			BundleContext bc = equinox.getBundleContext();
			// get the context finder explicitly to test
			ClassLoader contextFinder = bc.getService(bc.getServiceReferences(ClassLoader.class, "(equinox.classloader.type=contextClassLoader)").iterator().next());
			// Using a resource we know is in the framework
			String resource = "profile.list";
			URL fwkURL = Bundle.class.getClassLoader().getResource(resource);
			assertNotNull("Did not find a parent resource: " + resource, fwkURL);
			// should return the file defined in test bundle.
			URL url = contextFinder.getResource(resource);
			// the first element should be the file define in this bundle.
			List<URL> urls = Collections.list(contextFinder.getResources(resource));
			// make sure we have a resource located in the parent
			assertTrue("Did not find a parent resource: " + urls, urls.size() > 1);
			// assert failed as it return the one defined in parent class.
			assertEquals(url.toExternalForm(), urls.get(0).toExternalForm());
		} catch (BundleException e) {
			fail("Failed init", e);
		} finally {
			stop(equinox);
		}
	}

	public void testDynamicImportFromSystemBundle() throws IOException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map configuration = new HashMap();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		configuration.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "some.system.pkg");
		Equinox equinox = null;
		try {
			equinox = new Equinox(configuration);
			equinox.init();
			BundleContext bc = equinox.getBundleContext();

			Map<String, String> h2 = new HashMap<>();
			h2.put(Constants.BUNDLE_MANIFESTVERSION, "2");
			h2.put(Constants.BUNDLE_SYMBOLICNAME, getName() + ".dynamicimporter");
			h2.put(Constants.DYNAMICIMPORT_PACKAGE, "some.system.*; version=1.0");
			File f2 = SystemBundleTests.createBundle(config, getName() + ".importer", h2);
			Bundle b2 = bc.installBundle("reference:file:///" + f2.getAbsolutePath()); //$NON-NLS-1$
			b2.getResource("does/not/exist.txt");

			Map<String, String> h1 = new HashMap<>();
			h1.put(Constants.BUNDLE_MANIFESTVERSION, "2");
			h1.put(Constants.BUNDLE_SYMBOLICNAME, getName() + ".exporter");
			h1.put(Constants.EXPORT_PACKAGE, "some.system.pkg; version=1.0");
			File f1 = SystemBundleTests.createBundle(config, getName() + ".exporter", h1);
			Bundle b1 = bc.installBundle("reference:file:///" + f1.getAbsolutePath()); //$NON-NLS-1$

			b2.getResource("some/system/pkg/Test");

			BundleWiring w = b2.adapt(BundleWiring.class);
			assertNotNull("Null wiring.", w);
			List<BundleWire> pkgWires = w.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
			assertFalse("Empty wires.", pkgWires.isEmpty());
			assertEquals("Wrong provider", b1.adapt(BundleRevision.class), pkgWires.iterator().next().getProvider());
		} catch (BundleException e) {
			fail("Unexpected BundleException", e);
		} finally {
			stop(equinox);
		}
	}

	public void testDynamicImportPrivatePackage() throws IOException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map configuration = new HashMap();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());
		Equinox equinox = null;
		try {
			equinox = new Equinox(configuration);
			equinox.start();
			BundleContext bc = equinox.getBundleContext();

			Bundle importer = bc.installBundle(installer.getBundleLocation("test.dynamic.privateimport"));
			importer.start();

			Map<String, String> exporter = new HashMap<>();
			exporter.put(Constants.BUNDLE_MANIFESTVERSION, "2");
			exporter.put(Constants.BUNDLE_SYMBOLICNAME, getName() + ".exporter");
			exporter.put(Constants.EXPORT_PACKAGE, "test.dynamic.privateimport");
			File f1 = SystemBundleTests.createBundle(config, getName() + ".exporter", exporter);
			Bundle exporterBundle = bc.installBundle("reference:file:///" + f1.getAbsolutePath()); //$NON-NLS-1$
			exporterBundle.start();

			try {
				importer.loadClass("test.dynamic.privateimport.DoesNotExist");
			} catch (ClassNotFoundException e) {
				// expected
			}

			importer.stop();
			importer.start();
		} catch (BundleException e) {
			fail("Unexpected BundleException", e);
		} finally {
			stop(equinox);
		}
	}

	public void testCorruptStageInstallUpdate() throws IOException, BundleException {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); // $NON-NLS-1$
		final Equinox equinox = new Equinox(
				Collections.singletonMap(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath()));
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}

		File dirBundleFile = createBundle(config, "dir.bundle", false, true);
		File jarBundleFile = createBundle(config, "jar.bundle", false, false);

		// install dir bundle to get the path to storage
		BundleContext bc = equinox.getBundleContext();
		Bundle dirBundle = bc.installBundle(dirBundleFile.toURI().toASCIIString());
		assertEquals("Wrong BSN", "bundledir.bundle", dirBundle.getSymbolicName());

		URLConverter converter = bc.getService(bc.getServiceReference(URLConverter.class));
		URL dirFileURL = converter.resolve(dirBundle.getEntry("/"));
		File dirFile = new File(dirFileURL.getPath());
		File rootStore = dirFile.getParentFile().getParentFile().getParentFile();
		dirBundle.uninstall();

		long next = dirBundle.getBundleId() + 1;
		next = doTestExistingBundleFile(bc, next, rootStore, jarBundleFile, "bundlejar.bundle", true);
		next = doTestExistingBundleFile(bc, next, rootStore, jarBundleFile, "bundlejar.bundle", false);
		next = doTestExistingBundleFile(bc, next, rootStore, dirBundleFile, "bundledir.bundle", true);
		next = doTestExistingBundleFile(bc, next, rootStore, dirBundleFile, "bundledir.bundle", false);

	}

	private long doTestExistingBundleFile(BundleContext bc, long next, File rootStore, File content, String bsn,
			boolean d) throws IOException, BundleException {
		createGenerationContent(next, rootStore, d);
		Bundle b = bc.installBundle(content.toURI().toASCIIString());
		assertEquals("Wrong BSN", bsn, b.getSymbolicName());
		assertEquals("Wrong Bundle ID", next, b.getBundleId());
		b.uninstall();
		return b.getBundleId() + 1;
	}

	private Path createGenerationContent(long nextBundleID, File rootStore, boolean directory) throws IOException {
		Path nextBundleFile = new File(rootStore, nextBundleID + "/0/bundleFile").toPath();
		if (directory) {
			createDirectories(nextBundleFile);
			return write(createFile(new File(nextBundleFile.toFile(), "testContent.txt").toPath()),
					"Some Content".getBytes());
		}
		createDirectories(nextBundleFile.getParent());
		return write(createFile(nextBundleFile), "Some Content".getBytes());
	}

	// Note this is more of a performance test.  It has a timeout that will cause it to
	// fail if it takes too long.
	public void testMassiveParallelInstallStart() {
		File config = OSGiTestsActivator.getContext().getDataFile(getName()); //$NON-NLS-1$
		Map<String, Object> configuration = new HashMap<>();
		configuration.put(Constants.FRAMEWORK_STORAGE, config.getAbsolutePath());

		final Equinox equinox = new Equinox(configuration);
		try {
			equinox.init();
		} catch (BundleException e) {
			fail("Unexpected exception in init()", e); //$NON-NLS-1$
		}
		// should be in the STARTING state
		assertEquals("Wrong state for SystemBundle", Bundle.STARTING, equinox.getState()); //$NON-NLS-1$
		final BundleContext systemContext = equinox.getBundleContext();
		assertNotNull("System context is null", systemContext); //$NON-NLS-1$
		try {
			equinox.start();
		} catch (BundleException e) {
			fail("Failed to start the framework", e); //$NON-NLS-1$
		}
		assertEquals("Wrong state for SystemBundle", Bundle.ACTIVE, equinox.getState()); //$NON-NLS-1$

		long startCreateBundle = System.nanoTime();
		final int numBundles = 2000;
		final File[] testBundles;
		try {
			testBundles = createBundles(new File(config, "bundles"), numBundles, Collections.singletonMap(Constants.DYNAMICIMPORT_PACKAGE, "*")); //$NON-NLS-1$
		} catch (IOException e) {
			fail("Unexpected error creating budnles", e); //$NON-NLS-1$
			throw new RuntimeException();
		}
		System.out.println("Time to create: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startCreateBundle));

		long startReslveTime = System.nanoTime();
		ExecutorService executor = Executors.newFixedThreadPool(50);
		final List<Exception> errors = new CopyOnWriteArrayList<>();
		try {
			for (final File f : testBundles) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							String location = f.toURI().toURL().toExternalForm();
							System.out.println("Installing: " + f.getName());
							Bundle b = systemContext.installBundle(location);
							b.start();
							BundleWiring wiring = b.adapt(BundleWiring.class);
							wiring.getClassLoader().loadClass(BundleContext.class.getName());
						} catch (Exception e) {
							errors.add(e);
						}
					}
				});
			}
		} finally {
			executor.shutdown();
			try {
				assertTrue("Operation took too long", executor.awaitTermination(5, TimeUnit.MINUTES));
			} catch (InterruptedException e) {
				fail("Interrupted.", e);
			}
		}
		System.out.println("Time to resolve: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startReslveTime));

		Assert.assertEquals("Errors found.", Collections.emptyList(), errors);
		Assert.assertEquals("Wrong number of bundles.", numBundles + 1, systemContext.getBundles().length);

		List<BundleWire> providedWires = equinox.adapt(BundleWiring.class).getProvidedWires(PackageNamespace.PACKAGE_NAMESPACE);
		Assert.assertEquals("Wrong number of provided wires.", numBundles, providedWires.size());

		stop(equinox);
	}
}
