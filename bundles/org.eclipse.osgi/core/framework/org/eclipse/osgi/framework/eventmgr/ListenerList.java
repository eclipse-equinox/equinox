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
 * This internal class manages the list of listeners. Each object is
 * immutable, once created it cannot be modified. The most expensive
 * operation on the list is removeListener. This operation must descend
 * the list until the listener is found, then as it returns up the call
 * chain, new ListenerList objects are created and returned to rebuild the list.
 */

class ListenerList {
	/**
	 * Next item in list.
	 */
	final ListenerList list;

	/**
	 * Listener referenced by this item.
	 */
	final Object listener;

	/**
	 * Listener object referenced by this item.
	 */
	final Object object;

	static final boolean DEBUG = true;

	/**
	 * Private constructor used by addListener and removeListener.
	 *
	 * @param oldlist Existing list or null if no list.
	 * @param l Listener to be added to list.
	 * @return List object.
	 */
	private ListenerList(ListenerList oldlist, Object l, Object o) {
		list = oldlist;
		listener = l;
		object = o;
	}

	/**
	 * Static method to add a listener to a list.
	 *
	 * @param oldlist Existing list to which the listener is to be added.
	 * @param l Listener to be added to list.
	 * @param o Listener specific object to be added to list.
	 * @return New list with listener added.
	 */
	static ListenerList addListener(ListenerList oldlist, Object l, Object o) {
		/* Create the linked list element in the listener's memory space */
		// resman begin
		return (new ListenerList(oldlist, l, o));
		// resman end
	}

	/**
	 * Static method to remove a listener from the list.
	 *
	 * @param oldlist Existing list from which the listener is to be removed
	 * @param l Listener to be removed from list
	 * @return New list with listener removed.
	 */
	static ListenerList removeListener(ListenerList oldlist, Object l) {
		if (oldlist != null) /* list is not empty */ {
			try {
				return (oldlist.removeListener(l));
			} catch (IllegalArgumentException e) {
				/* Listener to be removed was not found */
			}
		}
		return (oldlist);
	}

	/**
	 * Private method to recurse down the list looking for the listener to remove.
	 * When the listener is found, the call chain returns creating new List
	 * objects rebuilding the list.
	 *
	 * @param l Listener to be removed from list
	 * @return New sublist with listener removed.
	 * @exception java.lang.IllegalArgumentException If listener to be removed
	 * is not found
	 */
	private ListenerList removeListener(Object l) throws IllegalArgumentException {
		if (listener == l) /* Check if this the guy to remove */ {
			return (list); /* return the next on the list */
		}

		if (list != null) /* we've not reached the end of the list */ {
			ListenerList ll = list.removeListener(l);

			/* Create the linked list element in the listener's memory space */
			// resman begin
			return (new ListenerList(ll, listener, object));
			// resman end
		}

		/* We have reached the end of the list and have not found the listener */
		throw new IllegalArgumentException();	//TODO This is not usually a good core practice.
	}

	/**
	 * This method calls the EventSource object to complete the dispatch of
	 * the event. If there are more listeners in the list, call dispatchEvent
	 * on the next item on the list.
	 *
	 * @param source Call back object which is called to complete the delivery of
	 * the event.
	 * @param action This value was passed by the event source and
	 * is passed to this method. This is passed on to the call back object.
	 * @param object This object was created by the event source and
	 * is passed to this method. This is passed on to the call back object.
	 */
	void dispatchEvent(EventSource source, int action, Object object) {
		/* Remember the current thread's active memory space */
		// resman begin
		for (ListenerList ll = this; ll != null; ll = ll.list) {
			/* Dispatch the event in the listener's memory space */
			// resman select memoryspace

			try {
				/* Call the call back method with the listener */
				source.dispatchEvent(ll.listener, ll.object, action, object);
			} catch (Throwable t) {
				/* Consume and ignore any exceptions thrown by the listener */

				if (DEBUG) {
					System.out.println("Exception in " + ll.listener);
					t.printStackTrace();
				}
			}
		}
		// resman end
	}
}
