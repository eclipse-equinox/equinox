/*******************************************************************************
 * Copyright (c) 2024,2024 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - Initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.preferences.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.UserScope;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;

public class ScopeStorageLocationTest {

	@Test
	public void testInstanceScope_writeLocation() throws Exception {
		Path instanceLocation = getInstancePreferenceLocation();
		assertWriteLocation(InstanceScope.INSTANCE, instanceLocation);
	}

	@Test
	public void testInstanceScope_readLocation() throws Exception {
		Path instanceLocation = getInstancePreferenceLocation();
		assertReadLocation(InstanceScope.INSTANCE, instanceLocation);
	}

	private static Path getInstancePreferenceLocation() throws URISyntaxException {
		return Path.of(Platform.getInstanceLocation().getURL().toURI())
				.resolve(".metadata/.plugins/org.eclipse.core.runtime/");
	}

	@Test
	public void testConfigurationScope_writeLocation() throws Exception {
		Path configurationLocation = getConfigurationPreferenceLocation();
		assertWriteLocation(ConfigurationScope.INSTANCE, configurationLocation);
	}

	@Test
	public void testConfigurationScope_readLocation() throws Exception {
		Path configurationLocation = getConfigurationPreferenceLocation();
		assertReadLocation(ConfigurationScope.INSTANCE, configurationLocation);
	}

	private static Path getConfigurationPreferenceLocation() throws URISyntaxException {
		return Path.of(Platform.getConfigurationLocation().getURL().toURI());
	}

	@Test
	public void testUserScope_writeLocation() throws Exception {
		Path configurationLocation = getUserPreferenenceLocation();
		assertWriteLocation(UserScope.INSTANCE, configurationLocation);
	}

	@Test
	public void testUserScope_readLocation() throws Exception {
		Path configurationLocation = getUserPreferenenceLocation();
		assertReadLocation(UserScope.INSTANCE, configurationLocation);
	}

	private static Path getUserPreferenenceLocation() {
		return Path.of(System.getProperty("user.home") + "/.eclipse");
	}

	private static void assertWriteLocation(IScopeContext scope, Path expectedPreferenceLocation)
			throws BackingStoreException, IOException {
		Path expectedFileLocation = expectedPreferenceLocation.resolve(".settings/foo.bar.prefs");
		assertFalse(Files.exists(expectedFileLocation));
		try {
			IEclipsePreferences node = scope.getNode("foo.bar");
			node.putInt("someCount", 5);
			node.flush();
			String preferenceFileContent = Files.readString(expectedFileLocation);
			assertEquals("""
					eclipse.preferences.version=1
					someCount=5
					""".replace("\n", System.lineSeparator()), preferenceFileContent);
		} finally {
			Files.deleteIfExists(expectedFileLocation);
		}
	}

	private static void assertReadLocation(IScopeContext scope, Path expectedPreferenceLocation)
			throws BackingStoreException, IOException {
		Path expectedFileLocation = expectedPreferenceLocation.resolve(".settings/foo.bar.buzz.prefs");
		assertFalse(Files.exists(expectedFileLocation));
		try {
			Files.createDirectories(expectedFileLocation.getParent());
			Files.writeString(expectedFileLocation, """
					eclipse.preferences.version=1
					aSetting=HelloWorld
					""");
			assertEquals("HelloWorld", scope.getNode("foo.bar.buzz").get("aSetting", null));
		} finally {
			Files.deleteIfExists(expectedFileLocation);
		}

	}

}
