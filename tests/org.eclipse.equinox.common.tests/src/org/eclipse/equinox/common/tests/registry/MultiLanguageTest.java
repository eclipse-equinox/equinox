/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 458490
 *******************************************************************************/
package org.eclipse.equinox.common.tests.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Locale;
import org.eclipse.core.internal.registry.IRegistryConstants;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.osgi.service.localization.LocaleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Run with no NL argument or with "-nl en".
 */
public class MultiLanguageTest {

	class LocaleProviderTest implements LocaleProvider {
		public Locale currentLocale;

		@Override
		public Locale getLocale() {
			return currentLocale;
		}
	}

	private ServiceTracker<?, PackageAdmin> bundleTracker;

	private static String helloWorld = "Hello World";
	private static String helloWorldGerman = "Hallo Welt";
	private static String helloWorldItalian = "Ciao a tutti";
	private static String helloWorldFinnish = "Hei maailma";

	private static String catsAndDogs = "Cats and dogs";
	private static String catsAndDogsGerman = "Hunde und Katzen";
	private static String catsAndDogsItalian = "Cani e gatti";
	private static String catsAndDogsFinnish = "Kissat ja koirat";

	private static String eclipse = "eclipse";
	private static String eclipseGerman = "Eklipse";
	private static String eclipseItalian = "eclissi";
	private static String eclipseFinnish = "pimennys";

	private static String proverb = "Make haste slowly";
	private static String proverbLatin = "Festina lente";

	private Bundle bundle;
	private Bundle bundleFragment;
	private String oldMultiLangValue;
	private IPath tmpPath;
	private File registryLocation;

	private BundleContext testBundleContext;

	@Before
	public void setUp() throws Exception {
		testBundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
		bundle = BundleTestingHelper.installBundle("0.1", testBundleContext, "Plugin_Testing/registry/multiLang/bundleA");
		bundleFragment = BundleTestingHelper.installBundle("0.2", testBundleContext, "Plugin_Testing/registry/multiLang/fragmentA");
		getBundleAdmin().resolveBundles(new Bundle[] {bundle});

		// find a place for the extension registry cache
		tmpPath = FileSystemHelper.getRandomLocation(FileSystemHelper.getTempDir());
		registryLocation = tmpPath.append("testMulti").toFile();
		registryLocation.mkdirs();

		// switch environment to multi-language
		oldMultiLangValue = System.getProperty(IRegistryConstants.PROP_MULTI_LANGUAGE);
		System.setProperty(IRegistryConstants.PROP_MULTI_LANGUAGE, "true");
	}

	@After
	public void tearDown() throws Exception {
		// delete registry cache
		FileSystemHelper.clear(tmpPath.toFile());

		// remove test bundles
		bundleFragment.uninstall();
		bundle.uninstall();
		refreshPackages(new Bundle[] {bundle});

		// restore system environment
		if (oldMultiLangValue == null) {
			System.clearProperty(IRegistryConstants.PROP_MULTI_LANGUAGE);
		} else {
			System.setProperty(IRegistryConstants.PROP_MULTI_LANGUAGE, oldMultiLangValue);
		}

		if (bundleTracker != null) {
			bundleTracker.close();
			bundleTracker = null;
		}
	}

	private void refreshPackages(Bundle[] refresh) {
		final boolean[] flag = new boolean[] {false};
		FrameworkListener listener = event -> {
			if (event.getType() == FrameworkEvent.PACKAGES_REFRESHED) {
				synchronized (flag) {
					flag[0] = true;
					flag.notifyAll();
				}
			}
		};
		testBundleContext.addFrameworkListener(listener);

		try {
			getBundleAdmin().refreshPackages(refresh);
			synchronized (flag) {
				while (!flag[0]) {
					try {
						flag.wait(5000);
					} catch (InterruptedException e) {
						// do nothing
					}
				}
			}
		} finally {
			testBundleContext.removeFrameworkListener(listener);
		}
	}

	/**
	 * Tests APIs that take Locale as an argument.
	 */
	@Test
	public void testMultiLocale() {
		Object masterToken = new Object();
		// Create a multi-language extension registry
		File[] registryLocations = new File[] {registryLocation};
		boolean[] readOnly = new boolean[] {false};
		RegistryStrategy strategy = RegistryFactory.createOSGiStrategy(registryLocations, readOnly, masterToken);
		IExtensionRegistry localRegistry = RegistryFactory.createRegistry(strategy, masterToken, null);
		assertTrue(localRegistry.isMultiLanguage());

		// this is a direct test
		checkTranslations(localRegistry, false);

		// test cache
		localRegistry.stop(masterToken);
		IExtensionRegistry registryCached = RegistryFactory.createRegistry(strategy, masterToken, null);
		assertTrue(registryCached.isMultiLanguage());
		checkTranslations(registryCached, true);

		registryCached.stop(masterToken);
	}

	/**
	 * Tests APIs that use implicit default Locale.
	 */
	@Test
	public void testMultiLocaleService() {
		Object masterToken = new Object();
		// Create a multi-language extension registry
		File[] registryLocations = new File[] {registryLocation};
		boolean[] readOnly = new boolean[] {false};
		RegistryStrategy strategy = RegistryFactory.createOSGiStrategy(registryLocations, readOnly, masterToken);
		IExtensionRegistry localRegistry = RegistryFactory.createRegistry(strategy, masterToken, null);
		assertTrue(localRegistry.isMultiLanguage());

		// this is a direct test
		checkTranslationsService(localRegistry, false);

		// test cache
		localRegistry.stop(masterToken);
		IExtensionRegistry registryCached = RegistryFactory.createRegistry(strategy, masterToken, null);
		assertTrue(registryCached.isMultiLanguage());
		checkTranslationsService(registryCached, true);
		registryCached.stop(masterToken);
	}

	private void checkTranslationsService(IExtensionRegistry registry, boolean extended) {
		ServiceRegistration<LocaleProvider> registration = null;
		try {
			IExtensionPoint extPoint = registry.getExtensionPoint("org.eclipse.test.registryMulti.PointA");
			assertNotNull(extPoint);
			IExtension extension = registry.getExtension("org.eclipse.test.registryMulti.ExtA");
			assertNotNull(extension);
			IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.test.registryMulti", "PointA", "org.eclipse.test.registryMulti.ExtA");
			assertNotNull(elements);
			assertEquals(1, elements.length);
			IConfigurationElement element = elements[0];
			assertNotNull(element);
			IConfigurationElement[] sectionElements = element.getChildren("section");
			assertNotNull(sectionElements);
			assertEquals(1, sectionElements.length);
			IConfigurationElement[] subdivisionElements = sectionElements[0].getChildren("subdivision");
			assertNotNull(subdivisionElements);
			assertEquals(1, subdivisionElements.length);
			IConfigurationElement[] elementsValue = registry.getConfigurationElementsFor("org.eclipse.test.registryMulti", "PointValue", "org.eclipse.test.registryMulti.ExtValue");
			assertNotNull(elementsValue);
			assertEquals(1, elementsValue.length);
			IConfigurationElement elementValue = elementsValue[0];
			assertNotNull(elementValue);

			// default: no service registered
			assertEquals(helloWorld, extPoint.getLabel());
			assertEquals(catsAndDogs, extension.getLabel());
			assertEquals(helloWorld, element.getAttribute("name"));
			assertEquals(eclipse, subdivisionElements[0].getAttribute("division"));
			assertEquals(catsAndDogs, elementValue.getValue());

			// locale set to German
			LocaleProviderTest localeProvider = new LocaleProviderTest();
			registration = testBundleContext.registerService(LocaleProvider.class, localeProvider, null);
			localeProvider.currentLocale = new Locale("de_DE");
			assertEquals(helloWorldGerman, extPoint.getLabel());
			assertEquals(catsAndDogsGerman, extension.getLabel());
			assertEquals(helloWorldGerman, element.getAttribute("name"));
			assertEquals(eclipseGerman, subdivisionElements[0].getAttribute("division"));
			assertEquals(catsAndDogsGerman, elementValue.getValue());

			// locale changed to Italian
			localeProvider.currentLocale = new Locale("it_IT");
			assertEquals(catsAndDogsItalian, extension.getLabel());
			assertEquals(helloWorldItalian, extPoint.getLabel());
			assertEquals(helloWorldItalian, element.getAttribute("name"));
			assertEquals(eclipseItalian, subdivisionElements[0].getAttribute("division"));
			assertEquals(catsAndDogsItalian, elementValue.getValue());

			if (extended) { // check Finnish
				localeProvider.currentLocale = new Locale("fi_FI");
				assertEquals(catsAndDogsFinnish, extension.getLabel());
				assertEquals(helloWorldFinnish, extPoint.getLabel());
				assertEquals(helloWorldFinnish, element.getAttribute("name"));
				assertEquals(eclipseFinnish, subdivisionElements[0].getAttribute("division"));
				assertEquals(catsAndDogsFinnish, elementValue.getValue());
			}

			// unregister service - locale back to default
			registration.unregister();
			registration = null;
			assertEquals(helloWorld, extPoint.getLabel());
			assertEquals(catsAndDogs, extension.getLabel());
			assertEquals(helloWorld, element.getAttribute("name"));
			assertEquals(eclipse, subdivisionElements[0].getAttribute("division"));
			assertEquals(catsAndDogs, elementValue.getValue());
		} finally {
			if (registration != null) {
				registration.unregister();
			}
		}
	}

	private void checkTranslations(IExtensionRegistry registry, boolean extended) {
		IExtensionPoint extPoint = registry.getExtensionPoint("org.eclipse.test.registryMulti.PointA");
		assertNotNull(extPoint);
		IExtension extension = registry.getExtension("org.eclipse.test.registryMulti.ExtA");
		assertNotNull(extension);
		IConfigurationElement[] elements = registry.getConfigurationElementsFor("org.eclipse.test.registryMulti", "PointA", "org.eclipse.test.registryMulti.ExtA");
		assertNotNull(elements);
		assertEquals(1, elements.length);
		IConfigurationElement element = elements[0];
		assertNotNull(element);
		IConfigurationElement[] sectionElements = element.getChildren("section");
		assertNotNull(sectionElements);
		assertEquals(1, sectionElements.length);
		IConfigurationElement[] subdivisionElements = sectionElements[0].getChildren("subdivision");
		assertNotNull(subdivisionElements);
		assertEquals(1, subdivisionElements.length);
		IConfigurationElement[] elementsValue = registry.getConfigurationElementsFor("org.eclipse.test.registryMulti", "PointValue", "org.eclipse.test.registryMulti.ExtValue");
		assertNotNull(elementsValue);
		assertEquals(1, elementsValue.length);
		IConfigurationElement elementValue = elementsValue[0];
		assertNotNull(elementValue);
		IConfigurationElement[] elementsFrag = registry.getConfigurationElementsFor("org.eclipse.test.registryMulti", "FragmentPointA", "org.eclipse.test.registryMulti.FragmentExtA");
		assertNotNull(elementsFrag);
		assertEquals(1, elementsFrag.length);
		IConfigurationElement elementFrag = elementsFrag[0];
		assertNotNull(elementFrag);

		assertEquals(helloWorldGerman, extPoint.getLabel("de_DE"));
		assertEquals(helloWorldItalian, extPoint.getLabel("it"));
		assertEquals(helloWorld, extPoint.getLabel());

		assertEquals(catsAndDogsGerman, extension.getLabel("de_DE"));
		assertEquals(catsAndDogsItalian, extension.getLabel("it"));
		assertEquals(catsAndDogs, extension.getLabel());

		assertEquals(helloWorldGerman, element.getAttribute("name", "de_DE"));
		assertEquals(helloWorldGerman, element.getAttribute("name", "de_DE")); // check internal cache

		assertEquals(helloWorldItalian, element.getAttribute("name", "it"));
		assertEquals(helloWorldItalian, element.getAttribute("name", "it")); // check internal cache

		assertEquals(helloWorld, element.getAttribute("name", "some_OtherABC"));
		assertEquals(helloWorld, element.getAttribute("name")); // "default" locale

		assertEquals(eclipseGerman, subdivisionElements[0].getAttribute("division", "de_DE"));
		assertEquals(eclipseItalian, subdivisionElements[0].getAttribute("division", "it"));
		assertEquals(eclipse, subdivisionElements[0].getAttribute("division", "some_OtherABC"));

		assertEquals(catsAndDogsGerman, elementValue.getValue("de_DE"));
		assertEquals(catsAndDogsGerman, elementValue.getValue("de_DE")); // check internal cache

		assertEquals(catsAndDogsItalian, elementValue.getValue("it"));
		assertEquals(catsAndDogsItalian, elementValue.getValue("it")); // check internal cache

		assertEquals(catsAndDogs, elementValue.getValue("some_OtherABC"));
		assertEquals(catsAndDogs, elementValue.getValue());

		assertEquals(proverbLatin, elementFrag.getAttribute("name", "la_LA"));
		assertEquals(proverbLatin, elementFrag.getAttribute("name", "la_LA")); // check internal cache
		assertEquals(proverb, elementFrag.getAttribute("name", "some_OtherABC"));

		if (!extended) {
			return;
		}

		assertEquals(helloWorldFinnish, extPoint.getLabel("fi_FI"));
		assertEquals(catsAndDogsFinnish, extension.getLabel("fi_FI"));
		assertEquals(helloWorldFinnish, element.getAttribute("name", "fi_FI"));
		assertEquals(helloWorldFinnish, element.getAttribute("name", "fi_FI")); // check internal cache
		assertEquals(eclipseFinnish, subdivisionElements[0].getAttribute("division", "fi_FI"));
		assertEquals(catsAndDogsFinnish, elementValue.getValue("fi_FI"));
		assertEquals(catsAndDogsFinnish, elementValue.getValue("fi_FI")); // check internal cache
	}

	/*
	 * Return the package admin service, if available.
	 */
	private PackageAdmin getBundleAdmin() {
		if (bundleTracker == null) {
			bundleTracker = new ServiceTracker<>(testBundleContext, PackageAdmin.class, null);
			bundleTracker.open();
		}
		return bundleTracker.getService();
	}
}
