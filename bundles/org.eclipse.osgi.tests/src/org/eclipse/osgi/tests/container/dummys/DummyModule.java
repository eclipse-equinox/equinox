/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
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
package org.eclipse.osgi.tests.container.dummys;

import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.container.ModuleRevision;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

public class DummyModule extends Module {
	private final CountDownLatch startLatch;
	private final CountDownLatch stopLatch;

	public DummyModule(Long id, String location, ModuleContainer container, EnumSet<Settings> settings, int startlevel) {
		this(id, location, container, settings, startlevel, null, null);
	}

	public DummyModule(Long id, String location, ModuleContainer container, EnumSet<Settings> settings, int startlevel, CountDownLatch startLatch, CountDownLatch stopLatch) {
		super(id, location, container, settings, startlevel);
		this.startLatch = startLatch == null ? new CountDownLatch(0) : startLatch;
		this.stopLatch = stopLatch == null ? new CountDownLatch(0) : stopLatch;
	}

	@Override
	public Bundle getBundle() {
		return null;
	}

	@Override
	protected void cleanup(ModuleRevision revision) {
		// Do nothing
	}

	@Override
	protected void startWorker() throws BundleException {
		try {
			startLatch.await();
		} catch (InterruptedException e) {
			Thread.interrupted();
			throw new BundleException("Interrupted.", e);
		}
		super.startWorker();
	}

	@Override
	protected void stopWorker() throws BundleException {
		try {
			stopLatch.await();
		} catch (InterruptedException e) {
			Thread.interrupted();
			throw new BundleException("Interrupted.", e);
		}
		super.stopWorker();
	}
}
