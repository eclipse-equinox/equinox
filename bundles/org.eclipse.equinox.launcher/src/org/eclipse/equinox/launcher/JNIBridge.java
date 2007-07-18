/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.launcher;


/**
 * <b>Note:</b> This class should not be referenced programmatically by
 * other Java code. This class exists only for the purpose of interacting with
 * a native launcher. To launch Eclipse programmatically, use 
 * org.eclipse.core.runtime.adaptor.EclipseStarter. This class is not API.
 */
public class JNIBridge {
	//TODO: This class should not be public
	private native void _set_exit_data(String sharedId, String data);
	private native void _update_splash();
	private native long  _get_splash_handle();
	private native void _show_splash(String bitmap);
	private native void _takedown_splash();
	
	private native int OleInitialize(int reserved);
	private native void OleUninitialize();
	
	private String library;
	private boolean libraryLoaded = false;
	public JNIBridge(String library) {
		this.library = library;
	}
	
	private void loadLibrary() {
		if (library != null) {
			try {
				if (library.indexOf("wpf") != -1) { //$NON-NLS-1$
					int idx = library.indexOf("eclipse_"); //$NON-NLS-1$
					if (idx != -1) {
						String comLibrary = library.substring(0, idx) + "com_"; //$NON-NLS-1$
						comLibrary += library.substring(idx + 8, library.length());
						Runtime.getRuntime().load(comLibrary);
						OleInitialize(0);
					}
				}
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
			if(!libraryLoaded){
				loadLibrary();
				return setExitData(sharedId, data);
			}
			return false;
		}
	}

	public boolean showSplash(String bitmap) {
		try {
			_show_splash(bitmap);
			return true;
		} catch (UnsatisfiedLinkError e) {
			if(!libraryLoaded){
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
			if(!libraryLoaded){
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
			if(!libraryLoaded){
				loadLibrary();
				return getSplashHandle();
			}
			return -1;
		}
	}
	
	public boolean takeDownSplash() {
		try {
			_takedown_splash();
			return true;
		} catch (UnsatisfiedLinkError e) {
			if(!libraryLoaded){
				loadLibrary();
				return takeDownSplash();
			}
			return false;
		}
	}
	
	public boolean uninitialize() {
		if (libraryLoaded && library != null) {
			if (library.indexOf("wpf") != -1)  { //$NON-NLS-1$
				try {
					OleUninitialize();
				} catch (UnsatisfiedLinkError e) {
					// library not loaded
					return false;
				}
			}
		}
		return true;
	}
}
