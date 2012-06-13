/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.util;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.debug.FrameworkDebugOptions;

public class ObjectPool {
	private static String OPTION_DEBUG_OBJECTPOOL_ADDS = Debug.ECLIPSE_OSGI + "/debug/objectPool/adds"; //$NON-NLS-1$
	private static String OPTION_DEBUG_OBJECTPOOL_DUPS = Debug.ECLIPSE_OSGI + "/debug/objectPool/dups"; //$NON-NLS-1$
	private static final boolean DEBUG_OBJECTPOOL_ADDS;
	private static final boolean DEBUG_OBJECTPOOL_DUPS;
	private static Map<Object, WeakReference<Object>> objectCache = new WeakHashMap<Object, WeakReference<Object>>();
	static {
		FrameworkDebugOptions dbgOptions = FrameworkDebugOptions.getDefault();
		if (dbgOptions != null) {
			DEBUG_OBJECTPOOL_ADDS = dbgOptions.getBooleanOption(OPTION_DEBUG_OBJECTPOOL_ADDS, false);
			DEBUG_OBJECTPOOL_DUPS = dbgOptions.getBooleanOption(OPTION_DEBUG_OBJECTPOOL_DUPS, false);
		} else {
			DEBUG_OBJECTPOOL_ADDS = false;
			DEBUG_OBJECTPOOL_DUPS = false;
		}
	}

	public static Object intern(Object obj) {
		synchronized (objectCache) {
			WeakReference<Object> ref = objectCache.get(obj);
			if (ref != null) {
				Object refValue = ref.get();
				if (refValue != null) {
					obj = refValue;
					if (DEBUG_OBJECTPOOL_DUPS)
						Debug.println("[ObjectPool] Found duplicate object: " + getObjectString(obj)); //$NON-NLS-1$
				}
			} else {
				objectCache.put(obj, new WeakReference<Object>(obj));
				if (DEBUG_OBJECTPOOL_ADDS)
					Debug.println("[ObjectPool] Added unique object to pool: " + getObjectString(obj) + " Pool size: " + objectCache.size()); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return obj;
	}

	private static String getObjectString(Object obj) {
		return "[(" + obj.getClass().getName() + ") " + obj.toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
