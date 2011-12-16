/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 * platform:/plugin/pluginId/		maps to pluginDescriptor.getInstallURLInternal()
 */
public class PlatformURLPluginConnection extends PlatformURLConnection {

	private Bundle target = null;
	private static boolean isRegistered = false;
	public static final String PLUGIN = "plugin"; //$NON-NLS-1$

	/*
	 * Constructor for the class.
	 */
	public PlatformURLPluginConnection(URL url) {
		super(url);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.url.PlatformURLConnection#allowCaching()
	 */
	protected boolean allowCaching() {
		return true;
	}

	/*
	 * spec - /plugin/com.example/META-INF/MANIFEST.MF
	 * originalURL - used only for exception messages
	 * result[0] - Bundle (e.g. com.example)
	 * result[1] - String (path) (e.g. META-INF/MANIFEST.MF)
	 */
	public static Object[] parse(String spec, URL originalURL) throws IOException {
		Object[] result = new Object[2];
		if (spec.startsWith("/")) //$NON-NLS-1$
			spec = spec.substring(1);
		if (!spec.startsWith(PLUGIN))
			throw new IOException(NLS.bind(CommonMessages.url_badVariant, originalURL));
		int ix = spec.indexOf("/", PLUGIN.length() + 1); //$NON-NLS-1$
		String ref = ix == -1 ? spec.substring(PLUGIN.length() + 1) : spec.substring(PLUGIN.length() + 1, ix);
		String id = getId(ref);
		Activator activator = Activator.getDefault();
		if (activator == null)
			throw new IOException(CommonMessages.activator_not_available);
		Bundle bundle = activator.getBundle(id);
		if (bundle == null)
			throw new IOException(NLS.bind(CommonMessages.url_resolvePlugin, originalURL));
		result[0] = bundle;
		result[1] = (ix == -1 || (ix + 1) >= spec.length()) ? "/" : spec.substring(ix + 1); //$NON-NLS-1$
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.url.PlatformURLConnection#resolve()
	 */
	protected URL resolve() throws IOException {
		String spec = url.getFile().trim();
		Object[] obj = parse(spec, url);
		Bundle b = (Bundle) obj[0];
		String path = (String) obj[1];
		URL result = b.getEntry(path);
		if (result != null || "/".equals(path)) //$NON-NLS-1$
			return result;
		// try resolving the path through the classloader
		result = b.getResource(path);
		if (result != null)
			return result;
		// if the result is null then force the creation of a URL that will throw FileNotFoundExceptions
		return new URL(b.getEntry("/"), path); //$NON-NLS-1$
	}

	public static void startup() {
		// register connection type for platform:/plugin handling
		if (isRegistered)
			return;
		PlatformURLHandler.register(PLUGIN, PlatformURLPluginConnection.class);
		isRegistered = true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.internal.url.PlatformURLConnection#getAuxillaryURLs()
	 */
	public URL[] getAuxillaryURLs() throws IOException {
		if (target == null) {
			String spec = url.getFile().trim();
			if (spec.startsWith("/")) //$NON-NLS-1$
				spec = spec.substring(1);
			if (!spec.startsWith(PLUGIN))
				throw new IOException(NLS.bind(CommonMessages.url_badVariant, url));
			int ix = spec.indexOf("/", PLUGIN.length() + 1); //$NON-NLS-1$
			String ref = ix == -1 ? spec.substring(PLUGIN.length() + 1) : spec.substring(PLUGIN.length() + 1, ix);
			String id = getId(ref);
			Activator activator = Activator.getDefault();
			if (activator == null)
				throw new IOException(CommonMessages.activator_not_available);
			target = activator.getBundle(id);
			if (target == null)
				throw new IOException(NLS.bind(CommonMessages.url_resolvePlugin, url));
		}
		Bundle[] fragments = Activator.getDefault().getFragments(target);
		int fragmentLength = (fragments == null) ? 0 : fragments.length;
		if (fragmentLength == 0)
			return null;
		URL[] result = new URL[fragmentLength];
		for (int i = 0; i < fragmentLength; i++)
			result[i] = fragments[i].getEntry("/"); //$NON-NLS-1$
		return result;
	}
}
