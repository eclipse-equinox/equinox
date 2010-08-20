/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.module;

import java.util.Map;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.wiring.BundleRevision;

public class GenericCapability extends VersionSupplier {
	ResolverBundle resolverBundle;

	GenericCapability(ResolverBundle resolverBundle, BaseDescription base) {
		super(base);
		this.resolverBundle = resolverBundle;
	}

	public BundleDescription getBundleDescription() {
		return getBaseDescription().getSupplier();
	}

	GenericDescription getGenericDescription() {
		return (GenericDescription) getBaseDescription();
	}

	public boolean isFromFragment() {
		return resolverBundle.isFragment();
	}

	public ResolverBundle getResolverBundle() {
		return resolverBundle;
	}

	public String getNamespace() {
		return getGenericDescription().getType();
	}

	public Map<String, String> getDirectives() {
		return getGenericDescription().getDeclaredDirectives();
	}

	public Map<String, Object> getAttributes() {
		return getGenericDescription().getDeclaredAttributes();
	}

	public BundleRevision getProviderRevision() {
		return resolverBundle;
	}
}
