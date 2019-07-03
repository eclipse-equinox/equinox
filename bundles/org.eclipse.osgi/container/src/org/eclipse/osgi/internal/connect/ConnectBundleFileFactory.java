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
package org.eclipse.osgi.internal.connect;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import org.eclipse.osgi.internal.connect.ConnectHookConfigurator.ConnectModules;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.hookregistry.BundleFileWrapperFactoryHook;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;
import org.eclipse.osgi.storage.bundlefile.MRUBundleFileList;
import org.osgi.framework.connect.ConnectModule;

public class ConnectBundleFileFactory implements BundleFileWrapperFactoryHook {
	final ConnectModules connectModules;
	final Debug debug;

	public ConnectBundleFileFactory(ConnectModules connectModules, Debug debug) {
		this.connectModules = connectModules;
		this.debug = debug;
	}

	@Override
	public BundleFileWrapper wrapBundleFile(BundleFile bundleFile, Generation generation, boolean base) {
		ConnectModule m = connectModules.getConnectModule(generation.getBundleInfo().getLocation());
		if (m == null) {
			return null;
		}
		MRUBundleFileList mruList = generation.getBundleInfo().getStorage().getMRUBundleFileList();
		try {
			ConnectBundleFile connectBundleFile = new ConnectBundleFile(m, bundleFile.getBaseFile(), generation, mruList, debug);
			return new ConnectBundleFileWrapper(bundleFile, connectBundleFile);
		} catch (IOException e) {
			// TODO should log this
		}
		return null;
	}

	public static class ConnectBundleFileWrapper extends BundleFileWrapper {
		private final ConnectBundleFile connectBundleFile;

		public ConnectBundleFileWrapper(BundleFile bundleFile, ConnectBundleFile connectBundleFile) {
			super(bundleFile);
			this.connectBundleFile = connectBundleFile;
		}

		@Override
		public BundleEntry getEntry(final String path) {
			return connectBundleFile.getEntry(path);
		}

		@Override
		public File getFile(String path, boolean nativeCode) {
			return connectBundleFile.getFile(path, nativeCode);
		}

		@Override
		public Enumeration<String> getEntryPaths(String path) {
			return connectBundleFile.getEntryPaths(path);
		}

		@Override
		public Enumeration<String> getEntryPaths(String path, boolean recurse) {
			return connectBundleFile.getEntryPaths(path, recurse);
		}

		@Override
		public boolean containsDir(String dir) {
			return connectBundleFile.containsDir(dir);
		}

		@Override
		public void open() throws IOException {
			connectBundleFile.open();
			super.open();
		}

		@Override
		public void close() throws IOException {
			super.close();
			connectBundleFile.close();
		}

		ConnectBundleFile getConnectBundleFile() {
			return connectBundleFile;
		}
	}
}
