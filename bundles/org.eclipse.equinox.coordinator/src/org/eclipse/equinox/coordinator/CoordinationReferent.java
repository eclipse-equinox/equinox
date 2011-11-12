/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.coordinator;

import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Participant;

public class CoordinationReferent implements Coordination {
	private final CoordinationImpl coordination;

	public CoordinationReferent(CoordinationImpl coordination) {
		if (coordination == null)
			throw new NullPointerException();
		this.coordination = coordination;
	}

	public long getId() {
		return coordination.getId();
	}

	public String getName() {
		return coordination.getName();
	}

	public void end() {
		coordination.end();
	}

	public boolean fail(Throwable cause) {
		return coordination.fail(cause);
	}

	public Throwable getFailure() {
		return coordination.getFailure();
	}

	public boolean isTerminated() {
		return coordination.isTerminated();
	}

	public void addParticipant(Participant participant) {
		coordination.addParticipant(participant);
	}

	public List<Participant> getParticipants() {
		return coordination.getParticipants();
	}

	public Map<Class<?>, Object> getVariables() {
		return coordination.getVariables();
	}

	public long extendTimeout(long timeMillis) {
		return coordination.extendTimeout(timeMillis);
	}

	public void join(long timeMillis) throws InterruptedException {
		coordination.join(timeMillis);
	}

	public Coordination push() {
		return coordination.push();
	}

	public Thread getThread() {
		return coordination.getThread();
	}

	public Bundle getBundle() {
		return coordination.getBundle();
	}

	public Coordination getEnclosingCoordination() {
		return coordination.getEnclosingCoordination();
	}

	@Override
	public boolean equals(Object object) {
		if (object == this)
			return true;
		if (!(object instanceof CoordinationReferent))
			return false;
		return coordination.equals(((CoordinationReferent)object).coordination);
	}

	@Override
	public int hashCode() {
		return coordination.hashCode();
	}

	@Override
	public String toString() {
		return coordination.toString();
	}
}
