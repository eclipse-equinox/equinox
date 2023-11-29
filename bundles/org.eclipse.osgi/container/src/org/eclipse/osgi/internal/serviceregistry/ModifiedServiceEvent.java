/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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

package org.eclipse.osgi.internal.serviceregistry;

import java.util.Map;
import org.osgi.framework.*;

/**
 * ServiceEvent subtype for MODIFIED_ENDMATCH computation.
 */
class ModifiedServiceEvent extends ServiceEvent {
	private static final long serialVersionUID = -5373850978543026102L;
	private final ServiceEvent modified;
	private final ServiceEvent modifiedEndMatch;
	private final Map<String, Object> previousProperties;

	/**
	 * Create a ServiceEvent containing the service properties prior to modification.
	 *
	 * @param reference Reference to service with modified properties.
	 * @param previousProperties Service properties prior to modification.
	 */
	ModifiedServiceEvent(ServiceReference<?> reference, Map<String, Object> previousProperties) {
		super(ServiceEvent.MODIFIED, reference);
		this.modified = new ServiceEvent(ServiceEvent.MODIFIED, reference);
		this.modifiedEndMatch = new ServiceEvent(ServiceEvent.MODIFIED_ENDMATCH, reference);
		this.previousProperties = previousProperties;
	}

	/**
	 * Return the service event of type MODIFIED.
	 *
	 * @return The service event of type MODIFIED.
	 */
	ServiceEvent getModifiedEvent() {
		return modified;
	}

	/**
	 * Return the service event of type MODIFIED_ENDMATCH.
	 *
	 * @return The service event of type MODIFIED_ENDMATCH.
	 */
	ServiceEvent getModifiedEndMatchEvent() {
		return modifiedEndMatch;
	}

	/**
	 * Return if the specified filter matches the previous service
	 * properties.
	 *
	 * @param filter The filer to evaluate using the previous service
	 * properties.
	 * @return True is the filter matches the previous service properties.
	 */
	boolean matchPreviousProperties(Filter filter) {
		/* We use matches here since ServiceProperties already
		 * does case insensitive lookup.
		 */
		return filter.matches(previousProperties);
	}
}
