/*
 * Copyright (c) OSGi Alliance (2012, 2014). All Rights Reserved.
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

package org.osgi.service.http.runtime.dto;

/**
 * Defines standard constants for the DTOs.
 */
public final class DTOConstants {
	private DTOConstants() {
		// non-instantiable
	}

	/**
	 * Failure reason is unknown
	 * <p>
	 * The value of {@code FAILURE_REASON_UNKNOWN} is 0.
	 */
	public static final int	FAILURE_REASON_UNKNOWN		= 0;

	/**
	 * No matching servlet context
	 * <p>
	 * The value of {@code FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING} is 1.
	 **/
	public static final int	FAILURE_REASON_NO_SERVLET_CONTEXT_MATCHING	= 1;

	/**
	 * Matching servlet context, but servlet context is not used due to a
	 * problem with the context.
	 * <p>
	 * The value of {@code FAILURE_REASON_SERVLET_CONTEXT_FAILURE} is 2.
	 */
	public static final int	FAILURE_REASON_SERVLET_CONTEXT_FAILURE		= 2;

	/**
	 * Service is shadowed by another service, e.g. a service with the same
	 * registration properties but a higher service ranking.
	 * <p>
	 * The value of {@code FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE} is 3.
	 */
	public static final int	FAILURE_REASON_SHADOWED_BY_OTHER_SERVICE	= 3;

	/**
	 * An exception occurred during initializing of the service. This reason can
	 * only happen for servlets and servlet filters.
	 * <p>
	 * The value of {@code FAILURE_REASON_EXCEPTION_ON_INIT} is 4.
	 */
	public static final int	FAILURE_REASON_EXCEPTION_ON_INIT			= 4;

	/**
	 * The service is registered in the servlet registry but getting the service
	 * fails as it returns {@code null}.
	 * <p>
	 * The value of {@code FAILURE_REASON_SERVICE_NOT_GETTABLE} is 5.
	 */
	public static final int	FAILURE_REASON_SERVICE_NOT_GETTABLE			= 5;
}
