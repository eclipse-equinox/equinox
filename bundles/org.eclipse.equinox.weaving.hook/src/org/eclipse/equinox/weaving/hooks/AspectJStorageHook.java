/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Matthew Webster           initial implementation
 *   Martin Lippert            supplementing mechanism reworked     
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.internal.adaptor.CachedManifest;
import org.eclipse.equinox.service.weaving.SupplementerRegistry;
import org.eclipse.equinox.weaving.adaptors.Debug;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.hooks.StorageHook;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.framework.util.KeyedElement;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class AspectJStorageHook implements StorageHook {
	
	public static final String KEY = AspectJStorageHook.class.getName();
	public static final int HASHCODE = KEY.hashCode();
	
	private BaseData bundleData;
	private final SupplementerRegistry supplementerRegistry;

	
	public AspectJStorageHook(SupplementerRegistry supplementerRegistry) {
		if (Debug.DEBUG_SUPPLEMENTS) Debug.println("- AspectJStorageHook.AspectJStorageHook()");
		this.supplementerRegistry = supplementerRegistry;
	}
	
	public AspectJStorageHook(BaseData bd, SupplementerRegistry supplementerRegistry) {
		if (Debug.DEBUG_SUPPLEMENTS) Debug.println("- AspectJStorageHook.AspectJStorageHook() baseDate=" + bd);
		this.bundleData = bd;
		this.supplementerRegistry = supplementerRegistry;
	}
	
	private String getSymbolicName () {
		return (bundleData == null)? "root" : bundleData.getSymbolicName();
	}
	
	public void copy(StorageHook storageHook) {
		// TODO Auto-generated method stub
	}

	public StorageHook create(BaseData bundledata) throws BundleException {
//		System.err.println("? AbstractAspectJHook.create()");
		// TODO Auto-generated method stub
//		StorageHook result;
		final StorageHook hook = new AspectJStorageHook(bundledata, supplementerRegistry);
////		if (bundledata.getSymbolicName().equals("demo.hello")) {
//			InvocationHandler ih = new InvocationHandler() {
//				public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
//					System.err.println("? " + method.getName());
//					return method.invoke(hook,args);
//				}
//			};
//			result = (StorageHook)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[] { StorageHook.class }, ih);
////		}
////		else {
////			result = hook;
////		}
//		return result;
		return hook;
	}

	public boolean forgetStartLevelChange(int startlevel) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean forgetStatusChange(int status) {
		// TODO Auto-generated method stub
		return false;
	}

	public Dictionary getManifest(boolean firstLoad) throws BundleException {
//		System.err.println("? AspectJStorageHook.getManifest() " + this + " firstLoad=" + firstLoad);
		// TODO Auto-generated method stub
		return null;
	}

	public int getStorageVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void initialize(Dictionary manifest) throws BundleException {
//		System.err.println("? AspectJStorageHook.initialize() " + this + " manifest=" + manifest);
		if (Debug.DEBUG_SUPPLEMENTS) Debug.println("> AspectJStorageHook.initialize() " + getSymbolicName());
		try {
			ManifestElement[] imports = ManifestElement.parseHeader(Constants.IMPORT_PACKAGE, (String) manifest.get(Constants.IMPORT_PACKAGE));
			ManifestElement[] exports = ManifestElement.parseHeader(Constants.EXPORT_PACKAGE, (String) manifest.get(Constants.EXPORT_PACKAGE));
			List supplementers = supplementerRegistry.getSupplementers(bundleData.getSymbolicName(), imports, exports);
			if (!supplementers.isEmpty()) {
				if (Debug.DEBUG_SUPPLEMENTS) Debug.println("- AspectJStorageHook.initialize() " + getSymbolicName() + " supplementers=" + supplementers);
				if (!getSymbolicName().equals("org.eclipse.osgi") 
//						&& !getSymbolicName().startsWith("org.eclipse.core")
//						&& !getSymbolicName().startsWith("org.eclipse.equinox")
						&& !getSymbolicName().startsWith("org.eclipse.team.ui")
						&& !getSymbolicName().startsWith("org.eclipse.update")
					) {
					if (addRequiredBundles(supplementers)) {
						if (AbstractAspectJHook.verbose) System.err.println("[org.aspectj.osgi] info supplementing " + getSymbolicName() + " with " + supplementers);
					}
					else {
						if (AbstractAspectJHook.verbose) System.err.println("[org.aspectj.osgi] info not supplementing " + getSymbolicName());
					}
				}
				else {
					if (AbstractAspectJHook.verbose) System.err.println("[org.aspectj.osgi] info cannot supplement " + getSymbolicName());
				}
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		if (Debug.DEBUG_SUPPLEMENTS) Debug.println("< AspectJStorageHook.initialize() ");
	}

	private boolean addRequiredBundles (List bundles) throws BundleException {
		Dictionary manifest = bundleData.getManifest();
		manifest = ((CachedManifest)manifest).getManifest();
		
		if (manifest != null) {
			String value = (String)manifest.get(Constants.REQUIRE_BUNDLE);
			for (Iterator i = bundles.iterator(); i.hasNext();) {
				String name = (String)i.next();
				if (value == null) value = name;
				else value += "," + name;
			}
			
			if (Debug.DEBUG_SUPPLEMENTS) Debug.println("- AspectJStorageHook.addRequiredBundles() " + bundleData.getSymbolicName() + " ,manifest=" + manifest.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(manifest)) + ", value=" + value);
			setHeader((Headers)manifest,Constants.REQUIRE_BUNDLE,value,true);
		}
		
		return true;
	}
	
	private Object setHeader(Headers manifest, Object key, Object value, boolean replace) throws BundleException {
		try {
			/* In Eclipse 3.3 we must use reflection to allow the manifest to be modified */
			if (readOnly != null) {
				readOnly.set(manifest,new Boolean(false));
			}
			
			return manifest.set(Constants.REQUIRE_BUNDLE,value,true);
		}
		catch (IllegalAccessException ex) {
			throw new BundleException(key + "=" + value,ex);
		}
	}
	
	private static Field readOnly; 
	
	static {
		try {
			readOnly = Headers.class.getDeclaredField("readOnly");
			readOnly.setAccessible(true);
		}
		catch (Exception ex) {
			if (Debug.DEBUG_SUPPLEMENTS) ex.printStackTrace();
		}
	}

	public StorageHook load(BaseData bundledata, DataInputStream is) throws IOException {
		if (Debug.DEBUG_SUPPLEMENTS) Debug.println("- AspectJStorageHook.load() " + getSymbolicName() + " bundleData=" + bundledata);
		return new AspectJStorageHook(bundledata, supplementerRegistry);
	}

	public boolean matchDNChain(String pattern) {
		// TODO Auto-generated method stub
//		System.err.println("? AspectJStorageHook.matchDNChain() " + getSymbolicName() + " pattern=" + pattern);
		return false;
	}

	public void save(DataOutputStream os) throws IOException {
		// TODO Auto-generated method stub
		if (Debug.DEBUG_SUPPLEMENTS) Debug.println("- AspectJStorageHook.save() " + getSymbolicName());
	}

	public void validate() throws IllegalArgumentException {
		// TODO Auto-generated method stub
//		System.err.println("? AspectJStorageHook.validate()");
	}

	public boolean compare(KeyedElement other) {
		// TODO Auto-generated method stub
		return other.getKey() == KEY;
	}

	public Object getKey() {
		return KEY;
	}

	public int getKeyHashCode() {
		return HASHCODE;
	}
	
	public String toString () {
		return "AspectJStorageHook[" + getSymbolicName() + "]"; 
	}

}
