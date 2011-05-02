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
package org.eclipse.equinox.internal.util.timer;

/**
 * This interface must be implemented by all classes which wish to be registered
 * to the Timer service and to be notified after given time periods.
 * 
 * @see Timer
 * 
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface TimerListener {

	/**
	 * This method will be invoked by Timer to notify the listener for the
	 * expiration of its time period.
	 * 
	 * @param event
	 *            the event code which is supplied when this listener had been
	 *            registered
	 */
	public void timer(int event);

}
