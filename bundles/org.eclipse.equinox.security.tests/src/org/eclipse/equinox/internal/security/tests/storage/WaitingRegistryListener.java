/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.tests.storage;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.*;

/**
 * Allows test cases to wait for the extension registry notifications. This
 * listener checks navigability to related elements from extensions.
 * 
 * @since 3.4 Copied from runtime tests
 */
public class WaitingRegistryListener extends org.junit.Assert implements IRegistryEventListener {

	final static long MIN_WAIT_TIME = 100; // minimum wait time in milliseconds

	private List<String> extensionIDs; // String[]
	private List<String> extPointIDs; // String[]

	private volatile boolean added;
	private volatile boolean removed;
	private volatile int callbacks;

	private String extPointId;

	public WaitingRegistryListener() {
		reset();
	}

	public void register(String id) {
		extPointId = id; // used for verification in callbacks
		if (extPointId != null)
			RegistryFactory.getRegistry().addListener(this, extPointId);
		else
			RegistryFactory.getRegistry().addListener(this);
	}

	public void unregister() {
		RegistryFactory.getRegistry().removeListener(this);
	}

	public void reset() {
		extensionIDs = null;
		extPointIDs = null;
		added = false;
		removed = false;
		callbacks = 0;
	}

	public boolean isAdded() {
		return added;
	}

	public boolean isRemoved() {
		return removed;
	}

	public synchronized String[] extensionsReceived(long timeout) {
		if (extensionIDs != null)
			return extensionIDs.toArray(new String[0]);
		try {
			wait(timeout);
		} catch (InterruptedException e) {
			// who cares?
		}
		if (extensionIDs == null)
			return null;
		return extensionIDs.toArray(new String[0]);
	}

	public synchronized String[] extPointsReceived(long timeout) {
		if (extPointIDs != null)
			return extPointIDs.toArray(new String[0]);
		try {
			wait(timeout);
		} catch (InterruptedException e) {
			// who cares?
		}
		if (extPointIDs == null)
			return null;
		return extPointIDs.toArray(new String[0]);
	}

	public synchronized int waitFor(int events, long maxTimeout) {
		long startTime = System.currentTimeMillis();
		try {
			while (callbacks < events) {
				long currentTime = System.currentTimeMillis();
				long alreadyWaited = currentTime - startTime;
				if (alreadyWaited < 0)
					alreadyWaited = 0; // just in case if system timer is not very precise
				long timeToWait = maxTimeout - alreadyWaited;
				if (timeToWait <= 0) {
					wait(MIN_WAIT_TIME); // give it a last chance
					break; // timed out
				}
				wait(timeToWait);
			}
		} catch (InterruptedException e) {
			// breaks the cycle
		}
		return callbacks;
	}

	@Override
	synchronized public void added(IExtension[] extensions) {
		extensionsToString(extensions);
		added = true;
		callbacks++;
		notify();
	}

	@Override
	synchronized public void removed(IExtension[] extensions) {
		extensionsToString(extensions);
		removed = true;
		callbacks++;
		notify();
	}

	@Override
	synchronized public void added(IExtensionPoint[] extensionPoints) {
		extPointsToString(extensionPoints);
		added = true;
		callbacks++;
		notify();
	}

	@Override
	synchronized public void removed(IExtensionPoint[] extensionPoints) {
		extPointsToString(extensionPoints);
		removed = true;
		callbacks++;
		notify();
	}

	private void extensionsToString(IExtension[] extensions) {
		extensionIDs = new ArrayList<>(extensions.length);
		for (IExtension extension : extensions) {
			extensionIDs.add(extension.getUniqueIdentifier());

			// test navigation: to extension point
			String ownerId = extension.getExtensionPointUniqueIdentifier();
			if (extPointId != null)
				assertTrue(extPointId.equals(ownerId));
			// test navigation: all children
			assertTrue(validContents(extension.getConfigurationElements()));
		}
	}

	private boolean validContents(IConfigurationElement[] children) {
		if (children == null)
			return true;
		for (IConfigurationElement child : children) {
			if (!child.isValid()) {
				return false;
			}
			if (!validContents(child.getChildren())) {
				return false;
			}
		}
		return true;
	}

	private void extPointsToString(IExtensionPoint[] extensionPoints) {
		extPointIDs = new ArrayList<>(extensionPoints.length);
		for (IExtensionPoint extensionPoint : extensionPoints) {
			extPointIDs.add(extensionPoint.getUniqueIdentifier());
		}
	}

}
