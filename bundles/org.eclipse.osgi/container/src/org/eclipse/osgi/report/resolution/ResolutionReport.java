/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.report.resolution;

import java.util.List;
import java.util.Map;
import org.osgi.resource.Resource;

/**
 * @since 3.10
 */
public interface ResolutionReport {
	public interface Entry {
		enum Type {
			/**
			 * Indicates a resource failed to resolve because a resolver hook
			 * filtered it out. The structure of the data is <code>null</code>.
			 */
			// TODO This could possibly be improved by adding a reference to the
			// hook that filtered it out.
			FILTERED_BY_RESOLVER_HOOK,
			/**
			 * Indicates a resource failed to resolve because no capabilities
			 * matching one of the resource's requirements could not be found.
			 * The structure of the data is <code>Capability</code>, which
			 * represents the missing capability.
			 */
			// TODO The data structure should include the requirement as well.
			MISSING_CAPABILITY,
			/**
			 * Indicates a resource failed to resolve because (1) it's a
			 * singleton, (2) there was at least one collision, and (3) it was
			 * not the selected singleton. The structure of the data is <code>
			 * Resource</code>, which represents the singleton with which the
			 * resource collided.
			 */
			// TODO Should the data present all of the colliding singletons
			// rather than multiple entries each containing a single collision?
			SINGLETON_SELECTION,
			/**
			 * Indicates a resource failed to resolve because one or more
			 * providers of capabilities matching the resource's requirements
			 * were not resolved. The structure of the data is <code>
			 * Map&lt;Requirement, Set&lt;Capability&gt;&gt;</code>.
			 */
			UNRESOLVED_PROVIDER
		}

		// TODO Can this make use of generics? Or should this be Map<String, Object>
		// and each enum would define the key constants?
		Object getData();

		Type getType();
	}

	public interface Listener {
		void handleResolutionReport(ResolutionReport report);
	}

	Map<Resource, List<Entry>> getEntries();
}
