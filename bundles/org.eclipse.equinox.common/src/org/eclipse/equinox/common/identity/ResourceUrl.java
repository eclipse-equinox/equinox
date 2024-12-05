/*******************************************************************************
 * Copyright (c) 2024 ArSysOp.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Alexander Fedorov (ArSysOp) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.common.identity;

/**
 * Allows to work with resources (like images) from headless code
 * 
 * @see ImageIdentity
 * 
 */
public interface ResourceUrl {

	/**
	 * An URL to access the resource, most probably with "platform" schema
	 * 
	 * @return an URL to access the resource
	 */
	String url();

}
