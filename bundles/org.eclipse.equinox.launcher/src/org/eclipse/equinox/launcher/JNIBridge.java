/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
 * @author aniefer
 *
 */
public class JNIBridge {
	private native void _set_exit_data(String data);
	private native void _update_splash();
	private native long  _get_splash_handle();
	private native void _show_splash(String bitmap);
	private native void _takedown_splash();
	
	private String library;
	private boolean libraryLoaded = false;
	public JNIBridge(String library) {
		this.library = library;
	}
	
	private void loadLibrary() {
		if(library != null) {
			try {
				System.load(library);
			} catch (UnsatisfiedLinkError e ) {
				//failed
			}
		}
		libraryLoaded = true;
	}

	public boolean setExitData(String data) {
		try {
			_set_exit_data(data);
			return true;
		} catch (UnsatisfiedLinkError e) {
			if(!libraryLoaded){
				loadLibrary();
				return setExitData(data);
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
}
