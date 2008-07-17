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
import java.util.Dictionary;
import java.util.Hashtable;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.internal.baseadaptor.StateManager;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class PlatformAdminTest extends AbstractStateTest {
	private static final String GENERIC_REQUIRE = "Eclipse-GenericRequire"; //$NON-NLS-1$
	private static final String GENERIC_CAPABILITY = "Eclipse-GenericCapability"; //$NON-NLS-1$

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

	public void testBug241128_01() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		int id = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		BundleDescription a1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "a; bundle-version=\"[1.0.0, 2.0.0)\"");
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++);

		state.addBundle(a1);
		state.addBundle(b);
		state.resolve(true);

		assertTrue("Bundle a1 is not resolved", a1.isResolved());
		assertTrue("Bundle b is not resolved", b.isResolved());

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a");
		manifest.put(Constants.BUNDLE_VERSION, "2.0.0");
		BundleDescription a2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), a1.getBundleId());
		state.updateBundle(a2);

		state.resolve(true);
		assertTrue("Bundle a2 is not resolved", a2.isResolved());
		assertFalse("Bundle b is resolved", b.isResolved());

		VersionConstraint[] unsatisified = state.getStateHelper().getUnsatisfiedLeaves(state.getBundles());
		assertEquals("Wrong number of unsatisified leaves", 1, unsatisified.length);
		assertEquals("Wrong unsatisfied constraint", b.getRequiredBundles()[0], unsatisified[0]);
	}

	public void testBug241128_02() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		int id = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		BundleDescription a1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.FRAGMENT_HOST, "a; bundle-version=\"[1.0.0, 2.0.0)\"");
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++);

		state.addBundle(a1);
		state.addBundle(b);
		state.resolve(true);

		assertTrue("Bundle a1 is not resolved", a1.isResolved());
		assertTrue("Bundle b is not resolved", b.isResolved());

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a");
		manifest.put(Constants.BUNDLE_VERSION, "2.0.0");
		BundleDescription a2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), a1.getBundleId());
		state.updateBundle(a2);

		state.resolve(true);
		assertTrue("Bundle a2 is not resolved", a2.isResolved());
		assertFalse("Bundle b is resolved", b.isResolved());

		VersionConstraint[] unsatisified = state.getStateHelper().getUnsatisfiedLeaves(state.getBundles());
		assertEquals("Wrong number of unsatisified leaves", 1, unsatisified.length);
		assertEquals("Wrong unsatisfied constraint", b.getHost(), unsatisified[0]);
	}

	public void testBug241128_03() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		int id = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "a; version=1.0");
		BundleDescription a1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "b");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.IMPORT_PACKAGE, "a; version=\"[1.0.0, 2.0.0)\"");
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++);

		state.addBundle(a1);
		state.addBundle(b);
		state.resolve(true);

		assertTrue("Bundle a1 is not resolved", a1.isResolved());
		assertTrue("Bundle b is not resolved", b.isResolved());

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a");
		manifest.put(Constants.BUNDLE_VERSION, "2.0.0");
		manifest.put(Constants.EXPORT_PACKAGE, "a; version=2.0");
		BundleDescription a2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), a1.getBundleId());
		state.updateBundle(a2);

		state.resolve(true);
		assertTrue("Bundle a2 is not resolved", a2.isResolved());
		assertFalse("Bundle b is resolved", b.isResolved());

		VersionConstraint[] unsatisified = state.getStateHelper().getUnsatisfiedLeaves(state.getBundles());
		assertEquals("Wrong number of unsatisified leaves", 1, unsatisified.length);
		assertEquals("Wrong unsatisfied constraint", b.getImportPackages()[0], unsatisified[0]);
	}

	public void testBug241128_04() throws BundleException {
		State state = buildEmptyState();
		Dictionary[] props = new Dictionary[] {new Hashtable()};
		props[0].put("osgi.os", "win32");
		props[0].put("osgi.arch", "x86");
		state.setPlatformProperties(props);

		Hashtable manifest = new Hashtable();
		int id = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.BUNDLE_NATIVECODE, "Bundle-NativeCode: nativefile1.txt;processor=x86;osname=win32");
		BundleDescription a1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), id++);

		state.addBundle(a1);
		state.resolve(true);

		assertTrue("Bundle a1 is not resolved", a1.isResolved());

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "a");
		manifest.put(Constants.BUNDLE_VERSION, "2.0.0");
		manifest.put(Constants.BUNDLE_NATIVECODE, "Bundle-NativeCode: nativefile1.txt;processor=linux;osname=gtk");
		BundleDescription a2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_" + manifest.get(Constants.BUNDLE_VERSION), a1.getBundleId());
		state.updateBundle(a2);

		state.resolve(true);
		assertFalse("Bundle a2 is resolved", a2.isResolved());

		VersionConstraint[] unsatisified = state.getStateHelper().getUnsatisfiedConstraints(a2);
		assertEquals("Wrong number of unsatisified leaves", 1, unsatisified.length);
		assertEquals("Wrong unsatisfied constraint", a2.getNativeCodeSpecification(), unsatisified[0]);

		unsatisified = state.getStateHelper().getUnsatisfiedLeaves(state.getBundles());
		assertEquals("Wrong number of unsatisified leaves", 1, unsatisified.length);
		assertEquals("Wrong unsatisfied constraint", a2.getNativeCodeSpecification(), unsatisified[0]);
	}

	public void testGenericsBasics() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericCapablity");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(GENERIC_CAPABILITY, "foo; version=\"1.3.1\"; attr1=\"value1\"; attr2=\"value2\"");
		BundleDescription genCap1 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericRequire");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(GENERIC_REQUIRE, "foo; selection-filter=\"(version>=1.3.0)\"");
		BundleDescription genReq = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(genCap1);
		state.addBundle(genReq);

		state.resolve();

		assertTrue("Bundle genCap1 is not resolved", genCap1.isResolved());
		assertTrue("Bundle genReq is not resolved", genReq.isResolved());

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericCapablity");
		manifest.put(Constants.BUNDLE_VERSION, "2.0.0");
		manifest.put(GENERIC_CAPABILITY, "foo; version=\"1.0\"; attr1=\"value1\"; attr2=\"value2\"");
		BundleDescription genCap2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), genCap1.getBundleId());

		state.updateBundle(genCap2);

		state.resolve(true);
		assertTrue("Bundle genCap2 is not resolved", genCap2.isResolved());
		assertFalse("Bundle genReq is resolved", genReq.isResolved());

		VersionConstraint[] unsatisified = state.getStateHelper().getUnsatisfiedLeaves(state.getBundles());
		assertEquals("Wrong number of unsatisified leaves", 1, unsatisified.length);
		assertEquals("Wrong unsatisfied constraint", genReq.getGenericRequires()[0], unsatisified[0]);
	}
}
//TODO tests to enable
//testFragmentUpdateNoVersionChanged()
//testFragmentUpdateVersionChanged()
//testHostUpdateNoVersionChanged()
//testHostUpdateVersionChanged()
