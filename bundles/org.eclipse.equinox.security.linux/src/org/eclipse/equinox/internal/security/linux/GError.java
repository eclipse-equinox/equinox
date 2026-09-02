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

import java.lang.foreign.GroupLayout;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * GLib's {@code GError}. Only the message is read; {@code domain} and
 * {@code code} are declared because the message offset depends on them.
 */
final class GError {

	private static final GroupLayout LAYOUT = MemoryLayout.structLayout( //
			ValueLayout.JAVA_INT.withName("domain"), //$NON-NLS-1$
			ValueLayout.JAVA_INT.withName("code"), //$NON-NLS-1$
			ValueLayout.ADDRESS.withName("message")); //$NON-NLS-1$

	private static final long MESSAGE_OFFSET = LAYOUT.byteOffset(PathElement.groupElement("message")); //$NON-NLS-1$

	private GError() {
	}

	/**
	 * Copies the message out of the {@code GError} at the given pointer, so that
	 * the result stays valid once the native error has been freed.
	 */
	static String readMessage(MemorySegment pointer) {
		MemorySegment error = pointer.reinterpret(LAYOUT.byteSize());
		return Foreign.readString(error.get(ValueLayout.ADDRESS, MESSAGE_OFFSET));
	}
}
