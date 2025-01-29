/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.compatibility.state;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.osgi.container.*;
import org.eclipse.osgi.internal.resolver.BaseDescriptionImpl.BaseCapability;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.*;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.*;

class PlatformBundleListener implements SynchronousBundleListener, FrameworkListener, ResolverHookFactory {

	private final State systemState;
	private final StateConverter converter;
	private final ModuleDatabase database;
	private final ModuleContainer container;
	private long lastResolveStamp = -1;
	private final AtomicBoolean gotUnresolved = new AtomicBoolean(false);

	PlatformBundleListener(State systemState, StateConverter converter, ModuleDatabase database, ModuleContainer container) {
		this.systemState = systemState;
		this.converter = converter;
		this.database = database;
		this.container = container;
	}

	@Override
	public void bundleChanged(BundleEvent event) {
		switch (event.getType()) {
			case BundleEvent.INSTALLED : {
				BundleRevision revision = event.getBundle().adapt(BundleRevision.class);
				if (revision != null) {
					systemState.addBundle(converter.createDescription(revision));
					systemState.setTimeStamp(database.getRevisionsTimestamp());
				}
				break;
			}
			case BundleEvent.UNINSTALLED : {
				systemState.removeBundle(event.getBundle().getBundleId());
				systemState.setTimeStamp(database.getRevisionsTimestamp());
				break;
			}
			case BundleEvent.UPDATED : {
				BundleRevision revision = event.getBundle().adapt(BundleRevision.class);
				if (revision != null) {
					systemState.updateBundle(converter.createDescription(revision));
					systemState.setTimeStamp(database.getRevisionsTimestamp());
				}
				break;
			}
			case BundleEvent.UNRESOLVED : {
				gotUnresolved.set(true);
				break;
			}
			case BundleEvent.RESOLVED : {
				resolve(gotUnresolved.getAndSet(false));
				break;
			}
			default :
				// do nothing
				break;
		}
	}

	private void resolve(boolean uninstalled) {
		database.readLock();
		try {
			if (lastResolveStamp != database.getRevisionsTimestamp()) {
				Collection<ModuleRevision> containerRemovalPending = container.getRemovalPending();
				BundleDescription[] stateRemovalPendingDescs = systemState.getRemovalPending();
				Collection<BundleDescription> stateRemovalPending = new ArrayList<>(stateRemovalPendingDescs.length);
				for (BundleDescription description : stateRemovalPendingDescs) {
					if (!containerRemovalPending.contains(description.getUserObject())) {
						stateRemovalPending.add(description);
					}
				}
				if (!stateRemovalPending.isEmpty()) {
					systemState.resolve(stateRemovalPending.toArray(new BundleDescription[stateRemovalPending.size()]), true);
				} else {
					systemState.resolve(!uninstalled);
				}
				lastResolveStamp = database.getRevisionsTimestamp();
				systemState.setTimeStamp(database.getRevisionsTimestamp());
			}
		} finally {
			database.readUnlock();
		}
	}

	@Override
	public void frameworkEvent(FrameworkEvent event) {
		if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
			resolve(gotUnresolved.getAndSet(false));
		}
	}

	@Override
	public ResolverHook begin(Collection<BundleRevision> triggers) {
		return new ResolverHook() {

			@Override
			public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
				BundleDescription desc = (BundleDescription) singleton.getRevision();
				ModuleRevision revision = (ModuleRevision) desc.getUserObject();
				if (revision.getWiring() != null) {
					collisionCandidates.clear();
				} else {
					for (Iterator<BundleCapability> iCandidates = collisionCandidates.iterator(); iCandidates.hasNext();) {
						BundleDescription candDesc = (BundleDescription) iCandidates.next().getRevision();
						ModuleRevision candRevision = (ModuleRevision) candDesc.getUserObject();
						if (candRevision.getWiring() == null) {
							iCandidates.remove();
						}
					}
				}
			}

			@Override
			public void filterResolvable(Collection<BundleRevision> candidates) {
				for (Iterator<BundleRevision> iCandidates = candidates.iterator(); iCandidates.hasNext();) {
					BundleDescription candDesc = (BundleDescription) iCandidates.next();
					ModuleRevision candRevision = (ModuleRevision) candDesc.getUserObject();
					if (candRevision.getWiring() == null) {
						iCandidates.remove();
					}
				}
			}

			@Override
			public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
				String namespace = requirement.getNamespace();
				BundleDescription reqDesc = (BundleDescription) requirement.getRevision();
				ModuleRevision reqRevision = (ModuleRevision) reqDesc.getUserObject();
				ModuleWiring reqWiring = reqRevision.getWiring();
				if (reqWiring == null) {
					candidates.clear();
					return;
				}
				Collection<ModuleWiring> wirings = new ArrayList<>(1);
				if ((reqRevision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
					if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE.equals(namespace) || HostNamespace.HOST_NAMESPACE.equals(namespace)) {
						wirings.add(reqWiring);
					} else {
						List<ModuleWire> hostWires = reqWiring.getRequiredModuleWires(namespace);
						for (ModuleWire hostWire : hostWires) {
							ModuleWiring hostWiring = hostWire.getProviderWiring();
							if (hostWiring != null) {
								wirings.add(hostWiring);
							}
						}
					}
				} else {
					wirings.add(reqWiring);
				}
				for (Iterator<BundleCapability> iCandidates = candidates.iterator(); iCandidates.hasNext();) {
					BaseCapability baseCapability = (BaseCapability) iCandidates.next();
					Object userObject = baseCapability.getBaseDescription().getUserObject();
					boolean foundCandidate = false;
					wirings: for (ModuleWiring wiring : wirings) {
						List<ModuleWire> wires = wiring.getRequiredModuleWires(namespace);
						for (ModuleWire wire : wires) {
							if (userObject instanceof ModuleRevision && userObject.equals(wire.getProvider())) {
								foundCandidate = true;
							} else if (userObject instanceof ModuleCapability && userObject.equals(wire.getCapability())) {
								foundCandidate = true;
							}
							if (foundCandidate)
								break wirings;
						}
					}
					if (!foundCandidate) {
						iCandidates.remove();
					}
				}
			}

			@Override
			public void end() {
				// nothing
			}
		};
	}

}
