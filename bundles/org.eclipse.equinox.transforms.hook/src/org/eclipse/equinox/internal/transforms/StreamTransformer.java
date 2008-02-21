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

package org.eclipse.equinox.internal.transforms;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * This class represents the fundamental building block of the transformer system.  
 * Implementations of this class are capable of transforming an input stream based on a given transformer url.
 * The meaning and content of this URL are unspecified - it is the transformers responsibility to interpret these as need be.
 */
public abstract class StreamTransformer {
	/**
	 * Provided a transformed version of the provided input stream.
	 * @param inputStream the original stream
	 * @param transformerUrl an url that may be used by the transformer in determining the proper transform to invoke.
	 * @return the transformed stream
	 * @throws IOException thrown if there is an issue invoking the transform
	 */
	public abstract InputStream getInputStream(InputStream inputStream, URL transformerUrl) throws IOException;
}
