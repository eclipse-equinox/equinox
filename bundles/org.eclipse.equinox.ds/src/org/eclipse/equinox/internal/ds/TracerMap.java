/*******************************************************************************
 * Copyright (c) 1997-2007 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds;

import org.eclipse.equinox.internal.util.hash.HashIntObjNS;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class TracerMap {

	public static HashIntObjNS getMap() {
		HashIntObjNS map = new HashIntObjNS(61);

		map.put(0, "SCR");
		map.put(-0x0100, "SCR Activator");

		/* time measurements */
		map.put(100, "[BEGIN - start method] Creating Log instance and initializing log system took ");
		map.put(101, "Getting FrameworkAccess service took ");
		map.put(102, "ConfigurationAdmin ServiceTracker instantiation took ");
		map.put(103, "ServiceTracker starting took ");
		map.put(104, "SCRManager instantiation took ");
		map.put(105, "addBundleListener() method took ");
		map.put(106, "ConfigurationListener service registered for ");
		map.put(107, "addServiceListener() method took ");
		map.put(108, "[END - start method] Activator.start() method executed for ");
		map.put(109, "SCR database got for ");
		map.put(110, "Queue instantiated for ");
		map.put(111, "Threadpool service tracker opened for ");
		map.put(112, "Resolver instantiated for ");
		map.put(113, "startIt() method took ");
		map.put(114, "[BEGIN - lazy SCR init] ");
		map.put(115, "[END - lazy SCR init] Activator.initSCR() method executed for ");
		map.put(116, "DBManager service tracker opened for ");

		// //old debug!
		map.put(10001, "FactoryReg.getService(): created new service for component '");
		map.put(10002, "FactoryReg.ungetService(): registration = ");
		map.put(10003, "InstanceProcess.buildComponents(): building immediate component ");
		map.put(10004, "InstanceProcess.buildComponents(): building component factory ");
		map.put(10005, "InstanceProcess.buildComponent(): building component ");
		map.put(10006, "InstanceProcess.disposeInstances(): disposing non-provider component ");
		map.put(10007, "InstanceProcess.disposeInstances(): disposing component factory ");
		map.put(10008, "InstanceProcess.disposeInstances(): unregistering component ");
		map.put(10009, "InstanceProcess.disposeInstances(): cannot find registrations for ");
		map.put(10010, "InstanceProcess.getService(): cannot get service because of circularity! Reference is: ");
		map.put(10012, "InstanceProcess.dynamicBind(): null instances! for component ");
		map.put(10013, "hasNext");
		map.put(10014, "component name ");
		map.put(10015, "SCRManager.configurationEvent(): found component - ");
		map.put(10016, "SCRManager.stoppingBundle : ");
		map.put(10017, "ComponentStorage.parseXMLDeclaration(): loading ");
		map.put(10018, "changeComponent method end");
		map.put(10019, "Resolver.enableComponents(): ignoring not enabled component ");
		map.put(10020, "Resolver.getEligible(): processing service event ");
		map.put(10021, "Resolver:resolveEligible(): resolved components = ");
		map.put(10022, "Resolver.disableComponents()");
		map.put(10023, "Resolver.performWork(): ");
		map.put(10025, "Resolver.selectDynamicBind(): selected = ");
		map.put(10026, "Resolver.selectDynamicUnBind(): entered");
		map.put(10027, "Resolver.selectDynamicUnBind(): unbinding ");
		map.put(10028, "Resolver.selectDynamicUnBind(): unbindTable is ");
		map.put(10029, "WorkThread.Run()");
		map.put(10030, "WorkThread.getObject ");
		map.put(10032, "ComponentFactoryImpl.newInstance(): ");
		map.put(10034, "getMethod() ");
		map.put(10035, "ServiceComponentProp.dispose(): ");
		map.put(10036, "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ServiceComponentProp.activate(): name: ");
		map.put(10037, "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ServiceComponentProp.instance: ");
		map.put(10038, "ServiceComponentProp.deactivate(): ");
		map.put(10039, "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ServiceComponentProp.using bundle: ");
		map.put(10040, "ServiceComponentProp:bind(): the folowing reference doesn't specify bind method : ");
		map.put(10041, "ServiceComponentProp.bindReferences(): component ");
		map.put(10042, "ServiceComponentProp.unbindReference(): component ");
		map.put(10050, "Service event type: ");
		map.put(10060, "Resolver.selectStaticUnBind(): selected = ");
		map.put(10061, "Resolver.selectStaticBind(): selected = ");
		map.put(10062, "Resolver.enableComponents(): ");
		map.put(10063, "Resolver.map(): Creating SCP for component");
		map.put(10070, "ComponentContextImpl.locateService(): ");
		map.put(10071, "ComponentContextImpl.locateServices():");
		map.put(10072, "ComponentContextImpl.locateServices() the specified service reference is not bound to the specified reference");
		map.put(10080, "ComponentInstanceImpl.dispose(): disposing instance of component ");

		return map;
	}

	public static HashIntObjNS getStarts() {
		return null;
	}
}
