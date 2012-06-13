/*******************************************************************************
 * Copyright (c) 2010, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.internal.core;

import java.util.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.internal.serviceregistry.*;
import org.eclipse.osgi.service.resolver.ResolverHookException;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.*;

/**
 * This class encapsulates the delegation to ResolverHooks that are registered with the service
 * registry.  This way the resolver implementation only has to call out to a single hook
 * which does all the necessary service registry lookups.
 * 
 * This class is not thread safe and expects external synchronization.
 *
 */
public class CoreResolverHookFactory implements ResolverHookFactory {
	// need a tuple to hold the service reference and hook object
	// do not use a map for performance reasons; no need to hash based on a key.
	static class HookReference {
		public HookReference(ServiceReferenceImpl<ResolverHookFactory> reference, ResolverHook hook) {
			this.reference = reference;
			this.hook = hook;
		}

		final ServiceReferenceImpl<ResolverHookFactory> reference;
		final ResolverHook hook;
	}

	private final BundleContextImpl context;
	private final ServiceRegistry registry;

	public CoreResolverHookFactory(BundleContextImpl context, ServiceRegistry registry) {
		this.context = context;
		this.registry = registry;
	}

	void handleHookException(Throwable t, Object hook, String method) {
		if (Debug.DEBUG_HOOKS) {
			Debug.println(hook.getClass().getName() + "." + method + "() exception:"); //$NON-NLS-1$ //$NON-NLS-2$
			if (t != null)
				Debug.printStackTrace(t);
		}
		String message = NLS.bind(Msg.SERVICE_FACTORY_EXCEPTION, hook.getClass().getName(), method);
		throw new ResolverHookException(message, t);
	}

	private ServiceReferenceImpl<ResolverHookFactory>[] getHookReferences() {
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
		if (Debug.DEBUG_HOOKS) {
			Debug.println("ResolverHook.begin"); //$NON-NLS-1$
		}
		ServiceReferenceImpl<ResolverHookFactory>[] refs = getHookReferences();
		@SuppressWarnings("unchecked")
		List<HookReference> hookRefs = refs == null ? Collections.EMPTY_LIST : new ArrayList<CoreResolverHookFactory.HookReference>(refs.length);
		if (refs != null)
			for (ServiceReferenceImpl<ResolverHookFactory> hookRef : refs) {
				ResolverHookFactory factory = context.getService(hookRef);
				if (factory != null) {
					try {
						ResolverHook hook = factory.begin(triggers);
						if (hook != null)
							hookRefs.add(new HookReference(hookRef, hook));
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

	void releaseHooks(List<HookReference> hookRefs) {
		for (HookReference hookRef : hookRefs)
			context.ungetService(hookRef.reference);
		hookRefs.clear();
	}

	class CoreResolverHook implements ResolverHook {
		private final List<HookReference> hooks;

		CoreResolverHook(List<HookReference> hooks) {
			this.hooks = hooks;
		}

		public void filterResolvable(Collection<BundleRevision> candidates) {
			if (Debug.DEBUG_HOOKS) {
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
			if (Debug.DEBUG_HOOKS) {
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
			if (Debug.DEBUG_HOOKS) {
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
			if (Debug.DEBUG_HOOKS) {
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
				releaseHooks(hooks);
			}
		}
	}
}
