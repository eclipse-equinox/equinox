/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/
package org.eclipse.equinox.http.servlet;

import java.util.Dictionary;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.http.*;

/**
 * @since 1.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
@ProviderType
public interface ExtendedHttpService extends HttpService {

	/**
	 * @param alias      name in the URI namespace at which the filter is registered
	 * @param filter     the filter object to register
	 * @param initparams initialization arguments for the filter or
	 *                   <code>null</code> if there are none. This argument is used
	 *                   by the filter's <code>FilterConfig</code> object.
	 * @param context    the <code>HttpContext</code> object for the registered
	 *                   filter, or <code>null</code> if a default
	 *                   <code>HttpContext</code> is to be created and used.
	 * @throws javax.servlet.ServletException     if the filter's <code>init</code>
	 *                                            method throws an exception, or the
	 *                                            given filter object has already
	 *                                            been registered at a different
	 *                                            alias.
	 * @throws java.lang.IllegalArgumentException if any of the arguments are
	 *                                            invalid
	 */
	public void registerFilter(String alias, Filter filter, Dictionary<String, String> initparams, HttpContext context)
			throws ServletException, NamespaceException;

	/**
	 * Unregisters a previous filter registration done by the
	 * <code>registerFilter</code> methods.
	 *
	 * <p>
	 * After this call, the registered filter will no longer be available. The Http
	 * Service must call the <code>destroy</code> method of the filter before
	 * returning.
	 * <p>
	 * If the bundle which performed the registration is stopped or otherwise
	 * "unget"s the Http Service without calling {@link #unregisterFilter} then the
	 * Http Service must automatically unregister the filter registration. However,
	 * the <code>destroy</code> method of the filter will not be called in this case
	 * since the bundle may be stopped. {@link #unregisterFilter} must be explicitly
	 * called to cause the <code>destroy</code> method of the filter to be called.
	 * This can be done in the <code>BundleActivator.stop</code> method of the
	 * bundle registering the filter.
	 *
	 * @param filter the filter object to unregister
	 * @throws java.lang.IllegalArgumentException if there is no registration for
	 *                                            the filter or the calling bundle
	 *                                            was not the bundle which
	 *                                            registered the filter.
	 */
	public void unregisterFilter(Filter filter);

}
