/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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

import org.eclipse.osgi.internal.hookregistry.BundleFileWrapperFactoryHook;

/**
 * Used to chain the BundleFile objects returned from {@link BundleFileWrapperFactoryHook}.  
 * This class is useful for traversing the chain of wrapped bundle files.
 */
public class BundleFileWrapperChain extends BundleFileWrapper {
	private final BundleFile wrapped;
	private final BundleFileWrapperChain next;

	public BundleFileWrapperChain(BundleFile wrapped, BundleFileWrapperChain next) {
		super(wrapped);
		this.wrapped = wrapped;
		this.next = next;
	}

	@Override
	public String toString() {
		return wrapped.toString();
	}

	/**
	 * The BundleFile that is wrapped
	 * @return the BunldeFile that is wrapped
	 */
	public BundleFile getWrapped() {
		return wrapped;
	}

	/**
	 * The next WrapperBundleFile in the chain.  A <code>null</code> value
	 * is returned if this is the end of the chain.
	 * @return the next WrapperBundleFile
	 */
	public BundleFileWrapperChain getNext() {
		return next;
	}
}
