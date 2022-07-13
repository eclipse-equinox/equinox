/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package org.eclipse.osgi.tests.bundles.classes;

import java.util.Collections;
import java.util.Hashtable;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	@Override
	public void start(BundleContext context) throws Exception {
		context.registerService(Activator.class, this, new Hashtable<>(Collections.singletonMap("activator", this.getClass().getSimpleName())));
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// do nothing
	}
}
