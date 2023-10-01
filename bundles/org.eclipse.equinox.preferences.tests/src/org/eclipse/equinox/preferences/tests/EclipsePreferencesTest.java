/*******************************************************************************
 * Copyright (c) 2018 InterSystems Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     InterSystems Corporation - tests for Bug 444188
 *******************************************************************************/
package org.eclipse.equinox.preferences.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.CountDownLatch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.service.datalocation.Location;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Test suite for class org.eclipse.core.internal.preferences.EclipsePreferences
 * WARNING: many tests are still located in
 * org.eclipse.core.tests.internal.preferences.EclipsePreferencesTest from
 * eclipse.platform.runtime
 */
public class EclipsePreferencesTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void testBundleStateScope()
			throws InvalidSyntaxException, IllegalStateException, MalformedURLException, IOException {
		Bundle bundle = FrameworkUtil.getBundle(EclipsePreferencesTest.class);
		assertNotNull(bundle);
		BundleContext bundleContext = bundle.getBundleContext();
		assertNotNull(bundleContext);
		initLocation(bundleContext);
		ServiceTracker<IScopeContext, IScopeContext> serviceTracker = new ServiceTracker<>(bundleContext,
				IScopeContext.class, null);
		serviceTracker.open();
		try {
			IScopeContext context = serviceTracker.getService();
			assertEquals(InstanceScope.INSTANCE.getNode(bundle.getSymbolicName()), context.getNode(""));
			assertEquals(Platform.getStateLocation(bundle), context.getLocation());
		} finally {
			serviceTracker.close();
		}
	}

	private void initLocation(BundleContext bundleContext)
			throws InvalidSyntaxException, IOException, MalformedURLException {
		ServiceTracker<Location, Location> serviceTracker = new ServiceTracker<>(bundleContext,
				bundleContext.createFilter(Location.INSTANCE_FILTER), null);
		serviceTracker.open();
		try {
			Location location = serviceTracker.getService();
			assertNotNull(location);
			if (!location.isSet()) {
				location.set(folder.getRoot().toURI().toURL(), false);
			}
		} finally {
			serviceTracker.close();
		}
	}

	/**
	 * Concurrent access to listener collection should not lead to exceptions
	 * 
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=444188
	 */
	@Test
	public void testConcurrentPreferenceChangeListener() throws InterruptedException, CoreException {
		final IEclipsePreferences node = createTestNode();
		final int runSize = 100000;

		executeInTwoThreads(new ICoreRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				IPreferenceChangeListener listener = new IPreferenceChangeListener() {
					@Override
					public void preferenceChange(PreferenceChangeEvent event) {
					}
				};
				for (int i = 0; i < runSize && !monitor.isCanceled(); i++) {
					node.addPreferenceChangeListener(listener); // Should not throw
					node.put("x", "y"); // Should not throw
					node.remove("x"); // Should not throw
					node.removePreferenceChangeListener(listener); // Should not throw
				}
			}
		});
	}

	/**
	 * Concurrent access to listener collection should not lead to exceptions
	 * 
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=444188
	 */
	@Test
	public void testConcurrentNodeChangeListener() throws InterruptedException, CoreException {
		final IEclipsePreferences node = createTestNode();
		final int runSize = 100000;
		executeInTwoThreads(new ICoreRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				INodeChangeListener listener = new INodeChangeListener() {

					@Override
					public void removed(NodeChangeEvent event) {
					}

					@Override
					public void added(NodeChangeEvent event) {
					}
				};
				for (int i = 0; i < runSize && !monitor.isCanceled(); i++) {
					node.addNodeChangeListener(listener); // Should not throw
					try {
						node.node(Thread.currentThread().getName()).removeNode(); // Should not throw
					} catch (BackingStoreException e) {
						throw new CoreException(
								new Status(IStatus.ERROR, "org.eclipse.core.tests.runtime", 0, "", null));
					}
					node.removeNodeChangeListener(listener); // Should not throw
				}
			}
		});
	}

	private static void executeInTwoThreads(final ICoreRunnable runnable) throws InterruptedException, CoreException {
		final CountDownLatch latch = new CountDownLatch(1);
		Job job = Job.create("", new ICoreRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				latch.countDown();
				runnable.run(monitor);
			}
		});
		job.schedule();
		try {
			latch.await();
			runnable.run(new NullProgressMonitor());
		} finally {
			job.cancel();
			job.join();
			IStatus result = job.getResult();
			assertNotNull("Job is expected to complete", result);
			if (!result.isOK()) {
				throw new CoreException(result);
			}
		}
	}

	@After
	public void after() throws BackingStoreException {
		getScopeRoot().removeNode();
	}

	private static String getUniqueString() {
		return System.currentTimeMillis() + "-" + Math.random();
	}

	private static IEclipsePreferences createTestNode() {
		return (IEclipsePreferences) getScopeRoot().node(getUniqueString());
	}

	private static IEclipsePreferences getScopeRoot() {
		return (IEclipsePreferences) Platform.getPreferencesService().getRootNode().node("EclipsePreferencesTest");
	}
}
