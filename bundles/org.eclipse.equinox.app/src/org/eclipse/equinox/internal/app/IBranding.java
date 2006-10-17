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

package org.eclipse.equinox.internal.app;

import org.osgi.framework.Bundle;

public interface IBranding {

	public String getApplication();

	public String getName();

	public String getDescription();

	public String getId();

	public String getProperty(String key);
	
	public Bundle getDefiningBundle();

	public Object getProduct();
}
