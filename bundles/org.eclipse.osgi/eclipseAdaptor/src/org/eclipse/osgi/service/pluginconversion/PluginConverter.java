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
public interface PluginConverter {
	/**
	 * Converts a plug-in/fragment manifest at the given source base location (a directory) and 
	 * generates a corresponding bundle manifest at the given default target locaton (a file).
	 * 
	 * @param pluginBaseLocation the base location for the plug-in/fragment manifest to be converted
	 * (a directory, e.g. the plug-in install location)
	 * @param bundleManifestLocation the location for the bundle manifest to be generated
	 * (including the file name)
	 * @param compatibilityManifest a boolean indicating if the manifest should contain headers to run with  
	 * the backward compatibility
	 * @return <code>true</code>, if a bundle manifest was successfully 
	 * generated (or already existed) in the specified location, <code>false</code> otherwise
	 */	
	public boolean convertManifest(File pluginBaseLocation, File bundleManifestLocation, boolean compatibilityManifest);
	/**
	 * Converts a plug-in/fragment manifest at the given source location (a directory) and 
	 * generates a corresponding bundle manifest at the default target location (the same 
	 * location used by the Eclipse adaptor to keep its own cached manifests.  
	 * 
	 * @param pluginLocation the location for the plug-in/fragment manifest to be converted
	 * @param compatibilityManifest a boolean indicating if the manifest should contain headers to run with  
	 * the backward compatibility 
	 * @return the generated manifest file location, if a bundle manifest was successfully 
	 * generated (or already existed), <code>null</code> otherwise
	 */
	public File convertManifest(File pluginLocation, boolean compatibilityManifest);
	
	/**
	 * 
	 * @param pluginLocation
	 * @return
	 * @deprecated @see #convertManifest(File, boolean)
	 */
	public File convertManifest(File pluginLocation);
	
	/**
	 * 
	 * @param pluginLocation
	 * @return
	 * @deprecated @see #convertManifest(File, File, boolean)
	 */
	public boolean convertManifest(File pluginBaseLocation, File bundleManifestLocation);
}