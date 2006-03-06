/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.io.IOException;
import java.net.URL;
import org.eclipse.core.internal.boot.PlatformURLConnection;
import org.eclipse.core.internal.boot.PlatformURLHandler;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * Platform URL support
 * platform:/fragment/fragmentId/		maps to fragmentDescriptor.getInstallURLInternal()
 */
public class PlatformURLFragmentConnection extends PlatformURLConnection {

	private Bundle target = null;
	private static boolean isRegistered = false;
	public static final String FRAGMENT = "fragment"; //$NON-NLS-1$

	/*
	 * Constructor for the class.
	 */
	public PlatformURLFragmentConnection(URL url) {
		super(url);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.url.PlatformURLConnection#allowCaching()
	 */
	protected boolean allowCaching() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.url.PlatformURLConnection#resolve()
	 */
	protected URL resolve() throws IOException {
		String spec = url.getFile().trim();
		if (spec.startsWith("/")) //$NON-NLS-1$
			spec = spec.substring(1);
		if (!spec.startsWith(FRAGMENT))
			throw new IOException(NLS.bind(CommonMessages.url_badVariant, url));
		int ix = spec.indexOf("/", FRAGMENT.length() + 1); //$NON-NLS-1$
		String ref = ix == -1 ? spec.substring(FRAGMENT.length() + 1) : spec.substring(FRAGMENT.length() + 1, ix);
		String id = getId(ref);
		Activator activator = Activator.getDefault();
		if (activator == null)
			throw new IOException(CommonMessages.activator_not_available);
		target = activator.getBundle(id);
		if (target == null)
			throw new IOException(NLS.bind(CommonMessages.url_resolveFragment, url));
		URL result = target.getEntry("/"); //$NON-NLS-1$
		if (ix == -1 || (ix + 1) >= spec.length())
			return result;
		return new URL(result, spec.substring(ix + 1));
	}

	public static void startup() {
		// register connection type for platform:/fragment handling
		if (isRegistered)
			return;
		PlatformURLHandler.register(FRAGMENT, PlatformURLFragmentConnection.class);
		isRegistered = true;
	}
}
