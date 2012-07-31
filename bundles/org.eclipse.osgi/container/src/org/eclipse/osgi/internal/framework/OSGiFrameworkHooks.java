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
package org.eclipse.osgi.internal.framework;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.container.ModuleCollisionHook;
import org.eclipse.osgi.framework.internal.core.Msg;
import org.eclipse.osgi.internal.serviceregistry.*;
import org.eclipse.osgi.next.internal.debug.Debug;
import org.eclipse.osgi.service.resolver.ResolverHookException;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.framework.hooks.bundle.CollisionHook;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.*;

class OSGiFrameworkHooks {
	static final String collisionHookName = CollisionHook.class.getName();
	private final ResolverHookFactory resolverHookFactory;
	private final ModuleCollisionHook collisionHook;

	OSGiFrameworkHooks(EquinoxContainer container) {
		resolverHookFactory = new CoreResolverHookFactory(container);
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
			Bundle targetBundle = target.getBundle();
			Collection<Bundle> candidateBundles = new ArrayList<Bundle>();
			for (Module module : collisionCandidates) {
				candidateBundles.add(module.getBundle());
			}
			notifyCollisionHooks(operationType, targetBundle, candidateBundles);
		}

		private void notifyCollisionHooks(final int operationType, final Bundle target, Collection<Bundle> collisionCandidates) {
			final Collection<Bundle> shrinkable = new ShrinkableCollection<Bundle>(collisionCandidates);
			if (System.getSecurityManager() == null) {
				notifyCollisionHooksPriviledged(operationType, target, shrinkable);
			} else {
				AccessController.doPrivileged(new PrivilegedAction<Object>() {
					public Object run() {
						notifyCollisionHooksPriviledged(operationType, target, shrinkable);
						return null;
					}
				});
			}
		}

		void notifyCollisionHooksPriviledged(final int operationType, final Bundle target, final Collection<Bundle> collisionCandidates) {
			if (debug.DEBUG_HOOKS) {
				Debug.println("notifyCollisionHooks(" + operationType + ", " + target + ", " + collisionCandidates + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
			}
			ServiceRegistry registry = container.getServiceRegistry();
			if (registry != null) {
				registry.notifyHooksPrivileged(new HookContext() {
					public void call(Object hook, ServiceRegistration<?> hookRegistration) throws Exception {
						if (hook instanceof CollisionHook) {
							((CollisionHook) hook).filterCollisions(operationType, target, collisionCandidates);
						}
					}

					public String getHookClassName() {
						return collisionHookName;
					}

					public String getHookMethodName() {
						return "filterCollisions"; //$NON-NLS-1$ 
					}
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

		public CoreResolverHookFactory(EquinoxContainer container) {
			this.container = container;
			this.debug = container.getConfiguration().getDebug();
		}

		void handleHookException(Throwable t, Object hook, String method) {
			if (debug.DEBUG_HOOKS) {
				Debug.println(hook.getClass().getName() + "." + method + "() exception:"); //$NON-NLS-1$ //$NON-NLS-2$
				if (t != null)
					Debug.printStackTrace(t);
			}
			String message = NLS.bind(Msg.SERVICE_FACTORY_EXCEPTION, hook.getClass().getName(), method);
			throw new ResolverHookException(message, t);
		}

		private ServiceReferenceImpl<ResolverHookFactory>[] getHookReferences(ServiceRegistry registry, BundleContextImpl context) {
			try {
				@SuppressWarnings("unchecked")
				ServiceReferenceImpl<ResolverHookFactory>[] result = (ServiceReferenceImpl<ResolverHookFactory>[]) registry.getServiceReferences(context, ResolverHookFactory.class.getName(), null, false, false);
				return result;
			} catch (InvalidSyntaxException e) {
				// cannot happen; no filter
				return null;
			}
		}

		public ResolverHook begin(Collection<BundleRevision> triggers) {
			if (debug.DEBUG_HOOKS) {
				Debug.println("ResolverHook.begin"); //$NON-NLS-1$
			}

			ServiceRegistry registry = container.getServiceRegistry();
			if (registry == null) {
				return new CoreResolverHook(Collections.<HookReference> emptyList());
			}
			Module systemModule = container.getStorage().getModuleContainer().getModule(0);
			BundleContextImpl context = (BundleContextImpl) systemModule.getBundle().getBundleContext();

			ServiceReferenceImpl<ResolverHookFactory>[] refs = getHookReferences(registry, context);
			@SuppressWarnings("unchecked")
			List<HookReference> hookRefs = refs == null ? Collections.EMPTY_LIST : new ArrayList<CoreResolverHookFactory.HookReference>(refs.length);
			if (refs != null)
				for (ServiceReferenceImpl<ResolverHookFactory> hookRef : refs) {
					ResolverHookFactory factory = context.getService(hookRef);
					if (factory != null) {
						try {
							ResolverHook hook = factory.begin(triggers);
							if (hook != null)
								hookRefs.add(new HookReference(hookRef, hook, context));
						} catch (Throwable t) {
							// need to force an end call on the ResolverHooks we got and release them
							try {
								new CoreResolverHook(hookRefs).end();
							} catch (Throwable endError) {
								// we are already in failure mode; just continue
							}
							handleHookException(t, factory, "begin"); //$NON-NLS-1$
						}
					}
				}
			return new CoreResolverHook(hookRefs);
		}

		class CoreResolverHook implements ResolverHook {
			private final List<HookReference> hooks;

			CoreResolverHook(List<HookReference> hooks) {
				this.hooks = hooks;
			}

			public void filterResolvable(Collection<BundleRevision> candidates) {
				if (debug.DEBUG_HOOKS) {
					Debug.println("ResolverHook.filterResolvable(" + candidates + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (hooks.isEmpty())
					return;
				candidates = new ShrinkableCollection<BundleRevision>(candidates);
				for (Iterator<HookReference> iHooks = hooks.iterator(); iHooks.hasNext();) {
					HookReference hookRef = iHooks.next();
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

			public void filterSingletonCollisions(BundleCapability singleton, Collection<BundleCapability> collisionCandidates) {
				if (debug.DEBUG_HOOKS) {
					Debug.println("ResolverHook.filterSingletonCollisions(" + singleton + ", " + collisionCandidates + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				if (hooks.isEmpty())
					return;
				collisionCandidates = new ShrinkableCollection<BundleCapability>(collisionCandidates);
				for (Iterator<HookReference> iHooks = hooks.iterator(); iHooks.hasNext();) {
					HookReference hookRef = iHooks.next();
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

			public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
				if (debug.DEBUG_HOOKS) {
					Debug.println("ResolverHook.filterMatches(" + requirement + ", " + candidates + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				if (hooks.isEmpty())
					return;
				candidates = new ShrinkableCollection<BundleCapability>(candidates);
				for (Iterator<HookReference> iHooks = hooks.iterator(); iHooks.hasNext();) {
					HookReference hookRef = iHooks.next();
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
					for (Iterator<HookReference> iHooks = hooks.iterator(); iHooks.hasNext();) {
						HookReference hookRef = iHooks.next();
						// We do not remove unregistered services here because we are going to remove all of them at the end
						if (hookRef.reference.getBundle() == null) {
							if (missingHook == null)
								missingHook = hookRef;
						} else {
							try {
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
		}
	}
}
