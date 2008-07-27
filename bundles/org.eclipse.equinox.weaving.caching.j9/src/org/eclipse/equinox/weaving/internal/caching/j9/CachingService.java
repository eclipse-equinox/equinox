/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Matthew Webster           Eclipse 3.2 changes     
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching.j9;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.equinox.service.weaving.CacheEntry;
import org.eclipse.equinox.service.weaving.ICachingService;
import org.osgi.framework.Bundle;

import com.ibm.oti.shared.HelperAlreadyDefinedException;
import com.ibm.oti.shared.Shared;
import com.ibm.oti.shared.SharedClassURLHelper;

public class CachingService implements ICachingService {
	
	private Bundle bundle;
	private ClassLoader classLoader;
	private String partition;
	SharedClassURLHelper urlhelper;
	
	public CachingService () {
		if (CachingServicePlugin.DEBUG) System.out.println("- CachingService.<init>()");
	}

	public CachingService (ClassLoader loader, Bundle bundle, String key) {
		if (CachingServicePlugin.DEBUG) System.out.println("> CachingService.<init>() bundle=" + bundle.getSymbolicName() + ", loader=" + loader + ", key='" + key + "'");
		this.bundle = bundle;
		this.classLoader = loader;
		this.partition = hashNamespace(key);
		try{
			urlhelper = Shared.getSharedClassHelperFactory().getURLHelper(classLoader);
		} catch (HelperAlreadyDefinedException e) {
			e.printStackTrace();
		}
		if (CachingServicePlugin.DEBUG) System.out.println("< CachingService.<init>() partition='" + partition + "', urlhelper=" + urlhelper);
	}
	
	public ICachingService getInstance(ClassLoader classLoader, Bundle bundle, String key) {
		return new CachingService(classLoader,bundle, key);
	}

	public CacheEntry findStoredClass(String namespace, URL sourceFileURL, String name) {
		byte[] bytes = urlhelper.findSharedClass(partition, sourceFileURL, name);
		if (CachingServicePlugin.DEBUG && bytes != null) System.out.println("- CachingService.findStoredClass() bundle=" + bundle.getSymbolicName() + ", name=" + name + ", url=" + sourceFileURL + ", bytes=" + bytes);
		
		if (bytes != null) {
		    return new CacheEntry(true, bytes);
		}
		else {
		    return new CacheEntry(false, null);
		}
	}

	public boolean storeClass(String namespace, URL sourceFileURL, Class clazz, byte[] classbytes) {
		boolean success = urlhelper.storeSharedClass(partition, sourceFileURL, clazz);
		if (CachingServicePlugin.DEBUG && success) System.out.println("- CachingService.storeClass() bundle=" + bundle.getSymbolicName() + ", clazz=" + clazz + ", url=" + sourceFileURL);
		return success;
	}
	
	/**
	 * Hash the shared class namespace using MD5
	 * @param keyToHash
	 * @return the MD5 version of the input string
	 */
	public String hashNamespace(String namespace){
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		byte[] bytes = md.digest(namespace.getBytes());
		StringBuffer result = new StringBuffer();
		for(int i=0; i<bytes.length; i++){
			byte b = bytes[i];
			int num;
			if(b<0) {
				num = b+256;
			}else{
				num=b;
			}
			String s = Integer.toHexString(num);
			while (s.length()<2){
				s = "0"+s;
			}
			result.append(s);
		}
		return new String(result);
	}

}
