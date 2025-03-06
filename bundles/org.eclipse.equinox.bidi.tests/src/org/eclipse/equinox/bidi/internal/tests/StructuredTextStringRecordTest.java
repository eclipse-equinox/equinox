/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
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
 ******************************************************************************/

package org.eclipse.equinox.bidi.internal.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.equinox.bidi.internal.StructuredTextStringRecord;
import org.junit.Test;

/**
 * Tests the StringRecord class
 */
public class StructuredTextStringRecordTest extends StructuredTextTestBase {
	@Test
	public void testStringRecord() {
		StructuredTextStringRecord sr;
		boolean catchFlag;
		// check handling of invalid arguments
		catchFlag = false;
		try {
			sr = StructuredTextStringRecord.addRecord(null, 1, StructuredTextTypeHandlerFactory.EMAIL, 0, 1);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch null string argument", catchFlag);
		catchFlag = false;
		try {
			sr = StructuredTextStringRecord.addRecord("abc", 1, null, 0, 1);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch null handler argument", catchFlag);
		catchFlag = false;
		try {
			sr = StructuredTextStringRecord.addRecord("abc", 0, StructuredTextTypeHandlerFactory.EMAIL, 0, 1);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch invalid segment count argument", catchFlag);
		catchFlag = false;
		try {
			sr = StructuredTextStringRecord.addRecord("abc", 1, StructuredTextTypeHandlerFactory.EMAIL, -1, 1);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch invalid start argument", catchFlag);
		catchFlag = false;
		try {
			sr = StructuredTextStringRecord.addRecord("abc", 1, StructuredTextTypeHandlerFactory.EMAIL, 4, 1);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch invalid start argument", catchFlag);
		catchFlag = false;
		try {
			sr = StructuredTextStringRecord.addRecord("abc", 1, StructuredTextTypeHandlerFactory.EMAIL, 0, 0);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch invalid limit argument", catchFlag);
		catchFlag = false;
		try {
			sr = StructuredTextStringRecord.addRecord("abc", 1, StructuredTextTypeHandlerFactory.EMAIL, 0, 5);
		} catch (IllegalArgumentException e) {
			catchFlag = true;
		}
		assertTrue("Catch invalid limit argument", catchFlag);

		int poolSize = StructuredTextStringRecord.POOLSIZE;
		int lim = poolSize / 2;
		sr = StructuredTextStringRecord.getRecord("XXX");
		assertEquals(null, sr);
		for (int i = 0; i < lim; i++) {
			String str = Integer.toString(i);
			sr = StructuredTextStringRecord.addRecord(str, 1, StructuredTextTypeHandlerFactory.EMAIL, 0, 1);
		}
		sr = StructuredTextStringRecord.getRecord(null);
		assertEquals(null, sr);
		sr = StructuredTextStringRecord.getRecord("");
		assertEquals(null, sr);

		for (int i = 0; i < poolSize; i++) {
			String str = Integer.toString(i);
			sr = StructuredTextStringRecord.getRecord(str);
			if (i < lim) {
				assertFalse(null == sr);
			} else {
				assertTrue(null == sr);
			}
		}

		for (int i = lim; i <= poolSize; i++) {
			String str = Integer.toString(i);
			sr = StructuredTextStringRecord.addRecord(str, 1, StructuredTextTypeHandlerFactory.EMAIL, 0, 1);
		}
		for (int i = 1; i <= poolSize; i++) {
			String str = Integer.toString(i);
			sr = StructuredTextStringRecord.getRecord(str);
			assertFalse(null == sr);
		}
		sr = StructuredTextStringRecord.getRecord("0");
		assertEquals(null, sr);
		sr = StructuredTextStringRecord.addRecord("thisisalongstring", 3, StructuredTextTypeHandlerFactory.EMAIL, 0, 2);
		sr.addSegment(StructuredTextTypeHandlerFactory.JAVA, 4, 5);
		sr.addSegment(StructuredTextTypeHandlerFactory.FILE, 6, 7);
		catchFlag = false;
		try {
			sr.addSegment(StructuredTextTypeHandlerFactory.EMAIL, 10, 13);
		} catch (IllegalStateException e) {
			catchFlag = true;
		}
		assertTrue("Catch too many segments", catchFlag);
		assertEquals(3, sr.getSegmentCount());
		assertEquals(StructuredTextTypeHandlerFactory.EMAIL, sr.getHandler(0));
		assertEquals(StructuredTextTypeHandlerFactory.JAVA, sr.getHandler(1));
		assertEquals(StructuredTextTypeHandlerFactory.FILE, sr.getHandler(2));
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

		StructuredTextStringRecord.clear();
		for (int i = 0; i <= poolSize; i++) {
			String str = Integer.toString(i);
			sr = StructuredTextStringRecord.getRecord(str);
			assertEquals(null, sr);
		}
	}
}
