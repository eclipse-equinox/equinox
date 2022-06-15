/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
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

import java.io.*;

/**
 * Internal class.
 */
public class Locker_JavaIo implements Locker {
	private File lockFile;
	private RandomAccessFile lockRAF;

	public Locker_JavaIo(File lockFile) {
		this.lockFile = lockFile;
	}

	@Override
	public synchronized boolean lock() throws IOException {
		//if the lock file already exists, try to delete,
		//assume failure means another eclipse has it open
		if (lockFile.exists())
			lockFile.delete();
		if (lockFile.exists())
			return false;

		//open the lock file so other instances can't co-exist
		lockRAF = new RandomAccessFile(lockFile, "rw"); //$NON-NLS-1$
		try {
			lockRAF.writeByte(0);
		} catch (IOException e) {
			lockRAF.close();
			lockRAF = null;
			throw e;
		}

		return true;
	}

	@Override
	public synchronized void release() {
		try {
			if (lockRAF != null) {
				lockRAF.close();
				lockRAF = null;
			}
		} catch (IOException e) {
			//don't complain, we're making a best effort to clean up
		}
		if (lockFile != null)
			lockFile.delete();
	}

	@Override
	public synchronized boolean isLocked() throws IOException {
		if (lockRAF != null)
			return true;
		try {
			return !lock();
		} finally {
			release();
		}
	}
}
