/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Connexta, LLC - evaluation cache implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.permadmin;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.osgi.internal.permadmin.SecurityRow.Decision;
import org.osgi.service.condpermadmin.Condition;

public class SecurityTable extends PermissionCollection {
	private static final long serialVersionUID = -1800193310096318060L;
	static final int GRANTED = 0x0001;
	static final int DENIED = 0x0002;
	static final int ABSTAIN = 0x0004;
	static final int POSTPONED = 0x0008;

	private static final int MUTABLE = 0x0016;

	private final SecurityRow[] rows;
	private final SecurityAdmin securityAdmin;

	private final transient Map<EvaluationCacheKey, Integer> evaluationCache = new ConcurrentHashMap<>(10000);

	public SecurityTable(SecurityAdmin securityAdmin, SecurityRow[] rows) {
		if (rows == null)
			throw new NullPointerException("rows cannot be null!!"); //$NON-NLS-1$
		this.rows = rows;
		this.securityAdmin = securityAdmin;
	}

	boolean isEmpty() {
		return rows.length == 0;
	}

	int evaluate(BundlePermissions bundlePermissions, Permission permission) {
		if (bundlePermissions == null) {
			return ABSTAIN;
		}
		if (evaluationCache.size() > 10000) {
			clearEvaluationCache();
		}
		EvaluationCacheKey evaluationCacheKey = new EvaluationCacheKey(bundlePermissions, permission);
		if (isEmpty()) {
			evaluationCache.put(evaluationCacheKey, ABSTAIN);
			return ABSTAIN;
		}

		// can't short-circuit early, so try cache
		Integer result = evaluationCache.get(evaluationCacheKey);
		boolean hasMutable = false;
		if (result != null) {
			hasMutable = (result & MUTABLE) == MUTABLE;
			if (!hasMutable) {
				return result;
			}
		}
		// cache miss or has mutable rows
		boolean postponed = false;
		Decision[] results = new Decision[rows.length];
		int immediateDecisionIdx = -1;
		// evaluate each row
		for (int i = 0; i < rows.length && immediateDecisionIdx == -1; i++) {
			if (result == null) {
				// check all conditions for any that are mutable, this will turn off the cache
				hasMutable |= checkMutable(bundlePermissions, evaluationCacheKey, rows[i]);
			}
			try {
				results[i] = rows[i].evaluate(bundlePermissions, permission);
			} catch (Exception e) {
				// TODO log?
				results[i] = SecurityRow.DECISION_ABSTAIN;
			}
			if ((results[i].decision & ABSTAIN) == ABSTAIN)
				continue; // ignore this row and continue to next row
			if ((results[i].decision & POSTPONED) == POSTPONED) {
				// row is postponed; we can no longer return quickly on a denied decision
				postponed = true;
				continue; // continue to next row
			}
			if (!postponed) {
				// no postpones encountered yet; we can return the decision quickly
				if (!hasMutable) {
					evaluationCache.put(evaluationCacheKey, results[i].decision);
				}
				return results[i].decision; // return GRANTED or DENIED
			}
			// got an immediate answer; but it is after a postponed condition.
			// no need to process the rest of the rows
			immediateDecisionIdx = i;
		}
		Integer immediateDecision = handlePostponedConditions(evaluationCacheKey, hasMutable, postponed, results,
				immediateDecisionIdx);
		if (immediateDecision != null)
			return immediateDecision;
		int finalDecision = postponed ? POSTPONED : ABSTAIN;
		if (!hasMutable && (finalDecision & POSTPONED) != POSTPONED) {
			evaluationCache.put(evaluationCacheKey, finalDecision);
		}
		return finalDecision;
	}

	private boolean checkMutable(BundlePermissions bundlePermissions, EvaluationCacheKey evaluationCacheKey,
			SecurityRow row) {
		Condition[] conditions = row.getConditions(bundlePermissions);
		if (conditions != null) {
			for (Condition condition : conditions) {
				if (condition != null && condition.isMutable()) {
					evaluationCache.put(evaluationCacheKey, MUTABLE);
					return true;
				}
			}
		}
		return false;
	}

	private Integer handlePostponedConditions(EvaluationCacheKey evaluationCacheKey, boolean hasMutable,
			boolean postponed, Decision[] results, int immediateDecisionIdx) {
		if (postponed) {
			int immediateDecision = immediateDecisionIdx < 0 ? DENIED : results[immediateDecisionIdx].decision;
			// iterate over all postponed conditions;
			// if they all provide the same decision as the immediate decision then return
			// the immediate decision
			boolean allSameDecision = true;
			int i = immediateDecisionIdx < 0 ? results.length - 1 : immediateDecisionIdx - 1;
			for (; i >= 0 && allSameDecision; i--) {
				if ((results[i].decision & POSTPONED) == POSTPONED) {
					if ((results[i].decision & immediateDecision) == 0)
						allSameDecision = false;
					else
						results[i] = SecurityRow.DECISION_ABSTAIN; // we can clear postpones with the same decision as
																	// the immediate
				}
			}
			if (allSameDecision) {
				if (!hasMutable) {
					evaluationCache.put(evaluationCacheKey, immediateDecision);
				}
				return immediateDecision;
			}

			// we now are forced to postpone; we need to also remember the postponed
			// decisions and
			// the immediate decision if there is one.
			EquinoxSecurityManager equinoxManager = securityAdmin.getSupportedSecurityManager();
			if (equinoxManager == null) {
				// TODO this is really an error condition.
				// This should never happen. We checked for a supported manager when the row was
				// postponed
				if (!hasMutable) {
					evaluationCache.put(evaluationCacheKey, ABSTAIN);
				}
				return ABSTAIN;
			}
			equinoxManager.addConditionsForDomain(results);
		}
		return null;
	}

	void clearEvaluationCache() {
		evaluationCache.clear();
	}

	SecurityRow getRow(int i) {
		return rows.length <= i || i < 0 ? null : rows[i];
	}

	SecurityRow getRow(String name) {
		for (SecurityRow row : rows) {
			if (name.equals(row.getName())) {
				return row;
			}
		}
		return null;
	}

	SecurityRow[] getRows() {
		return rows;
	}

	String[] getEncodedRows() {
		String[] encoded = new String[rows.length];
		for (int i = 0; i < rows.length; i++)
			encoded[i] = rows[i].getEncoded();
		return encoded;
	}

	@Override
	public void add(Permission permission) {
		throw new SecurityException();
	}

	@Override
	public Enumeration<Permission> elements() {
		return Collections.emptyEnumeration();
	}

	@Override
	public boolean implies(Permission permission) {
		return (evaluate(null, permission) & SecurityTable.GRANTED) != 0;
	}
}
