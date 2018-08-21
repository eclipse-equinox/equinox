/*******************************************************************************
 * Copyright (c) 1997, 2018 by ProSyst Software GmbH
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

package org.eclipse.equinox.internal.util.impl.tpt.threadpool;

import java.security.AccessControlContext;
import org.eclipse.equinox.internal.util.pool.ObjectCreator;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

class Job implements ObjectCreator {

	public Runnable run;

	public String name;

	public String context;

	public int priority = -1;
	public ThreadPoolFactoryImpl factory;

	AccessControlContext acc;

	private void setJob(Runnable run, String name, int priority, ThreadPoolFactoryImpl factory, AccessControlContext acc) {
		this.run = run;
		this.name = name;
		this.priority = priority;
		this.factory = factory;
		this.acc = acc;
	}

	@Override
	public Object getInstance() {
		return new Job();
	}

	public Job root = null;

	public Job last = null;

	private Job next;

	int counter = 0;

	public Job addJob(Runnable run, String name, int priority, ThreadPoolFactoryImpl factory, AccessControlContext acc) {
		Job tmp = (Job) (ThreadPoolManagerImpl.jobPool.getObject());
		counter++;
		tmp.setJob(run, name, priority, factory, acc);
		if (root == null) {
			root = tmp;
			last = tmp;
			return tmp;
		}
		last.next = tmp;
		last = tmp;
		return tmp;
	}

	public void addJob(Job j) {
		counter++;
		if (root == null) {
			root = j;
			last = j;
			return;
		}
		last.next = j;
		last = j;
	}

	private void clear() {
		next = null;
	}

	public void fullClear() {
		next = null;
		run = null;
		name = null;
		context = null;
		acc = null;
	}

	public Job getJob() {
		Job r = null;
		if (root == null) {
			return null;
		}
		counter--;
		r = root;
		root = root.next;
		if (root == null)
			last = root;
		r.clear();
		return r;
	}
}
