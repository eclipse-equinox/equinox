/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.provisional.service.security;

import org.eclipse.osgi.signedcontent.SignedContent;

/**
 * An event that is fired when an AuthorizationEngine implementation makes
 * a decision. 
 * @since 3.4
 */
public class AuthorizationEvent {

	/**
	 * Result code meaning that the operation was allowed
	 */
	public static final int ALLOWED = 0;

	/**
	 * Result code meaning that the operation was denied
	 */
	public static final int DENIED = 1;

	private final int result;
	private final SignedContent content;
	private final Object context;
	private final int severity;

	/**
	 * Create a new AuthorizationEvent
	 * @param result - the result code 
	 * @param content - the signed content
	 * @param context - operation specific context
	 * @param severity - severity code
	 */
	public AuthorizationEvent(int result, SignedContent content, Object context, int severity) {
		this.result = result;
		this.content = content;
		this.context = context;
		this.severity = severity;
	}

	/**
	 * Get the result code
	 * @return - the result code
	 */
	public int getResult() {
		return result;
	}

	/**
	 * get the severity
	 * @return - the severity
	 */
	public int getSeverity() {
		return severity;
	}

	/**
	 * Get the SignedContent object being evaluated
	 * @return - SignedContent
	 */
	public SignedContent getSignedContent() {
		return content;
	}

	/**
	 * Get the operation specific context
	 * @return - context
	 */
	public Object getContext() {
		return context;
	}
}
