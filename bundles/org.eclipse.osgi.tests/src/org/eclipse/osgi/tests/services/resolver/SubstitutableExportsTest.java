/*******************************************************************************
 * Copyright (c) 2003, 2022 IBM Corporation and others.
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
package org.eclipse.osgi.tests.services.resolver;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Hashtable;
import org.eclipse.osgi.service.resolver.BundleDelta;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateDelta;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class SubstitutableExportsTest extends AbstractStateTest {

	private State getSubstituteBasicState() throws BundleException {
		// Basic substitutable export test with A, B, C all exporting and importing x,y
		// packages
		// D, E, F all requiring A, B, C respectively to access x, y packages
		// all should get packages x and y from A
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; z; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "A"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "B"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.addBundle(e);
		state.addBundle(f);
		return state;
	}

	private State getSubstituteUsesState() throws BundleException {
		// Same as basic substitutable export test with A, B, C all exporting and
		// importing x,y packages + "uses" clause
		// D, E, F all requiring A, B, C respectively to access x, y packages
		// all should get packages x and y from A
		// bundle G cannot resolve because of uses conflict with x package from Z
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "Z"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; version=0.5; bundle=Z, z; uses:=x; version=1.0"); //$NON-NLS-1$
		BundleDescription z = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "A"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "B"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "G"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x;  bundle=Z"); //$NON-NLS-1$
		BundleDescription g = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(z);
		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.addBundle(e);
		state.addBundle(f);
		state.addBundle(g);
		return state;
	}

	private State getSubstituteUsesCycleState() throws BundleException {
		// Same as basic substitutable export test with A, B, C all exporting and
		// importing x,y packages + "uses" clause + cycle
		// D, E, F all requiring A, B, C respectively to access x, y packages
		// all should get packages x and y from A and package z from G
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "Z"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; z; version=0.5"); //$NON-NLS-1$
		BundleDescription z = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y,z\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; z; version=1.0"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y,z\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; z; version=1.0"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y,z\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; z; version=1.0"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "A"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "B"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z"); //$NON-NLS-1$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "G"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=1.0; uses:=x"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z; version=1.0"); //$NON-NLS-1$
		BundleDescription g = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "H"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=1.0; uses:=x"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z; version=1.0"); //$NON-NLS-1$
		BundleDescription h = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(z);
		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.addBundle(e);
		state.addBundle(f);
		state.addBundle(g);
		state.addBundle(h);
		return state;
	}

	private State getSubstituteBasicFragState() throws BundleException {
		// Basic substitutable export test with A, B, C all exporting and importing x,y
		// packages
		// Each have a fragment that exports and imports z package
		// D, E, F all requiring A, B, C respectively to access x, y, z packages
		// all should get packages x, y and z from A + fragment
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "AFrag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "A"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z; version=1.0"); //$NON-NLS-1$
		BundleDescription aFrag = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "BFrag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "B"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z; version=1.0"); //$NON-NLS-1$
		BundleDescription bFrag = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "CFrag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "C"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z; version=1.0"); //$NON-NLS-1$
		BundleDescription cFrag = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "A"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "B"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(a);
		state.addBundle(aFrag);
		state.addBundle(b);
		state.addBundle(bFrag);
		state.addBundle(c);
		state.addBundle(cFrag);
		state.addBundle(d);
		state.addBundle(e);
		state.addBundle(f);
		return state;
	}

	private State getSubstituteUsesFragState() throws BundleException {
		// Basic substitutable export test with A, B, C all exporting and importing x,y
		// packages + "uses" clause
		// Each have a fragment that exports and imports z package
		// D, E, F all requiring A, B, C respectively to access x, y, z packages
		// all should get packages x, y and z from A + fragment
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "Z"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; version=0.5; bundle=Z"); //$NON-NLS-1$
		BundleDescription z = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "AFrag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "A"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=1.0; uses:=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z; version=1.0"); //$NON-NLS-1$
		BundleDescription aFrag = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "BFrag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "B"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=1.0; uses:=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z; version=1.0"); //$NON-NLS-1$
		BundleDescription bFrag = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "CFrag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "C"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=1.0; uses:=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z; version=1.0"); //$NON-NLS-1$
		BundleDescription cFrag = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "A"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "B"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "G"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x;  bundle=Z"); //$NON-NLS-1$
		BundleDescription g = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(z);
		state.addBundle(a);
		state.addBundle(aFrag);
		state.addBundle(b);
		state.addBundle(bFrag);
		state.addBundle(c);
		state.addBundle(cFrag);
		state.addBundle(d);
		state.addBundle(e);
		state.addBundle(f);
		state.addBundle(g);
		return state;
	}

	private State getSubstituteUsesFragCycleState() throws BundleException {
		// Same as basic substitutable export test with A, B, C all exporting and
		// importing x,y packages + "uses" clause + cycle + frags
		// D, E, F all requiring A, B, C respectively to access x, y packages
		// all should get packages x and y from A and package z from G
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "Z"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; z; version=2.0"); //$NON-NLS-1$
		BundleDescription z = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y,q\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=\"[1.0,2.0)\", q"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "AFrag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "A"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=1.0; uses:=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z; version=\"[1.0,2.0)\""); //$NON-NLS-1$
		BundleDescription aFrag = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y,q\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=\"[1.0,2.0)\", q"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "BFrag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "B"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=1.0; uses:=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z; version=\"[1.0,2.0)\""); //$NON-NLS-1$
		BundleDescription bFrag = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y,q\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=\"[1.0,2.0)\", q"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "CFrag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "C"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=1.0; uses:=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z; version=\"[1.0,2.0)\""); //$NON-NLS-1$
		BundleDescription cFrag = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "A"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "B"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "G"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "q; version=1.0; uses:=x"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "q; version=1.0"); //$NON-NLS-1$
		BundleDescription g = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "H"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "q; version=1.0; uses:=x"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "q; version=1.0"); //$NON-NLS-1$
		BundleDescription h = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "I"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "q; x; version=1.0"); //$NON-NLS-1$
		BundleDescription i = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(z);
		state.addBundle(a);
		state.addBundle(aFrag);
		state.addBundle(b);
		state.addBundle(bFrag);
		state.addBundle(c);
		state.addBundle(cFrag);
		state.addBundle(d);
		state.addBundle(e);
		state.addBundle(f);
		state.addBundle(g);
		state.addBundle(h);
		state.addBundle(i);
		return state;
	}

	private State getSubstituteBasicReexportState() throws BundleException {
		// Basic substitutable export test with A, B, C all exporting and importing x,y
		// packages
		// D, E, F all requiring A, B, C respectively to access x, y packages are
		// reexporting
		// G, H, I all requiring D, E, F repectively to access x, y packages
		// all should get packages x and y from A
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "A; visibility:=reexport"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "B; visibility:=reexport"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C; visibility:=reexport"); //$NON-NLS-1$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "G"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "D"); //$NON-NLS-1$
		BundleDescription g = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "H"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "E"); //$NON-NLS-1$
		BundleDescription h = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "I"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "F"); //$NON-NLS-1$
		BundleDescription i = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.addBundle(e);
		state.addBundle(f);
		state.addBundle(g);
		state.addBundle(h);
		state.addBundle(i);
		return state;
	}

	private State getSubstituteUsesReexportState() throws BundleException {
		// Basic substitutable export test with A, B, C all exporting and importing x,y
		// packages + "uses" clause
		// D, E, F all requiring A, B, C respectively to access x, y packages are
		// reexporting
		// G, H, I all requiring D, E, F repectively to access x, y packages
		// all should get packages x and y from A
		// J cannot resolve because of uses conflicy with package x from Z
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "Z"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; version=0.5; bundle=Z"); //$NON-NLS-1$
		BundleDescription z = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "A; visibility:=reexport"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "B; visibility:=reexport"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C; visibility:=reexport"); //$NON-NLS-1$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "G"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "D"); //$NON-NLS-1$
		BundleDescription g = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "H"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "E"); //$NON-NLS-1$
		BundleDescription h = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "I"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "F"); //$NON-NLS-1$
		BundleDescription i = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "J"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "F"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x;  bundle=Z"); //$NON-NLS-1$
		BundleDescription j = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(z);
		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.addBundle(e);
		state.addBundle(f);
		state.addBundle(g);
		state.addBundle(h);
		state.addBundle(i);
		state.addBundle(j);
		return state;
	}

	private State getSubstituteUsesReexportCycleState() throws BundleException {
		// Basic substitutable export test with A, B, C all exporting and importing x,y
		// packages + uses clause + cycle
		// D, E, F all requiring A, B, C respectively to access x, y packages are
		// reexporting
		// G, H, I all requiring D, E, F repectively to access x, y packages
		// all should get packages x and y from A
		// J cannot resolve because of uses conflicy with package x from Z
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "Z"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; z; version=2.0"); //$NON-NLS-1$
		BundleDescription z = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y,z\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; z; version=\"[1.0,2.0)\""); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y,z\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; z; version=\"[1.0,2.0)\""); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y,z\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; z; version=\"[1.0,2.0)\""); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "A; visibility:=reexport"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "B; visibility:=reexport"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C; visibility:=reexport"); //$NON-NLS-1$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "G"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "D"); //$NON-NLS-1$
		BundleDescription g = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "H"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "E"); //$NON-NLS-1$
		BundleDescription h = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "I"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "F"); //$NON-NLS-1$
		BundleDescription i = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "J"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "F"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=1.0; uses=\"x,y\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z; version=\"[1.0,2.0)\""); //$NON-NLS-1$
		BundleDescription j = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(z);
		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.addBundle(e);
		state.addBundle(f);
		state.addBundle(g);
		state.addBundle(h);
		state.addBundle(i);
		state.addBundle(j);
		return state;
	}

	private State getSubstituteUnresolvedFragState() throws BundleException {
		// Basic substitutable export test with A, B, C all exporting and importing x,y
		// packages
		// Each have a fragment that exports and imports z package and an extra
		// unresolved import of q
		// D, E, F all requiring A, B, C respectively to access x, y. z should not be
		// accessible
		// all should get packages x, y.
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "AFrag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "A"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z; q; version=1.0"); //$NON-NLS-1$
		BundleDescription aFrag = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "BFrag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "B"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z; q; version=1.0"); //$NON-NLS-1$
		BundleDescription bFrag = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "CFrag"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.FRAGMENT_HOST, "C"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z, q; version=1.0"); //$NON-NLS-1$
		BundleDescription cFrag = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "A"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "B"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(a);
		state.addBundle(aFrag);
		state.addBundle(b);
		state.addBundle(bFrag);
		state.addBundle(c);
		state.addBundle(cFrag);
		state.addBundle(d);
		state.addBundle(e);
		state.addBundle(f);
		return state;
	}

	private State getSubstituteSplitState() throws BundleException {
		// Basic substitutable export test with A, B, C all exporting and importing x,y
		// packages
		// D, E, F all requiring A, B, C respectively to access x, y packages and export
		// more content (split)
		// all should get packages x and y from A
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; split=split; mandatory:=split"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0; split=split"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; split=split; mandatory:=split"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0; split=split"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; split=split; mandatory:=split"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0; split=split"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "A"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "B"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "G"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription g = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "H"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "D"); //$NON-NLS-1$
		BundleDescription h = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "I"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "E"); //$NON-NLS-1$
		BundleDescription i = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "J"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "F"); //$NON-NLS-1$
		BundleDescription j = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.addBundle(e);
		state.addBundle(f);
		state.addBundle(g);
		state.addBundle(h);
		state.addBundle(i);
		state.addBundle(j);
		return state;
	}

	private State getSubstituteSplitUsesState() throws BundleException {
		// Basic substitutable export test with A, B, C all exporting and importing x,y
		// packages + uses
		// D, E, F all requiring A, B, C respectively to access x, y packages and add
		// more content (split)
		// all should get packages x and y from A
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; split=split; mandatory:=split; uses:=\"x,y,q\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0; split=split, q; version=\"[1.0,2.0)\""); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; split=split; mandatory:=split; uses:=\"x,y,q\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0; split=split, q; version=\"[1.0,2.0)\""); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; split=split; mandatory:=split; uses:=\"x,y,q\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0; split=split, q; version=\"[1.0,2.0)\""); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y,r\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0, r; version=\"[1.0,2.0)\""); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "A"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y,r\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0, r; version=\"[1.0,2.0)\""); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "B"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0; uses:=\"x,y,r\""); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0, r; version=\"[1.0,2.0)\""); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "G"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription g = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "H"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "D"); //$NON-NLS-1$
		BundleDescription h = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "I"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "E"); //$NON-NLS-1$
		BundleDescription i = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "J"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "F"); //$NON-NLS-1$
		BundleDescription j = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "K"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "q; r; version=2.0"); //$NON-NLS-1$
		BundleDescription k = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "L"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "q; r; version=1.0"); //$NON-NLS-1$
		BundleDescription l = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "M"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x, y, q, r"); //$NON-NLS-1$
		BundleDescription m = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "N"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "q, r"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "F"); //$NON-NLS-1$
		BundleDescription n = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.addBundle(e);
		state.addBundle(f);
		state.addBundle(g);
		state.addBundle(h);
		state.addBundle(i);
		state.addBundle(j);
		state.addBundle(k);
		state.addBundle(l);
		state.addBundle(m);
		state.addBundle(n);
		return state;
	}

	private State getNonOverlapingSubstituteBasicState() throws BundleException {
		// Basic substitutable export test with A, B, C all exporting and importing x,y
		// packages
		// D, E, F all requiring A, B, C respectively to access x, y packages
		// all should get packages x and y from A
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0; nomatch=nomatch"); //$NON-NLS-1$
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "x; y; version=1.0"); //$NON-NLS-1$
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "A"); //$NON-NLS-1$
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "B"); //$NON-NLS-1$
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "F"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		BundleDescription f = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.addBundle(e);
		state.addBundle(f);
		return state;
	}

	@Test
	public void testSubstitutableExports001() throws BundleException {
		State state = getSubstituteBasicState();
		state.resolve();
		BundleDescription a = state.getBundle(0);
		BundleDescription b = state.getBundle(1);
		BundleDescription c = state.getBundle(2);
		BundleDescription d = state.getBundle(3);
		BundleDescription e = state.getBundle(4);
		BundleDescription f = state.getBundle(5);

		assertTrue("1.0", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aVisible = state.getStateHelper().getVisiblePackages(a);
		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);

		assertNotNull("aVisible is null", aVisible); //$NON-NLS-1$
		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$

		assertEquals("aVisible wrong number", 0, aVisible.length); //$NON-NLS-1$
		assertEquals("bVisible wrong number", 2, bVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 2, cVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 3, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 2, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 2, fVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] aExports = a.getSelectedExports();
		ExportPackageDescription[] aSubtitutes = new ExportPackageDescription[] { aExports[0], aExports[1] };
		assertArrayEquals("aVisible not correct", aExports, a.getExportPackages()); //$NON-NLS-1$
		assertArrayEquals("bVisible not correct", aSubtitutes, bVisible); //$NON-NLS-1$
		assertArrayEquals("cVisible not correct", aSubtitutes, cVisible); //$NON-NLS-1$
		assertArrayEquals("dVisible not correct", aExports, dVisible); //$NON-NLS-1$
		assertArrayEquals("eVisible not correct", aSubtitutes, eVisible); //$NON-NLS-1$
		assertArrayEquals("fVisible not correct", aSubtitutes, fVisible); //$NON-NLS-1$
	}

	@Test
	public void testSubstitutableExports002() throws BundleException {
		State state = getSubstituteUsesState();
		state.resolve();
		BundleDescription a = state.getBundle(1);
		BundleDescription b = state.getBundle(2);
		BundleDescription c = state.getBundle(3);
		BundleDescription d = state.getBundle(4);
		BundleDescription e = state.getBundle(5);
		BundleDescription f = state.getBundle(6);
		BundleDescription g = state.getBundle(7);

		assertTrue("1.0", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$
		assertFalse("1.6", g.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aVisible = state.getStateHelper().getVisiblePackages(a);
		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);
		ExportPackageDescription[] gVisible = state.getStateHelper().getVisiblePackages(g);

		assertNotNull("aVisible is null", aVisible); //$NON-NLS-1$
		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$
		assertNotNull("gVisible is null", gVisible); //$NON-NLS-1$

		assertEquals("aVisible wrong number", 0, aVisible.length); //$NON-NLS-1$
		assertEquals("bVisible wrong number", 2, bVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 2, cVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 2, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 2, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 2, fVisible.length); //$NON-NLS-1$
		assertEquals("gVisible wrong number", 0, gVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] aExports = a.getSelectedExports();
		assertArrayEquals("aVisible not correct", aExports, a.getExportPackages()); //$NON-NLS-1$
		assertArrayEquals("bVisible not correct", aExports, bVisible); //$NON-NLS-1$
		assertArrayEquals("cVisible not correct", aExports, cVisible); //$NON-NLS-1$
		assertArrayEquals("dVisible not correct", aExports, dVisible); //$NON-NLS-1$
		assertArrayEquals("eVisible not correct", aExports, eVisible); //$NON-NLS-1$
		assertArrayEquals("fVisible not correct", aExports, fVisible); //$NON-NLS-1$
	}

	@Test
	public void testSubstitutableExports003() throws BundleException {
		State state = getSubstituteUsesCycleState();
		state.resolve();
		BundleDescription a = state.getBundle(1);
		BundleDescription b = state.getBundle(2);
		BundleDescription c = state.getBundle(3);
		BundleDescription d = state.getBundle(4);
		BundleDescription e = state.getBundle(5);
		BundleDescription f = state.getBundle(6);
		BundleDescription g = state.getBundle(7);
		BundleDescription h = state.getBundle(8);

		assertTrue("1.0", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$
		assertTrue("1.6", g.isResolved()); //$NON-NLS-1$
		assertTrue("1.7", h.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aVisible = state.getStateHelper().getVisiblePackages(a);
		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);
		ExportPackageDescription[] gVisible = state.getStateHelper().getVisiblePackages(g);
		ExportPackageDescription[] hVisible = state.getStateHelper().getVisiblePackages(h);

		assertNotNull("aVisible is null", aVisible); //$NON-NLS-1$
		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$
		assertNotNull("gVisible is null", gVisible); //$NON-NLS-1$

		assertEquals("aVisible wrong number", 1, aVisible.length); //$NON-NLS-1$
		assertEquals("bVisible wrong number", 3, bVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 3, cVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 3, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 3, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 3, fVisible.length); //$NON-NLS-1$
		assertEquals("gVisible wrong number", 2, gVisible.length); //$NON-NLS-1$
		assertEquals("hVisible wrong number", 3, hVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] aExported = a.getSelectedExports();
		ExportPackageDescription[] gExported = g.getSelectedExports();
		ExportPackageDescription[] expected = new ExportPackageDescription[aExported.length + gExported.length];
		System.arraycopy(aExported, 0, expected, 0, aExported.length);
		System.arraycopy(gExported, 0, expected, aExported.length, gExported.length);
		for (int i = 0; i < expected.length; i++) {
			if (i == 2)
				assertContains("aVisible not correct", aVisible, expected[i]); //$NON-NLS-1$
			assertContains("bVisible not correct", bVisible, expected[i]); //$NON-NLS-1$
			assertContains("cVisible not correct", cVisible, expected[i]); //$NON-NLS-1$
			assertContains("dVisible not correct", dVisible, expected[i]); //$NON-NLS-1$
			assertContains("eVisible not correct", eVisible, expected[i]); //$NON-NLS-1$
			assertContains("fVisible not correct", fVisible, expected[i]); //$NON-NLS-1$
			if (i == 0 || i == 1)
				assertContains("gVisible not correct", gVisible, expected[i]); //$NON-NLS-1$
			assertContains("hVisible not correct", hVisible, expected[i]); //$NON-NLS-1$
		}
	}

	@Test
	public void testSubstitutableExports004() throws BundleException {
		State state = getSubstituteBasicFragState();
		state.resolve();

		BundleDescription a = state.getBundle(0);
		BundleDescription aFrag = state.getBundle(1);
		BundleDescription b = state.getBundle(2);
		BundleDescription bFrag = state.getBundle(3);
		BundleDescription c = state.getBundle(4);
		BundleDescription cFrag = state.getBundle(5);
		BundleDescription d = state.getBundle(6);
		BundleDescription e = state.getBundle(7);
		BundleDescription f = state.getBundle(8);

		assertTrue("1.0", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.0.1", aFrag.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.1.1", bFrag.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.2.1", cFrag.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aVisible = state.getStateHelper().getVisiblePackages(a);
		ExportPackageDescription[] aFragVisible = state.getStateHelper().getVisiblePackages(aFrag);
		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] bFragVisible = state.getStateHelper().getVisiblePackages(bFrag);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] cFragVisible = state.getStateHelper().getVisiblePackages(cFrag);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);

		assertNotNull("aVisible is null", aVisible); //$NON-NLS-1$
		assertNotNull("aFragVisible is null", aFragVisible); //$NON-NLS-1$
		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("bFragVisible is null", bFragVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("cFragVisible is null", cFragVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$

		assertEquals("aVisible wrong number", 0, aVisible.length); //$NON-NLS-1$
		assertEquals("aFragVisible wrong number", 0, aFragVisible.length); //$NON-NLS-1$
		assertEquals("bVisible wrong number", 2, bVisible.length); //$NON-NLS-1$
		assertEquals("bFragVisible wrong number", 1, bFragVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 2, cVisible.length); //$NON-NLS-1$
		assertEquals("cFragVisible wrong number", 1, cFragVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 3, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 3, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 3, fVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] aExports = a.getSelectedExports();
		assertEquals("a has wrong number of exports", 3, aExports.length); //$NON-NLS-1$
		ExportPackageDescription[] aDeclaredExports = a.getExportPackages();
		ExportPackageDescription[] aFragExports = new ExportPackageDescription[] { aExports[2] };
		assertArrayEquals("bVisible not correct", aDeclaredExports, bVisible); //$NON-NLS-1$
		assertArrayEquals("bFragVisible not correct", aFragExports, bFragVisible); //$NON-NLS-1$
		assertArrayEquals("cVisible not correct", aDeclaredExports, cVisible); //$NON-NLS-1$
		assertArrayEquals("cFragVisible not correct", aFragExports, cFragVisible); //$NON-NLS-1$

		assertArrayEquals("dVisible not correct", aExports, dVisible); //$NON-NLS-1$
		assertArrayEquals("eVisible not correct", aExports, eVisible); //$NON-NLS-1$
		assertArrayEquals("fVisible not correct", aExports, fVisible); //$NON-NLS-1$
	}

	@Test
	public void testSubstitutableExports005() throws BundleException {
		State state = getSubstituteUsesFragState();
		state.resolve();

		// BundleDescription z = state.getBundle(0);
		BundleDescription a = state.getBundle(1);
		BundleDescription aFrag = state.getBundle(2);
		BundleDescription b = state.getBundle(3);
		BundleDescription bFrag = state.getBundle(4);
		BundleDescription c = state.getBundle(5);
		BundleDescription cFrag = state.getBundle(6);
		BundleDescription d = state.getBundle(7);
		BundleDescription e = state.getBundle(8);
		BundleDescription f = state.getBundle(9);
		BundleDescription g = state.getBundle(10);

		assertTrue("1.0", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.0.1", aFrag.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.1.1", bFrag.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.2.1", cFrag.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$
		assertFalse("1.6", g.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aVisible = state.getStateHelper().getVisiblePackages(a);
		ExportPackageDescription[] aFragVisible = state.getStateHelper().getVisiblePackages(aFrag);
		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] bFragVisible = state.getStateHelper().getVisiblePackages(bFrag);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] cFragVisible = state.getStateHelper().getVisiblePackages(cFrag);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);
		ExportPackageDescription[] gVisible = state.getStateHelper().getVisiblePackages(g);

		assertNotNull("aVisible is null", aVisible); //$NON-NLS-1$
		assertNotNull("aFragVisible is null", aFragVisible); //$NON-NLS-1$
		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("bFragVisible is null", bFragVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("cFragVisible is null", cFragVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$
		assertNotNull("gVisible is null", gVisible); //$NON-NLS-1$

		assertEquals("aVisible wrong number", 0, aVisible.length); //$NON-NLS-1$
		assertEquals("aFragVisible wrong number", 0, aFragVisible.length); //$NON-NLS-1$
		assertEquals("bVisible wrong number", 2, bVisible.length); //$NON-NLS-1$
		assertEquals("bFragVisible wrong number", 1, bFragVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 2, cVisible.length); //$NON-NLS-1$
		assertEquals("cFragVisible wrong number", 1, cFragVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 3, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 3, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 3, fVisible.length); //$NON-NLS-1$
		assertEquals("gVisible wrong number", 0, gVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] aExports = a.getSelectedExports();
		assertEquals("a has wrong number of exports", 3, aExports.length); //$NON-NLS-1$
		ExportPackageDescription[] aDeclaredExports = a.getExportPackages();
		ExportPackageDescription[] aFragExports = new ExportPackageDescription[] { aExports[2] };
		assertArrayEquals("bVisible not correct", aDeclaredExports, bVisible); //$NON-NLS-1$
		assertArrayEquals("bFragVisible not correct", aFragExports, bFragVisible); //$NON-NLS-1$
		assertArrayEquals("cVisible not correct", aDeclaredExports, cVisible); //$NON-NLS-1$
		assertArrayEquals("cFragVisible not correct", aFragExports, cFragVisible); //$NON-NLS-1$

		assertArrayEquals("dVisible not correct", aExports, dVisible); //$NON-NLS-1$
		assertArrayEquals("eVisible not correct", aExports, eVisible); //$NON-NLS-1$
		assertArrayEquals("fVisible not correct", aExports, fVisible); //$NON-NLS-1$
	}

	@Test
	public void testSubstitutableExports006() throws BundleException {
		State state = getSubstituteUsesFragCycleState();
		state.resolve();

		// BundleDescription z = state.getBundle(0);
		BundleDescription a = state.getBundle(1);
		BundleDescription aFrag = state.getBundle(2);
		BundleDescription b = state.getBundle(3);
		BundleDescription bFrag = state.getBundle(4);
		BundleDescription c = state.getBundle(5);
		BundleDescription cFrag = state.getBundle(6);
		BundleDescription d = state.getBundle(7);
		BundleDescription e = state.getBundle(8);
		BundleDescription f = state.getBundle(9);
		BundleDescription g = state.getBundle(10);
		BundleDescription h = state.getBundle(11);
		BundleDescription i = state.getBundle(12);

		assertTrue("1.0", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.0.1", aFrag.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.1.1", bFrag.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.2.1", cFrag.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$
		assertTrue("1.6", g.isResolved()); //$NON-NLS-1$
		assertTrue("1.7", h.isResolved()); //$NON-NLS-1$
		assertTrue("1.7", i.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aVisible = state.getStateHelper().getVisiblePackages(a);
		ExportPackageDescription[] aFragVisible = state.getStateHelper().getVisiblePackages(aFrag);
		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] bFragVisible = state.getStateHelper().getVisiblePackages(bFrag);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] cFragVisible = state.getStateHelper().getVisiblePackages(cFrag);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);
		ExportPackageDescription[] gVisible = state.getStateHelper().getVisiblePackages(g);
		ExportPackageDescription[] hVisible = state.getStateHelper().getVisiblePackages(h);
		ExportPackageDescription[] iVisible = state.getStateHelper().getVisiblePackages(i);

		assertNotNull("aVisible is null", aVisible); //$NON-NLS-1$
		assertNotNull("aFragVisible is null", aFragVisible); //$NON-NLS-1$
		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("bFragVisible is null", bFragVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("cFragVisible is null", cFragVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$
		assertNotNull("gVisible is null", gVisible); //$NON-NLS-1$
		assertNotNull("gVisible is null", hVisible); //$NON-NLS-1$
		assertNotNull("iVisible is null", iVisible); //$NON-NLS-1$

		assertEquals("aVisible wrong number", 1, aVisible.length); //$NON-NLS-1$
		assertEquals("aFragVisible wrong number", 0, aFragVisible.length); //$NON-NLS-1$
		assertEquals("bVisible wrong number", 3, bVisible.length); //$NON-NLS-1$
		assertEquals("bFragVisible wrong number", 1, bFragVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 3, cVisible.length); //$NON-NLS-1$
		assertEquals("cFragVisible wrong number", 1, cFragVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 3, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 3, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 3, fVisible.length); //$NON-NLS-1$
		assertEquals("gVisible wrong number", 3, gVisible.length); //$NON-NLS-1$
		assertEquals("hVisible wrong number", 4, hVisible.length); //$NON-NLS-1$
		assertEquals("iVisible wrong number", 4, iVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] aExports = a.getSelectedExports();

		assertEquals("a has wrong number of exports", 3, aExports.length); //$NON-NLS-1$
		ExportPackageDescription[] aDeclaredExports = a.getExportPackages();
		ExportPackageDescription[] bcExpectedVisible = new ExportPackageDescription[aDeclaredExports.length + 1];
		System.arraycopy(aDeclaredExports, 0, bcExpectedVisible, 0, aDeclaredExports.length);
		bcExpectedVisible[2] = g.getSelectedExports()[0];
		ExportPackageDescription[] aFragExports = new ExportPackageDescription[] { aExports[2] };
		assertArrayEquals("aVisible not correct", g.getSelectedExports(), aVisible); //$NON-NLS-1$
		assertArrayEquals("bVisible not correct", bcExpectedVisible, bVisible); //$NON-NLS-1$
		assertArrayEquals("bFragVisible not correct", aFragExports, bFragVisible); //$NON-NLS-1$
		assertArrayEquals("cVisible not correct", bcExpectedVisible, cVisible); //$NON-NLS-1$
		assertArrayEquals("cFragVisible not correct", aFragExports, cFragVisible); //$NON-NLS-1$

		assertArrayEquals("dVisible not correct", aExports, dVisible); //$NON-NLS-1$
		assertArrayEquals("eVisible not correct", aExports, eVisible); //$NON-NLS-1$
		assertArrayEquals("fVisible not correct", aExports, fVisible); //$NON-NLS-1$

		ExportPackageDescription[] gExports = g.getSelectedExports();
		assertEquals("g has wrong number of exports", 1, gExports.length); //$NON-NLS-1$
		ExportPackageDescription[] expectedHIVisible = new ExportPackageDescription[] { gExports[0], aExports[0],
				aExports[1], aExports[2] };
		assertArrayEquals("gVisible not correct", aExports, gVisible); //$NON-NLS-1$
		assertArrayEquals("hVisible not correct", expectedHIVisible, hVisible); //$NON-NLS-1$
		assertArrayEquals("iVisible not correct", expectedHIVisible, iVisible); //$NON-NLS-1$
	}

	@Test
	public void testSubstitutableExports007() throws BundleException {
		State state = getSubstituteBasicReexportState();
		state.resolve();

		BundleDescription a = state.getBundle(0);
		BundleDescription b = state.getBundle(1);
		BundleDescription c = state.getBundle(2);
		BundleDescription d = state.getBundle(3);
		BundleDescription e = state.getBundle(4);
		BundleDescription f = state.getBundle(5);
		BundleDescription g = state.getBundle(5);
		BundleDescription h = state.getBundle(5);
		BundleDescription i = state.getBundle(5);

		assertTrue("1.0", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", g.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", h.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", i.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aVisible = state.getStateHelper().getVisiblePackages(a);
		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);
		ExportPackageDescription[] gVisible = state.getStateHelper().getVisiblePackages(g);
		ExportPackageDescription[] hVisible = state.getStateHelper().getVisiblePackages(h);
		ExportPackageDescription[] iVisible = state.getStateHelper().getVisiblePackages(i);

		assertNotNull("aVisible is null", aVisible); //$NON-NLS-1$
		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$
		assertNotNull("gVisible is null", gVisible); //$NON-NLS-1$
		assertNotNull("hVisible is null", hVisible); //$NON-NLS-1$
		assertNotNull("iVisible is null", iVisible); //$NON-NLS-1$

		assertEquals("aVisible wrong number", 0, aVisible.length); //$NON-NLS-1$
		assertEquals("bVisible wrong number", 2, bVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 2, cVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 2, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 2, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 2, fVisible.length); //$NON-NLS-1$
		assertEquals("gVisible wrong number", 2, gVisible.length); //$NON-NLS-1$
		assertEquals("hVisible wrong number", 2, hVisible.length); //$NON-NLS-1$
		assertEquals("iVisible wrong number", 2, iVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] aExports = a.getSelectedExports();
		assertArrayEquals("bVisible not correct", aExports, bVisible); //$NON-NLS-1$
		assertArrayEquals("cVisible not correct", aExports, cVisible); //$NON-NLS-1$
		assertArrayEquals("dVisible not correct", aExports, dVisible); //$NON-NLS-1$
		assertArrayEquals("eVisible not correct", aExports, eVisible); //$NON-NLS-1$
		assertArrayEquals("fVisible not correct", aExports, fVisible); //$NON-NLS-1$
		assertArrayEquals("gVisible not correct", aExports, gVisible); //$NON-NLS-1$
		assertArrayEquals("hVisible not correct", aExports, hVisible); //$NON-NLS-1$
		assertArrayEquals("iVisible not correct", aExports, iVisible); //$NON-NLS-1$
	}

	@Test
	public void testSubstitutableExports008() throws BundleException {
		State state = getSubstituteUsesReexportState();
		state.resolve();

		BundleDescription a = state.getBundle(1);
		BundleDescription b = state.getBundle(2);
		BundleDescription c = state.getBundle(3);
		BundleDescription d = state.getBundle(4);
		BundleDescription e = state.getBundle(5);
		BundleDescription f = state.getBundle(6);
		BundleDescription g = state.getBundle(7);
		BundleDescription h = state.getBundle(8);
		BundleDescription i = state.getBundle(9);
		BundleDescription j = state.getBundle(10);

		assertTrue("1.0", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$
		assertTrue("1.6", g.isResolved()); //$NON-NLS-1$
		assertTrue("1.7", h.isResolved()); //$NON-NLS-1$
		assertTrue("1.8", i.isResolved()); //$NON-NLS-1$
		assertFalse("1.9", j.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aVisible = state.getStateHelper().getVisiblePackages(a);
		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);
		ExportPackageDescription[] gVisible = state.getStateHelper().getVisiblePackages(g);
		ExportPackageDescription[] hVisible = state.getStateHelper().getVisiblePackages(h);
		ExportPackageDescription[] iVisible = state.getStateHelper().getVisiblePackages(i);
		ExportPackageDescription[] jVisible = state.getStateHelper().getVisiblePackages(j);

		assertNotNull("aVisible is null", aVisible); //$NON-NLS-1$
		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$
		assertNotNull("gVisible is null", gVisible); //$NON-NLS-1$
		assertNotNull("hVisible is null", hVisible); //$NON-NLS-1$
		assertNotNull("iVisible is null", iVisible); //$NON-NLS-1$
		assertNotNull("jVisible is null", jVisible); //$NON-NLS-1$

		assertEquals("aVisible wrong number", 0, aVisible.length); //$NON-NLS-1$
		assertEquals("bVisible wrong number", 2, bVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 2, cVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 2, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 2, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 2, fVisible.length); //$NON-NLS-1$
		assertEquals("gVisible wrong number", 2, gVisible.length); //$NON-NLS-1$
		assertEquals("hVisible wrong number", 2, hVisible.length); //$NON-NLS-1$
		assertEquals("iVisible wrong number", 2, iVisible.length); //$NON-NLS-1$
		assertEquals("jVisible wrong number", 0, jVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] aExports = a.getSelectedExports();
		assertArrayEquals("bVisible not correct", aExports, bVisible); //$NON-NLS-1$
		assertArrayEquals("cVisible not correct", aExports, cVisible); //$NON-NLS-1$
		assertArrayEquals("dVisible not correct", aExports, dVisible); //$NON-NLS-1$
		assertArrayEquals("eVisible not correct", aExports, eVisible); //$NON-NLS-1$
		assertArrayEquals("fVisible not correct", aExports, fVisible); //$NON-NLS-1$
		assertArrayEquals("gVisible not correct", aExports, gVisible); //$NON-NLS-1$
		assertArrayEquals("hVisible not correct", aExports, hVisible); //$NON-NLS-1$
		assertArrayEquals("iVisible not correct", aExports, iVisible); //$NON-NLS-1$
	}

	@Test
	public void testSubstitutableExports009() throws BundleException {
		State state = getSubstituteUsesReexportCycleState();
		state.resolve();
		BundleDescription a = state.getBundle(1);
		BundleDescription b = state.getBundle(2);
		BundleDescription c = state.getBundle(3);
		BundleDescription d = state.getBundle(4);
		BundleDescription e = state.getBundle(5);
		BundleDescription f = state.getBundle(6);
		BundleDescription g = state.getBundle(7);
		BundleDescription h = state.getBundle(8);
		BundleDescription i = state.getBundle(9);
		BundleDescription j = state.getBundle(10);

		assertTrue("1.0", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$
		assertTrue("1.6", g.isResolved()); //$NON-NLS-1$
		assertTrue("1.7", h.isResolved()); //$NON-NLS-1$
		assertTrue("1.8", i.isResolved()); //$NON-NLS-1$
		assertTrue("1.9", j.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aVisible = state.getStateHelper().getVisiblePackages(a);
		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);
		ExportPackageDescription[] gVisible = state.getStateHelper().getVisiblePackages(g);
		ExportPackageDescription[] hVisible = state.getStateHelper().getVisiblePackages(h);
		ExportPackageDescription[] iVisible = state.getStateHelper().getVisiblePackages(i);
		ExportPackageDescription[] jVisible = state.getStateHelper().getVisiblePackages(j);

		assertNotNull("aVisible is null", aVisible); //$NON-NLS-1$
		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$
		assertNotNull("gVisible is null", gVisible); //$NON-NLS-1$
		assertNotNull("hVisible is null", hVisible); //$NON-NLS-1$
		assertNotNull("iVisible is null", iVisible); //$NON-NLS-1$
		assertNotNull("jVisible is null", jVisible); //$NON-NLS-1$

		assertEquals("aVisible wrong number", 1, aVisible.length); //$NON-NLS-1$
		assertEquals("bVisible wrong number", 3, bVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 3, cVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 2, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 2, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 2, fVisible.length); //$NON-NLS-1$
		assertEquals("gVisible wrong number", 2, gVisible.length); //$NON-NLS-1$
		assertEquals("hVisible wrong number", 2, hVisible.length); //$NON-NLS-1$
		assertEquals("iVisible wrong number", 2, iVisible.length); //$NON-NLS-1$
		assertEquals("jVisible wrong number", 2, jVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] aExports = a.getSelectedExports();
		assertEquals("aExports count wrong", 2, aExports.length); //$NON-NLS-1$
		ExportPackageDescription[] jExports = j.getSelectedExports();
		assertEquals("aExports count wrong", 1, jExports.length); //$NON-NLS-1$
		ExportPackageDescription[] bcExpected = new ExportPackageDescription[] { aExports[0], aExports[1],
				jExports[0] };
		assertArrayEquals("aVisible not correct", jExports, aVisible); //$NON-NLS-1$
		assertArrayEquals("bVisible not correct", bcExpected, bVisible); //$NON-NLS-1$
		assertArrayEquals("cVisible not correct", bcExpected, cVisible); //$NON-NLS-1$
		assertArrayEquals("dVisible not correct", aExports, dVisible); //$NON-NLS-1$
		assertArrayEquals("eVisible not correct", aExports, eVisible); //$NON-NLS-1$
		assertArrayEquals("fVisible not correct", aExports, fVisible); //$NON-NLS-1$
		assertArrayEquals("gVisible not correct", aExports, gVisible); //$NON-NLS-1$
		assertArrayEquals("hVisible not correct", aExports, hVisible); //$NON-NLS-1$
		assertArrayEquals("iVisible not correct", aExports, iVisible); //$NON-NLS-1$
		assertArrayEquals("jVisible not correct", aExports, jVisible); //$NON-NLS-1$
	}

	@Test
	public void testSubstitutableExports010() throws BundleException {
		State state = getSubstituteBasicState();
		state.resolve();
		BundleDescription a = state.getBundle(0);
		BundleDescription b = state.getBundle(1);
		BundleDescription c = state.getBundle(2);
		BundleDescription d = state.getBundle(3);
		BundleDescription e = state.getBundle(4);
		BundleDescription f = state.getBundle(5);

		BundleDescription[] allBundles = new BundleDescription[] { a, b, c, d, e, f };
		StateDelta stateDelta = state.resolve(new BundleDescription[] { a });
		BundleDelta[] bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 6, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : allBundles) {
			boolean found = false;
			for (int j = 0; j < bundleDeltas.length && !found; j++) {
				assertEquals("unexpected delta type " + bundleDeltas[j], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[j].getType());
				found = bundleDeltas[j].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}
		stateDelta = state.resolve(new BundleDescription[] { f });
		bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 1, bundleDeltas.length); //$NON-NLS-1$
	}

	@Test
	public void testSubstitutableExports011() throws BundleException {
		State state = getSubstituteUsesState();
		state.resolve();
		BundleDescription z = state.getBundle(0);
		BundleDescription a = state.getBundle(1);
		BundleDescription b = state.getBundle(2);
		BundleDescription c = state.getBundle(3);
		BundleDescription d = state.getBundle(4);
		BundleDescription e = state.getBundle(5);
		BundleDescription f = state.getBundle(6);

		BundleDescription[] allRefreshBundles = new BundleDescription[] { a, b, c, d, e, f };
		StateDelta stateDelta = state.resolve(new BundleDescription[] { a });
		BundleDelta[] bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 6, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : allRefreshBundles) {
			boolean found = false;
			for (int j = 0; j < bundleDeltas.length && !found; j++) {
				assertEquals("unexpected delta type " + bundleDeltas[j], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[j].getType());
				found = bundleDeltas[j].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}
		stateDelta = state.resolve(new BundleDescription[] { f });
		bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 1, bundleDeltas.length); //$NON-NLS-1$

		stateDelta = state.resolve(new BundleDescription[] { z });
		bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 1, bundleDeltas.length); //$NON-NLS-1$
	}

	@Test
	public void testSubstitutableExports012() throws BundleException {
		State state = getSubstituteUsesCycleState();
		state.resolve();
		BundleDescription a = state.getBundle(1);
		BundleDescription b = state.getBundle(2);
		BundleDescription c = state.getBundle(3);
		BundleDescription d = state.getBundle(4);
		BundleDescription e = state.getBundle(5);
		BundleDescription f = state.getBundle(6);
		BundleDescription g = state.getBundle(7);
		BundleDescription h = state.getBundle(8);

		BundleDescription[] allRefreshBundles = new BundleDescription[] { a, b, c, d, e, f, g, h };
		StateDelta stateDelta = state.resolve(new BundleDescription[] { a });
		BundleDelta[] bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 8, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : allRefreshBundles) {
			boolean found = false;
			for (int j = 0; j < bundleDeltas.length && !found; j++) {
				assertEquals("unexpected delta type " + bundleDeltas[j], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[j].getType());
				found = bundleDeltas[j].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}
		stateDelta = state.resolve(new BundleDescription[] { f });
		bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 1, bundleDeltas.length); //$NON-NLS-1$

		stateDelta = state.resolve(new BundleDescription[] { c });
		bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 8, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : allRefreshBundles) {
			boolean found = false;
			for (int j = 0; j < bundleDeltas.length && !found; j++) {
				assertEquals("unexpected delta type " + bundleDeltas[j], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[j].getType());
				found = bundleDeltas[j].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}
	}

	@Test
	public void testSubstitutableExports013() throws BundleException {
		State state = getSubstituteBasicFragState();
		state.resolve();

		BundleDescription a = state.getBundle(0);
		BundleDescription aFrag = state.getBundle(1);
		BundleDescription b = state.getBundle(2);
		BundleDescription bFrag = state.getBundle(3);
		BundleDescription c = state.getBundle(4);
		BundleDescription cFrag = state.getBundle(5);
		BundleDescription d = state.getBundle(6);
		BundleDescription e = state.getBundle(7);
		BundleDescription f = state.getBundle(8);

		BundleDescription[] allBundles = new BundleDescription[] { a, aFrag, b, bFrag, c, cFrag, d, e, f };
		StateDelta stateDelta = state.resolve(new BundleDescription[] { a });
		BundleDelta[] bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 9, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : allBundles) {
			boolean found = false;
			for (int j = 0; j < bundleDeltas.length && !found; j++) {
				assertEquals("unexpected delta type " + bundleDeltas[j], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[j].getType());
				found = bundleDeltas[j].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}
		stateDelta = state.resolve(new BundleDescription[] { f });
		bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 1, bundleDeltas.length); //$NON-NLS-1$

		stateDelta = state.resolve(new BundleDescription[] { c });
		bundleDeltas = stateDelta.getChanges();
		BundleDescription[] expectedRefresh = new BundleDescription[] { c, cFrag, f };
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 3, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : expectedRefresh) {
			boolean found = false;
			for (int j = 0; j < bundleDeltas.length && !found; j++) {
				assertEquals("unexpected delta type " + bundleDeltas[j], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[j].getType());
				found = bundleDeltas[j].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}
	}

	@Test
	public void testSubstitutableExports014() throws BundleException {
		State state = getSubstituteUsesFragState();
		state.resolve();

		BundleDescription z = state.getBundle(0);
		BundleDescription a = state.getBundle(1);
		BundleDescription aFrag = state.getBundle(2);
		BundleDescription b = state.getBundle(3);
		BundleDescription bFrag = state.getBundle(4);
		BundleDescription c = state.getBundle(5);
		BundleDescription cFrag = state.getBundle(6);
		BundleDescription d = state.getBundle(7);
		BundleDescription e = state.getBundle(8);
		BundleDescription f = state.getBundle(9);
		// BundleDescription g = state.getBundle(10);

		BundleDescription[] allRefreshBundles = new BundleDescription[] { a, aFrag, b, bFrag, c, cFrag, d, e, f };
		StateDelta stateDelta = state.resolve(new BundleDescription[] { a });
		BundleDelta[] bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", allRefreshBundles.length, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : allRefreshBundles) {
			boolean found = false;
			for (int j = 0; j < bundleDeltas.length && !found; j++) {
				assertEquals("unexpected delta type " + bundleDeltas[j], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[j].getType());
				found = bundleDeltas[j].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}
		stateDelta = state.resolve(new BundleDescription[] { f });
		bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 1, bundleDeltas.length); //$NON-NLS-1$

		stateDelta = state.resolve(new BundleDescription[] { z });
		bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 1, bundleDeltas.length); //$NON-NLS-1$

		stateDelta = state.resolve(new BundleDescription[] { c });
		bundleDeltas = stateDelta.getChanges();
		BundleDescription[] expectedRefresh = new BundleDescription[] { c, cFrag, f };
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 3, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : expectedRefresh) {
			boolean found = false;
			for (int j = 0; j < bundleDeltas.length && !found; j++) {
				assertEquals("unexpected delta type " + bundleDeltas[j], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[j].getType());
				found = bundleDeltas[j].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}
	}

	@Test
	public void testSubstitutableExports015() throws BundleException {
		State state = getSubstituteUsesFragCycleState();
		state.resolve();
		// BundleDescription z = state.getBundle(0);
		BundleDescription a = state.getBundle(1);
		BundleDescription aFrag = state.getBundle(2);
		BundleDescription b = state.getBundle(3);
		BundleDescription bFrag = state.getBundle(4);
		BundleDescription c = state.getBundle(5);
		BundleDescription cFrag = state.getBundle(6);
		BundleDescription d = state.getBundle(7);
		BundleDescription e = state.getBundle(8);
		BundleDescription f = state.getBundle(9);
		BundleDescription g = state.getBundle(10);
		BundleDescription h = state.getBundle(11);
		BundleDescription i = state.getBundle(12);

		BundleDescription[] allRefreshBundles = new BundleDescription[] { a, aFrag, b, bFrag, c, cFrag, d, e, f, g, h,
				i };
		StateDelta stateDelta = state.resolve(new BundleDescription[] { a });
		BundleDelta[] bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", allRefreshBundles.length, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : allRefreshBundles) {
			boolean found = false;
			for (int k = 0; k < bundleDeltas.length && !found; k++) {
				assertEquals("unexpected delta type " + bundleDeltas[k], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[k].getType());
				found = bundleDeltas[k].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}
		stateDelta = state.resolve(new BundleDescription[] { f });
		bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 1, bundleDeltas.length); //$NON-NLS-1$

		stateDelta = state.resolve(new BundleDescription[] { c });
		bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", allRefreshBundles.length, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : allRefreshBundles) {
			boolean found = false;
			for (int k = 0; k < bundleDeltas.length && !found; k++) {
				assertEquals("unexpected delta type " + bundleDeltas[k], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[k].getType());
				found = bundleDeltas[k].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}
	}

	@Test
	public void testSubstitutableExports016() throws BundleException {
		State state = getSubstituteBasicReexportState();
		state.resolve();
		BundleDescription a = state.getBundle(0);
		BundleDescription b = state.getBundle(1);
		BundleDescription c = state.getBundle(2);
		BundleDescription d = state.getBundle(3);
		BundleDescription e = state.getBundle(4);
		BundleDescription f = state.getBundle(5);
		BundleDescription g = state.getBundle(6);
		BundleDescription h = state.getBundle(7);
		BundleDescription i = state.getBundle(8);

		BundleDescription[] allBundles = new BundleDescription[] { a, b, c, d, e, f, g, h, i };
		StateDelta stateDelta = state.resolve(new BundleDescription[] { a });
		BundleDelta[] bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", allBundles.length, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : allBundles) {
			boolean found = false;
			for (int k = 0; k < bundleDeltas.length && !found; k++) {
				assertEquals("unexpected delta type " + bundleDeltas[k], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[k].getType());
				found = bundleDeltas[k].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}

		stateDelta = state.resolve(new BundleDescription[] { f });
		bundleDeltas = stateDelta.getChanges();
		BundleDescription[] expectedRefresh = new BundleDescription[] { f, i };
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", expectedRefresh.length, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : expectedRefresh) {
			boolean found = false;
			for (int k = 0; k < bundleDeltas.length && !found; k++) {
				assertEquals("unexpected delta type " + bundleDeltas[k], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[k].getType());
				found = bundleDeltas[k].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}
		stateDelta = state.resolve(new BundleDescription[] { i });
		bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 1, bundleDeltas.length); //$NON-NLS-1$
	}

	@Test
	public void testSubstitutableExports017() throws BundleException {
		State state = getSubstituteUsesReexportState();
		state.resolve();
		BundleDescription z = state.getBundle(0);
		BundleDescription a = state.getBundle(1);
		BundleDescription b = state.getBundle(2);
		BundleDescription c = state.getBundle(3);
		BundleDescription d = state.getBundle(4);
		BundleDescription e = state.getBundle(5);
		BundleDescription f = state.getBundle(6);
		BundleDescription g = state.getBundle(7);
		BundleDescription h = state.getBundle(8);
		BundleDescription i = state.getBundle(9);
		// BundleDescription j = state.getBundle(10);

		BundleDescription[] allRefreshBundles = new BundleDescription[] { a, b, c, d, e, f, g, h, i };
		StateDelta stateDelta = state.resolve(new BundleDescription[] { a });
		BundleDelta[] bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", allRefreshBundles.length, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : allRefreshBundles) {
			boolean found = false;
			for (int m = 0; m < bundleDeltas.length && !found; m++) {
				assertEquals("unexpected delta type " + bundleDeltas[m], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[m].getType());
				found = bundleDeltas[m].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}

		stateDelta = state.resolve(new BundleDescription[] { f });
		bundleDeltas = stateDelta.getChanges();
		BundleDescription[] expectedRefresh = new BundleDescription[] { f, i };
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", expectedRefresh.length, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : expectedRefresh) {
			boolean found = false;
			for (int m = 0; m < bundleDeltas.length && !found; m++) {
				assertEquals("unexpected delta type " + bundleDeltas[m], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[m].getType());
				found = bundleDeltas[m].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}
		stateDelta = state.resolve(new BundleDescription[] { i });
		bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 1, bundleDeltas.length); //$NON-NLS-1$

		stateDelta = state.resolve(new BundleDescription[] { z });
		bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 1, bundleDeltas.length); //$NON-NLS-1$
	}

	@Test
	public void testSubstitutableExports018() throws BundleException {
		State state = getSubstituteUsesReexportCycleState();
		state.resolve();
		BundleDescription a = state.getBundle(1);
		BundleDescription b = state.getBundle(2);
		BundleDescription c = state.getBundle(3);
		BundleDescription d = state.getBundle(4);
		BundleDescription e = state.getBundle(5);
		BundleDescription f = state.getBundle(6);
		BundleDescription g = state.getBundle(7);
		BundleDescription h = state.getBundle(8);
		BundleDescription i = state.getBundle(9);
		BundleDescription j = state.getBundle(10);

		BundleDescription[] allRefreshBundles = new BundleDescription[] { a, b, c, d, e, f, g, h, i, j };
		StateDelta stateDelta = state.resolve(new BundleDescription[] { a });
		BundleDelta[] bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", allRefreshBundles.length, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : allRefreshBundles) {
			boolean found = false;
			for (int l = 0; l < bundleDeltas.length && !found; l++) {
				assertEquals("unexpected delta type " + bundleDeltas[l], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[l].getType());
				found = bundleDeltas[l].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}
		stateDelta = state.resolve(new BundleDescription[] { i });
		bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", 1, bundleDeltas.length); //$NON-NLS-1$

		stateDelta = state.resolve(new BundleDescription[] { c });
		bundleDeltas = stateDelta.getChanges();
		assertNotNull("bundleDeltas is null", bundleDeltas); //$NON-NLS-1$
		assertEquals("bunldeDeltas wrong number", allRefreshBundles.length, bundleDeltas.length); //$NON-NLS-1$
		for (BundleDescription description : allRefreshBundles) {
			boolean found = false;
			for (int l = 0; l < bundleDeltas.length && !found; l++) {
				assertEquals("unexpected delta type " + bundleDeltas[l], BundleDelta.RESOLVED, //$NON-NLS-1$
						bundleDeltas[l].getType());
				found = bundleDeltas[l].getBundle() == description;
			}
			if (!found) {
				fail("Did not find RESOLVED BundleDelta for " + description); //$NON-NLS-1$
			}
		}
	}

	@Test
	public void testSubstitutableExports019() throws BundleException {
		State state = getSubstituteUnresolvedFragState();
		state.resolve();

		BundleDescription a = state.getBundle(0);
		BundleDescription aFrag = state.getBundle(1);
		BundleDescription b = state.getBundle(2);
		BundleDescription bFrag = state.getBundle(3);
		BundleDescription c = state.getBundle(4);
		BundleDescription cFrag = state.getBundle(5);
		BundleDescription d = state.getBundle(6);
		BundleDescription e = state.getBundle(7);
		BundleDescription f = state.getBundle(8);

		assertTrue("1.0", a.isResolved()); //$NON-NLS-1$
		assertFalse("1.0.1", aFrag.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertFalse("1.1.1", bFrag.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertFalse("1.2.1", cFrag.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aVisible = state.getStateHelper().getVisiblePackages(a);
		ExportPackageDescription[] aFragVisible = state.getStateHelper().getVisiblePackages(aFrag);
		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] bFragVisible = state.getStateHelper().getVisiblePackages(bFrag);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] cFragVisible = state.getStateHelper().getVisiblePackages(cFrag);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);

		assertNotNull("aVisible is null", aVisible); //$NON-NLS-1$
		assertNotNull("aFragVisible is null", aFragVisible); //$NON-NLS-1$
		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("bFragVisible is null", bFragVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("cFragVisible is null", cFragVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$

		assertEquals("aVisible wrong number", 0, aVisible.length); //$NON-NLS-1$
		assertEquals("aFragVisible wrong number", 0, aFragVisible.length); //$NON-NLS-1$
		assertEquals("bVisible wrong number", 2, bVisible.length); //$NON-NLS-1$
		assertEquals("bFragVisible wrong number", 0, bFragVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 2, cVisible.length); //$NON-NLS-1$
		assertEquals("cFragVisible wrong number", 0, cFragVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 2, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 2, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 2, fVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] aExports = a.getSelectedExports();
		assertEquals("a has wrong number of exports", 2, aExports.length); //$NON-NLS-1$

		assertArrayEquals("bVisible not correct", aExports, bVisible); //$NON-NLS-1$
		assertArrayEquals("cVisible not correct", aExports, cVisible); //$NON-NLS-1$
		assertArrayEquals("dVisible not correct", aExports, dVisible); //$NON-NLS-1$
		assertArrayEquals("eVisible not correct", aExports, eVisible); //$NON-NLS-1$
		assertArrayEquals("fVisible not correct", aExports, fVisible); //$NON-NLS-1$
	}

	@Test
	public void testSubstitutableExports020() throws BundleException {
		State state = getSubstituteUnresolvedFragState();
		state.resolve();

		BundleDescription a = state.getBundle(0);
		BundleDescription aFrag = state.getBundle(1);
		BundleDescription b = state.getBundle(2);
		BundleDescription bFrag = state.getBundle(3);
		BundleDescription c = state.getBundle(4);
		BundleDescription cFrag = state.getBundle(5);
		BundleDescription d = state.getBundle(6);
		BundleDescription e = state.getBundle(7);
		BundleDescription f = state.getBundle(8);

		// add a bundle to resolve the fragments import of q
		Hashtable manifest = new Hashtable();
		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "Q"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "q; version=1.0"); //$NON-NLS-1$
		BundleDescription q = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				f.getBundleId() + 1);
		state.addBundle(q);
		state.resolve(new BundleDescription[] { a });

		assertTrue("1.0", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.0.1", aFrag.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.1.1", bFrag.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.2.1", cFrag.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aVisible = state.getStateHelper().getVisiblePackages(a);
		ExportPackageDescription[] aFragVisible = state.getStateHelper().getVisiblePackages(aFrag);
		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] bFragVisible = state.getStateHelper().getVisiblePackages(bFrag);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] cFragVisible = state.getStateHelper().getVisiblePackages(cFrag);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);

		assertNotNull("aVisible is null", aVisible); //$NON-NLS-1$
		assertNotNull("aFragVisible is null", aFragVisible); //$NON-NLS-1$
		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("bFragVisible is null", bFragVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("cFragVisible is null", cFragVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$

		assertEquals("aVisible wrong number", 0, aVisible.length); //$NON-NLS-1$
		assertEquals("aFragVisible wrong number", 1, aFragVisible.length); //$NON-NLS-1$
		assertEquals("bVisible wrong number", 2, bVisible.length); //$NON-NLS-1$
		assertEquals("bFragVisible wrong number", 2, bFragVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 2, cVisible.length); //$NON-NLS-1$
		assertEquals("cFragVisible wrong number", 2, cFragVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 3, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 3, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 3, fVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] aExports = a.getSelectedExports();
		assertEquals("a has wrong number of exports", 3, aExports.length); //$NON-NLS-1$

		ExportPackageDescription[] aDeclaredExports = a.getExportPackages();
		ExportPackageDescription[] aFragExpected = new ExportPackageDescription[] { aExports[2],
				q.getExportPackages()[0] };
		assertArrayEquals("aFragVisible not correct", q.getSelectedExports(), aFragVisible); //$NON-NLS-1$
		assertArrayEquals("bVisible not correct", aDeclaredExports, bVisible); //$NON-NLS-1$
		assertArrayEquals("bFragVisible not correct", aFragExpected, bFragVisible); //$NON-NLS-1$
		assertArrayEquals("cVisible not correct", aDeclaredExports, cVisible); //$NON-NLS-1$
		assertArrayEquals("cFragVisible not correct", aFragExpected, cFragVisible); //$NON-NLS-1$

		assertArrayEquals("dVisible not correct", aExports, dVisible); //$NON-NLS-1$
		assertArrayEquals("eVisible not correct", aExports, eVisible); //$NON-NLS-1$
		assertArrayEquals("fVisible not correct", aExports, fVisible); //$NON-NLS-1$
	}

	@Test
	public void testSubstitutableExports021() throws BundleException {
		State state = getSubstituteBasicState();
		state.resolve();
		BundleDescription a = state.getBundle(0);
		BundleDescription b = state.getBundle(1);
		BundleDescription c = state.getBundle(2);
		BundleDescription d = state.getBundle(3);
		BundleDescription e = state.getBundle(4);
		BundleDescription f = state.getBundle(5);

		state.removeBundle(a);
		state.resolve(new BundleDescription[] { a });
		assertFalse("1.0", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertFalse("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);

		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$

		assertEquals("bVisible wrong number", 0, bVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 2, cVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 0, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 2, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 2, fVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] bExports = b.getSelectedExports();
		assertArrayEquals("cVisible not correct", bExports, cVisible); //$NON-NLS-1$
		assertArrayEquals("eVisible not correct", bExports, eVisible); //$NON-NLS-1$
		assertArrayEquals("fVisible not correct", bExports, fVisible); //$NON-NLS-1$
	}

	@Test
	public void testSubstitutableExports022() throws BundleException {
		State state = getSubstituteSplitState();
		state.resolve();
		BundleDescription a = state.getBundle(0);
		BundleDescription b = state.getBundle(1);
		BundleDescription c = state.getBundle(2);
		BundleDescription d = state.getBundle(3);
		BundleDescription e = state.getBundle(4);
		BundleDescription f = state.getBundle(5);
		BundleDescription g = state.getBundle(6);
		BundleDescription h = state.getBundle(7);
		BundleDescription i = state.getBundle(8);
		BundleDescription j = state.getBundle(9);

		assertTrue("1.0", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$
		assertTrue("1.6", g.isResolved()); //$NON-NLS-1$
		assertTrue("1.7", h.isResolved()); //$NON-NLS-1$
		assertTrue("1.8", i.isResolved()); //$NON-NLS-1$
		assertTrue("1.9", j.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aVisible = state.getStateHelper().getVisiblePackages(a);
		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);
		ExportPackageDescription[] gVisible = state.getStateHelper().getVisiblePackages(g);
		ExportPackageDescription[] hVisible = state.getStateHelper().getVisiblePackages(h);
		ExportPackageDescription[] iVisible = state.getStateHelper().getVisiblePackages(i);
		ExportPackageDescription[] jVisible = state.getStateHelper().getVisiblePackages(j);

		assertNotNull("aVisible is null", aVisible); //$NON-NLS-1$
		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$
		assertNotNull("gVisible is null", gVisible); //$NON-NLS-1$
		assertNotNull("hVisible is null", hVisible); //$NON-NLS-1$
		assertNotNull("iVisible is null", iVisible); //$NON-NLS-1$
		assertNotNull("jVisible is null", jVisible); //$NON-NLS-1$

		assertEquals("aVisible wrong number", 0, aVisible.length); //$NON-NLS-1$
		assertEquals("bVisible wrong number", 2, bVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 2, cVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 2, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 4, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 4, fVisible.length); //$NON-NLS-1$
		assertEquals("gVisible wrong number", 4, gVisible.length); //$NON-NLS-1$
		assertEquals("hVisible wrong number", 4, hVisible.length); //$NON-NLS-1$
		assertEquals("iVisible wrong number", 4, iVisible.length); //$NON-NLS-1$
		assertEquals("jVisible wrong number", 4, jVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] aExports = a.getSelectedExports();
		assertArrayEquals("aVisible not correct", aExports, a.getExportPackages()); //$NON-NLS-1$
		assertArrayEquals("bVisible not correct", aExports, bVisible); //$NON-NLS-1$
		assertArrayEquals("cVisible not correct", aExports, cVisible); //$NON-NLS-1$
		assertArrayEquals("dVisible not correct", aExports, dVisible); //$NON-NLS-1$

		ExportPackageDescription[] aExported = a.getSelectedExports();
		ExportPackageDescription[] dExported = d.getSelectedExports();
		ExportPackageDescription[] expected = new ExportPackageDescription[aExported.length + dExported.length];
		System.arraycopy(aExported, 0, expected, 0, aExported.length);
		System.arraycopy(dExported, 0, expected, aExported.length, dExported.length);
		for (ExportPackageDescription exportDescription : expected) {
			assertContains("eVisible not correct", eVisible, exportDescription); //$NON-NLS-1$
			assertContains("fVisible not correct", fVisible, exportDescription); //$NON-NLS-1$
			assertContains("gVisible not correct", gVisible, exportDescription); //$NON-NLS-1$
			assertContains("hVisible not correct", hVisible, exportDescription); //$NON-NLS-1$
			assertContains("iVisible not correct", iVisible, exportDescription); //$NON-NLS-1$
			assertContains("jVisible not correct", jVisible, exportDescription); //$NON-NLS-1$
		}
	}

	@Test
	public void testSubstitutableExports023() throws BundleException {
		State state = getSubstituteSplitUsesState();
		state.resolve();
		BundleDescription a = state.getBundle(0);
		BundleDescription b = state.getBundle(1);
		BundleDescription c = state.getBundle(2);
		BundleDescription d = state.getBundle(3);
		BundleDescription e = state.getBundle(4);
		BundleDescription f = state.getBundle(5);
		BundleDescription g = state.getBundle(6);
		BundleDescription h = state.getBundle(7);
		BundleDescription i = state.getBundle(8);
		BundleDescription j = state.getBundle(9);
		BundleDescription k = state.getBundle(10);
		BundleDescription l = state.getBundle(11);
		BundleDescription m = state.getBundle(12);
		BundleDescription n = state.getBundle(13);

		assertTrue("1.0", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$
		assertTrue("1.6", g.isResolved()); //$NON-NLS-1$
		assertTrue("1.7", h.isResolved()); //$NON-NLS-1$
		assertTrue("1.8", i.isResolved()); //$NON-NLS-1$
		assertTrue("1.9", j.isResolved()); //$NON-NLS-1$
		assertTrue("1.9", k.isResolved()); //$NON-NLS-1$
		assertTrue("1.9", l.isResolved()); //$NON-NLS-1$
		assertTrue("1.9", j.isResolved()); //$NON-NLS-1$
		assertTrue("1.9", n.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aVisible = state.getStateHelper().getVisiblePackages(a);
		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);
		ExportPackageDescription[] gVisible = state.getStateHelper().getVisiblePackages(g);
		ExportPackageDescription[] hVisible = state.getStateHelper().getVisiblePackages(h);
		ExportPackageDescription[] iVisible = state.getStateHelper().getVisiblePackages(i);
		ExportPackageDescription[] jVisible = state.getStateHelper().getVisiblePackages(j);
		ExportPackageDescription[] kVisible = state.getStateHelper().getVisiblePackages(k);
		ExportPackageDescription[] lVisible = state.getStateHelper().getVisiblePackages(l);
		ExportPackageDescription[] mVisible = state.getStateHelper().getVisiblePackages(m);
		ExportPackageDescription[] nVisible = state.getStateHelper().getVisiblePackages(n);

		assertNotNull("aVisible is null", aVisible); //$NON-NLS-1$
		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$
		assertNotNull("gVisible is null", gVisible); //$NON-NLS-1$
		assertNotNull("hVisible is null", hVisible); //$NON-NLS-1$
		assertNotNull("iVisible is null", iVisible); //$NON-NLS-1$
		assertNotNull("jVisible is null", jVisible); //$NON-NLS-1$
		assertNotNull("kVisible is null", kVisible); //$NON-NLS-1$
		assertNotNull("lVisible is null", lVisible); //$NON-NLS-1$
		assertNotNull("mVisible is null", mVisible); //$NON-NLS-1$
		assertNotNull("nVisible is null", nVisible); //$NON-NLS-1$

		assertEquals("aVisible wrong number", 1, aVisible.length); //$NON-NLS-1$
		assertEquals("bVisible wrong number", 3, bVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 3, cVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 3, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 5, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 5, fVisible.length); //$NON-NLS-1$
		assertEquals("gVisible wrong number", 4, gVisible.length); //$NON-NLS-1$
		assertEquals("hVisible wrong number", 4, hVisible.length); //$NON-NLS-1$
		assertEquals("iVisible wrong number", 4, iVisible.length); //$NON-NLS-1$
		assertEquals("jVisible wrong number", 4, jVisible.length); //$NON-NLS-1$
		assertEquals("kVisible wrong number", 0, kVisible.length); //$NON-NLS-1$
		assertEquals("lVisible wrong number", 0, lVisible.length); //$NON-NLS-1$
		assertEquals("mVisible wrong number", 6, mVisible.length); //$NON-NLS-1$
		assertEquals("nVisible wrong number", 6, nVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] aExports = a.getSelectedExports();
		assertEquals("aExports wrong number", 2, aExports.length); //$NON-NLS-1$
		ExportPackageDescription[] bcExpected = new ExportPackageDescription[] { aExports[0], aExports[1],
				l.getSelectedExports()[0] };
		ExportPackageDescription[] aExpected = new ExportPackageDescription[] { l.getSelectedExports()[0] };
		assertArrayEquals("aVisible not correct", aExpected, aVisible); //$NON-NLS-1$
		assertArrayEquals("bVisible not correct", bcExpected, bVisible); //$NON-NLS-1$
		assertArrayEquals("cVisible not correct", bcExpected, cVisible); //$NON-NLS-1$

		ExportPackageDescription[] dExpected = new ExportPackageDescription[] { l.getSelectedExports()[1], aExports[0],
				aExports[1] };
		assertArrayEquals("dVisible not correct", dExpected, dVisible); //$NON-NLS-1$

		ExportPackageDescription[] aExported = a.getSelectedExports();
		ExportPackageDescription[] dExported = d.getSelectedExports();
		ExportPackageDescription[] efExpected = new ExportPackageDescription[aExported.length + dExported.length + 1];
		System.arraycopy(aExported, 0, efExpected, 0, aExported.length);
		System.arraycopy(dExported, 0, efExpected, aExported.length, dExported.length);
		efExpected[aExported.length + dExported.length] = l.getSelectedExports()[1];
		for (ExportPackageDescription efExport : efExpected) {
			assertContains("eVisible not correct", eVisible, efExport); //$NON-NLS-1$
			assertContains("fVisible not correct", fVisible, efExport); //$NON-NLS-1$
		}

		ExportPackageDescription[] ghijExpected = new ExportPackageDescription[aExported.length + dExported.length];
		System.arraycopy(aExported, 0, ghijExpected, 0, aExported.length);
		System.arraycopy(dExported, 0, ghijExpected, aExported.length, dExported.length);
		for (ExportPackageDescription ghijExport : ghijExpected) {
			assertContains("gVisible not correct", gVisible, ghijExport); //$NON-NLS-1$
			assertContains("hVisible not correct", hVisible, ghijExport); //$NON-NLS-1$
			assertContains("iVisible not correct", iVisible, ghijExport); //$NON-NLS-1$
			assertContains("jVisible not correct", jVisible, ghijExport); //$NON-NLS-1$
		}

		ExportPackageDescription[] lExported = l.getSelectedExports();
		ExportPackageDescription[] mnExpected = new ExportPackageDescription[aExported.length + dExported.length
				+ lExported.length];
		System.arraycopy(aExported, 0, mnExpected, 0, aExported.length);
		System.arraycopy(dExported, 0, mnExpected, aExported.length, dExported.length);
		System.arraycopy(lExported, 0, mnExpected, aExported.length + dExported.length, lExported.length);
		for (ExportPackageDescription mnExport : mnExpected) {
			assertContains("mVisible not correct", mVisible, mnExport); //$NON-NLS-1$
			assertContains("nVisible not correct", nVisible, mnExport); //$NON-NLS-1$
		}
	}

	@Test
	public void testSubstitutableExports024() throws BundleException {
		State state = getNonOverlapingSubstituteBasicState();
		state.resolve();
		BundleDescription a = state.getBundle(0);
		BundleDescription b = state.getBundle(1);
		BundleDescription c = state.getBundle(2);
		BundleDescription d = state.getBundle(3);
		BundleDescription e = state.getBundle(4);
		BundleDescription f = state.getBundle(5);

		assertTrue("1.0", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aVisible = state.getStateHelper().getVisiblePackages(a);
		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);

		assertNotNull("aVisible is null", aVisible); //$NON-NLS-1$
		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$

		assertEquals("aVisible wrong number", 0, aVisible.length); //$NON-NLS-1$
		assertEquals("bVisible wrong number", 2, bVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 2, cVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 2, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 2, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 2, fVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] aExports = a.getSelectedExports();
		assertArrayEquals("aVisible not correct", aExports, a.getExportPackages()); //$NON-NLS-1$
		assertArrayEquals("bVisible not correct", aExports, bVisible); //$NON-NLS-1$
		assertArrayEquals("cVisible not correct", aExports, cVisible); //$NON-NLS-1$
		assertArrayEquals("dVisible not correct", aExports, dVisible); //$NON-NLS-1$
		assertArrayEquals("eVisible not correct", aExports, eVisible); //$NON-NLS-1$
		assertArrayEquals("fVisible not correct", aExports, fVisible); //$NON-NLS-1$

		VersionConstraint[] unsatisfied = state.getStateHelper().getUnsatisfiedConstraints(a);
		assertEquals("Should not have any unresolvable constraints", 0, unsatisfied.length);
	}

	@Test
	public void testSubstitutableExports025() throws BundleException {
		State state = getSubstituteUsesState();
		BundleDescription a = state.getBundle(1);
		BundleDescription b = state.getBundle(2);
		BundleDescription c = state.getBundle(3);
		BundleDescription d = state.getBundle(4);
		BundleDescription e = state.getBundle(5);
		BundleDescription f = state.getBundle(6);
		BundleDescription g = state.getBundle(7);

		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "H"); //$NON-NLS-1$
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0"); //$NON-NLS-1$
		manifest.put(Constants.REQUIRE_BUNDLE, "C"); //$NON-NLS-1$
		manifest.put(Constants.EXPORT_PACKAGE, "z; version=0.5"); //$NON-NLS-1$
		manifest.put(Constants.IMPORT_PACKAGE, "z"); //$NON-NLS-1$
		BundleDescription h = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + (String) manifest.get(Constants.BUNDLE_VERSION),
				g.getBundleId() + 1);
		state.addBundle(h);

		state.resolve();

		assertTrue("1.0", a.isResolved()); //$NON-NLS-1$
		assertTrue("1.1", b.isResolved()); //$NON-NLS-1$
		assertTrue("1.2", c.isResolved()); //$NON-NLS-1$
		assertTrue("1.3", d.isResolved()); //$NON-NLS-1$
		assertTrue("1.4", e.isResolved()); //$NON-NLS-1$
		assertTrue("1.5", f.isResolved()); //$NON-NLS-1$
		assertFalse("1.6", g.isResolved()); //$NON-NLS-1$
		assertTrue("1.7", h.isResolved()); //$NON-NLS-1$

		ExportPackageDescription[] aVisible = state.getStateHelper().getVisiblePackages(a);
		ExportPackageDescription[] bVisible = state.getStateHelper().getVisiblePackages(b);
		ExportPackageDescription[] cVisible = state.getStateHelper().getVisiblePackages(c);
		ExportPackageDescription[] dVisible = state.getStateHelper().getVisiblePackages(d);
		ExportPackageDescription[] eVisible = state.getStateHelper().getVisiblePackages(e);
		ExportPackageDescription[] fVisible = state.getStateHelper().getVisiblePackages(f);
		ExportPackageDescription[] gVisible = state.getStateHelper().getVisiblePackages(g);
		ExportPackageDescription[] hVisible = state.getStateHelper().getVisiblePackages(h);

		assertNotNull("aVisible is null", aVisible); //$NON-NLS-1$
		assertNotNull("bVisible is null", bVisible); //$NON-NLS-1$
		assertNotNull("cVisible is null", cVisible); //$NON-NLS-1$
		assertNotNull("dVisible is null", dVisible); //$NON-NLS-1$
		assertNotNull("eVisible is null", eVisible); //$NON-NLS-1$
		assertNotNull("fVisible is null", fVisible); //$NON-NLS-1$
		assertNotNull("gVisible is null", gVisible); //$NON-NLS-1$
		assertNotNull("hVisible is null", hVisible); //$NON-NLS-1$

		assertEquals("aVisible wrong number", 0, aVisible.length); //$NON-NLS-1$
		assertEquals("bVisible wrong number", 2, bVisible.length); //$NON-NLS-1$
		assertEquals("cVisible wrong number", 2, cVisible.length); //$NON-NLS-1$
		assertEquals("dVisible wrong number", 2, dVisible.length); //$NON-NLS-1$
		assertEquals("eVisible wrong number", 2, eVisible.length); //$NON-NLS-1$
		assertEquals("fVisible wrong number", 2, fVisible.length); //$NON-NLS-1$
		assertEquals("gVisible wrong number", 0, gVisible.length); //$NON-NLS-1$
		assertEquals("hVisible wrong number", 2, hVisible.length); //$NON-NLS-1$

		ExportPackageDescription[] aExports = a.getSelectedExports();
		assertArrayEquals("aVisible not correct", aExports, a.getExportPackages()); //$NON-NLS-1$
		assertArrayEquals("bVisible not correct", aExports, bVisible); //$NON-NLS-1$
		assertArrayEquals("cVisible not correct", aExports, cVisible); //$NON-NLS-1$
		assertArrayEquals("dVisible not correct", aExports, dVisible); //$NON-NLS-1$
		assertArrayEquals("eVisible not correct", aExports, eVisible); //$NON-NLS-1$
		assertArrayEquals("fVisible not correct", aExports, fVisible); //$NON-NLS-1$
		assertArrayEquals("hVisible not correct", aExports, hVisible); //$NON-NLS-1$

		ExportPackageDescription[] hExported = h.getSelectedExports();
		assertEquals("Expected one export", 1, hExported.length);
		ExportPackageDescription[] hSubstituted = h.getSubstitutedExports();
		assertEquals("Expected no substitutions", 0, hSubstituted.length);

		ImportPackageSpecification[] hImports = h.getImportPackages();
		assertEquals("Expected one import", 1, hImports.length);
		assertEquals("Wrong supplier", hExported[0], hImports[0].getSupplier());
	}
}
