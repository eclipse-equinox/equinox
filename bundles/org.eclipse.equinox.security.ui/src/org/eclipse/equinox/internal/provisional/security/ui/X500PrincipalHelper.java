/*******************************************************************************
 * Copyright (c) 2008,  Jay Rosenthal and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Jay Rosenthal - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.provisional.security.ui;

import java.util.ArrayList;
import java.util.Iterator;
import javax.security.auth.x500.X500Principal;

/**
 * X500PrincipalHelper
 * <p>
 * Helper class to extract pieces (attributes) of an X500Principal object for display
 * in the UI.
 * <p>
 * This helper uses the X500Principal.RFC2253 format of X500Principal.getname() to parse an X500Principal name 
 *   into it's component parts. 
 *  <p> 
 * In principals which contain multiple occurrences of the same attribute,the default for all the methods 
 *  is to return the most significant (first) attribute found. 
 *    
 */

public class X500PrincipalHelper {
	public static int LEASTSIGNIFICANT = 0;
	public static int MOSTSIGNIFICANT = 1;

	public final static String attrCN = "CN"; //$NON-NLS-1$
	public final static String attrOU = "OU"; //$NON-NLS-1$
	public final static String attrO = "O"; //$NON-NLS-1$
	public final static String attrC = "C"; //$NON-NLS-1$
	public final static String attrL = "L"; //$NON-NLS-1$
	public final static String attrST = "ST"; //$NON-NLS-1$
	public final static String attrSTREET = "STREET";//$NON-NLS-1$
	public final static String attrEMAIL = "EMAILADDRESS"; //$NON-NLS-1$
	public final static String attrUID = "UID"; //$NON-NLS-1$

	ArrayList rdnNameArray = new ArrayList();

	private final static String attrTerminator = "="; //$NON-NLS-1$

	public X500PrincipalHelper() {
		// Do nothing constructor..
		// Wont be useful unless setPrincipal is called...
	}

	public X500PrincipalHelper(X500Principal principal) {
		parseDN(principal.getName(X500Principal.RFC2253));
	}

	/**
	 *  Set the X500Principal name object to be parsed.
	 *  <p>
	 * @param principal - X500Principal
	 */
	public void setPrincipal(X500Principal principal) {
		parseDN(principal.getName(X500Principal.RFC2253));
	}

	/**
	 *   Gets the most significant common name (CN) attribute from the given
	 *      X500Principal object.
	 *   For names that contains multiple attributes of this type.  The first
	 *      (most significant) one will be returned   
	 * <p> 
	 * @return the Most significant common name attribute.
	 * 
	 */
	public String getCN() {
		return findPart(attrCN);
	}

	/**
	 *   Gets the most significant Organizational Unit (OU) attribute from the given
	 *      X500Principal object.
	 *   For names that contains multiple attributes of this type.  The first
	 *      (most significant) one will be returned       
	 * <p> 
	 * 
	 * @return the Most significant OU attribute.
	 * 
	 */

	public String getOU() {
		return findPart(attrOU);

	}

	/**
	 *   Gets the most significant Organization (O) attribute from the given
	 *      X500Principal object.
	 *   For names that contains multiple attributes of this type.  The first
	 *      (most significant) one will be returned       
	 * <p> 
	 * 
	 * @return the Most significant O attribute.
	 * 
	 */

	public String getO() {
		return findPart(attrO);

	}

	/**
	 *   Gets the Country (C) attribute from the given
	 *      X500Principal object.      
	 * <p> 
	 * 
	 * @return the C attribute.
	 * 
	 */
	public String getC() {
		return findPart(attrC);
	}

	/**
	 *   Gets the Locale (L) attribute from the given
	 *      X500Principal object.      
	 * <p> 
	 * 
	 * @return the L attribute.
	 * 
	 */
	public String getL() {
		return findPart(attrL);
	}

	/**
	 *   Gets the State (ST) attribute from the given
	 *      X500Principal object.      
	 * <p> 
	 * 
	 * @return the ST attribute.
	 * 
	 */
	public String getST() {
		return findPart(attrST);
	}

	/**
	 *   Gets the Street (STREET) attribute from the given
	 *      X500Principal object.      
	 * <p> 
	 * 
	 * @return the STREET attribute.
	 * 
	 */
	public String getSTREET() {
		return findPart(attrSTREET);
	}

	/**
	 *   Gets the Email Address (EMAILADDRESS) attribute from the given
	 *      X500Principal object.      
	 * <p> 
	 * 
	 * @return the EMAILADDRESS attribute.
	 * 
	 */
	public String getEMAILDDRESS() {
		return findPart(attrEMAIL);
	}

	public String getUID() {
		return findPart(attrUID);
	}

	/**
	 * Derived From: org.eclipse.osgi.internal.verifier - DNChainMatching.java
	 * 
	 * Takes a distinguished name in canonical form and fills in the rdnArray
	 * with the extracted RDNs.
	 * 
	 * @param dn the distinguished name in canonical form.
	 * @throws IllegalArgumentException if a formatting error is found.
	 */
	private void parseDN(String dn) throws IllegalArgumentException {
		int startIndex = 0;
		char c = '\0';
		ArrayList nameValues = new ArrayList();

		// Clear the existing array, in case this instance is being re-used
		rdnNameArray.clear();

		while (startIndex < dn.length()) {
			int endIndex;
			for (endIndex = startIndex; endIndex < dn.length(); endIndex++) {
				c = dn.charAt(endIndex);
				if (c == ',' || c == '+')
					break;
				if (c == '\\') {
					endIndex++; // skip the escaped char
				}
			}

			if (endIndex > dn.length())
				throw new IllegalArgumentException("unterminated escape " + dn); //$NON-NLS-1$

			nameValues.add(dn.substring(startIndex, endIndex));

			if (c != '+') {
				rdnNameArray.add(nameValues);
				if (endIndex != dn.length())
					nameValues = new ArrayList();
				else
					nameValues = null;
			}
			startIndex = endIndex + 1;
		}
		if (nameValues != null) {
			throw new IllegalArgumentException("improperly terminated DN " + dn); //$NON-NLS-1$
		}
	}

	/**
	 * Returns an ArrayList containing all the values for the given attribute identifier.
	 * <p>
	 * @param  attributeID  String containing the X500 name attribute whose values are to be 
	 *      returned
	 * @return ArrayList containing the string values of the requested attribute. Values are in   
	 *      the order they occur.  May be empty.
	 * 
	 */
	public ArrayList getAllValues(String attributeID) {
		ArrayList retList = new ArrayList();
		String searchPart = attributeID + attrTerminator;

		for (Iterator iter = rdnNameArray.iterator(); iter.hasNext();) {
			ArrayList nameList = (ArrayList) iter.next();
			String namePart = (String) nameList.get(0);

			if (namePart.startsWith(searchPart)) {
				// Return the string starting after the ID string and the = sign that follows it.
				retList.add(namePart.toString().substring(searchPart.length()));
			}
		}

		return retList;

	}

	private String findPart(String attributeID) {
		return findSignificantPart(attributeID, MOSTSIGNIFICANT);
	}

	private String findSignificantPart(String attributeID, int significance) {
		String retNamePart = null;
		String searchPart = attributeID + attrTerminator;

		for (Iterator iter = rdnNameArray.iterator(); iter.hasNext();) {
			ArrayList nameList = (ArrayList) iter.next();
			String namePart = (String) nameList.get(0);

			if (namePart.startsWith(searchPart)) {
				// Return the string starting after the ID string and the = sign that follows it.
				retNamePart = namePart.toString().substring(searchPart.length());
				//  By definition the first one is most significant
				if (significance == MOSTSIGNIFICANT)
					break;
			}
		}

		return retNamePart;
	}

}
