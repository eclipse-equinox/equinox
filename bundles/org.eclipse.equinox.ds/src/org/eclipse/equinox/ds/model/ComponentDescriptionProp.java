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
package org.eclipse.equinox.ds.model;

import java.util.*;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentInstance;

/**
 * 
 * Component Description with properties
 * 
 * Component Configuration – A component configuration represents a component
 * description parameterized by component properties.
 * 
 * @version $Revision: 1.2 $
 */
public class ComponentDescriptionProp {

	private ComponentDescription cd;
	private Hashtable properties;
	private ServiceRegistration serviceRegistration;

	/**
	 * List of {@link org.eclipse.equinox.ds.resolver.Reference Reference} objects
	 */
	private List references;

	/**
	 * List of {@link ComponentInstance} objects
	 */
	private List instances;

	/**
	 * List of names (Strings) of Component Configurations we should not cause to
	 * activate during the activation of this Component Configuration.  This is
	 * populated by the {@link org.eclipse.equinox.ds.resolver.Resolver Resolver}
	 * and used by {@link org.eclipse.equinox.ds.instance.BuildDispose BuildDispose}.
	 */
	protected List delayActivateCDPNames;

	/**
	 * Flag to indicate that this Component Configuration registers 
	 * the {@link org.osgi.service.component.ComponentFactory} 
	 * service.  
	 * 
	 * (A Service Component that has the "factory" attribute will have
	 * one Component Configuration that registers 
	 * {@link org.osgi.service.component.ComponentFactory} and then a Component
	 * Configuration for every factory instance that registers a different service
	 * or none at all).
	 */
	protected boolean componentFactory;

	/**
	 * Create a new Component Configuration for this Service Component with
	 * it's own Reference objects, and properties.
	 * 
	 * @param cd the Service Component
	 * 
	 * @param references a List of 
	 *        {@link org.eclipse.equinox.ds.resolver.Reference Reference}
	 *        objects
	 *        
	 * @param properties Properties associated with this Component Configuration
	 * 
	 * @param componentFactory "Component Factory" flag - see 
	 *        {@link ComponentDescriptionProp#componentFactory}
	 */
	public ComponentDescriptionProp(ComponentDescription cd, List references, Hashtable properties, boolean componentFactory) {

		this.cd = cd;
		this.references = references;
		this.properties = properties;

		delayActivateCDPNames = new ArrayList();
		instances = new ArrayList();

		this.componentFactory = componentFactory;
	}

	/**
	 * Get the properties of this Component Configuration
	 */
	public Hashtable getProperties() {
		return properties;
	}

	public ComponentDescription getComponentDescription() {
		return cd;
	}

	/**
	 * Add a new Component Configuration name we should beware of activating to
	 * prevent a cycle.
	 * 
	 * @see ComponentDescriptionProp#delayActivateCDPNames
	 */
	public void setDelayActivateCDPName(String cdpName) {
		if (!delayActivateCDPNames.contains(cdpName))
			delayActivateCDPNames.add(cdpName);
	}

	public ServiceRegistration getServiceRegistration() {
		return serviceRegistration;
	}

	public void setServiceRegistration(ServiceRegistration serviceRegistration) {
		this.serviceRegistration = serviceRegistration;
	}

	public List getDelayActivateCDPNames() {
		return delayActivateCDPNames;
	}

	public void clearDelayActivateCDPNames() {
		delayActivateCDPNames.clear();
	}

	public List getReferences() {
		return references;
	}

	public void addInstance(ComponentInstance instance) {
		instances.add(instance);
	}

	public List getInstances() {
		return instances;
	}

	public void removeInstance(Object object) {
		instances.remove(object);
	}

	public void removeAllInstances() {
		instances.clear();
	}

	public boolean isComponentFactory() {
		return componentFactory;
	}
}
