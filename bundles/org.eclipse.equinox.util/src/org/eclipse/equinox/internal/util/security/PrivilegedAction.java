/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.util.security;

/**
 * <p>
 * This class is a wrapper of java.security.PrivilegedAction &&
 * java.security.PriviligedExceptionAction.
 * 
 * <p>
 * Its purpose is to hide the dependency on java.security package, so that the
 * user of this class, could be used on JDK 1.1 compatible JVM-s and the others.
 * In the first case, no security mechanism is provided in the JVM, so there is
 * no need to execute doPrivileged blocks at all. In the second case the real
 * java.security.AccessController, java.security.PrivilegedAction and
 * java.security.PrivilegedExceptionAction are used to execute doPrivileged
 * blocks.
 * 
 * @deprecated java.security package API should be used in order to execute
 *             appropriately doPrivileged blocks. The provided implementation is
 *             fake and is left only for backward compatibility.
 * 
 * @author Svetozar Dimov
 * @author Pavlin Dobrev
 * @version 1.0
 * 
 */
public interface PrivilegedAction {

	/**
	 * Substitute for PrivilegedAction.run() and PrivilegedExceptionAction.run()
	 * in java.security package.
	 * 
	 * @return
	 * @throws Exception
	 */
	public Object run0() throws Exception;
}
