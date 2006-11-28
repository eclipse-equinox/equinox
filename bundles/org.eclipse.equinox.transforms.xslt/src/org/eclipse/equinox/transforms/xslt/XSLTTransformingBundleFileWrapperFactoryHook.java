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

package org.eclipse.equinox.transforms.xslt;

import java.io.IOException;

import org.eclipse.equinox.transforms.TransformingBundleFileWrapperFactoryHook;
import org.eclipse.equinox.transforms.CSVTransformingBundleFile.TransformList;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;

public class XSLTTransformingBundleFileWrapperFactoryHook extends
		TransformingBundleFileWrapperFactoryHook {

	private TransformList transformList;

	public BundleFile wrapBundleFile(BundleFile bundleFile, Object content,
			BaseData data, boolean base) throws IOException {
		if (transformList != null)
			return new XSLTBundleFile(getContext(), data, bundleFile,
					transformList);
		return null;
	}

	public void frameworkStart(BundleContext context) throws BundleException {
		super.frameworkStart(context);

		try {
			transformList = new TransformList(context,
					XSLTTransformingBundleFileWrapperFactoryHook.class
							.getName());
			transformList.open();
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}
	}

	public void frameworkStop(BundleContext context) throws BundleException {
		super.frameworkStop(context);
		transformList.close();
	}
}
