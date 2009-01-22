/******************************************************************************
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.equinox.concurrent.future;

/**
 * Timeout exception thrown when timeout occurs
 */
public class TimeoutException extends Exception {

	private static final long serialVersionUID = -3198307514925924297L;

	private final long duration;

	public TimeoutException(long time) {
		duration = time;
	}

	public TimeoutException(String message, long time) {
		super(message);
		this.duration = time;
	}

	/**
	 * Return the timeout duration.
	 * @return long the timeout duration that caused this timeout exception.
	 */
	public long getDuration() {
		return duration;
	}
}