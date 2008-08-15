/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.permadmin;

import java.security.Permission;
import org.eclipse.osgi.internal.permadmin.SecurityRow.Decision;

public class SecurityTable {
	static final int GRANTED = 0x0001;
	static final int DENIED = 0x0002;
	static final int ABSTAIN = 0x0004;
	static final int POSTPONED = 0x0008;

	private final SecurityRow[] rows;
	private final SecurityAdmin securityAdmin;

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
		if (isEmpty())
			return ABSTAIN;
		boolean postponed = false;
		Decision[] results = new Decision[rows.length];
		int immediateDecision = -1;
		// evaluate each row
		for (int i = 0; i < rows.length; i++) {
			try {
				results[i] = rows[i].evaluate(bundlePermissions, permission);
			} catch (Throwable t) {
				// TODO log?
				results[i] = SecurityRow.DECISION_ABSTAIN;
			}
			if ((results[i].decision & ABSTAIN) != 0)
				continue; // ignore this row and continue to next row
			if ((results[i].decision & POSTPONED) != 0) {
				// row is postponed; we can no longer return quickly on a denied decision
				postponed = true;
				continue; // continue to next row
			}
			if (!postponed)
				// no postpones encountered yet; we can return the decision quickly
				return results[i].decision; // return GRANTED or DENIED
			// got an immediate answer; but it is after a postponed condition.
			// no need to process the rest of the rows
			immediateDecision = i;
			break;
		}
		if (postponed) {
			// iterate over all postponed conditions; 
			// if they all provide the same decision as the immediate decision then return the immediate decision
			boolean allSameDecision = immediateDecision > 0;
			for (int i = immediateDecision - 1; i >= 0 && allSameDecision; i--) {
				if (results[i] == null)
					continue;
				if ((results[i].decision & POSTPONED) != 0) {
					if ((results[i].decision & results[immediateDecision].decision) == 0)
						allSameDecision = false;
					else
						results[i] = SecurityRow.DECISION_ABSTAIN; // we can clear postpones with the same decision as the immediate
				}
			}
			if (allSameDecision)
				return results[immediateDecision].decision;
			// if there is no immediate Decision then check to make sure there is at lease one Grant postponed decision
			if (immediateDecision < 0) {
				boolean allDeny = true;
				for (int i = results.length - 1; i >= 0 && allDeny; i--) {
					if (results[i] == null)
						continue;
					if ((results[i].decision & GRANTED) != 0)
						allDeny = false;
				}
				if (allDeny)
					return DENIED;
			}
			// we now are forced to postpone; we need to also remember the postponed decisions and 
			// the immediate decision if there is one.
			EquinoxSecurityManager equinoxManager = securityAdmin.getSupportedSecurityManager();
			if (equinoxManager == null)
				// TODO this is really an error condition.
				// This should never happen.  We checked for a supported manager when the row was postponed
				return ABSTAIN;
			equinoxManager.addConditionsForDomain(results);
		}
		return postponed ? POSTPONED : ABSTAIN;
	}

	SecurityRow getRow(int i) {
		return rows.length <= i || i < 0 ? null : rows[i];
	}

	SecurityRow getRow(String name) {
		for (int i = 0; i < rows.length; i++) {
			if (name.equals(rows[i].getName()))
				return rows[i];
		}
		return null;
	}

	SecurityRow[] getRows() {
		return rows;
	}

	String[] getEncodedRows() {
		String[] encoded = new String[rows.length];
		for (int i = 0; i < rows.length; i++)
			encoded[i] = rows[i].toString();
		return encoded;
	}
}
