/*******************************************************************************
 * Copyright (c) 1997-2009 by ProSyst Software GmbH
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
package org.eclipse.equinox.ds.tests.tbc;

public class DSEvent implements Comparable {

	public static final int ACT_BOUND = 1;
	public static final int ACT_UNBOUND = 2;
	public static final int ACT_ACTIVATE = 3;
	public static final int ACT_DEACTIVATE = 4;

	private static long lastTime = System.currentTimeMillis();
	private static final Object lock = new Object();

	private long time;
	private int action;
	private Object object;

	public DSEvent(int action, Object object) {
		synchronized (lock) { // to prevent from creating BoundServiceEvents in one and the same millisecond
			this.action = action;
			this.object = object;
			while (lastTime == System.currentTimeMillis())
				;
			this.time = lastTime = System.currentTimeMillis();
		}
	}

	public int getAction() {
		return action;
	}

	public Object getObject() {
		return object;
	}

	public long getTime() {
		return time;
	}

	/**
	 * Returns whether this event is before the passed one
	 */
	public boolean before(DSEvent event) {
		if (event.time > this.time) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Object var0) {
		DSEvent event = (DSEvent) var0;
		if (event.time > this.time) {
			return -1;
		} else if (event.time < this.time) {
			return 1;
		} else {
			return 0;
		}
	}

	protected String getActionAsString() {
		switch (getAction()) {
		case ACT_BOUND:
			return "ACT_BOUND";
		case ACT_UNBOUND:
			return "ACT_UNBOUND";
		case ACT_ACTIVATE:
			return "ACT_ACTIVATE";
		case ACT_DEACTIVATE:
			return "ACT_DEACTIVATE";
		default:
			return "UNKNOWN (" + getAction() + ")";
		}
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("DSEvent[");
		buf.append("time=" + this.time + ";action=");
		buf.append(getActionAsString());
		buf.append(";object=" + (this.object != null ? this.object.toString() : "null"));
		buf.append("]");
		return buf.toString();
	}

}
