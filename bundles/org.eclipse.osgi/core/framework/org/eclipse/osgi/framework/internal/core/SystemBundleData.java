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

package org.eclipse.osgi.framework.internal.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Dictionary;
import java.util.Enumeration;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.util.Headers;


public class SystemBundleData implements BundleData {

	Headers manifest;
	Version version;

	public SystemBundleData(Headers manifest){
		this.manifest = manifest;
		String sVersion=(String)manifest.get(Constants.BUNDLE_VERSION);
		if (sVersion != null) {
			version = new Version(sVersion);
		}
	}

	public BundleClassLoader createClassLoader(
		ClassLoaderDelegate delegate,
		ProtectionDomain domain,
		String[] bundleclasspath) {
		return null;
	}

	public URL getEntry(String path) {
		return null;
	}

	public Enumeration getEntryPaths(String path) {
		return null;
	}

	public String findLibrary(String libname) {
		return null;
	}

	public void installNativeCode(String[] nativepaths) {
	}

	public File getDataFile(String path) {
		return null;
	}

	public Dictionary getManifest() {
		return manifest;
	}

	public long getBundleID() {
		return 0;
	}

	public String getLocation(){
		return Constants.SYSTEM_BUNDLE_LOCATION;
	}

	public Dictionary getHeaders() {
		return manifest;
	}

	public void close() {
	}

	public void open(){
	}

	public boolean isFragment() {
		return false;
	}

	public void setBundle(org.osgi.framework.Bundle bundle){
		// do nothing.
	}


	// The system bundle does not have any meta data capabilities so the following methods just
	// do nothing or return dummy values.
	public int getStartLevel() {
		return 0;
	}

	public int getStatus() {
		return 0;
	}

	public void setStartLevel(int value) {
	}

	public void setStatus(int value) {
	}

	public void save() throws IOException {
	}

	public String getUniqueId() {
		return (String)getHeaders().get(Constants.BUNDLE_GLOBALNAME);
	}

	public Version getVersion() {
		return version;
	}
	public String getClassPath() {
		return (String)getHeaders().get(Constants.BUNDLE_CLASSPATH);
	}
	public String getActivator() {
		return (String)getHeaders().get(Constants.BUNDLE_ACTIVATOR);
	}
	public String getDynamicImports(){
		return (String)getHeaders().get(Constants.DYNAMICIMPORT_PACKAGE);
	}
	public String getExecutionEnvironment(){
		return (String)getHeaders().get(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
	}
}
