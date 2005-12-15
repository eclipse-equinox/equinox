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
import java.security.*;
import java.util.Hashtable;
import javax.xml.parsers.SAXParserFactory;
import org.osgi.framework.*;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;

/**
 * Implementation of MetaTypeService
 */
public class MetaTypeServiceImpl implements MetaTypeService, SynchronousBundleListener {

	private BundleContext _context;
	private SAXParserFactory _parserFactory;
	private Hashtable _mtps = new Hashtable(7);

	/**
	 * Constructor of class MetaTypeServiceImpl.
	 */
	public MetaTypeServiceImpl(BundleContext context, SAXParserFactory parserFactory) {
		this._context = context;
		this._parserFactory = parserFactory;
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
			Logging.log(Logging.ERROR, "IOException in MetaTypeInformation:getMetaTypeInformation(Bundle bundle)");
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
				if (_mtps.containsKey(bID)) {
					return (MetaTypeInformation) _mtps.get(bID);
				} else {
					MetaTypeInformation mti = (MetaTypeInformation) AccessController.doPrivileged(new PrivilegedExceptionAction() {
						public Object run() throws IOException {
							MetaTypeInformationImpl mti = new MetaTypeInformationImpl(b, _parserFactory);
							if (!mti._isThereMeta)
								return new MetaTypeProviderTracker(_context, b);
							return mti;
						}
					});
					_mtps.put(bID, mti);
					return mti;
				}
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
