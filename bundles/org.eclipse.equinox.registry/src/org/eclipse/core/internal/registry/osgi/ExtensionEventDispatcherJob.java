/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package org.eclipse.core.internal.registry.osgi;

import java.util.Map;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.spi.RegistryStrategy;

/**
 * Use Eclipse job scheduling mechanism.
 */
final public class ExtensionEventDispatcherJob extends Job {
	// an "identy rule" that forces extension events to be queued
	private final static ISchedulingRule EXTENSION_EVENT_RULE = new ISchedulingRule() {
		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return rule == this;
		}
	};
	private final Map<String, ?> deltas;
	private final Object[] listenerInfos;
	private final Object registry;

	public ExtensionEventDispatcherJob(Object[] listenerInfos, Map<String, ?> deltas, Object registry) {
		// name not NL'd since it is a system job
		super("Registry event dispatcher"); //$NON-NLS-1$
		setSystem(true);
		this.listenerInfos = listenerInfos;
		this.deltas = deltas;
		this.registry = registry;
		// all extension event dispatching jobs use this rule
		setRule(EXTENSION_EVENT_RULE);
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		return RegistryStrategy.processChangeEvent(listenerInfos, deltas, registry);
	}
}
