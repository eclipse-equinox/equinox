/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import org.eclipse.core.internal.runtime.DataArea;

/**
 * The class contains a set of utilities working platform metadata area.
 * This class can only be used if OSGi plugin is available.
 * 
 * Copied from InternalPlatform as of August 30, 2005.
 * @since org.eclipse.equinox.common 3.2
 */
public class MetaDataKeeper {

	private static DataArea metaArea = null;

	/**
	 * Returns the object which defines the location and organization
	 * of the platform's meta area.
	 */
	public static DataArea getMetaArea() {
		if (metaArea != null)
			return metaArea;

		metaArea = new DataArea();
		return metaArea;
	}
}
