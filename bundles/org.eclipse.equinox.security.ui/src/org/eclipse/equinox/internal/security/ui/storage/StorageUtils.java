/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.equinox.internal.security.ui.storage;

import org.eclipse.equinox.internal.security.storage.friends.InternalExchangeUtils;
import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.equinox.security.storage.provider.IProviderHints;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class StorageUtils {

	/**
	 * Get the shell from an active window. If not found, returns null.
	 */
	static public Shell getShell() {
		if (runningUI()) {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window != null)
				return window.getShell();
		}
		return null;
	}

	/**
	 * Determines if it is a good idea to show UI prompts
	 */
	static public boolean showUI(IPreferencesContainer container) {
		if (!runningUI() || InternalExchangeUtils.isJUnitApp())
			return false;
		if (container == null)
			return true;
		if (container.hasOption(IProviderHints.PROMPT_USER)) {
			Object promptHint = container.getOption(IProviderHints.PROMPT_USER);
			if (promptHint instanceof Boolean)
				return ((Boolean) promptHint).booleanValue();
		}
		return true;
	}

	static public boolean runningUI() {
		return PlatformUI.isWorkbenchRunning();
	}

}
