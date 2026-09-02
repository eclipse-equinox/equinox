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

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * The GLib, GObject and GIO entry points needed by
 * {@link LinuxPasswordProvider}.
 */
final class LibGio {

	private static LibGio instance;

	private final MethodHandle busGetSyncHandle;
	private final MethodHandle errorFreeHandle;
	private final MethodHandle listAppendHandle;
	private final MethodHandle listFreeHandle;
	private final MethodHandle listFreeFullHandle;
	private final MethodHandle objectUnrefHandle;

	/** {@code g_object_unref} as a {@code GDestroyNotify} function pointer. */
	private final MemorySegment objectUnrefAddress;

	/**
	 * @throws SecurityException if the GLib stack is unavailable
	 */
	static synchronized LibGio getInstance() {
		if (instance == null) {
			instance = new LibGio();
		}
		return instance;
	}

	private LibGio() {
		SymbolLookup glib = Foreign.load("libglib-2.0.so.0"); //$NON-NLS-1$
		SymbolLookup gobject = Foreign.load("libgobject-2.0.so.0"); //$NON-NLS-1$
		SymbolLookup gio = Foreign.load("libgio-2.0.so.0"); //$NON-NLS-1$

		busGetSyncHandle = Foreign.downcall(gio, "g_bus_get_sync", //$NON-NLS-1$
				FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
						ValueLayout.ADDRESS));
		errorFreeHandle = Foreign.downcall(glib, "g_error_free", //$NON-NLS-1$
				FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
		listAppendHandle = Foreign.downcall(glib, "g_list_append", //$NON-NLS-1$
				FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
		listFreeHandle = Foreign.downcall(glib, "g_list_free", //$NON-NLS-1$
				FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
		listFreeFullHandle = Foreign.downcall(glib, "g_list_free_full", //$NON-NLS-1$
				FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
		objectUnrefHandle = Foreign.downcall(gobject, "g_object_unref", //$NON-NLS-1$
				FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
		objectUnrefAddress = Foreign.address(gobject, "g_object_unref"); //$NON-NLS-1$
	}

	/** {@code GDBusConnection *g_bus_get_sync(GBusType, GCancellable *, GError **)} */
	MemorySegment busGetSync(int busType, MemorySegment cancellable, MemorySegment error) {
		try {
			return (MemorySegment) busGetSyncHandle.invokeExact(busType, cancellable, error);
		} catch (Throwable t) {
			throw Foreign.callFailed("g_bus_get_sync", t); //$NON-NLS-1$
		}
	}

	/** {@code void g_error_free(GError *)} */
	void errorFree(MemorySegment error) {
		try {
			errorFreeHandle.invokeExact(error);
		} catch (Throwable t) {
			throw Foreign.callFailed("g_error_free", t); //$NON-NLS-1$
		}
	}

	/** {@code GList *g_list_append(GList *, gpointer)} */
	MemorySegment listAppend(MemorySegment list, MemorySegment data) {
		try {
			return (MemorySegment) listAppendHandle.invokeExact(list, data);
		} catch (Throwable t) {
			throw Foreign.callFailed("g_list_append", t); //$NON-NLS-1$
		}
	}

	/** {@code void g_list_free(GList *)} - frees the list, not its elements. */
	void listFree(MemorySegment list) {
		try {
			listFreeHandle.invokeExact(list);
		} catch (Throwable t) {
			throw Foreign.callFailed("g_list_free", t); //$NON-NLS-1$
		}
	}

	/**
	 * {@code void g_list_free_full(GList *, GDestroyNotify)} with
	 * {@code g_object_unref}, for lists that own a reference to each element.
	 */
	void listFreeFullUnref(MemorySegment list) {
		try {
			listFreeFullHandle.invokeExact(list, objectUnrefAddress);
		} catch (Throwable t) {
			throw Foreign.callFailed("g_list_free_full", t); //$NON-NLS-1$
		}
	}

	/** {@code void g_object_unref(gpointer)}; ignores {@code NULL}. */
	void objectUnref(MemorySegment object) {
		if (MemorySegment.NULL.equals(object)) {
			return;
		}
		try {
			objectUnrefHandle.invokeExact(object);
		} catch (Throwable t) {
			throw Foreign.callFailed("g_object_unref", t); //$NON-NLS-1$
		}
	}
}
