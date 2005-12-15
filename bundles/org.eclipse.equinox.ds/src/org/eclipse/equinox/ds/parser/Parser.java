/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.parser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.*;
import org.eclipse.equinox.ds.Activator;
import org.eclipse.equinox.ds.Log;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentConstants;
import org.osgi.util.tracker.ServiceTracker;
import org.xml.sax.SAXException;

/**
 * 
 * Parse the component description xml
 * 
 * @version $Revision: 1.2 $
 */
public class Parser {

	/* ServiceTracker for parser */
	private ServiceTracker parserTracker;

	/**
	 * Create and open a ServiceTracker which will track registered XML parsers
	 * 
	 * @param main Main object.
	 */
	public Parser(Activator main) {
		parserTracker = new ServiceTracker(main.context, ParserConstants.SAX_FACTORY_CLASS, null);
		parserTracker.open();
	}

	public void dispose() {
		parserTracker.close();
	}

	public List getComponentDescriptions(BundleContext bundleContext) {
		ManifestElement[] xml = parseManifestHeader(bundleContext.getBundle());

		List result;

		if (xml != null) {
			result = new ArrayList(xml.length);
			for (int i = 0; i < xml.length; i++) {
				List components = parseComponentDescription(bundleContext, xml[i].getValue());
				result.addAll(components);
			}
		} else {
			result = Collections.EMPTY_LIST;
		}

		return result;
	}

	/*
	 * Get the xml files from the bundle
	 * 
	 * @param bundle Bundle @return Vector holding all the xmlfiles for the
	 * specifed Bundle
	 */
	private ManifestElement[] parseManifestHeader(Bundle bundle) {

		Dictionary headers = bundle.getHeaders();
		String files = (String) headers.get(ComponentConstants.SERVICE_COMPONENT);

		try {
			return ManifestElement.parseHeader(ComponentConstants.SERVICE_COMPONENT, files);
		} catch (BundleException e) {
			Log.log(1, "[SCR] Error attempting parse Manifest Element Header. ", e);
			return new ManifestElement[0];
		}
	}

	/**
	 * Given the bundle and the xml filename, parset it!
	 * 
	 * @param bundle Bundle
	 * @param xml String
	 */
	private List parseComponentDescription(BundleContext bundleContext, String xml) {
		List result = new ArrayList();
		int fileIndex = xml.lastIndexOf('/');
		String path = fileIndex != -1 ? xml.substring(0, fileIndex) : "/";
		try {
			Enumeration urls = bundleContext.getBundle().findEntries(path, xml.substring(fileIndex + 1), false);
			if (urls == null || !urls.hasMoreElements()) {
				throw new BundleException("resource not found: " + xml);
			}
			URL url = (URL) urls.nextElement();
			InputStream is = url.openStream();
			SAXParserFactory parserFactory = (SAXParserFactory) parserTracker.getService();
			parserFactory.setNamespaceAware(true);
			parserFactory.setValidating(false);
			SAXParser saxParser = parserFactory.newSAXParser();
			saxParser.parse(is, new ParserHandler(bundleContext, result));
		} catch (IOException e) {
			Log.log(1, "[SCR] IOException attempting to parse ComponentDescription XML. ", e);
		} catch (BundleException e) {
			Log.log(1, "[SCR] BundleException attempting to parse ComponentDescription XML. ", e);
		} catch (SAXException e) {
			Log.log(1, "[SCR] SAXException attempting to parse ComponentDescription XML. ", e);
		} catch (ParserConfigurationException e) {
			Log.log(1, "[SCR] ParserConfigurationException attempting to parse ComponentDescription XML. ", e);
		}

		return result;
	}
}
