/*******************************************************************************
 * Copyright (c) 1997-2009 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds;

import org.eclipse.equinox.internal.util.event.Queue;
import org.eclipse.equinox.internal.util.ref.TimerRef;
import org.eclipse.equinox.internal.util.timer.TimerListener;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.cm.ConfigurationEvent;

/**
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 */

public class WorkThread implements Runnable, TimerListener {

	boolean processBundle = false;

	public static int IDLE_TIMEOUT = 1000;
	public static int BLOCK_TIMEOUT = 30000;
	private SCRManager mgr;
	private Object objectToProcess;
	boolean running = true;
	Thread processingThread;

	int waiting = 0;

	public WorkThread(SCRManager mgr) {
		this.mgr = mgr;
	}

	/**
	 * While the event queue has elements - they are processed, i.e.
	 * ManagedService(Factories) are informed for the event.
	 */
	public void run() {
		processingThread = Thread.currentThread();
		do {
			try {
				Queue queue = mgr.queue;
				synchronized (queue) {
					if (mgr.stopped) {
						mgr.running = false;
						break;
					}
					if (Activator.DEBUG) {
						Activator.log.debug("WorkThread.run()", null); //$NON-NLS-1$
					}
					if (queue.size() == 0) { // wait for more events
						try {
							waiting++;
							queue.wait(IDLE_TIMEOUT);
						} catch (Exception ignore) {
							//ignore
						}
						waiting--;
						if (mgr.stopped || queue.size() == 0) {
							mgr.running = false;
							break;
						}
					}
					objectToProcess = queue.get();

					if (objectToProcess != null) {
						if (Activator.DEBUG) {
							Activator.log.debug("WorkThread.run(): object to process " + objectToProcess.toString(), null); //$NON-NLS-1$
						}
					} else {
						continue;
					}
				}
				if (TimerRef.timer != null) {
					TimerRef.notifyAfter(this, BLOCK_TIMEOUT, 1);
				} else {
					if (Activator.DEBUG) {
						Activator.log.debug(Messages.TIMER_SERVICE_UNAVAILABLE, null);
					}
				}
				if (objectToProcess instanceof SCRManager.QueuedJob) {
					((SCRManager.QueuedJob) objectToProcess).dispatch();
				} else if (objectToProcess instanceof ConfigurationEvent) {
					mgr.processConfigurationEvent((ConfigurationEvent) objectToProcess);
				}
			} catch (Throwable t) {
				// just for any case. Must not happen in order to keep thread alive
				Activator.log.error(Messages.UNEXPECTED_EXCEPTION, t);
			} finally {
				TimerRef.removeListener(this, 1);
			}
		} while (running);
		objectToProcess = null;
		processingThread = null;
	}

	public void timer(int event) {
		Activator.log.warning(NLS.bind(Messages.TIMEOUT_PROCESSING, objectToProcess), null);
		running = false;
		objectToProcess = null;
		mgr.queueBlocked();
	}

}
