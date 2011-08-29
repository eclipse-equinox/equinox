/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
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
import java.util.Map.Entry;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.extras.DescriptionReference;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;

public abstract class BaseDescriptionImpl implements BaseDescription {

	protected final Object monitor = new Object();

	private volatile String name;

	private volatile Version version;

	private volatile Object userObject;

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

	static <V> String toString(Map<String, V> map, boolean directives) {
		if (map.size() == 0)
			return ""; //$NON-NLS-1$
		String assignment = directives ? ":=" : "="; //$NON-NLS-1$//$NON-NLS-2$
		Set<Entry<String, V>> set = map.entrySet();
		StringBuffer sb = new StringBuffer();
		for (Entry<String, V> entry : set) {
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

	public BaseDescription getFragmentDeclaration() {
		return null;
	}

	public BundleCapability getCapability() {
		return getCapability(null);
	}

	BundleCapability getCapability(String namespace) {
		BaseDescriptionImpl fragmentDeclaration = (BaseDescriptionImpl) getFragmentDeclaration();
		if (fragmentDeclaration != null)
			return fragmentDeclaration.getCapability(namespace);
		if (namespace == null)
			namespace = getInternalNameSpace();
		if (namespace == null)
			return null;
		return new BaseCapability(namespace);
	}

	public Object getUserObject() {
		return userObject;
	}

	public void setUserObject(Object userObject) {
		this.userObject = userObject;
	}

	class BaseCapability implements BundleCapability, DescriptionReference {
		private final String namespace;

		public BaseCapability(String namespace) {
			super();
			this.namespace = namespace;
		}

		public BundleRevision getRevision() {
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
			return getNamespace() + BaseDescriptionImpl.toString(getAttributes(), false) + BaseDescriptionImpl.toString(getDirectives(), true);
		}

		public BundleRevision getResource() {
			return getRevision();
		}

		public BaseDescription getDescription() {
			return BaseDescriptionImpl.this;
		}
	}
}
