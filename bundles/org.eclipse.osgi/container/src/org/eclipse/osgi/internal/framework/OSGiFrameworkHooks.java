/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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
package org.eclipse.osgi.internal.framework;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.Module.State;
import org.eclipse.osgi.container.ModuleCollisionHook;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.framework.util.ArrayMap;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.internal.serviceregistry.ServiceReferenceImpl;
import org.eclipse.osgi.internal.serviceregistry.ServiceRegistry;
import org.eclipse.osgi.internal.serviceregistry.ShrinkableCollection;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.eclipse.osgi.storage.Storage;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.hooks.bundle.CollisionHook;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

class OSGiFrameworkHooks {
	private final CoreResolverHookFactory resolverHookFactory;
	private final ModuleCollisionHook collisionHook;

	OSGiFrameworkHooks(EquinoxContainer container, Storage storage) {
		resolverHookFactory = new CoreResolverHookFactory(container, storage);
		collisionHook = new BundleCollisionHook(container);
	}

	public ResolverHookFactory getResolverHookFactory() {
		return resolverHookFactory;
	}

	public ModuleCollisionHook getModuleCollisionHook() {
		return collisionHook;
	}

	static class BundleCollisionHook implements ModuleCollisionHook {
		final Debug debug;
		final EquinoxContainer container;

		public BundleCollisionHook(EquinoxContainer container) {
			this.container = container;
			this.debug = container.getConfiguration().getDebug();
		}

		@Override
		public void filterCollisions(int operationType, Module target, Collection<Module> collisionCandidates) {
			switch (container.getConfiguration().BSN_VERSION) {
				case EquinoxConfiguration.BSN_VERSION_SINGLE : {
					return;
				}
				case EquinoxConfiguration.BSN_VERSION_MULTIPLE : {
					collisionCandidates.clear();
					return;
				}
				case EquinoxConfiguration.BSN_VERSION_MANAGED : {
					Bundle targetBundle = target.getBundle();
					ArrayMap<Bundle, Module> candidateBundles = new ArrayMap<>(collisionCandidates.size());
					for (Module module : collisionCandidates) {
						candidateBundles.put(module.getBundle(), module);
					}
					notifyCollisionHooks(operationType, targetBundle, candidateBundles);
					collisionCandidates.retainAll(candidateBundles.getValues());
					return;
				}
				default :
					throw new IllegalStateException("Bad configuration: " + container.getConfiguration().BSN_VERSION); //$NON-NLS-1$
			}
		}

		private void notifyCollisionHooks(final int operationType, final Bundle target, Collection<Bundle> collisionCandidates) {
			// Note that collision hook results are honored for the system bundle.
			final Collection<Bundle> shrinkable = new ShrinkableCollection<>(collisionCandidates);
			if (System.getSecurityManager() == null) {
				notifyCollisionHooksPriviledged(operationType, target, shrinkable);
			} else {
				AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
					notifyCollisionHooksPriviledged(operationType, target, shrinkable);
					return null;
				});
			}
		}

		void notifyCollisionHooksPriviledged(final int operationType, final Bundle target, final Collection<Bundle> collisionCandidates) {
			if (debug.DEBUG_HOOKS) {
				Debug.println("notifyCollisionHooks(" + operationType + ", " + target + ", " + collisionCandidates + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
			ServiceRegistry registry = container.getServiceRegistry();
			if (registry != null) {
				registry.notifyHooksPrivileged(CollisionHook.class, "filterCollisions", (hook, hookRegistration) -> { //$NON-NLS-1$
					hook.filterCollisions(operationType, target, collisionCandidates);
				});
			}
		}
	}

	/**
	 * This class encapsulates the delegation to ResolverHooks that are registered with the service
	 * registry.  This way the resolver implementation only has to call out to a single hook
	 * which does all the necessary service registry lookups.
	 *
	 * This class is not thread safe and expects external synchronization.
	 *
	 */
	static class CoreResolverHookFactory implements ResolverHookFactory {
		// need a tuple to hold the service reference and hook object
		// do not use a map for performance reasons; no need to hash based on a key.
		static class HookReference {
			public HookReference(ServiceReferenceImpl<ResolverHookFactory> reference, ResolverHook hook, BundleContextImpl context) {
				this.reference = reference;
				this.hook = hook;
				this.context = context;
			}

			final ServiceReferenceImpl<ResolverHookFactory> reference;
			final ResolverHook hook;
			final BundleContextImpl context;
		}

		final Debug debug;
		final EquinoxContainer container;
		final Storage storage;
		volatile boolean inInit = false;

		public CoreResolverHookFactory(EquinoxContainer container, Storage storage) {
			this.container = container;
			this.debug = container.getConfiguration().getDebug();
			this.storage = storage;
		}

		void handleHookException(Throwable t, Object hook, String method) {
			if (debug.DEBUG_HOOKS) {
				Debug.println(hook.getClass().getName() + "." + method + "() exception:"); //$NON-NLS-1$ //$NON-NLS-2$
				if (t != null)
					Debug.printStackTrace(t);
			}
			String message = NLS.bind(Msg.SERVICE_FACTORY_EXCEPTION, hook.getClass().getName(), method);
			throw new RuntimeException(message, new BundleException(message, BundleException.REJECTED_BY_HOOK, t));
		}

		private ServiceReferenceImpl<ResolverHookFactory>[] getHookReferences(final ServiceRegistry registry, final BundleContextImpl context) {
			return AccessController.doPrivileged((PrivilegedAction<ServiceReferenceImpl<ResolverHookFactory>[]>) () -> {
				try {
					@SuppressWarnings("unchecked")
					ServiceReferenceImpl<ResolverHookFactory>[] result = (ServiceReferenceImpl<ResolverHookFactory>[]) registry.getServiceReferences(context, ResolverHookFactory.class.getName(), null, false);
					return result;
				} catch (InvalidSyntaxException e) {
					// cannot happen; no filter
					return null;
				}
			});

		}

		@Override
		public ResolverHook begin(Collection<BundleRevision> triggers) {
			if (debug.DEBUG_HOOKS) {
				Debug.println("ResolverHook.begin"); //$NON-NLS-1$
			}
			ModuleContainer mContainer = storage.getModuleContainer();
			Module systemModule = mContainer == null ? null : mContainer.getModule(0);
			ServiceRegistry registry = container.getServiceRegistry();
			if (registry == null || systemModule == null) {
				return new CoreResolverHook(Collections.emptyList(), systemModule);
			}

			BundleContextImpl context = (BundleContextImpl) EquinoxContainer.secureAction.getContext(systemModule.getBundle());

			ServiceReferenceImpl<ResolverHookFactory>[] refs = getHookReferences(registry, context);
			List<HookReference> hookRefs = refs == null ? Collections.emptyList()
					: new ArrayList<>(refs.length);
			if (refs != null) {
				for (ServiceReferenceImpl<ResolverHookFactory> hookRef : refs) {
					ResolverHookFactory factory = EquinoxContainer.secureAction.getService(hookRef, context);
					if (factory != null) {
						try {
							ResolverHook hook = factory.begin(triggers);
							if (hook != null)
								hookRefs.add(new HookReference(hookRef, hook, context));
						} catch (Throwable t) {
							// need to force an end call on the ResolverHooks we got and release them
							try {
								new CoreResolverHook(hookRefs, systemModule).end();
							} catch (Throwable endError) {
								// we are already in failure mode; just continue
							}
							handleHookException(t, factory, "begin"); //$NON-NLS-1$
						}
					}
				}
			}
			return new CoreResolverHook(hookRefs, systemModule);
		}

		class CoreResolverHook implements ResolutionReport.Listener, ResolverHook {
			private final List<HookReference> hooks;
			private final Module systemModule;

			private volatile ResolutionReport resolutionReport;

			CoreResolverHook(List<HookReference> hooks, Module systemModule) {
				this.hooks = hooks;
				this.systemModule = systemModule;
			}

			@Override
			public void filterResolvable(Collection<BundleRevision> candidates) {
				if (debug.DEBUG_HOOKS) {
					Debug.println("ResolverHook.filterResolvable(" + candidates + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (isBootInit()) {
					// only allow the system bundle and its fragments resolve during boot up and init
					for (Iterator<BundleRevision> iCandidates = candidates.iterator(); iCandidates.hasNext();) {
						BundleRevision revision = iCandidates.next();
						if ((revision.getTypes() & BundleRevision.TYPE_FRAGMENT) == 0) {
							// host bundle; check if it is the system bundle
							if (revision.getBundle().getBundleId() != 0) {
								iCandidates.remove();
							}
						}
						// just leave all fragments.  Only the ones that are system bundle fragments will resolve
						// since we removed all the other possible hosts.
					}
				}
				if (hooks.isEmpty())
					return;
				candidates = new ShrinkableCollection<>(candidates);
				for (HookReference hookRef : hooks) {
					if (hookRef.reference.getBundle() == null) {
						handleHookException(null, hookRef.hook, "filterResolvable"); //$NON-NLS-1$
					} else {
						try {
							hookRef.hook.filterResolvable(candidates);
						} catch (Throwable t) {
							handleHookException(t, hookRef.hook, "filterResolvable"); //$NON-NLS-1$
						}
					}
				}
			}

			private boolean isBootInit() {
				return systemModule == null || !Module.RESOLVED_SET.contains(systemModule.getState()) || (systemModule.getState().equals(State.STARTING) && inInit);
			}

			@Override
			public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
				if (debug.DEBUG_HOOKS) {
					Debug.println("ResolverHook.filterSingletonCollisions(" + singleton + ", " + collisionCandidates + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				if (hooks.isEmpty())
					return;
				collisionCandidates = new ShrinkableCollection<>(collisionCandidates);
				for (HookReference hookRef : hooks) {
					if (hookRef.reference.getBundle() == null) {
						handleHookException(null, hookRef.hook, "filterSingletonCollisions"); //$NON-NLS-1$
					} else {
						try {
							hookRef.hook.filterSingletonCollisions(singleton, collisionCandidates);
						} catch (Throwable t) {
							handleHookException(t, hookRef.hook, "filterSingletonCollisions"); //$NON-NLS-1$
						}
					}
				}
			}

			@Override
			public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
				if (debug.DEBUG_HOOKS) {
					Debug.println("ResolverHook.filterMatches(" + requirement + ", " + candidates + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				if (hooks.isEmpty())
					return;
				candidates = new ShrinkableCollection<>(candidates);
				for (HookReference hookRef : hooks) {
					if (hookRef.reference.getBundle() == null) {
						handleHookException(null, hookRef.hook, "filterMatches"); //$NON-NLS-1$
					} else {
						try {
							hookRef.hook.filterMatches(requirement, candidates);
						} catch (Throwable t) {
							handleHookException(t, hookRef.hook, "filterMatches"); //$NON-NLS-1$
						}
					}
				}
			}

			@Override
			public void end() {
				if (debug.DEBUG_HOOKS) {
					Debug.println("ResolverHook.end"); //$NON-NLS-1$
				}
				if (hooks.isEmpty())
					return;
				try {
					HookReference missingHook = null;
					Throwable endError = null;
					HookReference endBadHook = null;
					for (HookReference hookRef : hooks) {
						// We do not remove unregistered services here because we are going to remove all of them at the end
						if (hookRef.reference.getBundle() == null) {
							if (missingHook == null)
								missingHook = hookRef;
						} else {
							try {
								if (hookRef.hook instanceof ResolutionReport.Listener)
									((ResolutionReport.Listener) hookRef.hook).handleResolutionReport(resolutionReport);
								hookRef.hook.end();
							} catch (Throwable t) {
								// Must continue on to the next hook.end method
								// save the error for throwing at the end
								if (endError == null) {
									endError = t;
									endBadHook = hookRef;
								}
							}
						}
					}
					if (missingHook != null)
						handleHookException(null, missingHook.hook, "end"); //$NON-NLS-1$
					if (endError != null)
						handleHookException(endError, endBadHook.hook, "end"); //$NON-NLS-1$
				} finally {
					for (HookReference hookRef : hooks) {
						hookRef.context.ungetService(hookRef.reference);
					}
					hooks.clear();
				}
			}

			@Override
			public void handleResolutionReport(ResolutionReport report) {
				resolutionReport = report;
			}
		}
	}

	public void initBegin() {
		resolverHookFactory.inInit = true;
	}

	public void initEnd() {
		resolverHookFactory.inInit = false;
	}
}
