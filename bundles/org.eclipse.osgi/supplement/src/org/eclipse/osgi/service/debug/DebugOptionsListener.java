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
package org.eclipse.osgi.service.debug;

import java.util.EventListener;

/**
 * A debug options listener is notified whenever one of its plug-in option-path entries is 
 * changed.  A listener is registered as an OSGi service using the {@link DebugOptions#LISTENER_SYMBOLICNAME}
 * service property to specify the symbolic name of the debug options listener.
 * <p>
 * The {@link DebugOptionsListener#optionsChanged(DebugOptions)} method will automatically 
 * be called upon registration of the debug options listener service.  This allows the 
 * listener to obtain the initial debug options.  This initial call to the listener 
 * will happen even if debug is not enabled at the time of registration 
 * ({@link DebugOptions#isDebugEnabled()} will return false in this case).
 * </p>
 * A debug options listener allows a bundle to cache trace option values in boolean fields for performance
 * and code cleanliness. For example:
 * <pre>
 * public class Activator implements BundleActivator, DebugOptionsListener {
 * 	public static boolean DEBUG = false;
 * 	public static DebugTrace trace;
 * 	
 * 	public void start(BundleContext context) {
 * 		Hashtable props = new Hashtable(4);
 * 		props.put(DebugOptions.LISTENER_SYMBOLICNAME, "com.mycompany.mybundle");
 * 		context.registerService(DebugOptionsListener.class.getName(), this, props);
 * 	}
 * 
 * 	public void optionsChanged(DebugOptions options) {
 * 		if (trace == null)
 * 			trace = options.newDebugTrace("com.mycompany.mybundle");
 * 		DEBUG = options.getBooleanOption("com.mycompany.mybundle/debug", false);
 * 	}
 * 	
 * 	public void doSomeWork() {
 * 		if (DEBUG)
 * 			trace.trace(null, "Doing some work");
 * 	}
 * 	...
 * } 
 * </pre>
 * @since 3.5
 */
public interface DebugOptionsListener extends EventListener {

	/**
	 * Notifies this listener that an option-path for its plug-in has changed.
	 * This method is also called initially by the DebugOptions implementation 
	 * when the listener is registered as a service.  This allows the listener
	 * to obtain the initial set of debug options without the need to 
	 * acquire the debug options service.
	 * @param options a reference to the DebugOptions
	 */
	public void optionsChanged(DebugOptions options);
}