/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.log;

public class FrameworkLogEntry {
	private int depth;
	private String entry;
	private String message;
	private int stackCode;
	private Throwable throwable;

	public FrameworkLogEntry(int depth, String entry, String message, int stackCode, Throwable throwable) {
		this.depth = depth;
		this.entry = entry;
		this.message = message;
		this.stackCode = stackCode;
		this.throwable = throwable;
	}
	/**
	 * @return Returns the depth.
	 */
	public int getDepth() {
		return depth;
	}
	/**
	 * @return Returns the entry.
	 */
	public String getEntry() {
		return entry;
	}
	/**
	 * @return Returns the message.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return Returns the stackCode.
	 */
	public int getStackCode() {
		return stackCode;
	}
	/**
	 * @return Returns the throwable.
	 */
	public Throwable getThrowable() {
		return throwable;
	}
}
