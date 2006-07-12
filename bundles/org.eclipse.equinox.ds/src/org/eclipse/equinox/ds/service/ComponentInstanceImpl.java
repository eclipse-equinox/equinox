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

import java.util.Collections;
import org.eclipse.equinox.ds.Activator;
import org.eclipse.equinox.ds.model.ComponentConfiguration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

/**
 * A ComponentInstance encapsulates an instance of a component.
 * ComponentInstances are created whenever an instance of a component is
 * created.
 * 
 * @version $Revision: 1.1 $
 */
public class ComponentInstanceImpl implements ComponentInstance {

	private Object instance;
	private Activator main;
	private ComponentConfiguration componentConfiguration;
	private ComponentContext componentContext;

	/**
	 * ComponentInstanceImpl
	 * 
	 * @param Object instance
	 * 
	 */
	public ComponentInstanceImpl(Activator main, ComponentConfiguration componentConfiguration, Object instance) {
		this.main = main;
		this.instance = instance;
		this.componentConfiguration = componentConfiguration;

	}

	public void setComponentContext(ComponentContext context) {
		this.componentContext = context;
	}

	public ComponentContext getComponentContext() {
		return componentContext;
	}

	/**
	 * Dispose of this component instance. The instance will be deactivated. If
	 * the instance has already been deactivated, this method does nothing.
	 */
	public void dispose() {
		// deactivate
		if (!componentConfiguration.isComponentFactory() && componentConfiguration.getComponentDescription().getFactory() != null) {
			// this is a factory instance, so dispose of component configuration
			componentConfiguration.getComponentDescription().removeComponentConfiguration(componentConfiguration);
			main.resolver.disposeComponentConfigurations(Collections.singletonList(componentConfiguration));
			componentConfiguration = null;
		} else {
			main.resolver.instanceProcess.buildDispose.disposeComponentInstance(this);
			componentConfiguration.removeInstance(this);
		}
		instance = null;
	}

	/**
	 * Returns the component instance. The instance has been activated.
	 * 
	 * @return The component instance or <code>null</code> if the instance has
	 *         been deactivated.
	 */
	public Object getInstance() {
		return instance;
	}

	public ComponentConfiguration getComponentConfiguration() {
		return componentConfiguration;
	}

}
