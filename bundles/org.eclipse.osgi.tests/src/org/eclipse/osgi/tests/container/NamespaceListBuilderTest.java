/*******************************************************************************
 * Copyright (c) 2021 Hannes Wellmann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.container;

import static org.eclipse.osgi.tests.container.NamespaceListTest.build;
import static org.eclipse.osgi.tests.container.NamespaceListTest.builderAddAfterLastMatch;
import static org.eclipse.osgi.tests.container.NamespaceListTest.builderAddAll;
import static org.eclipse.osgi.tests.container.NamespaceListTest.builderCreate;
import static org.eclipse.osgi.tests.container.NamespaceListTest.builderRemoveElementsOfNamespaceIf;
import static org.eclipse.osgi.tests.container.NamespaceListTest.builderRemoveNamespaceIf;
import static org.eclipse.osgi.tests.container.NamespaceListTest.createEmptyNamespaceList;
import static org.eclipse.osgi.tests.container.NamespaceListTest.getList;
import static org.eclipse.osgi.tests.container.NamespaceListTest.newNamespace;
import static org.eclipse.osgi.tests.container.NamespaceListTest.populate;
import static org.eclipse.osgi.tests.container.NamespaceListTest.randomListSort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.function.Function;
import org.eclipse.osgi.tests.container.NamespaceListTest.NamespaceElement;
import org.junit.Test;

public class NamespaceListBuilderTest {

	@Test
	public void testCreate() throws Exception {

		Collection<NamespaceElement> builder = builderCreate();

		assertTrue("Builder is not initially empty", builder.isEmpty());
	}

	@Test
	public void testIteratorsElementSequence_multipleNamespace() throws Exception {
		List<NamespaceElement> elements = populate(4, 3);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		Iterator<NamespaceElement> iterator = builder.iterator();

		for (int i = 0; i < 12; i++) {
			assertTrue(iterator.hasNext());
			assertSame(elements.get(i), iterator.next());
		}
		assertFalse(iterator.hasNext());
	}

	@Test
	public void testIteratorsElementSequence_oneNamespace() throws Exception {
		List<NamespaceElement> elements = populate(3, "ns-1");

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		Iterator<NamespaceElement> iterator = builder.iterator();

		for (int i = 0; i < 3; i++) {
			assertTrue(iterator.hasNext());
			assertSame(elements.get(i), iterator.next());
		}
		assertFalse(iterator.hasNext());
	}

	@Test
	public void testIteratorsElementSequence_empty() throws Exception {

		Collection<NamespaceElement> builder = builderCreate();

		Iterator<NamespaceElement> iterator = builder.iterator();

		assertFalse(iterator.hasNext());
	}

	@Test
	public void testIteratorsElementSequence_iterationBeyoundEnd_NoSuchElementException() throws Exception {
		List<NamespaceElement> elements = populate(2, 3);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		Iterator<NamespaceElement> iterator = builder.iterator();

		for (int i = 0; i < 6; i++) {
			assertTrue(iterator.hasNext());
			assertSame(elements.get(i), iterator.next());
		}
		assertFalse(iterator.hasNext());
		assertThrows(NoSuchElementException.class, iterator::next);
	}

	@Test
	public void testIteratorsElementSequence_iterationBeyoundEndOfEmptyBuilder_NoSuchElementException()
			throws Exception {

		Collection<NamespaceElement> builder = builderCreate();

		Iterator<NamespaceElement> iterator = builder.iterator();

		assertFalse(iterator.hasNext());
		assertThrows(NoSuchElementException.class, iterator::next);
	}

	@Test
	public void testIteratorRemove_removeOneElement() throws Exception {

		List<NamespaceElement> elements = populate(4, 3);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		Iterator<NamespaceElement> iterator = builder.iterator();

		for (int i = 0; i < 6; i++) {
			iterator.next();
		}
		iterator.remove();

		elements.remove(5);

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testIteratorRemove_RemoveAllElements() throws Exception {

		List<NamespaceElement> elements = populate(4, 3);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		Iterator<NamespaceElement> iterator = builder.iterator();

		for (int i = 0; i < 12; i++) {
			iterator.next();
			iterator.remove();
		}

		assertStricEqualContent(builder, Collections.emptyList());
	}

	@Test
	public void testIteratorRemove_RemoveTwice_IllegalStateException() throws Exception {

		List<NamespaceElement> elements = populate(4, 3);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		Iterator<NamespaceElement> iterator = builder.iterator();

		iterator.next();
		iterator.remove();
		assertThrows(IllegalStateException.class, iterator::remove);

		elements.remove(0);

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testIteratorRemove_NextNotCalled_IllegalStateException() throws Exception {

		List<NamespaceElement> elements = populate(1, 1);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		Iterator<NamespaceElement> iterator = builder.iterator();

		assertThrows(IllegalStateException.class, iterator::remove);

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testClear() throws Exception {
		List<NamespaceElement> elements = populate(3, 2);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		Object namespaceList = build(builder);

		builder.clear();

		assertEquals(0, builder.size());
		assertIterationOrderEquals(Collections.emptyList(), builder);

		// assert that a previously build list is not affected

		assertEquals(getList(namespaceList, null), elements);
		assertEquals(getList(namespaceList, "ns-0"), elements.subList(0, 3));
		assertEquals(getList(namespaceList, "ns-1"), elements.subList(3, 6));
	}

	// --- test addition ---

	@Test
	public void testAdd_singleElement() throws Exception {
		NamespaceElement e = new NamespaceElement(1, "ns-1");

		Collection<NamespaceElement> builder = builderCreate();

		builder.add(e);

		assertEquals(Collections.singletonList(e), getList(build(builder), "ns-1"));

		assertStricEqualContent(builder, Collections.singletonList(e));
	}

	@Test
	public void testAdd_multipleElements() throws Exception {
		List<NamespaceElement> elements = populate(4, 3);

		Collection<NamespaceElement> builder = builderCreate();

		for (NamespaceElement element : elements) {
			builder.add(element);
		}

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testAddAll_namespaceSortedList() throws Exception {
		List<NamespaceElement> elements = populate(4, 7);

		Collection<NamespaceElement> builder = builderCreate();

		builder.addAll(elements);

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testAddAll_randomlySortedList() throws Exception {
		List<NamespaceElement> elements = populate(4, 7);
		randomListSort(elements);

		Collection<NamespaceElement> builder = builderCreate();

		builder.addAll(elements);

		assertEqualContent(builder, elements);
	}

	@Test
	public void testAddAll_emptyList() throws Exception {

		Collection<NamespaceElement> builder = builderCreate();

		builder.addAll(Collections.emptyList());

		assertEqualContent(builder, Collections.emptyList());
	}

	@Test
	public void testAddAll_namespaceList() throws Exception {
		List<NamespaceElement> elements = populate(5, 5);

		Object namespaceList = newNamespace(elements);

		Collection<NamespaceElement> builder = builderCreate();

		builderAddAll(builder, namespaceList);

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testAddAll_emptyNamespaceList() throws Exception {

		Object namespaceList = createEmptyNamespaceList(NamespaceElement::getNamespace);

		Collection<NamespaceElement> builder = builderCreate();

		builderAddAll(builder, namespaceList);

		assertStricEqualContent(builder, Collections.emptyList());
	}

	@Test
	public void testAddAfterLastMatch_matchUpUntilTheMiddle() throws Exception {
		List<NamespaceElement> elements = populate(4, "ns-0");

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		NamespaceElement element = new NamespaceElement(5, "ns-0");
		builderAddAfterLastMatch(builder, element, e -> e.id < 2);

		elements.add(2, element);
		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testAddAfterLastMatch_allElementsMatches() throws Exception {
		List<NamespaceElement> elements = populate(4, "ns-0");

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		NamespaceElement element = new NamespaceElement(5, "ns-0");
		builderAddAfterLastMatch(builder, element, e -> true);

		elements.add(4, element);
		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testAddAfterLastMatch_noMatch() throws Exception {
		List<NamespaceElement> elements = populate(4, "ns-0");

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		NamespaceElement element = new NamespaceElement(5, "ns-0");
		builderAddAfterLastMatch(builder, element, e -> e.id > 100);

		elements.add(0, element);
		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testAddAfterLastMatch_emptyNamespaceList() throws Exception {
		Collection<NamespaceElement> builder = builderCreate();

		NamespaceElement element = new NamespaceElement(5, "ns-0");
		builderAddAfterLastMatch(builder, element, e -> e.id < 2);

		assertStricEqualContent(builder, Collections.singletonList(element));
	}

	// --- test removal ---

	@Test
	public void testRemove_elementIsOneOfMultipleOfNamespace() throws Exception {
		List<NamespaceElement> elements = populate(4, 4);
		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		assertTrue(builder.remove(new NamespaceElement(2, "ns-0")));

		elements.remove(2);
		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testRemove_onlyElementOfNamspace() throws Exception {
		Collection<NamespaceElement> builder = builderCreate();
		builder.add(new NamespaceElement(3, "ns-0"));

		assertTrue(builder.remove(new NamespaceElement(3, "ns-0")));

		assertStricEqualContent(builder, Collections.emptyList());
	}

	@Test
	public void testRemove_elementNotContainedInNamespaceList() throws Exception {
		List<NamespaceElement> elements = populate(4, 3);
		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		assertFalse(builder.remove(new NamespaceElement(100, "ns-0")));

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testRemove_elementWithNotPresentNamespace() throws Exception {
		List<NamespaceElement> elements = populate(4, 3);
		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		assertFalse(builder.remove(new NamespaceElement(1, "ns-100")));

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testRemove_emptyBuilder() throws Exception {
		Collection<NamespaceElement> builder = builderCreate();

		assertFalse(builder.remove(new NamespaceElement(3, "ns-0")));

		assertStricEqualContent(builder, Collections.emptyList());
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void testRemove_argumentOfOtherClass() throws Exception {
		List<NamespaceElement> elements = populate(2, 3);
		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		assertFalse(builder.remove("someString"));

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testRemoveAll_multipleElementsInMultipleNamsespaces() throws Exception {
		List<NamespaceElement> elements = populate(4, 5);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		List<NamespaceElement> toRemove = new ArrayList<>();
		toRemove.add(new NamespaceElement(1, "ns-0")); // has total index 1
		toRemove.add(new NamespaceElement(2, "ns-1")); // has total index 6
		toRemove.add(new NamespaceElement(0, "ns-1")); // has total index 4
		toRemove.add(new NamespaceElement(3, "ns-3")); // has total index 15
		toRemove.add(new NamespaceElement(2, "ns-3")); // has total index 14

		assertTrue(builder.removeAll(toRemove));

		elements.remove(15);
		elements.remove(14);
		elements.remove(6);
		elements.remove(4);
		elements.remove(1);

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testRemoveAll_multipleElementsInMultipleNamsespacesAndSomeNotPresent() throws Exception {
		List<NamespaceElement> elements = populate(4, 5);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		List<NamespaceElement> toRemove = new ArrayList<>();
		toRemove.add(new NamespaceElement(1, "ns-0")); // has total index 1
		toRemove.add(new NamespaceElement(2, "ns-1")); // has total index 6
		toRemove.add(new NamespaceElement(100, "ns-2")); // not present
		toRemove.add(new NamespaceElement(0, "ns-1")); // has total index 4
		toRemove.add(new NamespaceElement(3, "ns-3")); // has total index 15
		toRemove.add(new NamespaceElement(100, "ns-3")); // not present
		toRemove.add(new NamespaceElement(2, "ns-3")); // has total index 14
		toRemove.add(new NamespaceElement(100, "ns-3")); // not present
		toRemove.add(new NamespaceElement(1, "ns-100")); // not present

		assertTrue(builder.removeAll(toRemove));

		elements.remove(15);
		elements.remove(14);
		elements.remove(6);
		elements.remove(4);
		elements.remove(1);

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testRemoveAll_listOfAllElementsInBuilder() throws Exception {
		List<NamespaceElement> elements = populate(4, 5);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		assertTrue(builder.removeAll(elements));

		assertStricEqualContent(builder, Collections.emptyList());
	}

	@Test
	public void testRemoveAll_emptyList() throws Exception {
		List<NamespaceElement> elements = populate(4, 5);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		assertFalse(builder.removeAll(Collections.emptyList()));

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testRemoveAll_emptyBuilder() throws Exception {
		Collection<NamespaceElement> builder = builderCreate();

		assertFalse(builder.removeAll(populate(4, 3)));

		assertStricEqualContent(builder, Collections.emptyList());
	}

	@Test
	@SuppressWarnings("unlikely-arg-type")
	public void testRemoveAll_argumentListOfOtherClass() throws Exception {
		List<NamespaceElement> elements = populate(2, 3);
		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		List<String> listOfOtherElements = Arrays.asList("someString", "other");
		assertFalse(builder.removeAll(listOfOtherElements));

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testRemoveNamespaceIf_NamespaceMatches() throws Exception {
		List<NamespaceElement> elements = populate(4, 5);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		Collection<String> namespacesToRemove = Arrays.asList("ns-0", "ns-2");

		builderRemoveNamespaceIf(builder, namespacesToRemove::contains);

		elements.subList(8, 12).clear();
		elements.subList(0, 4).clear();

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testRemoveNamespaceIf_NamespaceMatchesExpunging() throws Exception {
		List<NamespaceElement> elements = populate(3, 2);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		builderRemoveNamespaceIf(builder, n -> true);

		assertStricEqualContent(builder, Collections.emptyList());
	}

	@Test
	public void testRemoveNamespaceIf_NoNamespaceMatches() throws Exception {
		List<NamespaceElement> elements = populate(3, 3);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		builderRemoveNamespaceIf(builder, "ns-100"::equals);

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testRemoveIf_multipleMatches() throws Exception {
		List<NamespaceElement> elements = populate(5, 5);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		builder.removeIf(e -> e.id % 3 == 0 || "ns-1".equals(e.namespace));

		elements.remove(23); // first and third of 20-25
		elements.remove(20);

		elements.remove(18); // first and third of 15-20
		elements.remove(15);

		elements.remove(13); // first and third of 10-15
		elements.remove(10);

		elements.subList(5, 10).clear(); // all of 5-10

		elements.remove(3); // first and third of 0-5
		elements.remove(0);

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testRemoveIf_allMatches() throws Exception {
		List<NamespaceElement> elements = populate(4, 3);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		builder.removeIf(e -> true);

		assertStricEqualContent(builder, Collections.emptyList());
	}

	@Test
	public void testRemoveIf_noMatches() throws Exception {
		List<NamespaceElement> elements = populate(4, 3);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		builder.removeIf(e -> false);

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testRemoveElementsOfNamespaceIf_multipleMatches() throws Exception {
		List<NamespaceElement> elements = populate(4, 2);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		builderRemoveElementsOfNamespaceIf(builder, "ns-0", e -> e.id == 1 || e.id == 2);

		elements.remove(2);
		elements.remove(1);

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testRemoveElementsOfNamespaceIf_allElementsOfNamespaceMatch() throws Exception {
		List<NamespaceElement> elements = populate(4, 2);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		builderRemoveElementsOfNamespaceIf(builder, "ns-0", e -> e.id == 1 || e.id == 2);

		elements.remove(2);
		elements.remove(1);

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testRemoveElementsOfNamespaceIf_allElementsMatch() throws Exception {
		List<NamespaceElement> elements = populate(4, 1);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		builderRemoveElementsOfNamespaceIf(builder, "ns-0", e -> true);

		assertStricEqualContent(builder, Collections.emptyList());
	}

	@Test
	public void testRemoveElementsOfNamespaceIf_noMatch() throws Exception {
		List<NamespaceElement> elements = populate(4, 1);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		builderRemoveElementsOfNamespaceIf(builder, "ns-0", e -> false);

		assertStricEqualContent(builder, elements);
	}

	@Test
	public void testRemoveElementsOfNamespaceIf_namespaceNotPresent() throws Exception {
		List<NamespaceElement> elements = populate(4, 1);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		builderRemoveElementsOfNamespaceIf(builder, "ns-100", e -> true);

		assertStricEqualContent(builder, elements);
	}

	// --- test build ---

	@Test
	public void testBuild_notEmptyBuilder() throws Exception {
		List<NamespaceElement> elements = populate(4, 2);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		Object namespaceList1 = build(builder);

		assertEquals(elements, getList(namespaceList1, null));
		assertEquals(elements.subList(0, 4), getList(namespaceList1, "ns-0"));
		assertEquals(elements.subList(4, 8), getList(namespaceList1, "ns-1"));

		Object namespaceList2 = build(builder);

		assertEquals(elements, getList(namespaceList1, null));
		assertEquals(elements.subList(0, 4), getList(namespaceList1, "ns-0"));
		assertEquals(elements.subList(4, 8), getList(namespaceList1, "ns-1"));

		assertEquals(elements, getList(namespaceList2, null));
		assertEquals(elements.subList(0, 4), getList(namespaceList2, "ns-0"));
		assertEquals(elements.subList(4, 8), getList(namespaceList2, "ns-1"));
	}

	@Test
	public void testBuild_emptyBuilder() throws Exception {
		Collection<NamespaceElement> builder = builderCreate();

		Object namespaceList = build(builder);

		assertEquals(Collections.emptyList(), getList(namespaceList, null));
	}

	@Test
	public void testBuild_subsequentModificationOfBuilder() throws Exception {
		List<NamespaceElement> elements = populate(4, 2);

		Collection<NamespaceElement> builder = builderCreate();
		builder.addAll(elements);

		Object namespaceList1 = build(builder);

		assertEquals(elements, getList(namespaceList1, null));
		assertEquals(elements.subList(0, 4), getList(namespaceList1, "ns-0"));
		assertEquals(elements.subList(4, 8), getList(namespaceList1, "ns-1"));

		List<NamespaceElement> additionalElements = populate(1, 3);
		builder.addAll(additionalElements);

		List<NamespaceElement> newElements = new ArrayList<>(elements);

		newElements.add(8, additionalElements.get(2));
		newElements.add(8, additionalElements.get(1));
		newElements.add(4, additionalElements.get(0));

		// assert the first list build is not modified
		assertEquals(elements, getList(namespaceList1, null));
		assertEquals(elements.subList(0, 4), getList(namespaceList1, "ns-0"));
		assertEquals(elements.subList(4, 8), getList(namespaceList1, "ns-1"));

		// asser the new content of the builder is as expected
		assertStricEqualContent(builder, newElements);
	}

	// --- utility methods ---

	private static void assertStricEqualContent(Collection<NamespaceElement> builder,
			List<NamespaceElement> expectedElements) throws Exception {
		assertStricEqualContent(builder, expectedElements, NamespaceElement::getNamespace);
	}

	private static <E> void assertStricEqualContent(Collection<E> builder, List<E> expectedElements,
			Function<E, String> getNamespace) throws Exception {
		// test all properties of the builder and its build list in order to ensure they
		// are updated correctly

		assertIterationOrderEquals(expectedElements, builder);
		assertEquals(expectedElements.size(), builder.size());

		Map<String, List<E>> namespaceElements = getNamespaceElements(expectedElements, getNamespace);
		Object namespaceList = build(builder);

		assertEquals(expectedElements.size(), getList(namespaceList, null).size());

		for (Entry<String, List<E>> entry : namespaceElements.entrySet()) {
			String namespace = entry.getKey();
			List<E> elements = entry.getValue();
			assertEquals(elements, getList(namespaceList, namespace));
		}

		assertEquals(expectedElements, getList(namespaceList, null));
	}

	private static void assertEqualContent(Collection<NamespaceElement> builder,
			List<NamespaceElement> expectedElements) throws Exception {
		// test all properties of the builder and its build list in order to ensure they
		// are updated correctly

		assertContentEquals(expectedElements, builder);
		assertEquals(expectedElements.size(), builder.size());

		Map<String, List<NamespaceElement>> namespaceElements = getNamespaceElements(expectedElements,
				NamespaceElement::getNamespace);
		Object namespaceList = build(builder);

		assertEquals(expectedElements.size(), getList(namespaceList, null).size());

		for (Entry<String, List<NamespaceElement>> entry : namespaceElements.entrySet()) {
			String namespace = entry.getKey();
			List<NamespaceElement> elements = entry.getValue();
			assertContentEquals(elements, getList(namespaceList, namespace));
		}

		assertContentEquals(expectedElements, getList(namespaceList, null));
	}

	private static <E> void assertIterationOrderEquals(Collection<E> expected, Collection<E> actual) {
		// instead of comparing the iterators, simply compare List-copies. They reflect
		// the iteration order.
		assertEquals(new ArrayList<>(expected), new ArrayList<>(actual));
	}

	private static <E> void assertContentEquals(List<E> expected, Collection<E> actual) {
		assertEquals(new HashSet<>(expected), new HashSet<>(actual));
	}

	private static <E> Map<String, List<E>> getNamespaceElements(List<E> elements, Function<E, String> getNamespace) {
		Map<String, List<E>> namespaceElements = new HashMap<>();
		for (E element : elements) {
			namespaceElements.computeIfAbsent(getNamespace.apply(element), n -> new ArrayList<>()).add(element);
		}
		return namespaceElements;
	}
}
