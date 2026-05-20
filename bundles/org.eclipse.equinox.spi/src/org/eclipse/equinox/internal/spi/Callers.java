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
package org.eclipse.equinox.internal.spi;

import java.lang.StackWalker.StackFrame;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class Callers {
	private static final int CALLER_SEARCH_DEPTH = 13; // TODO: Check this number
	private static final StackWalker STACK_WALKER = StackWalker
			.getInstance(Set.of(StackWalker.Option.RETAIN_CLASS_REFERENCE), CALLER_SEARCH_DEPTH);

	static <T> T walk(Function<Stream<Class<?>>, T> function) {
		return STACK_WALKER.walk(frames -> {
			Stream<Class<?>> classes = frames.<Class<?>>map(StackFrame::getDeclaringClass).filter(Objects::nonNull);
			// TODO: is this limit even necessary?
			return function.apply(classes.limit(CALLER_SEARCH_DEPTH));
		});
	}

	private static class CallersJava8 {
		// TODO: Use this by default and move the current main impl in Java-9+ release
		// folder
		private static final Class<?> REFLECTION_CLASS;
		private static final Method GET_CALLER_CLASS;
		static {
			try {
				REFLECTION_CLASS = Class.forName("sun.reflect.Reflection");
				GET_CALLER_CLASS = Objects.requireNonNull(REFLECTION_CLASS.getMethod("getCallerClass", int.class));
			} catch (ReflectiveOperationException e) {
				throw new AssertionError("Failed to get Reflection class", e);
			}
		}

		static <T> T walk(Function<Stream<Class<?>>, T> function) {
			Stream<Class<?>> classes = IntStream.range(0, CALLER_SEARCH_DEPTH).<Class<?>>mapToObj(i -> {
				try {
					return (Class<?>) GET_CALLER_CLASS.invoke(null, i);
				} catch (ReflectiveOperationException e) {
					throw new AssertionError("Failed to call method " + GET_CALLER_CLASS, e);
				}
			}).filter(Objects::nonNull);
			return function.apply(classes);
		}
	}
}
