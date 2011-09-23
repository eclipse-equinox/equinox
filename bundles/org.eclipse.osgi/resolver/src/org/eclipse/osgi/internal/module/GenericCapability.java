/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.module;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.GenericDescription;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Constants;

public class GenericCapability extends VersionSupplier {
	final ResolverBundle resolverBundle;
	final String[] uses;
	final boolean effective;

	GenericCapability(ResolverBundle resolverBundle, GenericDescription base) {
		super(base);
		this.resolverBundle = resolverBundle;
		String usesDirective = base.getDeclaredDirectives().get(Constants.USES_DIRECTIVE);
		uses = ManifestElement.getArrayFromList(usesDirective);
		String effectiveDirective = base.getDeclaredDirectives().get(Constants.EFFECTIVE_DIRECTIVE);
		effective = effectiveDirective == null || Constants.EFFECTIVE_RESOLVE.equals(effectiveDirective);
	}

	public BundleDescription getBundleDescription() {
		return getBaseDescription().getSupplier();
	}

	GenericDescription getGenericDescription() {
		return (GenericDescription) getBaseDescription();
	}

	public ResolverBundle getResolverBundle() {
		return resolverBundle;
	}

	String[] getUsesDirective() {
		return uses;
	}

	boolean isEffective() {
		return effective;
	}
}
