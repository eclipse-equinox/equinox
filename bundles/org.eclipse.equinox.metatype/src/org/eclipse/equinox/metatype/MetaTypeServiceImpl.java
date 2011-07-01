/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.metatype;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.osgi.util.NLS;
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
		return getMetaTypeProvider(bundle);
	}

	/**
	 * Internal Method - to get MetaTypeProvider object.
	 */
	private MetaTypeInformation getMetaTypeProvider(final Bundle b) {
		final LogService loggerTemp = this.logger;
		final ServiceTracker<Object, Object> tracker = this.metaTypeProviderTracker;
		try {
			Long bID = new Long(b.getBundleId());
			synchronized (_mtps) {
				if (_mtps.containsKey(bID))
					return _mtps.get(bID);
				// Avoid synthetic accessor method warnings.

				MetaTypeInformation mti = AccessController.doPrivileged(new PrivilegedExceptionAction<MetaTypeInformation>() {
					public MetaTypeInformation run() {
						MetaTypeInformationImpl impl = new MetaTypeInformationImpl(b, _parserFactory, loggerTemp);
						if (!impl._isThereMeta)
							return new MetaTypeProviderTracker(b, loggerTemp, tracker);
						return impl;
					}
				});
				_mtps.put(bID, mti);
				return mti;
			}
		} catch (Exception e) {
			logger.log(LogService.LOG_ERROR, NLS.bind(MetaTypeMsg.EXCEPTION_MESSAGE, e.getMessage()), e);
			return new MetaTypeProviderTracker(b, loggerTemp, tracker);
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
