/*******************************************************************************
 * Copyright (c) 2020, 2026 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial version
 *     Aleksandar Kurtakov - modified to make Java FFM version
 *******************************************************************************/
package org.eclipse.equinox.internal.security.linux;

import java.lang.foreign.Arena;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * libsecret's {@code SecretSchema}.
 */
final class SecretSchema {

	/** Fixed size of the inline {@code attributes} array in the C struct. */
	private static final int MAX_ATTRIBUTES = 32;

	/** {@code SecretSchemaAttribute { const gchar *name; SecretSchemaAttributeType type; }} */
	private static final GroupLayout ATTRIBUTE_LAYOUT = MemoryLayout.structLayout( //
			ValueLayout.ADDRESS.withName("name"), //$NON-NLS-1$
			ValueLayout.JAVA_INT.withName("type"), //$NON-NLS-1$
			MemoryLayout.paddingLayout(4));

	private static final GroupLayout LAYOUT = MemoryLayout.structLayout( //
			ValueLayout.ADDRESS.withName("name"), //$NON-NLS-1$
			ValueLayout.JAVA_INT.withName("flags"), //$NON-NLS-1$
			MemoryLayout.paddingLayout(4),
			MemoryLayout.sequenceLayout(MAX_ATTRIBUTES, ATTRIBUTE_LAYOUT).withName("attributes"), //$NON-NLS-1$
			ValueLayout.JAVA_INT.withName("reserved"), //$NON-NLS-1$
			MemoryLayout.paddingLayout(4),
			MemoryLayout.sequenceLayout(7, ValueLayout.ADDRESS).withName("reserved1")); //$NON-NLS-1$

	private static final long NAME_OFFSET = LAYOUT.byteOffset(PathElement.groupElement("name")); //$NON-NLS-1$
	private static final long FLAGS_OFFSET = LAYOUT.byteOffset(PathElement.groupElement("flags")); //$NON-NLS-1$

	private final MemorySegment segment;

	/**
	 * Allocates the schema in {@code arena}, which must stay reachable for as long
	 * as the schema is passed to libsecret.
	 * <p>
	 * The inline attribute array is left zeroed: this schema declares no
	 * attributes, and libsecret stops at the first entry with a {@code NULL} name.
	 */
	SecretSchema(Arena arena, String name, int flags) {
		segment = arena.allocate(LAYOUT);
		segment.set(ValueLayout.ADDRESS, NAME_OFFSET, arena.allocateFrom(name));
		segment.set(ValueLayout.JAVA_INT, FLAGS_OFFSET, flags);
	}

	/** The native {@code SecretSchema *} to hand to libsecret. */
	MemorySegment segment() {
		return segment;
	}
}
