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
import java.util.Dictionary;

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
	 * (including the file name). If <code>null</code> is passed as a parameter, the manifest will be generated into the 
	 * pool of cached manifest. 
	 * @param compatibilityManifest a boolean indicating if the manifest should contain headers to run with  
	 * the backward compatibility
	 *	@return the generated manifest file location, if a bundle manifest was successfully 
	 * generated (or already existed), <code>null</code> otherwise.
	 */	
	public File convertManifest(File pluginBaseLocation, File bundleManifestLocation, boolean compatibilityManifest);
	
	/**
	 * Converts a plug-in/fragment manifest at the given source base location (a directory) and 
	 * generates a corresponding bundle manifest returned as a dictionary.
	 * 
	 * @param pluginBaseLocation the base location for the plug-in/fragment manifest to be converted
	 * (a directory, e.g. the plug-in install location)
	 * @param compatibilityManifest a boolean indicating if the manifest should contain headers to run with  
	 * the backward compatibility
	 *	@return the generated manifest as a dictionary,  if a bundle manifest was successfully 
	 * generated, <code>null</code> otherwise
	 */	
	public Dictionary convertManifest(File pluginBaseLocation, boolean compatibility);
	
}