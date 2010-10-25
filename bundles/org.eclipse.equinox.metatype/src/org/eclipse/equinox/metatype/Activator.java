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

import java.util.Dictionary;
import java.util.Hashtable;
import javax.xml.parsers.SAXParserFactory;
import org.osgi.framework.*;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * MetaType Activator
 */
public class Activator implements BundleActivator {
	/*
	 * The following filter guarantees only services meeting the following
	 * criteria will be tracked.
	 * 
	 * (1) A ManagedService or ManagedServiceFactory registered with a
	 * SERVICE_PID property. May also be registered as a MetaTypeProvider.
	 * (2) A MetaTypeProvider registered with a METATYPE_PID or
	 * METATYPE_FACTORY_PID property.
	 * 
	 * Note that it's still necessary to inspect a ManagedService or
	 * ManagedServiceFactory to ensure it also implements MetaTypeProvider.
	 */
	private static final String FILTER = "(|(&(" + Constants.OBJECTCLASS + '=' + ManagedService.class.getName() + "*)(" + Constants.SERVICE_PID + "=*))(&(" + Constants.OBJECTCLASS + '=' + MetaTypeProvider.class.getName() + ")(|(" + MetaTypeProvider.METATYPE_PID + "=*)(" + MetaTypeProvider.METATYPE_FACTORY_PID + "=*))))"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	private static final String SERVICE_PID = "org.osgi.impl.service.metatype.MetaTypeService"; //$NON-NLS-1$

	private LogTracker logServiceTracker;
	// Could be ManagedService, ManagedServiceFactory, or MetaTypeProvider.
	// The tracker tracks all services regardless of bundle. Services are
	// filtered by bundle later in the MetaTypeProviderTracker class. It may 
	// therefore be shared among multiple instances of that class.
	private ServiceTracker<Object, Object> metaTypeProviderTracker;
	private ServiceTracker<SAXParserFactory, SAXParserFactory> saxParserFactoryTracker;

	public void start(BundleContext context) throws InvalidSyntaxException {
		LogTracker lsTracker;
		ServiceTracker<Object, Object> mtpTracker;
		ServiceTracker<SAXParserFactory, SAXParserFactory> spfTracker;
		Filter filter = context.createFilter(FILTER);
		synchronized (this) {
			lsTracker = logServiceTracker = new LogTracker(context, System.out);
			mtpTracker = metaTypeProviderTracker = new ServiceTracker<Object, Object>(context, filter, null);
			spfTracker = saxParserFactoryTracker = new ServiceTracker<SAXParserFactory, SAXParserFactory>(context, SAXParserFactory.class, new SAXParserFactoryTrackerCustomizer(context, lsTracker, mtpTracker));
		}
		// Do this first to make logging available as early as possible.
		lsTracker.open();
		lsTracker.log(LogService.LOG_DEBUG, "====== Meta Type Service starting ! ====="); //$NON-NLS-1$
		// Do this next to make MetaTypeProviders available as early as possible.
		mtpTracker.open();
		// Do this last because it may result in the MetaTypeService being registered.
		spfTracker.open();
	}

	public void stop(BundleContext context) {
		ServiceTracker<SAXParserFactory, SAXParserFactory> spfTracker;
		ServiceTracker<Object, Object> mtpTracker;
		LogTracker lsTracker;
		synchronized (this) {
			spfTracker = saxParserFactoryTracker;
			// Set this to null so the SAXParserFactoryTrackerCustomizer knows
			// not to register a new MetaTypeService when removedService() is
			// called while the tracker is closing.
			saxParserFactoryTracker = null;
			mtpTracker = metaTypeProviderTracker;
			lsTracker = logServiceTracker;
		}
		lsTracker.log(LogService.LOG_DEBUG, "====== Meta Type Service stopping ! ====="); //$NON-NLS-1$
		spfTracker.close();
		mtpTracker.close();
		// Do this last to leave logging available as long as possible.
		lsTracker.close();
	}

	synchronized ServiceTracker<SAXParserFactory, SAXParserFactory> getSAXParserFactoryTracker() {
		return saxParserFactoryTracker;
	}

	private class SAXParserFactoryTrackerCustomizer implements ServiceTrackerCustomizer<SAXParserFactory, SAXParserFactory> {
		private final BundleContext bundleCtx;
		private final LogService logService;
		private final ServiceTracker<Object, Object> mtpTracker;

		private MetaTypeServiceImpl metaTypeService;
		private ServiceRegistration<MetaTypeService> metaTypeServiceRegistration;
		private SAXParserFactory saxParserFactory;

		public SAXParserFactoryTrackerCustomizer(BundleContext bundleContext, LogService logService, ServiceTracker<Object, Object> metaTypeProviderTracker) {
			this.bundleCtx = bundleContext;
			this.logService = logService;
			this.mtpTracker = metaTypeProviderTracker;
		}

		public SAXParserFactory addingService(ServiceReference<SAXParserFactory> ref) {
			SAXParserFactory parserFactory = bundleCtx.getService(ref);
			if (parserFactory == null)
				return null;
			boolean register = false;
			synchronized (this) {
				if (saxParserFactory == null) {
					// Save this parserFactory as the currently used parserFactory
					saxParserFactory = parserFactory;
					register = true;
				}
			}
			if (register) {
				registerMetaTypeService();
			}
			return parserFactory;
		}

		public void modifiedService(ServiceReference<SAXParserFactory> ref, SAXParserFactory object) {
			// noop
		}

		public void removedService(ServiceReference<SAXParserFactory> ref, SAXParserFactory object) {
			ServiceRegistration<MetaTypeService> registration = null;
			MetaTypeServiceImpl service = null;
			synchronized (this) {
				if (object == saxParserFactory) {
					// This means that this SAXParserFactory was used to start the
					// MetaTypeService. Set to null to indicate a new MetaTypeService
					// needs to be registered.
					saxParserFactory = null;
					registration = metaTypeServiceRegistration;
					service = metaTypeService;
				}
			}
			if (registration != null) {
				registration.unregister();
				bundleCtx.removeBundleListener(service);
				// See if another factory is available
				ServiceTracker<SAXParserFactory, SAXParserFactory> tracker = getSAXParserFactoryTracker();
				// If the SAXParserFactory tracker is null, the bundle is stopping, and we
				// shouldn't register a new MetaTypeService to avoid unnecessary churn.
				if (tracker != null) {
					SAXParserFactory factory = tracker.getService();
					if (factory != null) {
						// We have another parser so lets restart the MetaTypeService
						boolean register = false;
						synchronized (this) {
							if (saxParserFactory == null) {
								saxParserFactory = factory;
								register = true;
							}
						}
						if (register) {
							registerMetaTypeService();
						}
					}
				}
			}
			bundleCtx.ungetService(ref);
		}

		private void registerMetaTypeService() {
			Dictionary<String, Object> properties = new Hashtable<String, Object>(7);
			properties = new Hashtable<String, Object>(7);
			properties.put(Constants.SERVICE_VENDOR, "IBM"); //$NON-NLS-1$
			properties.put(Constants.SERVICE_DESCRIPTION, MetaTypeMsg.SERVICE_DESCRIPTION);
			properties.put(Constants.SERVICE_PID, SERVICE_PID);
			MetaTypeServiceImpl service;
			synchronized (this) {
				service = metaTypeService = new MetaTypeServiceImpl(saxParserFactory, logService, mtpTracker);
			}
			bundleCtx.addBundleListener(service);
			ServiceRegistration<MetaTypeService> registration = bundleCtx.registerService(MetaTypeService.class, service, properties);
			synchronized (this) {
				metaTypeServiceRegistration = registration;
			}
		}
	}
}
