/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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

package org.eclipse.equinox.internal.transforms;

import java.io.*;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.equinox.internal.transforms.LazyInputStream.InputStreamProvider;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.StorageUtil;
import org.eclipse.osgi.storage.bundlefile.*;
import org.osgi.framework.Bundle;

/**
 * This class is capable of providing transformed versions of entries contained
 * within a base bundle file. For requests that transform bundle contents into
 * local resources (such as file URLs) the transformed state of the bundle is
 * written to the configuration area.
 */
public class TransformedBundleFile extends BundleFileWrapper {

	private final TransformerHook transformerHook;
	private final BundleFile delegate;
	private final Generation generation;

	/**
	 * Create a wrapped bundle file. Requests into this file will be compared to the
	 * list of known transformers and transformer templates and if there's a match
	 * the transformed entity is returned instead of the original.
	 * 
	 * @param transformerHook the transformer hook
	 * @param generation      the original data
	 * @param delegate        the original file
	 */
	public TransformedBundleFile(TransformerHook transformerHook, Generation generation, BundleFile delegate) {
		super(delegate);
		this.transformerHook = transformerHook;
		this.generation = generation;
		this.delegate = delegate;
	}

	Generation getGeneration() {
		return generation;
	}

	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	public BundleEntry getEntry(String path) {

		final BundleEntry original = delegate.getEntry(path);
		if (generation.getRevision() == null || path == null || original == null)
			return original;

		LazyInputStream stream = new LazyInputStream(new InputStreamProvider() {

			@Override
			public InputStream getInputStream() throws IOException {
				return original.getInputStream();
			}
		});
		InputStream wrappedStream = getInputStream(stream, generation.getRevision().getBundle(), path);
		if (wrappedStream == null)
			return original;
		return new TransformedBundleEntry(this, original, wrappedStream);
	}

	/**
	 * Return the input stream that results from applying the given transformer URL
	 * to the provided input stream.
	 * 
	 * @param inputStream the stream to transform
	 * @param bundle      the resource representing the transformer
	 * @return the transformed stream
	 */
	protected InputStream getInputStream(InputStream inputStream, Bundle bundle, String path) {
		String namespace = bundle.getSymbolicName();

		String[] transformTypes = transformerHook.getTransformTypes();
		if (transformTypes.length == 0)
			return null;
		for (String transformType : transformTypes) {
			StreamTransformer transformer = transformerHook.getTransformer(transformType);
			if (transformer == null)
				continue;
			TransformTuple[] transformTuples = transformerHook.getTransformsFor(transformType);
			if (transformTuples == null)
				continue;
			for (TransformTuple transformTuple : transformTuples) {
				if (match(transformTuple.bundlePattern, namespace) && match(transformTuple.pathPattern, path)) {
					try {
						return transformer.getInputStream(inputStream, transformTuple.transformerUrl);
					} catch (IOException e) {
						generation.getBundleInfo().getStorage().getLogServices().log(EquinoxContainer.NAME,
								FrameworkLogEntry.ERROR, "Problem obtaining transformed stream from transformer : " //$NON-NLS-1$
										+ transformer.getClass().getName(),
								e);

					}
				}
			}
		}

		return null;
	}

	/**
	 * Return whether the given string matches the given pattern.
	 * 
	 * @return whether the given string matches the given pattern
	 */
	private boolean match(Pattern pattern, String string) {
		Matcher matcher = pattern.matcher(string);
		return matcher.matches();
	}

	/**
	 * This file is a copy of {@link ZipBundleFile#getFile(String, boolean)} with
	 * modifications.
	 */
	public File getFile(String path, boolean nativeCode) {
		File originalFile = delegate.getFile(path, nativeCode);

		if (originalFile == null)
			return null;
		if (!hasTransforms(path))
			return originalFile;
		try {
			File nested = getExtractFile(path);
			if (nested != null) {
				if (nested.exists()) {
					if (nested.isDirectory())
						// must ensure the complete directory is extracted (bug
						// 182585)
						extractDirectory(path);
				} else {
					if (originalFile.isDirectory()) {
						if (!nested.mkdirs()) {
							throw new IOException("Unable to create directory: " + nested.getAbsolutePath()); //$NON-NLS-1$
						}
						extractDirectory(path);
					} else {
						InputStream in = getEntry(path).getInputStream();
						if (in == null)
							return null;
						/* the entry has not been cached */
						/* create the necessary directories */
						File dir = new File(nested.getParent());
						if (!dir.exists() && !dir.mkdirs()) {
							throw new IOException("Unable to create directory: " + dir.getAbsolutePath()); //$NON-NLS-1$
						}
						/* copy the entry to the cache */
						StorageUtil.readFile(in, nested);
						if (nativeCode) {
							generation.getBundleInfo().getStorage().setPermissions(nested);
						}
					}
				}

				return nested;
			}
		} catch (IOException e) {
			// consider logging
		}
		return null;
	}

	/**
	 * Answers whether the resource at the given path or any of its children has a
	 * transform associated with it.
	 * 
	 * @return whether the resource at the given path or any of its children has a
	 *         transform associated with it.
	 */
	private boolean hasTransforms(String path) {
		if (!transformerHook.hasTransformers())
			return false;
		return transformerHook.hasTransformsFor(generation.getRevision().getBundle());
	}

	/**
	 * Extracts a directory and all sub content to disk
	 * 
	 * @param dirName the directory name to extract
	 * @return the File used to extract the content to. A value of <code>null</code>
	 *         is returned if the directory to extract does not exist or if content
	 *         extraction is not supported.
	 * 
	 *         This method is derived from ZipBundleFile#extractDirectory(String).
	 */
	protected synchronized File extractDirectory(String dirName) {
		Enumeration<String> entries = delegate.getEntryPaths(dirName);

		while (entries.hasMoreElements()) {
			String entryPath = entries.nextElement();
			if (entryPath.startsWith(dirName))
				getFile(entryPath, false);
		}
		return getExtractFile(dirName);
	}

	protected File getExtractFile(String entryName) {
		String path = ".tf"; /* put all these entries in this subdir *///$NON-NLS-1$
		String name = entryName.replace('/', File.separatorChar);
		/*
		 * if name has a leading slash
		 */
		if ((name.length() > 1) && (name.charAt(0) == File.separatorChar))
			path = path.concat(name);
		else
			path = path + File.separator + name;
		return generation.getExtractFile(path);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public String toString() {
		return delegate.toString();
	}
}
