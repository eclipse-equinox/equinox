/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds;

import java.security.AccessController;
import java.security.PrivilegedAction;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * This class provide implementation hooks into the Eclipse OSGi framework
 * implementation. This class must be modified to connect SCR with other
 * framework implementations.
 * 
 * @version $Revision: 1.2 $
 */
class FrameworkHook extends AbstractReflector {
	/**
	 * Return the BundleContext for the specified bundle.
	 * 
	 * @param bundle The bundle whose BundleContext is desired.
	 * @return The BundleContext for the specified bundle.
	 */
	BundleContext getBundleContext(final Bundle bundle) {
		if (System.getSecurityManager() == null) {
			return (BundleContext) invokeMethod(bundle, "getContext", null, null); //$NON-NLS-1$
		}
		return (BundleContext) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return invokeMethod(bundle, "getContext", null, null); //$NON-NLS-1$
			}
		});
	}

	/**
	 * Throws an IllegalStateException if the reflection logic cannot find what
	 * it is looking for. This probably means this class does not properly
	 * recognize the framework implementation.
	 * 
	 * @param e Exception which indicates the reflection logic is confused.
	 */
	protected void reflectionException(Exception e) {
		throw new IllegalStateException("FrameworkHook does not recognize the framework implementation: " + e.getMessage());
	}

}
