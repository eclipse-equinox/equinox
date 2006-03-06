/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.io.*;
import java.net.URL;
import org.eclipse.core.internal.boot.PlatformURLConnection;
import org.eclipse.core.internal.boot.PlatformURLHandler;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

public class PlatformURLMetaConnection extends PlatformURLConnection {
	private Bundle target = null;
	private static boolean isRegistered = false;
	public static final String META = "meta"; //$NON-NLS-1$

	/*
	 * Constructor for the class.
	 */
	public PlatformURLMetaConnection(URL url) {
		super(url);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.url.PlatformURLConnection#resolve()
	 */
	protected URL resolve() throws IOException {
		String spec = url.getFile().trim();
		if (spec.startsWith("/")) //$NON-NLS-1$
			spec = spec.substring(1);
		if (!spec.startsWith(META))
			throw new IOException(NLS.bind(CommonMessages.url_badVariant, url.toString()));
		int ix = spec.indexOf("/", META.length() + 1); //$NON-NLS-1$
		String ref = ix == -1 ? spec.substring(META.length() + 1) : spec.substring(META.length() + 1, ix);
		String id = getId(ref);
		Activator activator = Activator.getDefault();
		if (activator == null)
			throw new IOException(CommonMessages.activator_not_available);
		target = activator.getBundle(id);
		if (target == null)
			throw new IOException(NLS.bind(CommonMessages.url_resolvePlugin, url.toString()));
		IPath path = MetaDataKeeper.getMetaArea().getStateLocation(target);
		if (ix != -1 || (ix + 1) <= spec.length())
			path = path.append(spec.substring(ix + 1));
		return path.toFile().toURL();
	}

	public static void startup() {
		// register connection type for platform:/meta handling
		if (isRegistered)
			return;
		PlatformURLHandler.register(META, PlatformURLMetaConnection.class);
		isRegistered = true;
	}

	/* (non-Javadoc)
	 * @see java.net.URLConnection#getOutputStream()
	 */
	public OutputStream getOutputStream() throws IOException {
		//This is not optimal but connection is a private instance variable in super.
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
