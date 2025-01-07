/*******************************************************************************
 * Copyright (c) 2009, 2021 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.osgi.framework.util;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import org.eclipse.osgi.internal.debug.Debug;

public class ObjectPool {
	private static String OPTION_DEBUG_OBJECTPOOL_ADDS = Debug.ECLIPSE_OSGI + "/debug/objectPool/adds"; //$NON-NLS-1$
	private static String OPTION_DEBUG_OBJECTPOOL_DUPS = Debug.ECLIPSE_OSGI + "/debug/objectPool/dups"; //$NON-NLS-1$
	// TODO need to set these
	private static final boolean DEBUG_OBJECTPOOL_ADDS = false;
	private static final boolean DEBUG_OBJECTPOOL_DUPS = false;
	private static Map<Object, WeakReference<Object>> objectCache = new WeakHashMap<>();

	@SuppressWarnings("unchecked")
	public static <T> T intern(T obj) {
		synchronized (objectCache) {
			WeakReference<Object> ref = objectCache.get(obj);
			if (ref != null) {
				Object refValue = ref.get();
				if (refValue != null) {
					obj = (T) refValue;
					if (DEBUG_OBJECTPOOL_DUPS)
						Debug.println("[ObjectPool] Found duplicate object: " + getObjectString(obj)); //$NON-NLS-1$
				}
			} else {
				objectCache.put(obj, new WeakReference<>(obj));
				if (DEBUG_OBJECTPOOL_ADDS)
					Debug.println("[ObjectPool] Added unique object to pool: " + getObjectString(obj) + " Pool size: " //$NON-NLS-1$ //$NON-NLS-2$
							+ objectCache.size());
			}
		}
		return obj;
	}

	private static String getObjectString(Object obj) {
		return "[(" + obj.getClass().getName() + ") " + obj.toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}
