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
package org.eclipse.osgi.tests.eventmgr;

import java.util.*;
import junit.framework.*;
import org.eclipse.osgi.framework.eventmgr.EventListeners;

public class EventManagerTests extends TestCase {
	public static Test suite() {
		return new TestSuite(EventManagerTests.class);
	}

	public void testEventListeners() {
		Object l1 = new Object();
		Object l2 = new Object();
		Object l3 = new Object();
		Object l4 = new Object();
		Object c1 = new Object();
		Object c2 = new Object();
		Object c3 = new Object();
		Object c4 = new Object();

		EventListeners el1 = new EventListeners();

		assertTrue("not empty", el1.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 0, el1.size()); //$NON-NLS-1$

		assertNull("non null", el1.put(l1, c1)); //$NON-NLS-1$
		assertFalse("empty", el1.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 1, el1.size()); //$NON-NLS-1$

		assertNull("non null", el1.put(l2, c2)); //$NON-NLS-1$
		assertFalse("empty", el1.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 2, el1.size()); //$NON-NLS-1$

		assertNull("non null", el1.put(l3, c3)); //$NON-NLS-1$
		assertFalse("empty", el1.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 3, el1.size()); //$NON-NLS-1$

		assertNull("non null", el1.put(l4, c4)); //$NON-NLS-1$
		assertFalse("empty", el1.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 4, el1.size()); //$NON-NLS-1$

		assertEquals("wrong companion", c1, el1.get(l1)); //$NON-NLS-1$
		assertEquals("wrong companion", c2, el1.get(l2)); //$NON-NLS-1$
		assertEquals("wrong companion", c3, el1.get(l3)); //$NON-NLS-1$
		assertEquals("wrong companion", c4, el1.get(l4)); //$NON-NLS-1$

		EventListeners el2 = new EventListeners(el1);
		assertFalse("empty", el2.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 4, el2.size()); //$NON-NLS-1$

		assertEquals("wrong companion", c1, el2.get(l1)); //$NON-NLS-1$
		assertEquals("wrong companion", c2, el2.get(l2)); //$NON-NLS-1$
		assertEquals("wrong companion", c3, el2.get(l3)); //$NON-NLS-1$
		assertEquals("wrong companion", c4, el2.get(l4)); //$NON-NLS-1$

		assertEquals("wrong companion", c2, el1.remove(l2)); //$NON-NLS-1$
		assertFalse("empty", el1.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 3, el1.size()); //$NON-NLS-1$
		assertEquals("not null", null, el1.remove(l2)); //$NON-NLS-1$
		assertFalse("empty", el1.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 3, el1.size()); //$NON-NLS-1$
		assertEquals("wrong companion", c4, el1.remove(l4)); //$NON-NLS-1$
		assertFalse("empty", el1.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 2, el1.size()); //$NON-NLS-1$
		assertEquals("not null", null, el1.remove(l4)); //$NON-NLS-1$
		assertFalse("empty", el1.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 2, el1.size()); //$NON-NLS-1$
		assertEquals("wrong companion", c1, el1.get(l1)); //$NON-NLS-1$
		assertEquals("wrong companion", c3, el1.get(l3)); //$NON-NLS-1$

		assertFalse("empty", el2.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 4, el2.size()); //$NON-NLS-1$
		assertEquals("wrong companion", c1, el2.get(l1)); //$NON-NLS-1$
		assertEquals("wrong companion", c2, el2.get(l2)); //$NON-NLS-1$
		assertEquals("wrong companion", c3, el2.get(l3)); //$NON-NLS-1$
		assertEquals("wrong companion", c4, el2.get(l4)); //$NON-NLS-1$

		assertEquals("wrong companion", c1, el2.remove(l1)); //$NON-NLS-1$
		assertEquals("wrong companion", c3, el2.remove(l3)); //$NON-NLS-1$

		el1.putAll(el2);
		assertFalse("empty", el1.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 4, el1.size()); //$NON-NLS-1$
		assertEquals("wrong companion", c1, el1.get(l1)); //$NON-NLS-1$
		assertEquals("wrong companion", c2, el1.get(l2)); //$NON-NLS-1$
		assertEquals("wrong companion", c3, el1.get(l3)); //$NON-NLS-1$
		assertEquals("wrong companion", c4, el1.get(l4)); //$NON-NLS-1$
		assertFalse("empty", el2.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 2, el2.size()); //$NON-NLS-1$
		assertEquals("wrong companion", c2, el2.get(l2)); //$NON-NLS-1$
		assertEquals("wrong companion", c4, el2.get(l4)); //$NON-NLS-1$

		el2.putAll(el1);
		assertFalse("empty", el1.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 4, el1.size()); //$NON-NLS-1$
		assertEquals("wrong companion", c1, el1.get(l1)); //$NON-NLS-1$
		assertEquals("wrong companion", c2, el1.get(l2)); //$NON-NLS-1$
		assertEquals("wrong companion", c3, el1.get(l3)); //$NON-NLS-1$
		assertEquals("wrong companion", c4, el1.get(l4)); //$NON-NLS-1$
		assertFalse("empty", el2.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 4, el2.size()); //$NON-NLS-1$
		assertEquals("wrong companion", c1, el2.get(l1)); //$NON-NLS-1$
		assertEquals("wrong companion", c2, el2.get(l2)); //$NON-NLS-1$
		assertEquals("wrong companion", c3, el2.get(l3)); //$NON-NLS-1$
		assertEquals("wrong companion", c4, el2.get(l4)); //$NON-NLS-1$

		el2.clear();
		assertTrue("not empty", el2.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 0, el2.size()); //$NON-NLS-1$

		assertNull("non null", el2.put(l1, c1)); //$NON-NLS-1$
		assertEquals("wrong companion", c1, el2.put(l1, c3)); //$NON-NLS-1$
		assertEquals("wrong companion", c3, el2.get(l1)); //$NON-NLS-1$
		assertTrue("missed key", el2.containsKey(l1)); //$NON-NLS-1$
		assertTrue("missed value", el2.containsValue(c3)); //$NON-NLS-1$
		assertFalse("invalid key", el2.containsKey(l2)); //$NON-NLS-1$
		assertFalse("invalid value", el2.containsValue(c1)); //$NON-NLS-1$

		el2.clear();
		Map source = new HashMap();
		source.put(l1, c1);
		source.put(l4, c4);
		el2.putAll(source);
		assertFalse("empty", el2.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 2, el2.size()); //$NON-NLS-1$
		assertEquals("wrong companion", c1, el2.get(l1)); //$NON-NLS-1$
		assertEquals("wrong companion", c4, el2.get(l4)); //$NON-NLS-1$

		el2.clear();
		Set k1 = el1.keySet();
		Set k2 = el2.keySet();
		Collection v1 = el1.values();
		Collection v2 = el2.values();
		Set e2 = el2.entrySet();
		Set e1 = el1.entrySet();
		el1.clear();

		assertFalse("empty", k1.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 4, k1.size()); //$NON-NLS-1$
		assertTrue("missed key", k1.contains(l1)); //$NON-NLS-1$
		assertTrue("missed key", k1.contains(l2)); //$NON-NLS-1$
		assertTrue("missed key", k1.contains(l3)); //$NON-NLS-1$
		assertTrue("missed key", k1.contains(l4)); //$NON-NLS-1$
		assertTrue("missed key", k1.containsAll(Arrays.asList(new Object[] {l1, l2, l3, l4}))); //$NON-NLS-1$

		Iterator i1 = k1.iterator();
		assertTrue("missing next", i1.hasNext()); //$NON-NLS-1$
		assertEquals("wrong key", l1, i1.next()); //$NON-NLS-1$
		try {
			i1.remove();
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		assertTrue("missing next", i1.hasNext()); //$NON-NLS-1$
		assertEquals("wrong key", l3, i1.next()); //$NON-NLS-1$
		try {
			i1.remove();
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		assertTrue("missing next", i1.hasNext()); //$NON-NLS-1$
		assertEquals("wrong key", l2, i1.next()); //$NON-NLS-1$
		try {
			i1.remove();
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		assertTrue("missing next", i1.hasNext()); //$NON-NLS-1$
		assertEquals("wrong key", l4, i1.next()); //$NON-NLS-1$
		try {
			i1.remove();
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		assertFalse("extra next", i1.hasNext()); //$NON-NLS-1$
		try {
			i1.next();
			fail("next did not throw exception"); //$NON-NLS-1$
		} catch (NoSuchElementException e) {
			//expected
		}

		assertTrue("not empty", k2.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 0, k2.size()); //$NON-NLS-1$

		Iterator i2 = k2.iterator();
		assertFalse("extra next", i2.hasNext()); //$NON-NLS-1$
		try {
			i2.next();
			fail("next did not throw exception"); //$NON-NLS-1$
		} catch (NoSuchElementException e) {
			//expected
		}

		try {
			k2.add(l1);
			fail("add did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			k2.addAll(Arrays.asList(new Object[] {l1, l2}));
			fail("addAll did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			k1.clear();
			fail("clear did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			k1.remove(l1);
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			k1.removeAll(Arrays.asList(new Object[] {l1, l2}));
			fail("removeAll did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			k1.retainAll(Arrays.asList(new Object[] {l1, l2}));
			fail("retainAll did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}

		assertTrue("array unequal", Arrays.equals(new Object[] {l1, l3, l2, l4}, k1.toArray())); //$NON-NLS-1$
		assertTrue("array unequal", Arrays.equals(new Object[] {l1, l3, l2, l4}, k1.toArray(new Object[4]))); //$NON-NLS-1$

		assertFalse("empty", v1.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 4, v1.size()); //$NON-NLS-1$
		assertTrue("missed key", v1.contains(c1)); //$NON-NLS-1$
		assertTrue("missed key", v1.contains(c2)); //$NON-NLS-1$
		assertTrue("missed key", v1.contains(c3)); //$NON-NLS-1$
		assertTrue("missed key", v1.contains(c4)); //$NON-NLS-1$
		assertTrue("missed key", v1.containsAll(Arrays.asList(new Object[] {c1, c2, c3, c4}))); //$NON-NLS-1$

		Iterator i3 = v1.iterator();
		assertTrue("missing next", i3.hasNext()); //$NON-NLS-1$
		assertEquals("wrong key", c1, i3.next()); //$NON-NLS-1$
		try {
			i3.remove();
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		assertTrue("missing next", i3.hasNext()); //$NON-NLS-1$
		assertEquals("wrong key", c3, i3.next()); //$NON-NLS-1$
		try {
			i3.remove();
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		assertTrue("missing next", i3.hasNext()); //$NON-NLS-1$
		assertEquals("wrong key", c2, i3.next()); //$NON-NLS-1$
		try {
			i3.remove();
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		assertTrue("missing next", i3.hasNext()); //$NON-NLS-1$
		assertEquals("wrong key", c4, i3.next()); //$NON-NLS-1$
		try {
			i3.remove();
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		assertFalse("extra next", i3.hasNext()); //$NON-NLS-1$
		try {
			i3.next();
			fail("next did not throw exception"); //$NON-NLS-1$
		} catch (NoSuchElementException e) {
			//expected
		}

		assertTrue("not empty", v2.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 0, v2.size()); //$NON-NLS-1$

		Iterator i4 = v2.iterator();
		assertFalse("extra next", i4.hasNext()); //$NON-NLS-1$
		try {
			i4.next();
			fail("next did not throw exception"); //$NON-NLS-1$
		} catch (NoSuchElementException e) {
			//expected
		}

		try {
			v2.add(c1);
			fail("add did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			v2.addAll(Arrays.asList(new Object[] {c1, c2}));
			fail("addAll did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			v1.clear();
			fail("clear did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			v1.remove(c1);
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			v1.removeAll(Arrays.asList(new Object[] {c1, c2}));
			fail("removeAll did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			v1.retainAll(Arrays.asList(new Object[] {c1, c2}));
			fail("retainAll did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}

		assertTrue("array unequal", Arrays.equals(new Object[] {c1, c3, c2, c4}, v1.toArray())); //$NON-NLS-1$
		assertTrue("array unequal", Arrays.equals(new Object[] {c1, c3, c2, c4}, v1.toArray(new Object[4]))); //$NON-NLS-1$

		assertFalse("empty", e1.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 4, e1.size()); //$NON-NLS-1$

		Iterator i5 = e1.iterator();
		assertTrue("missing next", i5.hasNext()); //$NON-NLS-1$
		Map.Entry me1 = (Map.Entry) i5.next();
		assertEquals("wrong key", l1, me1.getKey()); //$NON-NLS-1$
		assertEquals("wrong value", c1, me1.getValue()); //$NON-NLS-1$
		try {
			i5.remove();
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			me1.setValue(c2);
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		assertTrue("missing next", i5.hasNext()); //$NON-NLS-1$
		Map.Entry me3 = (Map.Entry) i5.next();
		assertEquals("wrong key", l3, me3.getKey()); //$NON-NLS-1$
		assertEquals("wrong value", c3, me3.getValue()); //$NON-NLS-1$
		try {
			i5.remove();
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			me3.setValue(c2);
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		assertTrue("missing next", i5.hasNext()); //$NON-NLS-1$
		Map.Entry me2 = (Map.Entry) i5.next();
		assertEquals("wrong key", l2, me2.getKey()); //$NON-NLS-1$
		assertEquals("wrong value", c2, me2.getValue()); //$NON-NLS-1$
		try {
			i5.remove();
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			me2.setValue(c3);
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		assertTrue("missing next", i5.hasNext()); //$NON-NLS-1$
		Map.Entry me4 = (Map.Entry) i5.next();
		assertEquals("wrong key", l4, me4.getKey()); //$NON-NLS-1$
		assertEquals("wrong value", c4, me4.getValue()); //$NON-NLS-1$
		try {
			i5.remove();
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			me4.setValue(c2);
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		assertFalse("extra next", i5.hasNext()); //$NON-NLS-1$
		try {
			i1.next();
			fail("next did not throw exception"); //$NON-NLS-1$
		} catch (NoSuchElementException e) {
			//expected
		}

		assertTrue("not empty", e2.isEmpty()); //$NON-NLS-1$
		assertEquals("wrong size", 0, e2.size()); //$NON-NLS-1$

		Iterator i6 = e2.iterator();
		assertFalse("extra next", i6.hasNext()); //$NON-NLS-1$
		try {
			i6.next();
			fail("next did not throw exception"); //$NON-NLS-1$
		} catch (NoSuchElementException e) {
			//expected
		}

		try {
			e2.add(me1);
			fail("add did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			e2.addAll(Arrays.asList(new Map.Entry[] {me2, me4}));
			fail("addAll did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			e1.clear();
			fail("clear did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			e1.remove(me1);
			fail("remove did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			e1.removeAll(Arrays.asList(new Map.Entry[] {me1, me2}));
			fail("removeAll did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}
		try {
			e1.retainAll(Arrays.asList(new Map.Entry[] {me1, me2}));
			fail("retainAll did not throw exception"); //$NON-NLS-1$
		} catch (UnsupportedOperationException e) {
			//expected
		}

		assertTrue("array unequal", Arrays.equals(new Map.Entry[] {me1, me3, me2, me4}, e1.toArray())); //$NON-NLS-1$
		assertTrue("array unequal", Arrays.equals(new Map.Entry[] {me1, me3, me2, me4}, e1.toArray(new Map.Entry[4]))); //$NON-NLS-1$

	}
}
