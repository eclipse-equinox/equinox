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

package org.eclipse.osgi.framework.internal.defaultadaptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * A Default implementation of the MetaData interface.  This class uses a 
 * Properties object to store and get MetaData information.  All data
 * converted into String data before saving.
 */
public class MetaData {

	/**
	 * The Properties file to store the data.
	 */
	Properties properties;

	/**
	 * The File object to store and load the Properties object.
	 */
	File datafile;

	/**
	 * The header string to use when storing data to the datafile.
	 */
	String header;

	/**
	 * Constructs a MetaData object that uses the datafile to persistently
	 * store data.
	 * @param datafile The File object used to persistently load and store data.
	 * @param header The header to use when storing data persistently.
	 */
	public MetaData(File datafile, String header) {
		this.datafile = datafile;
		this.header = header;
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.MetaData#get(java.lang.String, java.lang.String)
	 */
	public String get(String key, String def) {
		return properties.getProperty(key,def);
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.MetaData#getInt(String, int)
	 */
	public int getInt(String key, int def) {
		String result = get(key,null);
		if (result == null) {
			return def;
		}
		try{
			return Integer.parseInt(result);
		}
		catch (NumberFormatException nfe) {
			return def;
		}
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.MetaData#getLong(String, long)
	 */
	public long getLong(String key, long def) {
		String result = get(key,null);
		if (result == null) {
			return def;
		}
		try{
			return Long.parseLong(result);
		}
		catch (NumberFormatException nfe) {
			return def;
		}
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.MetaData#getBoolean(String, boolean)
	 */
	public boolean getBoolean(String key, boolean def) {
		String result = get(key,null);
		if (result == null) {
			return def;
		}
		return Boolean.valueOf(result).booleanValue();
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.MetaData#set(String, String)
	 */
	public void set(String key, String val) {
		properties.put(key,val);
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.MetaData#setInt(String, int)
	 */
	public void setInt(String key, int val) {
		properties.put(key,Integer.toString(val));
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.MetaData#setLong(String, long)
	 */
	public void setLong(String key, long val) {
		properties.put(key,Long.toString(val));
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.MetaData#setBoolean(String, boolean)
	 */
	public void setBoolean(String key, boolean val) {
		properties.put(key,new Boolean(val).toString());
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.MetaData#remove(String)
	 */
	public void remove(String key){
		properties.remove(key);
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.MetaData#save()
	 */
	public void save() throws IOException {
		FileOutputStream fos = new FileOutputStream(datafile);
		properties.store(fos,header);
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.MetaData#load()
	 */
	public void load() throws IOException {
		properties = new Properties();
		if (datafile.exists()) {
			FileInputStream fis = new FileInputStream(datafile);
			properties.load(fis);
			fis.close();
		}
	}

	/**
	 * Returns the result of toString on the Properties object.
	 */
	public String toString(){
		return properties.toString();
	}


}
