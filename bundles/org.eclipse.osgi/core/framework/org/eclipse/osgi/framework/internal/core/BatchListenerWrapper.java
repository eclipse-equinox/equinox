/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.util.ArrayList;
import org.eclipse.osgi.event.BatchBundleListener;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

/*
 * Wrappers a <code>BatchBundleListener</code> to queue <code>BundleEvent</code>s
 * while a batching operation is occurring.  Once an end to a batching operation
 * is detected then wrapped BatchBundleListener method bundlesChanged is called
 * with the list of BundleEvents that occurred during the batching operation.
 */
public class BatchListenerWrapper implements SynchronousBundleListener {
	// the wrapped BatchBundleListener
	private BatchBundleListener listener;
	// flag to indication a batching operation
	private boolean batching = false;
	// list of BundleEvents that have occurred in the current batching operation
	private ArrayList batchedEvents;

	BatchListenerWrapper(BatchBundleListener listener) {
		this.listener = listener;
	}

	public synchronized void bundleChanged(BundleEvent event) {
		if (event.getType() == Framework.BATCHEVENT_BEGIN) {
			batching = true; // we have started a batching operation
			return;
		}
		if (event.getType() == Framework.BATCHEVENT_END) {
			batching = false; // a batching operation has ended
			// deliver the list of BundleEvents that have occurued.
			if (batchedEvents != null && batchedEvents.size() > 0)
				listener.bundlesChanged((BundleEvent[]) batchedEvents.toArray(new BundleEvent[batchedEvents.size()]));
			batchedEvents = null; // clear the batchedEvents
			return;
		}
		if (batching) {
			// we are in the middle of batching; save the event til the batching has ended.
			if (batchedEvents == null)
				batchedEvents = new ArrayList(10);
			batchedEvents.add(event);
			return;
		}
		// this is a normal BundleEvent that has occurred outside a batching operation
		// deliver the event to the listener like a normal BundleEvent.
		listener.bundleChanged(event);
	}

}
