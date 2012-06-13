/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.baseadaptor.bundlefile;

import java.io.IOException;
import java.util.Map;
import org.eclipse.osgi.framework.eventmgr.*;

/**
 * A simple/quick/small implementation of an MRU (Most Recently Used) list to keep
 * track of open BundleFiles.  The MRU will use the file limit specified by the property
 * &quot;osgi.bundlefile.limit&quot; by default unless the MRU is constructed with a specific
 * file limit.
 * @since 3.2
 */
public class MRUBundleFileList implements EventDispatcher<Object, Object, BundleFile> {
	private static final String PROP_FILE_LIMIT = "osgi.bundlefile.limit"; //$NON-NLS-1$
	private static final int MIN = 10;
	private static final int PROP_FILE_LIMIT_VALUE;
	private static final ThreadLocal<BundleFile> closingBundleFile = new ThreadLocal<BundleFile>();
	static {
		int propValue = 100; // enable to 100 open files by default
		try {
			String prop = BundleFile.secureAction.getProperty(PROP_FILE_LIMIT);
			if (prop != null)
				propValue = Integer.parseInt(prop);
		} catch (NumberFormatException e) {
			//MRU will be disabled
		}
		PROP_FILE_LIMIT_VALUE = propValue;
	}
	// list of open bundle files
	final private BundleFile[] bundleFileList;
	// list of open bundle files use stamps
	final private long[] useStampList;
	// the limit of open files to allow before least used bundle file is closed
	final private int fileLimit; // value < MIN will disable MRU
	private EventManager bundleFileCloserManager = null;
	final private Map<Object, Object> bundleFileCloser;
	// the current number of open bundle files
	private int numOpen = 0;
	// the current use stamp
	private long curUseStamp = 0;
	// used to work around bug 275166
	private boolean firstDispatch = true;

	public MRUBundleFileList() {
		this(PROP_FILE_LIMIT_VALUE);
	}

	public MRUBundleFileList(int fileLimit) {
		// only enable the MRU if the initFileLimit is > MIN
		this.fileLimit = fileLimit;
		if (fileLimit >= MIN) {
			this.bundleFileList = new BundleFile[fileLimit];
			this.useStampList = new long[fileLimit];
			this.bundleFileCloser = new CopyOnWriteIdentityMap<Object, Object>();
			this.bundleFileCloser.put(this, this);
		} else {
			this.bundleFileList = null;
			this.useStampList = null;
			this.bundleFileCloser = null;
		}
	}

	/**
	 * Adds a BundleFile which is about to be opened to the MRU list.  If 
	 * the number of open BundleFiles == the fileLimit then the least 
	 * recently used BundleFile is closed.
	 * @param bundleFile the bundle file about to be opened.
	 */
	public void add(BundleFile bundleFile) {
		if (fileLimit < MIN)
			return; // MRU is disabled
		BundleFile toRemove = null;
		EventManager manager = null;
		synchronized (this) {
			if (bundleFile.getMruIndex() >= 0)
				return; // do nothing; someone is trying add a bundleFile that is already in an MRU list
			int index = 0; // default to the first slot
			if (numOpen < fileLimit) {
				// numOpen does not exceed the fileLimit
				// find the first null slot to use in the MRU
				for (int i = 0; i < fileLimit; i++)
					if (bundleFileList[i] == null) {
						index = i;
						break;
					}
			} else {
				// numOpen has reached the fileLimit
				// find the least recently used bundleFile and close it 
				// and use it slot for the new bundleFile to be opened.
				index = 0;
				for (int i = 1; i < fileLimit; i++)
					if (useStampList[i] < useStampList[index])
						index = i;
				toRemove = bundleFileList[index];
				if (toRemove.getMruIndex() != index)
					throw new IllegalStateException("The BundleFile has the incorrect mru index: " + index + " != " + toRemove.getMruIndex()); //$NON-NLS-1$//$NON-NLS-2$
				removeInternal(toRemove);
			}
			// found an index to place to bundleFile to be opened
			bundleFileList[index] = bundleFile;
			bundleFile.setMruIndex(index);
			incUseStamp(index);
			numOpen++;
			if (toRemove != null) {
				if (bundleFileCloserManager == null)
					bundleFileCloserManager = new EventManager("Bundle File Closer"); //$NON-NLS-1$
				manager = bundleFileCloserManager;
			}

		}
		// must not close the toRemove bundle file while holding the lock of another bundle file (bug 161976)
		// This queues the bundle file for close asynchronously.
		closeBundleFile(toRemove, manager);
	}

	/**
	 * Removes a bundle file which is about to be closed
	 * @param bundleFile the bundle file about to be closed
	 * @return true if the bundleFile existed in the MRU; false otherwise
	 */
	public boolean remove(BundleFile bundleFile) {
		if (fileLimit < MIN)
			return false; // MRU is disabled
		synchronized (this) {
			int index = bundleFile.getMruIndex();
			if ((index >= 0 && index < fileLimit) && bundleFileList[index] == bundleFile) {
				removeInternal(bundleFile);
				return true;
			}
		}
		return false;
	}

	// must be called while synchronizing "this"
	private void removeInternal(BundleFile bundleFile) {
		int index = bundleFile.getMruIndex();
		bundleFile.setMruIndex(-1);
		bundleFileList[index] = null;
		useStampList[index] = -1;
		numOpen--;
	}

	/**
	 * Increments the use stamp of a bundle file
	 * @param bundleFile the bundle file to increment the use stamp for
	 */
	public void use(BundleFile bundleFile) {
		if (fileLimit < MIN)
			return; // MRU is disabled
		synchronized (this) {
			int index = bundleFile.getMruIndex();
			if ((index >= 0 && index < fileLimit) && bundleFileList[index] == bundleFile)
				incUseStamp(index);
		}
	}

	// must be called while synchronizing "this"
	private void incUseStamp(int index) {
		if (curUseStamp == Long.MAX_VALUE) {
			// we hit the curUseStamp max better reset all the stamps
			for (int i = 0; i < fileLimit; i++)
				useStampList[i] = 0;
			curUseStamp = 0;
		}
		useStampList[index] = ++curUseStamp;
	}

	public final void dispatchEvent(Object eventListener, Object listenerObject, int eventAction, BundleFile eventObject) {
		if (firstDispatch) {
			// used to work around bug 275166; we don't want to leak the TCCL in this thread.
			Thread.currentThread().setContextClassLoader(null);
			firstDispatch = false;
		}
		try {
			closingBundleFile.set(eventObject);
			eventObject.close();
		} catch (IOException e) {
			// TODO should log ??
		} finally {
			closingBundleFile.set(null);
		}
	}

	private void closeBundleFile(BundleFile toRemove, EventManager manager) {
		if (toRemove == null)
			return;
		try {
			/* queue to hold set of listeners */
			ListenerQueue<Object, Object, BundleFile> queue = new ListenerQueue<Object, Object, BundleFile>(manager);
			/* add bundle file closer to the queue */
			queue.queueListeners(bundleFileCloser.entrySet(), this);
			/* dispatch event to set of listeners */
			queue.dispatchEventAsynchronous(0, toRemove);
		} catch (Throwable t) {
			// we cannot propagate exceptions out of this method
			// failing to queue a bundle close should not cause an error (bug 283797)
			// TODO should consider logging
		}
	}

	/**
	 * Closes the bundle file closer thread for the MRU list
	 */
	public void shutdown() {
		synchronized (this) {
			if (bundleFileCloserManager != null)
				bundleFileCloserManager.close();
			bundleFileCloserManager = null;
		}
	}

	/**
	 * Returns true if this MRUBundleFileList is currently closing the specified bundle file on the current thread.
	 * @param bundleFile the bundle file
	 * @return true if the bundle file is being closed on the current thread
	 */
	public boolean isClosing(BundleFile bundleFile) {
		if (fileLimit < MIN)
			return false; // MRU is disabled
		// check the thread local variable
		return closingBundleFile.get() == bundleFile;
	}

	public boolean isEnabled() {
		return fileLimit >= MIN;
	}
}
