/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.console.storage;

import java.security.MessageDigest;
/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

import java.security.NoSuchAlgorithmException;

/**
 * This class provides utility method for one-way hashing of strings
 * 
 */
public class DigestUtil {
	private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
	private static final String MD5 = "MD5";
	private static final String SHA1 = "SHA1";
	
	/**
	 * Create a one-way hash of an input strings. First a MD5 hash of the input string
	 * is calculated and appended to the string, and then the new string is hashed with SHA1  
	 * 
	 * @param originalText the string to be hashed
	 * @return hashed string
	 * @throws Exception
	 */
	public static String encrypt(String originalText)throws Exception{
		try {
			String password_salt = appendSalt(originalText);
			byte[] sha_digest;

			sha_digest = getDigest(password_salt.getBytes(), SHA1);
			return asHex(sha_digest);
		} catch (NoSuchAlgorithmException e) {
			throw new Exception ("Encryption Failed!");
		}		
	}
	
	private static String appendSalt(String inputPassword) throws NoSuchAlgorithmException{
		byte [] salt = getDigest(inputPassword.getBytes(), MD5);
		return inputPassword + asHex(salt);
	}
	
	//byte array into hexademical string
    private static String asHex(byte[] buf)
    {
        char[] chars = new char[2 * buf.length];
        for (int i = 0; i < buf.length; ++i)
        {
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }
        return new String(chars);
    }
    
    //generate digest byte[]  
    private static byte[] getDigest(byte[] inputData, String algorithm) throws NoSuchAlgorithmException { 
    	MessageDigest md = MessageDigest.getInstance(algorithm);
    	md.update(inputData);
        return md.digest();
    }
}
