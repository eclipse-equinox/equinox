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

import org.eclipse.equinox.bidi.STextProcessorFactory;
import org.eclipse.equinox.bidi.STextStringRecord;

/**
 *	Tests the StringRecord class	
 */
public class STextStringRecordTest extends STextTestBase {
	public void testStringRecord() {
		STextStringRecord sr;
		boolean catchFlag;
		// check handling of invalid arguments
		catchFlag = false;
		try {
			sr = STextStringRecord.addRecord(null, 1, STextProcessorFactory.EMAIL, 0, 1);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch null string argument", catchFlag);
		catchFlag = false;
		try {
			sr = STextStringRecord.addRecord("abc", 1, null, 0, 1);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch null processor argument", catchFlag);
		catchFlag = false;
		try {
			sr = STextStringRecord.addRecord("abc", 0, STextProcessorFactory.EMAIL, 0, 1);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch invalid segment count argument", catchFlag);
		catchFlag = false;
		try {
			sr = STextStringRecord.addRecord("abc", 1, STextProcessorFactory.EMAIL, -1, 1);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch invalid start argument", catchFlag);
		catchFlag = false;
		try {
			sr = STextStringRecord.addRecord("abc", 1, STextProcessorFactory.EMAIL, 4, 1);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch invalid start argument", catchFlag);
		catchFlag = false;
		try {
			sr = STextStringRecord.addRecord("abc", 1, STextProcessorFactory.EMAIL, 0, 0);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch invalid limit argument", catchFlag);
		catchFlag = false;
		try {
			sr = STextStringRecord.addRecord("abc", 1, STextProcessorFactory.EMAIL, 0, 5);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch invalid limit argument", catchFlag);

		int poolSize = STextStringRecord.POOLSIZE;
		int lim = poolSize / 2;
		sr = STextStringRecord.getRecord("XXX");
		assertEquals(null, sr);
		for (int i = 0; i < lim; i++) {
			String str = Integer.toString(i);
			sr = STextStringRecord.addRecord(str, 1, STextProcessorFactory.EMAIL, 0, 1);
		}
		sr = STextStringRecord.getRecord(null);
		assertEquals(null, sr);
		sr = STextStringRecord.getRecord("");
		assertEquals(null, sr);

		for (int i = 0; i < poolSize; i++) {
			String str = Integer.toString(i);
			sr = STextStringRecord.getRecord(str);
			if (i < lim)
				assertFalse(null == sr);
			else
				assertTrue(null == sr);
		}

		for (int i = lim; i <= poolSize; i++) {
			String str = Integer.toString(i);
			sr = STextStringRecord.addRecord(str, 1, STextProcessorFactory.EMAIL, 0, 1);
		}
		for (int i = 1; i <= poolSize; i++) {
			String str = Integer.toString(i);
			sr = STextStringRecord.getRecord(str);
			assertFalse(null == sr);
		}
		sr = STextStringRecord.getRecord("0");
		assertEquals(null, sr);
		sr = STextStringRecord.addRecord("thisisalongstring", 3, STextProcessorFactory.EMAIL, 0, 2);
		sr.addSegment(STextProcessorFactory.JAVA, 4, 5);
		sr.addSegment(STextProcessorFactory.FILE, 6, 7);
		catchFlag = false;
		try {
			sr.addSegment(STextProcessorFactory.EMAIL, 10, 13);
		} catch (IllegalStateException e) {
			catchFlag = true;
		}
		assertTrue("Catch too many segments", catchFlag);
		assertEquals(3, sr.getSegmentCount());
		assertEquals(STextProcessorFactory.EMAIL, sr.getProcessor(0));
		assertEquals(STextProcessorFactory.JAVA, sr.getProcessor(1));
		assertEquals(STextProcessorFactory.FILE, sr.getProcessor(2));
		assertEquals(0, sr.getStart(0));
		assertEquals(4, sr.getStart(1));
		assertEquals(6, sr.getStart(2));
		assertEquals(2, sr.getLimit(0));
		assertEquals(5, sr.getLimit(1));
		assertEquals(7, sr.getLimit(2));
		catchFlag = false;
		try {
			sr.getLimit(3);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch segment number too large", catchFlag);

		STextStringRecord.clear();
		for (int i = 0; i <= poolSize; i++) {
			String str = Integer.toString(i);
			sr = STextStringRecord.getRecord(str);
			assertEquals(null, sr);
		}
	}
}
