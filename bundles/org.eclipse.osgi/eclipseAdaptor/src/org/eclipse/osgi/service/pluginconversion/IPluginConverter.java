/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.pluginconversion;

import java.io.File;

/**
 * The interface of the service that allows to convert plugin.xml into manifest.mf
 */
public interface IPluginConverter {
	/**
	 * Converts a plug-in/fragment manifest in the given source base location (a directory) and 
	 * generates a corresponding bundle manifest in the given default target locaton (a file).
	 * 
	 * @param pluginBaseLocation the base location for the plug-in/fragment manifest to be converted
	 * (a directory, e.g. the plug-in install location)
	 * @param bundleManifestLocation the location for the bundle manifest to be generated
	 * (including the file name) 
	 * @return <code>true</code>, if a bundle manifest was successfully 
	 * generated (or already existed) in the specified location, <code>false</code> otherwise
	 */	
	public boolean convertManifest(File pluginBaseLocation, File bundleManifestLocation);
	/**
	 * Converts a plug-in/fragment manifest in the given source location (a directory) and 
	 * generates a corresponding bundle manifest in a default target locaton.
	 * 
	 * @param pluginLocation the location for the plug-in/fragment manifest to be converted
	 * @return the generated manifest file location, if a bundle manifest was successfully 
	 * generated (or already existed), <code>null</code> otherwise
	 */
	public File convertManifest(File pluginLocation);
}
