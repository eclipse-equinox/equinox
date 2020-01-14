/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package org.eclipse.osgi.tests.container.dummys;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class UnresolvedProviderEntryBuilder {
	private final Map<Requirement, List<Capability>> data = new HashMap<>();

	private Requirement lastRequirement;

	public Map<Requirement, List<Capability>> build() {
		return new HashMap<>(data);
	}

	public UnresolvedProviderEntryBuilder capability(Capability value) {
		data.get(lastRequirement).add(value);
		return this;
	}

	public UnresolvedProviderEntryBuilder requirement(Requirement key) {
		data.put(key, new ArrayList<Capability>());
		lastRequirement = key;
		return this;
	}
}
