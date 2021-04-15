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
 *     Hannes Wellmann - Bug 573025: introduce and apply NamespaceList.Builder
 *******************************************************************************/
package org.eclipse.osgi.tests.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Using reflection because to avoid exporting internals.
 */
public class NamespaceListTest extends AbstractTest {
	static final Method NAMESPACELIST_GET_LIST;
	static final Method NAMESPACELIST_IS_EMPTY;
	static final Method NAMESPACELIST_EMPTY;
	static final Method NAMESPACELIST_CREATE_BUILDER;

	// only access fields reflectively that are not part of the Collection-API
	static final Method BUILDER_CREATE;
	static final Method BUILDER_ADD_ALL;
	static final Method BUILDER_ADD_AFTER_LAST_MATCH;
	static final Method BUILDER_REMOVE_NAMESPACE_IF;
	static final Method BUILDER_REMOVE_ELEMENTS_OF_NAMESPACE_IF;
	static final Method BUILDER_BUILD;

	static {
		try {
			ClassLoader classLoader = Bundle.class.getClassLoader();
			Class<?> namespaceList = classLoader.loadClass("org.eclipse.osgi.internal.container.NamespaceList");
			Class<?> namespaceListBuilder = classLoader
					.loadClass("org.eclipse.osgi.internal.container.NamespaceList$Builder");

			NAMESPACELIST_GET_LIST = namespaceList.getMethod("getList", String.class);
			NAMESPACELIST_IS_EMPTY = namespaceList.getMethod("isEmpty");
			NAMESPACELIST_EMPTY = namespaceList.getMethod("empty", Function.class);
			NAMESPACELIST_CREATE_BUILDER = namespaceList.getMethod("createBuilder");

			BUILDER_CREATE = namespaceListBuilder.getMethod("create", Function.class);
			BUILDER_ADD_ALL = namespaceListBuilder.getMethod("addAll", namespaceList);
			BUILDER_ADD_AFTER_LAST_MATCH = namespaceListBuilder.getMethod("addAfterLastMatch", Object.class,
					Predicate.class);
			BUILDER_REMOVE_NAMESPACE_IF = namespaceListBuilder.getMethod("removeNamespaceIf", Predicate.class);
			BUILDER_REMOVE_ELEMENTS_OF_NAMESPACE_IF = namespaceListBuilder.getMethod("removeElementsOfNamespaceIf",
					String.class, Predicate.class);
			BUILDER_BUILD = namespaceListBuilder.getMethod("build");
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	static Object newNamespace(List<NamespaceElement> elements) throws Exception {
		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);
		return build(builder);
	}

	// --- reflectively invoked methods of NamespaceList ---

	static <E> Object createEmptyNamespaceList(Function<E, String> getNamespace) throws Exception {
		return NAMESPACELIST_EMPTY.invoke(null, getNamespace);
	}

	static boolean isEmpty(Object namespaceList) throws Exception {
		return (boolean) NAMESPACELIST_IS_EMPTY.invoke(namespaceList);
	}

	static List<NamespaceElement> getList(Object namespaceList, String namespace) throws Exception {
		return (List<NamespaceElement>) NAMESPACELIST_GET_LIST.invoke(namespaceList, namespace);
	}

	static Collection<NamespaceElement> createBuilder(Object namespaceList) throws Exception {
		return (Collection<NamespaceElement>) NAMESPACELIST_CREATE_BUILDER.invoke(namespaceList);
	}

	// --- reflectively invoked non-Collection methods of NamespaceList$Builder ---

	static Collection<NamespaceElement> builderCreate() throws Exception {
		Function<NamespaceElement, String> getNamespace = NamespaceElement::getNamespace;
		return (Collection<NamespaceElement>) BUILDER_CREATE.invoke(null, getNamespace);
	}

	static <E> void builderAddAll(Collection<E> builder, Object namespaceList) throws Exception {
		BUILDER_ADD_ALL.invoke(builder, namespaceList);
	}

	static <E> void builderAddAfterLastMatch(Collection<E> builder, E e, Predicate<E> matcher) throws Exception {
		BUILDER_ADD_AFTER_LAST_MATCH.invoke(builder, e, matcher);
	}

	static <E> void builderRemoveNamespaceIf(Collection<E> builder, Predicate<String> filter) throws Exception {
		BUILDER_REMOVE_NAMESPACE_IF.invoke(builder, filter);
	}

	static <E> void builderRemoveElementsOfNamespaceIf(Collection<E> builder, String namespace, Predicate<E> filter)
			throws Exception {
		BUILDER_REMOVE_ELEMENTS_OF_NAMESPACE_IF.invoke(builder, namespace, filter);
	}

	static <E> Object build(Collection<E> builder) throws Exception {
		return BUILDER_BUILD.invoke(builder);
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
			return Objects.hash(namespace, id);
		}

		@Override
		public String toString() {
			return namespace + ':' + id;
		}
	}

	// --- tests ---

	@Test
	public void testCreateEmptyList() throws Exception {
		Object namespaceList = createEmptyNamespaceList(NamespaceElement::getNamespace);
		assertTrue("List is not empty.", isEmpty(namespaceList));
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
	public void testOutOfOrderNamespace() throws Exception {
		List<NamespaceElement> elements = populate(4, 4);
		randomListSort(elements);
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
	public void testCreateBuilder() throws Exception {

		List<NamespaceElement> elements = populate(5, 10);
		Object namespaceList = newNamespace(elements);
		Collection<NamespaceElement> builder = createBuilder(namespaceList);

		Object buildNamespaceList = build(builder);

		// The order of all elements should be maintained

		assertEquals("Builder not populated correctly", getList(buildNamespaceList, null),
				getList(namespaceList, null));
	}

	// --- uility methods ---

	private void failAdd(List<NamespaceElement> list) {
		NamespaceElement e = new NamespaceElement(0, "ns");
		assertThrows(UnsupportedOperationException.class, () -> list.add(e));
	}

	static List<NamespaceElement> populate(int numElementsPerNS, int numNS) {
		List<NamespaceElement> elements = new ArrayList<>(numElementsPerNS * numNS);
		for (int namespace = 0; namespace < numNS; namespace++) {
			for (int element = 0; element < numElementsPerNS; element++) {
				elements.add(new NamespaceElement(element, "ns-" + namespace));
			}
		}
		return elements;
	}

	static List<NamespaceElement> populate(int numElements, String namespace) {
		List<NamespaceElement> elements = new ArrayList<>(numElements);
		for (int element = 0; element < numElements; element++) {
			elements.add(new NamespaceElement(element, namespace));
		}
		return elements;
	}

	static void randomListSort(List<NamespaceElement> elements) {
		// random sort in reproducible order
		Collections.shuffle(elements, new Random(43L));
	}
}
