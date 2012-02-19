/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
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

import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.CoordinationPermission;
import org.osgi.service.coordinator.Participant;
import org.osgi.service.log.LogService;

public class CoordinationImpl {
	// Holds a strong reference to the CoordinationWeakReference object associated
	// with this CoordinationImpl. Serves no other purpose. Needs no guarding.
	CoordinationWeakReference reference;

	private volatile Throwable failure;
	private volatile boolean terminated;

	private Date deadline;
	private CoordinationImpl enclosingCoordination;
	private Thread thread;
	private long totalTimeout;
	private TimerTask timerTask;

	private final CoordinatorImpl coordinator;
	private final long id;
	private final String name;
	private final List<Participant> participants;
	// Store a referent to be used by clients other than the initiator. It must
	// not be a reference to the referent returned to the initiator.
	private final CoordinationReferent referent;
	private final Map<Class<?>, Object> variables;

	public CoordinationImpl(long id, String name, long timeout, CoordinatorImpl coordinator) {
		validateName(name);
		validateTimeout(timeout);
		this.id = id;
		this.name = name;
		totalTimeout = timeout;
		this.coordinator = coordinator;
		participants = Collections.synchronizedList(new ArrayList<Participant>());
		variables = new HashMap<Class<?>, Object>();
		// Not an escaping 'this' reference. It will not escape the thread calling the constructor.
		referent = new CoordinationReferent(this);
	}

	public void addParticipant(Participant participant) throws CoordinationException {
		// This method requires the PARTICIPATE permission.
		coordinator.checkPermission(CoordinationPermission.PARTICIPATE, name);
		if (participant == null)
			throw new NullPointerException(NLS.bind(Messages.NullParameter, "participant")); //$NON-NLS-1$
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
					Thread t = coordination.getThread();
					// If thread is null, the coordination is not associated with
					// any thread, and there's nothing to compare. If the coordination
					// is using this thread, then we can't block due to risk of deadlock.
					if (t == Thread.currentThread()) {
						throw new CoordinationException(Messages.Deadlock, referent, CoordinationException.DEADLOCK_DETECTED);
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
				coordinator.getLogService().log(LogService.LOG_DEBUG, Messages.LockInterrupted, e);
				// This thread was interrupted while waiting for the coordination
				// to terminate.
				throw new CoordinationException(Messages.LockInterrupted, referent, CoordinationException.LOCK_INTERRUPTED, e);
			}
		}
	}

	public void end() throws CoordinationException {
		coordinator.checkPermission(CoordinationPermission.INITIATE, name);
		// Terminating the coordination must be atomic.
		synchronized (this) {
			// If this coordination is associated with a thread, an additional
			// check is required.
			if (thread != null) {
				// Coordinations may only be ended by the same thread that
				// pushed them onto the stack, if any.
				if (thread != Thread.currentThread()) {
					throw new CoordinationException(Messages.EndingThreadNotSame, referent, CoordinationException.WRONG_THREAD);
				}
				// Unwind the stack in case there are other coordinations higher
				// up than this one.
				while (!coordinator.peek().equals(referent)) {
					try {
						coordinator.peek().end();
					} catch (CoordinationException e) {
						coordinator.peek().fail(e);
					}
				}
				// A coordination is removed from the thread local stack only when being ended.
				// This must occur even if the coordination is already terminated due to a
				// failure.
				coordinator.pop();
			}
			terminate();
		}
		// Notify participants this coordination has ended. Track whether or
		// not a partial ending has occurred.
		Exception exception = null;
		// No additional synchronization is needed here because the participant
		// list will not be modified post termination.
		List<Participant> participantsToNotify = new ArrayList<Participant>(this.participants);
		Collections.reverse(participantsToNotify);
		for (Participant participant : participantsToNotify) {
			try {
				participant.ended(referent);
			} catch (Exception e) {
				coordinator.getLogService().log(LogService.LOG_WARNING, Messages.ParticipantEndedError, e);
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
			throw new CoordinationException(Messages.CoordinationPartiallyEnded, referent, CoordinationException.PARTIALLY_ENDED, exception);
		}
	}

	public long extendTimeout(long timeInMillis) throws CoordinationException {
		coordinator.checkPermission(CoordinationPermission.PARTICIPATE, name);
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
			long maxTimeout = coordinator.getMaxTimeout();
			long newTotalTimeout = totalTimeout + timeInMillis;
			// If there is no maximum timeout, there's no need to track the total timeout.
			if (maxTimeout != 0) {
				// If the max timeout has already been reached, return 0 indicating that no
				// extension has taken place.
				if (totalTimeout == maxTimeout)
					return 0;
				// If the extension would exceed the maximum timeout, add as much time
				// as possible.
				else if (newTotalTimeout > maxTimeout) {
					totalTimeout = maxTimeout;
					// Adjust the requested extension amount with the allowable amount.
					timeInMillis = newTotalTimeout - maxTimeout;
				}
				// Otherwise, accept the full extension.
				else
					totalTimeout = newTotalTimeout;
			}
			// Cancel the current timeout.
			boolean cancelled = timerTask.cancel();
			if (!cancelled) {
				// This means the previous task has run and is waiting to get a lock on
				// this coordination. We can't throw an exception yet because we can't
				// know which one to use (ALREADY_ENDED or FAILED). Once the lock is
				// released, the running task may fail this coordination due to a timeout,
				// or something else might be waiting to fail this coordination for other
				// reasons or to end it. We simply don't know who will win the race.
				try {
					// Wait until this coordination terminates.
					join(0);
					// Now determine how it terminated and throw the appropriate exception.
					checkTerminated();
				}
				catch (InterruptedException e) {
					throw new CoordinationException(Messages.InterruptedTimeoutExtension, referent, CoordinationException.UNKNOWN, e);
				}
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
		coordinator.checkPermission(CoordinationPermission.PARTICIPATE, name);
		// The reason must not be null.
		if (reason == null)
			throw new NullPointerException(Messages.MissingFailureCause);
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
		List<Participant> participantsToNotify = new ArrayList<Participant>(this.participants);
		Collections.reverse(participantsToNotify);
		for (Participant participant : participantsToNotify) {
			try {
				participant.failed(referent);
			} catch (Exception e) {
				coordinator.getLogService().log(LogService.LOG_WARNING, Messages.ParticipantFailedError, e);
			}
		}
		synchronized (this) {
			// Notify everything joined to this coordination that it has finished.
			notifyAll();
		}
		// Return true to indicate this call resulted in the coordination's failure.
		return true;
	}

	public Bundle getBundle() {
		coordinator.checkPermission(CoordinationPermission.ADMIN, name);
		return coordinator.getBundle();
	}

	public synchronized Coordination getEnclosingCoordination() {
		coordinator.checkPermission(CoordinationPermission.ADMIN, name);
		if (enclosingCoordination == null)
			return null;
		return enclosingCoordination.getReferent();
	}

	public Throwable getFailure() {
		coordinator.checkPermission(CoordinationPermission.INITIATE, name);
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
		coordinator.checkPermission(CoordinationPermission.INITIATE, name);
		// Return a mutable snapshot.
		synchronized (participants) {
			return new ArrayList<Participant>(participants);
		}
	}

	public synchronized Thread getThread() {
		coordinator.checkPermission(CoordinationPermission.ADMIN, name);
		return thread;
	}

	public Map<Class<?>, Object> getVariables() {
		coordinator.checkPermission(CoordinationPermission.PARTICIPATE, name);
		return variables;
	}

	public boolean isTerminated() {
		return terminated;
	}

	public void join(final long timeInMillis) throws InterruptedException {
		coordinator.checkPermission(CoordinationPermission.PARTICIPATE, name);
		validateTimeout(timeInMillis);
		// How much system time has elapsed across all waits.
		long elapsed = 0;
		// The system time at the start of the wait.
		long start = System.currentTimeMillis();
		// Wait until this coordination has terminated. Guard against spurious
		// wakeups using the termination status.
		synchronized (this) {
			while (!terminated) {
				// Wait for the desired amount of time minus any time that has already elapsed.
				wait(timeInMillis - elapsed);
				// Only track elapsed time if a definite interval was specified.
				if (timeInMillis != 0) {
					// Update the elapsed time.
					elapsed = System.currentTimeMillis() - start;
					// If the allotted wait time has fully expired, we're done.
					if (elapsed >= timeInMillis) // Don't allow a wait of zero here!
						break;
				}
			}
		}
	}

	public Coordination push() throws CoordinationException {
		coordinator.checkPermission(CoordinationPermission.INITIATE, name);
		synchronized (this) {
			checkTerminated();
			coordinator.push(this);
		}
		return referent;
	}

	LogService getLogService() {
		return coordinator.getLogService();
	}

	// Return the referent to be used by clients other than the initiator.
	CoordinationReferent getReferent() {
		return referent;
	}

	synchronized void setTimerTask(TimerTask timerTask) {
		this.timerTask = timerTask;
		deadline = new Date(System.currentTimeMillis() + totalTimeout);
		coordinator.schedule(timerTask, deadline);
	}

	synchronized void setThreadAndEnclosingCoordination(Thread t, CoordinationImpl c) {
		thread = t;
		enclosingCoordination = c;
	}

	private void checkTerminated() throws CoordinationException {
		// If this coordination is not terminated, simply return.
		if (!terminated)
			return;
		// The coordination has terminated. Figure out which type of exception
		// must be thrown.
		if (failure != null) {
			// The fail() method was called indicating the coordination failed.
			throw new CoordinationException(Messages.CoordinationFailed, referent, CoordinationException.FAILED, failure);
		}
		// The coordination did not fail, so it either partially ended or
		// ended successfully.
		throw new CoordinationException(Messages.CoordinationEnded, referent, CoordinationException.ALREADY_ENDED);
	}

	private void terminate() throws CoordinationException {
		checkTerminated();
		terminated = true;
		// Cancel the timeout. Purge the task if it was, in fact, canceled.
		if (timerTask != null && timerTask.cancel()) {
			coordinator.purge();
		}
		coordinator.terminate(this, participants);
	}

	private static void validateName(String name) {
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
			throw new IllegalArgumentException(Messages.InvalidCoordinationName);
	}

	private static void validateTimeout(long timeout) {
		if (timeout < 0)
			throw new IllegalArgumentException(Messages.InvalidTimeInterval);
	}
}
