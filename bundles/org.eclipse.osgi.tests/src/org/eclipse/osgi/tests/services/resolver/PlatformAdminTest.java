/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
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
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
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
		} catch (Throwable t) {
			fail("Unexpected error while cloning a BundleDescription", t);
		}
	}
}
//TODO tests to enable
//testFragmentUpdateNoVersionChanged()
//testFragmentUpdateVersionChanged()
//testHostUpdateNoVersionChanged()
//testHostUpdateVersionChanged()
