/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.permadmin;

import java.security.*;
import java.util.*;
import org.eclipse.osgi.internal.permadmin.SecurityRow.Decision;
import org.osgi.service.condpermadmin.Condition;

/**
 * 
 * This security manager implements the ConditionalPermission processing for
 * OSGi. It is to be used with ConditionalPermissionAdmin.
 * 
 */
public class EquinoxSecurityManager extends SecurityManager {
	/* 
	 * This is super goofy, but we need to make sure that the CheckContext and
	 * CheckPermissionAction classes load early. Otherwise, we run into problems later.
	 */
	static {
		Class<?> c;
		c = CheckPermissionAction.class;
		c = CheckContext.class;
		c.getName(); // to prevent compiler warnings
	}

	static class CheckContext {
		// A non zero depth indicates that we are doing a recursive permission check.
		List<List<Decision[]>> depthCondSets = new ArrayList<List<Decision[]>>(2);
		List<AccessControlContext> accs = new ArrayList<AccessControlContext>(2);
		List<Class<?>> CondClassSet;

		public int getDepth() {
			return depthCondSets.size() - 1;
		}
	}

	static class CheckPermissionAction implements PrivilegedAction<Object> {
		Permission perm;
		Object context;
		EquinoxSecurityManager fsm;

		CheckPermissionAction(EquinoxSecurityManager fsm, Permission perm, Object context) {
			this.fsm = fsm;
			this.perm = perm;
			this.context = context;
		}

		public Object run() {
			fsm.internalCheckPermission(perm, context);
			return null;
		}
	}

	private final ThreadLocal<CheckContext> localCheckContext = new ThreadLocal<CheckContext>();

	boolean addConditionsForDomain(Decision[] results) {
		CheckContext cc = localCheckContext.get();
		if (cc == null) {
			// We are being invoked in a weird way. Perhaps the ProtectionDomain is
			// getting invoked directly.
			return false;
		}
		List<Decision[]> condSets = cc.depthCondSets.get(cc.getDepth());
		if (condSets == null) {
			condSets = new ArrayList<Decision[]>(1);
			cc.depthCondSets.set(cc.getDepth(), condSets);
		}
		condSets.add(results);
		return true;
	}

	boolean inCheckPermission() {
		return localCheckContext.get() != null;
	}

	public void checkPermission(Permission perm, Object context) {
		AccessController.doPrivileged(new CheckPermissionAction(this, perm, context));
	}

	/**
	 * Gets the AccessControlContext currently being evaluated by
	 * the SecurityManager.
	 * 
	 * @return the AccessControlContext currently being evaluated by the SecurityManager, or
	 * null if no AccessControlContext is being evaluated. Note: this method will
	 * return null if the permission check is being done directly on the AccessControlContext
	 * rather than the SecurityManager.
	 */
	public AccessControlContext getContextToBeChecked() {
		CheckContext cc = localCheckContext.get();
		if (cc != null && cc.accs != null && !cc.accs.isEmpty())
			return cc.accs.get(cc.accs.size() - 1);
		return null;
	}

	void internalCheckPermission(Permission perm, Object context) {
		AccessControlContext acc = (AccessControlContext) context;
		CheckContext cc = localCheckContext.get();
		if (cc == null) {
			cc = new CheckContext();
			localCheckContext.set(cc);
		}
		cc.depthCondSets.add(null); // initialize postponed condition set to null
		cc.accs.add(acc);
		try {
			acc.checkPermission(perm);
			// We want to pop the first set of postponed conditions and process them
			List<Decision[]> conditionSets = cc.depthCondSets.get(cc.getDepth());
			if (conditionSets == null)
				return;
			// TODO the spec seems impossible to implement just doing the simple thing for now
			Map<Class<? extends Condition>, Dictionary<Object, Object>> conditionDictionaries = new HashMap<Class<? extends Condition>, Dictionary<Object, Object>>();
			for (Decision[] domainDecisions : conditionSets) {
				boolean grant = false;
				for (int i = 0; i < domainDecisions.length; i++) {
					if (domainDecisions[i] == null)
						break;
					if ((domainDecisions[i].decision & SecurityTable.ABSTAIN) != 0)
						continue;
					if ((domainDecisions[i].decision & SecurityTable.POSTPONED) == 0) {
						// hit an immediate decision; use it
						if ((domainDecisions[i].decision & SecurityTable.GRANTED) != 0)
							grant = true;
						break;
					}
					int decision = getPostponedDecision(domainDecisions[i], conditionDictionaries, cc);
					if ((decision & SecurityTable.ABSTAIN) != 0)
						continue;
					if ((decision & SecurityTable.GRANTED) != 0)
						grant = true;
					break;
				}
				if (!grant)
					// did not find a condition to grant the permission for this domain
					throw new SecurityException("Conditions not satisfied"); //$NON-NLS-1$
				// continue to next domain
			}

		} finally {
			cc.depthCondSets.remove(cc.getDepth());
			cc.accs.remove(cc.accs.size() - 1);
		}
	}

	private int getPostponedDecision(Decision decision, Map<Class<? extends Condition>, Dictionary<Object, Object>> conditionDictionaries, CheckContext cc) {
		Condition[] postponed = decision.postponed;
		for (int i = 0; i < postponed.length; i++) {
			Dictionary<Object, Object> condContext = conditionDictionaries.get(postponed[i].getClass());
			if (condContext == null) {
				condContext = new Hashtable<Object, Object>();
				conditionDictionaries.put(postponed[i].getClass(), condContext);
			}
			// prevent recursion into Condition
			if (cc.CondClassSet == null)
				cc.CondClassSet = new ArrayList<Class<?>>(2);
			if (cc.CondClassSet.contains(postponed[i].getClass()))
				return SecurityTable.ABSTAIN;
			cc.CondClassSet.add(postponed[i].getClass());
			try {
				// must call isMutable before calling isSatisfied according to the specification
				boolean mutable = postponed[i].isMutable();
				boolean isSatisfied = postponed[i].isSatisfied(new Condition[] {postponed[i]}, condContext);
				decision.handleImmutable(postponed[i], isSatisfied, mutable);
				if (!isSatisfied)
					return SecurityTable.ABSTAIN;
			} finally {
				cc.CondClassSet.remove(postponed[i].getClass());
			}
		}
		// call postponed conditions are satisfied return the decision
		return decision.decision;
	}

	public void checkPermission(Permission perm) {
		checkPermission(perm, getSecurityContext());
	}

	public Object getSecurityContext() {
		return AccessController.getContext();
	}
}
