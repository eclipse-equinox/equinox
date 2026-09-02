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
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * The libsecret entry points needed by {@link LinuxPasswordProvider}.
 * <p>
 * {@code secret_password_lookup_sync} and {@code secret_password_store_sync} are
 * variadic: the descriptors below declare the fixed parameters plus the single
 * trailing {@code NULL} that terminates the (empty) attribute list, and mark
 * where the variadic part starts.
 */
final class LibSecret {

	private static final String SONAME = "libsecret-1.so.0"; //$NON-NLS-1$

	private static LibSecret instance;

	private final MethodHandle serviceGetSyncHandle;
	private final MethodHandle collectionForAliasSyncHandle;
	private final MethodHandle collectionGetLockedHandle;
	private final MethodHandle serviceUnlockSyncHandle;
	private final MethodHandle passwordLookupSyncHandle;
	private final MethodHandle passwordStoreSyncHandle;
	private final MethodHandle passwordFreeHandle;

	/**
	 * @throws SecurityException if libsecret is unavailable
	 */
	static synchronized LibSecret getInstance() {
		if (instance == null) {
			instance = new LibSecret();
		}
		return instance;
	}

	private LibSecret() {
		SymbolLookup library = Foreign.load(SONAME);
		serviceGetSyncHandle = Foreign.downcall(library, "secret_service_get_sync", //$NON-NLS-1$
				FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
						ValueLayout.ADDRESS));
		collectionForAliasSyncHandle = Foreign.downcall(library, "secret_collection_for_alias_sync", //$NON-NLS-1$
				FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
						ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
		collectionGetLockedHandle = Foreign.downcall(library, "secret_collection_get_locked", //$NON-NLS-1$
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
		serviceUnlockSyncHandle = Foreign.downcall(library, "secret_service_unlock_sync", //$NON-NLS-1$
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
						ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
		// (schema, cancellable, error, ...) - 3 fixed parameters
		passwordLookupSyncHandle = Foreign.downcall(library, "secret_password_lookup_sync", //$NON-NLS-1$
				FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
						ValueLayout.ADDRESS, ValueLayout.ADDRESS),
				Linker.Option.firstVariadicArg(3));
		// (schema, collection, label, password, cancellable, error, ...) - 6 fixed
		passwordStoreSyncHandle = Foreign.downcall(library, "secret_password_store_sync", //$NON-NLS-1$
				FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
						ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
						ValueLayout.ADDRESS),
				Linker.Option.firstVariadicArg(6));
		passwordFreeHandle = Foreign.downcall(library, "secret_password_free", //$NON-NLS-1$
				FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
	}

	/** {@code SecretService *secret_service_get_sync(SecretServiceFlags, GCancellable *, GError **)} */
	MemorySegment serviceGetSync(int flags, MemorySegment cancellable, MemorySegment error) {
		try {
			return (MemorySegment) serviceGetSyncHandle.invokeExact(flags, cancellable, error);
		} catch (Throwable t) {
			throw Foreign.callFailed("secret_service_get_sync", t); //$NON-NLS-1$
		}
	}

	/**
	 * {@code SecretCollection *secret_collection_for_alias_sync(SecretService *, const gchar *, SecretCollectionFlags, GCancellable *, GError **)}
	 */
	MemorySegment collectionForAliasSync(MemorySegment service, MemorySegment alias, int flags,
			MemorySegment cancellable, MemorySegment error) {
		try {
			return (MemorySegment) collectionForAliasSyncHandle.invokeExact(service, alias, flags, cancellable, error);
		} catch (Throwable t) {
			throw Foreign.callFailed("secret_collection_for_alias_sync", t); //$NON-NLS-1$
		}
	}

	/** {@code gboolean secret_collection_get_locked(SecretCollection *)} */
	boolean collectionGetLocked(MemorySegment collection) {
		try {
			return (int) collectionGetLockedHandle.invokeExact(collection) != 0;
		} catch (Throwable t) {
			throw Foreign.callFailed("secret_collection_get_locked", t); //$NON-NLS-1$
		}
	}

	/**
	 * {@code gint secret_service_unlock_sync(SecretService *, GList *, GCancellable *, GList **, GError **)}
	 */
	int serviceUnlockSync(MemorySegment service, MemorySegment objects, MemorySegment cancellable,
			MemorySegment unlocked, MemorySegment error) {
		try {
			return (int) serviceUnlockSyncHandle.invokeExact(service, objects, cancellable, unlocked, error);
		} catch (Throwable t) {
			throw Foreign.callFailed("secret_service_unlock_sync", t); //$NON-NLS-1$
		}
	}

	/**
	 * {@code gchar *secret_password_lookup_sync(const SecretSchema *, GCancellable *, GError **, ...)}
	 * called with an empty, {@code NULL}-terminated attribute list.
	 */
	MemorySegment passwordLookupSync(MemorySegment schema, MemorySegment cancellable, MemorySegment error) {
		try {
			return (MemorySegment) passwordLookupSyncHandle.invokeExact(schema, cancellable, error,
					MemorySegment.NULL);
		} catch (Throwable t) {
			throw Foreign.callFailed("secret_password_lookup_sync", t); //$NON-NLS-1$
		}
	}

	/**
	 * {@code void secret_password_free(gchar *)}, which clears the memory before
	 * releasing it. Must be called on every string returned by
	 * {@link #passwordLookupSync}.
	 */
	void passwordFree(MemorySegment password) {
		try {
			passwordFreeHandle.invokeExact(password);
		} catch (Throwable t) {
			throw Foreign.callFailed("secret_password_free", t); //$NON-NLS-1$
		}
	}

	/**
	 * {@code gboolean secret_password_store_sync(const SecretSchema *, const gchar *collection, const gchar *label, const gchar *password, GCancellable *, GError **, ...)}
	 * called with an empty, {@code NULL}-terminated attribute list.
	 */
	boolean passwordStoreSync(MemorySegment schema, MemorySegment collection, MemorySegment label,
			MemorySegment password, MemorySegment cancellable, MemorySegment error) {
		try {
			return (int) passwordStoreSyncHandle.invokeExact(schema, collection, label, password, cancellable, error,
					MemorySegment.NULL) != 0;
		} catch (Throwable t) {
			throw Foreign.callFailed("secret_password_store_sync", t); //$NON-NLS-1$
		}
	}
}
