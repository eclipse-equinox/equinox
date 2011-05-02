/*******************************************************************************
 * Copyright (c) 2006, 2009 Cognos Incorporated, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.equinox.log.internal;

import java.util.LinkedList;

/**
 * SerializedTaskQueue is a utility class that will allow asynchronous but serialized execution of tasks
 */
public class SerializedTaskQueue {

	private static final int MAX_WAIT = 5000;
	private final LinkedList tasks = new LinkedList();
	private Thread thread;
	private final String queueName;

	public SerializedTaskQueue(String queueName) {
		this.queueName = queueName;
	}

	public synchronized void put(Runnable newTask) {
		tasks.add(newTask);
		if (thread == null) {
			thread = new Thread(queueName) {
				public void run() {
					Runnable task = nextTask(MAX_WAIT);
					while (task != null) {
						task.run();
						task = nextTask(MAX_WAIT);
					}
				}
			};
			thread.start();
		} else
			notify();
	}

	synchronized Runnable nextTask(int maxWait) {
		if (tasks.isEmpty()) {
			try {
				wait(maxWait);
			} catch (InterruptedException e) {
				// ignore -- we control the stack here and do not need to propagate it.
			}

			if (tasks.isEmpty()) {
				thread = null;
				return null;
			}
		}
		return (Runnable) tasks.removeFirst();
	}
}
