/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.bidi;

import java.lang.ref.SoftReference;
import org.eclipse.equinox.bidi.custom.BidiComplexStringProcessor;

/**
 *  This class records strings which are complex expressions. Several static
 *  methods in this class allow to record such strings in a pool, and to find if
 *  a given string is member of the pool.
 *  <p>
 *  Instances of this class are the records which are members of the pool.
 *  <p>
 *  The pool is managed as a cyclic list. When the pool is full,
 *  each new element overrides the oldest element in the list.
 */
public class BidiComplexStringRecord {
	/**
	 * Number of entries in the pool of recorded strings
	 */
	public static final int POOLSIZE = 100;

	// maximum index allowed
	private static final int MAXINDEX = POOLSIZE - 1;

	// index of the last entered record
	private static int last = MAXINDEX;

	// the pool
	private static BidiComplexStringRecord[] records = new BidiComplexStringRecord[POOLSIZE];

	// complex expression types
	private static final String[] types = BidiComplexStringProcessor.getKnownTypes();

	// maximum type index allowed
	private static int MAXTYPE = types.length - 1;

	// reference to the recorded string
	private SoftReference strRef;

	// hash code of the recorded string
	private int hash;

	// reference to the triplets of the recorded string
	private SoftReference triRef;

	/**
	 *  Constructor.
	 *
	 *  @param  string the string to record
	 *
	 *  @param  triplets
	 *          array of short integers, the number of elements in the array
	 *          must be a multiple of 3, so that the array is made of one or
	 *          more triplets of short integers.
	 *          <p>
	 *          The first element in each triplet is the beginning offset of a
	 *          susbstring of <code>string</code> which is a complex
	 *          expression.
	 *          <p>
	 *          The second element in each triplet is the ending offset of a
	 *          susbstring of <code>string</code> which is a complex
	 *          expression. This offset points to one position beyond the last
	 *          character of the substring.
	 *          <p>
	 *          The third element in each triplet is the numeric type of the
	 *          complex expression.<br>
	 *          The type of a complex expression must be one of the string
	 *          values listed in {@link IBidiComplexExpressionTypes}.<br>
	 *          The corresponding numeric type must be obtained using the
	 *          method {@link #typeStringToShort typeStringToShort}.
	 */
	public BidiComplexStringRecord(String string, short[] triplets) {
		if (string == null || triplets == null)
			throw new IllegalArgumentException("The string and triplets argument must not be null!"); //$NON-NLS-1$
		if ((triplets.length % 3) != 0)
			throw new IllegalArgumentException("The number of elements in triplets must be a multiple of 3!"); //$NON-NLS-1$
		for (int i = 2; i < triplets.length; i += 3)
			if (triplets[i] < 0 || triplets[i] > MAXTYPE)
				throw new IllegalArgumentException("Illegal type value in element" + i);
		strRef = new SoftReference(string);
		triRef = new SoftReference(triplets);
		hash = string.hashCode();
	}

	/**
	 *  Get the numeric type of a complex expression given its string type.
	 *
	 *  @param  type type of complex expression as string. It must be one
	 *          of the strings listed in {@link IBidiComplexExpressionTypes}.
	 *
	 *  @return a value which is the corresponding numeric type. If
	 *          <code>type</code> is invalid, the method returns <code>-1</code>.
	 */
	public static short typeStringToShort(String type) {
		for (int i = 0; i < types.length; i++)
			if (types[i].equals(type))
				return (short) i;
		return -1;
	}

	/**
	 *  Get the string type of a complex expression given its numeric type.
	 *
	 *  @param shType
	 *         the numeric type of a complex expression. It should be a value
	 *         obtained using {@link #typeStringToShort typeStringToShort}.
	 *
	 *  @return the corresponding string type. If <code>shType</code> is invalid,
	 *          the method returns <code>null</code>.
	 */
	public static String typeShortToString(short shType) {
		if (shType < 0 || shType > MAXTYPE)
			return null;
		return types[shType];
	}

	/**
	 *  Add a record to the pool.
	 *
	 *  @param record a BidiComplexStringRecord instance
	 */
	public static synchronized void add(BidiComplexStringRecord record) {
		if (last < MAXINDEX)
			last++;
		else
			last = 0;
		records[last] = record;
	}

	/**
	 *  Check if a string is recorded and retrieve its triplets.
	 *
	 *  @param  string the string to check
	 *
	 *  @return <code>null</code> if the string is not recorded in the pool;
	 *          otherwise, return the triplets associated with this string.
	 */
	public static short[] getTriplets(String string) {
		if (records[0] == null) // no records at all
			return null;
		if (string == null || string.length() < 1)
			return null;
		BidiComplexStringRecord rec;
		String str;
		short[] tri;
		int myLast = last;
		int hash = string.hashCode();
		for (int i = myLast; i >= 0; i--) {
			rec = records[i];
			if (hash == rec.hash && (tri = (short[]) rec.triRef.get()) != null && (str = (String) rec.strRef.get()) != null && string.equals(str)) {
				return tri;
			}
		}
		if (records[MAXINDEX] == null) // never recorded past myLast
			return null;
		for (int i = MAXINDEX; i > myLast; i--) {
			rec = records[i];
			if (hash == rec.hash && (tri = (short[]) rec.triRef.get()) != null && (str = (String) rec.strRef.get()) != null && string.equals(str)) {
				return tri;
			}
		}
		return null;
	}

	/**
	 *  Clear the pool. All elements of the pool are erased and any associated
	 *  memory is freed.
	 *
	 */
	public static synchronized void clear() {
		for (int i = 0; i <= MAXINDEX; i++) {
			BidiComplexStringRecord sr = records[i];
			if (sr == null)
				break;
			sr.hash = 0;
			sr.strRef.clear();
			sr.triRef.clear();
			records[i] = null;
		}
		last = MAXINDEX;
	}

}
