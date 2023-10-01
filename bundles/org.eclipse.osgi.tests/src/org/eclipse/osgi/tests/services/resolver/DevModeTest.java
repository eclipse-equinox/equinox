/*******************************************************************************
 * Copyright (c) 2007, 2022 IBM Corporation and others.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.State;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class DevModeTest extends AbstractStateTest {

	private State buildDevModeState() {
		State state = buildEmptyState();
		Dictionary[] props = new Dictionary[] { new Hashtable() };
		props[0].put("osgi.resolverMode", "development");
		state.setPlatformProperties(props);
		return state;
	}

	@Test
	public void testDevModeDomino02() throws BundleException {
		State state = buildDevModeState();

		int bundleID = 0;
		Hashtable manifest = new Hashtable();

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "a");
		manifest.put(Constants.IMPORT_PACKAGE, "x, d");
		manifest.put(Constants.REQUIRE_BUNDLE, "X, E");
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "A");
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "A");
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "D");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "d");
		BundleDescription d = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "E");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "e");
		BundleDescription e = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.addBundle(d);
		state.addBundle(e);
		state.resolve();
		// bundle a has an unsatisfied constraint
		// b, c, d, e should still be resolved though
		assertFalse("0.1", a.isResolved());
		assertTrue("0.3", b.isResolved());
		assertTrue("0.4", c.isResolved());
		assertTrue("0.5", d.isResolved());
		assertTrue("0.5", e.isResolved());

		BundleDescription[] aRequired = a.getResolvedRequires();
		assertTrue("1.1", aRequired.length == 1);
		assertTrue("1.2", aRequired[0] == e);
		ExportPackageDescription[] aImported = a.getResolvedImports();
		assertTrue("1.3", aImported.length == 1);
		assertTrue("1.4", aImported[0].getExporter() == d);

		BundleDescription[] bRequired = b.getResolvedRequires();
		assertTrue("2.1", bRequired.length == 1);
		assertTrue("2.2", bRequired[0] == a);

		BundleDescription[] cRequired = c.getResolvedRequires();
		assertTrue("3.1", cRequired.length == 1);
		assertTrue("3.2", cRequired[0] == a);
	}

	@Test
	public void testDevModeDomino01() throws BundleException {
		State state = buildDevModeState();

		int bundleID = 0;
		Hashtable manifest = new Hashtable();

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "a");
		manifest.put(Constants.IMPORT_PACKAGE, "x");
		manifest.put(Constants.REQUIRE_BUNDLE, "X");
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "A");
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "A");
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(a);
		state.addBundle(b);
		state.addBundle(c);
		state.resolve();
		// bundle a has an unsatisfied constraint
		// b and c should still be resolved to bundle a though
		assertFalse("0.1", a.isResolved());
		assertTrue("0.3", b.isResolved());
		assertTrue("0.4", c.isResolved());

		BundleDescription[] bRequired = b.getResolvedRequires();
		assertTrue("1.1", bRequired.length == 1);
		assertTrue("1.2", bRequired[0] == a);

		BundleDescription[] cRequired = c.getResolvedRequires();
		assertTrue("2.1", cRequired.length == 1);
		assertTrue("2.2", cRequired[0] == a);
	}

	@Test
	public void testDevModeFragment01() throws BundleException {
		State state = buildDevModeState();

		int bundleID = 0;
		Hashtable manifest = new Hashtable();

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "a");
		manifest.put(Constants.IMPORT_PACKAGE, "c");
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "AFrag");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.FRAGMENT_HOST, "A");
		manifest.put(Constants.EXPORT_PACKAGE, "a.frag");
		BundleDescription aFrag = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.IMPORT_PACKAGE, "a, a.frag");
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "c");
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(a);
		state.addBundle(aFrag);
		state.addBundle(b);
		state.resolve();
		// only A should not be resolved, do not want the domino effect.
		assertFalse("0.1", a.isResolved());
		assertTrue("0.2", aFrag.isResolved());
		assertTrue("0.3", b.isResolved());

		ExportPackageDescription[] aExports = a.getSelectedExports();
		ExportPackageDescription[] bImports = b.getResolvedImports();
		assertTrue("1.1", aExports.length == 2);
		assertTrue("1.2", bImports.length == 2);
		assertTrue("1.3", aExports[0] == bImports[0]);
		assertTrue("1.4", aExports[1] == bImports[1]);
		assertTrue("1.5", aFrag.getHost().getSupplier() == a);

		state.addBundle(c);
		state.resolve();
		assertTrue("2.1", a.isResolved());
		assertTrue("2.2", aFrag.isResolved());
		assertTrue("2.3", b.isResolved());
		assertTrue("2.4", c.isResolved());

		aExports = a.getSelectedExports();
		bImports = b.getResolvedImports();
		assertTrue("3.1", aExports.length == 2);
		assertTrue("3.2", bImports.length == 2);
		assertTrue("3.3", aExports[0] == bImports[0]);
		assertTrue("3.4", aExports[1] == bImports[1]);
		assertTrue("3.5", aFrag.getHost().getSupplier() == a);
	}

	@Test
	public void testDevModeSingleton01() throws BundleException {
		State state = buildDevModeState();

		int bundleID = 0;
		Hashtable manifest = new Hashtable();

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A; singleton:=true");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "a");
		BundleDescription a1 = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A; singleton:=true");
		manifest.put(Constants.BUNDLE_VERSION, "2.0");
		manifest.put(Constants.EXPORT_PACKAGE, "a");
		BundleDescription a2 = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "A; bundle-version=2.0");
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "A; bundle-version=\"[1.0,2.0)\"");
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(a1);
		state.addBundle(a2);
		state.addBundle(b);
		state.addBundle(c);
		state.resolve();
		// both versions of A should be resolved
		assertTrue("0.1", a1.isResolved());
		assertTrue("0.2", a2.isResolved());
		assertTrue("0.3", b.isResolved());
		assertTrue("0.4", c.isResolved());

		BundleDescription[] bRequired = b.getResolvedRequires();
		assertTrue("1.1", bRequired.length == 1);
		assertTrue("1.2", bRequired[0] == a2);

		BundleDescription[] cRequired = c.getResolvedRequires();
		assertTrue("2.1", cRequired.length == 1);
		assertTrue("2.2", cRequired[0] == a1);
	}

	@Test
	public void testDevModeSingleton02() throws BundleException {
		State state = buildDevModeState();

		int bundleID = 0;
		Hashtable manifest = new Hashtable();

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A; singleton:=true");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.EXPORT_PACKAGE, "a");
		manifest.put(Constants.IMPORT_PACKAGE, "x");
		BundleDescription a1 = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A; singleton:=true");
		manifest.put(Constants.BUNDLE_VERSION, "2.0");
		manifest.put(Constants.EXPORT_PACKAGE, "a");
		BundleDescription a2 = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "A; bundle-version=2.0");
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "C");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_BUNDLE, "A; bundle-version=\"[1.0,2.0)\"");
		BundleDescription c = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(a1);
		state.addBundle(a2);
		state.addBundle(b);
		state.addBundle(c);
		state.resolve();
		// only a2 is resolved because a1 has an unsatisfied constraint
		// c should still be resolved to a1 though
		assertFalse("0.1", a1.isResolved());
		assertTrue("0.2", a2.isResolved());
		assertTrue("0.3", b.isResolved());
		assertTrue("0.4", c.isResolved());

		BundleDescription[] bRequired = b.getResolvedRequires();
		assertTrue("1.1", bRequired.length == 1);
		assertTrue("1.2", bRequired[0] == a2);

		BundleDescription[] cRequired = c.getResolvedRequires();
		assertTrue("2.1", cRequired.length == 1);
		assertTrue("2.2", cRequired[0] == a1);
	}

	@Test
	public void testDevModeGenericCapability() throws BundleException {
		State state = buildDevModeState();

		int bundleID = 0;
		Hashtable manifest = new Hashtable();

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "A; singleton:=true");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.REQUIRE_CAPABILITY,
				"osgi.service; filter:=\"(objectClass=foo.Bar)\";\n" + "  effective:=\"active\"");
		BundleDescription a = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(a);
		state.resolve();
		// a can't be resolved since its required capability is not provided

		assertFalse("0.1", a.isResolved());

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "B; singleton:=true");
		manifest.put(Constants.BUNDLE_VERSION, "1.0");
		manifest.put(Constants.PROVIDE_CAPABILITY, "osgi.service; objectClass:List<String>=\"foo.Bar\"");
		BundleDescription b = state.getFactory().createBundleDescription(state, manifest,
				(String) manifest.get(Constants.BUNDLE_SYMBOLICNAME) + "_"
						+ (String) manifest.get(Constants.BUNDLE_VERSION),
				bundleID++);

		state.addBundle(b);
		state.resolve();
		// with bundle B, A is resolvable now
		assertTrue("0.2", a.isResolved());
	}

}
