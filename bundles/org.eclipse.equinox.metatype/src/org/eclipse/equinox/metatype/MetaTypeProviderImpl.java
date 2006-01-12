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
package org.eclipse.equinox.metatype;

import java.io.IOException;
import java.util.*;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.metatype.*;

/**
 * Implementation of MetaTypeProvider
 */
public class MetaTypeProviderImpl implements MetaTypeProvider {

	public static final String METADATA_NOT_FOUND = "METADATA_NOT_FOUND"; //$NON-NLS-1$
	public static final String OCD_ID_NOT_FOUND = "OCD_ID_NOT_FOUND"; //$NON-NLS-1$
	public static final String ASK_INVALID_LOCALE = "ASK_INVALID_LOCALE"; //$NON-NLS-1$

	public static final String META_FILE_EXT = ".XML"; //$NON-NLS-1$
	public static final String RESOURCE_FILE_CONN = "_"; //$NON-NLS-1$
	public static final String RESOURCE_FILE_EXT = ".properties"; //$NON-NLS-1$
	public static final char DIRECTORY_SEP = '/'; //$NON-NLS-1$

	Bundle _bundle;

	Hashtable _allPidOCDs = new Hashtable(7);
	Hashtable _allFPidOCDs = new Hashtable(7);

	String[] _locales;
	boolean _isThereMeta = false;

	/**
	 * Constructor of class MetaTypeProviderImpl.
	 */
	MetaTypeProviderImpl(Bundle bundle, SAXParserFactory parserFactory) throws IOException {

		this._bundle = bundle;

		// read all bundle's metadata files and build internal data structures
		_isThereMeta = readMetaFiles(bundle, parserFactory);

		if (!_isThereMeta) {
			Logging.log(Logging.WARN, NLS.bind(MetaTypeMsg.METADATA_NOT_FOUND, new Long(bundle.getBundleId()), bundle.getSymbolicName()));
		}
	}

	/**
	 * This method should do the following:
	 * <p> - Obtain a SAX parser from the XML Parser Service:
	 * <p>
	 * 
	 * <pre>	</pre>
	 * 
	 * The parser may be SAX 1 (eXML) or SAX 2 (XML4J). It should attempt to use
	 * a SAX2 parser by instantiating an XMLReader and extending DefaultHandler
	 * BUT if that fails it should fall back to instantiating a SAX1 Parser and
	 * extending HandlerBase.
	 * <p> - Pass the parser the URL for the bundle's METADATA.XML file
	 * <p> - Handle the callbacks from the parser and build the appropriate
	 * MetaType objects - ObjectClassDefinitions & AttributeDefinitions
	 * 
	 * @param bundle The bundle object for which the metadata should be read
	 * @param parserFactory The bundle object for which the metadata should be
	 *        read
	 * @return void
	 * @throws IOException If there are errors accessing the metadata.xml file
	 */
	private boolean readMetaFiles(Bundle bundle, SAXParserFactory parserFactory) throws IOException {

		boolean isThereMetaHere = false;

		Enumeration allFileKeys = FragmentUtils.findEntryPaths(bundle, MetaTypeService.METATYPE_DOCUMENTS_LOCATION);
		if (allFileKeys == null)
			return isThereMetaHere;

		while (allFileKeys.hasMoreElements()) {
			boolean _isMetaDataFile;
			String fileName = (String) allFileKeys.nextElement();

			Vector allOCDsInFile = null;
			java.net.URL[] urls = FragmentUtils.findEntries(bundle, fileName);
			if (urls != null) {
				for (int i = 0; i < urls.length; i++) {
					try {
						// Assume all XML files are what we want by default.
						_isMetaDataFile = true;
						DataParser parser = new DataParser(bundle, urls[i], parserFactory);
						allOCDsInFile = parser.doParse();
						if (allOCDsInFile == null) {
							_isMetaDataFile = false;
						}
					} catch (Exception e) {
						// Ok, looks like it is not what we want.
						_isMetaDataFile = false;
					}

					if ((_isMetaDataFile) && (allOCDsInFile != null)) {
						// We got some OCDs now.
						for (int j = 0; j < allOCDsInFile.size(); j++) {
							ObjectClassDefinitionImpl ocd = (ObjectClassDefinitionImpl) allOCDsInFile.elementAt(j);

							if (ocd.getType() == ObjectClassDefinitionImpl.PID) {
								isThereMetaHere = true;
								_allPidOCDs.put(ocd.getID(), ocd);
							} else {
								isThereMetaHere = true;
								_allFPidOCDs.put(ocd.getID(), ocd);
							}
						} // End of for
					}
				}
			} // End of if(urls!=null)
		} // End of while

		return isThereMetaHere;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.MetaTypeProvider#getObjectClassDefinition(java.lang.String,
	 *      java.lang.String)
	 */
	public ObjectClassDefinition getObjectClassDefinition(String pid, String locale) {

		if (isInvalidLocale(locale)) {
			throw new IllegalArgumentException(NLS.bind(MetaTypeMsg.ASK_INVALID_LOCALE, pid, locale));
		}

		ObjectClassDefinitionImpl ocd;
		if (_allPidOCDs.containsKey(pid)) {
			ocd = (ObjectClassDefinitionImpl) ((ObjectClassDefinitionImpl) _allPidOCDs.get(pid)).clone();
			ocd.setResourceBundle(locale, _bundle);
			return (ObjectClassDefinition) ocd;
		} else if (_allFPidOCDs.containsKey(pid)) {
			ocd = (ObjectClassDefinitionImpl) ((ObjectClassDefinitionImpl) _allFPidOCDs.get(pid)).clone();
			ocd.setResourceBundle(locale, _bundle);
			return (ObjectClassDefinition) ocd;
		} else {
			throw new IllegalArgumentException(NLS.bind(MetaTypeMsg.OCD_ID_NOT_FOUND, pid));
		}
	}

	/**
	 * Internal Method - Check if the locale is invalid.
	 */
	public boolean isInvalidLocale(String locale) {

		// Just a simple and quick check here.
		if (locale == null || locale.length() == 0)
			return false;

		int idx_first = locale.indexOf(ObjectClassDefinitionImpl.LOCALE_SEP);
		int idx_second = locale.lastIndexOf(ObjectClassDefinitionImpl.LOCALE_SEP);
		if (idx_first == -1 && locale.length() == 2)
			// It is format of only language.
			return false;
		if ((idx_first == 2) && (idx_second == 5 || idx_second == 2))
			// It is format of language + "_" + country [ + "_" + variation ].
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.MetaTypeProvider#getLocales()
	 */
	public synchronized String[] getLocales() {

		if (_locales != null)
			return checkForDefault(_locales);

		Vector localizationFiles = new Vector(7);
		// get all the localization resources for PIDS
		Enumeration ocds = _allPidOCDs.elements();
		while (ocds.hasMoreElements()) {
			ObjectClassDefinitionImpl ocd = (ObjectClassDefinitionImpl) ocds.nextElement();
			if (ocd._localization != null && !localizationFiles.contains(ocd._localization))
				localizationFiles.add(ocd._localization);
		}
		// get all the localization resources for FPIDS
		ocds = _allFPidOCDs.elements();
		while (ocds.hasMoreElements()) {
			ObjectClassDefinitionImpl ocd = (ObjectClassDefinitionImpl) ocds.nextElement();
			if (ocd._localization != null && !localizationFiles.contains(ocd._localization))
				localizationFiles.add(ocd._localization);
		}
		if (localizationFiles.size() == 0)
			localizationFiles.add(Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME);
		Vector locales = new Vector(7);
		Enumeration eLocalizationFiles = localizationFiles.elements();
		while (eLocalizationFiles.hasMoreElements()) {
			String localizationFile = (String) eLocalizationFiles.nextElement();
			int iSlash = localizationFile.lastIndexOf(DIRECTORY_SEP);
			String baseDir;
			String baseFileName;
			if (iSlash < 0) {
				baseDir = ""; //$NON-NLS-1$
			} else {
				baseDir = localizationFile.substring(0, iSlash);
			}
			baseFileName = localizationFile + RESOURCE_FILE_CONN;
			Enumeration resources = FragmentUtils.findEntryPaths(this._bundle, baseDir);
			if (resources != null) {
				while (resources.hasMoreElements()) {
					String resource = (String) resources.nextElement();
					if (resource.startsWith(baseFileName) && resource.toLowerCase().endsWith(RESOURCE_FILE_EXT))
						locales.add(resource.substring(baseFileName.length(), resource.length() - RESOURCE_FILE_EXT.length()));
				}
			}
		}
		_locales = (String[]) locales.toArray(new String[locales.size()]);
		return checkForDefault(_locales);
	}

	/**
	 * Internal Method - checkForDefault
	 */
	private String[] checkForDefault(String[] locales) {

		if (locales == null || locales.length == 0 || (locales.length == 1 && Locale.getDefault().toString().equals(locales[0])))
			return null;
		return locales;
	}
}
