/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipsei.equinox.event.mapper;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogReaderService;

/**
 * @version $Revision: 1.4 $
 */
public interface LogReaderServiceListener {
	public void logReaderServiceAdding(ServiceReference reference,
			LogReaderService service);

	public void logReaderServiceRemoved(ServiceReference reference,
			LogReaderService service);
}