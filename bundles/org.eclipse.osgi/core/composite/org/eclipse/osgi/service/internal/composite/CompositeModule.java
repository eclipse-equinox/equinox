/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.internal.composite;

import java.io.InputStream;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.BundleException;

/**
 * An internal interface only used by the composite implementation
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface CompositeModule {
	public void updateContent(InputStream content) throws BundleException;

	public void refreshContent();

	public boolean resolveContent();

	public BundleDescription getCompositeDescription();

	public ClassLoaderDelegate getDelegate();

	public void started(CompositeModule compositeBundle);

	public void stopped(CompositeModule compositeBundle);
}
