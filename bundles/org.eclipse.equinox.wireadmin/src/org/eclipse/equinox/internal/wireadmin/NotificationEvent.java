/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.wireadmin;

import org.osgi.service.wireadmin.*;

/**
 * This class holds the producer and consumer which have to be notified for
 * changes in their wires
 * 
 * @author Stoyan Boshev
 * @author Pavlin Dobrev
 * @version 1.0
 */
public class NotificationEvent {

	Producer producer;
	Consumer consumer;
	Wire source;
	Wire[] wires;

	public NotificationEvent(Producer pr, Consumer cm, Wire source, Wire[] wires) {
		producer = pr;
		consumer = cm;
		this.source = source;
		this.wires = wires;
	}

}
