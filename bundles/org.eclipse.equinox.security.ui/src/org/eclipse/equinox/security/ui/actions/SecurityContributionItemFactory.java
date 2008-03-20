/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.security.ui.actions;

import org.eclipse.equinox.internal.security.ui.SecurityStatusControl;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ContributionItemFactory;

public abstract class SecurityContributionItemFactory extends ContributionItemFactory {

	protected SecurityContributionItemFactory(String contributionItemId) {
		super(contributionItemId);
	}

	/**
	 * Workbench contribution item (id "securityStatus"): An icon for 
	 * evaluating and inspecting the security status of the system.
	 */
	public static final ContributionItemFactory SECURITY_STATUS = new ContributionItemFactory("securityStatus") {//$NON-NLS-1$
		public IContributionItem create(IWorkbenchWindow window) {
			if (window == null) {
				throw new IllegalArgumentException();
			}
			return new SecurityStatusControl(window, getId());
		}
	};
}
