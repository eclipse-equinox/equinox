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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Dictionary;
import java.util.Hashtable;
import javax.xml.parsers.SAXParserFactory;
import org.osgi.framework.*;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * MetaType Activator
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer<SAXParserFactory, SAXParserFactory> {

	protected final String mtsClazz = "org.osgi.service.metatype.MetaTypeService"; //$NON-NLS-1$
	protected final String mtsPid = "org.osgi.impl.service.metatype.MetaTypeService"; //$NON-NLS-1$
	protected final static String saxFactoryClazz = "javax.xml.parsers.SAXParserFactory"; //$NON-NLS-1$

	private ServiceTracker<SAXParserFactory, SAXParserFactory> _parserTracker;
	BundleContext _context;
	ServiceRegistration<MetaTypeService> _mtsReg;
	MetaTypeServiceImpl _mts = null;

	// This field may be accessed by different threads.
	private volatile LogTracker logger;

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

		// Do this first to make logging available as early as possible, also to ensure a
		// null logger is not passed to other objects.
		logger = new LogTracker(context, System.out);
		logger.open();
		this._context = context;
		_parserTracker = new ServiceTracker<SAXParserFactory, SAXParserFactory>(context, saxFactoryClazz, this);
		_parserTracker.open();
		logger.log(LogService.LOG_DEBUG, "====== Meta Type Service starting ! ====="); //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {

		logger.log(LogService.LOG_DEBUG, "====== Meta Type Service stoping ! ====="); //$NON-NLS-1$
		// No null checks required because this method will not be called unless the start method completed successfully.
		_parserTracker.close();
		_parserTracker = null;
		// Do this last to leave logging available as long as possible.
		logger.close();
		logger = null;
		context = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
	 */
	public SAXParserFactory addingService(ServiceReference<SAXParserFactory> ref) {

		SAXParserFactory parserFactory = _context.getService(ref);
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
	public void modifiedService(ServiceReference<SAXParserFactory> ref, SAXParserFactory object) {
		// do nothing
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference,
	 *      java.lang.Object)
	 */
	public void removedService(ServiceReference<SAXParserFactory> ref, SAXParserFactory object) {

		if (object == _currentParserFactory) {
			// This means that this SAXParserFactory was used to start the
			// MetaType Service.
			synchronized (lock) {
				_currentParserFactory = null;

				if (_mtsReg != null) {
					_mtsReg.unregister();
					_mtsReg = null;
					_context.removeBundleListener(_mts);
					_mts = null;
				}
				// See if another factory is available
				SAXParserFactory[] parsers = _parserTracker.getServices(new SAXParserFactory[0]);
				if (parsers != null && parsers.length > 0) {
					_currentParserFactory = parsers[0];
					// We have another parser so lets restart the MetaType
					// Service
					registerMetaTypeService();
				}
			}
		}
		_context.ungetService(ref);
	}

	/**
	 * Internal method in MetaTypeActivator for implementing
	 * ServiceTrackerCustomizer.
	 */
	private void registerMetaTypeService() {

		final Dictionary<String, Object> properties = new Hashtable<String, Object>(7);

		properties.put(Constants.SERVICE_VENDOR, "IBM"); //$NON-NLS-1$
		properties.put(Constants.SERVICE_DESCRIPTION, MetaTypeMsg.SERVICE_DESCRIPTION);
		properties.put(Constants.SERVICE_PID, mtsPid);
		// The intent is that logger can never be null here, but is that really the case?
		_mts = new MetaTypeServiceImpl(_context, _currentParserFactory, logger);
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			public Object run() {
				_context.addBundleListener(_mts);
				_mtsReg = _context.registerService(MetaTypeService.class, _mts, properties);
				return null;
			}
		});
	}
}
