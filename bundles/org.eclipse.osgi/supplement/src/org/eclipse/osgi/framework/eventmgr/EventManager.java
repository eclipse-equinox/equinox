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
 * This class is the central class for the event manager. Each
 * program that wishes to use the event manager should construct
 * an EventManager object and use that object to construct
 * EventQueue for dispatching events.
 */

public class EventManager
{
    /**
     * EventThread for asynchronous dispatch of events.
     */
    protected EventThread thread;

    /**
     * EventThread Name
     */
    protected String threadName;

    /**
     * EventManager constructor. An EventManager object is responsible for
     * the delivery of events to listeners via an EventSource.
     *
     */
    public EventManager()
    {
        thread = null;
    }

    public EventManager(String threadName){
    	this();
    	this.threadName = threadName;
    }

    /**
     * This method can be called to release any resources associated with this
     * EventManager.
     *
     */
    public synchronized void close()
    {
        if (thread != null)
        {
            thread.close();
            thread = null;
        }
    }

    /**
     * Asynchronously dispatch an event to the set of listeners. An event dispatch thread
     * maintained by the associated EventManager is used to deliver the events.
     * This method may return immediately to the caller.
     *
     * @param ll The set of listeners to which the event will be dispatched.
     * @param eventAction This value is passed back to the event source when the call back
     * is made to the EventSource object along with each listener.
     * @param eventObject This object is passed back to the event source when the call back
     * is made to the EventSource object along with each listener.
     */
    void dispatchEventAsynchronous(ListenerList ll, int eventAction, Object eventObject)
    {
        EventThread thread = getEventThread();

        for (; ll != null; ll = ll.list)
        {
            thread.postEvent((ListenerList) ll.listener, (EventSource) ll.object, eventAction, eventObject);
        }
    }

    /**
     * Synchronously dispatch an event to the set of listeners. The event may
     * be dispatch on the current thread or an event dispatch thread
     * maintained by the associated EventManager.
     * This method will not return to the caller until an EventSource
     * has been called (and returned) for each listener.
     *
     * @param ll The set of listeners to which the event will be dispatched.
     * @param eventAction This value is passed back to the event source when the call back
     * is made to the EventSource object along with each listener.
     * @param eventObject This object is passed back to the event source when the call back
     * is made to the EventSource object along with each listener.
     */
    void dispatchEventSynchronous(ListenerList ll, int eventAction, Object eventObject)
    {
        for (; ll != null; ll = ll.list)
        {
            ((ListenerList) ll.listener).dispatchEvent((EventSource) ll.object, eventAction, eventObject);
        }
    }

    /**
     * Returns an EventThread to use for dispatching events asynchronously.
     *
     * @return EventThread.
     */
    private synchronized EventThread getEventThread()
    {
        if (thread == null)
        {
        	if (threadName == null)
        	{
        		thread = new EventThread();
        	}
        	else
        	{	
        		thread = new EventThread(threadName);
        	}
        }

        return(thread);
    }
}

