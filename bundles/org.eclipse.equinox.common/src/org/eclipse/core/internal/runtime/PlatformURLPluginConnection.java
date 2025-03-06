/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import java.io.IOException;
import java.net.URL;
import org.eclipse.core.internal.boot.PlatformURLConnection;
import org.eclipse.core.internal.boot.PlatformURLHandler;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * Platform URL support platform:/plugin/pluginId/ maps to
 * pluginDescriptor.getInstallURLInternal()
 */
public class PlatformURLPluginConnection extends PlatformURLConnection {

	private static boolean isRegistered = false;
	public static final String PLUGIN = "plugin"; //$NON-NLS-1$

	/*
	 * Constructor for the class.
	 */
	public PlatformURLPluginConnection(URL url) {
		super(url);
	}

	@Override
	protected boolean allowCaching() {
		return true;
	}

	/*
	 * spec - /plugin/com.example/META-INF/MANIFEST.MF originalURL - used only for
	 * exception messages result[0] - Bundle (e.g. com.example) result[1] - String
	 * (path) (e.g. META-INF/MANIFEST.MF)
	 */
	public static Object[] parse(String spec, URL originalURL) throws IOException {
		Object[] result = new Object[2];
		if (spec.startsWith("/")) { //$NON-NLS-1$
			spec = spec.substring(1);
		}
		if (!spec.startsWith(PLUGIN)) {
			throw new IOException(NLS.bind(CommonMessages.url_badVariant, originalURL));
		}
		int ix = spec.indexOf('/', PLUGIN.length() + 1);
		String ref = ix == -1 ? spec.substring(PLUGIN.length() + 1) : spec.substring(PLUGIN.length() + 1, ix);
		String id = getId(ref);
		Activator activator = Activator.getDefault();
		if (activator == null) {
			throw new IOException(CommonMessages.activator_not_available);
		}
		Bundle bundle = activator.getBundle(id);
		if (bundle == null) {
			throw new IOException(NLS.bind(CommonMessages.url_resolvePlugin, id));
		}
		result[0] = bundle;
		result[1] = (ix == -1 || (ix + 1) >= spec.length()) ? "/" : spec.substring(ix + 1); //$NON-NLS-1$
		return result;
	}

	@Override
	protected URL resolve() throws IOException {
		String spec = url.getFile().trim();
		Object[] obj = parse(spec, url);
		Bundle b = (Bundle) obj[0];
		String path = (String) obj[1];
		URL result = b.getEntry(path);
		if (result != null || "/".equals(path)) { //$NON-NLS-1$
			return result;
		}
		// try resolving the path through the classloader
		result = b.getResource(path);
		if (result != null) {
			return result;
		}
		// if the result is null then force the creation of a URL that will throw
		// FileNotFoundExceptions
		return new URL(b.getEntry("/"), path); //$NON-NLS-1$
	}

	public static void startup() {
		// register connection type for platform:/plugin handling
		if (isRegistered) {
			return;
		}
		PlatformURLHandler.register(PLUGIN, PlatformURLPluginConnection.class);
		isRegistered = true;
	}
}
