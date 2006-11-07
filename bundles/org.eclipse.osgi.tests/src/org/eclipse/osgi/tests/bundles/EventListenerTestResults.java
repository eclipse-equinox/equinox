/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import org.osgi.framework.*;

public class EventListenerTestResults extends TestResults implements BundleListener, FrameworkListener {
	public void bundleChanged(BundleEvent event) {
		addEvent(event);
	}

	public void frameworkEvent(FrameworkEvent event) {
		addEvent(event);
	}
}