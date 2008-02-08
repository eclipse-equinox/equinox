/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.transforms;

import java.io.IOException;
import java.net.URLConnection;
import java.util.Properties;

import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.HookConfigurator;
import org.eclipse.osgi.baseadaptor.HookRegistry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.baseadaptor.hooks.BundleFileWrapperFactoryHook;
import org.eclipse.osgi.framework.adaptor.FrameworkAdaptor;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;

public class TransformerHook implements BundleFileWrapperFactoryHook,
		HookConfigurator, AdaptorHook {
	private TransformerList transformers;
	private TransformInstanceListData templates;
	private static BaseAdaptor ADAPTOR;

	public BundleFile wrapBundleFile(BundleFile bundleFile, Object content,
			BaseData data, boolean base) throws IOException {
		if (transformers == null || templates == null)
			return null;
		return new TransformedBundleFile(transformers, templates, data,
				bundleFile);
	}

	public void addHooks(HookRegistry hookRegistry) {
		hookRegistry.addAdaptorHook(this);
		hookRegistry.addBundleFileWrapperFactoryHook(this);
	}

	public void addProperties(Properties properties) {
	}

	public FrameworkLog createFrameworkLog() {
		return null;
	}

	public void frameworkStart(BundleContext context) throws BundleException {
		try {
			this.transformers = new TransformerList(context);
		} catch (InvalidSyntaxException e) {
			throw new BundleException(
					"Problem registering service tracker: transformers", e);
		}
		try {
			this.templates = new TransformInstanceListData(context);
		} catch (InvalidSyntaxException e) {
			transformers.close();
			transformers = null;
			throw new BundleException(
					"Problem registering service tracker: templates", e);
		}

	}

	public void frameworkStop(BundleContext context) throws BundleException {
		transformers.close();
		templates.close();
	}

	protected BundleContext getContext() {
		return TransformerHook.ADAPTOR.getContext();
	}

	public void frameworkStopping(BundleContext context) {
	}

	public void handleRuntimeError(Throwable error) {
	}

	public void initialize(BaseAdaptor adaptor) {
		TransformerHook.ADAPTOR = adaptor;
	}

	public URLConnection mapLocationToURLConnection(String location)
			throws IOException {
		return null;
	}

	public boolean matchDNChain(String pattern, String[] dnChain) {
		return false;
	}

	static void log(int severity, String msg, Throwable t) {
		if (TransformerHook.ADAPTOR == null) {
			System.err.println(msg);
			t.printStackTrace();
			return;
		}
		FrameworkLogEntry entry = new FrameworkLogEntry(
				FrameworkAdaptor.FRAMEWORK_SYMBOLICNAME, severity, 0, msg, 0,
				t, null);
		TransformerHook.ADAPTOR.getFrameworkLog().log(entry);
	}
}
