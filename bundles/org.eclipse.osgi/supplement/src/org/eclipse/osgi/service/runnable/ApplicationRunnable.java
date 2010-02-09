/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.runnable;

/**
 * A parameterized runnable that can be stopped.
 * <p>
 * This class is for internal use by the platform-related plug-ins.
 * Clients outside of the base platform should not reference or subclass this class.
 * </p>
 * @since 3.2
 */
public interface ApplicationRunnable extends ParameterizedRunnable {
	/**
	 * Forces this runnable to stop.
	 */
	public void stop();
}
