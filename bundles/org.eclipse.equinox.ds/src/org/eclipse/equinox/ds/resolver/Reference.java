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
package org.eclipse.equinox.ds.resolver;

import java.util.*;
import org.eclipse.equinox.ds.model.ComponentDescriptionProp;
import org.eclipse.equinox.ds.model.ReferenceDescription;
import org.osgi.framework.*;

/**
 * 
 * Wrapper for a {@link ReferenceDescription} that may have
 * a different target filter set by ConfigAdmin or ComponentFactory.newInstance()
 * 
 * @see org.eclipse.equinox.ds.model.ReferenceDescription
 * @version $Revision: 1.2 $
 */
public class Reference {

	private static final String TARGET = ".target";
	private ReferenceDescription referenceDescription;
	private ComponentDescriptionProp cdp;
	private String target;

	/**
	 * The Set of {@link ServiceReference ServiceReferences} bound to this Reference
	 */
	private Set serviceReferences = new HashSet();

	// ServiceReference:ServiceObject that binded to this instance
	private Map serviceReferenceToServiceObject = new Hashtable();

	/**
	 * Create a new Reference object.
	 * 
	 * If properties include a (reference name).target property, that overrides
	 * the target in the {@link ReferenceDescription}.
	 * 
	 * @param referenceDescription
	 * @param properties Properties for this Component Configuration
	 */
	Reference(ReferenceDescription referenceDescription, Hashtable properties) {
		this.referenceDescription = referenceDescription;

		//properties can override the Service Component XML
		this.target = (String) properties.get(referenceDescription.getName() + TARGET);

		this.target = this.target != null ? this.target : referenceDescription.getTarget();

		// RFC 80 section 5.3.1.3:
		// If [target] is not specified and there is no <reference-name>.target
		// component
		// property, then the selection filter used to select the desired
		// service is
		// â€œ(objectClass=â€?+<interface-name>+â€?)â€?.
		this.target = this.target != null ? this.target : "(objectClass=" + referenceDescription.getInterfacename() + ")";

	}

	/**
	 * Set the Component Configuration that this reference belongs to
	 */
	void setComponentDescriptionProp(ComponentDescriptionProp parent) {
		cdp = parent;
	}

	/**
	 * Get the Component Configuration that this reference belongs to
	 */
	public ComponentDescriptionProp getComponentDescriptionProp() {
		return cdp;
	}

	/**
	 * Check if there is at least one service registered that satisfies this
	 * reference.
	 * 
	 * Checks ServicePermission.GET.
	 * 
	 * @param context Bundle context used to call 
	 *        {@link BundleContext#getServiceReferences(java.lang.String, java.lang.String)}
	 * @return whether this Reference can be satisfied by the currently registered services
	 */
	boolean hasProvider(BundleContext context) {

		// Get all service references for this target filter
		try {
			ServiceReference[] providers = null;
			providers = context.getServiceReferences(referenceDescription.getInterfacename(), target);
			// if there is no service published that this Service
			// ComponentReferences
			if (providers != null) {
				return true;
			}
			return false;
		} catch (InvalidSyntaxException e) {
			//TODO log something?
			return false;
		}
	}

	public ReferenceDescription getReferenceDescription() {
		return referenceDescription;
	}

	public String getTarget() {
		return target;
	}

	/**
	 * Check if a {@link ServiceReference} should be dynamically bound to this 
	 * Reference.
	 * 
	 * @param serviceReference
	 */
	public boolean dynamicBindReference(ServiceReference serviceReference) {

		//check policy
		if ("static".equals(referenceDescription.getPolicy())) {
			return false;
		}

		//check interface
		List provideList = Arrays.asList((String[]) (serviceReference.getProperty("objectClass")));
		if (!provideList.contains(this.getReferenceDescription().getInterfacename())) {
			return false;
		}

		//check target filter
		Filter filter;
		try {
			filter = FrameworkUtil.createFilter(target);
		} catch (InvalidSyntaxException e) {
			//TODO log something?
			return false;
		}
		if (!filter.match(serviceReference)) {
			return false;
		}

		//check cardinality
		int currentRefCount = serviceReferences.size();
		if (currentRefCount < referenceDescription.getCardinalityHigh()) {
			return true;
		}
		return false;

	}

	/**
	 * Check if we need to be dynamically unbound from a {@link ServiceReference}
	 * 
	 * @param serviceReference
	 */
	boolean dynamicUnbindReference(ServiceReference serviceReference) {

		// nothing dynamic to do if static
		if ("static".equals(referenceDescription.getPolicy())) {
			return false;
		}

		// now check if the ServiceReference is found in the list of saved
		// ServiceReferences for this reference
		if (!serviceReferences.contains(serviceReference)) {
			return false;
		}

		return true;

	}

	public void addServiceReference(ServiceReference serviceReference) {
		serviceReferences.add(serviceReference);
	}

	public void removeServiceReference(ServiceReference serviceReference) {
		serviceReferences.remove(serviceReference);
		serviceReferenceToServiceObject.remove(serviceReference);
	}

	public void clearServiceReferences() {
		serviceReferences.clear();
		serviceReferenceToServiceObject.clear();
	}

	public Set getServiceReferences() {
		return serviceReferences;
	}

	public boolean bindedToServiceReference(ServiceReference serviceReference) {
		return serviceReferences.contains(serviceReference);
	}

	public void addServiceReference(ServiceReference serviceReference, Object serviceObject) {
		addServiceReference(serviceReference);
		serviceReferenceToServiceObject.put(serviceReference, serviceObject);
	}

	public Object getServiceObject(ServiceReference serviceReference) {
		return serviceReferenceToServiceObject.get(serviceReference);
	}

	/**
	 * Check if this reference can be satisfied by the service provided by one
	 * of a list of Component Configurations
	 * 
	 * @param cdps a List of {@link ComponentDescriptionProp}s to search for providers
	 * for this reference
	 * @return the providing CDP or null if none
	 */
	ComponentDescriptionProp findProviderCDP(List cdps) {

		Filter filter;
		try {

			filter = FrameworkUtil.createFilter(target);
		} catch (InvalidSyntaxException e) {
			//TODO log something?
			return null;
		}

		// loop thru cdps to search for provider of service
		Iterator it = cdps.iterator();
		while (it.hasNext()) {
			ComponentDescriptionProp providerCDP = (ComponentDescriptionProp) it.next();
			List provideList = providerCDP.getComponentDescription().getServicesProvided();

			if (provideList.contains(this.getReferenceDescription().getInterfacename())) {
				// check the target field
				if (filter.match(providerCDP.getProperties())) {
					return providerCDP;
				}
			}
		}

		return null;
	}

}
