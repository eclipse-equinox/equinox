/*******************************************************************************
 * Copyright (c) 1997-2011 by ProSyst Software GmbH
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
import java.util.Enumeration;
import java.util.Vector;
import org.eclipse.equinox.internal.ds.model.DeclarationParser;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;

/**
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 */

public abstract class ComponentStorage {

	private final DeclarationParser parser = new DeclarationParser();

	/**
	 * This method will load the component definitions from a bundle. The
	 * returned value should contain vector with 'ServiceComponent' elements.
	 * 
	 * @param bundle  bundle, containing DS components
	 * @param dsHeader  the DS header value which is in the bundle's manifest
	 * @return <code>null</code> if there are no component definitions.
	 */
	public abstract Vector loadComponentDefinitions(Bundle bundle, String dsHeader);

	/**
	 * This method is called when a bundle has been uninstalled and therefore its cached components must be removed
	 * @param bundleID the id of the uninstalled bundle 
	 */
	public abstract void deleteComponentDefinitions(long bundleID);

	/**
	 * Called when the DS bundle is about to stop. This method must store any unsaved data
	 */
	public abstract void stop();

	protected Vector parseXMLDeclaration(Bundle bundle, String dsHeader) throws Exception {
		Vector components = new Vector();
		if (dsHeader == null)
			return components;
		ManifestElement[] elements = ManifestElement.parseHeader(ComponentConstants.SERVICE_COMPONENT, dsHeader);
		// the parser is not thread safe!!!
		synchronized (parser) {
			// process all definition file
			for (int i = 0; i < elements.length; i++) {
				String[] definitionFiles = elements[i].getValueComponents();
				for (int j = 0; j < definitionFiles.length; j++) {
					String definitionFile = definitionFiles[j];
					int ind = definitionFile.lastIndexOf('/');
					String path = ind != -1 ? definitionFile.substring(0, ind) : "/"; //$NON-NLS-1$
					InputStream is = null;

					Enumeration urls = bundle.findEntries(path, ind != -1 ? definitionFile.substring(ind + 1) : definitionFile, false);
					if (urls == null || !urls.hasMoreElements()) {
						Activator.log(bundle.getBundleContext(), LogService.LOG_ERROR, NLS.bind(Messages.COMPONENT_XML_NOT_FOUND, bundle.getSymbolicName(), definitionFile), null);
						continue;
					}

					// illegal components are ignored, but framework event is posted for
					// them; however, it will continue and try to load any legal
					// definitions
					URL url;
					while (urls.hasMoreElements()) {
						url = (URL) urls.nextElement();
						if (Activator.DEBUG) {
							Activator.log.debug("ComponentStorage.parseXMLDeclaration(): loading " + url.toString(), null); //$NON-NLS-1$
						}
						try {
							is = url.openStream();
							if (is == null) {
								Activator.log(bundle.getBundleContext(), LogService.LOG_ERROR, NLS.bind(Messages.CANT_OPEN_STREAM_TO_COMPONENT_XML, url), null);
							} else {
								int compSize = components.size();
								parser.parse(is, bundle, components, url.toString());
								if (compSize == components.size()) {
									Activator.log(bundle.getBundleContext(), LogService.LOG_WARNING, NLS.bind(Messages.NO_COMPONENTS_FOUND, url), null);
								}
							}
						} catch (IOException ie) {
							Activator.log(bundle.getBundleContext(), LogService.LOG_ERROR, NLS.bind(Messages.ERROR_OPENING_COMP_XML, url), ie);
						} catch (Throwable t) {
							Activator.log(bundle.getBundleContext(), LogService.LOG_ERROR, NLS.bind(Messages.ILLEGAL_DEFINITION_FILE, url), t);
						} finally {
							if (is != null) {
								is.close();
							}
						}
					} // end while
				} // end for definitionFiles
			} // end for elements

			components = parser.components;
			// make sure the clean-up the parser cache, for the next bundle to
			// work properly!!!
			parser.components = null;
		}
		return components;
	}

}
