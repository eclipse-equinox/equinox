/*******************************************************************************
 * Copyright (c) 2022, 2022 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.ds.tests;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

public class LazyServiceComponentActivationDeadLockTest {

	// TODO: clean up when finalized

	// TODO: add test cases for dead-lock due to lazy activation in different
	// bundles?!

	// TODO: add a simple test-case that uses a generic ServiceFactory

	@Test
	public void testLateBindingInSameBundleDeadLock() throws Exception {
		BundleContext ctx = FrameworkUtil.getBundle(LazyServiceComponentActivationDeadLockTest.class)
				.getBundleContext();
		ServiceReference<ProjectConfigurationManager> reference = ctx
				.getServiceReference(ProjectConfigurationManager.class);

		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			Future<ProjectConfigurationManager> service = executor.submit(() -> ctx.getService(reference));
			assertNotNull(service.get(5, TimeUnit.SECONDS)); // times out in case of dead-lock
		} finally {
			executor.shutdown();
			//			ctx.ungetService(reference);
		}

		// TODO: when we have a strategy to re-cover from the deadlock, check that the
		// component is properly populated after recovery.
	}

	// TODO: move the components to dedicated bundles_src subfolders?
	// TODO: make the test cases less m2e specific?

	@Component(service = ProjectConfigurationManager.class)
	public static class ProjectConfigurationManager {

		@Reference
		MavenImpl maven;

		@Reference
		MavenModelManager mavenModelManager;

	}

	@Component(service = MavenModelManager.class)
	public static class MavenModelManager {

		@Reference
		private MavenProjectManager projectManager;

		// The nested component below ensures that the activation of this component
		// starts after the activation of MavenProjectManager has started in the SCR
		// Actor thread.

		@Reference
		HeavyComponent AAA; // Capital letters are important to ensure this reference is handled first

		@Component(service = HeavyComponent.class)
		public static class HeavyComponent {

			@Activate
			public void activated() throws InterruptedException {
				MavenProjectManager.HeavyComponent.ACTIVATED.await();
			}
		}
	}

	@Component(service = MavenProjectManager.class)
	public static class MavenProjectManager {

		private final List<ProjectConfigurationManager> listenerManager = new ArrayList<>();

		@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
		public void addMavenProjectChangedListener(ProjectConfigurationManager listener) {
			listenerManager.add(listener);
		}

		public void removeMavenProjectChangedListener(ProjectConfigurationManager listener) {
			listenerManager.remove(listener);
		}

		// The nested component below ensures that the activation of this component
		// starts after the activation of MavenModelManager has started in the main
		// thread.

		@Reference
		HeavyComponent AAA; // Capital letters are important to ensure this reference is handled first

		@Component(service = HeavyComponent.class)
		public static class HeavyComponent {
			public static final CountDownLatch ACTIVATED = new CountDownLatch(1);

			@Activate
			public void activated() {
				ACTIVATED.countDown();
			}
		}
	}

	@Component(service = MavenImpl.class)
	public static class MavenImpl {

		@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
		private final List<RepositoryRegistry> settingsListeners = new CopyOnWriteArrayList<>();
	}

	@Component(service = RepositoryRegistry.class)
	public static class RepositoryRegistry {

		@Reference
		private MavenImpl maven;

		@Reference
		MavenProjectManager projectManager;
	}

}
