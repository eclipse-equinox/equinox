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

package org.eclipse.osgi.framework.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;

import java.security.PrivilegedExceptionAction;
import java.util.Properties;

import org.eclipse.osgi.framework.security.action.GetProperty;

/**
 * Utility class to execute common privileged code.
 */
public class SecureAction {
	/**
	 * Returns a system property.  Same as calling
	 * System.getProperty(String).
	 * @param property the property key.
	 * @return the value of the property or null if it does not exist.
	 */
	public static String getProperty(String property){
		if (System.getSecurityManager() == null)
			return System.getProperty(property);
		else
			return (String) AccessController.doPrivileged(new GetProperty(property,null));
	}

	/**
	 * Returns a system property.  Same as calling
	 * System.getProperty(String,String).
	 * @param property the property key.
	 * @param def the default value if the property key does not exist.
	 * @return the value of the property or the def value if the property
	 * does not exist.
	 */
	public static String getProperty(String property, String def){
		if (System.getSecurityManager() == null)
			return System.getProperty(property,def);
		else
			return (String) AccessController.doPrivileged(new GetProperty(property,def));
	}

	/**
	 * Returns the system properties.  Same as calling
	 * System.getProperties().
	 * @return the system properties.
	 */
	public static Properties getProperties() {
		if (System.getSecurityManager() == null)
			return System.getProperties();
		else
			return (Properties) AccessController.doPrivileged(new GetProperty(null));
	}

	/**
	 * Creates a FileInputStream from a File.  Same as calling
	 * new FileInputStream(File).
	 * @param file the File to craete a FileInputStream from.
	 * @return The FileInputStream.
	 * @throws FileNotFoundException if the File does not exist.
	 */
	public static FileInputStream getFileInputStream(final File file) throws FileNotFoundException {
		if (System.getSecurityManager() == null)
			return new FileInputStream(file);
		else
			try {
				return (FileInputStream) AccessController.doPrivileged( new PrivilegedExceptionAction(){
					public Object run() throws FileNotFoundException{
						return new FileInputStream(file);
					}
				});
			} catch (PrivilegedActionException e) {
				throw (FileNotFoundException) e.getCause();
			}
	}

	/**
	 * Creates a FileInputStream from a File.  Same as calling
	 * new FileOutputStream(File,boolean).
	 * @param file the File to craete a FileOutputStream from.
	 * @param append indicates if the OutputStream should append content.
	 * @return The FileOutputStream.
	 * @throws FileNotFoundException if the File does not exist.
	 */
	public static FileOutputStream getFileOutputStream(final File file, final boolean append) throws FileNotFoundException {
		if (System.getSecurityManager() == null)
			return new FileOutputStream(file,append);
		else
			try {
				return (FileOutputStream) AccessController.doPrivileged( new PrivilegedExceptionAction(){
					public Object run() throws FileNotFoundException{
						return new FileOutputStream(file,append);
					}
				});
			} catch (PrivilegedActionException e) {
				throw (FileNotFoundException) e.getCause();
			}
	}
}
