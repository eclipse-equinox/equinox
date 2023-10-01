/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipse.osgi.internal.log;

import org.osgi.framework.ServiceReference;

public class Arguments {
	private final Object[] arguments;
	private final ServiceReference<?> serviceReference;
	private final Throwable throwable;

	public Arguments(Object... arguments) {
		if (arguments == null || arguments.length == 0) {
			this.arguments = new Object[0];
			serviceReference = null;
			throwable = null;
			return;
		}
		int length = arguments.length;
		ServiceReference<?> context = null;
		Throwable exception = null;
		Object object = arguments[arguments.length - 1];
		if (object instanceof Throwable || object instanceof ServiceReference) {
			length--;
			if (object instanceof Throwable) {
				exception = (Throwable) object;
			} else {
				context = (ServiceReference<?>) object;
			}
			if (arguments.length > 1) {
				object = arguments[arguments.length - 2];
				if ((object instanceof ServiceReference && context == null)
						|| (object instanceof Throwable && exception == null)) {
					length--;
					if (object instanceof Throwable) {
						exception = (Throwable) object;
					} else {
						context = (ServiceReference<?>) object;
					}
				}
			}
		}
		serviceReference = context;
		throwable = exception;
		this.arguments = new Object[length];
		System.arraycopy(arguments, 0, this.arguments, 0, length);
	}

	public Object[] arguments() {
		return arguments;
	}

	public boolean isEmpty() {
		return arguments == null || arguments.length == 0;
	}

	public ServiceReference<?> serviceReference() {
		return serviceReference;
	}

	public Throwable throwable() {
		return throwable;
	}
}
