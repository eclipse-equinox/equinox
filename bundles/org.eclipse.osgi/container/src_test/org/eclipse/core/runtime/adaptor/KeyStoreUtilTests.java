/*******************************************************************************
 * Copyright (c) 2025 SAP SE and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assume.assumeThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
import org.eclipse.osgi.service.environment.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SuppressWarnings("nls")
public class KeyStoreUtilTests {

	private static final List<String> SYSTEM_PROPERTIES_TO_BACKUP_AND_RESTORE = Arrays.asList( //
			"eclipse.load.os.trust.store.by.default", //
			"javax.net.ssl.trustStore", //
			"javax.net.ssl.trustStorePassword", //
			"javax.net.ssl.trustStoreProvider", //
			"javax.net.ssl.trustStoreType", //
			"javax.net.ssl.keyStore", //
			"javax.net.ssl.keyStorePassword", //
			"javax.net.ssl.keyStoreProvider", //
			"javax.net.ssl.keyStoreType");

	@Rule
	public TemporaryFolder tempDir = new TemporaryFolder();

	private String os;
	private Map<String, String> systemPropertyBackups = new HashMap<>();
	private SSLContext previousSslContext;

	@Before
	public void setup() throws Exception {
		for (String property : SYSTEM_PROPERTIES_TO_BACKUP_AND_RESTORE) {
			systemPropertyBackups.put(property, System.getProperty(property, null));
		}
		previousSslContext = SSLContext.getDefault();

		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.contains("win")) {
			this.os = Constants.OS_WIN32;
		} else if (osName.contains("mac")) {
			this.os = Constants.OS_MACOSX;
		} else if (osName.contains("nux") || osName.contains("nix")) {
			this.os = Constants.OS_LINUX;
		} else {
			this.os = Constants.OS_UNKNOWN;
		}
	}

	@After
	public void teardown() {
		systemPropertyBackups.forEach((property, backupValue) -> {
			if (backupValue == null) {
				System.clearProperty(property);
			} else {
				System.setProperty(property, backupValue);
			}
		});
		SSLContext.setDefault(previousSslContext);
	}

	@Test
	public void loadTrustManagers_Default() throws Exception {

		TestSpecificKeyStoreUtil keyStoreUtil = new TestSpecificKeyStoreUtil(os);

		keyStoreUtil.setUpSslContext();

		assertThat(SSLContext.getDefault(), is(keyStoreUtil.recordedSslContext));

		assertThat(keyStoreUtil.recordedTrustManagers, arrayWithSize(1));
		assertThat(keyStoreUtil.recordedTrustManagers[0], instanceOf(CollectionTrustManager.class));
		assertThat(((CollectionTrustManager) keyStoreUtil.recordedTrustManagers[0]).getAcceptedIssuers(),
				not(emptyArray()));

		CollectionTrustManager tm = (CollectionTrustManager) keyStoreUtil.recordedTrustManagers[0];

		// jvm
		assertThat(tm.trustManagers, not(empty()));
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores, not(empty()));
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(0).manager, is(tm.trustManagers.get(0)));
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(0).store, is(nullValue()));
		assertThat(Arrays.stream(tm.trustManagers.get(0).getAcceptedIssuers())
				.map(X509Certificate::getSubjectX500Principal).map(X500Principal::getName).collect(Collectors.toList()),
				hasItem(matchesRegex("(?i).*digicert.*root.*")));

		if (Constants.OS_WIN32.equals(os)) {
			assertThat(tm.trustManagers, hasSize(2));
			assertThat(keyStoreUtil.createdTrustManagersAndKeyStores, hasSize(2));
			assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(1).manager, is(tm.trustManagers.get(1)));
			assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(1).store.getType(), is("Windows-ROOT"));
			assertThat(Arrays.stream(tm.trustManagers.get(1).getAcceptedIssuers())
					.map(X509Certificate::getSubjectX500Principal).map(X500Principal::getName)
					.collect(Collectors.toList()), hasItem(matchesRegex("(?i).*digicert.*root.*")));
		} else if (Constants.OS_MACOSX.equals(os)) {
			assertThat(tm.trustManagers, hasSize(2));
			assertThat(keyStoreUtil.createdTrustManagersAndKeyStores, hasSize(2));
			assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(1).manager, is(tm.trustManagers.get(1)));
			assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(1).store.getType(), is("KeychainStore"));
			// Apple KeychainStore only includes the 'System' certificates
			// (enterprise/admin managed)
			// but not the 'System Roots' ones (public CAs).
			// There's nothing guaranteed / deterministic in the 'System' on CI machines
			// that we could check for here...
		} else {
			assertThat(tm.trustManagers, hasSize(1));
			assertThat(keyStoreUtil.createdTrustManagersAndKeyStores, hasSize(1));
		}

		// no private keys
		assertThat(keyStoreUtil.recordedKeyManagers, emptyArray());
	}

	@Test
	public void loadTrustManagers_TrustSystemPropertiesPointToCustomTrustStore() throws Exception {

		System.setProperty("javax.net.ssl.trustStore", copyResourceToTempDirAndGetPath("truststore.jks"));
		System.setProperty("javax.net.ssl.trustStorePassword", "verysecret");

		TestSpecificKeyStoreUtil keyStoreUtil = new TestSpecificKeyStoreUtil(os);

		keyStoreUtil.setUpSslContext();

		assertThat(SSLContext.getDefault(), is(keyStoreUtil.recordedSslContext));

		assertThat(keyStoreUtil.recordedTrustManagers, arrayWithSize(1));
		assertThat(((CollectionTrustManager) keyStoreUtil.recordedTrustManagers[0]).getAcceptedIssuers(),
				not(emptyArray()));

		CollectionTrustManager tm = (CollectionTrustManager) keyStoreUtil.recordedTrustManagers[0];

		assertThat(tm.trustManagers, hasSize(1)); // only the properties-based store

		assertThat(Arrays.stream(tm.trustManagers.get(0).getAcceptedIssuers())
				.map(X509Certificate::getSubjectX500Principal).map(X500Principal::getName).collect(Collectors.toList()),
				hasItem("CN=Test,C=DE"));
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores, hasSize(1));
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(0).manager, is(tm.trustManagers.get(0)));
		// null caused KeyManagerFactory to load default system properties
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(0).store, is(nullValue()));

		assertThat(keyStoreUtil.recordedKeyManagers, emptyArray());
	}

	@Test
	public void loadTrustManagers_TrustSystemPropertiesPointToPlatformSpecificKeystore() throws Exception {
		assumeThat(os, in(Arrays.asList(Constants.OS_WIN32, Constants.OS_MACOSX)));
		if (Constants.OS_WIN32.equals(os)) {
			System.setProperty("javax.net.ssl.trustStore", "NONE");
			System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");
		} else if (Constants.OS_MACOSX.equals(os)) {
			System.setProperty("javax.net.ssl.trustStore", "NONE");
			System.setProperty("javax.net.ssl.trustStoreType", "KeychainStore");
			System.setProperty("javax.net.ssl.trustStoreProvider", "Apple");
		}

		TestSpecificKeyStoreUtil keyStoreUtil = new TestSpecificKeyStoreUtil(os);

		keyStoreUtil.setUpSslContext();

		assertThat(SSLContext.getDefault(), is(keyStoreUtil.recordedSslContext));

		assertThat(keyStoreUtil.recordedTrustManagers, arrayWithSize(1));

		CollectionTrustManager tm = (CollectionTrustManager) keyStoreUtil.recordedTrustManagers[0];

		assertThat(tm.trustManagers, hasSize(1)); // only the properties-based store
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores, hasSize(1));
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(0).manager, is(tm.trustManagers.get(0)));
		assertThat(keyStoreUtil.createdTrustManagersAndKeyStores.get(0).store, is(nullValue()));

		if (Constants.OS_WIN32.equals(os)) {
			assertThat(((CollectionTrustManager) keyStoreUtil.recordedTrustManagers[0]).getAcceptedIssuers(),
					not(emptyArray()));
			assertThat(Arrays.stream(tm.trustManagers.get(0).getAcceptedIssuers())
					.map(X509Certificate::getSubjectX500Principal).map(X500Principal::getName)
					.collect(Collectors.toList()), hasItem(matchesRegex("(?i).*digicert.*root.*")));
		} else if (Constants.OS_MACOSX.equals(os)) {
			// Apple KeychainStore only includes the 'System' certificates
			// (enterprise/admin managed)
			// but not the 'System Roots' ones (public CAs).
			// There's nothing guaranteed / deterministic in the 'System' on CI machines
			// that we could check for here...
		}
	}

	@Test
	public void loadKeyManagers_Default() throws Exception {

		TestSpecificKeyStoreUtil keyStoreUtil = new TestSpecificKeyStoreUtil(os);

		keyStoreUtil.setUpSslContext();

		assertThat(SSLContext.getDefault(), is(keyStoreUtil.recordedSslContext));

		assertThat(keyStoreUtil.recordedKeyManagers, emptyArray());
		assertThat(keyStoreUtil.createdKeyManagersAndKeyStores, hasSize(0));
	}

	@Test
	public void loadKeyManagers_KeySystemPropertiesPointToCustomKeyStore() throws Exception {

		System.setProperty("javax.net.ssl.keyStore", copyResourceToTempDirAndGetPath("keystore.p12"));
		System.setProperty("javax.net.ssl.keyStorePassword", "verysecret");
		System.setProperty("javax.net.ssl.keyStoreType", "PKCS12");

		TestSpecificKeyStoreUtil keyStoreUtil = new TestSpecificKeyStoreUtil(os);

		keyStoreUtil.setUpSslContext();

		assertThat(SSLContext.getDefault(), is(keyStoreUtil.recordedSslContext));

		assertThat(keyStoreUtil.recordedKeyManagers, arrayWithSize(1));
		assertThat(keyStoreUtil.recordedKeyManagers[0], instanceOf(X509KeyManager.class));

		X509KeyManager km = (X509KeyManager) keyStoreUtil.recordedKeyManagers[0];

		assertThat(keyStoreUtil.createdKeyManagersAndKeyStores, hasSize(1));
		assertThat(keyStoreUtil.createdKeyManagersAndKeyStores.get(0).manager, is(km));
		assertThat(keyStoreUtil.createdKeyManagersAndKeyStores.get(0).store.getType(), is("PKCS12"));

		assertThat(km.getPrivateKey("test.key"), not(nullValue()));
	}

	@Test
	public void optOutViaSystemProperty() throws Exception {
		System.setProperty("eclipse.load.os.trust.store.by.default", "false");

		TestSpecificKeyStoreUtil keyStoreUtil = new TestSpecificKeyStoreUtil(os);

		keyStoreUtil.setUpSslContext();

		assertThat(SSLContext.getDefault(), is(previousSslContext));
	}

	private String copyResourceToTempDirAndGetPath(String resourceName) throws IOException {
		File keyStoreFile = this.tempDir.newFile();
		Files.copy(getClass().getResourceAsStream(resourceName), keyStoreFile.toPath(),
				StandardCopyOption.REPLACE_EXISTING);
		return keyStoreFile.getAbsolutePath();
	}

	private static final class TestSpecificKeyStoreUtil extends KeyStoreUtil {

		public TestSpecificKeyStoreUtil(String os) {
			super(os);
		}

		public static class X509TrustManagerAndKeyStore {
			public final X509TrustManager manager;
			public final KeyStore store;

			public X509TrustManagerAndKeyStore(X509TrustManager manager, KeyStore store) {
				this.manager = manager;
				this.store = store;
			}
		}

		public static class X509KeyManagerAndKeyStore {
			public final X509KeyManager manager;
			public final KeyStore store;

			public X509KeyManagerAndKeyStore(X509KeyManager manager, KeyStore store) {
				this.manager = manager;
				this.store = store;
			}
		}

		public TrustManager[] recordedTrustManagers;
		public KeyManager[] recordedKeyManagers;
		public SSLContext recordedSslContext;
		public final List<X509TrustManagerAndKeyStore> createdTrustManagersAndKeyStores = new ArrayList<>();
		public final List<X509KeyManagerAndKeyStore> createdKeyManagersAndKeyStores = new ArrayList<>();

		@Override
		X509TrustManager createX509TrustManager(KeyStore keyStore) throws GeneralSecurityException {
			X509TrustManager manager = super.createX509TrustManager(keyStore);
			this.createdTrustManagersAndKeyStores.add(new X509TrustManagerAndKeyStore(manager, keyStore));
			return manager;
		}

		@Override
		X509KeyManager createX509KeyManager(KeyStore keyStore, char[] password) throws GeneralSecurityException {
			X509KeyManager manager = super.createX509KeyManager(keyStore, password);
			this.createdKeyManagersAndKeyStores.add(new X509KeyManagerAndKeyStore(manager, keyStore));
			return manager;
		}

		@Override
		void initSSLContext(SSLContext context, TrustManager[] trustManagers, KeyManager[] keyManagers,
				SecureRandom random) throws KeyManagementException {
			this.recordedTrustManagers = trustManagers;
			this.recordedKeyManagers = keyManagers;
			this.recordedSslContext = context;
		}
	}

}
