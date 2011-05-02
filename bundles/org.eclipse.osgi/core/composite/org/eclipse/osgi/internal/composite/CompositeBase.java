/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.composite;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.osgi.framework.adaptor.BundleData;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.framework.internal.core.BundleHost;
import org.eclipse.osgi.internal.module.CompositeResolveHelper;
import org.eclipse.osgi.service.internal.composite.CompositeModule;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

/**
 * This is a base class for both composite and surrogate bundles.
 */
public abstract class CompositeBase extends BundleHost implements CompositeResolveHelper, CompositeModule {
	protected static String PROP_COMPOSITE = "org.eclipse.equinox.Composite"; //$NON-NLS-1$
	protected static String PROP_PARENTFRAMEWORK = "org.eclipse.equinox.parentFramework"; //$NON-NLS-1$

	protected final Framework companionFramework;
	protected final ThreadLocal resolving = new ThreadLocal();

	public CompositeBase(BundleData bundledata, org.eclipse.osgi.framework.internal.core.Framework framework) throws BundleException {
		super(bundledata, framework);
		this.companionFramework = findCompanionFramework(framework, bundledata);
	}

	/* 
	 * Finds the companion framework for the composite/surrogate.
	 * For surrogate bundles this is the parent framework.
	 * For composite bundles this is the child framework.
	 */
	protected abstract Framework findCompanionFramework(org.eclipse.osgi.framework.internal.core.Framework thisFramework, BundleData thisData) throws BundleException;

	/*
	 * Gets the companion bundle for the composite/surrogate.
	 * For surrogate bundles this is the composite bundle.
	 * For composite bundles this is the surrogate bundle.
	 */
	abstract protected Bundle getCompanionBundle();

	protected boolean isSurrogate() {
		return false;
	}

	public BundleDescription getCompositeDescription() {
		return getBundleDescription();
	}

	public ClassLoaderDelegate getDelegate() {
		return getBundleLoader();
	}

	public void refreshContent() {
		resolving.set(Boolean.TRUE);
		try {
			framework.getPackageAdmin().refreshPackages(new Bundle[] {this}, true, null);
		} finally {
			resolving.set(null);
		}
	}

	public boolean resolveContent() {
		resolving.set(Boolean.TRUE);
		try {
			return framework.getPackageAdmin().resolveBundles(new Bundle[] {this});
		} finally {
			resolving.set(null);
		}
	}

	public void started(CompositeModule surrogate) {
		// nothing
	}

	public void stopped(CompositeModule surrogate) {
		// nothing
	}

	public void updateContent(InputStream content) throws BundleException {
		super.update(content);
	}

	public void update() throws BundleException {
		throw new BundleException("Cannot update composite bundles", BundleException.INVALID_OPERATION); //$NON-NLS-1$
	}

	public void update(InputStream in) throws BundleException {
		try {
			in.close();
		} catch (IOException e) {
			// ignore
		}
		throw new BundleException("Cannot update composite bundles", BundleException.INVALID_OPERATION); //$NON-NLS-1$
	}
}
