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

package org.eclipse.osgi.framework.internal.core;

import org.eclipse.osgi.framework.debug.Debug;
import org.osgi.framework.*;

public class FilteredServiceListener implements ServiceListener
{
    /** Filter for listener. */
    protected Filter filter;

    /** Real listener. */
    protected ServiceListener listener;

    /**
     * Constructor.
     *
     * @param filterstring filter for this listener.
     * @param listener real listener.
     * @exception InvalidSyntaxException if the filter is invalid.
     */
    protected FilteredServiceListener(String filterstring, ServiceListener listener)
                                     throws InvalidSyntaxException
    {
        filter = new Filter(filterstring);
        this.listener = listener;
    }

    /**
     * Receive notification that a service has had a
     * change occur in it's lifecycle.
     *
     * @param event The ServiceEvent.
     */
    public void serviceChanged(ServiceEvent event)
    {
        ServiceReference reference = (ServiceReference)event.getServiceReference();

        if (Debug.DEBUG && Debug.DEBUG_EVENTS)
        {
                String listenerName = this.getClass().getName() + "@" + Integer.toHexString(this.hashCode());
                Debug.println("filterServiceEvent("+listenerName+", \""+filter+"\", "+reference.registration.properties+")");
        }

        if (filter.match(reference))
        {
            if (Debug.DEBUG && Debug.DEBUG_EVENTS)
            {
                    String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(listener.hashCode());
                    Debug.println("dispatchFilteredServiceEvent("+listenerName+")");
            }

            listener.serviceChanged(event);
        }
    }

    /**
     * Get the filter string used by this Filtered listener.
     *
     * @return The filter string used by this listener.
     */
    public String toString()
    {
        return(filter.toString());
    }

}
