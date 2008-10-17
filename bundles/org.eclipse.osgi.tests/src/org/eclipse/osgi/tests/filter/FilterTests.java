/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.filter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import junit.framework.*;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.*;

public class FilterTests extends TestCase {
	public static Test suite() {
		return new TestSuite(FilterTests.class);
	}

	static final int ISTRUE = 1;
	static final int ISFALSE = 2;
	static final int ISILLEGAL = 3;

	public void testFilter() {
		final BundleContext testContext = OSGiTestsActivator.getContext();
		Properties props = new Properties();
		props.put("room", "bedroom"); //$NON-NLS-1$ //$NON-NLS-2$
		props.put("channel", new Object[] {new Integer(34), "101"}); //$NON-NLS-1$//$NON-NLS-2$
		props.put("status", "(on\\)*"); //$NON-NLS-1$//$NON-NLS-2$
		Vector vec = new Vector(10, 10);
		vec.addElement(new Long(150));
		vec.addElement("100"); //$NON-NLS-1$
		props.put("max record time", vec); //$NON-NLS-1$
		props.put("canrecord", "true(x)"); //$NON-NLS-1$ //$NON-NLS-2$
		props.put("shortvalue", new Short((short) 1000)); //$NON-NLS-1$
		props.put("bytevalue", new Byte((byte) 10)); //$NON-NLS-1$
		props.put("floatvalue", new Float(1.01)); //$NON-NLS-1$
		props.put("doublevalue", new Double(2.01)); //$NON-NLS-1$
		props.put("charvalue", new Character('A')); //$NON-NLS-1$
		props.put("booleanvalue", new Boolean(false)); //$NON-NLS-1$
		props.put("weirdvalue", new Hashtable()); //$NON-NLS-1$
		props.put("primintarrayvalue", new int[] {1, 2, 3}); //$NON-NLS-1$
		props.put("primlongarrayvalue", new long[] {1, 2, 3}); //$NON-NLS-1$
		props.put("primbytearrayvalue", new byte[] {(byte) 1, (byte) 2, (byte) 3}); //$NON-NLS-1$
		props.put("primshortarrayvalue", new short[] {(short) 1, (short) 2, (short) 3}); //$NON-NLS-1$
		props.put("primfloatarrayvalue", new float[] {(float) 1.1, (float) 2.2, (float) 3.3}); //$NON-NLS-1$
		props.put("primdoublearrayvalue", new double[] {1.1, 2.2, 3.3}); //$NON-NLS-1$
		props.put("primchararrayvalue", new char[] {'A', 'b', 'C', 'd'}); //$NON-NLS-1$
		props.put("primbooleanarrayvalue", new boolean[] {false}); //$NON-NLS-1$
		try {
			props.put("bigintvalue", new BigInteger("4123456")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (NoClassDefFoundError e) {
			// ignore
		}
		try {
			props.put("bigdecvalue", new BigDecimal("4.123456")); //$NON-NLS-1$  //$NON-NLS-2$
		} catch (NoClassDefFoundError e) {
			// ignore
		}

		testFilter("(room=*)", props, ISTRUE); //$NON-NLS-1$
		testFilter("(room=bedroom)", props, ISTRUE); //$NON-NLS-1$
		testFilter("(room~= B E D R O O M )", props, ISTRUE); //$NON-NLS-1$
		testFilter("(room=abc)", props, ISFALSE); //$NON-NLS-1$
		testFilter(" ( room >=aaaa)", props, ISTRUE); //$NON-NLS-1$
		testFilter("(room <=aaaa)", props, ISFALSE); //$NON-NLS-1$
		testFilter("  ( room =b*) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("  ( room =*m) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(room=bed*room)", props, ISTRUE); //$NON-NLS-1$
		testFilter("  ( room =b*oo*m) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("  ( room =*b*oo*m*) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("  ( room =b*b*  *m*) ", props, ISFALSE); //$NON-NLS-1$
		testFilter("  (& (room =bedroom) (channel ~= 34))", props, ISTRUE); //$NON-NLS-1$
		testFilter("  (&  (room =b*)  (room =*x) (channel=34))", props, ISFALSE); //$NON-NLS-1$
		testFilter("(| (room =bed*)(channel=222)) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(| (room =boom*)(channel=101)) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("  (! (room =ab*b*oo*m*) ) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("  (status =\\(o*\\\\\\)\\*) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("  (canRecord =true\\(x\\)) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(max Record Time <=140) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(shortValue >= 100) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("  (  &  (  byteValue <= 100  )  (  byteValue >= 10  )  )  ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(weirdValue = 100) ", props, ISFALSE); //$NON-NLS-1$
		testFilter("(bigIntValue = 4123456) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(bigDecValue = 4.123456) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(floatValue >= 1.0) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(doubleValue <= 2.011) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(charValue ~= a) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(booleanValue = false) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(primIntArrayValue = 1) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(primLongArrayValue = 2) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(primByteArrayValue = 3) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(primShortArrayValue = 1) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(primFloatArrayValue = 1.1) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(primDoubleArrayValue = 2.2) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(primCharArrayValue ~= D) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(primBooleanArrayValue = false) ", props, ISTRUE); //$NON-NLS-1$
		testFilter("(& (| (room =d*m) (room =bed*) (room=abc)) (! (channel=999)))", props, ISTRUE); //$NON-NLS-1$
		testFilter("(room=bedroom)", null, ISFALSE); //$NON-NLS-1$

		testFilter("", props, ISILLEGAL); //$NON-NLS-1$
		testFilter("()", props, ISILLEGAL); //$NON-NLS-1$
		testFilter("(=foo)", props, ISILLEGAL); //$NON-NLS-1$
		testFilter("(", props, ISILLEGAL); //$NON-NLS-1$
		testFilter("(abc = ))", props, ISILLEGAL); //$NON-NLS-1$
		testFilter("(& (abc = xyz) (& (345))", props, ISILLEGAL); //$NON-NLS-1$
		testFilter("  (room = b**oo!*m*) ) ", props, ISILLEGAL); //$NON-NLS-1$
		testFilter("  (room = b**oo)*m*) ) ", props, ISILLEGAL); //$NON-NLS-1$
		testFilter("  (room = *=b**oo*m*) ) ", props, ISILLEGAL); //$NON-NLS-1$
		testFilter("  (room = =b**oo*m*) ) ", props, ISILLEGAL); //$NON-NLS-1$

		try {
			Filter f1 = testContext.createFilter("( a = bedroom  )"); //$NON-NLS-1$
			Filter f2 = testContext.createFilter(" (a= bedroom  ) "); //$NON-NLS-1$
			assertEquals("not equal", "(a= bedroom  )", f1.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("not equal", "(a= bedroom  )", f2.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			assertEquals("not equal", f1, f2); //$NON-NLS-1$
			assertEquals("not equal", f2, f1); //$NON-NLS-1$
			assertEquals("not equal", f1.hashCode(), f2.hashCode()); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("unexpected invalid syntax: " + e); //$NON-NLS-1$
		}
	}

	private void testFilter(String query, Properties props, int expect) {
		final BundleContext testContext = OSGiTestsActivator.getContext();
		Filter p;
		try {
			p = testContext.createFilter(query);

			if (expect == ISILLEGAL) {
				fail("expected exception"); //$NON-NLS-1$
			}
		} catch (InvalidSyntaxException e) {
			if (expect != ISILLEGAL) {
				fail("exception: " + e.toString()); //$NON-NLS-1$
			}
			return;
		}

		boolean val = p.match(props);
		assertEquals("wrong result", expect == ISTRUE, val); //$NON-NLS-1$
	}

	public void testComparable() {
		final BundleContext testContext = OSGiTestsActivator.getContext();
		Filter filter = null;
		try {
			filter = testContext.createFilter("(comparable=42)"); //$NON-NLS-1$
		} catch (InvalidSyntaxException e) {
			fail("invalid syntax" + e); //$NON-NLS-1$
		}
		Object comp;
		Hashtable hash = new Hashtable();

		comp = new SampleComparable("42"); //$NON-NLS-1$
		hash.put("comparable", comp); //$NON-NLS-1$
		assertTrue("does not match filter", filter.match(hash)); //$NON-NLS-1$

		comp = new Long(42);
		hash.put("comparable", comp); //$NON-NLS-1$
		assertTrue("does not match filter", filter.match(hash)); //$NON-NLS-1$
	}

	private static class SampleComparable implements Comparable {
		private int value = -1;

		public SampleComparable(String value) {
			this.value = Integer.parseInt(value);
		}

		public int compareTo(Object o) {
			return value - ((SampleComparable) o).value;
		}

		public String toString() {
			return String.valueOf(value);
		}
	}

}
