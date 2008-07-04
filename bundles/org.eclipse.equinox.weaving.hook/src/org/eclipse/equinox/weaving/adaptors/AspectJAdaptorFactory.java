/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Matthew Webster           Eclipse 3.2 changes     
 *******************************************************************************/
package org.eclipse.equinox.weaving.adaptors;

import org.eclipse.equinox.service.weaving.ICachingService;
import org.eclipse.equinox.service.weaving.IWeavingService;
import org.eclipse.equinox.service.weaving.SupplementerRegistry;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.loader.BaseClassLoader;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

public class AspectJAdaptorFactory {

	private BundleContext bundleContext;
	private IWeavingService singletonWeavingService;
	private ICachingService singletonCachingService;
	private PackageAdmin packageAdminService;
	private SupplementerRegistry supplementerRegistry;
	
	public AspectJAdaptorFactory () {
	}
	
	public void initialize (BundleContext context, SupplementerRegistry supplementerRegistry) {
		if (Debug.DEBUG_GENERAL) Debug.println("> AspectJAdaptorFactory.initialize() context=" + context);
		this.bundleContext = context;
		this.supplementerRegistry = supplementerRegistry;
	
		String  weavingFilter = "(objectclass="+IWeavingService.class.getName()+")"; 
		String  cachingFilter = "(objectclass="+ICachingService.class.getName()+")"; 
		/*
		 * Add listeners to listen for the 
		 * registration of the weaving and caching services
		 */
		ServiceListener weavingListener = new ServiceListener() {

			public void serviceChanged(ServiceEvent event) {
				if(event.getType() == ServiceEvent.REGISTERED) {
//					System.err.println("ServiceListener.serviceChanged() event=" + event);
					initializeWeavingService();
				}
			}
			
		};
		
		ServiceListener cachingListener = new ServiceListener(){

			public void serviceChanged(ServiceEvent event) {
				if(event.getType() == ServiceEvent.REGISTERED) {
//					System.err.println("ServiceListener.serviceChanged() event=" + event);
					initializeCachingService();
				}
			}
			
		};

		try {
			bundleContext.addServiceListener(weavingListener,weavingFilter);
			bundleContext.addServiceListener(cachingListener,cachingFilter);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
		initializePackageAdminService(context);
		
		if (Debug.DEBUG_GENERAL) Debug.println("< AspectJAdaptorFactory.initialize() weavingListener=" + weavingListener + ", cachingListener=" + cachingListener);
	}
	
	protected void initializeWeavingService () {
		if (Debug.DEBUG_GENERAL) Debug.println("> AspectJAdaptorFactory.initializeWeavingService()");
		ServiceReference ref = bundleContext.getServiceReference(IWeavingService.class.getName());
		if (ref!=null){
			singletonWeavingService = (IWeavingService)bundleContext.getService(ref);
		}
		if (Debug.DEBUG_GENERAL) Debug.println("< AspectJAdaptorFactory.initializeWeavingService() weavingService=" + singletonWeavingService);
	}

	protected IWeavingService getWeavingService (BaseClassLoader loader) {
		if (Debug.DEBUG_WEAVE) Debug.println("> AspectJAdaptorFactory.getWeavingService() baseClassLoader=" + loader);
		IWeavingService weavingService = null;

		if (singletonWeavingService != null) {
			BaseData baseData = loader.getClasspathManager().getBaseData();
			State state = baseData.getAdaptor().getState();
			Bundle bundle = baseData.getBundle();
			BundleDescription bundleDescription = state.getBundle(bundle.getBundleId()); 
			weavingService = singletonWeavingService.getInstance((ClassLoader)loader, bundle, state, bundleDescription, supplementerRegistry);
		}
		if (Debug.DEBUG_WEAVE) Debug.println("< AspectJAdaptorFactory.getWeavingService() service=" + weavingService);
		return weavingService;
	}
	
	protected void initializeCachingService () {
		if (Debug.DEBUG_CACHE) Debug.println("> AspectJAdaptorFactory.initializeCachingService()");
		ServiceReference ref = bundleContext.getServiceReference(ICachingService.class.getName());
		if (ref != null){
			singletonCachingService = (ICachingService)bundleContext.getService(ref);
		}
		if (Debug.DEBUG_CACHE) Debug.println("< AspectJAdaptorFactory.initializeCachingService() singletonCachingService=" + singletonCachingService);
	}

	protected ICachingService getCachingService (BaseClassLoader loader, Bundle bundle, IWeavingService weavingService) {
		if (Debug.DEBUG_CACHE) Debug.println("> AspectJAdaptorFactory.getCachingService() bundle=" + bundle + ", weavingService=" + weavingService);
		ICachingService service = null;
		String key = "";

		if (weavingService != null) {
			key = weavingService.getKey();
		}
		if (singletonCachingService != null) {
			service = singletonCachingService.getInstance((ClassLoader)loader,bundle,key);
		}
		if (Debug.DEBUG_CACHE) Debug.println("< AspectJAdaptorFactory.getCachingService() service=" + service + ", key='" + key + "'");
		return service;
	}
	
	private void initializePackageAdminService (BundleContext context) {
		if (Debug.DEBUG_GENERAL) Debug.println("> AspectJAdaptorFactory.initializePackageAdminService() context=" + context);

		ServiceReference ref = context.getServiceReference(PackageAdmin.class.getName());
		if (ref != null) {
			packageAdminService = (PackageAdmin)context.getService(ref);
		}
		
		if (Debug.DEBUG_GENERAL) Debug.println("< AspectJAdaptorFactory.initializePackageAdminService() " + packageAdminService);
	}

	public Bundle getHost (Bundle fragment) {
		if (Debug.DEBUG_GENERAL) Debug.println("> AspectJAdaptorFactory.getHost() fragment=" + fragment);

		Bundle host = null;
		if (packageAdminService != null) host = packageAdminService.getHosts(fragment)[0];

		if (Debug.DEBUG_GENERAL) Debug.println("< AspectJAdaptorFactory.getHost() " + host);
		return host;
	}
}
