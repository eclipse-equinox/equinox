/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.io.*;
import java.nio.channels.FileLock;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;

/**
 * Internal class.
 */
public class Locker_JavaNio implements Locker {
	private File lockFile;
	private FileLock fileLock;
	private FileOutputStream fileStream;

	public Locker_JavaNio(File lockFile) {
		this.lockFile = lockFile;
	}

	public synchronized boolean lock() throws IOException {
		fileStream = new FileOutputStream(lockFile, true);
		try {
			fileLock = fileStream.getChannel().tryLock();
		} catch (IOException ioe) {
			// log the original exception if debugging
			if (BasicLocation.DEBUG) {
				String basicMessage = EclipseAdaptorMsg.formatter.getString("location.cannotLock", lockFile); //$NON-NLS-1$
				FrameworkLogEntry basicEntry = new FrameworkLogEntry(EclipseAdaptor.FRAMEWORK_SYMBOLICNAME, basicMessage, 0, ioe, null);
				EclipseAdaptor.getDefault().getFrameworkLog().log(basicEntry);
			}
			// produce a more specific message for clients
			String specificMessage = EclipseAdaptorMsg.formatter.getString("location.cannotLockNIO", new Object[] {lockFile, ioe.getMessage(), BasicLocation.PROP_OSGI_LOCKING}); //$NON-NLS-1$			
			throw new IOException(specificMessage);
		}
		if (fileLock != null)
			return true;
		fileStream.close();
		fileStream = null;
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
		if (fileStream != null) {
			try {
				fileStream.close();
			} catch (IOException e) {
				//don't complain, we're making a best effort to clean up
			}
			fileStream = null;
		}
	}
}
