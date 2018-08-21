/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
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
