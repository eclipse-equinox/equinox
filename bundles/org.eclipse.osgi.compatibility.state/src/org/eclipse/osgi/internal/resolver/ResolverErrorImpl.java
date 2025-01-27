/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
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
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;

public final class ResolverErrorImpl implements ResolverError {
	private final BundleDescriptionImpl bundle;
	private final int type;
	private final String data;
	private final VersionConstraint unsatisfied;

	public ResolverErrorImpl(BundleDescriptionImpl bundle, int type, String data, VersionConstraint unsatisfied) {
		this.bundle = bundle;
		this.data = data;
		this.type = type;
		this.unsatisfied = unsatisfied;
	}

	@Override
	public BundleDescription getBundle() {
		return bundle;
	}

	@Override
	public int getType() {
		return type;
	}

	@Override
	public String getData() {
		return data;
	}

	@Override
	public VersionConstraint getUnsatisfiedConstraint() {
		return unsatisfied;
	}

	@Override
	public String toString() {
		switch (getType()) {
			case ResolverError.EXPORT_PACKAGE_PERMISSION :
			case ResolverError.IMPORT_PACKAGE_PERMISSION :
			case ResolverError.REQUIRE_BUNDLE_PERMISSION :
			case ResolverError.PROVIDE_BUNDLE_PERMISSION :
			case ResolverError.FRAGMENT_BUNDLE_PERMISSION :
			case ResolverError.HOST_BUNDLE_PERMISSION :
			case ResolverError.REQUIRE_CAPABILITY_PERMISSION :
			case ResolverError.PROVIDE_CAPABILITY_PERMISSION :
				return NLS.bind(StateMsg.RES_ERROR_MISSING_PERMISSION, getData());
			case ResolverError.MISSING_IMPORT_PACKAGE :
			case ResolverError.MISSING_REQUIRE_BUNDLE :
			case ResolverError.MISSING_FRAGMENT_HOST :
			case ResolverError.MISSING_EXECUTION_ENVIRONMENT :
			case ResolverError.MISSING_GENERIC_CAPABILITY :
				return NLS.bind(StateMsg.RES_ERROR_MISSING_CONSTRAINT, getData());
			case ResolverError.FRAGMENT_CONFLICT :
				return NLS.bind(StateMsg.RES_ERROR_FRAGMENT_CONFLICT, getData());
			case ResolverError.IMPORT_PACKAGE_USES_CONFLICT :
			case ResolverError.REQUIRE_BUNDLE_USES_CONFLICT :
				return NLS.bind(StateMsg.RES_ERROR_USES_CONFLICT, getData());
			case ResolverError.SINGLETON_SELECTION :
				return NLS.bind(StateMsg.RES_ERROR_SINGLETON_CONFLICT, getData());
			case ResolverError.PLATFORM_FILTER :
				return NLS.bind(StateMsg.RES_ERROR_PLATFORM_FILTER, getData());
			case ResolverError.NO_NATIVECODE_MATCH :
				return NLS.bind(StateMsg.RES_ERROR_NO_NATIVECODE_MATCH, getData());
			case ResolverError.INVALID_NATIVECODE_PATHS :
				return NLS.bind(StateMsg.RES_ERROR_NATIVECODE_PATH_INVALID, getData());
			case ResolverError.DISABLED_BUNDLE :
				return NLS.bind(StateMsg.RES_ERROR_DISABLEDBUNDLE, getData());
			default :
				return StateMsg.RES_ERROR_UNKNOWN;
		}
	}
}
