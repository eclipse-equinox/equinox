/*******************************************************************************
 * Copyright (c) 2006, 2024 IBM Corporation and others.
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
 *     Rapicorp, Inc - Default the configuration to Application Support (bug 461725)
 *******************************************************************************/
package org.eclipse.equinox.launcher;

/**
 * <b>Note:</b> This class should not be referenced programmatically by
 * other Java code. This class exists only for the purpose of interacting with
 * a native launcher. To launch Eclipse programmatically, use
 * org.eclipse.core.runtime.adaptor.EclipseStarter. This class is not API.
 *
 */
class JNIBridge {
	private native void _set_exit_data(String sharedId, String data);

	private native void _set_launcher_info(String launcher, String name);

	private native void _update_splash();

	private native long _get_splash_handle();

	private native void _show_splash(String bitmap);

	private native void _takedown_splash();

	private native String _get_os_recommended_folder();

	private String library;
	private boolean libraryLoaded = false;

	/**
	 * @param library the given library
	 */
	public JNIBridge(String library) {
		this.library = library;
	}

	private void loadLibrary() {
		if (library != null) {
			try {
				Runtime.getRuntime().load(library);
			} catch (UnsatisfiedLinkError e) {
				//failed
			}
		}
		libraryLoaded = true;
	}

	public boolean setExitData(String sharedId, String data) {
		try {
			_set_exit_data(sharedId, data);
			return true;
		} catch (UnsatisfiedLinkError e) {
			if (!libraryLoaded) {
				loadLibrary();
				return setExitData(sharedId, data);
			}
			return false;
		}
	}

	public boolean setLauncherInfo(String launcher, String name) {
		try {
			_set_launcher_info(launcher, name);
			return true;
		} catch (UnsatisfiedLinkError e) {
			if (!libraryLoaded) {
				loadLibrary();
				return setLauncherInfo(launcher, name);
			}
			return false;
		}
	}

	public boolean showSplash(String bitmap) {
		try {
			_show_splash(bitmap);
			return true;
		} catch (UnsatisfiedLinkError e) {
			if (!libraryLoaded) {
				loadLibrary();
				return showSplash(bitmap);
			}
			return false;
		}
	}

	public boolean updateSplash() {
		try {
			_update_splash();
			return true;
		} catch (UnsatisfiedLinkError e) {
			if (!libraryLoaded) {
				loadLibrary();
				return updateSplash();
			}
			return false;
		}
	}

	public long getSplashHandle() {
		try {
			return _get_splash_handle();
		} catch (UnsatisfiedLinkError e) {
			if (!libraryLoaded) {
				loadLibrary();
				return getSplashHandle();
			}
			return -1;
		}
	}

	/**
	 * Whether or not we loaded the shared library here from java.
	 * False does not imply the library is not available, it could have
	 * been loaded natively by the executable.
	 *
	 * @return boolean
	 */
	boolean isLibraryLoadedByJava() {
		return libraryLoaded;
	}

	public boolean takeDownSplash() {
		try {
			_takedown_splash();
			return true;
		} catch (UnsatisfiedLinkError e) {
			if (!libraryLoaded) {
				loadLibrary();
				return takeDownSplash();
			}
			return false;
		}
	}

	public String getOSRecommendedFolder() {
		try {
			return _get_os_recommended_folder();
		} catch (UnsatisfiedLinkError e) {
			if (!libraryLoaded) {
				loadLibrary();
				return getOSRecommendedFolder();
			}
			return null;
		}
	}
}
