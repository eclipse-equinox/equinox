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
package org.eclipse.equinox.internal.app;

import org.osgi.framework.Bundle;

public class ProviderExtensionBranding implements IBranding {
	Object product;
	public ProviderExtensionBranding(Object product) {
		this.product = product;
	}

	public String getApplication() {
		return (String) EclipseAppContainer.callMethod(product, "getApplication", null, null); //$NON-NLS-1$
	}

	public Bundle getDefiningBundle() {
		return (Bundle) EclipseAppContainer.callMethod(product, "getDefiningBundle", null, null); //$NON-NLS-1$
	}

	public String getDescription() {
		return (String) EclipseAppContainer.callMethod(product, "getDescription", null, null); //$NON-NLS-1$
	}

	public String getId() {
		return (String) EclipseAppContainer.callMethod(product, "getId", null, null); //$NON-NLS-1$
	}

	public String getName() {
		return (String) EclipseAppContainer.callMethod(product, "getName", null, null); //$NON-NLS-1$
	}

	public String getProperty(String key) {
		return (String) EclipseAppContainer.callMethod(product, "getProperty", new Class[] {String.class}, new Object[] {key}); //$NON-NLS-1$
	}

	public Object getProduct() {
		return product;
	}

}
