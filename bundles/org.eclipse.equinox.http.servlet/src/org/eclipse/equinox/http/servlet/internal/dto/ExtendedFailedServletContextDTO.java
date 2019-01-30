/*******************************************************************************
 * Copyright (c) Jan. 29, 2019 Liferay, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Liferay, Inc. - initial API and implementation and/or initial
 *                    documentation
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.dto;

import org.osgi.service.http.runtime.dto.FailedServletContextDTO;

/**
 * Internal Extended DTO model used for simplifying handling logic.
 */
public class ExtendedFailedServletContextDTO extends FailedServletContextDTO {

	/**
	 * Holds the serviceId of the service that shadowed this context.
	 */
	public long shadowingServiceId;

}
