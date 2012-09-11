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
package org.eclipse.osgi.framework.report;

import java.util.List;
import java.util.Map;
import org.osgi.resource.Resource;

/**
 * @since 3.10
 */
public interface ResolutionReport {
	public interface Entry {
		enum Type {
			FILTERED_BY_HOOK, SINGLETON
		}

		Type getType();
	}

	public interface Listener {
		void handleResolutionReport(ResolutionReport report);
	}

	Map<Resource, List<Entry>> getEntries();
}
