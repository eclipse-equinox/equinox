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
 *     Hannes Wellmann - Bug 573025 & 573026: introduce and apply NamespaceList.Builder
 *******************************************************************************/
package org.eclipse.osgi.internal.container;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import org.eclipse.osgi.container.ModuleCapability;
import org.eclipse.osgi.container.ModuleRequirement;
import org.eclipse.osgi.container.ModuleWire;

/**
 * An immutable list of elements for which each element has a namespace.
 * <p>
 * The elements are stored in a map where each key is a namespace and the
 * associated value is the list of all elements with that namespace in this
 * NamespaceList. Within one namespace the element's order is stable. Due to
 * this internal structure access to the elements of off one or all namespace(s)
 * has always constant runtime regardless of the number of namespaces present.
 * <p>
 * 
 * @param <E> the type of elements in this list, which have a name-space
 *            associated
 */
public class NamespaceList<E> {

	public final static Function<ModuleWire, String> WIRE = new Function<ModuleWire, String>() { 
		public String apply(ModuleWire wire) {
			return wire.getCapability().getNamespace();
		}
	};
	public final static Function<ModuleCapability, String> CAPABILITY = new Function<ModuleCapability, String>() {
		public String apply(ModuleCapability capability) {
			return capability.getNamespace();
		}
	};
	public final static Function<ModuleRequirement, String> REQUIREMENT = new Function<ModuleRequirement, String>() {
		public String apply(ModuleRequirement requirement) {
			return requirement.getNamespace();
		}
	};

	/**
	 * Returns an empty NamespaceList.
	 * <p>
	 * The required argument is used to derive the type of elements and if a builder
	 * is created from the returned list.
	 * </p>
	 * 
	 * @param <E>          the type of elements in the NamespaceList
	 * @param getNamespace the function to compute the namespace of an element
	 * @return an empty NamespaceList
	 */
	public static <E> NamespaceList<E> empty(Function<E, String> getNamespace) {
		return new NamespaceList<>(getNamespace, Collections.emptyMap(), Collections.emptyList());
	}

	private final List<E> elements;
	private final Map<String, List<E>> namespaces;
	private final Function<E, String> getNamespace;

	NamespaceList(Function<E, String> getNamespace, Map<String, List<E>> namespaces, List<E> fullList) {
		this.getNamespace = getNamespace;
		this.namespaces = namespaces;
		this.elements = fullList;
	}

	Map<String, List<E>> namespaces() {
		return namespaces;
	}

	/**
	 * Returns {@code true} if this NamespaceList contains no elements.
	 * 
	 * @return {@code true} if this list contains no elements
	 */
	public boolean isEmpty() {
		return elements.isEmpty();
	}

	/**
	 * Returns an immutable list of elements with the specified namespace.
	 * <p>
	 * An empty list is returned if there are no elements with the specified
	 * namespace. For the {@code null} namespace the elements of all namespaces are
	 * returned as flat.
	 * </p>
	 * 
	 * @param namespace the namespace of the elements to return. May be {@code null}
	 * @return The list of elements found
	 */
	public List<E> getList(String namespace) {
		if (namespace == null) {
			return elements;
		}
		return namespaces.getOrDefault(namespace, Collections.emptyList());
	}

	/**
	 * Returns a new {@link Builder NamespaceList.Builder} that contains all
	 * elements of this NamespaceList.
	 * <p>
	 * The returned builder uses the same function to compute the namespace of an
	 * element like this NamespaceList.
	 * </p>
	 * 
	 * @return a new builder containing all elements of this list
	 */
	public Builder<E> createBuilder() {
		Builder<E> builder = Builder.create(getNamespace);
		builder.addAll(this);
		return builder;
	}

	/**
	 * A reusable builder to create {@link NamespaceList NamespaceLists}.
	 * 
	 * @param <E> the type of elements in this builder
	 * @author Hannes Wellmann
	 */
	public static class Builder<E> extends AbstractCollection<E> {

		/**
		 * Returns a new {@link Builder NamespaceList.Builder} that uses the specified
		 * function to compute the namespace of its elements.
		 * 
		 * @param <E>          the type of elements in this builder
		 * @param getNamespace the function to compute the namespace of an element
		 * @return a new builder
		 */
		public static <E> Builder<E> create(Function<E, String> getNamespace) {
			return new Builder<>(getNamespace, 3);
		}

		private final Function<E, String> getNamespace;
		private LinkedHashMap<String, List<E>> namespaceElements;
		private int size = 0;

		private List<E> lastBuildElements;

		private Builder(Function<E, String> getNamespace, int expectedNamespaces) {
			this.getNamespace = getNamespace;
			this.namespaceElements = new LinkedHashMap<>(expectedNamespaces * 4 / 3 + 1);
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public Iterator<E> iterator() {
			prepareModification();

			final Iterator<? extends List<E>> outer = namespaceElements.values().iterator();
			return new Iterator<E>() {
				Iterator<E> inner = Collections.emptyIterator();
				List<E> lastInnerList = null;

				@Override
				public boolean hasNext() {
					while (!inner.hasNext() && outer.hasNext()) {
						lastInnerList = outer.next();
						inner = lastInnerList.iterator();
					}
					return inner.hasNext();
				}

				public E next() {
					if (!hasNext()) {
						throw new NoSuchElementException();
					}
					return inner.next();
				}

				@Override
				@SuppressWarnings("synthetic-access")
				public void remove() {
					inner.remove();
					Builder.this.size--;
					if (lastInnerList.isEmpty()) {
						outer.remove();
					}
				}
			};
		}

		@Override
		public void clear() {
			namespaceElements = new LinkedHashMap<>(); // could have been build before, so map must not be cleared
			lastBuildElements = null;
			size = 0;
		}

		/**
		 * Returns an immutable list of elements with the specified namespace in this
		 * builder.
		 * <p>
		 * An empty list is returned if there are no elements with the specified
		 * namespace. For the {@code null} namespace the elements of all namespaces are
		 * returned as flat.
		 * </p>
		 * 
		 * @param namespace the namespace of the elements to return. May be {@code null}
		 * @return the list of element with the specified namespace
		 */
		public List<E> getNamespaceElements(String namespace) {
			if (namespace == null) {
				List<E> list = new ArrayList<>(size);
				for (List<E> es : namespaceElements.values()) {
					list.addAll(es);
				}
				return Collections.unmodifiableList(list);
			}
			List<E> namespaceList = namespaceElements.get(namespace);
			return namespaceList != null ? Collections.unmodifiableList(new ArrayList<>(namespaceList))
					: Collections.emptyList();
		}

		/**
		 * Returns a new builder whose content is the result of applying the specified
		 * transformation to each element of this builder.
		 * <p>
		 * It is assumed that the transformation does not change the element's
		 * namespace, so the namespace of original and transformed element are the same!
		 * This builder is not modified.
		 * </p>
		 * 
		 * @param <R>             the type of elements in the returned builder
		 * @param transformation  the transformation applied to each element
		 * @param newGetNamespace the function to compute the namespace of a transformed
		 *                        element
		 * @return a new builder containing the result of applying the transformation to
		 *         each element in this builder
		 */
		public <R> Builder<R> transformIntoCopy(Function<E, R> transformation, Function<R, String> newGetNamespace) {
			Builder<R> transformedBuilder = new Builder<>(newGetNamespace, this.namespaceElements.size());
			transformedBuilder.size = this.size;
                        for (Map.Entry<String, List<E>> entry : namespaceElements.entrySet()) {
				List<E> es = entry.getValue();
				List<R> transformedElements = new ArrayList<>(es.size());
				for (E e : es) {
					transformedElements.add(transformation.apply(e));
				}
				transformedBuilder.namespaceElements.put(entry.getKey(), transformedElements);
			}
			return transformedBuilder;
		}

		// --- addition ---

		@Override
		public boolean add(E e) {
			prepareModification();

			String namespace = getNamespace.apply(e);
			getNamespaceList(namespace).add(e);
			this.size++;
			return true;
		}

		@Override
		public boolean addAll(Collection<? extends E> c) {
			if (c.isEmpty()) {
				return false;
			}
			prepareModification();

			if (c instanceof Builder) {
				@SuppressWarnings("unchecked")
				Builder<E> builder = (Builder<E>) c;
				return addAll(builder.namespaceElements);
			}
			String currentNamespace = null; // $NON-NLS-1$
			List<E> currentNamespaceList = null;
			for (E e : c) {
				String namespace = getNamespace.apply(e);
				// optimization if elements are already grouped per namespace
				if (currentNamespace == null || !currentNamespace.equals(namespace)) {
					currentNamespace = namespace;
					currentNamespaceList = getNamespaceList(namespace);
				}
				currentNamespaceList.add(e);
			}
			this.size += c.size();
			return true;
		}

		/**
		 * Adds all elements in the specified NamespaceList to this builder.
		 * 
		 * @param list the NamespaceList containing elements to be added
		 * @return true if any element was added to this builder
		 */
		public boolean addAll(NamespaceList<E> list) {
			if (list.isEmpty()) {
				return false;
			}
			prepareModification();

			return addAll(list.namespaces());
		}

		private boolean addAll(Map<String, List<E>> perNamespaceElements) {
			for (Map.Entry<String, List<E>> entry : perNamespaceElements.entrySet()) {
				List<E> es = entry.getValue();
				getNamespaceList(entry.getKey()).addAll(es);
				this.size += es.size();
			}
			return true;
		}

		private List<E> getNamespaceList(String namespace) {
			return namespaceElements.computeIfAbsent(namespace, new Function<String, List<E>>() {
				public List<E> apply(String n) {
					return new ArrayList<>();
				}
			});
		}

		/**
		 * Appends all elements in the specified NamespaceList to this builder that
		 * satisfy both specified predicates for themselves and their namespace.
		 * <p>
		 * For an element to be added both predicates, the one for the namespace as well
		 * as the one for the element itself must be satisfied.
		 * </p>
		 * 
		 * @param list            the NamespaceList containing elements to be added
		 * @param namespaceFilter the predicate that returns true for namespaces whose
		 *                        elements should not be excluded from being added
		 * @param elementFilter   the predicate that returns true for elements to be
		 *                        added
		 * 
		 */
		public void addAllFiltered(NamespaceList<E> list, Predicate<? super String> namespaceFilter,
				Predicate<? super E> elementFilter) {

			addAllFilteredAfterLastMatch(list, namespaceFilter, elementFilter, null);
		}

		/**
		 * Inserts all elements in the specified NamespaceList to this builder that
		 * satisfy both specified predicates for themselves and their namespace, after
		 * the last element in this builder for the corresponding namespace that
		 * satisfies the specified bi-predicate together with the corresponding element
		 * to be added.
		 * <p>
		 * For an element to be added both predicates, the one for the namespace as well
		 * as the one for the element itself must be satisfied. If both predicates are
		 * satisfied by an element of the specified list, it is added after the <em>
		 * last</em> element with the same namespace in this builder that satisfies the
		 * specified bi-predicate together with the element to add.
		 * </p>
		 * 
		 * @param list             the NamespaceList containing elements to be added
		 * @param namespaceFilter  the predicate that returns true for namespaces whose
		 *                         elements should not be excluded from being added
		 * @param elementFilter    the predicate that returns true for elements to be
		 *                         added
		 * @param insertionMatcher the bi-predicate whose first argument is the element
		 *                         to add and second argument is an element for the same
		 *                         namespace in this builder, which returns true if the
		 *                         element to add can be added after this builder's
		 *                         element
		 */
		public void addAllFilteredAfterLastMatch(NamespaceList<E> list, Predicate<? super String> namespaceFilter,
				Predicate<? super E> elementFilter, BiPredicate<E, E> insertionMatcher) {
			if (list.isEmpty()) {
				return;
			}
			prepareModification();

			for (Map.Entry<String, List<E>> entry : list.namespaces().entrySet()) {
				String namespace = entry.getKey();
				if (namespaceFilter.test(namespace)) {
					List<E> targetList = getNamespaceList(namespace);
					List<E> elementsToAdd = entry.getValue();
					for (E toAdd : elementsToAdd) {
						if (elementFilter.test(toAdd)) {
							if (insertionMatcher == null) {
								targetList.add(toAdd);
							} else {
								addAfterLastMatch(toAdd, targetList, new Predicate<E>() {
									public boolean test(E e) {
										return insertionMatcher.test(toAdd, e);
									}
								});
							}
							this.size++;
						}
					}
					if (targetList.isEmpty()) { // maybe no elements are added
						namespaceElements.remove(namespace);
					}
				}
			}
		}

		private void addAfterLastMatch(E e, List<E> list, Predicate<E> matcher) {
			for (int i = list.size() - 1; 0 <= i; i--) {
				if (matcher.test(list.get(i))) {
					list.add(i + 1, e);
					return;
				}
			}
			list.add(0, e);
		}

		// --- removal ---

		@Override
		public boolean remove(Object o) {
			@SuppressWarnings("unchecked")
			E e = (E) o;
			String namespace;
			try {
				namespace = getNamespace.apply(e);
			} catch (ClassCastException ex) {
				return false; // e does not seem to be of type E after all
			}
			prepareModification();

			int sizeBefore = this.size;
			removeNamespaceElement(namespace, e);
			return this.size < sizeBefore;
		}

		private void removeNamespaceElement(String namespace, E element) {
			namespaceElements.computeIfPresent(namespace, new BiFunction<String, List<E>, List<E>>() {
				public List<E> apply (String n, List<E> es) {
					if (es.remove(element)) {
						Builder.this.size--;
					}
					return es.isEmpty() ? null : es;
				}
			});
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			if (c.isEmpty()) {
				return false;
			}
			prepareModification();

			// this is more efficient than the super implementation
			boolean removed = false;
			for (Object e : c) {
				removed |= remove(e);
			}
			return removed;
		}

		/**
		 * Removes from this builder all elements of each namespace that satisfies the
		 * specified predicate.
		 * 
		 * @param filter the predicate which returns true for a namespace to remove
		 */
		public void removeNamespaceIf(Predicate<String> filter) {
			prepareModification();

			namespaceElements.entrySet().removeIf(new Predicate<Map.Entry<String, List<E>>>() {
				public boolean test(Map.Entry<String, List<E>> e) {
					if (filter.test(e.getKey())) {
						Builder.this.size -= e.getValue().size();
						return true;
					}
					return false;
				}
			});
		}

		@Override
		public boolean removeIf(Predicate<? super E> filter) {
			prepareModification();

			int s = size;
			namespaceElements.values().removeIf(new Predicate<List<E>>() {
				public boolean test(List<E> es) {
					return removeElementsIf(es, filter) == null;
				}
			});
			return size < s;
		}

		/**
		 * Removes from this builder those elements of the specified namespace that
		 * satisfy the specified predicate.
		 * 
		 * @param namespace the namespace of
		 * @param filter    the predicate which returns true for elements to remove
		 */
		public void removeElementsOfNamespaceIf(String namespace, Predicate<? super E> filter) {
			prepareModification();

			namespaceElements.computeIfPresent(namespace, new BiFunction<String, List<E>, List<E>>() {
				public List<E> apply(String n, List<E> es) {
					return removeElementsIf(es, filter);
				}
			});
		}

		private List<E> removeElementsIf(List<E> list, Predicate<? super E> filter) {
			int sizeBefore = list.size();
			list.removeIf(filter);
			this.size -= sizeBefore - list.size();
			return list.isEmpty() ? null : list;
		}

		// --- build ---

		/**
		 * Returns an immutable {@link NamespaceList} containing a snapshot of the
		 * current elements of this builder.
		 * <p>
		 * The content of this builder is not changed by this call and subsequent
		 * modifications to this builder do not reflect into the returned list (the
		 * returned list is not connected to this builder at all).
		 * </p>
		 * 
		 * @return a {@link NamespaceList} reflecting the current state of this builder
		 */
		public NamespaceList<E> build() {
			if (size == 0) {
				return empty(getNamespace);
			}
			if (lastBuildElements == null) {
				lastBuildElements = new ArrayList<>(size);
				for (List<E> es : namespaceElements.values()) {
					lastBuildElements.addAll(es);
				}
				lastBuildElements = Collections.unmodifiableList(lastBuildElements);

				int[] start = new int[] { 0 };
				namespaceElements.replaceAll(new BiFunction<String, List<E>, List<E>>() {
					public List<E> apply(String n, List<E> es) {
						int from = start[0];
						int to = start[0] += es.size();
						return lastBuildElements.subList(from, to);
					}
				});
			}
			return new NamespaceList<>(getNamespace, namespaceElements, lastBuildElements);
		}

		private void prepareModification() {
			if (lastBuildElements != null) {
				// this builder was build before. Create a copy of the Map and their
				// namespace-lists for subsequent modification
				namespaceElements = new LinkedHashMap<>(namespaceElements);
				namespaceElements.replaceAll(new BiFunction<String, List<E>, List<E>>() {
					public List<E> apply(String n, List<E> es) {
						return new ArrayList<>(es);
					}
				});
				lastBuildElements = null;
			}
		}
	}
}
