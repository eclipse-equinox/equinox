/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
import java.security.*;
import java.util.Hashtable;
import javax.xml.parsers.SAXParserFactory;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Implementation of MetaTypeService
 */
public class MetaTypeServiceImpl implements MetaTypeService, SynchronousBundleListener {

	SAXParserFactory _parserFactory;
	private Hashtable<Long, MetaTypeInformation> _mtps = new Hashtable<Long, MetaTypeInformation>(7);

	private final LogService logger;
	private final ServiceTracker<Object, Object> metaTypeProviderTracker;

	/**
	 * Constructor of class MetaTypeServiceImpl.
	 */
	public MetaTypeServiceImpl(SAXParserFactory parserFactory, LogService logger, ServiceTracker<Object, Object> metaTypeProviderTracker) {
		this._parserFactory = parserFactory;
		this.logger = logger;
		this.metaTypeProviderTracker = metaTypeProviderTracker;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.MetaTypeService#getMetaTypeInformation(org.osgi.framework.Bundle)
	 */
	public MetaTypeInformation getMetaTypeInformation(Bundle bundle) {

		MetaTypeInformation mti;
		try {
			mti = getMetaTypeProvider(bundle);
		} catch (IOException e) {
			logger.log(LogService.LOG_ERROR, "IOException in MetaTypeInformation:getMetaTypeInformation(Bundle bundle)"); //$NON-NLS-1$
			e.printStackTrace();
			mti = null;
		}
		return mti;
	}

	/**
	 * Internal Method - to get MetaTypeProvider object.
	 */
	private MetaTypeInformation getMetaTypeProvider(final Bundle b) throws java.io.IOException {

		try {
			Long bID = new Long(b.getBundleId());
			synchronized (_mtps) {
				if (_mtps.containsKey(bID))
					return _mtps.get(bID);
				// Avoid synthetic accessor method warnings.
				final LogService loggerTemp = this.logger;
				final ServiceTracker<Object, Object> tracker = this.metaTypeProviderTracker;
				MetaTypeInformation mti = AccessController.doPrivileged(new PrivilegedExceptionAction<MetaTypeInformation>() {
					public MetaTypeInformation run() throws IOException {
						MetaTypeInformationImpl impl = new MetaTypeInformationImpl(b, _parserFactory, loggerTemp);
						if (!impl._isThereMeta)
							return new MetaTypeProviderTracker(b, loggerTemp, tracker);
						return impl;
					}
				});
				_mtps.put(bID, mti);
				return mti;
			}
		} catch (PrivilegedActionException pae) {
			throw (IOException) pae.getException();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
	 */
	public void bundleChanged(BundleEvent event) {

		int type = event.getType();
		Long bID = new Long(event.getBundle().getBundleId());

		switch (type) {
			case BundleEvent.UPDATED :
			case BundleEvent.UNINSTALLED :
				_mtps.remove(bID);
				break;
			case BundleEvent.INSTALLED :
			case BundleEvent.RESOLVED :
			case BundleEvent.STARTED :
			case BundleEvent.STOPPED :
			case BundleEvent.UNRESOLVED :
			default :
				break;
		}
	}
}
