/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.osgi.framework.eventmgr.CopyOnWriteIdentityMap;
import org.eclipse.osgi.framework.eventmgr.EventDispatcher;
import org.eclipse.osgi.framework.eventmgr.EventManager;
import org.eclipse.osgi.framework.eventmgr.ListenerQueue;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.serviceregistry.HookContext;
import org.eclipse.osgi.internal.serviceregistry.ServiceRegistry;
import org.eclipse.osgi.internal.serviceregistry.ShrinkableCollection;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.hooks.bundle.CollisionHook;
import org.osgi.framework.hooks.bundle.EventHook;

public class EquinoxEventPublisher {
	static final String eventHookName = EventHook.class.getName();
	static final String collisionHookName = CollisionHook.class.getName();
	static final int FRAMEWORK_STOPPED_MASK = (FrameworkEvent.STOPPED | FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED | FrameworkEvent.STOPPED_UPDATE);

	static final int BUNDLEEVENT = 1;
	static final int BUNDLEEVENTSYNC = 2;
	/* SERVICEEVENT(3) is now handled by ServiceRegistry */
	static final int FRAMEWORKEVENT = 4;

	private final EquinoxContainer container;

	private Object monitor = new Object();
	private EventManager eventManager;

	/*
	 * The following maps objects keep track of event listeners
	 * by BundleContext.  Each element is a Map that is the set
	 * of event listeners for a particular BundleContext.  The max number of
	 * elements each of the following maps will have is the number of bundles
	 * installed in the Framework.
	 */
	// Map of BundleContexts for bundle's BundleListeners.
	private final Map<BundleContextImpl, CopyOnWriteIdentityMap<BundleListener, BundleListener>> allBundleListeners = new LinkedHashMap<>();

	// Map of BundleContexts for bundle's SynchronousBundleListeners.
	private final Map<BundleContextImpl, CopyOnWriteIdentityMap<SynchronousBundleListener, SynchronousBundleListener>> allSyncBundleListeners = new LinkedHashMap<>();

	// Map of BundleContexts for bundle's FrameworkListeners.
	private final Map<BundleContextImpl, CopyOnWriteIdentityMap<FrameworkListener, FrameworkListener>> allFrameworkListeners = new LinkedHashMap<>();

	public EquinoxEventPublisher(EquinoxContainer container) {
		this.container = container;
	}

	void init() {
		// create our event manager on init()
		resetEventManager(new EventManager("Framework Event Dispatcher: " + container.toString())); //$NON-NLS-1$
	}

	void close() {
		// ensure we have flushed any events in the queue
		flushFrameworkEvents();
		// close and clear out the event manager
		resetEventManager(null);
		// make sure we clear out all the remaining listeners
		allBundleListeners.clear();
		allSyncBundleListeners.clear();
		allFrameworkListeners.clear();
	}

	private void resetEventManager(EventManager newEventManager) {
		EventManager currentEventManager;
		synchronized (this.monitor) {
			currentEventManager = eventManager;
			eventManager = newEventManager;
		}
		if (currentEventManager != null) {
			currentEventManager.close();
		}
	}

	public <K, V, E> ListenerQueue<K, V, E> newListenerQueue() {
		synchronized (this.monitor) {
			return new ListenerQueue<>(eventManager);
		}
	}

	private boolean isEventManagerSet() {
		synchronized (this.monitor) {
			return eventManager != null;
		}
	}

	/**
	 * Deliver a BundleEvent to SynchronousBundleListeners (synchronous) and
	 * BundleListeners (asynchronous).
	 *
	 * @param type
	 *            BundleEvent type.
	 * @param bundle
	 *            Affected bundle or null.
	 * @param origin
	 *            The origin of the event
	 */
	public void publishBundleEvent(int type, Bundle bundle, Bundle origin) {
		if (origin != null) {
			publishBundleEvent(new BundleEvent(type, bundle, origin));
		} else {
			publishBundleEvent(new BundleEvent(type, bundle));
		}
	}

	private void publishBundleEvent(final BundleEvent event) {
		if (System.getSecurityManager() == null) {
			publishBundleEventPrivileged(event);
		} else {
			AccessController.doPrivileged(new PrivilegedAction<Void>() {
				@Override
				public Void run() {
					publishBundleEventPrivileged(event);
					return null;
				}
			});
		}
	}

	void publishBundleEventPrivileged(BundleEvent event) {
		if (!isEventManagerSet()) {
			return;
		}
		/*
		 * We must collect the snapshots of the sync and async listeners
		 * BEFORE we dispatch the event.
		 */
		/* Collect snapshot of SynchronousBundleListeners */
		/* Build the listener snapshot */
		Map<BundleContextImpl, Set<Map.Entry<SynchronousBundleListener, SynchronousBundleListener>>> listenersSync;
		BundleContextImpl systemContext = null;
		Set<Map.Entry<SynchronousBundleListener, SynchronousBundleListener>> systemBundleListenersSync = null;
		synchronized (allSyncBundleListeners) {
			listenersSync = new LinkedHashMap<>(allSyncBundleListeners.size());
			for (Map.Entry<BundleContextImpl, CopyOnWriteIdentityMap<SynchronousBundleListener, SynchronousBundleListener>> entry : allSyncBundleListeners.entrySet()) {
				CopyOnWriteIdentityMap<SynchronousBundleListener, SynchronousBundleListener> listeners = entry.getValue();
				if (!listeners.isEmpty()) {
					Set<Map.Entry<SynchronousBundleListener, SynchronousBundleListener>> listenerEntries = listeners.entrySet();
					if (entry.getKey().getBundleImpl().getBundleId() == 0) {
						systemContext = entry.getKey();
						// record the snapshot; no need to create another copy
						// because the hooks are not exposed to this set
						systemBundleListenersSync = listenerEntries;
					}
					listenersSync.put(entry.getKey(), listeners.entrySet());
				}
			}
		}
		/* Collect snapshot of BundleListeners; only if the event is NOT STARTING or STOPPING or LAZY_ACTIVATION */
		Map<BundleContextImpl, Set<Map.Entry<BundleListener, BundleListener>>> listenersAsync = null;
		Set<Map.Entry<BundleListener, BundleListener>> systemBundleListenersAsync = null;
		if ((event.getType() & (BundleEvent.STARTING | BundleEvent.STOPPING | BundleEvent.LAZY_ACTIVATION)) == 0) {
			synchronized (allBundleListeners) {
				listenersAsync = new LinkedHashMap<>(allBundleListeners.size());
				for (Map.Entry<BundleContextImpl, CopyOnWriteIdentityMap<BundleListener, BundleListener>> entry : allBundleListeners.entrySet()) {
					CopyOnWriteIdentityMap<BundleListener, BundleListener> listeners = entry.getValue();
					if (!listeners.isEmpty()) {
						Set<Map.Entry<BundleListener, BundleListener>> listenerEntries = listeners.entrySet();
						if (entry.getKey().getBundleImpl().getBundleId() == 0) {
							systemContext = entry.getKey();
							// record the snapshot; no need to create another copy
							// because the hooks are not exposed to this set
							systemBundleListenersAsync = listenerEntries;
						}
						listenersAsync.put(entry.getKey(), listenerEntries);
					}
				}
			}
		}

		/* shrink the snapshot.
		 * keySet returns a Collection which cannot be added to and
		 * removals from that collection will result in removals of the
		 * entry from the snapshot.
		 */
		Collection<BundleContext> shrinkable;
		if (listenersAsync == null) {
			shrinkable = asBundleContexts(listenersSync.keySet());
		} else {
			shrinkable = new ShrinkableCollection<>(asBundleContexts(listenersSync.keySet()), asBundleContexts(listenersAsync.keySet()));
		}

		notifyEventHooksPrivileged(event, shrinkable);

		// always add back the system bundle listeners if they were removed
		if (systemBundleListenersSync != null && !listenersSync.containsKey(systemContext)) {
			listenersSync.put(systemContext, systemBundleListenersSync);
		}
		if (systemBundleListenersAsync != null && !listenersAsync.containsKey(systemContext)) {
			listenersAsync.put(systemContext, systemBundleListenersAsync);
		}

		/* Dispatch the event to the snapshot for sync listeners */
		if (!listenersSync.isEmpty()) {
			ListenerQueue<SynchronousBundleListener, SynchronousBundleListener, BundleEvent> queue = newListenerQueue();
			for (Map.Entry<BundleContextImpl, Set<Map.Entry<SynchronousBundleListener, SynchronousBundleListener>>> entry : listenersSync.entrySet()) {
				@SuppressWarnings({"rawtypes", "unchecked"})
				EventDispatcher<SynchronousBundleListener, SynchronousBundleListener, BundleEvent> dispatcher = (EventDispatcher) entry.getKey();
				Set<Map.Entry<SynchronousBundleListener, SynchronousBundleListener>> listeners = entry.getValue();
				queue.queueListeners(listeners, dispatcher);
			}
			queue.dispatchEventSynchronous(BUNDLEEVENTSYNC, event);
		}

		/* Dispatch the event to the snapshot for async listeners */
		if ((listenersAsync != null) && !listenersAsync.isEmpty()) {
			ListenerQueue<BundleListener, BundleListener, BundleEvent> queue = newListenerQueue();
			for (Map.Entry<BundleContextImpl, Set<Map.Entry<BundleListener, BundleListener>>> entry : listenersAsync.entrySet()) {
				@SuppressWarnings({"rawtypes", "unchecked"})
				EventDispatcher<BundleListener, BundleListener, BundleEvent> dispatcher = (EventDispatcher) entry.getKey();
				Set<Map.Entry<BundleListener, BundleListener>> listeners = entry.getValue();
				queue.queueListeners(listeners, dispatcher);
			}
			queue.dispatchEventAsynchronous(BUNDLEEVENT, event);
		}
	}

	private void notifyEventHooksPrivileged(final BundleEvent event, final Collection<BundleContext> result) {
		if (container.getConfiguration().getDebug().DEBUG_HOOKS) {
			Debug.println("notifyBundleEventHooks(" + event.getType() + ":" + event.getBundle() + ", " + result + " )"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		ServiceRegistry serviceRegistry = container.getServiceRegistry();
		if (serviceRegistry != null) {
			serviceRegistry.notifyHooksPrivileged(new HookContext() {
				@Override
				public void call(Object hook, ServiceRegistration<?> hookRegistration) throws Exception {
					if (hook instanceof EventHook) {
						((EventHook) hook).event(event, result);
					}
				}

				@Override
				public String getHookClassName() {
					return eventHookName;
				}

				@Override
				public String getHookMethodName() {
					return "event"; //$NON-NLS-1$
				}

				@Override
				public boolean skipRegistration(ServiceRegistration<?> hookRegistration) {
					return false;
				}
			});
		}
	}

	/**
	 * Deliver a FrameworkEvent.
	 *
	 * @param type
	 *            FrameworkEvent type.
	 * @param bundle
	 *            Affected bundle or null for system bundle.
	 * @param throwable
	 *            Related exception or null.
	 */
	public void publishFrameworkEvent(int type, Bundle bundle, Throwable throwable) {
		publishFrameworkEvent(type, bundle, throwable, (FrameworkListener[]) null);
	}

	public void publishFrameworkEvent(int type, Bundle bundle, Throwable throwable, final FrameworkListener... listeners) {
		if (bundle == null)
			bundle = container.getStorage().getModuleContainer().getModule(0).getBundle();
		final FrameworkEvent event = new FrameworkEvent(type, bundle, throwable);
		if (System.getSecurityManager() == null) {
			publishFrameworkEventPrivileged(event, listeners);
		} else {
			AccessController.doPrivileged(new PrivilegedAction<Void>() {
				@Override
				public Void run() {
					publishFrameworkEventPrivileged(event, listeners);
					return null;
				}
			});
		}
	}

	public void publishFrameworkEventPrivileged(FrameworkEvent event, FrameworkListener... callerListeners) {
		if (!isEventManagerSet()) {
			return;
		}
		// Build the listener snapshot
		Map<BundleContextImpl, Set<Map.Entry<FrameworkListener, FrameworkListener>>> listenerSnapshot;
		synchronized (allFrameworkListeners) {
			listenerSnapshot = new LinkedHashMap<>(allFrameworkListeners.size());
			for (Map.Entry<BundleContextImpl, CopyOnWriteIdentityMap<FrameworkListener, FrameworkListener>> entry : allFrameworkListeners.entrySet()) {
				CopyOnWriteIdentityMap<FrameworkListener, FrameworkListener> listeners = entry.getValue();
				if (!listeners.isEmpty()) {
					listenerSnapshot.put(entry.getKey(), listeners.entrySet());
				}
			}
		}
		// If framework event hook were defined they would be called here

		// deliver the event to the snapshot
		ListenerQueue<FrameworkListener, FrameworkListener, FrameworkEvent> queue = newListenerQueue();

		// add the listeners specified by the caller first
		if (callerListeners != null && callerListeners.length > 0) {
			Map<FrameworkListener, FrameworkListener> listeners = new HashMap<>();
			for (FrameworkListener listener : callerListeners) {
				if (listener != null)
					listeners.put(listener, listener);
			}
			// We use the system bundle context as the dispatcher
			if (listeners.size() > 0) {
				BundleContextImpl systemContext = (BundleContextImpl) container.getStorage().getModuleContainer().getModule(0).getBundle().getBundleContext();
				@SuppressWarnings({"rawtypes", "unchecked"})
				EventDispatcher<FrameworkListener, FrameworkListener, FrameworkEvent> dispatcher = (EventDispatcher) systemContext;
				queue.queueListeners(listeners.entrySet(), dispatcher);
			}
		}

		for (Map.Entry<BundleContextImpl, Set<Map.Entry<FrameworkListener, FrameworkListener>>> entry : listenerSnapshot.entrySet()) {
			@SuppressWarnings({"rawtypes", "unchecked"})
			EventDispatcher<FrameworkListener, FrameworkListener, FrameworkEvent> dispatcher = (EventDispatcher) entry.getKey();
			Set<Map.Entry<FrameworkListener, FrameworkListener>> listeners = entry.getValue();
			queue.queueListeners(listeners, dispatcher);
		}

		queue.dispatchEventAsynchronous(FRAMEWORKEVENT, event);
		// close down the publisher if we got the stopped event
		if ((event.getType() & FRAMEWORK_STOPPED_MASK) != 0) {
			close();
		}
	}

	/**
	 * Coerce the generic type of a collection from Collection<BundleContextImpl>
	 * to Collection<BundleContext>
	 * @param c Collection to be coerced.
	 * @return c coerced to Collection<BundleContext>
	 */
	@SuppressWarnings("unchecked")
	public static Collection<BundleContext> asBundleContexts(Collection<? extends BundleContext> c) {
		return (Collection<BundleContext>) c;
	}

	void addBundleListener(BundleListener listener, BundleContextImpl context) {
		if (listener instanceof SynchronousBundleListener) {
			container.checkAdminPermission(context.getBundle(), AdminPermission.LISTENER);
			synchronized (allSyncBundleListeners) {
				CopyOnWriteIdentityMap<SynchronousBundleListener, SynchronousBundleListener> listeners = allSyncBundleListeners.get(context);
				if (listeners == null) {
					listeners = new CopyOnWriteIdentityMap<>();
					allSyncBundleListeners.put(context, listeners);
				}
				listeners.put((SynchronousBundleListener) listener, (SynchronousBundleListener) listener);
			}
		} else {
			synchronized (allBundleListeners) {
				CopyOnWriteIdentityMap<BundleListener, BundleListener> listeners = allBundleListeners.get(context);
				if (listeners == null) {
					listeners = new CopyOnWriteIdentityMap<>();
					allBundleListeners.put(context, listeners);
				}
				listeners.put(listener, listener);
			}
		}
	}

	void removeBundleListener(BundleListener listener, BundleContextImpl context) {
		if (listener instanceof SynchronousBundleListener) {
			container.checkAdminPermission(context.getBundle(), AdminPermission.LISTENER);
			synchronized (allSyncBundleListeners) {
				CopyOnWriteIdentityMap<SynchronousBundleListener, SynchronousBundleListener> listeners = allSyncBundleListeners.get(context);
				if (listeners != null)
					listeners.remove(listener);
			}
		} else {
			synchronized (allBundleListeners) {
				CopyOnWriteIdentityMap<BundleListener, BundleListener> listeners = allBundleListeners.get(context);
				if (listeners != null)
					listeners.remove(listener);
			}
		}
	}

	void addFrameworkListener(FrameworkListener listener, BundleContextImpl context) {
		synchronized (allFrameworkListeners) {
			CopyOnWriteIdentityMap<FrameworkListener, FrameworkListener> listeners = allFrameworkListeners.get(context);
			if (listeners == null) {
				listeners = new CopyOnWriteIdentityMap<>();
				allFrameworkListeners.put(context, listeners);
			}
			listeners.put(listener, listener);
		}
	}

	void removeFrameworkListener(FrameworkListener listener, BundleContextImpl context) {
		synchronized (allFrameworkListeners) {
			CopyOnWriteIdentityMap<FrameworkListener, FrameworkListener> listeners = allFrameworkListeners.get(context);
			if (listeners != null)
				listeners.remove(listener);
		}
	}

	void removeAllListeners(BundleContextImpl context) {
		// leave any left over listeners until the framework STOPPED event
		if (context.getBundleImpl().getBundleId() != 0) {
			synchronized (allBundleListeners) {
				allBundleListeners.remove(context);
			}
			synchronized (allSyncBundleListeners) {
				allSyncBundleListeners.remove(context);
			}
		}
		synchronized (allFrameworkListeners) {
			allFrameworkListeners.remove(context);
		}
	}

	void flushFrameworkEvents() {
		EventDispatcher<Object, Object, CountDownLatch> dispatcher = new EventDispatcher<Object, Object, CountDownLatch>() {
			@Override
			public void dispatchEvent(Object eventListener, Object listenerObject, int eventAction, CountDownLatch flushedSignal) {
				// Signal that we have flushed all events
				flushedSignal.countDown();
			}
		};

		ListenerQueue<Object, Object, CountDownLatch> queue = newListenerQueue();
		queue.queueListeners(Collections.<Object, Object> singletonMap(dispatcher, dispatcher).entrySet(), dispatcher);

		// fire event with the flushedSignal latch
		CountDownLatch flushedSignal = new CountDownLatch(1);
		queue.dispatchEventAsynchronous(0, flushedSignal);

		try {
			// Wait for the flush signal; timeout after 30 seconds
			flushedSignal.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// ignore but reset the interrupted flag
			Thread.currentThread().interrupt();
		}
	}
}
