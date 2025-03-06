/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
		if (coordination == null) {
			throw new NullPointerException();
		}
		this.coordination = coordination;
	}

	@Override
	public long getId() {
		return coordination.getId();
	}

	@Override
	public String getName() {
		return coordination.getName();
	}

	@Override
	public void end() {
		coordination.end();
	}

	@Override
	public boolean fail(Throwable cause) {
		return coordination.fail(cause);
	}

	@Override
	public Throwable getFailure() {
		return coordination.getFailure();
	}

	@Override
	public boolean isTerminated() {
		return coordination.isTerminated();
	}

	@Override
	public void addParticipant(Participant participant) {
		coordination.addParticipant(participant);
	}

	@Override
	public List<Participant> getParticipants() {
		return coordination.getParticipants();
	}

	@Override
	public Map<Class<?>, Object> getVariables() {
		return coordination.getVariables();
	}

	@Override
	public long extendTimeout(long timeMillis) {
		return coordination.extendTimeout(timeMillis);
	}

	@Override
	public void join(long timeMillis) throws InterruptedException {
		coordination.join(timeMillis);
	}

	@Override
	public Coordination push() {
		return coordination.push();
	}

	@Override
	public Thread getThread() {
		return coordination.getThread();
	}

	@Override
	public Bundle getBundle() {
		return coordination.getBundle();
	}

	@Override
	public Coordination getEnclosingCoordination() {
		return coordination.getEnclosingCoordination();
	}

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (!(object instanceof CoordinationReferent)) {
			return false;
		}
		return coordination.equals(((CoordinationReferent) object).coordination);
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
