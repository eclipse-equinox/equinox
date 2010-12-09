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

	WiredCapability getWiredCapability() {
		synchronized (this.monitor) {
			BundleDescription supplier = getSupplier();
			BundleWiring wiring = supplier.getBundleWiring();
			if (wiring == null)
				return null;
			return new BaseWiredCapability(wiring);
		}
	}

	public Capability getCapability() {
		return new BaseCapability();
	}

	class BaseCapability implements Capability {
		public BundleRevision getProviderRevision() {
			return getSupplier();
		}

		public String getNamespace() {
			return getInternalNameSpace();
		}

		public Map<String, String> getDirectives() {
			return getDeclaredDirectives();
		}

		public Map<String, Object> getAttributes() {
			return getDeclaredAttributes();
		}

		public int hashCode() {
			return System.identityHashCode(BaseDescriptionImpl.this);
		}

		private BaseDescriptionImpl getBaseDescription() {
			return BaseDescriptionImpl.this;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof Capability))
				return false;
			if (obj instanceof BaseCapability)
				return (((BaseCapability) obj).getBaseDescription() == BaseDescriptionImpl.this);
			Capability other = (Capability) obj;
			String otherName = other.getNamespace();
			if (!getProviderRevision().equals(other.getProviderRevision()))
				return false;
			if (otherName == null ? getNamespace() != null : !otherName.equals(getNamespace()))
				return false;
			if (!getAttributes().equals(other.getAttributes()))
				return false;
			if (!getDirectives().equals(other.getDirectives()))
				return false;
			return true;
		}

		public String toString() {
			return getNamespace() + BaseDescriptionImpl.toString(getAttributes(), false);
		}
	}

	class BaseWiredCapability extends BaseCapability implements WiredCapability {
		private final BundleWiring originalWiring;

		public BaseWiredCapability(BundleWiring originalWiring) {
			this.originalWiring = originalWiring;
		}

		public Collection<BundleWiring> getRequirerWirings() {
			BundleWiring wiring = getProviderWiring();
			if (wiring == null)
				return null;
			BundleDescription supplier = getSupplier();
			BundleDescription[] dependents = supplier.getDependents();
			Collection<BundleWiring> requirers = new ArrayList<BundleWiring>();
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

		BaseDescriptionImpl getImpl() {
			return BaseDescriptionImpl.this;
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
			return (other.originalWiring == this.originalWiring) && (this.getImpl() == other.getImpl());
		}
	}
}
