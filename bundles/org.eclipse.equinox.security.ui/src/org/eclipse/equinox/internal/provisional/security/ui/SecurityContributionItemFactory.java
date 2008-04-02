/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.provisional.security.ui;

import org.eclipse.equinox.internal.security.ui.SecurityStatusControl;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ContributionItemFactory;

/**
 * Access to security related contribution items for the workbench.
 * <p>
 * Clients may declare subclasses that provide additional application-specific
 * contribution item factories.
 * </p>
 */
public abstract class SecurityContributionItemFactory {

	/**
	 * Id of contribution items created by this factory.
	 */
	private final String contributionItemId;

	/**
	* Creates a new contribution item factory with the given id.
	* @param contributionItemId the id of contribution items created by this factory
	*/
	protected SecurityContributionItemFactory(String contributionItemId) {
		this.contributionItemId = contributionItemId;
	}

	/**
	 * Returns the id of this contribution item factory.
	 * @return the id of contribution items created by this factory
	 */
	public String getId() {
		return contributionItemId;
	}

	/**
	 * Workbench contribution item (id &quot;securityStatus&quot;): An icon for 
	 * evaluating and inspecting the security status of the system.
	 */
	public static final ContributionItemFactory SECURITY_STATUS = new ContributionItemFactory("securityStatus") {//$NON-NLS-1$
		public IContributionItem create(IWorkbenchWindow window) {
			if (window == null)
				throw new IllegalArgumentException();
			return new SecurityStatusControl(window, getId());
		}
	};
}
