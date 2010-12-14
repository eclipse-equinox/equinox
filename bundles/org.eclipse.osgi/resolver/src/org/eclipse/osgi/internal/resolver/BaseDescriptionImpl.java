/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.util.*;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.*;

abstract class BaseDescriptionImpl implements BaseDescription {

	protected final Object monitor = new Object();

	private volatile String name;

	private volatile Version version;

	public String getName() {
		return name;
	}

	public Version getVersion() {
		synchronized (this.monitor) {
			if (version == null)
				return Version.emptyVersion;
			return version;
		}
	}

	protected void setName(String name) {
		this.name = name;
	}

	protected void setVersion(Version version) {
		this.version = version;
	}

	static String toString(Map<String, Object> map, boolean directives) {
		if (map.size() == 0)
			return ""; //$NON-NLS-1$
		String assignment = directives ? ":=" : "="; //$NON-NLS-1$//$NON-NLS-2$
		Set<Map.Entry<String, Object>> set = map.entrySet();
		StringBuffer sb = new StringBuffer();
		for (Map.Entry<String, Object> entry : set) {
			sb.append("; "); //$NON-NLS-1$
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof List) {
				@SuppressWarnings("unchecked")
				List<Object> list = (List<Object>) value;
				if (list.size() == 0)
					continue;
				Object component = list.get(0);
				String className = component.getClass().getName();
				String type = className.substring(className.lastIndexOf('.') + 1);
				sb.append(key).append(':').append("List<").append(type).append(">").append(assignment).append('"'); //$NON-NLS-1$ //$NON-NLS-2$
				for (Object object : list)
					sb.append(object).append(',');
				sb.setLength(sb.length() - 1);
				sb.append('"');
			} else {
				String type = ""; //$NON-NLS-1$
				if (!(value instanceof String)) {
					String className = value.getClass().getName();
					type = ":" + className.substring(className.lastIndexOf('.') + 1); //$NON-NLS-1$
				}
				sb.append(key).append(type).append(assignment).append('"').append(value).append('"');
			}
		}
		return sb.toString();
	}

	String getInternalNameSpace() {
		return null;
	}

	WiredCapability getWiredCapability(String namespace) {
		if (namespace == null)
			namespace = getInternalNameSpace();
		if (namespace == null)
			return null;

		BundleDescription supplier = getSupplier();
		BundleWiring wiring = supplier.getBundleWiring();
		if (wiring == null)
			return null;
		return new BaseWiredCapability(namespace, wiring);
	}

	public Capability getCapability() {
		return getCapability(null);
	}

	Capability getCapability(String namespace) {
		if (namespace == null)
			namespace = getInternalNameSpace();
		if (namespace == null)
			return null;
		return new BaseCapability(namespace);
	}

	class BaseCapability implements Capability {
		private final String namespace;

		public BaseCapability(String namespace) {
			super();
			this.namespace = namespace;
		}

		public BundleRevision getProviderRevision() {
			return getSupplier();
		}

		public String getNamespace() {
			return namespace;
		}

		public Map<String, String> getDirectives() {
			return getDeclaredDirectives();
		}

		public Map<String, Object> getAttributes() {
			Map<String, Object> attrs = getDeclaredAttributes();
			String internalName = BaseDescriptionImpl.this.getInternalNameSpace();
			if (namespace.equals(internalName))
				return attrs;
			// we are doing an alias, must remove internal Name and add alias
			attrs = new HashMap<String, Object>(attrs);
			Object nameValue = attrs.remove(internalName);
			if (nameValue != null)
				attrs.put(namespace, nameValue);
			return Collections.unmodifiableMap(attrs);
		}

		public int hashCode() {
			return System.identityHashCode(BaseDescriptionImpl.this);
		}

		protected BaseDescriptionImpl getBaseDescription() {
			return BaseDescriptionImpl.this;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof BaseCapability))
				return false;
			return (((BaseCapability) obj).getBaseDescription() == BaseDescriptionImpl.this) && namespace.equals(((BaseCapability) obj).getNamespace());
		}

		public String toString() {
			return getNamespace() + BaseDescriptionImpl.toString(getAttributes(), false);
		}
	}

	class BaseWiredCapability extends BaseCapability implements WiredCapability {
		private final BundleWiring originalWiring;

		public BaseWiredCapability(String namespace, BundleWiring originalWiring) {
			super(namespace);
			this.originalWiring = originalWiring;
		}

		public Collection<BundleWiring> getRequirerWirings() {
			BundleWiring wiring = getProviderWiring();
			if (wiring == null)
				return null;
			BundleDescription supplier = getSupplier();
			BundleDescription[] dependents = supplier.getDependents();
			Collection<BundleWiring> requirers = new ArrayList<BundleWiring>();
			if (Capability.HOST_CAPABILITY.equals(getNamespace())) {
				// special casing osgi.host capability.
				// this is needed because the host capability is manufactured only for 
				// representation in the wiring API.  We need to represent a host wiring
				// as requiring its own osgi.host capability if it has attached fragments
				List<BundleRevision> fragments = wiring.getFragmentRevisions();
				if (fragments != null && fragments.size() > 0)
					// found at least one fragment add the host wiring as a requirer and return
					requirers.add(wiring);
			}

			for (BundleDescription dependent : dependents) {
				BundleWiring dependentWiring = dependent.getBundleWiring();
				if (dependentWiring == null) // fragments have no wiring
					continue;
				List<WiredCapability> namespace = dependentWiring.getRequiredCapabilities(getNamespace());
				if (namespace == null)
					continue;
				if (namespace.contains(this))
					requirers.add(dependentWiring);
			}
			return requirers;
		}

		public BundleWiring getProviderWiring() {
			return originalWiring.isInUse() ? originalWiring : null;
		}

		public int hashCode() {
			return System.identityHashCode(BaseDescriptionImpl.this) ^ System.identityHashCode(originalWiring);
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof BaseWiredCapability))
				return false;
			BaseWiredCapability other = (BaseWiredCapability) obj;
			return (other.originalWiring == this.originalWiring) && super.equals(obj);
		}
	}
}
