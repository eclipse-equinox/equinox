/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.storage;

import static org.junit.Assert.*;
import java.security.MessageDigest;

import org.junit.Test;

public class DigestUtilTests {

	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
	private static final String MD5 = "MD5";
	private static final String SHA1 = "SHA1";
	private static final String TEXT = "sometext";

	@Test
	public void testEncrypt() throws Exception {
		MessageDigest md = MessageDigest.getInstance(MD5);
		md.update(TEXT.getBytes());
		byte[] digest = md.digest();

		char[] chars = new char[2 * digest.length];
		for (int i = 0; i < digest.length; ++i) {
			chars[2 * i] = HEX_CHARS[(digest[i] & 0xF0) >>> 4];
			chars[2 * i + 1] = HEX_CHARS[digest[i] & 0x0F];
		}

		String modifiedText = TEXT + new String(chars);
		md = MessageDigest.getInstance(SHA1);
		md.update(modifiedText.getBytes());
		digest = md.digest();

		chars = new char[2 * digest.length];
		for (int i = 0; i < digest.length; ++i) {
			chars[2 * i] = HEX_CHARS[(digest[i] & 0xF0) >>> 4];
			chars[2 * i + 1] = HEX_CHARS[digest[i] & 0x0F];
		}

		String expectedEncryptedText = new String(chars);

		assertEquals("Encrypted text not as expected", expectedEncryptedText, DigestUtil.encrypt(TEXT));
	}
}
