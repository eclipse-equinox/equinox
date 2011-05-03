/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.region;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleRevision;

/**
 * Isolates all equinox specific code to a specific class to allow the equinox packages to be optional.
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

		public long getStateTimeStamp(BundleContext context) {
			ServiceReference<PlatformAdmin> ref = context.getServiceReference(PlatformAdmin.class);
			if (ref == null)
				return -1;
			PlatformAdmin pa = context.getService(ref);
			if (pa == null)
				return -1;
			try {
				return pa.getState(false).getTimeStamp();
			} finally {
				context.ungetService(ref);
			}
		}
	}

	public static final long getBundleId(BundleRevision revision) {
		// For testability, use the bundle revision's bundle before casting to ResolverBundle.
		Bundle bundle = revision.getBundle();
		if (bundle != null) {
			return bundle.getBundleId();
		}
		Long result = equinoxSupport == null ? null : equinoxSupport.getBundleId(revision);
		if (result == null)
			throw new RuntimeException(String.format("Cannot determine bundle id of BundleRevision '%s'", revision)); //$NON-NLS-1$
		return result;
	}

	public static final long getStateTimeStamp(BundleContext context) {
		return equinoxSupport == null ? -1 : equinoxSupport.getStateTimeStamp(context);
	}
}
