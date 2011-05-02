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
package org.eclipse.equinox.internal.util.hash;

/**
 *  Synchronized extension of org.eclipse.equinox.internal.util.hash.HashObjLongNS
 *  
 * @author Pavlin Dobrev
 * @version 1.0
 */

public final class HashObjLongS extends HashObjLongNS {

	public HashObjLongS() {
		super(101, LOAD_FACTOR);
	}

	public HashObjLongS(int capacity) {
		super(capacity, LOAD_FACTOR);
	}

	public HashObjLongS(int capacity, double lf) {
		super(capacity, lf);
	}

	public synchronized void put(Object key, long value) {
		super.put(key, value);
	}

	public synchronized long get(Object key) {
		return super.get(key);
	}

	public synchronized long remove(Object key) {
		return super.remove(key);
	}

	public synchronized int size() {
		return super.size();
	}

	public synchronized void removeAll() {
		super.removeAll();
	}
}
