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
package org.eclipse.osgi.container.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.tests.dummys.DummyContainerAdaptor;
import org.eclipse.osgi.container.tests.dummys.DummyResolverHookFactory;
import org.eclipse.osgi.framework.report.ResolutionReport;
import org.eclipse.osgi.framework.report.ResolutionReportReader;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.*;

public class ResolutionReportTest extends AbstractTest {
	private static class ResolverHookFactory implements org.osgi.framework.hooks.resolver.ResolverHookFactory {
		private AtomicBoolean receivedCallback;

		public ResolverHookFactory(AtomicBoolean receivedCallback) {
			this.receivedCallback = receivedCallback;
		}

		@Override
		public org.osgi.framework.hooks.resolver.ResolverHook begin(Collection<BundleRevision> triggers) {
			return new ResolverHook(receivedCallback);
		}
	}

	private static class ResolverHook implements org.osgi.framework.hooks.resolver.ResolverHook, ResolutionReportReader {
		private final AtomicBoolean receivedCallback;

		public ResolverHook(AtomicBoolean receivedCallback) {
			this.receivedCallback = receivedCallback;
		}

		@Override
		public void end() {
			// TODO Auto-generated method stub

		}

		@Override
		public void filterResolvable(Collection<BundleRevision> candidates) {
			// TODO Auto-generated method stub

		}

		@Override
		public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
			// TODO Auto-generated method stub

		}

		@Override
		public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
			// TODO Auto-generated method stub

		}

		@Override
		public void read(ResolutionReport report) {
			receivedCallback.set(true);
		}
	}

	@Test
	public void testResolutionReportReadCallback() throws Exception {
		AtomicBoolean receivedCallback = new AtomicBoolean(false);
		registerService(org.osgi.framework.hooks.resolver.ResolverHookFactory.class, new ResolverHookFactory(receivedCallback));

		getSystemBundle().adapt(FrameworkWiring.class).resolveBundles(Collections.singleton(getSystemBundle()));

		//		DummyContainerAdaptor adaptor = createDummyAdaptor();
		//		ModuleContainer container = adaptor.getContainer();
		//		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		//		container.resolve(Arrays.asList(systemBundle), true);
		assertTrue("Did not receive a resolution report callback", receivedCallback.get());
	}

	@Test
	public void testResolutionReportWriteCallback() throws Exception {
		DummyContainerAdaptor adaptor = createDummyAdaptor();
		ModuleContainer container = adaptor.getContainer();
		Module systemBundle = installDummyModule("system.bundle.MF", Constants.SYSTEM_BUNDLE_LOCATION, container);
		container.resolve(Arrays.asList(systemBundle), true);
		assertEquals("No resolution report write callback", 1, ((DummyResolverHookFactory) adaptor.getResolverHookFactory()).getWriteCount());
	}
}
