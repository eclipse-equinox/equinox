/*******************************************************************************
 * Copyright (c) 2024 ArSysOp.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Alexander Fedorov (ArSysOp) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.common.identity;

/**
 * Default implementation for {@link ImageIdentity}
 * 
 * Useful to declare images constants and then use them from both headless and
 * GUI part:
 * 
 * <pre>
 * public static final ImageIdentity EXAMPLE = new ImageIdentityRecord(//
 * 		new BundleIdentityRecord("org.examples.resources"), //
 * 		"icons/full/obj16/example.png", //
 * 		"example");
 * </pre>
 * 
 */
public record ImageIdentityRecord(BundleIdentity bundle, String path, String key) implements ImageIdentity {

	@Override
	public String id() {
		return bundle.symbolic() + ".image." + key; //$NON-NLS-1$
	}

	@Override
	public String url() {
		return "platform:/plugin/" + bundle.symbolic() + "/$nl$/" + path; //$NON-NLS-1$ //$NON-NLS-2$
	}

}
