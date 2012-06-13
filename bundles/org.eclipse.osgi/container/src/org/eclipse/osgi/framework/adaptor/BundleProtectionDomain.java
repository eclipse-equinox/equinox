/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.adaptor;

import java.security.*;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

/**
 * 
 * This is a specialized ProtectionDomain for a bundle.
 * <p>
 * This class is not intended to be extended by clients.
 * </p>
 * @since 3.1
 */
public class BundleProtectionDomain extends ProtectionDomain implements BundleReference {

	private volatile Bundle bundle;

	/**
	 * Constructs a special ProtectionDomain for a bundle.
	 * 
	 * @param permCollection
	 *            the PermissionCollection for the Bundle
	 * @deprecated use {@link #BundleProtectionDomain(PermissionCollection, CodeSource, Bundle)}
	 */
	public BundleProtectionDomain(PermissionCollection permCollection) {
		this(permCollection, null, null);
	}

	/**
	 * Constructs a special ProtectionDomain for a bundle.
	 * 
	 * @param permCollection
	 *            the PermissionCollection for the Bundle
	 * @param codeSource
	 *            the code source for this domain, may be null
	 * @param bundle
	 *            the bundle associated with this domain, may be null
	 */
	public BundleProtectionDomain(PermissionCollection permCollection, CodeSource codeSource, Bundle bundle) {
		super(codeSource, permCollection);
		this.bundle = bundle;
	}

	/**
	 * Sets the bundle object associated with this protection domain.
	 * The bundle can only be set once with either this method or with 
	 * the constructor.
	 * @param bundle the bundle object associated with this protection domain
	 */
	public void setBundle(Bundle bundle) {
		if (this.bundle != null || bundle == null)
			return;
		this.bundle = bundle;
	}

	public Bundle getBundle() {
		return bundle;
	}
}
