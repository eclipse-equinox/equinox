/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype.impl;

import org.eclipse.osgi.util.NLS;

public class Designate {
	public static class Builder {
		String bundle;
		String factoryPid;
		boolean merge;
		ObjectClassDefinitionImpl ocd;
		boolean optional;
		String pid;

		public Builder(ObjectClassDefinitionImpl ocd) {
			if (ocd == null) {
				throw new IllegalArgumentException(NLS.bind(MetaTypeMsg.MISSING_REQUIRED_PARAMETER, "ocd")); //$NON-NLS-1$
			}
			this.ocd = ocd;
		}

		public Designate build() {
			return new Designate(this);
		}

		public Builder bundle(String value) {
			bundle = value;
			return this;
		}

		public Builder factoryPid(String value) {
			factoryPid = value;
			return this;
		}

		public Builder merge(boolean value) {
			merge = value;
			return this;
		}

		public Builder optional(boolean value) {
			optional = value;
			return this;
		}

		public Builder pid(String value) {
			pid = value;
			return this;
		}
	}

	private final String bundle;
	private final String factoryPid;
	private final boolean merge;
	private final ObjectClassDefinitionImpl ocd;
	private final boolean optional;
	private final String pid;

	Designate(Builder b) {
		bundle = b.bundle;
		factoryPid = b.factoryPid;
		merge = b.merge;
		ocd = b.ocd;
		optional = b.optional;
		pid = b.pid;
	}

	public String getBundle() {
		return bundle;
	}

	public String getFactoryPid() {
		return factoryPid;
	}

	public boolean isFactory() {
		return factoryPid != null && factoryPid.length() != 0;
	}

	public boolean isMerge() {
		return merge;
	}

	public ObjectClassDefinitionImpl getObjectClassDefinition() {
		return ocd;
	}

	public boolean isOptional() {
		return optional;
	}

	public String getPid() {
		return pid;
	}
}
