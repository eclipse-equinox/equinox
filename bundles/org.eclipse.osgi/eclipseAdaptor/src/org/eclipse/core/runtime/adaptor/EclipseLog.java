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
package org.eclipse.core.runtime.adaptor;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.eclipse.osgi.framework.internal.defaultadaptor.DefaultLog;

public class EclipseLog extends DefaultLog {
	private static final String PASSWORD = "-password"; //$NON-NLS-1$	
	public EclipseLog(File logFile) {
		super(logFile);
	}
	public EclipseLog(Writer logWriter) {
		super(logWriter);
	}
	public EclipseLog() {
		// default constructor
	}
	protected String getDate() {
		try {
			DateFormat formatter = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss.SS"); //$NON-NLS-1$
			return formatter.format(new Date());
		} catch (Exception e) {
			// If there were problems writing out the date, ignore and
			// continue since that shouldn't stop us from logging the rest
			// of the information
		}
		return Long.toString(System.currentTimeMillis());
	}
	protected void writeSession() throws IOException {
		write(SESSION);
		writeSpace();
		//TODO "timestamp" is a more correct name
		String date = getDate();
		write(date);
		writeSpace();
		for (int i = SESSION.length() + date.length(); i < 78; i++) {
			write("-"); //$NON-NLS-1$
		}
		writeln();
		// Write out certain values found in System.getProperties()
		try {
			String key = "java.fullversion"; //$NON-NLS-1$
			String value = System.getProperty(key);
			if (value == null) {
				key = "java.version"; //$NON-NLS-1$
				value = System.getProperty(key);
				writeln(key + "=" + value); //$NON-NLS-1$
				key = "java.vendor"; //$NON-NLS-1$
				value = System.getProperty(key);
				writeln(key + "=" + value); //$NON-NLS-1$
			} else {
				writeln(key + "=" + value); //$NON-NLS-1$
			}
		} catch (Exception e) {
			// If we're not allowed to get the values of these properties
			// then just skip over them.
		}
		// The Bootloader has some information that we might be interested in.
		write("BootLoader constants: OS=" + EnvironmentInfo.getDefault().getOS()); //$NON-NLS-1$
		write(", ARCH=" + EnvironmentInfo.getDefault().getOSArch()); //$NON-NLS-1$
		write(", WS=" + EnvironmentInfo.getDefault().getWS()); //$NON-NLS-1$
		writeln(", NL=" + EnvironmentInfo.getDefault().getNL()); //$NON-NLS-1$
		// Add the command-line arguments used to invoke the platform 
		// XXX: this includes runtime-private arguments - should we do that?
		String[] args = EnvironmentInfo.getDefault().getNonFrameworkArgs();
		if (args != null && args.length > 0) {
			write("Command-line arguments:"); //$NON-NLS-1$
			for (int i = 0; i < args.length; i++) {
				//mask out the password argument for security
				if (i > 0 && PASSWORD.equals(args[i - 1]))
					write(" (omitted)"); //$NON-NLS-1$
				else
					write(" " + args[i]); //$NON-NLS-1$
			}
			writeln();
		}
	}

	public synchronized void setFile(File newFile, boolean append)
			throws IOException {
		super.setFile(newFile, append);
		System.setProperty(EclipseStarter.PROP_LOGFILE, newFile.getAbsolutePath());
	}
}