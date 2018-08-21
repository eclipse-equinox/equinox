/*******************************************************************************
 * Copyright (c) 1997, 2018 by ProSyst Software GmbH
 * http://www.prosyst.com
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.util.hash;

/**
 *  Synchronized extension of org.eclipse.equinox.internal.util.hash.HashIntObjNS
 *  
 * @author Pavlin Dobrev
 * @version 1.0
 */

public final class HashIntObjS extends HashIntObjNS {

	public HashIntObjS() {
		super(101, LOAD_FACTOR);
	}

	public HashIntObjS(int capacity) {
		super(capacity, LOAD_FACTOR);
	}

	public HashIntObjS(int capacity, double lf) {
		super(capacity, lf);
	}

	@Override
	public synchronized void put(int key, Object value) {
		super.put(key, value);
	}

	@Override
	public synchronized Object get(int key) {
		return super.get(key);
	}

	@Override
	public synchronized Object remove(int key) {
		return super.remove(key);
	}

	@Override
	public synchronized int size() {
		return super.size();
	}

	@Override
	public synchronized void removeAll() {
		super.removeAll();
	}
}
