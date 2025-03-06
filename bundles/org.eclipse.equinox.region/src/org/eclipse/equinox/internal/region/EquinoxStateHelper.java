/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
package org.eclipse.equinox.internal.region;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleRevision;

/**
 * Isolates all equinox specific code to a specific class to allow the equinox
 * packages to be optional.
 */
public class EquinoxStateHelper {
	static final Worker equinoxSupport;

	static {
		Worker result = null;
		try {
			Class.forName("org.eclipse.osgi.service.resolver.BundleDescription"); //$NON-NLS-1$
			result = new Worker();
		} catch (ClassNotFoundException e) {
			// nothing
		}
		equinoxSupport = result;
	}

	static final class Worker {
		final Long getBundleId(BundleRevision revision) {
			if (revision instanceof BundleDescription) {
				return ((BundleDescription) revision).getBundleId();
			}
			return null;
		}
	}

	public static final long getBundleId(BundleRevision revision) {
		// For testability, use the bundle revision's bundle before casting to
		// BundleDescription.
		Bundle bundle = revision.getBundle();
		if (bundle != null) {
			return bundle.getBundleId();
		}
		// Note that this bit of code is never useful at runtime since the framework
		// never uses BundleDescriptions.
		// It is only useful if the region hooks are used with the old Equinox State API
		Long result = equinoxSupport == null ? null : equinoxSupport.getBundleId(revision);
		if (result == null) {
			throw new RuntimeException(String.format("Cannot determine bundle id of BundleRevision '%s'", revision)); //$NON-NLS-1$
		}
		return result;
	}

}
