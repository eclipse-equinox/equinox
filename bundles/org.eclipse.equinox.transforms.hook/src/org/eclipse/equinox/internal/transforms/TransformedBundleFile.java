/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.baseadaptor.AdaptorMsg;
import org.eclipse.osgi.internal.baseadaptor.AdaptorUtil;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * This class is capable of providing transformed versions of entries contained within a base bundle file.
 * For requests that transform bundle contents into local resources (such as file URLs) the transformed state of the bundle is written to the configuration area.
 */
public class TransformedBundleFile extends BundleFile {

	private BundleFile delegate;
	private BaseData data;
	private TransformerList transformers;
	private TransformInstanceListData templates;

	/**
	 * Create a wrapped bundle file.  
	 * Requests into this file will be compared to the list of known transformers and transformer templates and if there's a match the transformed entity is returned instead of the original.
	 * @param transformers the list of known transformers
	 * @param templates the list of known templates
	 * @param data the original data
	 * @param delegate the original file
	 */
	public TransformedBundleFile(TransformerList transformers, TransformInstanceListData templates, BaseData data, BundleFile delegate) {
		this.transformers = transformers;
		this.templates = templates;
		this.data = data;
		this.delegate = delegate;
	}

	public void close() throws IOException {
		delegate.close();
	}

	public boolean containsDir(String dir) {
		return delegate.containsDir(dir);
	}

	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	public File getBaseFile() {
		return delegate.getBaseFile();
	}

	public BundleEntry getEntry(String path) {

		final BundleEntry original = delegate.getEntry(path);
		if (data.getBundle() == null || path == null || original == null)
			return original;

		LazyInputStream stream = new LazyInputStream(new InputStreamProvider() {

			public InputStream getInputStream() throws IOException {
				return original.getInputStream();
			}
		});
		InputStream wrappedStream = getInputStream(stream, data.getBundle(), path);
		if (wrappedStream == null)
			return original;
		return new TransformedBundleEntry(this, original, wrappedStream);
	}

	/**
	 * Return the input stream that results from applying the given transformer
	 * URL to the provided input stream.
	 * 
	 * @param inputStream
	 *            the stream to transform
	 * @param bundle
	 *            the resource representing the transformer
	 * @return the transformed stream
	 */
	protected InputStream getInputStream(InputStream inputStream, Bundle bundle, String path) {
		String namespace = bundle.getSymbolicName();

		String[] transformTypes = templates.getTransformTypes();
		if (transformTypes.length == 0)
			return null;
		for (int i = 0; i < transformTypes.length; i++) {
			StreamTransformer transformer = transformers.getTransformer(transformTypes[i]);
			if (transformer == null)
				continue;
			TransformTuple[] transformTuples = templates.getTransformsFor(transformTypes[i]);
			if (transformTuples == null)
				continue;
			for (int j = 0; j < transformTuples.length; j++) {
				TransformTuple transformTuple = transformTuples[j];
				if (match(transformTuple.bundlePattern, namespace) && match(transformTuple.pathPattern, path)) {
					try {
						return transformer.getInputStream(inputStream, transformTuple.transformerUrl);
					} catch (IOException e) {
						TransformerHook.log(FrameworkLogEntry.ERROR, "Problem obtaining transformed stream from transformer : " //$NON-NLS-1$
								+ transformer.getClass().getName(), e);
					}
				}
			}
		}

		return null;
	}

	/**
	 * Return whether the given string matches the given pattern.
	 * 
	 * @param pattern
	 * @param string
	 * @return whether the given string matches the given pattern
	 */
	private boolean match(Pattern pattern, String string) {
		Matcher matcher = pattern.matcher(string);
		return matcher.matches();
	}

	public Enumeration getEntryPaths(String path) {
		return delegate.getEntryPaths(path);
	}

	/**
	 * This file is a copy of {@link ZipBundleFile#getFile(String, boolean)}
	 * with modifications.
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
					/* the entry is already cached */
					if (Debug.DEBUG_GENERAL)
						Debug.println("File already present: " + nested.getPath()); //$NON-NLS-1$
					if (nested.isDirectory())
						// must ensure the complete directory is extracted (bug
						// 182585)
						extractDirectory(path);
				} else {
					if (originalFile.isDirectory()) {
						if (!nested.mkdirs()) {
							if (Debug.DEBUG_GENERAL)
								Debug.println("Unable to create directory: " + nested.getPath()); //$NON-NLS-1$
							throw new IOException(NLS.bind(AdaptorMsg.ADAPTOR_DIRECTORY_CREATE_EXCEPTION, nested.getAbsolutePath()));
						}
						extractDirectory(path);
					} else {
						InputStream in = getEntry(path).getInputStream();
						if (in == null)
							return null;
						// if (in instanceof )
						/* the entry has not been cached */
						if (Debug.DEBUG_GENERAL)
							Debug.println("Creating file: " + nested.getPath()); //$NON-NLS-1$
						/* create the necessary directories */
						File dir = new File(nested.getParent());
						if (!dir.exists() && !dir.mkdirs()) {
							if (Debug.DEBUG_GENERAL)
								Debug.println("Unable to create directory: " + dir.getPath()); //$NON-NLS-1$
							throw new IOException(NLS.bind(AdaptorMsg.ADAPTOR_DIRECTORY_CREATE_EXCEPTION, dir.getAbsolutePath()));
						}
						/* copy the entry to the cache */
						AdaptorUtil.readFile(in, nested);
						if (nativeCode)
							setPermissions(nested);
					}
				}

				return nested;
			}
		} catch (IOException e) {
			if (Debug.DEBUG_GENERAL)
				Debug.printStackTrace(e);
		}
		return null;
	}

	/**
	 * Answers whether the resource at the given path or any of its children has
	 * a transform associated with it.
	 * 
	 * @param path
	 * @return whether the resource at the given path or any of its children has
	 * a transform associated with it.
	 */
	private boolean hasTransforms(String path) {
		if (!transformers.hasTransformers())
			return false;
		return templates.hasTransformsFor(data.getBundle());
	}

	/**
	 * Extracts a directory and all sub content to disk
	 * 
	 * @param dirName
	 *            the directory name to extract
	 * @return the File used to extract the content to. A value of
	 *         <code>null</code> is returned if the directory to extract does
	 *         not exist or if content extraction is not supported.
	 * 
	 * This method is derived from ZipBundleFile#extractDirectory(String).
	 */
	protected synchronized File extractDirectory(String dirName) {
		Enumeration entries = delegate.getEntryPaths(dirName);

		while (entries.hasMoreElements()) {
			String entryPath = (String) entries.nextElement();
			if (entryPath.startsWith(dirName))
				getFile(entryPath, false);
		}
		return getExtractFile(dirName);
	}

	protected File getExtractFile(String entryName) {
		if (data == null)
			return null;
		String path = ".tf"; /* put all these entries in this subdir *///$NON-NLS-1$
		String name = entryName.replace('/', File.separatorChar);
		/*
		 * if name has a leading slash
		 */
		if ((name.length() > 1) && (name.charAt(0) == File.separatorChar))
			path = path.concat(name);
		else
			path = path + File.separator + name;
		return data.getExtractFile(path);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public void open() throws IOException {
		delegate.open();
	}

	public String toString() {
		return delegate.toString();
	}
}
