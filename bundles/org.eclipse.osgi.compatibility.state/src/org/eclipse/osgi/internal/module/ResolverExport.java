/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
package org.eclipse.osgi.internal.module;

import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.osgi.framework.Constants;

/*
 * A companion to ExportPackageDescription from the state used while resolving.
 */
public class ResolverExport extends VersionSupplier {
	private final ResolverBundle resolverBundle;

	ResolverExport(ResolverBundle resolverBundle, ExportPackageDescription epd) {
		super(epd);
		this.resolverBundle = resolverBundle;
	}

	public ExportPackageDescription getExportPackageDescription() {
		return (ExportPackageDescription) base;
	}

	@Override
	public BundleDescription getBundleDescription() {
		return getExportPackageDescription().getExporter();
	}

	ResolverBundle getExporter() {
		return resolverBundle;
	}

	@Override
	ResolverBundle getResolverBundle() {
		return getExporter();
	}

	String[] getUsesDirective() {
		return (String[]) getExportPackageDescription().getDirective(Constants.USES_DIRECTIVE);
	}
}
