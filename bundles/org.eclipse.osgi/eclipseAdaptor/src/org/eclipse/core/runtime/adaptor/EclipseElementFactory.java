/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.io.IOException;
import java.io.InputStream;
import java.security.ProtectionDomain;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.framework.adaptor.core.AbstractBundleData;
import org.eclipse.osgi.framework.internal.defaultadaptor.*;

public class EclipseElementFactory extends AdaptorElementFactory {

	public AbstractBundleData getBundleData(DefaultAdaptor adaptor) throws IOException {
		return new EclipseBundleData(adaptor);
	}

	public org.eclipse.osgi.framework.adaptor.BundleClassLoader createClassLoader(ClassLoaderDelegate delegate, ProtectionDomain domain, String[] bundleclasspath, DefaultBundleData data) {
		return new EclipseClassLoader(delegate, domain, bundleclasspath, data);
	}
	
}
