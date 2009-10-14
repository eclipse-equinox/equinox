/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Danail Nachev (Prosyst) - bug 185654
 *     Andrei Loskutov - bug 44735
 *******************************************************************************/
package org.eclipse.core.runtime.internal.adaptor;

import java.io.*;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import org.eclipse.osgi.util.NLS;

/**
 * Internal class.
 */
public class Locker_JavaNio implements Locker {
	private final File lockFile;
	private FileLock fileLock;
	private RandomAccessFile raFile;

	public Locker_JavaNio(File lockFile) {
		this.lockFile = lockFile;
	}

	public synchronized boolean lock() throws IOException {
		raFile = new RandomAccessFile(lockFile, "rw"); //$NON-NLS-1$
		try {
			/*
			 * fix for bug http://bugs.sun.com/view_bug.do?bug_id=6628575 and
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=44735#c17
			 */
			fileLock = raFile.getChannel().tryLock(0, 1, false);
		} catch (IOException ioe) {
			// print exception if debugging
			if (BasicLocation.DEBUG)
				System.out.println(NLS.bind(EclipseAdaptorMsg.location_cannotLock, lockFile));
			// produce a more specific message for clients
			String specificMessage = NLS.bind(EclipseAdaptorMsg.location_cannotLockNIO, new Object[] {lockFile, ioe.getMessage(), "\"-D" + BasicLocation.PROP_OSGI_LOCKING + "=none\""}); //$NON-NLS-1$ //$NON-NLS-2$
			throw new IOException(specificMessage);
		} catch (OverlappingFileLockException e) {
			// handle it as null result
			fileLock = null;
		} finally {
			if (fileLock != null)
				return true;
			raFile.close();
			raFile = null;
		}
		return false;
	}

	public synchronized void release() {
		if (fileLock != null) {
			try {
				fileLock.release();
			} catch (IOException e) {
				//don't complain, we're making a best effort to clean up
			}
			fileLock = null;
		}
		if (raFile != null) {
			try {
				raFile.close();
			} catch (IOException e) {
				//don't complain, we're making a best effort to clean up
			}
			raFile = null;
		}
	}

	public synchronized boolean isLocked() throws IOException {
		if (fileLock != null)
			return true;
		try {
			RandomAccessFile temp = new RandomAccessFile(lockFile, "rw"); //$NON-NLS-1$
			FileLock tempLock = null;
			try {
				/*
				 * fix for bug http://bugs.sun.com/view_bug.do?bug_id=6628575 and
				 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=44735#c17
				 */
				try {
					tempLock = temp.getChannel().tryLock(0, 1, false);
				} catch (IOException ioe) {
					if (BasicLocation.DEBUG)
						System.out.println(NLS.bind(EclipseAdaptorMsg.location_cannotLock, lockFile));
					// produce a more specific message for clients
					String specificMessage = NLS.bind(EclipseAdaptorMsg.location_cannotLockNIO, new Object[] {lockFile, ioe.getMessage(), "\"-D" + BasicLocation.PROP_OSGI_LOCKING + "=none\""}); //$NON-NLS-1$ //$NON-NLS-2$
					throw new IOException(specificMessage);
				}
				if (tempLock != null) {
					tempLock.release(); // allow IOException to propagate because that would mean it is still locked
					return false;
				}
				return true;
			} catch (OverlappingFileLockException e) {
				return true;
			} finally {
				temp.close();
			}
		} catch (FileNotFoundException e) {
			return false;
		}
	}
}
