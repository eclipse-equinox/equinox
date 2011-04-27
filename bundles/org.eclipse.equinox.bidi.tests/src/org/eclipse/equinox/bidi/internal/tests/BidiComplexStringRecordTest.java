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

package org.eclipse.equinox.bidi.internal.tests;

import org.eclipse.equinox.bidi.BidiComplexStringRecord;
import org.eclipse.equinox.bidi.custom.BidiComplexStringProcessor;

/**
 *	Tests the StringRecord class	
 */
public class BidiComplexStringRecordTest extends BidiComplexTestBase {
	public void testStringRecord() {
		BidiComplexStringRecord sr;
		boolean catchFlag;
		short[] badTriplet1 = new short[] {0, 0};
		short[] badTriplet2 = new short[] {0, 0, -1};
		short[] badTriplet3 = new short[] {0, 0, 999};
		short[] goodTriplet = new short[] {0, 3, 2};
		short[] triplets;
		short type;
		String strType;
		// check handling of invalid arguments
		catchFlag = false;
		try {
			sr = new BidiComplexStringRecord(null, badTriplet1);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch null string argument", catchFlag);
		catchFlag = false;
		try {
			sr = new BidiComplexStringRecord("xxx", null);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch null triplets argument", catchFlag);
		catchFlag = false;
		try {
			sr = new BidiComplexStringRecord("xxx", badTriplet1);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch bad triplet #1 argument", catchFlag);
		catchFlag = false;
		try {
			sr = new BidiComplexStringRecord("xxx", badTriplet2);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch bad triplet #2 argument", catchFlag);
		catchFlag = false;
		try {
			sr = new BidiComplexStringRecord("xxx", badTriplet3);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch bad triplet #3 argument", catchFlag);

		String[] types = BidiComplexStringProcessor.getKnownTypes();
		for (int i = 0; i < types.length; i++) {
			type = BidiComplexStringRecord.typeStringToShort(types[i]);
			assertFalse(type == -1);
			strType = BidiComplexStringRecord.typeShortToString(type);
			assertEquals(types[i], strType);
		}
		type = BidiComplexStringRecord.typeStringToShort("dummy");
		assertEquals(-1, type);
		strType = BidiComplexStringRecord.typeShortToString((short) 999);
		assertEquals(null, strType);
		int poolSize = BidiComplexStringRecord.POOLSIZE;
		int lim = poolSize / 2;
		triplets = BidiComplexStringRecord.getTriplets("xxx");
		assertEquals(null, triplets);
		for (int i = 0; i < lim; i++) {
			String str = Integer.toString(i);
			sr = new BidiComplexStringRecord(str, goodTriplet);
			BidiComplexStringRecord.add(sr);
		}
		triplets = BidiComplexStringRecord.getTriplets(null);
		assertEquals(null, triplets);
		triplets = BidiComplexStringRecord.getTriplets("");
		assertEquals(null, triplets);
		for (int i = 0; i < poolSize; i++) {
			String str = Integer.toString(i);
			triplets = BidiComplexStringRecord.getTriplets(str);
			if (i < lim)
				assertFalse(null == triplets);
			else
				assertTrue(null == triplets);
		}
		for (int i = lim; i <= poolSize; i++) {
			String str = Integer.toString(i);
			sr = new BidiComplexStringRecord(str, goodTriplet);
			BidiComplexStringRecord.add(sr);
		}
		for (int i = 1; i <= poolSize; i++) {
			String str = Integer.toString(i);
			triplets = BidiComplexStringRecord.getTriplets(str);
			assertFalse(null == triplets);
		}
		triplets = BidiComplexStringRecord.getTriplets("0");
		assertEquals(null, triplets);
		BidiComplexStringRecord.clear();
		for (int i = 0; i <= poolSize; i++) {
			String str = Integer.toString(i);
			triplets = BidiComplexStringRecord.getTriplets(str);
			assertEquals(null, triplets);
		}
	}
}
