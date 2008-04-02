/*******************************************************************************
 * Copyright (c) 1997-2007 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.internal.ds.model.DeclarationParser;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentConstants;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public abstract class ComponentStorage {

	private final DeclarationParser parser = new DeclarationParser();

	/**
	 * This method will load the component definitions from a bundle. The
	 * returned value should contain vector with 'ServiceComponent' elements.
	 * 
	 * @param bundleID
	 *            id of the bundle, which is processed and if contains a
	 *            component definitions - it's xml definition file got parsed.
	 * @return <code>null</code> if there are no component definitions.
	 */
	public abstract Vector loadComponentDefinitions(long bundleID);

	public abstract void deleteComponentDefinitions(long bundleID) throws Exception;

	public abstract void stop();

	protected Vector parseXMLDeclaration(Bundle bundle) throws Exception {
		Vector components = new Vector();
		Dictionary headers = bundle.getHeaders(null);
		String header = (String) headers.get(ComponentConstants.SERVICE_COMPONENT);

		if (header != null) {
			StringTokenizer tok = new StringTokenizer(header, ",");
			// the parser is not thread safe!!!
			synchronized (parser) {
				// process all definition file
				while (tok.hasMoreElements()) {
					String definitionFile = tok.nextToken().trim();
					int ind = definitionFile.lastIndexOf('/');
					String path = ind != -1 ? definitionFile.substring(0, ind) : "/";
					InputStream is = null;

					Enumeration urls = bundle.findEntries(path, ind != -1 ? definitionFile.substring(ind + 1) : definitionFile, false);
					if (urls == null || !urls.hasMoreElements()) {
						Activator.log.error("Component definition XMLs not found in bundle " + bundle.getSymbolicName() + ". The component header value is " + definitionFile, null);
						continue;
					}

					// illegal components are ignored, but framework event is posted for
					// them; however, it will continue and try to load any legal
					// definitions
					URL url;
					while (urls.hasMoreElements()) {
						url = (URL) urls.nextElement();
						if (Activator.DEBUG) {
							Activator.log.debug(0, 10017, url.toString(), null, false);
							////Activator.log.debug("ComponentStorage.parseXMLDeclaration(): loading " + definitionFile, null);
						}
						try {
							is = url.openStream();
							if (is == null) {
								Activator.log.error("ComponentStorage.parseXMLDeclaration(): missing file " + url, null);
							} else {
								parser.parse(is, bundle, components, url.toString());
							}
						} catch (IOException ie) {
							Activator.log.error("[SCR] Error occured while opening component definition file " + url, ie);
						} catch (Throwable t) {
							Activator.log.error("Illegal definition file: " + url, t);
						}
					}
				} // end while

				components = parser.components;
				// make sure the clean-up the parser cache, for the next bundle to
				// work properly!!!
				parser.components = null;
			}

		}
		return components;
	}

}
