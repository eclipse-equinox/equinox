/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.wireadmin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.*;
import java.util.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.service.prefs.*;
import org.osgi.service.wireadmin.WireConstants;

/*
 *  WireAdminStore is responsible for managing the persistence data of the wireadmin
 *  service.  It uses the PersistenceNode service as its underlying storage.
 */

public class WireAdminStore {

	static protected final String persistenceUserName = "WireAdmin"; //$NON-NLS-1$
	static protected final String lastPidString = "lastPid"; //$NON-NLS-1$
	static protected final String defaultLastPid = "1"; //$NON-NLS-1$

	protected BundleContext context;
	protected WireAdmin wireadmin;
	protected LogService log;
	protected Preferences rootNode;
	protected PreferencesService preferencesService;

	protected WireAdminStore(BundleContext context, WireAdmin wireadmin, LogService log, PreferencesService preferencesService) {
		this.context = context;
		this.wireadmin = wireadmin;
		this.log = log;
	}

	protected void init(PreferencesService preferencesService_) throws BackingStoreException {
		this.preferencesService = preferencesService_;
	}

	protected void addWire(final Wire wire, final Dictionary properties) throws BackingStoreException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws Exception {
					String pid = wire.getPid();
					rootNode.put(lastPidString, pid);
					Preferences node = rootNode.node(pid);
					storeProperties(properties, node);

					/* Enumeration enum = properties.keys();
					 
					 while(enum.hasMoreElements())
					 {
					 String key = (String)enum.nextElement();
					 String value = (String)properties.get(key);
					 node.put(key,value);
					 }
					 */
					rootNode.flush();
					return (null);
				}
			});
		} catch (PrivilegedActionException ex) {
			//log.log(log.LOG_ERROR,Text.BACKING_STORE_WRITE_EXCEPTION,ex);	
			throw ((BackingStoreException) ex.getException());
		}
	}

	protected void removeWire(final Wire wire) throws BackingStoreException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws Exception {
					Preferences node = rootNode.node(wire.getPid());
					node.removeNode();
					rootNode.node("").flush(); //$NON-NLS-1$
					return (null);
				}
			});
		} catch (PrivilegedActionException ex) {
			//log.log(log.LOG_ERROR,Text.BACKING_STORE_WRITE_EXCEPTION,ex);		
			throw ((BackingStoreException) ex.getException());
		}
	}

	protected void loadWires() throws BackingStoreException {
		synchronized (this) {
			wireadmin.lastPid = Integer.parseInt(rootNode.get(lastPidString, defaultLastPid));
			String[] children = rootNode.node("").childrenNames(); //$NON-NLS-1$
			for (int i = 0; i < children.length; i++) {
				loadWire(rootNode.node(children[i]));
			}
		}
	}

	protected void loadWire(Preferences node) throws BackingStoreException {
		String pid = node.name();
		Hashtable properties = loadProperties(node);
		/*  String[] keys = node.keys();
		 for(int i=0;i<keys.length;i++)
		 {
		 String value = (String)node.get(keys[i],null);
		 properties.put(keys[i],value);
		 }  */
		String consumerPid = (String) properties.get(WireConstants.WIREADMIN_CONSUMER_PID);
		String producerPid = (String) properties.get(WireConstants.WIREADMIN_PRODUCER_PID);
		wireadmin.createWire(pid, producerPid, consumerPid, properties);
	}

	protected void updateWire(final Wire wire, final Dictionary properties) throws BackingStoreException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws Exception {
					String pid = wire.getPid();
					Preferences node = rootNode.node(pid);
					node.clear();
					storeProperties(properties, node);

					/*Enumeration enum = properties.keys();
					 while(enum.hasMoreElements())
					 {
					 String key = (String)enum.nextElement();
					 String value = (String)properties.get(key);
					 node.put(key,value);
					 }
					 */
					node.flush();
					return (null);
				}
			});
		} catch (PrivilegedActionException ex) {
			//log.log(log.LOG_ERROR,Text.BACKING_STORE_WRITE_EXCEPTION,ex);	
			throw ((BackingStoreException) ex.getException());
		}
	}

	protected void destroy() {
		try {
			rootNode.flush();
		} catch (BackingStoreException ex) {
			//log.log(log.LOG_ERROR,Text.BACKING_STORE_WRITE_EXCEPTION,ex);	
		}
	}

	protected void storeProperties(Dictionary properties, Preferences node) throws BackingStoreException {
		Enumeration e = properties.keys();
		while (e.hasMoreElements()) {
			String key = (String) e.nextElement();
			Object value = properties.get(key);
			if (value instanceof Vector) {
				storeVector(node.node("Vector/" + key), (Vector) value); //$NON-NLS-1$
			} else {
				Preferences childNode = node.node("basetype/" + value.getClass().getName()); //$NON-NLS-1$
				childNode.put(key, String.valueOf(value));
			}

			//node.put(key,value);
		}
		node.flush();
	}

	private void storeVector(Preferences node, Vector vector) {
		Enumeration e = vector.elements();
		while (e.hasMoreElements()) {
			Object value = e.nextElement();
			node.put(value.getClass().getName(), String.valueOf(value));
		}
	}

	private Hashtable loadProperties(Preferences node) throws BackingStoreException {
		Hashtable props = new Hashtable(15);
		if (node.nodeExists("Vector")) { //$NON-NLS-1$
			loadVectors(node.node("Vector"), props); //$NON-NLS-1$
		}
		if (node.nodeExists("basetype")) { //$NON-NLS-1$
			loadBaseTypes(node.node("basetype"), props); //$NON-NLS-1$
		}
		return props;
	}

	private void loadVectors(Preferences node, Hashtable props) throws BackingStoreException {
		String[] children = node.childrenNames();
		for (int j = 0; j < children.length; j++) {
			Preferences vectorNode = node.node(children[j]);
			String keys[] = vectorNode.keys();
			Vector vector = new Vector(keys.length);
			for (int i = 0; i < keys.length; i++) {
				String value = node.get(keys[i], null);
				vector.add(getValue(keys[i], value));

			}
			props.put(children[j], vector);
		}
	}

	private void loadBaseTypes(Preferences node, Hashtable props) throws BackingStoreException {
		String[] children = node.childrenNames();
		for (int j = 0; j < children.length; j++) {
			Preferences childNode = node.node(children[j]);
			String keys[] = childNode.keys();
			for (int i = 0; i < keys.length; i++) {
				props.put(keys[i], getValue(children[j], childNode.get(keys[i], null)));
			}
		}
	}

	private Object getValue(String className, String value) {
		//Float does not have a zero-arguement constructor
		if (className.equals(Float.class.getName())) {
			return Float.valueOf(value);
		} else if (className.equals(Boolean.class.getName())) {
			return Boolean.valueOf(value);
		}
		try {
			Class clazz = Class.forName(className);
			Object object = clazz.newInstance();
			Method method = clazz.getDeclaredMethod("valueOf", new Class[] {Object.class}); //$NON-NLS-1$
			return (method.invoke(object, new Object[] {value}));

		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		} catch (IllegalAccessException ex1) {
			ex1.printStackTrace();
		} catch (InstantiationException ex2) {

			ex2.printStackTrace();
		} catch (NoSuchMethodException ex3) {
			ex3.printStackTrace();
		} catch (InvocationTargetException ex4) {
			ex4.printStackTrace();
		}
		return null;
	}

}
