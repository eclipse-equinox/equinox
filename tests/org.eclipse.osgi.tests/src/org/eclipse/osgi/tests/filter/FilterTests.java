/*******************************************************************************
 * Copyright (c) 2008, 2022 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 578602 - Make FilterTests parametrized and simplify it
 *******************************************************************************/
package org.eclipse.osgi.tests.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import org.eclipse.osgi.framework.util.CaseInsensitiveDictionaryMap;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.eclipse.osgi.tests.util.MapDictionary;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@RunWith(Parameterized.class)
public class FilterTests {

	@FunctionalInterface
	private interface FilterFactory {
		Filter createFilter(String filterString) throws InvalidSyntaxException;
	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> allFilterFactories() {
		return Arrays.asList(
				new Object[] { "BundleContextFilter", (FilterFactory) OSGiTestsActivator.getContext()::createFilter },
				new Object[] { "FrameworkUtilFilter", (FilterFactory) FrameworkUtil::createFilter });
	}

	@Parameter(0)
	public String name;
	@Parameter(1)
	public FilterFactory filterFactory;


	static final int ISTRUE = 1;
	static final int ISFALSE = 2;
	static final int ISILLEGAL = 3;

	private Filter createFilter(String filterString) throws InvalidSyntaxException {
		return filterFactory.createFilter(filterString);
	}

	private Dictionary<String, Object> getProperties() {
		Dictionary<String, Object> props = new Hashtable<>();
		props.put("room", "bedroom");
		props.put("channel", new Object[] { Integer.valueOf(34), "101" });
		props.put("status", "(on\\)*");
		List<Object> vec = new ArrayList<>(10);
		vec.add(Long.valueOf(150));
		vec.add("100");
		props.put("max record time", vec);
		props.put("canrecord", "true(x)");
		props.put("shortvalue", Short.valueOf((short) 1000));
		props.put("intvalue", Integer.valueOf(100000));
		props.put("longvalue", Long.valueOf(10000000000L));
		props.put("bytevalue", Byte.valueOf((byte) 10));
		props.put("floatvalue", Float.valueOf(1.01f));
		props.put("doublevalue", Double.valueOf(2.01));
		props.put("charvalue", Character.valueOf('A'));
		props.put("booleanvalue", Boolean.TRUE);
		props.put("weirdvalue", new Hashtable<>());
		props.put("primintarrayvalue", new int[] { 1, 2, 3 });
		props.put("primlongarrayvalue", new long[] { 1, 2, 3 });
		props.put("primbytearrayvalue", new byte[] { (byte) 1, (byte) 2, (byte) 3 });
		props.put("primshortarrayvalue", new short[] { (short) 1, (short) 2, (short) 3 });
		props.put("primfloatarrayvalue", new float[] { (float) 1.1, (float) 2.2, (float) 3.3 });
		props.put("primdoublearrayvalue", new double[] { 1.1, 2.2, 3.3 });
		props.put("primchararrayvalue", new char[] { 'A', 'b', 'C', 'd' });
		props.put("primbooleanarrayvalue", new boolean[] { false });
		props.put("bigintvalue", new BigInteger("4123456"));
		props.put("bigdecvalue", new BigDecimal("4.123456"));
		props.put("*", "foo");
		props.put("!  ab", "b");
		props.put("|   ab", "b");
		props.put("&    ab", "b");
		props.put("!", "c");
		props.put("|", "c");
		props.put("&", "c");
		props.put("empty", "");
		props.put("space", Character.valueOf(' '));
		return props;
	}

	@Test
	public void testFilter() throws InvalidSyntaxException {
		Dictionary<String, Object> props = getProperties();
		testFilter("(room=*)", props, ISTRUE);
		testFilter("(room=bedroom)", props, ISTRUE);
		testFilter("(room~= B E D R O O M )", props, ISTRUE);
		testFilter("(room=abc)", props, ISFALSE);
		testFilter(" ( room >=aaaa)", props, ISTRUE);
		testFilter("(room <=aaaa)", props, ISFALSE);
		testFilter("  ( room =b*) ", props, ISTRUE);
		testFilter("  ( room =*m) ", props, ISTRUE);
		testFilter("(room=bed*room)", props, ISTRUE);
		testFilter("  ( room =b*oo*m) ", props, ISTRUE);
		testFilter("  ( room =*b*oo*m*) ", props, ISTRUE);
		testFilter("  ( room =b*b*  *m*) ", props, ISFALSE);
		testFilter("  (& (room =bedroom) (channel ~=34))", props, ISTRUE);
		testFilter("  (&  (room =b*)  (room =*x) (channel=34))", props, ISFALSE);
		testFilter("(| (room =bed*)(channel=222)) ", props, ISTRUE);
		testFilter("(| (room =boom*)(channel=101)) ", props, ISTRUE);
		testFilter("  (! (room =ab*b*oo*m*) ) ", props, ISTRUE);
		testFilter("  (status =\\(o*\\\\\\)\\*) ", props, ISTRUE);
		testFilter("  (canRecord =true\\(x\\)) ", props, ISTRUE);
		testFilter("(max Record Time <=140) ", props, ISTRUE);
		testFilter("(shortValue >=100) ", props, ISTRUE);
		testFilter("(intValue <=100001) ", props, ISTRUE);
		testFilter("(longValue >=10000000000) ", props, ISTRUE);
		testFilter("  (  &  (  byteValue <=100)  (  byteValue >=10)  )  ", props, ISTRUE);
		testFilter("(weirdValue =100) ", props, ISFALSE);
		testFilter("(bigIntValue =4123456) ", props, ISTRUE);
		testFilter("(bigDecValue =4.123456) ", props, ISTRUE);
		testFilter("(floatValue >=1.0) ", props, ISTRUE);
		testFilter("(doubleValue <=2.011) ", props, ISTRUE);
		testFilter("(charValue ~=a) ", props, ISTRUE);
		testFilter("(booleanValue =true) ", props, ISTRUE);
		testFilter("(primIntArrayValue =1) ", props, ISTRUE);
		testFilter("(primLongArrayValue =2) ", props, ISTRUE);
		testFilter("(primByteArrayValue =3) ", props, ISTRUE);
		testFilter("(primShortArrayValue =1) ", props, ISTRUE);
		testFilter("(primFloatArrayValue =1.1) ", props, ISTRUE);
		testFilter("(primDoubleArrayValue =2.2) ", props, ISTRUE);
		testFilter("(primCharArrayValue ~=D) ", props, ISTRUE);
		testFilter("(primBooleanArrayValue =false) ", props, ISTRUE);
		testFilter("(& (| (room =d*m) (room =bed*) (room=abc)) (! (channel=999)))", props, ISTRUE);
		testFilter("(room=bedroom)", null, ISFALSE);
		testFilter("(*=foo)", props, ISTRUE);
		testFilter("(!  ab=b)", props, ISTRUE);
		testFilter("(|   ab=b)", props, ISTRUE);
		testFilter("(&=c)", props, ISTRUE);
		testFilter("(!=c)", props, ISTRUE);
		testFilter("(|=c)", props, ISTRUE);
		testFilter("(&    ab=b)", props, ISTRUE);
		testFilter("(!ab=*)", props, ISFALSE);
		testFilter("(|ab=*)", props, ISFALSE);
		testFilter("(&ab=*)", props, ISFALSE);
		testFilter("(empty=)", props, ISTRUE);
		testFilter("(empty=*)", props, ISTRUE);
		testFilter("(space= )", props, ISTRUE);
		testFilter("(space=*)", props, ISTRUE);
	}

	@Test
	public void testInvalidValues() throws InvalidSyntaxException {
		Dictionary<String, Object> props = getProperties();
		testFilter("(intvalue=*)", props, ISTRUE);
		testFilter("(intvalue=b)", props, ISFALSE);
		testFilter("(intvalue=)", props, ISFALSE);
		testFilter("(longvalue=*)", props, ISTRUE);
		testFilter("(longvalue=b)", props, ISFALSE);
		testFilter("(longvalue=)", props, ISFALSE);
		testFilter("(shortvalue=*)", props, ISTRUE);
		testFilter("(shortvalue=b)", props, ISFALSE);
		testFilter("(shortvalue=)", props, ISFALSE);
		testFilter("(bytevalue=*)", props, ISTRUE);
		testFilter("(bytevalue=b)", props, ISFALSE);
		testFilter("(bytevalue=)", props, ISFALSE);
		testFilter("(charvalue=*)", props, ISTRUE);
		testFilter("(charvalue=)", props, ISFALSE);
		testFilter("(floatvalue=*)", props, ISTRUE);
		testFilter("(floatvalue=b)", props, ISFALSE);
		testFilter("(floatvalue=)", props, ISFALSE);
		testFilter("(doublevalue=*)", props, ISTRUE);
		testFilter("(doublevalue=b)", props, ISFALSE);
		testFilter("(doublevalue=)", props, ISFALSE);
		testFilter("(booleanvalue=*)", props, ISTRUE);
		testFilter("(booleanvalue=b)", props, ISFALSE);
		testFilter("(booleanvalue=)", props, ISFALSE);
	}

	@Test
	public void testIllegal() throws InvalidSyntaxException {
		Dictionary<String, Object> props = getProperties();
		testFilter("", props, ISILLEGAL);
		testFilter("()", props, ISILLEGAL);
		testFilter("(=foo)", props, ISILLEGAL);
		testFilter("(", props, ISILLEGAL);
		testFilter("(abc = ))", props, ISILLEGAL);
		testFilter("(& (abc = xyz) (& (345))", props, ISILLEGAL);
		testFilter("  (room = b**oo!*m*) ) ", props, ISILLEGAL);
		testFilter("  (room = b**oo)*m*) ) ", props, ISILLEGAL);
		testFilter("  (room = *=b**oo*m*) ) ", props, ISILLEGAL);
		testFilter("  (room = =b**oo*m*) ) ", props, ISILLEGAL);
	}

	@Test
	public void testScalarSubstring() throws InvalidSyntaxException {
		Dictionary<String, Object> props = getProperties();
		testFilter("(shortValue =100*) ", props, ISFALSE);
		testFilter("(intValue =100*) ", props, ISFALSE);
		testFilter("(longValue =100*) ", props, ISFALSE);
		testFilter("(  byteValue =1*00  )", props, ISFALSE);
		testFilter("(bigIntValue =4*23456) ", props, ISFALSE);
		testFilter("(bigDecValue =4*123456) ", props, ISFALSE);
		testFilter("(floatValue =1*0) ", props, ISFALSE);
		testFilter("(doubleValue =2*011) ", props, ISFALSE);
		testFilter("(charValue =a*) ", props, ISFALSE);
		testFilter("(booleanValue =t*ue) ", props, ISFALSE);
	}

	@Test
	public void testNormalization() throws InvalidSyntaxException {
		Filter f1 = createFilter("( a = bedroom  )");
		Filter f2 = createFilter(" (a= bedroom  ) ");
		assertEquals("not equal", "(a= bedroom  )", f1.toString());
		assertEquals("not equal", "(a= bedroom  )", f2.toString());
		assertEquals("not equal", f1, f2);
		assertEquals("not equal", f2, f1);
		assertEquals("not equal", f1.hashCode(), f2.hashCode());
	}

	private void testFilter(String query, Dictionary<String, Object> props, int expect) throws InvalidSyntaxException {

		if (expect == ISILLEGAL) {
			assertThrows(InvalidSyntaxException.class, () -> createFilter(query));
			return;
		}

		Filter f1 = createFilter(query);
		ServiceReference<?> ref = new DictionaryServiceReference(props);

		boolean val = f1.match(props);
		assertEquals("wrong result", expect == ISTRUE, val);

		val = f1.match(ref);
		assertEquals("wrong result", expect == ISTRUE, val);

		String normalized = f1.toString();
		Filter f2 = createFilter(normalized);

		val = f2.match(props);
		assertEquals("wrong result", expect == ISTRUE, val);

		val = f2.match(ref);
		assertEquals("wrong result", expect == ISTRUE, val);

		assertEquals("normalized not equal", normalized, f2.toString());

	}

	@Test
	public void testComparable() throws InvalidSyntaxException {
		Object comp42 = new SampleComparable("42");
		Object comp43 = new SampleComparable("43");
		Dictionary<String, Object> hash = new Hashtable<>();

		Filter f1 = createFilter("(comparable=42)");

		hash.put("comparable", comp42);
		assertTrue("does not match filter", f1.match(hash));
		assertTrue("does not match filter", f1.match(new DictionaryServiceReference(hash)));

		hash.put("comparable", comp43);
		assertFalse("does match filter", f1.match(hash));
		assertFalse("does match filter", f1.match(new DictionaryServiceReference(hash)));

		f1 = createFilter("(comparable<=42)");

		hash.put("comparable", comp42);
		assertTrue("does not match filter", f1.match(hash));
		assertTrue("does not match filter", f1.match(new DictionaryServiceReference(hash)));

		hash.put("comparable", comp43);
		assertFalse("does match filter", f1.match(hash));
		assertFalse("does match filter", f1.match(new DictionaryServiceReference(hash)));

		f1 = createFilter("(comparable>=42)");

		hash.put("comparable", comp42);
		assertTrue("does not match filter", f1.match(hash));
		assertTrue("does not match filter", f1.match(new DictionaryServiceReference(hash)));

		hash.put("comparable", comp43);
		assertTrue("does not match filter", f1.match(hash));
		assertTrue("does not match filter", f1.match(new DictionaryServiceReference(hash)));

		f1 = createFilter("(comparable=4*2)");

		hash.put("comparable", comp42);
		assertFalse("does match filter", f1.match(hash));
		assertFalse("does match filter", f1.match(new DictionaryServiceReference(hash)));
	}

	@Test
	public void testObject() throws InvalidSyntaxException {
		Object obj42 = new SampleObject("42");
		Object obj43 = new SampleObject("43");
		Dictionary<String, Object> hash = new Hashtable<>();

		Filter f1 = createFilter("(object=42)");

		hash.put("object", obj42);
		assertTrue("does not match filter", f1.match(hash));
		assertTrue("does not match filter", f1.match(new DictionaryServiceReference(hash)));

		hash.put("object", obj43);
		assertFalse("does match filter", f1.match(hash));
		assertFalse("does match filter", f1.match(new DictionaryServiceReference(hash)));

		f1 = createFilter("(object=4*2)");

		hash.put("object", obj42);
		assertFalse("does match filter", f1.match(hash));
		assertFalse("does match filter", f1.match(new DictionaryServiceReference(hash)));
	}

	@Test
	public void testNullValueMatch() throws InvalidSyntaxException {
		Dictionary<String, Object> nullProps = new MapDictionary<>();
		nullProps.put("test.null", null);
		nullProps.put("test.non.null", "v1");
		assertFalse(createFilter("(test.null=*)").match(nullProps));
		assertTrue(createFilter("(&(!(test.null=*))(test.non.null=v1))").match(nullProps));
	}

	@Test
	public void testNullKeyMatch() throws InvalidSyntaxException {
		Dictionary<String, Object> nullProps = new MapDictionary<>();
		nullProps.put(null, "null.v1");
		nullProps.put("test.non.null", "v1");
		assertTrue(createFilter("(test.non.null=v1)").match(nullProps));
	}

	// Equinox specific test to make sure we continue to use the Equinox FilterImpl
	// from the FrameworkUtil createFilter method
	@Test
	public void testFrameworkUtilCreateFilter() throws InvalidSyntaxException {
		Filter bundleContextFilter = OSGiTestsActivator.getContext().createFilter("(simplefilter=true)");
		Filter frameworkUtilFilter = FrameworkUtil.createFilter("(simplefilter=true)");
		assertTrue("Wrong Fitler impl type: " + frameworkUtilFilter.getClass().getName(),
				bundleContextFilter.getClass().equals(frameworkUtilFilter.getClass()));
	}

	private static class SampleComparable implements Comparable<SampleComparable> {
		private int value = -1;

		public SampleComparable(String value) {
			this.value = Integer.parseInt(value);
		}

		@Override
		public int compareTo(SampleComparable o) {
			return Integer.compare(value, o.value);
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}
	}

	private static class SampleObject {
		private int value = -1;

		public SampleObject(String value) {
			this.value = Integer.parseInt(value);
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof SampleObject) && value == ((SampleObject) o).value;
		}

		@Override
		public String toString() {
			return String.valueOf(value);
		}
	}

	private static class DictionaryServiceReference implements ServiceReference<Object> {
		private final Dictionary<String, ?> dictionary;

		DictionaryServiceReference(Dictionary<String, ?> dictionary) {
			this.dictionary = new CaseInsensitiveDictionaryMap<>(dictionary == null ? new Hashtable<>() : dictionary);
		}

		@Override
		public Object getProperty(String k) {
			return dictionary.get(k);
		}

		@Override
		public String[] getPropertyKeys() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int compareTo(Object reference) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Bundle getBundle() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Bundle[] getUsingBundles() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean isAssignableTo(Bundle bundle, String className) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Dictionary<String, Object> getProperties() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object adapt(Class type) {
			throw new UnsupportedOperationException();
		}
	}
}
