/*******************************************************************************
 * Copyright (c) 2026, 2026 Hannes Wellmann and others.
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

package org.eclipse.equinox.spi.internal;

import java.lang.StackWalker.StackFrame;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

class Utilities {
	private Utilities() {
	}

	private static final int CALLER_SEARCH_DEPTH = 14;
	private static final StackWalker STACK_WALKER = StackWalker
			.getInstance(Set.of(StackWalker.Option.RETAIN_CLASS_REFERENCE), CALLER_SEARCH_DEPTH);

	static <T> T walkCallerClasses(Function<Stream<Class<?>>, T> function) {
		return STACK_WALKER.walk(frames -> {
			Stream<Class<?>> classes = frames.<Class<?>>map(StackFrame::getDeclaringClass).filter(Objects::nonNull);
			return function.apply(classes.limit(CALLER_SEARCH_DEPTH));
		});
	}

	static <E> List<E> listOf() {
		return List.of();
	}

	@SafeVarargs
	static <E> Set<E> setOf(E... elements) {
		return Set.of(elements);
	}

	static <K, V> Map<K, V> mapOf() {
		return Map.of();
	}

	@SuppressWarnings("unchecked")
	static <K, V> Map<K, V> mapCopyOf(Map<K, V> map) {
		return Map.ofEntries(map.entrySet().toArray(new Entry[0]));
	}
}
