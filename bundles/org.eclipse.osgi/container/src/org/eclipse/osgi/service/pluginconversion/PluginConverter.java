/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
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
package org.eclipse.osgi.service.pluginconversion;

import java.io.File;
import java.util.Dictionary;

/**
 * A plug-in convertor is able to convert plug-in manifest files
 * (<code>plugin.xml</code>) and fragment manifest files
 * (<code>fragment.xml</code>) to bundle manifest files
 * (<code>MANIFEST.MF</code>).
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @since 3.0
 */
public interface PluginConverter {
	/**
	 * Converts a plug-in/fragment manifest at the given source base location (a
	 * directory or jar file) and generates a corresponding bundle manifest at the
	 * given default target location (a file).
	 *
	 * @param pluginBaseLocation     the base location for the plug-in/fragment
	 *                               manifest to be converted (a directory or jar
	 *                               file, e.g. the plug-in install location)
	 * @param bundleManifestLocation the location for the bundle manifest to be
	 *                               generated (including the file name).
	 * @param compatibilityManifest  a boolean indicating if the manifest should
	 *                               contain headers to run in backward
	 *                               compatibility
	 * @param target                 a string indicating the version of the runtime
	 *                               for which the manifest generated is targeted
	 * @param analyseJars            a boolean indicating if the code jars of the
	 *                               given plugin must be analysed. When set to
	 *                               false the Export-Package header will not be set
	 *                               in the bundle manifest.
	 * @param devProperties          a dictionary of development time classpath
	 *                               properties. The dictionary contains a mapping
	 *                               from plugin id to development time classpath. A
	 *                               value of <code>null</code> indicates that the
	 *                               default development time classpath properties
	 *                               will be used.
	 * @return the generated manifest file location, if a bundle manifest was
	 *         successfully generated (or already existed), <code>null</code>
	 *         otherwise.
	 * @throws PluginConversionException if an error occurs while converting the
	 *                                   manifest
	 */
	public File convertManifest(File pluginBaseLocation, File bundleManifestLocation, boolean compatibilityManifest,
			String target, boolean analyseJars, Dictionary<String, String> devProperties)
			throws PluginConversionException;

	/**
	 * Converts a plug-in/fragment manifest at the given source base location (a
	 * directory or jar file) and generates a corresponding bundle manifest returned
	 * as a dictionary.
	 *
	 * @param pluginBaseLocation the base location for the plug-in/fragment manifest
	 *                           to be converted (a directory or jar file, e.g. the
	 *                           plug-in install location)
	 * @param compatibility      a boolean indicating if the manifest should contain
	 *                           headers to run in backward compatibility
	 * @param target             a string indicating the version of the runtime for
	 *                           which the manifest generated is targeted
	 * @param analyseJars        a boolean indicating if the code jars of the given
	 *                           plugin must be analysed. When set to false the
	 *                           Export-Package header will not be set in the bundle
	 *                           manifest.
	 * @param devProperties      a dictionary of development time classpath
	 *                           properties. The dictionary contains a mapping from
	 *                           plugin id to development time classpath. A value of
	 *                           <code>null</code> indicates that the default
	 *                           development time classpath properties will be used.
	 * @return the generated manifest as a dictionary, if a bundle manifest was
	 *         successfully generated, <code>null</code> otherwise
	 * @throws PluginConversionException if an error occurs while converting the
	 *                                   manifest
	 */
	public Dictionary<String, String> convertManifest(File pluginBaseLocation, boolean compatibility, String target,
			boolean analyseJars, Dictionary<String, String> devProperties) throws PluginConversionException;

	/**
	 * Construct a bundle manifest file from the given dictionary and write it out
	 * to the specified location in the file system.
	 * <p>
	 * If the <code>compatibilityManifest</code> parameter is <code>true</code> then
	 * the generated manifest will include the necessary headers to all the manifest
	 * to be run in backwards compatibility mode.
	 * </p>
	 *
	 * @param generationLocation    the location for the bundle manifest to be
	 *                              written
	 * @param manifestToWrite       the dictionary to write into generationLocation
	 *                              file
	 * @param compatibilityManifest a boolean indicating if the file should contain
	 *                              headers to allow running in backward
	 *                              compatibility mode
	 * @throws PluginConversionException if an error occurs while writing the given
	 *                                   manifest
	 */
	public void writeManifest(File generationLocation, Dictionary<String, String> manifestToWrite,
			boolean compatibilityManifest) throws PluginConversionException;
}
