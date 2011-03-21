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

import java.util.Collection;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;

public class CoordinatorComponent implements Coordinator {
	private volatile Coordinator coordinator;

	public boolean addParticipant(Participant participant) throws CoordinationException {
		return coordinator.addParticipant(participant);
	}

	public Coordination begin(String name, long timeout) {
		return coordinator.begin(name, timeout);
	}

	public Coordination create(String name, long timeout) {
		return coordinator.create(name, timeout);
	}

	public boolean fail(Throwable reason) {
		return coordinator.fail(reason);
	}

	public Coordination getCoordination(long id) {
		return coordinator.getCoordination(id);
	}

	public Collection<Coordination> getCoordinations() {
		return coordinator.getCoordinations();
	}

	public Coordination peek() {
		return coordinator.peek();
	}

	public Coordination pop() {
		return coordinator.pop();
	}

	void activate(ComponentContext componentContext) {
		coordinator = Activator.factory.getService(componentContext.getUsingBundle(), null);
	}

	void deactivate(BundleContext context) {
		((CoordinatorImpl) coordinator).shutdown();
	}
}
