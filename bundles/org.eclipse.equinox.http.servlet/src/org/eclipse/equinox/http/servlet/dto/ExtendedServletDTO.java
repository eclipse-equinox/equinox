/*******************************************************************************
 * Copyright (c) 2016, 2019 Raymond Augé.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 497271
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.dto;

import org.osgi.service.http.runtime.dto.ServletDTO;

/**
 * This type may become irrelevant if the properties appear as part of a
 * future OSGi Http Whiteboard specification.
 */
@Deprecated
public class ExtendedServletDTO extends ServletDTO {
	// deprecated
}
