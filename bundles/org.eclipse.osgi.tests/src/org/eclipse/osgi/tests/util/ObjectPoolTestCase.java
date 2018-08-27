/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.osgi.tests.util;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.osgi.framework.util.ObjectPool;
import org.osgi.framework.Version;

public class ObjectPoolTestCase extends CoreTest {
	public void testObjectPool01() {
		// Tests ObjectPool with strings only
		List objects = new ArrayList();
		int num = 2000;
		// new objects are added to the object pool; interning should add the object to the pool and return the same object
		for (int i = 0; i < num; i++) {
			String test1 = getName() + "_" + i; //$NON-NLS-1$
			String test2 = ObjectPool.intern(test1);
			assertTrue("Strings are not the same: " + test1, test1 == test2); //$NON-NLS-1$
			objects.add(test2);
		}
		doGC();
		// after doing a GC the interned objects should still be in the pool; interning a duplicate should return the objects that were added above
		for (int i = 0; i < num; i++) {
			String test1 = getName() + "_" + i; //$NON-NLS-1$
			String test2 = ObjectPool.intern(test1);
			assertFalse("Strings are the same: " + test1, test1 == test2); //$NON-NLS-1$
			assertTrue("Strings are not the same: " + test1, test2 == objects.get(i)); //$NON-NLS-1$
		}
		// clear the hard references to the interned objects
		objects.clear();
		doGC();
		// after doing a GC the interned objects should have been removed from the object pool
		for (int i = 0; i < num; i++) {
			String test1 = getName() + "_" + i; //$NON-NLS-1$
			String test2 = ObjectPool.intern(test1);
			assertTrue("Strings are not the same: " + test1, test1 == test2); //$NON-NLS-1$
			objects.add(test2);
		}
		// flush out the objects again.
		objects.clear();
		doGC();
	}

	public void testObjectPool02() {
		// Test both strings and versions
		List strings = new ArrayList();
		List versions = new ArrayList();
		int num = 2000;
		// new objects are added to the object pool; interning should add the object to the pool and return the same object
		for (int i = 0; i < num; i++) {
			String testString1 = getName() + "_" + i; //$NON-NLS-1$
			String testString2 = ObjectPool.intern(testString1);
			assertTrue("Strings are not the same: " + testString1, testString1 == testString2); //$NON-NLS-1$
			strings.add(testString2);
			Version testVersion1 = new Version(i, i, i, getName() + "_" + i); //$NON-NLS-1$
			Version testVersion2 = ObjectPool.intern(testVersion1);
			assertTrue("Versions are not the same: " + testVersion1, testVersion1 == testVersion2); //$NON-NLS-1$
			versions.add(testVersion2);
		}
		doGC();
		// after doing a GC the interned objects should still be in the pool; interning a duplicate should return the objects that were added above
		for (int i = 0; i < num; i++) {
			String testString1 = getName() + "_" + i; //$NON-NLS-1$
			String testString2 = ObjectPool.intern(testString1);
			assertFalse("Strings are the same: " + testString1, testString1 == testString2); //$NON-NLS-1$
			assertTrue("Strings are not the same: " + testString1, testString2 == strings.get(i)); //$NON-NLS-1$
			Version testVersion1 = new Version(i, i, i, getName() + "_" + i); //$NON-NLS-1$
			Version testVersion2 = ObjectPool.intern(testVersion1);
			assertFalse("Versions are the same: " + testVersion1, testVersion1 == testVersion2); //$NON-NLS-1$
			assertTrue("Versions are not the same: " + testVersion1, testVersion2 == versions.get(i)); //$NON-NLS-1$
		}
		// clear the hard references to the interned objects
		strings.clear();
		versions.clear();
		// after doing a GC the interned objects should have been removed from the object pool
		doGC();
		for (int i = 0; i < num; i++) {
			String testString1 = getName() + "_" + i; //$NON-NLS-1$
			String testString2 = ObjectPool.intern(testString1);
			assertTrue("Strings are not the same: " + testString1, testString1 == testString2); //$NON-NLS-1$
			strings.add(testString2);
			Version testVersion1 = new Version(i, i, i, getName() + "_" + i); //$NON-NLS-1$
			Version testVersion2 = ObjectPool.intern(testVersion1);
			assertTrue("Versions are not the same: " + testVersion1, testVersion1 == testVersion2); //$NON-NLS-1$
			versions.add(testVersion2);
		}
		// flush out the objects again.
		strings.clear();
		versions.clear();
		doGC();
	}

	private static void doGC() {
		// We go through great effort to force the VM to throw our weakly referenced objects away.
		System.gc();
		System.runFinalization();
		System.gc();
		System.runFinalization();
	}
}
