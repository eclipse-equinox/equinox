/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package org.eclipse.osgi.internal.hookregistry;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * The helper provides alternative implementations for methods in the
 * {@link FrameworkUtil} class.  While this is not a hook, it is possible
 * for framework fragments to provide a META-INF/services configuration
 * to allow their own implementation to be loaded by the {@link FrameworkUtil}
 * class.
 */
public class FrameworkUtilHelper {
	/**
	 * See {@link FrameworkUtil#getBundle(Class)}
	 * @param classFromBundle a class defined by a bundle class loader.
	 * @return A Bundle for the specified bundle class or null if the 
	 * specified class was not defined by a bundle class loader.
	 */
	public Bundle getBundle(Class<?> classFromBundle) {
		return null;
	}
}
