/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
