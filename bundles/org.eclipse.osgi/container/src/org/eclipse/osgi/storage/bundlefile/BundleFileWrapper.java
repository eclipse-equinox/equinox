/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
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
package org.eclipse.osgi.storage.bundlefile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import org.eclipse.osgi.container.Module;
import org.eclipse.osgi.internal.hookregistry.BundleFileWrapperFactoryHook;
import org.eclipse.osgi.storage.BundleInfo;

/**
 * A {@link BundleFile bundle file} decorator.
 * <p/>
 * Clients wishing to modify or extend the behavior of a bundle file at runtime
 * should extend this class instead. A hook is provided by the related {@link
 * BundleFileWrapperFactoryHook abstract factory} class in response to a
 * {@link BundleFileWrapperFactoryHook#wrapBundleFile(BundleFile,
 * BundleInfo.Generation, boolean) call} from the framework.
 */
public class BundleFileWrapper extends BundleFile {
	private final BundleFile bundleFile;

	/**
	 * Creates a new <code>BundleFileWrapper</code> instance wrapping the
	 * given {@link BundleFile bundle file}.
	 *
	 * @param bundleFile - The bundle file to wrap.
	 * @throws NullPointerException - If the bundle file is <code>null</code>.
	 */
	public BundleFileWrapper(BundleFile bundleFile) {
		super(bundleFile.getBaseFile());
		this.bundleFile = bundleFile;
	}

	@Override
	public File getFile(String path, boolean nativeCode) {
		return bundleFile.getFile(path, nativeCode);
	}

	@Override
	public BundleEntry getEntry(String path) {
		return bundleFile.getEntry(path);
	}

	@Override
	public Enumeration<String> getEntryPaths(String path) {
		return bundleFile.getEntryPaths(path);
	}

	@Override
	public Enumeration<String> getEntryPaths(String path, boolean recurse) {
		return bundleFile.getEntryPaths(path, recurse);
	}

	/**
	 * Get the wrapped bundle file.
	 *
	 * @return The wrapped bundle file.
	 */
	public BundleFile getBundleFile() {
		return bundleFile;
	}

	@Override
	public void close() throws IOException {
		bundleFile.close();
	}

	@Override
	public void open() throws IOException {
		bundleFile.open();
	}

	@Override
	public boolean containsDir(String dir) {
		return bundleFile.containsDir(dir);
	}

	@Override
	protected URL createResourceURL(BundleEntry bundleEntry, Module hostModule, int index, String path) {
		return bundleFile.createResourceURL(bundleEntry, hostModule, index, path);
	}

}
