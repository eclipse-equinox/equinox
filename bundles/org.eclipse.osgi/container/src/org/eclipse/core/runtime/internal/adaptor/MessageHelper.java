/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.internal.adaptor;

import java.util.Date;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Constants;

/**
 * @since 3.3
 */
public class MessageHelper {
	public static String getResolutionFailureMessage(VersionConstraint unsatisfied) {
		if (unsatisfied.isResolved())
			throw new IllegalArgumentException();
		if (unsatisfied instanceof ImportPackageSpecification) {
			if (ImportPackageSpecification.RESOLUTION_OPTIONAL.equals(((ImportPackageSpecification) unsatisfied).getDirective(Constants.RESOLUTION_DIRECTIVE)))
				return NLS.bind(EclipseAdaptorMsg.ECLIPSE_MISSING_OPTIONAL_IMPORTED_PACKAGE, toString(unsatisfied));
			if (ImportPackageSpecification.RESOLUTION_DYNAMIC.equals(((ImportPackageSpecification) unsatisfied).getDirective(Constants.RESOLUTION_DIRECTIVE)))
				return NLS.bind(EclipseAdaptorMsg.ECLIPSE_MISSING_DYNAMIC_IMPORTED_PACKAGE, toString(unsatisfied));
			return NLS.bind(EclipseAdaptorMsg.ECLIPSE_MISSING_IMPORTED_PACKAGE, toString(unsatisfied));
		} else if (unsatisfied instanceof BundleSpecification) {
			if (((BundleSpecification) unsatisfied).isOptional())
				return NLS.bind(EclipseAdaptorMsg.ECLIPSE_MISSING_OPTIONAL_REQUIRED_BUNDLE, toString(unsatisfied));
			return NLS.bind(EclipseAdaptorMsg.ECLIPSE_MISSING_REQUIRED_BUNDLE, toString(unsatisfied));
		} else if (unsatisfied instanceof HostSpecification) {
			return NLS.bind(EclipseAdaptorMsg.ECLIPSE_MISSING_HOST, toString(unsatisfied));
		} else if (unsatisfied instanceof NativeCodeSpecification) {
			return NLS.bind(EclipseAdaptorMsg.ECLIPSE_MISSING_NATIVECODE, unsatisfied.toString());
		} else if (unsatisfied instanceof GenericSpecification) {
			return NLS.bind(EclipseAdaptorMsg.ECLIPSE_MISSING_REQUIRED_CAPABILITY, unsatisfied.toString());
		}
		return NLS.bind(EclipseAdaptorMsg.ECLIPSE_MISSING_REQUIREMENT, unsatisfied.toString());
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
