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
package org.eclipse.core.internal.boot;

import java.io.IOException;
import java.net.URL;
import org.eclipse.core.internal.runtime.CommonMessages;
import org.eclipse.osgi.util.NLS;

/**
 * Platform URL support
 * platform:/base/	maps to platform installation location
 */
public class PlatformURLBaseConnection extends PlatformURLConnection {

	// platform/ protocol
	public static final String PLATFORM = "base"; //$NON-NLS-1$
	public static final String PLATFORM_URL_STRING = PlatformURLHandler.PROTOCOL + PlatformURLHandler.PROTOCOL_SEPARATOR + "/" + PLATFORM + "/"; //$NON-NLS-1$ //$NON-NLS-2$

	private static URL installURL;

	/*
	 * Constructor for the class.
	 */
	public PlatformURLBaseConnection(URL url) {
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
		if (!spec.startsWith(PLATFORM + "/")) { //$NON-NLS-1$
			String message = NLS.bind(CommonMessages.url_badVariant, url);
			throw new IOException(message);
		}
		return spec.length() == PLATFORM.length() + 1 ? installURL : new URL(installURL, spec.substring(PLATFORM.length() + 1));
	}

	public static void startup(URL url) {
		// register connection type for platform:/base/ handling
		if (installURL != null)
			return;
		installURL = url;
		PlatformURLHandler.register(PLATFORM, PlatformURLBaseConnection.class);
	}
}
