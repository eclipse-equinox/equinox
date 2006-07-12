/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import org.eclipse.equinox.ds.model.ComponentConfiguration;
import org.osgi.service.component.*;

/**
 * When a Service Component is declared with the <code>factory</code> attribute 
 * on its <code>component</code> element, the Service Component Runtime will 
 * register a ComponentFactory service to allow new component configurations 
 * to be created and activated.
 * 
 * @version $Revision: 1.4 $
 */
public class ComponentFactoryImpl implements ComponentFactory {

	private ComponentConfiguration componentConfiguration;
	private Activator main;

	/**
	 * ComponentFactoryImpl
	 * 
	 * @param context the SC bundle context
	 * @param componentDescriptionProp the ComponentDescription Object with
	 *        Properties
	 * @param buildDispose
	 */
	public ComponentFactoryImpl(ComponentConfiguration componentConfiguration, Activator main) {
		this.componentConfiguration = componentConfiguration;
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
		Hashtable properties = componentConfiguration.getProperties();
		if (newProperties != null) {
			properties = (Hashtable) properties.clone();
			Enumeration propsEnum = newProperties.keys();
			while (propsEnum.hasMoreElements()) {
				Object key = propsEnum.nextElement();
				properties.put(key, newProperties.get(key));
			}
		}

		// create a new componentConfiguration (adds to resolver enabledComponentConfigurations list)
		ComponentConfiguration newComponentConfiguration = main.resolver.mapFactoryInstance(componentConfiguration.getComponentDescription(), properties);

		// try to resolve new componentConfiguration - adds to resolver's satisfied list
		if (!main.resolver.justResolve(newComponentConfiguration)) {
			main.resolver.enabledComponentConfigurations.remove(newComponentConfiguration); // was added by
			// mapFactoryInstance
			throw new ComponentException("Could not resolve instance of " + componentConfiguration + " with properties " + properties);
		}

		// if new componentConfiguration resolves, send it to instance process (will register
		// service
		// if it has one)
		main.resolver.instanceProcess.registerComponentConfigurations(Collections.singletonList(newComponentConfiguration));

		// Instance process will have created an instance
		
		return (ComponentInstance) newComponentConfiguration.getInstances().get(0);
	}
}
