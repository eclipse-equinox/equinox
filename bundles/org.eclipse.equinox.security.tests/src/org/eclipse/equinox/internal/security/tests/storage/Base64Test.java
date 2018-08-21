/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.equinox.internal.security.tests.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Random;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.junit.Test;

public class Base64Test {

	/**
	 * Number of random-generated round trip tests to run
	 */
	final private static int RANDOM_ITERATIONS = 1000;

	final private static String decoded1 = "sample^^*";
	final private static String encoded1 = "c2FtcGxlXl4q";

	final private static String decoded2 = "lazy frog jumped over sleeping dog";
	final private static String encoded2 = "bGF6eSBmcm9nIGp1bXBlZCBvdmVyIHNsZWVwaW5nIGRvZw==";

	final private static byte[] decoded3 = {5, 0, 0, 12, 32, 1, 127, (byte) 0xFF};
	final private static String encoded3 = "BQAADCABf/8=";

	final private static byte[] decoded4 = new byte[0];
	final private static String encoded4 = "";

	final private static String decoded5 = "1.234";
	final private static String encoded5 = "M\05S4y\tM\n\rzQ=\r\n"; // tests invalid characters

	final private static String decoded6 = "a";
	final private static String encoded6 = "YQ=="; // tests array bounds

	/**
	 * Tests encoding using hand-calculated examples.
	 */
	@Test
	public void testHandCoded() {
		String encoded = EncodingUtils.encodeBase64(decoded1.getBytes());
		assertEquals(encoded1, encoded);

		byte[] bytes = EncodingUtils.decodeBase64(encoded2);
		String decoded = new String(bytes);
		assertEquals(decoded2, decoded);

		String testZeroes = EncodingUtils.encodeBase64(decoded3);
		assertEquals(encoded3, testZeroes);
		byte[] roundtripBytes = EncodingUtils.decodeBase64(testZeroes);
		compareArrays(decoded3, roundtripBytes);

		byte[] bytesInvalidChars = EncodingUtils.decodeBase64(encoded5);
		String decodedInvalidChars = new String(bytesInvalidChars);
		assertEquals(decoded5, decodedInvalidChars);

		String shortSample = EncodingUtils.encodeBase64(decoded6.getBytes());
		assertEquals(encoded6, shortSample);
		assertEquals(decoded6, new String(EncodingUtils.decodeBase64(shortSample)));
	}

	/**
	 * Tests edge conditions: null or empty arguments 
	 */
	@Test
	public void testEdge() {
		assertNull(EncodingUtils.encodeBase64(null));
		assertNull(EncodingUtils.decodeBase64(null));

		String encoded = EncodingUtils.encodeBase64(decoded4);
		assertNotNull(encoded);
		assertEquals(encoded4, encoded);

		byte[] decoded = EncodingUtils.decodeBase64(encoded);
		compareArrays(decoded4, decoded);
	}

	/**
	 * Tests round trip using large random sequences 
	 */
	@Test
	public void testRandom() {
		Random generator = new Random(System.currentTimeMillis());

		for (int i = 0; i < RANDOM_ITERATIONS; i++) {
			// length of array is random in [100, 1000)
			int length = 100 + generator.nextInt(900);
			byte[] bytes = new byte[length];
			generator.nextBytes(bytes);

			// round trip
			String encoded = EncodingUtils.encodeBase64(bytes);
			byte[] decoded = EncodingUtils.decodeBase64(encoded);
			compareArrays(bytes, decoded);
		}
	}

	private void compareArrays(byte[] array1, byte[] array2) {
		assertNotNull(array1);
		assertNotNull(array2);
		assertEquals(array1.length, array2.length);
		for (int i = 0; i < array1.length; i++)
			assertEquals(array1[i], array2[i]);
	}

}
