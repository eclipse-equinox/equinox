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
import org.eclipse.osgi.container.ModuleResolverHookFactory;
import org.eclipse.osgi.container.ResolutionReportBuilder;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.wiring.*;

public class DummyResolverHookFactory implements ModuleResolverHookFactory {
	private int writeCount;

	@Override
	public ResolverHook begin(Collection<BundleRevision> triggers, ResolutionReportBuilder builder) {
		writeCount++;
		return new ResolverHook() {
			@Override
			public void filterResolvable(Collection<BundleRevision> candidates) {
				// TODO Auto-generated method stub

			}

			@Override
			public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
				// TODO Auto-generated method stub

			}

			@Override
			public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
				// TODO Auto-generated method stub

			}

			@Override
			public void end() {
				// TODO Auto-generated method stub

			}
		};
	}

	public int getWriteCount() {
		return writeCount;
	}
}
