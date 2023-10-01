/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
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

package org.eclipse.osgi.storage.url.reference;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.osgi.storage.ContentProvider;

/**
 * InputStream subclass which provides a reference (via File) to the data rather
 * than allowing the input stream to be directly read.
 */
public class ReferenceInputStream extends InputStream implements ContentProvider {
	private final File reference;

	public ReferenceInputStream(File reference) {
		this.reference = reference;
	}

	/*
	 * This method should not be called.
	 */
	@Override
	public int read() throws IOException {
		throw new IOException();
	}

	public File getReference() {
		return getContent();
	}

	@Override
	public File getContent() {
		return reference;
	}

	@Override
	public Type getType() {
		return Type.REFERENCE;
	}
}
