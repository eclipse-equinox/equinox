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
package org.eclipse.equinox.internal.util.timer;

/**
 * Timer service provides means for sending notifications at given time periods
 * to each listener registered with Timer. To receive notifications, a module
 * should first register a TimerListener, associated with an (int) event and a
 * time period. When the time period passes, TimerListener's timer method is
 * invoked, and the listener is removed from the queue with the waiting event
 * notifications.
 * 
 * @see TimerListener
 * 
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface Timer {

	/**
	 * Constant indicates that timer listener will be notified only once and
	 * afterwards discarded.
	 */
	public int ONE_SHOT_TIMER = 0;

	/**
	 * Constant indicates that timer listener will be notified periodically at a
	 * given time interval.
	 */
	public int PERIODICAL_TIMER = 1;

	/**
	 * Constant indicates that timer listener will be notified only once. Timer
	 * implementation would do its best to execute the notification with minimum
	 * possible delay.
	 */
	public int ONE_SHOT_TIMER_NO_DELAY = 2;

	/**
	 * Constant indicates that timer listener will be notified periodically at a
	 * given time interval. Timer implementation would do its best to execute
	 * the notification with minimum possible delay.
	 */
	public int PERIODICAL_TIMER_NO_DELAY = 3;

	/**
	 * Adds new TimerListener to the timer event quueue. The listener will be
	 * notified after the given <code>timePeriod</code> with specified
	 * <code>event</code>. If the event queue already contains a listener and
	 * event pair equal to those being passed, then the old notification object
	 * is removed from queue and the new data takes its place.
	 * 
	 * @param listener
	 *            the listener which will be notified after the given time
	 *            period
	 * @param timePeriod
	 *            time period in milliseconds after which the listener will be
	 *            notified
	 * @param event
	 *            which will be supplied to the listener when it is notified
	 * 
	 * @exception IllegalArgumentException
	 *                if time period is not positive
	 * @deprecated
	 */
	public void notifyAfterMillis(TimerListener listener, long timePeriod, int event) throws IllegalArgumentException;

	/**
	 * Adds new TimerListener to the timer event quueue. The listener will be
	 * notified after the given <code>timePeriod</code> with specified
	 * <code>event</code>. If the event queue already contains a listener and
	 * event pair equal to those being passed, then the old notification object
	 * is removed from queue and the new data takes its place.
	 * 
	 * @param listener
	 *            the listener which will be notified after the given time
	 *            period
	 * @param priority
	 *            priority of executing thread
	 * @param timePeriod
	 *            time period in milliseconds after which the listener will be
	 *            notified
	 * @param event
	 *            which will be supplied to the listener when it is notified
	 * 
	 * @exception IllegalArgumentException
	 *                if time period is not positive priority is not between
	 *                Thread.MIN_PRIORITY and Thread.MAX_PRIORITY
	 * @deprecated
	 */
	public void notifyAfterMillis(TimerListener listener, int priority, long timePeriod, int event) throws IllegalArgumentException;

	/**
	 * Adds new TimerListener to the timer event quueue. The listener will be
	 * notified after the given <code>timePeriod</code> with specified
	 * <code>event</code>. If the event queue already contains a listener and
	 * event pair equal to those being passed, then the old notification object
	 * is removed from queue and the new data takes its place.
	 * 
	 * @param listener
	 *            the listener which will be notified after the given time
	 *            period
	 * @param timePeriod
	 *            time period in seconds after which the listener will be
	 *            notified
	 * @param event
	 *            which will be supplied to the listener when it is notified
	 * 
	 * @exception IllegalArgumentException
	 *                if time period is not positive
	 * @deprecated
	 */
	public void notifyAfter(TimerListener listener, int timePeriod, int event) throws IllegalArgumentException;

	/**
	 * Adds new TimerListener to the timer event quueue. The listener will be
	 * notified after the given <code>timePeriod</code> with specified
	 * <code>event</code>. If the event queue already contains a listener and
	 * event pair equal to those being passed, then the old notification object
	 * is removed from queue and the new data takes its place.
	 * 
	 * @param listener
	 *            the listener which will be notified after the given time
	 *            period
	 * @param priority
	 *            priority of executing thread
	 * @param timePeriod
	 *            time period in seconds after which the listener will be
	 *            notified
	 * @param event
	 *            which will be supplied to the listener when it is notified
	 * 
	 * @exception IllegalArgumentException
	 *                if time period is not positive or priority is not between
	 *                Thread.MIN_PRIORITY and Thread.MAX_PRIORITY
	 * @deprecated
	 */
	public void notifyAfter(TimerListener listener, int priority, int timePeriod, int event) throws IllegalArgumentException;

	/**
	 * Adds new TimerListener to the timer event quueue. The listener will be
	 * notified after the given <code>timePeriod</code> with specified
	 * <code>event</code>. If the event queue already contains a listener and
	 * event pair equal to those being passed, then the old notification object
	 * is removed from queue and the new data takes its place.
	 * 
	 * @param listener
	 *            the listener which will be notified after the given time
	 *            period
	 * @param priority
	 *            priority of executing thread
	 * @param timerType
	 *            the type of the timer "Periodical", "One shot", "Periodical No
	 *            Delay", or "One shot no delay"
	 * @param timePeriod
	 *            time period in seconds after which the listener will be
	 *            notified
	 * @param event
	 *            which will be supplied to the listener when it is notified
	 * 
	 * @exception IllegalArgumentException
	 *                if time period is not positive or priority is not between
	 *                Thread.MIN_PRIORITY and Thread.MAX_PRIORITY or the
	 *                timerType is not a correct timer type or the listener is
	 *                null
	 */
	public void addNotifyListener(TimerListener listener, int priority, int timerType, long periodMilis, int event);

	/**
	 * Removes the TimerListener-event pair from the queue, so that the listener
	 * should not be notified after the time period passes.
	 * 
	 * @param listener
	 *            to be removed.
	 * @param event
	 *            for which the timer listener should have been notified.
	 * 
	 */
	public void removeListener(TimerListener listener, int event);
}
