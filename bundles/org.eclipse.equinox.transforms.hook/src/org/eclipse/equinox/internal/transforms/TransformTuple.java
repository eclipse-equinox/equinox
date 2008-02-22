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

import java.net.URL;
import java.util.regex.Pattern;

/**
 * Class that represents an association between a bundle pattern, a path pattern, and the location of a transformer to apply to any resource that matches both the bundle and path pattern.
 */
public class TransformTuple {

	/**
	 * Constant used when registering transform tuples to identify the type of transformer they should be assigned to.
	 */
	public static final String TRANSFORMER_TYPE = "equinox.transformerType"; //$NON-NLS-1$
	public Pattern bundlePattern;
	public Pattern pathPattern;
	public URL transformerUrl;
}