/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.services.resolver;

import java.io.File;
import org.eclipse.osgi.internal.baseadaptor.StateManager;
import org.osgi.framework.BundleContext;

public class SimplePlatformAdmin extends StateManager {
	public SimplePlatformAdmin(File bundleRootDir, BundleContext context) {
		super(new File(bundleRootDir, ".state"), new File(bundleRootDir, ".lazy"), context); //$NON-NLS-1$//$NON-NLS-2$
		createSystemState();
	}
}
