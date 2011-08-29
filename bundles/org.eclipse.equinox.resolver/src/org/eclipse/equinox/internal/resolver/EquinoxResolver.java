/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.resolver;

import java.util.*;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.osgi.framework.resource.Resource;
import org.osgi.framework.resource.Wire;
import org.osgi.service.resolver.*;

public class EquinoxResolver implements Resolver {
	private final StateObjectFactory factory;

	public EquinoxResolver(StateObjectFactory factory) {
		this.factory = factory;
	}

	public Map<Resource, List<Wire>> resolve(Environment environment, Collection<? extends Resource> mandatoryResources, Collection<? extends Resource> optionalResources) throws ResolutionException {
		State state = factory.createState(true);
		EquinoxResolverHook resolverHook = new EquinoxResolverHook(state, environment);
		state.setResolverHookFactory(resolverHook);
		return resolverHook.resolve(mandatoryResources, optionalResources);
	}
}
