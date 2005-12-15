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

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * 
 * This class models the reference element.
 * A reference declares a dependency that a component has on a set 
 * of target services.
 * 
 * @see org.eclipse.equinox.ds.resolver.Reference
 * 
 * @version $Revision: 1.2 $
 */
public class ReferenceDescription implements Serializable {

	/**
	 * Eclipse-generated <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -6454039348113422074L;

	private static final int CARDINALITY_HIGH_DEFAULT = 1;
	private static final int CARDINALITY_LOW_DEFAULT = 1;
	private static final String POLICY_DEFAULT = "static";

	private String name;
	private String interfacename;

	// Cardinality indicates the number of services, matching this
	// reference,
	// which will bind to this Service Component. Possible values are:
	// 0..1, 0..n, 1..1 (i.e. exactly one), 1..n (i.e. at least one).
	// This attribute is optional. If it is not specified, then a
	// cardinality
	// of â€œ1..1â€? is used.
	private int cardinalityHigh;
	private int cardinalityLow;
	private String policy;
	private String target;
	private String bind;
	private String unbind;

	//Cache bind and unbind methods
	transient private Method bindMethod;
	transient private Method unbindMethod;

	public ReferenceDescription() {
		// set defaults
		cardinalityHigh = CARDINALITY_HIGH_DEFAULT;
		cardinalityLow = CARDINALITY_LOW_DEFAULT;
		policy = POLICY_DEFAULT;
	}

	/**
	 * @return Returns the bind method.
	 */
	public String getBind() {
		return bind;
	}

	/**
	 * @param bind The bind method.
	 */
	public void setBind(String bind) {
		this.bind = bind;
	}

	/**
	 * @return Returns the cardinality.
	 */
	public int getCardinalityHigh() {
		return cardinalityHigh;
	}

	/**
	 * @param cardinality 
	 */
	public void setCardinality(String cardinality) {
		if (cardinality.equals("0..1")) {
			cardinalityLow = 0;
			cardinalityHigh = 1;
		} else if (cardinality.equals("0..n")) {
			cardinalityLow = 0;
			cardinalityHigh = 999999999; // infinite
		} else if (cardinality.equals("1..1")) {
			cardinalityLow = 1;
			cardinalityHigh = 1;
		} else if (cardinality.equals("1..n")) {
			cardinalityLow = 1;
			cardinalityHigh = 999999999;
		} else {
			// TODO throw exception?
		}
	}

	/**
	 * @return Returns the interfacename.
	 */
	public String getInterfacename() {
		return interfacename;
	}

	/**
	 * @param interfacename The interfacename to set.
	 */
	public void setInterfacename(String interfacename) {
		this.interfacename = interfacename;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the policy.
	 */
	public String getPolicy() {
		return policy;
	}

	/**
	 * @param policy The policy to set.
	 */
	public void setPolicy(String policy) {
		// TODO validate
		this.policy = policy;
	}

	/**
	 * @return Returns the target.
	 */
	public String getTarget() {
		return target;
	}

	/**
	 * @param target The target to set.
	 */
	public void setTarget(String target) {
		// TODO validate
		this.target = target;
	}

	/**
	 * @return Returns the unbind method
	 */
	public String getUnbind() {
		return unbind;
	}

	/**
	 * @param unbind The unbind method to set.
	 */
	public void setUnbind(String unbind) {
		this.unbind = unbind;
	}

	// if the cardinality is "1..1" or "1..n" then this reference is required
	public boolean isRequired() {
		return (cardinalityLow == 1);
	}

	//Cache bind and unbind methods

	/**
	 * @return Returns the bind method.
	 */
	public Method getBindMethod() {
		return bindMethod;
	}

	/**
	 * @param bind The bind method.
	 */
	public void setBindMethod(Method method) {
		this.bindMethod = method;
	}

	/**
	 * @return Returns the unbind method
	 */
	public Method getUnbindMethod() {
		return unbindMethod;
	}

	/**
	 * @param unbind The unbind method to set.
	 */
	public void setUnbindMethod(Method method) {
		this.unbindMethod = method;
	}

}
