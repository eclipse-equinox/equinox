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

import java.io.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import javax.xml.parsers.*;
import org.eclipse.equinox.metatype.EquinoxMetaTypeInformation;
import org.eclipse.equinox.metatype.EquinoxMetaTypeService;
import org.eclipse.equinox.metatype.impl.Persistence.Reader;
import org.eclipse.equinox.metatype.impl.Persistence.Writer;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.util.tracker.ServiceTracker;
import org.xml.sax.SAXException;

/**
 * Implementation of MetaTypeService
 */
public class MetaTypeServiceImpl implements EquinoxMetaTypeService, SynchronousBundleListener {
	private static String CACHE_FILE = "metaTypeCache"; //$NON-NLS-1$
	SAXParserFactory _parserFactory;
	private Hashtable<Long, EquinoxMetaTypeInformation> _mtps = new Hashtable<>(7);

	private final LogTracker logger;
	private final ServiceTracker<Object, Object> metaTypeProviderTracker;

	/**
	 * Constructor of class MetaTypeServiceImpl.
	 */
	public MetaTypeServiceImpl(SAXParserFactory parserFactory, LogTracker logger,
			ServiceTracker<Object, Object> metaTypeProviderTracker) {
		this._parserFactory = parserFactory;
		this.logger = logger;
		this.metaTypeProviderTracker = metaTypeProviderTracker;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.service.metatype.MetaTypeService#getMetaTypeInformation(org.osgi.
	 * framework.Bundle)
	 */
	@Override
	public EquinoxMetaTypeInformation getMetaTypeInformation(Bundle bundle) {
		return getMetaTypeProvider(bundle);
	}

	/**
	 * Internal Method - to get MetaTypeProvider object.
	 */
	private EquinoxMetaTypeInformation getMetaTypeProvider(final Bundle b) {
		// Avoid synthetic accessor method warnings.
		final LogTracker loggerTemp = this.logger;
		final ServiceTracker<Object, Object> tracker = this.metaTypeProviderTracker;
		Long bID = Long.valueOf(b.getBundleId());
		synchronized (_mtps) {
			if (_mtps.containsKey(bID))
				return _mtps.get(bID);
			EquinoxMetaTypeInformation mti = AccessController
					.doPrivileged(new PrivilegedAction<EquinoxMetaTypeInformation>() {
						@Override
						public EquinoxMetaTypeInformation run() {
							MetaTypeInformationImpl impl = null;
							try {
								impl = new MetaTypeInformationImpl(b, newParser(), loggerTemp);
							} catch (Exception e) {
								loggerTemp.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.METADATA_PARSE_ERROR,
										b.getBundleId(), b.getSymbolicName()), e);
							}
							if (impl == null || !impl._isThereMeta)
								return new MetaTypeProviderTracker(b, loggerTemp, tracker);
							return impl;
						}
					});
			_mtps.put(bID, mti);
			return mti;
		}
	}

	SAXParser newParser() throws ParserConfigurationException, SAXException {
		boolean namespaceAware = _parserFactory.isNamespaceAware();
		boolean validating = _parserFactory.isValidating();
		// Always want a non-validating parser.
		_parserFactory.setValidating(false);
		try {
			// If the factory is already namespace aware, we know it can create namespace
			// aware parsers
			// because that was checked in the service tracker.
			if (namespaceAware) {
				return _parserFactory.newSAXParser();
			}
			// If the factory is not already namespace aware, it may or may not be able to
			// create
			// namespace aware parsers.
			_parserFactory.setNamespaceAware(true);
			try {
				return _parserFactory.newSAXParser();
			} catch (Exception e) {
				// Factory cannot create namespace aware parsers. Go with the last resort.
				_parserFactory.setNamespaceAware(false);
				return _parserFactory.newSAXParser();
			}
		} finally {
			// Restore the previous settings in all cases.
			_parserFactory.setNamespaceAware(namespaceAware);
			_parserFactory.setValidating(validating);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.
	 * BundleEvent)
	 */
	public void bundleChanged(BundleEvent event) {

		int type = event.getType();
		Long bID = Long.valueOf(event.getBundle().getBundleId());

		switch (type) {
		case BundleEvent.UPDATED:
		case BundleEvent.UNINSTALLED:
			_mtps.remove(bID);
			break;
		case BundleEvent.INSTALLED:
		case BundleEvent.RESOLVED:
		case BundleEvent.STARTED:
		case BundleEvent.STOPPED:
		case BundleEvent.UNRESOLVED:
		default:
			break;
		}
	}

	void load(BundleContext context, LogTracker log, ServiceTracker<Object, Object> tracker) throws IOException {
		File cache = context.getDataFile(CACHE_FILE);
		// using system context to see all bundles by the ID
		BundleContext systemContext = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
		if (cache.isFile()) {
			try (Reader reader = new Reader(new DataInputStream(new BufferedInputStream(new FileInputStream(cache))))) {
				if (!reader.isValidPersistenceVersion()) {
					logger.log(LogTracker.LOG_INFO, "Metatype cache version is not supported.  Ignoring cache."); //$NON-NLS-1$
					return;
				}
				int numService = reader.readInt();
				for (int i = 0; i < numService; i++) {
					long id = reader.readLong();
					Bundle b = systemContext.getBundle(id);
					if (b != null) {
						_mtps.put(b.getBundleId(), new MetaTypeProviderTracker(b, log, tracker));
					}
				}

				reader.readIndexedStrings();

				int numXML = reader.readInt();
				for (int i = 0; i < numXML; i++) {
					MetaTypeInformationImpl info = MetaTypeInformationImpl.load(systemContext, log, reader);
					if (info != null) {
						_mtps.put(info.getBundle().getBundleId(), info);
					}
				}
			}
		}
	}

	void save(BundleContext context) throws IOException {
		File cache = context.getDataFile(CACHE_FILE);
		try (Writer writer = new Writer(new DataOutputStream(new BufferedOutputStream(new FileOutputStream(cache))))) {
			writer.writePersistenceVersion();
			List<MetaTypeInformation> serviceInfos = new ArrayList<>();
			List<MetaTypeInformationImpl> xmlInfos = new ArrayList<>();
			synchronized (_mtps) {
				for (MetaTypeInformation info : _mtps.values()) {
					if (info instanceof MetaTypeInformationImpl) {
						xmlInfos.add((MetaTypeInformationImpl) info);
					} else {
						serviceInfos.add(info);
					}
				}
			}

			writer.writeInt(serviceInfos.size());
			for (MetaTypeInformation info : serviceInfos) {
				writer.writeLong(info.getBundle().getBundleId());
			}

			Set<String> strings = new HashSet<>();
			for (MetaTypeInformationImpl info : xmlInfos) {
				info.getStrings(strings);
			}
			writer.writeIndexedStrings(strings);

			writer.writeInt(xmlInfos.size());
			for (MetaTypeInformationImpl info : xmlInfos) {
				info.write(writer);
			}
		}
	}
}
