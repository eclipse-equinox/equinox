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
package org.eclipse.equinox.internal.util.ref;

import org.eclipse.equinox.internal.util.timer.Timer;
import org.eclipse.equinox.internal.util.timer.TimerListener;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class TimerRef {

	public static Timer timer = null;

	public static void notifyAfter(TimerListener listener, long millis, int event) {

		if (timer == null) {
			listener.timer(event);
			return;
		}
		timer.addNotifyListener(listener, Thread.NORM_PRIORITY, Timer.ONE_SHOT_TIMER, millis, event);
	}

	public static void removeListener(TimerListener listener, int event) {
		if (timer == null)
			return;
		timer.removeListener(listener, event);
	}
}
