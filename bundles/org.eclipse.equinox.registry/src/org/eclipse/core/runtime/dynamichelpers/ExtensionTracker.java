/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
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
 *     Sebastian Davids - fix for the bug 178028 (removals not propagated to handlers)
 *******************************************************************************/
package org.eclipse.core.runtime.dynamichelpers;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.internal.registry.RegistryMessages;
import org.eclipse.core.internal.runtime.ReferenceHashSet;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.*;

/**
 * Implementation of the IExtensionTracker.
 * <p>
 * This class can be used without OSGi running.
 * </p>
 * 
 * @see org.eclipse.core.runtime.dynamichelpers.IExtensionTracker
 * @since 3.1
 */
public class ExtensionTracker implements IExtensionTracker, IRegistryChangeListener {
	// Map keeping the association between extensions and a set of objects. Key:
	// IExtension, value: ReferenceHashSet.
	private Map<IExtension, ReferenceHashSet<Object>> extensionToObjects = new HashMap<>();
	private ListenerList<HandlerWrapper> handlers = new ListenerList<>();
	private final Object lock = new Object();
	private boolean closed = false;
	private final IExtensionRegistry registry; // the registry that this tacker works with

	private static final Object[] EMPTY_ARRAY = new Object[0];

	/**
	 * Construct a new instance of the extension tracker.
	 */
	public ExtensionTracker() {
		this(RegistryFactory.getRegistry());
	}

	/**
	 * Construct a new instance of the extension tracker using the given registry
	 * containing tracked extensions and extension points.
	 *
	 * @param theRegistry the extension registry to track
	 * @since org.eclipse.equinox.registry 3.2
	 */
	public ExtensionTracker(IExtensionRegistry theRegistry) {
		registry = theRegistry;
		if (registry != null) {
			registry.addRegistryChangeListener(this);
		} else {
			RuntimeLog.log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, 0,
					RegistryMessages.registry_no_default, null));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.runtime.dynamichelpers.IExtensionTracker#registerHandler(org
	 * .eclipse.core.runtime.dynamichelpers.IExtensionChangeHandler,
	 * org.eclipse.core.runtime.dynamichelpers.IFilter)
	 */
	@Override
	public void registerHandler(IExtensionChangeHandler handler, IFilter filter) {
		synchronized (lock) {
			if (closed) {
				return;
			}
			// TODO need to store the filter with the handler
			handlers.add(new HandlerWrapper(handler, filter));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see IExtensionTracker@unregisterHandler(IExtensionChangeHandler)
	 */
	@Override
	public void unregisterHandler(IExtensionChangeHandler handler) {
		synchronized (lock) {
			if (closed) {
				return;
			}
			handlers.remove(new HandlerWrapper(handler, null));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see IExtensionTracker@registerObject(IExtension, Object, int)
	 */
	@Override
	public void registerObject(IExtension element, Object object, int referenceType) {
		if (element == null || object == null) {
			return;
		}

		synchronized (lock) {
			if (closed) {
				return;
			}

			ReferenceHashSet<Object> associatedObjects = extensionToObjects.get(element);
			if (associatedObjects == null) {
				associatedObjects = new ReferenceHashSet<>();
				extensionToObjects.put(element, associatedObjects);
			}
			associatedObjects.add(object, referenceType);
		}
	}

	/**
	 * Implementation of IRegistryChangeListener interface.
	 * <p>
	 * <em>This method must not be called by clients.</em>
	 * </p>
	 */
	@Override
	public void registryChanged(IRegistryChangeEvent event) {
		IExtensionDelta delta[] = event.getExtensionDeltas();
		int len = delta.length;
		for (int i = 0; i < len; i++) {
			switch (delta[i].getKind()) {
			case IExtensionDelta.ADDED:
				doAdd(delta[i]);
				break;
			case IExtensionDelta.REMOVED:
				doRemove(delta[i]);
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Notify all handlers whose filter matches that the given delta occurred. If
	 * the list of objects is not <code>null</code> then this is a removal and the
	 * handlers will be given a chance to process the list. If it is
	 * <code>null</code> then the notification is an addition.
	 *
	 * @param delta   the change to broadcast
	 * @param objects the objects to pass to the handlers on removals
	 */
	private void notify(IExtensionDelta delta, Object[] objects) {
		// Get a copy of the handlers for safe notification
		Object[] handlersCopy = null;
		synchronized (lock) {
			if (closed) {
				return;
			}

			if (handlers == null || handlers.isEmpty()) {
				return;
			}
			handlersCopy = handlers.getListeners();
		}

		for (Object w : handlersCopy) {
			HandlerWrapper wrapper = (HandlerWrapper) w;
			if (wrapper.filter == null || wrapper.filter.matches(delta.getExtensionPoint())) {
				if (objects == null) {
					applyAdd(wrapper.handler, delta.getExtension());
				} else {
					applyRemove(wrapper.handler, delta.getExtension(), objects);
				}
			}
		}
	}

	protected void applyAdd(IExtensionChangeHandler handler, IExtension extension) {
		handler.addExtension(this, extension);
	}

	private void doAdd(IExtensionDelta delta) {
		notify(delta, null);
	}

	private void doRemove(IExtensionDelta delta) {
		Object[] removedObjects = null;
		synchronized (lock) {
			if (closed) {
				return;
			}

			ReferenceHashSet<?> associatedObjects = extensionToObjects.remove(delta.getExtension());
			if (associatedObjects == null) {
				removedObjects = EMPTY_ARRAY;
			} else {
				// Copy the objects early so we don't hold the lock too long
				removedObjects = associatedObjects.toArray();
			}
		}
		notify(delta, removedObjects);
	}

	protected void applyRemove(IExtensionChangeHandler handler, IExtension removedExtension, Object[] removedObjects) {
		handler.removeExtension(removedExtension, removedObjects);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see IExtensionTracker@getObjects(IExtension)
	 */
	@Override
	public Object[] getObjects(IExtension element) {
		synchronized (lock) {
			if (closed) {
				return EMPTY_ARRAY;
			}
			ReferenceHashSet<?> objectSet = extensionToObjects.get(element);
			if (objectSet == null) {
				return EMPTY_ARRAY;
			}

			return objectSet.toArray();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see IExtensionTracker@close()
	 */
	@Override
	public void close() {
		synchronized (lock) {
			if (closed) {
				return;
			}
			if (registry != null) {
				registry.removeRegistryChangeListener(this);
			}
			extensionToObjects = null;
			handlers = null;

			closed = true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see IExtensionTracker@unregisterObject(IExtension, Object)
	 */
	@Override
	public void unregisterObject(IExtension extension, Object object) {
		synchronized (lock) {
			if (closed) {
				return;
			}
			ReferenceHashSet<Object> associatedObjects = extensionToObjects.get(extension);
			if (associatedObjects != null) {
				associatedObjects.remove(object);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see IExtensionTracker@unregisterObject(IExtension)
	 */
	@Override
	public Object[] unregisterObject(IExtension extension) {
		synchronized (lock) {
			if (closed) {
				return EMPTY_ARRAY;
			}
			ReferenceHashSet<?> associatedObjects = extensionToObjects.remove(extension);
			if (associatedObjects == null) {
				return EMPTY_ARRAY;
			}
			return associatedObjects.toArray();
		}
	}

	/**
	 * Return an instance of filter matching all changes for the given extension
	 * point.
	 *
	 * @param xpt the extension point
	 * @return a filter
	 */
	public static IFilter createExtensionPointFilter(final IExtensionPoint xpt) {
		return xpt::equals;
	}

	/**
	 * Return an instance of filter matching all changes for the given extension
	 * points.
	 *
	 * @param xpts the extension points used to filter
	 * @return a filter
	 */
	public static IFilter createExtensionPointFilter(final IExtensionPoint[] xpts) {
		return (IExtensionPoint target) -> {
			for (IExtensionPoint xpt : xpts) {
				if (xpt.equals(target)) {
					return true;
				}
			}
			return false;
		};
	}

	/**
	 * Return an instance of filter matching all changes from a given plugin.
	 *
	 * @param id the plugin id
	 * @return a filter
	 */
	public static IFilter createNamespaceFilter(final String id) {
		return (IExtensionPoint target) -> id.equals(target.getNamespaceIdentifier());
	}

	private static class HandlerWrapper {
		IExtensionChangeHandler handler;
		IFilter filter;

		public HandlerWrapper(IExtensionChangeHandler handler, IFilter filter) {
			this.handler = handler;
			this.filter = filter;
		}

		@Override
		public boolean equals(Object target) {
			return handler.equals(((HandlerWrapper) target).handler);
		}

		@Override
		public int hashCode() {
			return handler.hashCode();
		}
	}

}
