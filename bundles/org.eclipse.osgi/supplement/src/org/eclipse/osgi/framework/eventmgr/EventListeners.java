/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
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
 * This class manages a list of listeners.
 * Listeners may be added or removed as necessary.
 */

public class EventListeners {
	/**
	 * This field contains the listener list.
	 */
	private ElementList list;

	/**
	 * An empty list of listeners is created.
	 *
	 */
	public EventListeners() {
		list = new ElementList();
	}

	/**
	 * Add a listener to the list.
	 * If a listener object is already in the list, then it is replaced.
	 *
	 * @param listener This is the listener object to be added to the list.
	 * @param listenerObject This is an optional listener-specific object.
	 * This object will be passed to the EventDispatcher along with the listener
	 * when the listener is to be called. This may be null
	 * @throws IllegalArgumentException If listener is null.
	 */
	public synchronized void addListener(Object listener, Object listenerObject) { // resman begin
		list.addElement(new ListElement(listener, listenerObject));
	}

	/**
	 * Remove a listener from the list.
	 *
	 * @param listener This is the listener object to be removed from the list.
	 */
	public synchronized void removeListener(Object listener) {
		list.removeElement(listener);
	}

	/**
	 * Remove all listeners from the list.
	 */
	public synchronized void removeAllListeners() {
		list = new ElementList(); /* allocate a new list */
	}

	/**
	 * Return an array containing the listener, listenerObject pairs.
	 * Package private method.
	 * 
	 * @return A copy of the array of ListElements in the list. 
	 */
	synchronized ListElement[] getListeners() {
		return list.getElements();
	}
}