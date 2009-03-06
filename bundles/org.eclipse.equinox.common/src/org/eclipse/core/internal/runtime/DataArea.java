/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

/**
 * This class can only be used if OSGi plugin is available
 */
public class DataArea {
	private static final String OPTION_DEBUG = "org.eclipse.equinox.common/debug"; //$NON-NLS-1$;

	/* package */static final String F_META_AREA = ".metadata"; //$NON-NLS-1$
	/* package */static final String F_PLUGIN_DATA = ".plugins"; //$NON-NLS-1$
	/* package */static final String F_LOG = ".log"; //$NON-NLS-1$
	/* package */static final String F_TRACE = "trace.log"; //$NON-NLS-1$

	/**
	 * Internal name of the preference storage file (value <code>"pref_store.ini"</code>) in this plug-in's (read-write) state area.
	 */
	/* package */static final String PREFERENCES_FILE_NAME = "pref_store.ini"; //$NON-NLS-1$

	private IPath location; //The location of the instance data
	private boolean initialized = false;

	protected void assertLocationInitialized() throws IllegalStateException {
		if (location != null && initialized)
			return;
		Activator activator = Activator.getDefault();
		if (activator == null)
			throw new IllegalStateException(CommonMessages.activator_not_available);
		Location service = activator.getInstanceLocation();
		if (service == null)
			throw new IllegalStateException(CommonMessages.meta_noDataModeSpecified);
		try {
			URL url = service.getURL();
			if (url == null)
				throw new IllegalStateException(CommonMessages.meta_instanceDataUnspecified);
			// TODO assume the URL is a file: 
			// Use the new File technique to ensure that the resultant string is 
			// in the right format (e.g., leading / removed from /c:/foo etc)
			location = new Path(new File(url.getFile()).toString());
			initializeLocation();
		} catch (CoreException e) {
			throw new IllegalStateException(e.getMessage());
		}
	}

	public IPath getMetadataLocation() throws IllegalStateException {
		assertLocationInitialized();
		return location.append(F_META_AREA);
	}

	public IPath getInstanceDataLocation() throws IllegalStateException {
		assertLocationInitialized();
		return location;
	}

	/**
	 * Returns the local file system path of the log file, or the default log location
	 * if the log is not in the local file system.
	 */
	public IPath getLogLocation() throws IllegalStateException {
		//make sure the log location is initialized if the instance location is known
		if (isInstanceLocationSet())
			assertLocationInitialized();
		FrameworkLog log = Activator.getDefault().getFrameworkLog();
		if (log != null) {
			java.io.File file = log.getFile();
			if (file != null)
				return new Path(file.getAbsolutePath());
		}
		if (location == null)
			throw new IllegalStateException(CommonMessages.meta_instanceDataUnspecified);
		return location.append(F_META_AREA).append(F_LOG);
	}

	public IPath getTraceLocation() throws IllegalStateException {

		DebugOptions debugOptions = Activator.getDefault().getDebugOptions();
		if (debugOptions == null) {
			return null;
		}
		return new Path(debugOptions.getFile().getAbsolutePath());
	}

	/**
	 * Returns true if the instance location has been set, and <code>false</code> otherwise.
	 */
	private boolean isInstanceLocationSet() {
		Activator activator = Activator.getDefault();
		if (activator == null)
			return false;
		Location service = activator.getInstanceLocation();
		if (service == null)
			return false;
		return service.isSet();
	}

	/**
	 * Returns the read/write location in which the given bundle can manage private state.
	 */
	public IPath getStateLocation(Bundle bundle) throws IllegalStateException {
		assertLocationInitialized();
		return getStateLocation(bundle.getSymbolicName());
	}

	public IPath getStateLocation(String bundleName) throws IllegalStateException {
		assertLocationInitialized();
		return getMetadataLocation().append(F_PLUGIN_DATA).append(bundleName);
	}

	public IPath getPreferenceLocation(String bundleName, boolean create) throws IllegalStateException {
		IPath result = getStateLocation(bundleName);
		if (create)
			result.toFile().mkdirs();
		return result.append(PREFERENCES_FILE_NAME);
	}

	private void initializeLocation() throws CoreException {
		// check if the location can be created
		if (location.toFile().exists()) {
			if (!location.toFile().isDirectory()) {
				String message = NLS.bind(CommonMessages.meta_notDir, location);
				throw new CoreException(new Status(IStatus.ERROR, IRuntimeConstants.PI_RUNTIME, IRuntimeConstants.FAILED_WRITE_METADATA, message, null));
			}
		}
		//try infer the device if there isn't one (windows)
		if (location.getDevice() == null)
			location = new Path(location.toFile().getAbsolutePath());
		createLocation();
		initialized = true;
	}

	private void createLocation() throws CoreException {
		// append on the metadata location so that the whole structure is created.  
		File file = location.append(F_META_AREA).toFile();
		try {
			file.mkdirs();
		} catch (Exception e) {
			String message = NLS.bind(CommonMessages.meta_couldNotCreate, file.getAbsolutePath());
			throw new CoreException(new Status(IStatus.ERROR, IRuntimeConstants.PI_RUNTIME, IRuntimeConstants.FAILED_WRITE_METADATA, message, e));
		}
		if (!file.canWrite()) {
			String message = NLS.bind(CommonMessages.meta_readonly, file.getAbsolutePath());
			throw new CoreException(new Status(IStatus.ERROR, IRuntimeConstants.PI_RUNTIME, IRuntimeConstants.FAILED_WRITE_METADATA, message, null));
		}
		// set the log file location now that we created the data area
		IPath logPath = location.append(F_META_AREA).append(F_LOG);
		try {
			Activator activator = Activator.getDefault();
			if (activator != null) {
				FrameworkLog log = activator.getFrameworkLog();
				if (log != null)
					log.setFile(logPath.toFile(), true);
				else if (debug())
					System.out.println("ERROR: Unable to acquire log service. Application will proceed, but logging will be disabled."); //$NON-NLS-1$
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// set the trace file location now that we created the data area
		IPath tracePath = location.append(F_META_AREA).append(F_TRACE);
		Activator activator = Activator.getDefault();
		if (activator != null) {
			DebugOptions debugOptions = activator.getDebugOptions();
			if (debugOptions != null) {
				debugOptions.setFile(tracePath.toFile());
			} else {
				System.out.println("ERROR: Unable to acquire debug service. Application will proceed, but debugging will be disabled."); //$NON-NLS-1$
			}
		}
	}

	private boolean debug() {
		Activator activator = Activator.getDefault();
		if (activator == null)
			return false;
		DebugOptions debugOptions = activator.getDebugOptions();
		if (debugOptions == null)
			return false;
		return debugOptions.getBooleanOption(OPTION_DEBUG, false);
	}
}
