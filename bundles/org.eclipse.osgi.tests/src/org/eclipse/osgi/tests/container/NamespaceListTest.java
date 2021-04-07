/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.osgi.tests.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Using reflection because to avoid exporting internals.
 */
public class NamespaceListTest extends AbstractTest {
	static final Method getList;
	static final Method isEmpty;
	static final Method getNamespaceIndex;
	static final Method copyList;
	static final Constructor<?> newNamespaceList;
	static {
		try {
			Class<?> namespaceList = Bundle.class.getClassLoader()
					.loadClass("org.eclipse.osgi.internal.container.NamespaceList");
			getList = namespaceList.getMethod("getList", String.class);
			isEmpty = namespaceList.getMethod("isEmpty");
			getNamespaceIndex = namespaceList.getMethod("getNamespaceIndex", String.class);
			copyList = namespaceList.getMethod("copyList");
			newNamespaceList = namespaceList.getConstructor(List.class, Function.class);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}
	static class NamespaceElement {
		final int id;
		final String namespace;

		public NamespaceElement(int id, String namespace) {
			super();
			this.id = id;
			this.namespace = namespace;
		}

		public int getId() {
			return id;
		}

		public String getNamespace() {
			return namespace;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof NamespaceElement) {
				NamespaceElement other = (NamespaceElement) o;
				return this.id == other.id && this.namespace.equals(other.namespace);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return namespace.hashCode() ^ id;
		}

		@Override
		public String toString() {
			return namespace + ':' + id;
		}
	}

	static final Function<NamespaceElement, String> getNamespaceFunc = (Function<NamespaceElement, String>) NamespaceElement::getNamespace;
	Object newNamespace(List<NamespaceElement> elements) throws Exception {
		return newNamespaceList.newInstance(elements, getNamespaceFunc);
	}
	@Test
	public void testIsEmpty() throws Exception {
		Object namespaceList = newNamespace(Collections.emptyList());
		assertTrue("List is not empty.", isEmpty(namespaceList));

		List<NamespaceElement> elements = new ArrayList<>();
		elements.add(new NamespaceElement(0, "ns1"));
		namespaceList = newNamespace(elements);
		assertFalse("List is empty.", isEmpty(namespaceList));

		elements.add(new NamespaceElement(1, "ns2"));
		namespaceList = newNamespace(elements);
		assertFalse("List is empty.", isEmpty(namespaceList));
	}

	private boolean isEmpty(Object namespaceList) throws Exception {
		return (boolean) isEmpty.invoke(namespaceList);
	}

	private List<NamespaceElement> getList(Object namespaceList, String namespace) throws Exception {
		return (List<NamespaceElement>) getList.invoke(namespaceList, namespace);
	}

	@Test
	public void testGetList() throws Exception {
		Object namespaceList = newNamespace(Collections.emptyList());

		List<NamespaceElement> list = getList(namespaceList, null);
		assertTrue("List is not empty.", list.isEmpty());
		failAdd(list);

		list = getList(namespaceList, "ns-1");
		assertTrue("List is not empty.", list.isEmpty());
		failAdd(list);

		List<NamespaceElement> elements = populate(5, 10);

		namespaceList = newNamespace(elements);

		for (int i = 0; i < 10; i++) {
			list = getList(namespaceList, "ns-" + i);
			failAdd(list);
			assertEquals("Wrong list.", populate(5, "ns-" + i), list);
		}

		list = getList(namespaceList, null);
		failAdd(list);
		assertEquals("Wrong list.", populate(5, 10), list);
	}

	@Test
	public void testGetNamespaceIndex() throws Exception {
		Object namespaceList = newNamespace(Collections.emptyList());
		assertNull("Unexpected index.", getNamespaceIndex(namespaceList, "ns-0"));

		List<NamespaceElement> elements = populate(21, 13);

		namespaceList = newNamespace(elements);
		Entry<Integer, Integer> nsIndex = getNamespaceIndex(namespaceList, "ns-0");
		assertNotNull("Expected an index", nsIndex);
		checkIndex(nsIndex, 0, 21);

		nsIndex = getNamespaceIndex(namespaceList, "ns-12");
		assertNotNull("Expected an index", nsIndex);
		checkIndex(nsIndex, 21 * 12, 21 * 13);

		nsIndex = getNamespaceIndex(namespaceList, "ns-4");
		assertNotNull("Expected an index", nsIndex);
		checkIndex(nsIndex, 21 * 4, 21 * 5);
	}

	@Test
	public void testOutOfOrderNamespace() throws Exception {
		List<NamespaceElement> elements = populate(4, 4);
		// random sort by hashcode
		elements.sort((n1, n2) -> n1.hashCode() - n2.hashCode());
		Object namespaceList = newNamespace(elements);
		for (int i = 0; i < 4; i++) {
			List<NamespaceElement> list = getList(namespaceList, "ns-" + i);
			failAdd(list);
			// list is random order now, but should all have the same namespace
			assertEquals("Wrong number of elements", 4, list.size());
			for (NamespaceElement e : list) {
				assertEquals("Wrong namespace", "ns-" + i, e.getNamespace());
			}
		}
	}

	@Test
	public void testCopyList() throws Exception {
		Object namespaceList = newNamespace(Collections.emptyList());
		List<NamespaceElement> copy = copyList(namespaceList);
		assertEquals("Wrong list.", Collections.emptyList(), copy);
		successAdd(copy);
		copy = copyList(namespaceList);
		assertEquals("Wrong list.", Collections.emptyList(), copy);

		List<NamespaceElement> elements = populate(100, 13);
		namespaceList = newNamespace(elements);
		copy = copyList(namespaceList);
		assertEquals("Wrong list.", elements, copy);
		successAdd(copy);
		copy = copyList(namespaceList);
		assertEquals("Wrong list.", elements, copy);
	}

	private List<NamespaceElement> copyList(Object namespaceList) throws Exception {
		return (List<NamespaceElement>) copyList.invoke(namespaceList);
	}

	private void checkIndex(Entry<Integer, Integer> nsIndex, int start, int end) {
		assertEquals("Unexpected Start", start, (int) nsIndex.getKey());
	}

	private Entry<Integer, Integer> getNamespaceIndex(Object namespaceList, String namespace) throws Exception {
		return (Entry<Integer, Integer>) getNamespaceIndex.invoke(namespaceList, namespace);
	}

	private void failAdd(List<NamespaceElement> list) {
		try {
			list.add(new NamespaceElement(0, "ns"));
			fail("Should fail to modify list");
		} catch (UnsupportedOperationException e) {
			// expected
		}
	}

	private void successAdd(List<NamespaceElement> list) {
		try {
			list.add(new NamespaceElement(0, "ns"));
		} catch (UnsupportedOperationException e) {
			fail("Should not fail to modify list");
		}
	}

	private List<NamespaceElement> populate(int numElementsPerNS, int numNS) {
		ArrayList<NamespaceElement> elements = new ArrayList<>(numElementsPerNS * numNS);
		for (int namespace = 0; namespace < numNS; namespace++) {
			for (int element = 0; element < numElementsPerNS; element++) {
				elements.add(new NamespaceElement(element, "ns-" + namespace));
			}
		}
		return elements;
	}

	private List<NamespaceElement> populate(int numElements, String namespace) {
		ArrayList<NamespaceElement> elements = new ArrayList<>(numElements);
		for (int element = 0; element < numElements; element++) {
			elements.add(new NamespaceElement(element, namespace));
		}
		return elements;
	}
}
