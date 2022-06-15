/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
 *     Karsten Thoms (itemis) - Consider devmode for effective status computation
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

	GenericCapability(ResolverBundle resolverBundle, GenericDescription base, boolean developmentMode) {
		super(base);
		this.resolverBundle = resolverBundle;
		String usesDirective = base.getDeclaredDirectives().get(Constants.USES_DIRECTIVE);
		uses = ManifestElement.getArrayFromList(usesDirective);
		String effectiveDirective = base.getDeclaredDirectives().get(Constants.EFFECTIVE_DIRECTIVE);
		effective = effectiveDirective == null || Constants.EFFECTIVE_RESOLVE.equals(effectiveDirective) || (Constants.EFFECTIVE_ACTIVE.equals(effectiveDirective) && developmentMode);
	}

	@Override
	public BundleDescription getBundleDescription() {
		return getBaseDescription().getSupplier();
	}

	GenericDescription getGenericDescription() {
		return (GenericDescription) getBaseDescription();
	}

	@Override
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
