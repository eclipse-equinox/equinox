/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.metatype.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.parsers.SAXParser;
import org.eclipse.equinox.metatype.EquinoxMetaTypeInformation;
import org.eclipse.equinox.metatype.impl.Persistence.Writer;
import org.osgi.framework.*;

/**
 * Implementation of MetaTypeProvider
 * <p>
 * Extension of MetaTypeProvider
 * </p>
 * <p>
 * Provides methods to:
 * </p>
 * <ul>
 * <li>getPids() get the Pids for a given Locale</li>
 * <li>getFactoryPids() get the Factory Pids for a given Locale</li>
 * </ul>
 */
public class MetaTypeInformationImpl extends MetaTypeProviderImpl implements EquinoxMetaTypeInformation {
	static final String[] emptyStringArray = new String[0];

	/**
	 * Constructor of class MetaTypeInformationImpl.
	 */
	MetaTypeInformationImpl(Bundle bundle, SAXParser parser, LogTracker logger) {
		super(bundle, parser, logger);
	}

	public MetaTypeInformationImpl(Bundle bundle, LogTracker logger, Map<String, ObjectClassDefinitionImpl> pidOCDs,
			Map<String, ObjectClassDefinitionImpl> fPidOCDs) {
		super(bundle, logger, pidOCDs, fPidOCDs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.MetaTypeInformation#getPids()
	 */
	public String[] getPids() {

		if (_allPidOCDs.isEmpty()) {
			return emptyStringArray;
		}
		return _allPidOCDs.keySet().toArray(emptyStringArray);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.MetaTypeInformation#getFactoryPids()
	 */
	public String[] getFactoryPids() {
		if (_allFPidOCDs.isEmpty()) {
			return emptyStringArray;
		}
		return _allFPidOCDs.keySet().toArray(emptyStringArray);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.MetaTypeInformation#getBundle()
	 */
	public Bundle getBundle() {
		return this._bundle;
	}

	static MetaTypeInformationImpl load(BundleContext systemContext, LogTracker log, Persistence.Reader reader)
			throws IOException {
		long id = reader.readLong();
		Bundle b = systemContext.getBundle(id);
		boolean valid = true;
		if (b == null) {
			valid = false;
			// just use the system bundle for load purposes
			b = systemContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
		}

		long lastModified = reader.readLong();
		if (lastModified != b.getLastModified()) {
			valid = false;
		}
		Map<String, ObjectClassDefinitionImpl> allPidOCDs = new HashMap<>();
		int numPidOCDs = reader.readInt();
		for (int i = 0; i < numPidOCDs; i++) {
			String key = reader.readString();
			ObjectClassDefinitionImpl ocd = ObjectClassDefinitionImpl.load(b, log, reader);
			allPidOCDs.put(key, ocd);
		}
		Map<String, ObjectClassDefinitionImpl> allFPidOCDs = new HashMap<>();
		int numFPidOCDs = reader.readInt();
		for (int i = 0; i < numFPidOCDs; i++) {
			String key = reader.readString();
			ObjectClassDefinitionImpl ocd = ObjectClassDefinitionImpl.load(b, log, reader);
			allFPidOCDs.put(key, ocd);
		}

		return !valid ? null : new MetaTypeInformationImpl(b, log, allPidOCDs, allFPidOCDs);
	}

	void write(Writer writer) throws IOException {
		writer.writeLong(getBundle().getBundleId());
		writer.writeLong(getBundle().getLastModified());

		writer.writeInt(_allPidOCDs.size());
		for (Entry<String, ObjectClassDefinitionImpl> entry : _allPidOCDs.entrySet()) {
			writer.writeString(entry.getKey());
			entry.getValue().write(writer);
		}

		writer.writeInt(_allFPidOCDs.size());
		for (Entry<String, ObjectClassDefinitionImpl> entry : _allFPidOCDs.entrySet()) {
			writer.writeString(entry.getKey());
			entry.getValue().write(writer);
		}
	}

}
