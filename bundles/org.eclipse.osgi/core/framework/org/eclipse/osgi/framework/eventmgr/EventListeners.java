/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.eventmgr;

/**
 * This class manages the list of listeners.
 * Listeners may be added or removed as necessary.
 */

public class EventListeners {
	/**
	 * This field contains the listener list.
	 */
	ListenerList list;

	/**
	 * Constructor for EventListeners. An empty list of listeners is created.
	 *
	 */
	public EventListeners() {
		list = null;
	}

	/**
	 * This method is called to add a listener to the list.
	 *
	 * @param listener This is the listener object to be added to the list.
	 * @param listenerObject This is an optional listener-specific object.
	 * This object will be passed to the EventSource along with the listener
	 * when the listener is to be called.
	 */
	public synchronized void addListener(Object listener, Object listenerObject) {
		if (listener != null) {
			list = ListenerList.addListener(list, listener, listenerObject);
		}
	}

	/**
	 * This method is called to remove a listener from the list.
	 *
	 * @param listener This is the listener object to be removed from the list.
	 */
	public synchronized void removeListener(Object listener) {
		if (listener != null) {
			list = ListenerList.removeListener(list, listener);
		}
	}

	/**
	 * This method is called to remove all listeners from the list.
	 */
	public synchronized void removeAllListeners() {
		list = null;
	}
}
