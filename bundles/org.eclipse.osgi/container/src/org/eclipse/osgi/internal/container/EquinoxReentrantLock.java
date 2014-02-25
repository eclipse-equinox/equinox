/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	final public Thread getOwner() {
		return super.getOwner();
	}
}
