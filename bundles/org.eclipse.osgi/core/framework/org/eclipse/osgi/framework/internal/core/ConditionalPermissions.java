/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.Vector;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;

/**
 * 
 * This class manages the Permissions for a given code source. It tracks the
 * permissions that have yet to be satisfied as well as conditions that are
 * already satisfied.
 * 
 * @version $Revision: 1.1 $
 */
public class ConditionalPermissions extends PermissionCollection {
	private static final long serialVersionUID = 3907215965749000496L;
	AbstractBundle bundle;
	/**
	 * This is the list of satisfiedCPIs that we are waiting to process in bulk
	 * when evaluating the satisfied permissions. Elements are of type
	 * ConditionalPermissionInfoImpl.
	 */
	Vector satisfiedCPIs = new Vector();
	/**
	 * This is set contains that ConditionalPermissionInfos that are satisfied
	 * and immutable.
	 */
	ConditionalPermissionSet satisfiedCPS = new ConditionalPermissionSet(new ConditionalPermissionInfoImpl[0], new Condition[0]);
	/**
	 * These are the CPIs that may match this CodeSource. Elements are of type
	 * ConditionalPermissionSet.
	 */
	Vector satisfiableCPSs = new Vector();

	boolean empty;

	/**
	 * Constructs a ConditionalPermission for the given bundle.
	 * 
	 * @param bundle the bundle for which this ConditionalPermission tracks Permissions.
	 */
	public ConditionalPermissions(AbstractBundle bundle, ConditionalPermissionAdmin cpa) {
		this.bundle = bundle;
		Enumeration en = cpa.getConditionalPermissionInfos();
		while (en.hasMoreElements()) {
			ConditionalPermissionInfoImpl cpi = (ConditionalPermissionInfoImpl) en.nextElement();
			checkConditionalPermissionInfo(cpi);
		}
	}

	public void checkConditionalPermissionInfo(ConditionalPermissionInfoImpl cpi) {
		try {
			Condition conds[] = cpi.getConditions(bundle);
			boolean satisfied = true;
			for (int i = 0; i < conds.length; i++) {
				Condition cond = conds[i];
				if (cond.isMutable()) {
					satisfied = false;
				} else if (!cond.isSatisfied() /*
				 * Note: the RFC says if
				 * !mutable, evaluated must be
				 * true
				 */) {
					/*
					 * We can just dump here since we have an immutable and
					 * unsatisfied condition.
					 */
					return;
				} else {
					conds[i] = null; /* We can remove satisfied conditions */
				}
			}
			if (satisfied) {
				satisfiedCPIs.add(cpi);
			} else {
				satisfiableCPSs.add(new ConditionalPermissionSet(new ConditionalPermissionInfoImpl[] {cpi}, conds));
			}
		} catch (Exception e) {
			/* TODO: we need to log these somewhere. In any event we need to just move on... */
			e.printStackTrace();
		}
	}

	/**
	 * This method is not implemented since this PermissionCollection should
	 * only be used by the ConditionalPolicy which never calls this method.
	 * 
	 * @see java.security.PermissionCollection#elements()
	 */
	public void add(Permission perm) {
		// do nothing
	}

	public boolean implies(Permission perm) {
		processPending();
		if (satisfiedCPS.implies(perm)) {
			this.empty = false;
			return true;
		}

		boolean newEmpty = !satisfiedCPS.isNonEmpty();

		ConditionalPermissionSet cpsArray[] = (ConditionalPermissionSet[]) satisfiableCPSs.toArray(new ConditionalPermissionSet[0]);
		cpsLoop: for (int i = 0; i < cpsArray.length; i++) {
			if (cpsArray[i].isNonEmpty()) {
				newEmpty = false;
				if (cpsArray[i].implies(perm)) {
					Condition conds[] = cpsArray[i].getNeededConditions();
					Vector unevaluatedConds = new Vector();
					for (int j = 0; j < conds.length; j++) {
						if (conds[j].isEvaluated() && !conds[j].isSatisfied()) {
							continue cpsLoop;
						}
						if (!conds[j].isEvaluated()) {
							unevaluatedConds.add(conds[j]);
						}
					}
					/*
					 * TODO: we need to add the unevaluatedConds to the
					 * ThreadLocal variable. We also need to track where we left
					 * off incase we need to do another pass
					 */
					this.empty = false;
					return true;
				}
			} else {
				satisfiedCPIs.remove(cpsArray[i]);
			}
		}
		this.empty = newEmpty;
		return false;
	}

	/**
	 * Process any satisfiedCPIs that have been added.
	 */
	private void processPending() {
		if (satisfiedCPIs.size() > 0) {
			synchronized (satisfiedCPIs) {
				for (int i = 0; i < satisfiedCPIs.size(); i++) {
					ConditionalPermissionInfoImpl cpi = (ConditionalPermissionInfoImpl) satisfiedCPIs.get(i);
					satisfiedCPS.addConditionalPermissionInfo(cpi);
				}
				satisfiableCPSs.clear();
			}
		}
	}

	/**
	 * This method is not implemented since this PermissionCollection should
	 * only be used by the ConditionalPolicy which never calls this method.
	 * 
	 * @return always returns null.
	 * 
	 * @see java.security.PermissionCollection#elements()
	 */
	public Enumeration elements() {
		return null;
	}

	/**
	 * This method returns true if there are no ConditionalPermissionInfos in
	 * this PermissionCollection. The empty condition is only checked during
	 * the implies method, it should only be checked after implies has been
	 * called.
	 * 
	 * @return false if there are any Conditions that may provide permissions to
	 * this bundle.
	 */
	public boolean isEmpty() {
		return empty;
	}
}
