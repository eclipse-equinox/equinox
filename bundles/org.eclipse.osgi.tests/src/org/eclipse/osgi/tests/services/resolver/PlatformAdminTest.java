/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.services.resolver;

import java.io.*;
import java.util.Hashtable;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.internal.baseadaptor.StateManager;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class PlatformAdminTest extends AbstractStateTest {
	public static Test suite() {
		return new TestSuite(PlatformAdminTest.class);
	}

	public PlatformAdminTest(String name) {
		super(name);
	}

	private State storeAndRetrieve(State toStore) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		toStore.getFactory().writeState(toStore, baos);
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		return toStore.getFactory().readState(bais);
	}

	public void testCache() throws IOException, BundleException {
		State originalState = buildSimpleState();
		State retrievedState = storeAndRetrieve(originalState);
		assertEquals("0.9", 0, retrievedState.getChanges().getChanges().length);
		assertIdentical("1.0", originalState, retrievedState);
		originalState.resolve();
		retrievedState = storeAndRetrieve(originalState);
		assertIdentical("2.0", originalState, retrievedState);
	}

	public void testClone() throws BundleException {
		State original = buildSimpleState();
		State newState = original.getFactory().createState(original);
		assertEquals("1", original, newState);
		original = buildComplexState();
		newState = original.getFactory().createState(original);
		assertEquals("2", original, newState);
	}

	public void testBug205270() throws BundleException {
		State state = buildSimpleState();
		Hashtable manifest = new Hashtable();
		int id = 0;
		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.BUNDLE_NATIVECODE, "libwrapper-linux-x86-32.so; wrapper-linux-x86-32; osname=linux; processor=x86");
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++);
		try {
			BundleDescription aPrime = state.getFactory().createBundleDescription(a);
			assertEquals("Copy is not equal", a, aPrime);
		} catch (Throwable t) {
			fail("Unexpected error while cloning a BundleDescription", t);
		}
	}

	public void testBug184127() throws BundleException {
		File resolverData = getContext().getDataFile("resolverData");
		resolverData.mkdirs();

		File stateFile = new File(resolverData, ".state");
		File lazyFile = new File(resolverData, ".lazy");
		stateFile.delete();
		lazyFile.delete();
		StateManager sm = new StateManager(stateFile, lazyFile, getContext());
		State systemState = sm.readSystemState();
		assertNull("SystemState is not null", systemState);
		systemState = sm.createSystemState();

		Hashtable manifest = new Hashtable();
		int id = 0;
		manifest = new Hashtable();

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		BundleDescription a = systemState.getFactory().createBundleDescription(systemState, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "b, c");
		manifest.put(Constants.REQUIRE_BUNDLE, "a");
		manifest.put(Constants.IMPORT_PACKAGE, "afrag2");
		BundleDescription b = systemState.getFactory().createBundleDescription(systemState, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "afrag2");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "afrag2");
		manifest.put(Constants.FRAGMENT_HOST, "a");
		manifest.put(Constants.IMPORT_PACKAGE, "c");
		BundleDescription afrag2 = systemState.getFactory().createBundleDescription(systemState, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "afrag1");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.FRAGMENT_HOST, "a");
		manifest.put(Constants.IMPORT_PACKAGE, "b");
		BundleDescription afrag1 = systemState.getFactory().createBundleDescription(systemState, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++);

		systemState.addBundle(afrag1);
		systemState.addBundle(afrag2);
		systemState.addBundle(a);
		systemState.addBundle(b);

		systemState.resolve();

		assertTrue("aFrag1 is not resolved", afrag1.isResolved());
		assertTrue("aFrag2 is not resolved", afrag2.isResolved());
		assertTrue("a is not resolved", a.isResolved());
		assertTrue("b is not resolved", b.isResolved());

		try {
			sm.shutdown(stateFile, lazyFile);
		} catch (IOException e) {
			fail("failed to shudown StateManager", e);
		}

		sm = new StateManager(stateFile, lazyFile, getContext());
		systemState = sm.readSystemState();
		assertNotNull("SystemState is null", systemState);
		b = systemState.getBundle("b", null);
		ExportPackageDescription[] exports = null;
		try {
			exports = b.getExportPackages();
		} catch (Throwable e) {
			fail("Unexpected exception getting exports", e);
		}
		assertNotNull("exports is null", exports);
		assertEquals("Wrong number of exports", 2, exports.length);
	}
}
//TODO tests to enable
//testFragmentUpdateNoVersionChanged()
//testFragmentUpdateVersionChanged()
//testHostUpdateNoVersionChanged()
//testHostUpdateVersionChanged()
