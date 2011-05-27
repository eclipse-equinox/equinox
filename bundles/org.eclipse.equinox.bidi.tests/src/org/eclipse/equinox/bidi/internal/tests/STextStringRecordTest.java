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

import org.eclipse.equinox.bidi.STextStringRecord;
import org.eclipse.equinox.bidi.custom.STextStringProcessor;

/**
 *	Tests the StringRecord class	
 */
public class STextStringRecordTest extends STextTestBase {
	public void testStringRecord() {
		STextStringRecord sr;
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
			sr = new STextStringRecord(null, badTriplet1);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch null string argument", catchFlag);
		catchFlag = false;
		try {
			sr = new STextStringRecord("xxx", null);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch null triplets argument", catchFlag);
		catchFlag = false;
		try {
			sr = new STextStringRecord("xxx", badTriplet1);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch bad triplet #1 argument", catchFlag);
		catchFlag = false;
		try {
			sr = new STextStringRecord("xxx", badTriplet2);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch bad triplet #2 argument", catchFlag);
		catchFlag = false;
		try {
			sr = new STextStringRecord("xxx", badTriplet3);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch bad triplet #3 argument", catchFlag);

		String[] types = STextStringProcessor.getKnownTypes();
		for (int i = 0; i < types.length; i++) {
			type = STextStringRecord.typeStringToShort(types[i]);
			assertFalse(type == -1);
			strType = STextStringRecord.typeShortToString(type);
			assertEquals(types[i], strType);
		}
		type = STextStringRecord.typeStringToShort("dummy");
		assertEquals(-1, type);
		strType = STextStringRecord.typeShortToString((short) 999);
		assertEquals(null, strType);
		int poolSize = STextStringRecord.POOLSIZE;
		int lim = poolSize / 2;
		triplets = STextStringRecord.getTriplets("xxx");
		assertEquals(null, triplets);
		for (int i = 0; i < lim; i++) {
			String str = Integer.toString(i);
			sr = new STextStringRecord(str, goodTriplet);
			STextStringRecord.add(sr);
		}
		triplets = STextStringRecord.getTriplets(null);
		assertEquals(null, triplets);
		triplets = STextStringRecord.getTriplets("");
		assertEquals(null, triplets);
		for (int i = 0; i < poolSize; i++) {
			String str = Integer.toString(i);
			triplets = STextStringRecord.getTriplets(str);
			if (i < lim)
				assertFalse(null == triplets);
			else
				assertTrue(null == triplets);
		}
		for (int i = lim; i <= poolSize; i++) {
			String str = Integer.toString(i);
			sr = new STextStringRecord(str, goodTriplet);
			STextStringRecord.add(sr);
		}
		for (int i = 1; i <= poolSize; i++) {
			String str = Integer.toString(i);
			triplets = STextStringRecord.getTriplets(str);
			assertFalse(null == triplets);
		}
		triplets = STextStringRecord.getTriplets("0");
		assertEquals(null, triplets);
		STextStringRecord.clear();
		for (int i = 0; i <= poolSize; i++) {
			String str = Integer.toString(i);
			triplets = STextStringRecord.getTriplets(str);
			assertEquals(null, triplets);
		}
	}
}
