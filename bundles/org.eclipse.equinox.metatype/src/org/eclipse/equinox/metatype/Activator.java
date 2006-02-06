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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;
import javax.xml.parsers.SAXParserFactory;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * MetaType Activator
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer {

	protected final String mtsClazz = "org.osgi.service.metatype.MetaTypeService"; //$NON-NLS-1$
	protected final String mtsPid = "org.osgi.impl.service.metatype.MetaTypeService"; //$NON-NLS-1$
	protected final static String saxFactoryClazz = "javax.xml.parsers.SAXParserFactory"; //$NON-NLS-1$

	private ServiceTracker _parserTracker;
	BundleContext _context;
	ServiceRegistration _mtsReg;
	MetaTypeServiceImpl _mts = null;

	/**
	 * The current SaxParserFactory being used by the WebContainer
	 */
	private SAXParserFactory _currentParserFactory = null;

	/**
	 * The lock used when modifying the currentParserFactory
	 */
	private Object lock = new Object();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {

		this._context = context;
		_parserTracker = new ServiceTracker(context, saxFactoryClazz, this);
		_parserTracker.open();
		ServiceReference ref = context.getServiceReference(PackageAdmin.class.getName());
		FragmentUtils.packageAdmin = ref == null ? null : (PackageAdmin) context.getService(ref);
		Logging.debug("====== Meta Type Service starting ! ====="); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {

		Logging.debug("====== Meta Type Service stoping ! ====="); //$NON-NLS-1$
		_parserTracker.close();
		_parserTracker = null;
		FragmentUtils.packageAdmin = null;
		context = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
	 */
	public Object addingService(ServiceReference ref) {

		SAXParserFactory parserFactory = (SAXParserFactory) _context.getService(ref);
		synchronized (lock) {
			if (_mts == null) {
				// Save this parserFactory as the currently used parserFactory
				_currentParserFactory = parserFactory;
				registerMetaTypeService();
			}
		}
		return parserFactory;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference,
	 *      java.lang.Object)
	 */
	public void modifiedService(ServiceReference ref, Object object) {
		// do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference,
	 *      java.lang.Object)
	 */
	public void removedService(ServiceReference ref, Object object) {

		SAXParserFactory parserFactory = (SAXParserFactory) _context.getService(ref);

		if (parserFactory == _currentParserFactory) {
			// This means that this SAXParserFactory was used to start the
			// MetaType Service.
			synchronized (lock) {
				_currentParserFactory = null;

				if (_mtsReg != null) {
					_mtsReg.unregister();
					_mtsReg = null;
					_context.removeBundleListener(_mts);
					_mts = null;
					parserFactory = null;
				}
				// See if another factory is available
				Object[] parsers = _parserTracker.getServices();
				if (parsers != null && parsers.length > 0) {
					_currentParserFactory = (SAXParserFactory) parsers[0];
					// We have another parser so lets restart the MetaType
					// Service
					registerMetaTypeService();
				}
			}
		}
	}

	/**
	 * Internal method in MetaTypeActivator for implementing
	 * ServiceTrackerCustomizer.
	 */
	private void registerMetaTypeService() {

		final Hashtable properties = new Hashtable(7);

		properties.put(Constants.SERVICE_VENDOR, "IBM"); //$NON-NLS-1$
		properties.put(Constants.SERVICE_DESCRIPTION, MetaTypeMsg.SERVICE_DESCRIPTION);
		properties.put(Constants.SERVICE_PID, mtsPid);

		_mts = new MetaTypeServiceImpl(_context, _currentParserFactory);
		AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				_context.addBundleListener(_mts);
				_mtsReg = _context.registerService(mtsClazz, _mts, properties);
				return null;
			}
		});
	}
}
