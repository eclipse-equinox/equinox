/*******************************************************************************
 * Copyright (c) 1997-2010 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *    Joerg-Christian Boehme - bug.id = 246757
 *    Andrew Teirney		 - bug.id = 278732
 *******************************************************************************/
package org.eclipse.equinox.internal.ds;

import java.util.*;
import org.eclipse.equinox.internal.ds.model.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;

/**
 * The reference class is used only in the resolver. It is actually like
 * "instance of a references". It is used to track available, eligible
 * references. The reference objects relates to ServiceComponentProp as many to
 * one.
 * 
 * @author Valentin Valchev
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 */
public final class Reference implements org.apache.felix.scr.Reference {

	public ComponentReference reference;
	ServiceComponentProp scp;

	// -- begin cache
	String interfaceName;
	String target;
	int policy;
	int cardinalityHigh;
	int cardinalityLow;

	//holds the matching service references in case the component has no bind method
	//in case the cardinality is 1..1, the vector will hold only one matching ServiceReference
	Vector boundServiceReferences = new Vector(1);

	// -- end cache

	/**
	 * Reference object
	 * 
	 * @param reference
	 *            the component reference
	 * @param properties
	 *            the *actual* properties that the component has. These are the
	 *            SCP properties, e.g the default one in the XML file + the one
	 *            that are configured in the CM.
	 */
	Reference(ComponentReference reference, ServiceComponentProp scp, Dictionary properties) {
		this.reference = reference;
		this.scp = scp;
		this.interfaceName = reference.interfaceName;
		this.target = reference.target;

		// RFC 80 section 5.3.1.3:
		// If [target] is not specified and there is no <reference-name>.target
		// component
		// property, then the selection filter used to select the desired
		// service is
		// �(objectClass=�+<interface-name>+�)�.
		if (properties != null) {
			target = (String) properties.get(reference.name + ComponentConstants.REFERENCE_TARGET_SUFFIX);
		}
		if (target == null) {
			target = "(objectClass=" + interfaceName + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		}

		// If it is not specified, then a policy of �static� is used.
		policy = reference.policy;

		// Cardinality indicates the number of services, matching this
		// reference,
		// which will bind to this Service Component. Possible values are:
		// 0..1, 0..n, 1..1 (i.e. exactly one), 1..n (i.e. at least one).
		// This attribute is optional. If it is not specified, then a
		// cardinality
		// of �1..1� is used.
		switch (reference.cardinality) {
			case ComponentReference.CARDINALITY_0_1 :
				cardinalityLow = 0;
				cardinalityHigh = 1;
				break;
			case ComponentReference.CARDINALITY_0_N :
				cardinalityLow = 0;
				cardinalityHigh = Integer.MAX_VALUE;
				break;
			case ComponentReference.CARDINALITY_1_1 :
				cardinalityLow = 1;
				cardinalityHigh = 1;
				break;
			case ComponentReference.CARDINALITY_1_N :
				cardinalityLow = 1;
				cardinalityHigh = Integer.MAX_VALUE;
		}

	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String newTarget) {
		target = newTarget;
	}

	// used in Resolver.resolveEligible()
	final boolean hasProviders(Hashtable serviceReferenceTable) {
		// check whether the component's bundle has service GET permission
		if (System.getSecurityManager() != null && !scp.bc.getBundle().hasPermission(new ServicePermission(interfaceName, ServicePermission.GET))) {
			return false;
		}
		// Get all service references for this target filter
		try {
			ServiceReference[] serviceReferences = null;
			serviceReferences = scp.bc.getServiceReferences(interfaceName, target);
			// Only return true if there is a service published that this Reference
			// represents and we know about it (if we care about it)
			if (serviceReferences != null) {
				if (serviceReferenceTable == null)
					return true;
				for (int i = 0; i < serviceReferences.length; i++) {
					if (serviceReferenceTable.containsKey(serviceReferences[i])) {
						return true;
					}
				}
			}
		} catch (InvalidSyntaxException e) {
			Activator.log(reference.component.bc, LogService.LOG_WARNING, "Reference.hasProviders(): " + NLS.bind(Messages.INVALID_TARGET_FILTER, target), e); //$NON-NLS-1$
		}
		return false;
	}

	// if the cardinality is "0..1" or "0..n" then this refernce is not required
	final boolean isRequiredFor(ServiceComponent cd) {
		// // we want to re-resolve if the component is static and already
		// eligible
		// if (policy == ComponentReference.POLICY_STATIC && cd.eligible) {
		// return true;
		// }
		return cardinalityLow == 1;
	}

	public boolean isRequired() {
		return cardinalityLow == 1;
	}

	public boolean isUnary() {
		return cardinalityHigh == 1;
	}

	// used in Resolver.selectDynamicBind()
	final boolean bindNewReference(ServiceReference referenceToBind, boolean dynamicBind) {
		if (dynamicBind) {
			if (policy == ComponentReference.POLICY_STATIC) {
				return false;
			}
		} else {
			if (policy == ComponentReference.POLICY_DYNAMIC) {
				return false;
			}
			if (this.reference.policy_option == ComponentReference.POLICY_OPTION_RELUCTANT) {
				return false;
			}
		}

		String[] serviceNames = (String[]) referenceToBind.getProperty(Constants.OBJECTCLASS);
		boolean hasName = false;
		for (int i = 0; i < serviceNames.length; i++) {
			if (serviceNames[i].equals(interfaceName)) {
				hasName = true;
				break;
			}
		}
		if (!hasName) {
			return false;
		}

		if (this.reference.bind != null) {
			if (this.reference.serviceReferences.size() >= cardinalityHigh) {
				if (this.reference.policy_option == ComponentReference.POLICY_OPTION_RELUCTANT) {
					return false;
				} else if (this.reference.policy_option == ComponentReference.POLICY_OPTION_GREEDY) {
					//check if the single bound service needs replacement with higher ranked service
					Object currentBoundServiceReference = this.reference.serviceReferences.keys().nextElement();
					int res = referenceToBind.compareTo(currentBoundServiceReference);
					if (res <= 0) {
						// bound service shall not be replaced
						return false;
					}
				}
			}
		} else if (!dynamicBind) {
			//custom case: static reference with no bind method - check its bound service references list
			if (boundServiceReferences.size() >= cardinalityHigh) {
				if (this.reference.policy_option == ComponentReference.POLICY_OPTION_GREEDY) {
					//check if the single bound service needs replacement with higher ranked service
					Object currentBoundServiceReference = boundServiceReferences.elementAt(0);
					int res = referenceToBind.compareTo(currentBoundServiceReference);
					if (res <= 0) {
						// bound service shall not be replaced
						return false;
					}
				}
			}
		}

		// check target filter
		try {
			Filter filter = FrameworkUtil.createFilter(target);
			if (!filter.match(referenceToBind)) {
				return false;
			}
		} catch (InvalidSyntaxException e) {
			return false;
		}
		return true;
	}

	/**
	 * Called to determine if the specified new target filter will still satisfy the current state of the reference 
	 * @param newTargetFilter the new target filter to be checked
	 * @return true if the reference will still be satisfied after the filter replacement
	 */
	public boolean doSatisfy(String newTargetFilter) {
		ServiceReference[] refs = null;
		try {
			refs = scp.bc.getServiceReferences(reference.interfaceName, newTargetFilter);
		} catch (InvalidSyntaxException e) {
			Activator.log(scp.bc, LogService.LOG_WARNING, "[SCR] " + NLS.bind(Messages.INVALID_TARGET_FILTER, newTargetFilter), e); //$NON-NLS-1$
			return false;
		}

		if (refs == null) {
			if (cardinalityLow > 0) {
				//the reference is mandatory, but there are no matching services with the new target filter
				return false;
			}
			if (policy == ComponentReference.POLICY_STATIC) {
				if (this.reference.bind != null) {
					if (this.reference.serviceReferences.size() > 0) {
						//have bound services which are not matching the new filter 
						return false;
					}
				} else {
					//custom case: static reference with no bind method - check its bound service references list
					if (boundServiceReferences.size() > 0) {
						//have bound services which are not matching the new filter 
						return false;
					}
				}
			}
			//the reference is not mandatory and the dynamic bound services can be unbound
			return true;
		}
		if (policy == ComponentReference.POLICY_STATIC) {
			if (this.reference.bind != null) {
				Enumeration keys = this.reference.serviceReferences.keys();
				while (keys.hasMoreElements()) {
					Object serviceRef = keys.nextElement();
					boolean found = false;
					for (int i = 0; i < refs.length; i++) {
						if (refs[i] == serviceRef) {
							found = true;
							break;
						}
					}
					if (!found) {
						//the bound service reference is not already in the satisfied references set. 
						//since this is a static reference a restart of the component is required
						return false;
					}
				}
				if (cardinalityHigh > 1 && this.reference.serviceReferences.size() < refs.length) {
					//there are more services to bind. this requires restart of the component since the reference is static
					return false;
				}
			} else {
				//custom case: static reference with no bind method
				for (int i = 0; i < boundServiceReferences.size(); i++) {
					Object serviceRef = boundServiceReferences.elementAt(i);
					boolean found = false;
					for (int j = 0; j < refs.length; j++) {
						if (refs[j] == serviceRef) {
							found = true;
							break;
						}
					}
					if (!found) {
						//the bound service reference is not already in the satisfied references set. 
						//since this is a static reference a restart of the component is required
						return false;
					}
				}
				if (cardinalityHigh > 1 && boundServiceReferences.size() < refs.length) {
					//there are more services to bind. this requires restart of the component since the reference is static
					return false;
				}

			}
		}
		return true;
	}

	// used in Resolver.selectDynamicUnBind();
	final boolean dynamicUnbindReference(ServiceReference changedReference) {
		// nothing dynamic to do if static
		if (policy == ComponentReference.POLICY_STATIC) {
			return false;
		}
		// now check if the Service Reference is found in the list of saved
		// ServiceReferences
		if (!this.reference.serviceReferences.containsKey(changedReference)) {
			return false;
		}
		return true;
	}

	final boolean staticUnbindReference(ServiceReference changedReference) {
		// does not apply if its policy is dynamic
		if (policy == ComponentReference.POLICY_DYNAMIC) {
			return false;
		}
		// now check if the Service Reference is found in the list of saved
		// ServiceReferences; this list is saved in case the component has a bind method
		if (this.reference.serviceReferences.containsKey(changedReference)) {
			return true;
		}
		if (boundServiceReferences.size() > 0) {
			return (cardinalityHigh == 1) ? boundServiceReferences.elementAt(0) == changedReference : boundServiceReferences.contains(changedReference);
		}
		return false;
	}

	/**
	 * Checks if the passed service reference is satisfying this reference according to the target filter
	 * @param serviceReference the service reference to check
	 * @return true, if the service reference do satisfy this reference, otherwise returns false
	 */
	public boolean isInSatisfiedList(ServiceReference serviceReference) {
		Filter filter;
		try {
			filter = FrameworkUtil.createFilter(target);
		} catch (InvalidSyntaxException e) {
			Activator.log(reference.component.bc, LogService.LOG_WARNING, "Reference.isInSatisfiedList(): " + NLS.bind(Messages.INVALID_TARGET_FILTER, target), e); //$NON-NLS-1$
			return false;
		}
		return filter.match(serviceReference);
	}

	public void setBoundServiceReferences(ServiceReference[] references) {
		if (policy == ComponentReference.POLICY_DYNAMIC) {
			//not relevant to dynamic references
			return;
		}

		boundServiceReferences.removeAllElements();
		if (cardinalityHigh == 1) {
			boundServiceReferences.addElement(references[0]);
		} else {
			for (int i = 0; i < references.length; i++) {
				boundServiceReferences.addElement(references[i]);
			}
		}
	}

	/**
	 * Selects the providers that provide service required by this reference
	 * @param scps  the list of SCPs to be searched for providers
	 * @return an array of SCPs that provide service required by this reference
	 */
	public ServiceComponentProp[] selectProviders(Vector scps) {
		Filter filter;
		try {
			filter = FrameworkUtil.createFilter(target);
		} catch (InvalidSyntaxException e) {
			Activator.log(reference.component.bc, LogService.LOG_WARNING, "Reference.selectProviders(): " + NLS.bind(Messages.INVALID_TARGET_FILTER, target), e); //$NON-NLS-1$
			return null;
		}
		Vector result = new Vector(2);
		for (int i = 0; i < scps.size(); i++) {
			ServiceComponentProp providerSCP = (ServiceComponentProp) scps.elementAt(i);
			if (providerSCP.serviceComponent.serviceInterfaces != null && providerSCP.serviceComponent.serviceInterfaces.contains(interfaceName)) {
				if (filter.match(providerSCP.getProperties())) {
					result.addElement(providerSCP);
				}
			}
		}
		ServiceComponentProp[] res = new ServiceComponentProp[result.size()];
		result.copyInto(res);
		return res;
	}

	public boolean isBound() {
		if (this.reference.bind != null) {
			return (this.reference.serviceReferences.size() >= cardinalityLow);
		}
		return true;
	}

	public Vector getBoundServiceReferences() {
		return boundServiceReferences;
	}

	public String getBindMethodName() {
		return reference.bind;
	}

	public String getName() {
		return reference.name;
	}

	public String getServiceName() {
		return reference.interfaceName;
	}

	public ServiceReference[] getServiceReferences() {
		Vector result = null;
		if (this.reference.bind != null) {
			if (!reference.serviceReferences.isEmpty()) {
				result = new Vector(2);
				Enumeration keys = reference.serviceReferences.keys();
				while (keys.hasMoreElements()) {
					result.add(keys.nextElement());
				}
			}
		} else {
			//no bind method
			if (!boundServiceReferences.isEmpty()) {
				result = (Vector) boundServiceReferences.clone();
			}
		}
		if (result != null && !result.isEmpty()) {
			ServiceReference[] finalResult = new ServiceReference[result.size()];
			result.copyInto(finalResult);
			return finalResult;
		}
		return null;
	}

	public String getUnbindMethodName() {
		return reference.unbind;
	}

	public boolean isMultiple() {
		return cardinalityHigh > 1;
	}

	public boolean isOptional() {
		return cardinalityLow == 0;
	}

	public boolean isSatisfied() {
		if (cardinalityLow == 0) {
			return true;
		}
		try {
			ServiceReference[] serviceReferences = reference.component.bc.getServiceReferences(interfaceName, target);
			if (serviceReferences != null) {
				return true;
			}
		} catch (InvalidSyntaxException e) {
			// do nothing
		}
		return false;
	}

	public boolean isStatic() {
		return policy == ComponentReference.POLICY_STATIC;
	}

	/* (non-Javadoc)
	 * @see org.apache.felix.scr.Reference#getUpdatedMethodName()
	 */
	public String getUpdatedMethodName() {
		if (reference.component.isNamespaceAtLeast12()) {
			return reference.updated;
		}
		return null;
	}

}
