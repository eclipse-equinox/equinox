/*******************************************************************************
 * Copyright (c) Jan. 28, 2019 Liferay, Inc.
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

import org.osgi.service.http.runtime.dto.ErrorPageDTO;

/**
 * Internal Extended DTO model used for simplifying handling logic.
 */
public class ExtendedErrorPageDTO extends ErrorPageDTO {

	public enum ErrorCodeType {
		RANGE_4XX, RANGE_5XX, SPECIFIC
	}

	/**
	 * Indicates the type of error codes defined. This is calculated by the system.
	 */
	public ErrorCodeType erroCodeType;
}
