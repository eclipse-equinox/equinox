/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.events;

/**
 * Defines constants for memory event handlers.  A memory event handler implements
 * the <code>org.osgi.service.event.EventHandler</code> interface as specified by the OSGi 
 * Event Admin Service Specification.
 * <p>
 * For example, the following handler is registered to handle critical memory
 * events:
 * <pre>
 * BundleContext context = getContext();
 * EventHandler handler = getHandler();
 * Hashtable ht = new Hashtable();
 * ht.put(EventConstants.EVENT_TOPIC, MemoryEventConstants.TOPIC_CRITICAL);
 * context.registerService(EventHandler.class.getName(), handler, ht);
 * </pre>
 * There is no policy implemented for sending memory events in Equinox or
 * Eclipse by default.  Another bundle must implement the policy that determines
 * when to send memory events.  This policy must use the Event Admin Service
 * to fire memory events to the registered handlers.
 * </p>
 * @since 3.6
 */
public final class MemoryEventConstants {
	private MemoryEventConstants() {
		// prevent construction.
	}

	/**
	 * The base topic that all memory events use.
	 */
	public static final String TOPIC_BASE = "org/eclipse/equinox/events/MemoryEvent/"; //$NON-NLS-1$

	/**
	 * A memory event topic for normal memory events.
	 * Indicates memory is running low at the lowest severity.
	 * Listeners are requested to release caches that can easily be recomputed.
	 * The Java VM is not seriously in trouble, but process size is getting higher than 
	 * is deemed acceptable.
	 */
	public static final String TOPIC_NORMAL = TOPIC_BASE + "NORMAL"; //$NON-NLS-1$

	/**
	 * A memory event topic for serious memory events.
	 * Indicates memory is running low at medium severity. 
	 * Listeners are requested to release complex intermediate models
	 * (.e.g. intermediate build results).
	 * Memory is getting low and may cause operating system level stress, such as swapping.
	 */
	public static final String TOPIC_SERIOUS = TOPIC_BASE + "SERIOUS"; //$NON-NLS-1$

	/**
	 * A memory event topic for critical memory events.
	 * Indicates memory is running low at highest severity.
	 * Listeners are requested to do what ever is possible to free memory.
	 * Things like free in memory caches, close editors and perspectives, 
	 * close database connections, etc.
	 * Restoring these resources and caches constitutes lots of work, but
	 * memory is so low that drastic measures are required.
	 */

	public static final String TOPIC_CRITICAL = TOPIC_BASE + "CRITICAL"; //$NON-NLS-1$

	/**
	 * A memory event topic for all memory events.
	 */
	public static final String TOPIC_ALL = TOPIC_BASE + "*"; //$NON-NLS-1$
}
