/*******************************************************************************
 * Copyright (c) 2012, 2020 IBM Corporation and others.
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
package org.eclipse.osgi.container;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.osgi.framework.namespace.NativeNamespace;
import org.osgi.framework.wiring.BundleCapability;

/**
 * An implementation of {@link BundleCapability}.
 * @since 3.10
 */
public final class ModuleCapability implements BundleCapability {
	private final String namespace;
	private final Map<String, String> directives;
	private final Map<String, Object> attributes;
	private final Map<String, Object> transientAttrs;
	private final ModuleRevision revision;

	ModuleCapability(String namespace, Map<String, String> directives, Map<String, Object> attributes, ModuleRevision revision) {
		this.namespace = namespace;
		this.directives = directives;
		this.attributes = attributes;
		this.transientAttrs = NativeNamespace.NATIVE_NAMESPACE.equals(namespace) ? new HashMap<>(0) : null;
		this.revision = revision;
	}

	@Override
	public ModuleRevision getRevision() {
		return revision;
	}

	@Override
	public String getNamespace() {
		return namespace;
	}

	@Override
	public Map<String, String> getDirectives() {
		return directives;
	}

	@Override
	public Map<String, Object> getAttributes() {
		if (transientAttrs == null)
			return attributes;
		Map<String, Object> result = new HashMap<>(transientAttrs);
		result.putAll(attributes);
		return Collections.unmodifiableMap(result);
	}

	Map<String, Object> getPersistentAttributes() {
		return attributes;
	}

	/**
	 * Only used by the system module for setting transient attributes associated
	 * with the {@link NativeNamespace osgi.native} namespace.
	 */
	public void setTransientAttrs(Map<String, ?> transientAttrs) {
		if (this.transientAttrs == null) {
			throw new UnsupportedOperationException(namespace + ": namespace does not support transient attributes."); //$NON-NLS-1$
		}
		if (!(getResource().getRevisions().getModule() instanceof SystemModule)) {
			throw new UnsupportedOperationException("Only allowed to set transient attributes for the system module: " + getResource()); //$NON-NLS-1$
		}
		this.transientAttrs.clear();
		this.transientAttrs.putAll(transientAttrs);
	}

	@Override
	public ModuleRevision getResource() {
		return revision;
	}

	@Override
	public String toString() {
		return namespace + ModuleContainer.toString(attributes, false) + ModuleContainer.toString(directives, true);
	}
}
