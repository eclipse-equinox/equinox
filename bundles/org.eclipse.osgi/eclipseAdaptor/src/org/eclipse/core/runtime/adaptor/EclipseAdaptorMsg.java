/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor;

import java.util.Date;
import org.eclipse.osgi.framework.msg.MessageFormat;
import org.eclipse.osgi.framework.util.FrameworkMessageFormat;
import org.eclipse.osgi.service.resolver.*;

/**
 * This class retrieves strings from a resource bundle and returns them,
 * formatting them with MessageFormat when required.
 * <p>
 * It is used by the system classes to provide national language support, by
 * looking up messages in the <code>
 *    org.eclipse.osgi.framework.internal.core.ExternalMessages
 * </code>
 * resource bundle. Note that if this file is not available, or an invalid key
 * is looked up, or resource bundle support is not available, the key itself
 * will be returned as the associated message. This means that the <em>KEY</em>
 * should a reasonable human-readable (english) string.
 * 
 * <p>Internal class.</p>
 */
public class EclipseAdaptorMsg {
	public static final String NEW_LINE = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
	public static MessageFormat formatter;
	// Attempt to load the message bundle.
	static {
		formatter = FrameworkMessageFormat.getMessageFormat("org.eclipse.core.runtime.adaptor.EclipseAdaptorMessages"); //$NON-NLS-1$
	}

	public static String getResolutionFailureMessage(VersionConstraint unsatisfied) {
		if (unsatisfied.isResolved())
			throw new IllegalArgumentException();
		if (unsatisfied instanceof ImportPackageSpecification)
			return EclipseAdaptorMsg.formatter.getString("ECLIPSE_MISSING_IMPORTED_PACKAGE", toString(unsatisfied)); //$NON-NLS-1$
		else if (unsatisfied instanceof BundleSpecification)
			if (((BundleSpecification) unsatisfied).isOptional())
				return EclipseAdaptorMsg.formatter.getString("ECLIPSE_MISSING_OPTIONAL_REQUIRED_BUNDLE", toString(unsatisfied)); //$NON-NLS-1$
			else
				return EclipseAdaptorMsg.formatter.getString("ECLIPSE_MISSING_REQUIRED_BUNDLE", toString(unsatisfied));//$NON-NLS-1$
		else
			return EclipseAdaptorMsg.formatter.getString("ECLIPSE_MISSING_HOST", toString(unsatisfied)); //$NON-NLS-1$
	}
	
	/**
	 * Print a debug message to the console. 
	 * Pre-pend the message with the current date and the name of the current thread.
	 */
	public static void debug(String message) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(new Date(System.currentTimeMillis()));
		buffer.append(" - ["); //$NON-NLS-1$
		buffer.append(Thread.currentThread().getName());
		buffer.append("] "); //$NON-NLS-1$
		buffer.append(message);
		System.out.println(buffer.toString());
	}

	private static String toString(VersionConstraint constraint) {
		org.eclipse.osgi.service.resolver.VersionRange versionRange = constraint.getVersionRange();
		if (versionRange == null)
			return constraint.getName();
		return constraint.getName() + '_' + versionRange;
	}
}
