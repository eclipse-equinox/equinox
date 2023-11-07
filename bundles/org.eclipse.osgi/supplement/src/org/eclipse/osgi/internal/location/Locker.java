/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.osgi.internal.location;

import java.io.IOException;

/**
 * Internal class.
 */
public interface Locker {
	public boolean lock() throws IOException;

	public boolean isLocked() throws IOException;

	public void release();

	static class MockLocker implements Locker {
		@Override
		public boolean lock() throws IOException {
			// locking always successful
			return true;
		}

		@Override
		public boolean isLocked() {
			// this lock is never locked
			return false;
		}

		@Override
		public void release() {
			// nothing to release
		}

	}
}
