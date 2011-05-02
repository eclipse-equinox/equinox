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

import org.eclipse.equinox.internal.util.impl.tpt.ServiceFactoryImpl;
import org.eclipse.equinox.internal.util.impl.tpt.threadpool.ThreadPoolFactoryImpl;
import org.eclipse.equinox.internal.util.ref.Log;
import org.eclipse.equinox.internal.util.timer.Timer;
import org.eclipse.equinox.internal.util.timer.TimerListener;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class TimerFactory extends ServiceFactoryImpl implements Timer {
	private static TimerImpl timer;

	public TimerFactory(String bundleName, ThreadPoolFactoryImpl factory, Log log) {

		super(bundleName, log);
		timer = new TimerImpl(factory);
	}

	public TimerFactory(String bundleName) {
		super(bundleName);
	}

	public Object getInstance(String bundleName) {
		if (timer == null)
			throw new RuntimeException("ServiceFactory is currently off!");
		return new TimerFactory(bundleName);
	}

	public void notifyAfterMillis(TimerListener listener, long timePeriod, int event) throws IllegalArgumentException {
		addNotifyListener(listener, Thread.NORM_PRIORITY, Timer.ONE_SHOT_TIMER, timePeriod, event);
	}

	public void notifyAfterMillis(TimerListener listener, int priority, long timePeriod, int event) throws IllegalArgumentException {
		addNotifyListener(listener, priority, Timer.ONE_SHOT_TIMER, timePeriod, event);
	}

	public void notifyAfter(TimerListener listener, int timePeriod, int event) throws IllegalArgumentException {
		addNotifyListener(listener, Thread.NORM_PRIORITY, Timer.ONE_SHOT_TIMER, timePeriod * 1000, event);
	}

	public void notifyAfter(TimerListener listener, int priority, int timePeriod, int event) throws IllegalArgumentException {
		addNotifyListener(listener, priority, Timer.ONE_SHOT_TIMER, timePeriod * 1000, event);
	}

	public void addNotifyListener(TimerListener listener, int priority, int timerType, long periodMilis, int event) {
		TimerImpl tmp = timer;
		if (tmp == null)
			throw new RuntimeException("This is a zombie!");
		tmp.addNotifyListener(listener, priority, timerType, periodMilis, event, bundleName);
	}

	public static void stopTimer() {
		if (timer != null) {
			timer.terminate();
			timer = null;
		}
	}

	public void removeListener(TimerListener listener, int event) {
		TimerImpl tmp = timer;
		if (tmp == null)
			throw new RuntimeException("This is a zombie!");
		tmp.removeListener(listener, event);
	}
}
