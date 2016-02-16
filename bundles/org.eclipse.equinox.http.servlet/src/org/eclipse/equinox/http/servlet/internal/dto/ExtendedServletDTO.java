/*******************************************************************************
 * Copyright (c) 2016 Raymond Augé.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Raymond Augé - Initial implementation
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal.dto;

import org.osgi.service.http.runtime.dto.ServletDTO;

public class ExtendedServletDTO extends ServletDTO {
	public boolean	multipartSupported = false;
}