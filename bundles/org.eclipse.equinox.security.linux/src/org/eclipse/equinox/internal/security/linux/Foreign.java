/*******************************************************************************
 * Copyright (c) 2026 Aleksandar Kurtakov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Aleksandar Kurtakov - initial version
 *******************************************************************************/
package org.eclipse.equinox.internal.security.linux;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.NoSuchElementException;

/**
 * Helpers shared by the {@link java.lang.foreign} bindings in this package.
 */
final class Foreign {

	private static final Linker LINKER = Linker.nativeLinker();

	private Foreign() {
	}

	/**
	 * Opens the shared library with the given SONAME for the lifetime of the JVM.
	 *
	 * @throws SecurityException if the library is not available
	 */
	static SymbolLookup load(String soname) {
		try {
			return SymbolLookup.libraryLookup(soname, Arena.global());
		} catch (IllegalArgumentException e) {
			throw new SecurityException("Unable to load " + soname, e); //$NON-NLS-1$
		}
	}

	/**
	 * Links the named function of {@code library}.
	 *
	 * @throws SecurityException if the library does not export the symbol
	 */
	static MethodHandle downcall(SymbolLookup library, String name, FunctionDescriptor signature,
			Linker.Option... options) {
		try {
			return LINKER.downcallHandle(library.findOrThrow(name), signature, options);
		} catch (NoSuchElementException | IllegalArgumentException e) {
			throw new SecurityException("Unable to link " + name, e); //$NON-NLS-1$
		}
	}

	/**
	 * Resolves the named symbol, for passing to native code as a function pointer.
	 *
	 * @throws SecurityException if the library does not export the symbol
	 */
	static MemorySegment address(SymbolLookup library, String name) {
		try {
			return library.findOrThrow(name);
		} catch (NoSuchElementException e) {
			throw new SecurityException("Unable to resolve " + name, e); //$NON-NLS-1$
		}
	}

	/**
	 * Wraps whatever {@link MethodHandle#invokeExact} threw for the given function.
	 */
	static SecurityException callFailed(String name, Throwable cause) {
		if (cause instanceof Error error) {
			throw error;
		}
		return new SecurityException("Call to " + name + " failed", cause); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Copies the NUL-terminated UTF-8 string at {@code pointer}, or returns
	 * {@code null} if the pointer is {@code NULL}.
	 */
	static String readString(MemorySegment pointer) {
		if (MemorySegment.NULL.equals(pointer)) {
			return null;
		}
		return pointer.reinterpret(Long.MAX_VALUE).getString(0);
	}
}
