/*
 * $Date: 2008-07-10 17:24:58 -0400 (Thu, 10 Jul 2008) $
 * 
 * Copyright (c) OSGi Alliance (2008). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.framework.hooks.service;

import java.util.Collection;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceListener;

/**
 * OSGi Framework Service Listener Hook Service.
 * 
 * <p>
 * Bundles registering this service will be called during service listener
 * addition and removal. Service hooks are not called for service operations on
 * other service hooks.
 * 
 * @ThreadSafe
 * @version $Revision: 5176 $
 */

public interface ListenerHook {
	/**
	 * Add listener hook method. This method is called during service listener
	 * addition. This method will be called once for each service listener added
	 * after this hook had been registered.
	 * 
	 * @param listener A listener which is now listening to service events.
	 */
	void added(Collection listeners);

	/**
	 * Remove listener hook method. This method is called during service
	 * listener removal. This method will be called once for each service
	 * listener removed after this hook had been registered.
	 * 
	 * @param listener A listener which is no longer listening to service
	 * 	events.
	 */
	void removed(Collection listeners);

	/**
	 * A Service Listener wrapper. This immutable class encapsulates a {@link
	 * ServiceListener} and the bundle which added it and the filter with which
	 * it was added. Objects of this type are created by the framework and
	 * passed to the {@link ListenerHook}.
	 * 
	 * @Immutable
	 */
	public interface ListenerInfo {
		/**
		 * Return the context of the bundle which added the listener.
		 * 
		 * @return The context of the bundle which added the listener.
		 */
		public BundleContext getBundleContext();

		/**
		 * Return the filter with which the listener was added.
		 * 
		 * @return The filter with which the listener was added. This may be
		 * 	<code>null</code> if the listener was added without a filter.
		 */
		public String getFilter();
	}
}
