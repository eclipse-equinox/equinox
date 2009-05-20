/*******************************************************************************
 * Copyright (c) 2005, 2007 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *******************************************************************************/

package org.eclipse.equinox.http.registry.internal;

import java.util.*;
import org.eclipse.core.runtime.*;

public class ExtensionPointTracker {
	public interface Listener {
		public void added(IExtension extension);

		public void removed(IExtension extension);
	}

	private static final Listener NULL_LISTENER = new Listener() {
		public void added(IExtension extension) {
		}

		public void removed(IExtension extension) {
		}
	};

	private final IExtensionRegistry registry;
	private final String extensionPointId;
	final String namespace;
	final String simpleIdentifier;
	private final Set extensionCache = new HashSet();
	protected final Listener listener;
	private final RegistryChangeListener registryChangeListener = new RegistryChangeListener();
	private boolean closed = true;

	public ExtensionPointTracker(IExtensionRegistry registry, String extensionPointId, Listener listener) {
		this.registry = registry;
		this.extensionPointId = extensionPointId;
		this.listener = (listener != null) ? listener : NULL_LISTENER;

		if (extensionPointId == null || -1 == extensionPointId.indexOf('.'))
			throw new IllegalArgumentException("Unexpected Extension Point Identifier: " + extensionPointId); //$NON-NLS-1$
		int lastDotIndex = extensionPointId.lastIndexOf('.');
		namespace = extensionPointId.substring(0, lastDotIndex);
		simpleIdentifier = extensionPointId.substring(lastDotIndex + 1);
	}

	public void open() {
		IExtension[] extensions = null;
		synchronized (this) {
			if (!closed) {
				return;
			}
			registry.addRegistryChangeListener(registryChangeListener, namespace);
			try {
				IExtensionPoint point = registry.getExtensionPoint(extensionPointId);
				if (point != null) {
					extensions = point.getExtensions();
					extensionCache.addAll(Arrays.asList(extensions));
				}
				closed = false;
			} catch (InvalidRegistryObjectException e) {
				registry.removeRegistryChangeListener(registryChangeListener);
				throw e;
			}
		}
		if (extensions != null) {
			for (int i = 0; i < extensions.length; ++i) {
				listener.added(extensions[i]);
			}
		}
	}

	public void close() {
		IExtension[] extensions = null;
		synchronized (this) {
			if (closed) {
				return;
			}
			closed = true;
			registry.removeRegistryChangeListener(registryChangeListener);
			extensions = this.getExtensions();
			extensionCache.clear();
		}
		for (int i = 0; i < extensions.length; ++i) {
			listener.removed(extensions[i]);
		}
	}

	synchronized boolean removeExtension(IExtension extension) {
		if (closed) {
			return false;
		}
		return extensionCache.remove(extension);
	}

	synchronized boolean addExtension(IExtension extension) {
		if (closed) {
			return false;
		}
		return extensionCache.add(extension);
	}

	public synchronized IExtension[] getExtensions() {
		return (IExtension[]) extensionCache.toArray(new IExtension[extensionCache.size()]);
	}

	class RegistryChangeListener implements IRegistryChangeListener {
		public void registryChanged(IRegistryChangeEvent event) {
			IExtensionDelta[] deltas = event.getExtensionDeltas(namespace, simpleIdentifier);
			for (int i = 0; i < deltas.length; ++i) {
				IExtensionDelta delta = deltas[i];
				IExtension extension = delta.getExtension();
				switch (delta.getKind()) {
					case IExtensionDelta.ADDED :
						if (addExtension(extension)) {
							listener.added(extension);
						}
						break;
					case IExtensionDelta.REMOVED :
						if (removeExtension(extension)) {
							listener.removed(extension);
						}
					default :
						break;
				}
			}
		}
	}
}
