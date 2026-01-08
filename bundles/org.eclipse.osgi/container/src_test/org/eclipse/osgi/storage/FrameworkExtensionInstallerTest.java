/*******************************************************************************
 * Copyright (c) 2013, 2026 IBM Corporation and others.
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
package org.eclipse.osgi.storage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleRevisionBuilder.GenericInfo;
import org.eclipse.osgi.container.ModuleRevisions;
import org.eclipse.osgi.internal.container.NamespaceList;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;

/**
 * Test for issue 1229: ClassLoading from embedded jars inside extension bundles
 * is broken.
 * 
 * The problem manifests when FrameworkExtensionInstaller.addExtensionContent is
 * called. Inside method addExtensionContent0 the extension files are added to
 * the classloader by calling appendToClassPathForInstrumentation via
 * reflection. When line setAccessible in findMethod is removed the
 * appendToClassPathForInstrumentation method reference gets set but later on we
 * get an IllegalAccessException in addExtensionContent0. I haven't analyzed why
 * the call is functioning in previous versions.
 * 
 * This test implementation is far from nice with all the reflection and
 * inherited classes. But i didn't find a better solution to trigger the problem
 * at its core. From my perspective this test doesn't have to land in the main
 * codebase.
 */
@SuppressWarnings("nls")
public class FrameworkExtensionInstallerTest {

	private static FrameworkExtensionInstaller extensionInstaller;

	@BeforeClass
	public static void createExtensionInstaller() throws Exception {
		Constructor<EquinoxConfiguration> constr;
		constr = EquinoxConfiguration.class.getDeclaredConstructor(Map.class, HookRegistry.class);
		constr.setAccessible(true);
		EquinoxConfiguration cfg = constr.newInstance(new HashMap<>(), new HookRegistry(null));
		extensionInstaller = new FrameworkExtensionInstaller(cfg);
	}

	@Test
	public void testAddExtensionContent() throws Exception {
		Constructor<ModuleRevision> constr;
		constr = ModuleRevision.class.getDeclaredConstructor(String.class, // symbolicName
				Version.class, // version
				int.class, // types
				NamespaceList.Builder.class, // capabilityInfos
				NamespaceList.Builder.class, // requirementInfos
				ModuleRevisions.class, // revisions
				Object.class); // revisionInfo
		constr.setAccessible(true);

		NamespaceList.Builder<GenericInfo> emptyInfos = NamespaceList.empty(new Function<GenericInfo, String>() {
			public String apply(GenericInfo genericInfo) {
				return null;
			}
		}).createBuilder();

		ModuleRevisions revisions = new ModuleEx().getRevisions();

		BundleInfo bundleInfo = new BundleInfo( /* Storage storage */ null, /* long bundleId */ 1,
				/* String location */ null, /* long nextGenerationId */ 1);
		Generation gen = bundleInfo.createGeneration();
		Field field = Generation.class.getDeclaredField("bundleFile");
		field.setAccessible(true);
		field.set(gen, new BundleFileEx());

		ModuleRevision moduleRevision = constr.newInstance("test", new Version(1, 0, 0),
				BundleRevision.TYPE_FRAGMENT, emptyInfos, emptyInfos, revisions, gen);
		extensionInstaller.addExtensionContent(Collections.singleton(moduleRevision), null);
	}

	static class ModuleEx extends org.eclipse.osgi.container.Module {
		public ModuleEx() {
			super(1L, // id
					"test", // location
					null, // container
					null, // settings
					1 // startlevel
			);
		}

		@Override
		public Bundle getBundle() {
			return null;
		}

		@Override
		protected void cleanup(ModuleRevision revision) {
			//
		}
	}

	static class BundleFileEx extends BundleFile {
		public BundleFileEx() {
			super(new File("./test"));
		}

		@Override
		public void close() throws IOException {
			//
		}

		@Override
		public boolean containsDir(String dir) {
			return false;
		}

		@Override
		public BundleEntry getEntry(String path) {
			return null;
		}

		@Override
		public Enumeration<String> getEntryPaths(String path, boolean recurse) {
			return null;
		}

		@Override
		public File getFile(String path, boolean nativeCode) {
			return new File(path);
		}

		@Override
		public void open() throws IOException {
			//
		}
	}
}
