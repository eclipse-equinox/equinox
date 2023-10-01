/*******************************************************************************
 * Copyright (c) 2018 Inno-Tec Innovative Technologies GmbH. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Inno-Tec (Juergen Bogdahn) - Fix for Bug 388055
 *
 *******************************************************************************/
package org.eclipse.equinox.internal.security.tests.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.security.Provider;
import java.security.Security;
import java.util.*;
import org.eclipse.equinox.internal.security.storage.JavaEncryption;
import org.junit.Test;

/**
 * In this test the number of PBE ciphers in the VM are determined and compared
 * to the available ciphers in the secure storage implementation.
 *
 * This test is only interested in PBE Ciphers for the secure storage.
 *
 * !IMPORTANT! It can only pass if crypto.policy is set to 'unlimited'
 * !IMPORTANT!
 *
 * The test "rebuilds" the logic of searching for keyFactories and matching PBE
 * ciphers. For these ciphers the "roundtrip" has to be successful, otherwise
 * the SecureStorage implementation does not use the cipher correctly and it
 * cannot be used.
 */
public class DetectPBECiphersTest {

	@Test
	public void testPBEDetect() {
		int cipherJVMCount = 0;
		Set<String> keyFactories = new HashSet<>();
		Provider[] providers = Security.getProviders();
		for (Provider p : providers) { // find all key factories
			for (Provider.Service service : p.getServices()) {
				// skip properties like "[Cipher.ABC SupportedPaddings]")
				if (service.getType().equals("SecretKeyFactory") && service.getAlgorithm().indexOf(' ') == -1) {
					keyFactories.add(service.getAlgorithm());
				}
			}
		}
		for (Provider p : providers) { // find all ciphers matching a key factory and start with PBE
			for (Provider.Service service : p.getServices()) {
				if (service.getType().equals("Cipher") && service.getAlgorithm().startsWith("PBE")
						&& keyFactories.contains(service.getAlgorithm())) {
					cipherJVMCount++;
				}
			}
		}

		JavaEncryption encryption = new JavaEncryption();
		HashMap<String, String> detectedCiphers = encryption.detect();

		assertTrue(!detectedCiphers.isEmpty());
		assertEquals(cipherJVMCount, detectedCiphers.size());
	}
}
