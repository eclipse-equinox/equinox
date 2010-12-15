/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.coordinator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.CoordinationPermission;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.log.LogService;

public class CoordinationImpl implements Coordination {
	private final CoordinatorImpl coordinator;
	private final long id;
	private final String name;
	private final List<Participant> participants;
	private final Map<Class<?>, Object> variables;

	private volatile Date deadline;
	private volatile Throwable failure;
	private volatile boolean terminated;
	private volatile Thread thread;
	private volatile TimerTask timerTask;

	public CoordinationImpl(long id, String name, long timeout, CoordinatorImpl coordinator) {
		validateName(name);
		validateTimeout(timeout);
		this.id = id;
		this.name = name;
		this.deadline = new Date(System.currentTimeMillis() + timeout);
		this.coordinator = coordinator;
		participants = Collections.synchronizedList(new ArrayList<Participant>());
		variables = new HashMap<Class<?>, Object>();
	}

	public void addParticipant(Participant participant) throws CoordinationException {
		// This method requires the PARTICIPATE permission.
		coordinator.checkPermission(CoordinationPermission.PARTICIPATE, name);
		/* The caller has permission. Check to see if the participant is already 
		 * participating in another coordination. Do this in a loop in case the 
		 * participant must wait for the other coordination to finish. The loop 
		 * will exit under the following circumstances.
		 * 
		 * (1) This coordination is terminated.
		 * (2) The participant is already participating in another coordination
		 * using the same thread as this one.
		 * (3) This thread is interrupted.
		 * (4) The participant is not participating in another coordination.
		 */
		while (true) {
			CoordinationImpl coordination;
			synchronized (this) {
				// Check to see if this coordination has already terminated. If so, 
				// throw the appropriate exception.
				checkTerminated();
				coordination = coordinator.addParticipant(participant, this);
				if (coordination == null) {
					// The same participant is not currently participating in 
					// any coordination. Add it to this coordination and break 
					// out of the loop.
					participants.add(participant);
					break;
				} else if (coordination == this) {
					// The same participant is being added twice to this 
					// coordination. Nothing to do.
					break;
				} else {
					// This means the participant is already participating in another
					// coordination. Check to see if it's on the same thread.
					@SuppressWarnings("hiding")
					Thread thread = coordination.getThread();
					// If thread is null, the coordination is not associated with
					// any thread, and there's nothing to compare. If the coordination 
					// is using this thread, then we can't block due to risk of deadlock.
					if (thread == Thread.currentThread()) {
						throw new CoordinationException(Messages.CoordinationImpl_1, CoordinationImpl.this, CoordinationException.DEADLOCK_DETECTED);
					}
				}
			}
			// The participant is already participating in another coordination 
			// that's not using this thread. Block until that coordination has
			// finished. A decision was made here to use a timeout and incur the
			// expense of waking up and rejoining in order to make a reasonably 
			// timely exit if this coordination terminates.
			try {
				coordination.join(1000);
			} catch (InterruptedException e) {
				coordinator.getLogService().log(LogService.LOG_DEBUG, Messages.CoordinationImpl_2, e);
				// This thread was interrupted while waiting for the coordination
				// to terminate.
				throw new CoordinationException(Messages.CoordinationImpl_3, CoordinationImpl.this, CoordinationException.LOCK_INTERRUPTED, e);
			}
		}
	}

	public void end() throws CoordinationException {
		// Terminating the coordination must be atomic.
		terminate();
		// Notify participants this coordination has ended. Track whether or
		// not a partial ending has occurred.
		Exception exception = null;
		// No additional synchronization is needed here because the participant
		// list will not be modified post termination.
		for (Participant participant : participants) {
			try {
				participant.ended(this);
			} catch (Exception e) {
				coordinator.getLogService().log(LogService.LOG_WARNING, Messages.CoordinationImpl_4, e);
				// Only the first exception will be propagated.
				if (exception == null)
					exception = e;
			}
		}
		synchronized (this) {
			// Notify everything joined to this coordination that it has finished.
			notifyAll();
		}
		// If a partial ending has occurred, throw the required exception.
		if (exception != null) {
			throw new CoordinationException(Messages.CoordinationImpl_5, this, CoordinationException.PARTIALLY_ENDED, exception);
		}
	}

	public long extendTimeout(long timeInMillis) throws CoordinationException {
		validateTimeout(timeInMillis);
		// We don't want this coordination to terminate before the new timer is 
		// in place.
		synchronized (this) {
			// Check to see if this coordination has already terminated. If so, 
			// throw the appropriate exception.
			checkTerminated();
			// If there was no previous timeout set, return 0 indicating that no
			// extension has taken place.
			if (timerTask == null)
				return 0;
			// Passing anything less than zero as well as zero itself will return the
			// existing deadline. The deadline will not be null if timerTask is not null.
			if (timeInMillis == 0)
				return deadline.getTime();
			// Cancel the current timeout.
			boolean cancelled = timerTask.cancel();
			if (!cancelled) {
				// This means the previous task has run and is waiting to get a lock on
				// this coordination. We can't throw an exception here because we can't
				// know which one to use (ALREADY_ENDED or FAILED). Once the lock is
				// released, the running task may fail this coordination due to a timeout, 
				// or something else might be waiting to fail this coordination for other
				// reasons or to end it. We simply don't know who will win the race.
				// Return 0 to indicate that no extension has occurred.
				return 0;
			}
			// Create the new timeout.
			timerTask = new CoordinationTimerTask(this);
			// Extend the current deadline.
			deadline = new Date(deadline.getTime() + timeInMillis);
			// Schedule the new timeout.
			coordinator.schedule(timerTask, deadline);
			// Return the new deadline.
			return deadline.getTime();
		}
	}

	public boolean fail(Throwable reason) {
		// The reason must not be null.
		if (reason == null)
			throw new NullPointerException(Messages.CoordinationImpl_11);
		// Terminating the coordination must be atomic.
		synchronized (this) {
			// If this coordination is terminated, return false. Do not throw a
			// CoordinationException as in other methods.
			if (terminated)
				return false;
			// This coordination has not already terminated, so terminate now.
			terminate();
			// Store the reason for the failure.
			failure = reason;
		}
		// Notify participants this coordination has failed.
		// No additional synchronization is needed here because the participant
		// list will not be modified post termination.
		for (Participant participant : participants) {
			try {
				participant.failed(this);
			} catch (Exception e) {
				coordinator.getLogService().log(LogService.LOG_WARNING, Messages.CoordinationImpl_6, e);
			}
		}
		synchronized (this) {
			// Notify everything joined to this coordination that it has finished.
			notifyAll();
		}
		// Return true to indicate this call resulted in the coordination's failure.
		return true;
	}

	public Throwable getFailure() {
		return failure;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<Participant> getParticipants() {
		// This method requires the ADMIN permission.
		coordinator.checkPermission(CoordinationPermission.ADMIN, name);
		// Return a mutable snapshot.
		synchronized (participants) {
			return new ArrayList<Participant>(participants);
		}
	}

	public Thread getThread() {
		return thread;
	}

	public Map<Class<?>, Object> getVariables() {
		return variables;
	}

	public boolean isTerminated() {
		return terminated;
	}

	public void join(long timeInMillis) throws InterruptedException {
		// Treat negative arguments as if they were zero.
		if (timeInMillis < 0)
			timeInMillis = 0;
		long start = System.currentTimeMillis();
		// Wait until this coordination has terminated. Guard against spurious
		// wakeups using isTerminated().
		synchronized (this) {
			while (!terminated) {
				wait(timeInMillis);
				long elapsed = System.currentTimeMillis() - start;
				if (elapsed > timeInMillis)
					break;
			}
		}
	}

	public Coordination push() throws CoordinationException {
		synchronized (this) {
			checkTerminated();
			thread = Thread.currentThread();
			coordinator.push(this);
		}
		return this;
	}

	Date getDeadline() {
		return deadline;
	}

	LogService getLogService() {
		return coordinator.getLogService();
	}

	void pop() {
		thread = null;
	}

	void setTimerTask(TimerTask timerTask) {
		this.timerTask = timerTask;
	}

	private synchronized void checkTerminated() throws CoordinationException {
		// If this coordination is not terminated, simply return.
		if (!terminated)
			return;
		// The coordination has terminated. Figure out which type of exception
		// must be thrown.
		if (failure != null) {
			// The fail() method was called indicating the coordination failed.
			throw new CoordinationException(Messages.CoordinationImpl_7, this, CoordinationException.FAILED, failure);
		}
		// The coordination did not fail, so it either partially ended or 
		// ended successfully.
		throw new CoordinationException(Messages.CoordinationImpl_8, CoordinationImpl.this, CoordinationException.ALREADY_ENDED);
	}

	private synchronized void terminate() throws CoordinationException {
		checkTerminated();
		terminated = true;
		// Cancel the timeout. Purge the task if it was, in fact, canceled.
		if (timerTask != null && timerTask.cancel()) {
			coordinator.purge();
		}
		coordinator.terminate(this, participants);
	}

	private void validateName(@SuppressWarnings("hiding")
	String name) {
		boolean valid = true;
		if (name == null || name.length() == 0)
			valid = false;
		else {
			boolean period = false;
			for (char c : name.toCharArray()) {
				if (Character.isLetterOrDigit(c) || c == '_' || c == '-') {
					period = false;
				} else if (c == '.' && !period) {
					period = true;
				} else {
					valid = false;
					break;
				}
			}
		}
		if (!valid)
			throw new IllegalArgumentException(Messages.CoordinationImpl_10);
	}

	private void validateTimeout(long timeout) {
		if (timeout < 0)
			throw new IllegalArgumentException(Messages.CoordinationImpl_12);
	}
}
