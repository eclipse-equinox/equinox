/**
 * Copyright (c) 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.equinox.console.internal.ssh;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.apache.mina.util.Base64;

/**
 * Reader for 'authorized_keys' file as typically found on Unix systems.
 */
public class AuthorizedKeys {

	public static enum KeyType {
		RSA, DSA
	}

	public static class ParseKeyException extends IOException {

		/** serialVersionUID */
		private static final long serialVersionUID = 1L;

		/**
		 * Creates a new instance.
		 * 
		 * @param message
		 * @param cause
		 */
		public ParseKeyException(final String message, final Throwable cause) {
			super(message, cause);
		}

	}

	private static final String PREFIX_KEY_TYPE = "ssh-";
	private static final String PREFIX_KEY_TYPE_DSA = "ssh-dsa ";
	private static final String PREFIX_KEY_TYPE_RSA = "ssh-rsa ";
	private static final String NEWLINE = "\n";

	private static byte[] asBytes(final String string) {
		try {
			return string.getBytes("UTF-8");
		} catch (final UnsupportedEncodingException e) {
			return string.getBytes();
		}
	}

	private static String asString(final byte[] bytes) {
		try {
			return new String(bytes, "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			return new String(bytes);
		}
	}

	public static void main(final String[] args) {
		try {
			final List<PublicKey> keys = new AuthorizedKeys(args[0]).getKeys();
			for (final PublicKey key : keys) {
				System.out.println(key);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private final List<PublicKey> keys;

	/**
	 * Creates a new instance.
	 * 
	 * @throws FileNotFoundException
	 */
	public AuthorizedKeys(final String authorizedKeysFile) throws FileNotFoundException, IOException {
		// read file line-by-line
		final File file = new File(authorizedKeysFile);
		final Scanner scanner = new Scanner(file);
		scanner.useDelimiter(NEWLINE);
		int lineNumber = 0;
		final List<PublicKey> keys = new ArrayList<PublicKey>();

		try {
			while (scanner.hasNext()) {
				lineNumber++;

				// get line (without leading and trailing blanks)
				final String line = scanner.next().trim();

				// ignore blank line and comments
				if ((line.length() == 0) || (line.charAt(0) == '#')) {
					continue;
				}

				// read key
				try {
					keys.add(readPublicKey(line));
				} catch (final Exception e) {
					throw new ParseKeyException("Line " + lineNumber + ": " + e.getMessage(), e);
				}
			}
		} finally {
			scanner.close();
		}

		this.keys = Collections.unmodifiableList(keys);
	}

	/**
	 * Returns the keys.
	 * 
	 * @return the keys
	 */
	public List<PublicKey> getKeys() {
		return keys;
	}

	private BigInteger readBigInteger(final ByteBuffer buffer) {
		final int len = buffer.getInt();
		final byte[] bytes = new byte[len];
		buffer.get(bytes);
		final BigInteger pubExp = new BigInteger(bytes);
		return pubExp;
	}

	private PublicKey readPublicKey(String line) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
		// [options] <type> <base64> <comment>
		final KeyType type;
		final byte[] key;

		// skip options (if any)
		if (!line.startsWith(PREFIX_KEY_TYPE)) {
			final int keyTypeStart = line.indexOf(PREFIX_KEY_TYPE);
			if (keyTypeStart == -1) {
				throw new IOException("missing key type");
			}
			line = line.substring(keyTypeStart);
		}

		// key type
		if (line.startsWith(PREFIX_KEY_TYPE_DSA)) {
			line = line.substring(PREFIX_KEY_TYPE_DSA.length());
			type = KeyType.DSA;
		} else if (line.startsWith(PREFIX_KEY_TYPE_RSA)) {
			line = line.substring(PREFIX_KEY_TYPE_RSA.length());
			type = KeyType.RSA;
		} else {
			throw new IOException("unsupported key type");
		}

		// key
		final int keyEndIdx = line.indexOf(' ');
		if (keyEndIdx != -1) {
			key = Base64.decodeBase64(asBytes(line.substring(0, keyEndIdx)));
			line = line.substring(keyEndIdx + 1);
		} else {
			key = Base64.decodeBase64(asBytes(line));
		}

		// wrap key into byte buffer
		final ByteBuffer buffer = ByteBuffer.wrap(key);

		// skip key type
		readString(buffer);

		// parse key
		switch (type) {
			case RSA:
				// exponent + modulus
				final BigInteger pubExp = readBigInteger(buffer);
				final BigInteger mod = readBigInteger(buffer);
				return KeyFactory.getInstance(KeyType.RSA.name()).generatePublic(new RSAPublicKeySpec(mod, pubExp));
			case DSA:
				// p + q+ g + y
				final BigInteger p = readBigInteger(buffer);
				final BigInteger q = readBigInteger(buffer);
				final BigInteger g = readBigInteger(buffer);
				final BigInteger y = readBigInteger(buffer);
				return KeyFactory.getInstance(KeyType.DSA.name()).generatePublic(new DSAPublicKeySpec(y, p, q, g));
			default:
				throw new IOException("not implemented: " + type);
		}
	}

	private String readString(final ByteBuffer buffer) {
		final int len = buffer.getInt();
		final byte[] bytes = new byte[len];
		buffer.get(bytes);
		return asString(bytes);
	}
}
