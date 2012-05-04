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
package org.eclipse.osgi.container.tests.dummys;

import java.util.Collection;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.*;

public class DummyResolverHookFactory implements ResolverHookFactory {

	@Override
	public ResolverHook begin(Collection<BundleRevision> triggers) {
		return new ResolverHook() {

			@Override
			public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
				// nothing
			}

			@Override
			public void filterResolvable(Collection<BundleRevision> candidates) {
				// nothing
			}

			@Override
			public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
				// nothing
			}

			@Override
			public void end() {
				// nothing
			}
		};
	}

}
