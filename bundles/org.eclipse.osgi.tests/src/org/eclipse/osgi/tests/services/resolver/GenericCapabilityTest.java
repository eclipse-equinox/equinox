/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.services.resolver;

import java.util.Dictionary;
import java.util.Hashtable;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class GenericCapabilityTest extends AbstractStateTest {
	private static final String GENERIC_REQUIRE = "Eclipse-GenericRequire"; //$NON-NLS-1$
	private static final String GENERIC_CAPABILITY = "Eclipse-GenericCapability"; //$NON-NLS-1$

	public static Test suite() {
		return new TestSuite(GenericCapabilityTest.class);
	}

	public GenericCapabilityTest(String name) {
		super(name);
	}

	public void testGenericsBasics() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericCapablity");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		StringBuffer capabililty = new StringBuffer();
		capabililty.append("foo; version=\"1.3.1\"; attr1=\"value1\"; attr2=\"value2\",");
		capabililty.append("bar:bartype; version=\"1.4.1\"; attr1=\"value1\"; attr2=\"value2\",");
		capabililty.append("test.types:testtype;");
		capabililty.append(" aVersion:version=\"2.0.0\";");
		capabililty.append(" aLong:long=\"10000000000\";");
		capabililty.append(" aDouble:double=\"1.000109\";");
		capabililty.append(" aUri:uri=\"file:/test\";");
		capabililty.append(" aSet:set=\"a,b,c,d\";");
		capabililty.append(" aString:string=\"someString\"");
		manifest.put(GENERIC_CAPABILITY, capabililty.toString());
		BundleDescription genCap = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericRequire");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		StringBuffer required = new StringBuffer();
		required.append("foo; selection-filter=\"(version>=1.3.0)\",");
		required.append("bar:bartype; selection-filter=\"(attr1=value1)\",");
		required.append("test.types:testtype; selection-filter=\"(&(aVersion>=2.0.0)(aLong>=5555)(aDouble>=1.00)(aUri=file:/test)(aSet=c)(aString=someString))\"");
		manifest.put(GENERIC_REQUIRE, required.toString());
		BundleDescription genReq = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(genCap);
		state.addBundle(genReq);

		state.resolve();
		assertTrue("1.0", genCap.isResolved());
		assertTrue("1.1", genReq.isResolved());
		GenericSpecification[] genSpecs = genReq.getGenericRequires();
		assertTrue("2.0", genSpecs.length == 3);
		assertTrue("2.1", genSpecs[0].isResolved());
		assertEquals("2.1.1", genSpecs[0].getSupplier(), genCap.getGenericCapabilities()[0]);
		assertTrue("2.2", genSpecs[1].isResolved());
		assertEquals("2.2.1", genSpecs[1].getSupplier(), genCap.getGenericCapabilities()[1]);
		assertTrue("2.3", genSpecs[2].isResolved());
		assertEquals("2.3.1", genSpecs[2].getSupplier(), genCap.getGenericCapabilities()[2]);
	}

	public void testGenericsUpdate() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericCapablity");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		StringBuffer capabililty = new StringBuffer();
		capabililty.append("foo; version=\"1.3.1\"; attr1=\"value1\"; attr2=\"value2\",");
		capabililty.append("bar:bartype; version=\"1.4.1\"; attr1=\"value1\"; attr2=\"value2\",");
		capabililty.append("test.types:testtype;");
		capabililty.append(" aVersion:version=\"2.0.0\";");
		capabililty.append(" aLong:long=\"10000000000\";");
		capabililty.append(" aDouble:double=\"1.000109\";");
		capabililty.append(" aUri:uri=\"file:/test\";");
		capabililty.append(" aSet:set=\"a,b,c,d\";");
		capabililty.append(" aString:string=\"someString\"");
		manifest.put(GENERIC_CAPABILITY, capabililty.toString());
		BundleDescription genCap = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericRequire");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		StringBuffer required = new StringBuffer();
		required.append("foo; selection-filter=\"(version>=1.3.0)\",");
		required.append("bar:bartype; selection-filter=\"(attr1=value1)\",");
		required.append("test.types:testtype; selection-filter=\"(&(aVersion>=2.0.0)(aLong>=5555)(aDouble>=1.00)(aUri=file:/test)(aSet=c)(aString=someString))\"");
		manifest.put(GENERIC_REQUIRE, required.toString());
		BundleDescription genReq = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(genCap);
		state.addBundle(genReq);

		state.resolve();
		assertTrue("1.0", genCap.isResolved());
		assertTrue("1.1", genReq.isResolved());
		GenericSpecification[] genSpecs = genReq.getGenericRequires();
		assertTrue("2.0", genSpecs.length == 3);
		assertTrue("2.1", genSpecs[0].isResolved());
		assertEquals("2.1.1", genSpecs[0].getSupplier(), genCap.getGenericCapabilities()[0]);
		assertTrue("2.2", genSpecs[1].isResolved());
		assertEquals("2.2.1", genSpecs[1].getSupplier(), genCap.getGenericCapabilities()[1]);
		assertTrue("2.3", genSpecs[2].isResolved());
		assertEquals("2.3.1", genSpecs[2].getSupplier(), genCap.getGenericCapabilities()[2]);

		manifest.clear();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericCapablity");
		manifest.put(Constants.BUNDLE_VERSION, "2.0.0");
		capabililty = new StringBuffer();
		capabililty.append("foo; version=\"1.3.2\"; attr1=\"value1\"; attr2=\"value2\",");
		capabililty.append("bar:bartype; version=\"1.4.2\"; attr1=\"value1\"; attr2=\"value2\",");
		capabililty.append("test.types:testtype;");
		capabililty.append(" aVersion:version=\"2.0.1\";");
		capabililty.append(" aLong:long=\"10000000000\";");
		capabililty.append(" aDouble:double=\"1.000109\";");
		capabililty.append(" aUri:uri=\"file:/test\";");
		capabililty.append(" aSet:set=\"a,b,c,d\";");
		capabililty.append(" aString:string=\"someString\"");
		manifest.put(GENERIC_CAPABILITY, capabililty.toString());
		BundleDescription genCap2 = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), genCap.getBundleId());

		state.updateBundle(genCap2);
		state.resolve(new BundleDescription[] {genCap2});

		assertTrue("3.0", genCap2.isResolved());
		assertTrue("3.1", genReq.isResolved());
		genSpecs = genReq.getGenericRequires();
		assertTrue("4.0", genSpecs.length == 3);
		assertTrue("4.1", genSpecs[0].isResolved());
		assertEquals("4.1.1", genSpecs[0].getSupplier(), genCap2.getGenericCapabilities()[0]);
		assertTrue("4.2", genSpecs[1].isResolved());
		assertEquals("4.2.1", genSpecs[1].getSupplier(), genCap2.getGenericCapabilities()[1]);
		assertTrue("4.3", genSpecs[2].isResolved());
		assertEquals("4.3.1", genSpecs[2].getSupplier(), genCap2.getGenericCapabilities()[2]);

	}

	public void testGenericsRefresh() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericCapablity");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		StringBuffer capabililty = new StringBuffer();
		capabililty.append("foo; version=\"1.3.1\"; attr1=\"value1\"; attr2=\"value2\",");
		capabililty.append("bar:bartype; version=\"1.4.1\"; attr1=\"value1\"; attr2=\"value2\",");
		capabililty.append("test.types:testtype;");
		capabililty.append(" aVersion:version=\"2.0.0\";");
		capabililty.append(" aLong:long=\"10000000000\";");
		capabililty.append(" aDouble:double=\"1.000109\";");
		capabililty.append(" aUri:uri=\"file:/test\";");
		capabililty.append(" aSet:set=\"a,b,c,d\";");
		capabililty.append(" aString:string=\"someString\"");
		manifest.put(GENERIC_CAPABILITY, capabililty.toString());
		BundleDescription genCap = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericRequire");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		StringBuffer required = new StringBuffer();
		required.append("foo; selection-filter=\"(version>=1.3.0)\",");
		required.append("bar:bartype; selection-filter=\"(attr1=value1)\",");
		required.append("test.types:testtype; selection-filter=\"(&(aVersion>=2.0.0)(aLong>=5555)(aDouble>=1.00)(aUri=file:/test)(aSet=c)(aString=someString))\"");
		manifest.put(GENERIC_REQUIRE, required.toString());
		BundleDescription genReq = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(genCap);
		state.addBundle(genReq);

		state.resolve();
		assertTrue("1.0", genCap.isResolved());
		assertTrue("1.1", genReq.isResolved());
		GenericSpecification[] genSpecs = genReq.getGenericRequires();
		assertTrue("2.0", genSpecs.length == 3);
		assertTrue("2.1", genSpecs[0].isResolved());
		assertEquals("2.1.1", genSpecs[0].getSupplier(), genCap.getGenericCapabilities()[0]);
		assertTrue("2.2", genSpecs[1].isResolved());
		assertEquals("2.2.1", genSpecs[1].getSupplier(), genCap.getGenericCapabilities()[1]);
		assertTrue("2.3", genSpecs[2].isResolved());
		assertEquals("2.3.1", genSpecs[2].getSupplier(), genCap.getGenericCapabilities()[2]);

		state.resolve(new BundleDescription[] {genCap});

		assertTrue("3.0", genCap.isResolved());
		assertTrue("3.1", genReq.isResolved());
		genSpecs = genReq.getGenericRequires();
		assertTrue("4.0", genSpecs.length == 3);
		assertTrue("4.1", genSpecs[0].isResolved());
		assertEquals("4.1.1", genSpecs[0].getSupplier(), genCap.getGenericCapabilities()[0]);
		assertTrue("4.2", genSpecs[1].isResolved());
		assertEquals("4.2.1", genSpecs[1].getSupplier(), genCap.getGenericCapabilities()[1]);
		assertTrue("4.3", genSpecs[2].isResolved());
		assertEquals("4.3.1", genSpecs[2].getSupplier(), genCap.getGenericCapabilities()[2]);

	}

	public void testGenericsFrags() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericCapablity");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		StringBuffer capabililty = new StringBuffer();
		capabililty.append("foo; version=\"1.3.1\"; attr1=\"value1\"; attr2=\"value2\",");
		capabililty.append("bar:bartype; version=\"1.4.1\"; attr1=\"value1\"; attr2=\"value2\",");
		capabililty.append("test.types:testtype;");
		capabililty.append(" aVersion:version=\"2.0.0\";");
		capabililty.append(" aLong:long=\"10000000000\";");
		capabililty.append(" aDouble:double=\"1.000109\";");
		capabililty.append(" aUri:uri=\"file:/test\";");
		capabililty.append(" aSet:set=\"a,b,c,d\";");
		capabililty.append(" aString:string=\"someString\"");
		manifest.put(GENERIC_CAPABILITY, capabililty.toString());
		BundleDescription genCap = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericCapability.frag1");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.FRAGMENT_HOST, "genericCapablity;bundle-version=\"[1.0.0,2.0.0)\"");
		capabililty = new StringBuffer();
		capabililty.append("fragmentStuff");
		manifest.put(GENERIC_CAPABILITY, capabililty.toString());
		BundleDescription genCapFrag = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericRequire");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		StringBuffer required = new StringBuffer();
		required.append("foo; selection-filter=\"(version>=1.3.0)\",");
		required.append("bar:bartype; selection-filter=\"(attr1=value1)\",");
		required.append("test.types:testtype; selection-filter=\"(&(aVersion>=2.0.0)(aLong>=5555)(aDouble>=1.00)(aUri=file:/test)(aSet=c)(aString=someString))\",");
		required.append("fragmentStuff");
		manifest.put(GENERIC_REQUIRE, required.toString());
		BundleDescription genReq = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(genCap);
		state.addBundle(genCapFrag);
		state.addBundle(genReq);

		state.resolve();
		assertTrue("1.0", genCap.isResolved());
		assertTrue("1.1", genReq.isResolved());
		assertTrue("1.2", genCapFrag.isResolved());
		GenericSpecification[] genSpecs = genReq.getGenericRequires();
		GenericDescription[] selectedCapabilities = genCap.getSelectedGenericCapabilities();
		assertTrue("2.0", genSpecs.length == 4);
		assertEquals("Wrong number of selected capabilities", 4, selectedCapabilities.length);
		assertTrue("2.1", genSpecs[0].isResolved());
		assertEquals("2.1.1", genSpecs[0].getSupplier(), selectedCapabilities[0]);
		assertTrue("2.2", genSpecs[1].isResolved());
		assertEquals("2.2.1", genSpecs[1].getSupplier(), selectedCapabilities[1]);
		assertTrue("2.3", genSpecs[2].isResolved());
		assertEquals("2.3.1", genSpecs[2].getSupplier(), selectedCapabilities[2]);
		assertTrue("2.4", genSpecs[3].isResolved());
		assertEquals("2.4.1", genSpecs[3].getSupplier(), selectedCapabilities[3]);
	}

	public void testGenericsIntraFrags() throws BundleException {
		State state = buildEmptyState();
		long bundleID = 0;

		Hashtable manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.equinox.generic.frag.a");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.FRAGMENT_HOST, "org.eclipse.equinox.generic.host;bundle-version=\"1.0.0\"");
		manifest.put("Eclipse-GenericCapability", "frag.a");
		BundleDescription genFragA = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.equinox.generic.host");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		BundleDescription genHost = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "org.eclipse.equinox.generic.frag.b");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.FRAGMENT_HOST, "org.eclipse.equinox.generic.host;bundle-version=\"1.0.0\"");
		manifest.put("Eclipse-GenericRequire", "frag.a");
		BundleDescription genFragB = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(genHost);
		state.addBundle(genFragA);
		state.addBundle(genFragB);

		state.resolve();
		assertTrue("1.0", genHost.isResolved());
		assertTrue("1.1", genFragA.isResolved());
		assertTrue("1.2", genFragB.isResolved());
		GenericSpecification[] genSpecs = genFragB.getGenericRequires();
		GenericDescription[] selected = genHost.getSelectedGenericCapabilities();
		assertEquals("Wrong number of selected", 1, selected.length);
		assertTrue("2.0", genSpecs.length == 1);
		assertTrue("2.1", genSpecs[0].isResolved());
		assertEquals("2.1.1", genSpecs[0].getSupplier(), selected[0]);
	}

	public void testGenericsAliases() throws BundleException {
		State state = buildEmptyState();
		Dictionary[] allPlatProps = state.getPlatformProperties();
		Dictionary platProps = (Dictionary) ((Hashtable) allPlatProps[0]).clone();
		platProps.put("osgi.genericAliases", "Export-Service:Import-Service:service,TJW-Export:TJW-Import:tjw");
		state.setPlatformProperties(platProps);

		Hashtable manifest = new Hashtable();
		long bundleID = 0;
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericCapablity");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.EXPORT_SERVICE, "org.osgi.service.log.LogService; version=1.2");
		manifest.put("TJW-Export", "my.great.stuff; aLong:long=5150; aDouble:double=3.14; aVersion:version=1.2.0");
		BundleDescription genCap = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericRequire");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.IMPORT_SERVICE, "org.osgi.service.log.LogService; selection-filter=(version>=1.0.0)");
		manifest.put("TJW-Import", "my.great.stuff; selection-filter=(&(aLong<=10000)(aLong>=5000))");
		BundleDescription genReq = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(genCap);
		state.addBundle(genReq);

		state.resolve();
		assertTrue("1.0", genCap.isResolved());
		assertTrue("1.1", genReq.isResolved());
		GenericSpecification[] genSpecs = genReq.getGenericRequires();
		assertTrue("2.0", genSpecs.length == 2);
		assertTrue("2.1", genSpecs[0].isResolved());
		assertEquals("2.1.1", genSpecs[0].getSupplier(), genCap.getGenericCapabilities()[0]);
		assertTrue("2.2", genSpecs[1].isResolved());
		assertEquals("2.2.1", genSpecs[1].getSupplier(), genCap.getGenericCapabilities()[1]);
	}

	public void testGenericsOptionalMultiple() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericCapablity");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		StringBuffer capabililty = new StringBuffer();
		capabililty.append("foo; version=\"1.3.1\"; attr1=\"value1\"; attr2=\"value2\",");
		capabililty.append("bar:bartype; version=\"1.4.1\"; attr1=\"value1\"; attr2=\"value2\",");
		capabililty.append("test.types:testtype;");
		capabililty.append(" aVersion:version=\"2.0.0\";");
		capabililty.append(" aLong:long=\"10000000000\";");
		capabililty.append(" aDouble:double=\"1.000109\";");
		capabililty.append(" aUri:uri=\"file:/test\";");
		capabililty.append(" aSet:set=\"a,b,c,d\";");
		capabililty.append(" aString:string=\"someString\",");
		capabililty.append("test.real.optional:thisisoptional,");
		capabililty.append("test.real.multiple:thisismultiple; version=1.0,");
		capabililty.append("test.real.multiple:thisismultiple; version=2.0");
		manifest.put(GENERIC_CAPABILITY, capabililty.toString());
		BundleDescription genCap = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericRequire");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		StringBuffer required = new StringBuffer();
		required.append("foo; selection-filter=\"(version>=1.3.0)\",");
		required.append("bar:bartype; selection-filter=\"(attr1=value1)\",");
		required.append("test.types:testtype; selection-filter=\"(&(aVersion>=2.0.0)(aLong>=5555)(aDouble>=1.00)(aUri=file:/test)(aSet=c)(aString=someString))\",");
		required.append("test.optional:thisisoptional; optional=true,");
		required.append("test.real.optional:thisisoptional; optional=true,");
		required.append("test.real.multiple:thisismultiple; multiple=true");
		manifest.put(GENERIC_REQUIRE, required.toString());
		BundleDescription genReq = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(genCap);
		state.addBundle(genReq);

		state.resolve();
		assertTrue("1.0", genCap.isResolved());
		assertTrue("1.1", genReq.isResolved());
		GenericSpecification[] genSpecs = genReq.getGenericRequires();
		assertTrue("2.0", genSpecs.length == 6);
		assertTrue("2.1", genSpecs[0].isResolved());
		assertEquals("2.1.1", genSpecs[0].getSupplier(), genCap.getGenericCapabilities()[0]);
		assertTrue("2.2", genSpecs[1].isResolved());
		assertEquals("2.2.1", genSpecs[1].getSupplier(), genCap.getGenericCapabilities()[1]);
		assertTrue("2.3", genSpecs[2].isResolved());
		assertEquals("2.3.1", genSpecs[2].getSupplier(), genCap.getGenericCapabilities()[2]);
		assertFalse("2.4", genSpecs[3].isResolved());
		assertTrue("2.5", genSpecs[4].isResolved());
		assertEquals("2.5.1", genSpecs[4].getSupplier(), genCap.getGenericCapabilities()[3]);
		assertTrue("2.6", genSpecs[5].isResolved());
		GenericDescription[] suppliers = genSpecs[5].getSuppliers();
		assertTrue("2.6.1", suppliers != null && suppliers.length == 2);
		assertEquals("2.6.2", suppliers[0], genCap.getGenericCapabilities()[5]);
		assertEquals("2.6.3", suppliers[1], genCap.getGenericCapabilities()[4]);
	}

	public void testGenericsCycles() throws BundleException {
		State state = buildEmptyState();
		Hashtable manifest = new Hashtable();
		long bundleID = 0;

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericCapablity");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		StringBuffer capabililty = new StringBuffer();
		capabililty.append("foo; version=\"1.3.1\"; attr1=\"value1\"; attr2=\"value2\",");
		capabililty.append("bar:bartype; version=\"1.4.1\"; attr1=\"value1\"; attr2=\"value2\",");
		capabililty.append("test.types:testtype;");
		capabililty.append(" aVersion:version=\"2.0.0\";");
		capabililty.append(" aLong:long=\"10000000000\";");
		capabililty.append(" aDouble:double=\"1.000109\";");
		capabililty.append(" aUri:uri=\"file:/test\";");
		capabililty.append(" aSet:set=\"a,b,c,d\";");
		capabililty.append(" aString:string=\"someString\"");
		manifest.put(GENERIC_CAPABILITY, capabililty.toString());
		StringBuffer required = new StringBuffer();
		required.append("foo:cycle; selection-filter=\"(version>=1.3.0)\"");
		manifest.put(GENERIC_REQUIRE, required.toString());
		BundleDescription genCap = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericCapability.frag1");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		manifest.put(Constants.FRAGMENT_HOST, "genericCapablity;bundle-version=\"[1.0.0,2.0.0)\"");
		capabililty = new StringBuffer();
		capabililty.append("fragmentStuff");
		manifest.put(GENERIC_CAPABILITY, capabililty.toString());
		BundleDescription genCapFrag = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		manifest = new Hashtable();
		manifest.put(Constants.BUNDLE_MANIFESTVERSION, "2");
		manifest.put(Constants.BUNDLE_SYMBOLICNAME, "genericRequire");
		manifest.put(Constants.BUNDLE_VERSION, "1.0.0");
		capabililty = new StringBuffer();
		capabililty.append("foo:cycle; version:version=\"2.0\"");
		manifest.put(GENERIC_CAPABILITY, capabililty.toString());
		required = new StringBuffer();
		required.append("foo; selection-filter=\"(version>=1.3.0)\",");
		required.append("bar:bartype; selection-filter=\"(attr1=value1)\",");
		required.append("test.types:testtype; selection-filter=\"(&(aVersion>=2.0.0)(aLong>=5555)(aDouble>=1.00)(aUri=file:/test)(aSet=c)(aString=someString))\",");
		required.append("fragmentStuff");
		manifest.put(GENERIC_REQUIRE, required.toString());
		BundleDescription genReq = state.getFactory().createBundleDescription(state, manifest, (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME), bundleID++);

		state.addBundle(genCap);
		state.addBundle(genCapFrag);
		state.addBundle(genReq);

		state.resolve();
		assertTrue("1.0", genCap.isResolved());
		assertTrue("1.1", genReq.isResolved());
		assertTrue("1.2", genCapFrag.isResolved());
		GenericSpecification[] genSpecs = genReq.getGenericRequires();
		GenericDescription[] selected = genCap.getSelectedGenericCapabilities();
		assertTrue("2.0", genSpecs.length == 4);
		assertEquals("Wrong number of selected", 4, selected.length);
		assertTrue("2.1", genSpecs[0].isResolved());
		assertEquals("2.1.1", genSpecs[0].getSupplier(), selected[0]);
		assertTrue("2.2", genSpecs[1].isResolved());
		assertEquals("2.2.1", genSpecs[1].getSupplier(), selected[1]);
		assertTrue("2.3", genSpecs[2].isResolved());
		assertEquals("2.3.1", genSpecs[2].getSupplier(), selected[2]);
		assertTrue("2.4", genSpecs[3].isResolved());
		assertEquals("2.4.1", genSpecs[3].getSupplier(), selected[3]);
		genSpecs = genCap.getGenericRequires();
		assertTrue("3.0", genSpecs.length == 1);
		assertTrue("3.1", genSpecs[0].isResolved());
		assertEquals("3.1.1", genSpecs[0].getSupplier(), genReq.getGenericCapabilities()[0]);
	}
}
