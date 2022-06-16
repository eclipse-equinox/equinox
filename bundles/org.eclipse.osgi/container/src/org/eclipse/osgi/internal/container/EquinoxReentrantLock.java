/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package org.eclipse.osgi.internal.container;

import java.util.concurrent.locks.ReentrantLock;

/**
 * This is just a ReentrantLock that makes getOwner a public methd
 */
public final class EquinoxReentrantLock extends ReentrantLock {
	private static final long serialVersionUID = 1L;

	@Override
	final public Thread getOwner() {
		return super.getOwner();
	}
}
