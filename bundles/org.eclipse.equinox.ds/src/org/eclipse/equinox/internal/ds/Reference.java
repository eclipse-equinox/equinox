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

import java.util.Dictionary;
import java.util.Vector;
import org.eclipse.equinox.internal.ds.model.*;
import org.osgi.framework.*;
import org.osgi.service.component.ComponentConstants;

/**
 * The reference class is used only in the resolver. It is actually like
 * "instance of a references". It is used to track available, eligible
 * references. The reference objects relates to ServiceComponentProp as many to
 * one.
 * 
 * @author Valentin Valchev
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 * @version 1.1
 */
public final class Reference {

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
			target = "(objectClass=" + interfaceName + ")";
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
				cardinalityHigh = 999999999;
				break;
			case ComponentReference.CARDINALITY_1_1 :
				cardinalityLow = 1;
				cardinalityHigh = 1;
				break;
			case ComponentReference.CARDINALITY_1_N :
				cardinalityLow = 1;
				cardinalityHigh = 999999999;
		}

	}

	// used in Resolver.resolveEligible()
	final boolean hasProviders() {
		// check whether the component's bundle has service GET permission
		if (System.getSecurityManager() != null && !scp.bc.getBundle().hasPermission(new ServicePermission(interfaceName, ServicePermission.GET))) {
			return false;
		}
		// Get all service references for this target filter
		try {
			ServiceReference[] serviceReferences = null;
			serviceReferences = scp.bc.getServiceReferences(interfaceName, target);
			// if there is no service published that this Service
			// ComponentReferences
			if (serviceReferences != null) {
				return true;
			}
		} catch (InvalidSyntaxException e) {
			Activator.log.warning("Reference.hasLegacyProviders(): invalid target filter " + target, e);
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
	final boolean bindNewReference(ServiceReference reference, boolean dynamicBind) {
		if (dynamicBind) {
			if (policy == ComponentReference.POLICY_STATIC) {
				return false;
			}
		} else {
			if (policy == ComponentReference.POLICY_DYNAMIC) {
				return false;
			}
		}
		String[] serviceNames = (String[]) reference.getProperty(Constants.OBJECTCLASS);
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
		// check target filter
		try {
			Filter filter = FrameworkUtil.createFilter(target);
			if (!filter.match(reference)) {
				return false;
			}
		} catch (InvalidSyntaxException e) {
			return false;
		}
		if (this.reference.bind != null) {
			if (this.reference.serviceReferences.size() < cardinalityHigh) {
				return true;
			}
		} else if (!dynamicBind) {
			//custom case: static reference with no bind method - check its bound service references list
			if (boundServiceReferences.size() < cardinalityHigh) {
				return true;
			}
		}
		return false;
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
			Activator.log.warning("Reference.findProviderSCP(): invalid target filter " + target, e);
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

	public Vector getBoundServiceReferences() {
		return boundServiceReferences;
	}

}
