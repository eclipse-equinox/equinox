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
 * This class is used for asynchronously dispatching events.
 */

class EventThread extends Thread
{
    /**
     * EventThreadItem is a nested top-level (non-member) class. This class
     * represents the items which are placed on the queue.
     */
    static class EventThreadItem
    {
        /** listener list for this event */
        private final ListenerList listeners;
        /** source of this event */
        private final EventSource source;
        /** action for this event */
        private final int action;
        /** object for this event */
        private final Object object;
        /** next item in event queue */
        EventThreadItem next;

        /**
         * Constructor for event queue item
         *
         * @param l Listener list for this event
         * @param s Source for this event
         * @param a Action for this event
         * @param o Object for this event
         */
        EventThreadItem(ListenerList l, EventSource s, int a, Object o)
        {
            listeners = l;
            source = s;
            action = a;
            object = o;
            next = null;
        }

        /**
         * This method will dispatch this event queue item to its listeners
         */
        void dispatchEvent()
        {
            listeners.dispatchEvent(source, action, object);
        }
    }

    /** item at the head of the event queue */
    private EventThreadItem head;
    /** item at the tail of the event queue */
    private EventThreadItem tail;
    /** if false the thread must terminate */
    private volatile boolean running;

	/**
	 * Constructor for the event queue. The queue is created empty and the
	 * queue dispatcher thread is started.
	 */
	EventThread(String threadName)
	{
		super(threadName);
		init();
	}

    /**
     * Constructor for the event queue. The queue is created empty and the
     * queue dispatcher thread is started.
     */
    EventThread()
    {
        super();
        init();
    }

    void init(){
		running = true;
		head = null;
		tail = null;

		setDaemon(true);    /* Mark thread as daemon thread */
		start();            /* Start thread */
    }

    /**
     * Stop thread.
     */
    void close()
    {
        running = false;
        interrupt();
    }

    /**
     * This method is the event queue dispatcher thread. It pulls events from
     * the queue and dispatches them.
     */
    public void run()
    {
        while (running)
        {
            try
            {
                getNextEvent().dispatchEvent();
            }
            catch (Throwable t)
            {
            }
        }
    }

    /**
     * This methods takes the input parameters and creates an EventThreadItem
     * and queues it.
     * The thread is notified.
     *
     * @param l Listener list for this event
     * @param s Source for this event
     * @param a Action for this event
     * @param o Object for this event
     */
    synchronized void postEvent(ListenerList l, EventSource s, int a, Object o)
    {
        EventThreadItem item = new EventThreadItem(l, s, a, o);

        if (head == null)  /* if the queue was empty */
        {
            head = item;
            tail = item;
        }
        else                /* else add to end of queue */
        {
            tail.next = item;
            tail = item;
        }

        notify();
    }

    /**
     * This method is called by the thread to remove
     * items from the queue so that they can be dispatched to their listeners.
     * If the queue is empty, the thread waits.
     *
     * @return The EventThreadItem removed from the top of the queue.
     */
    private synchronized EventThreadItem getNextEvent() throws InterruptedException
    {
        while (running && (head == null))
        {
            try
            {
                wait();
            }
            catch (InterruptedException e)
            {
            }
        }

        if (!running)   /* if we are stopping */
        {
            throw new InterruptedException();    /* throw an exception */
        }

        EventThreadItem item = head;
        head = item.next;
        if (head == null)
        {
            tail = null;
        }

        return(item);
    }
}
