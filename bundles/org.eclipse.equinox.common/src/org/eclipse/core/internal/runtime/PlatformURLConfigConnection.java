/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.core.internal.runtime;

import java.io.*;
import java.net.URL;
import java.net.UnknownServiceException;
import org.eclipse.core.internal.boot.PlatformURLConnection;
import org.eclipse.core.internal.boot.PlatformURLHandler;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;

public class PlatformURLConfigConnection extends PlatformURLConnection {
	private static final String FILE_PROTOCOL = "file"; //$NON-NLS-1$
	private static boolean isRegistered = false;
	public static final String CONFIG = "config"; //$NON-NLS-1$

	private boolean parentConfiguration = false;

	/*
	 * Constructor for the class.
	 */
	public PlatformURLConfigConnection(URL url) {
		super(url);
	}

	@Override
	protected URL resolve() throws IOException {
		String spec = url.getFile().trim();
		if (spec.startsWith("/")) //$NON-NLS-1$
			spec = spec.substring(1);
		if (!spec.startsWith(CONFIG))
			throw new IOException(NLS.bind(CommonMessages.url_badVariant, url.toString()));
		String path = spec.substring(CONFIG.length() + 1);
		// resolution takes parent configuration into account (if it exists)
		Activator activator = Activator.getDefault();
		if (activator == null)
			throw new IOException(CommonMessages.activator_not_available);
		Location localConfig = activator.getConfigurationLocation();
		Location parentConfig = localConfig.getParentLocation();
		// assume we will find the file locally
		URL localURL = new URL(localConfig.getURL(), path);
		if (!FILE_PROTOCOL.equals(localURL.getProtocol()) || parentConfig == null)
			// we only support cascaded file: URLs
			return localURL;
		File localFile = new File(localURL.getPath());
		if (localFile.exists())
			// file exists in local configuration
			return localURL;
		// try to find in the parent configuration
		URL parentURL = new URL(parentConfig.getURL(), path);
		if (FILE_PROTOCOL.equals(parentURL.getProtocol())) {
			// we only support cascaded file: URLs
			File parentFile = new File(parentURL.getPath());
			if (parentFile.exists()) {
				// parent has the location
				parentConfiguration = true;
				return parentURL;
			}
		}
		return localURL;
	}

	public static void startup() {
		// register connection type for platform:/config handling
		if (isRegistered)
			return;
		PlatformURLHandler.register(CONFIG, PlatformURLConfigConnection.class);
		isRegistered = true;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		if (parentConfiguration || Activator.getDefault() == null
				|| Activator.getDefault().getConfigurationLocation().isReadOnly())
			throw new UnknownServiceException(NLS.bind(CommonMessages.url_noOutput, url));
		// This is not optimal but connection is a private instance variable in the
		// super-class.
		URL resolved = getResolvedURL();
		if (resolved != null) {
			String fileString = resolved.getFile();
			if (fileString != null) {
				File file = new File(fileString);
				String parent = file.getParent();
				if (parent != null)
					new File(parent).mkdirs();
				return new FileOutputStream(file);
			}
		}
		return null;
	}
}
