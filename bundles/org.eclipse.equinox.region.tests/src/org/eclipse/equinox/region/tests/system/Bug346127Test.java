/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.equinox.region.tests.system;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import org.eclipse.equinox.region.Region;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.url.*;

/**
 * Region Digraph should not impose a policy of always using reference: URLs.
 * This policy is further exasperated by the fact that reference: URLs only
 * work if the embedded URL uses the file: protocol. It should be up to the
 * client to decide which protocol to use. Digraph should support any URL for
 * which there is a compatible URL Handler.
 */
public class Bug346127Test extends AbstractRegionSystemTest {

	@Test
	public void testBug346127() throws Exception {
		String location = bundleInstaller.getBundleLocation(PP1);
		location = "regiondigraphtest:" + location;
		Region region = digraph.getRegion(getContext().getBundle());
		try {
			Bundle bundle = region.installBundle(location);
			assertNotNull(bundle);
		} catch (BundleException e) {
			e.printStackTrace();
			fail("Failed to install a bundle into a region using a custom, non-file URL: " + e.getMessage());
		}
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		properties.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] {"regiondigraphtest"});
		getContext().registerService(URLStreamHandlerService.class.getName(), new AbstractURLStreamHandlerService() {
			@Override
			public URLConnection openConnection(URL u) {
				return new URLConnection(u) {
					@Override
					public void connect() {
						// noop
					}

					@Override
					public InputStream getInputStream() throws IOException {
						String s = getURL().toString();
						s = s.substring(s.indexOf("regiondigraphtest:") + "regiondigraphtest:".length());
						return new URL(s).openStream();
					}
				};
			}
		}, properties);
	}
}
