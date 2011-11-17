/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.coordinator;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.coordinator.messages"; //$NON-NLS-1$
	public static String NullParameter;
	public static String Deadlock;
	public static String InvalidCoordinationName;
	public static String MissingFailureCause;
	public static String InvalidTimeInterval;
	public static String InterruptedTimeoutExtension;
	public static String EndingThreadNotSame;
	public static String LockInterrupted;
	public static String ParticipantEndedError;
	public static String CoordinationPartiallyEnded;
	public static String ParticipantFailedError;
	public static String CoordinationFailed;
	public static String CoordinationEnded;
	public static String CoordinationTimedOutError;
	public static String MaxCoordinationIdExceeded;
	public static String GetCoordinationNotPermitted;
	public static String CoordinatorShutdown;
	public static String CoordinationAlreadyExists;
	public static String CanceledTaskNotPurged;
	public static String OrphanedCoordinationError;
	public static String MaximumTimeout;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		// noop
	}
}
