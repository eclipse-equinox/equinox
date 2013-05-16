/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.util;

public final class SupplementDebug {

	private SupplementDebug() {
		// prevent construction
	}

	// these static debug options are left overs because it would be messy to fix references to them
	// this means that if multiple frameworks are using this class these debug options may get overwritten
	/**
	 * Manifest debug flag.
	 */
	public static boolean STATIC_DEBUG_MANIFEST = false; // "debug.manifest"
	/**
	 * Message debug flag.
	 */
	public static boolean STATIC_DEBUG_MESSAGE_BUNDLES = false; //"/debug/messageBundles"
}
