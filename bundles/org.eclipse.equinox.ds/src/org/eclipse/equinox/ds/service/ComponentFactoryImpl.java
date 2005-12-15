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
package org.eclipse.equinox.ds.service;

import java.util.*;
import org.eclipse.equinox.ds.Activator;
import org.eclipse.equinox.ds.model.ComponentDescriptionProp;
import org.osgi.service.component.*;

/**
 * When a Service Component is declared with the <code>factory</code> attribute 
 * on its <code>component</code> element, the Service Component Runtime will 
 * register a ComponentFactory service to allow new component configurations 
 * to be created and activated.
 * 
 * @version $Revision: 1.2 $
 */
public class ComponentFactoryImpl implements ComponentFactory {

	private ComponentDescriptionProp cdp;
	private Activator main;

	/**
	 * ComponentFactoryImpl
	 * 
	 * @param context the SC bundle context
	 * @param componentDescriptionProp the ComponentDescription Object with
	 *        Properties
	 * @param buildDispose
	 */
	public ComponentFactoryImpl(ComponentDescriptionProp cdp, Activator main) {
		this.cdp = cdp;
		this.main = main;
	}

	/**
	 * Create and activate a new component configuration. Additional properties
	 * may be provided for the component configuration.
	 * 
	 * @param newProperties Additional properties for the component configuration.
	 * @return A ComponentInstance object encapsulating an instance of the 
	 *         component configuration. The returned Component Configuration 
	 *         instance has been activated and, if the Service Component 
	 *         specifies a <code>service</code> element, the Component 
	 *         Configuration has been registered as a service.
	 * @throws ComponentException If the Service Component Runtime is unable to
	 *         activate the Component Configuration instance.
	 */
	public ComponentInstance newInstance(Dictionary newProperties) {

		// merge properties
		Hashtable properties = (Hashtable) cdp.getProperties().clone();
		Enumeration propsEnum = newProperties.keys();
		while (propsEnum.hasMoreElements()) {
			Object key = propsEnum.nextElement();
			properties.put(key, newProperties.get(key));
		}

		// create a new cdp (adds to resolver enabledCDPs list)
		ComponentDescriptionProp newCDP = main.resolver.mapFactoryInstance(cdp.getComponentDescription(), properties);

		// try to resolve new cdp - adds to resolver's satisfied list
		if (!main.resolver.justResolve(newCDP)) {
			main.resolver.enabledCDPs.remove(newCDP); // was added by
			// mapFactoryInstance
			throw new ComponentException("Could not resolve instance of " + cdp + " with properties " + properties);
		}

		// if new cdp resolves, send it to instance process (will register
		// service
		// if it has one)
		main.resolver.instanceProcess.registerComponentConfigs(Collections.singletonList(newCDP));

		// get instance of new cdp to return

		if (newCDP.getComponentDescription().isImmediate()) {
			// if cdp is immediate then instanceProcess created one
			return (ComponentInstance) newCDP.getInstances().get(0);
		}

		return main.resolver.instanceProcess.buildDispose.buildComponentConfigInstance(null, newCDP);
	}
}
