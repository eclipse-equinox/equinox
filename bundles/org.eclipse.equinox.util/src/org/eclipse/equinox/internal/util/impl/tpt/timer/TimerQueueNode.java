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

package org.eclipse.equinox.internal.util.impl.tpt.timer;

import java.security.AccessControlContext;
import org.eclipse.equinox.internal.util.impl.tpt.ServiceFactoryImpl;
import org.eclipse.equinox.internal.util.pool.ObjectCreator;
import org.eclipse.equinox.internal.util.timer.Timer;
import org.eclipse.equinox.internal.util.timer.TimerListener;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

class TimerQueueNode implements Runnable, ObjectCreator {
	public static final String PERIODICAL_TASK_NAME = "[Timer] - Periodical Task";

	public static final String PERIODICAL_NO_DELAY_TASK_NAME = "[Timer] - Periodical No Delay Task";

	public static final String ONE_SHOT_TASK_NAME = "[Timer] - One Shot Task";

	public static final String ONE_SHOT_NO_DELAY_TASK_NAME = "[Timer] - One Shot No Delay Task";

	public TimerQueueNode() {
	}

	TimerListener listener = null;

	int event;

	long runOn = -1;

	int type = -1;

	int priority = -1;

	long period = -1;

	boolean running = false;

	private int theHash;

	String name;

	boolean named = false;

	String context;

	AccessControlContext acc;

	boolean enabled = false;

	public String getEName() {
		if (ServiceFactoryImpl.useNames && !named) {
			named = true;
			String tmp = null;
			switch (type) {
				case Timer.ONE_SHOT_TIMER : {
					tmp = ONE_SHOT_TASK_NAME;
					break;
				}
				case Timer.ONE_SHOT_TIMER_NO_DELAY : {
					tmp = ONE_SHOT_NO_DELAY_TASK_NAME;
					break;
				}
				case Timer.PERIODICAL_TIMER : {
					tmp = PERIODICAL_TASK_NAME;
					break;
				}
				case Timer.PERIODICAL_TIMER_NO_DELAY : {
					tmp = PERIODICAL_NO_DELAY_TASK_NAME;
					break;
				}
			}
			name = (name != null) ? tmp.concat(name) : tmp;
		}
		return ((name == null) ? "" : name);
	}

	public void setEvent(TimerListener listener, int priority, int timerType, long runOn, long periodMilis, int event, String name, AccessControlContext acc) {
		this.enabled = true;
		this.listener = listener;
		theHash = listener.hashCode() + event;
		this.priority = priority;
		type = timerType;
		period = periodMilis;
		this.runOn = runOn;
		this.event = event;
		this.name = name;
		this.named = false;
		this.acc = acc;
	}

	void returnInPool() {
		synchronized (this) {
			if (!enabled || (running && type == Timer.ONE_SHOT_TIMER)) {
				/* this node has already been put in pool or will be put */
				return;
			}
			clear();
		}
		if (TimerImpl.nodePool != null) {
			TimerImpl.nodePool.releaseObject(this);
		}
	}

	public void run() {
		synchronized (this) {
			running = true;
		}
		TimerListener tmp = listener;
		try {
			if (tmp != null && enabled) {
				tmp.timer(event);
			}
		} catch (Throwable t) {
			if (ServiceFactoryImpl.log != null) {
				ServiceFactoryImpl.log.error("[Timer] - Error while notifying:\r\n" + tmp, t);
			}
		}
		tmp = null;
		if (type == Timer.ONE_SHOT_TIMER)
			returnInPool();
		running = false;
	}

	public Object getInstance() {
		return new TimerQueueNode();
	}

	private void clear() {
		named = false;
		name = null;
		enabled = false;
		listener = null;
		event = -1;
		runOn = Long.MAX_VALUE;
		type = -1;
		acc = null;

	}

	public int hashCode() {
		TimerListener lis = listener;
		return (lis != null) ? (theHash = (lis.hashCode() + event)) : theHash;
	}

	public String toString() {
		return "QueueNode: " + super.toString() + "\r\n" + "\t\tListener: " + listener + "\r\n" + "\t\tEvent: " + event + "\r\n" + "\t\tType: " + type + "\r\n" + "\t\trunafter: " + (runOn - System.currentTimeMillis()) + "\r\n" + "\t\tEnabled: " + enabled;
	}

	public boolean equals(Object a) {
		if (a instanceof TimerQueueNode) {
			TimerQueueNode b = (TimerQueueNode) a;
			return b.listener == listener && b.event == event;
		}
		return false;
	}
}
