/*******************************************************************************
 * Copyright (c) 2005, 2014 Cognos Incorporated, IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Cognos Incorporated - initial API and implementation
 *     IBM Corporation - bug fixes and enhancements
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal.registration;

import org.osgi.dto.DTO;

public abstract class Registration<T, D extends DTO> {

	private final D d;
	private final T t;

	protected int referenceCount;

	public Registration(T t, D d) {
		this.t = t;
		this.d = d;
	}

	public synchronized void addReference() {
		++referenceCount;
	}

	public synchronized void removeReference() {
		--referenceCount;
		if (referenceCount == 0) {
			notifyAll();
		}
	}

	public synchronized void destroy() {
		boolean interrupted = false;
		try {
			while (referenceCount != 0) {
				try {
					(new Exception()).printStackTrace();
					wait();
				} catch (InterruptedException e) {
					// wait until the servlet is inactive but save the interrupted status
					interrupted = true;
				}
			}
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt(); //restore the interrupted state
		}
	}

	public D getD() {
		return d;
	}

	public T getT() {
		return t;
	}

	@Override
	public String toString() {
		return getD().toString();
	}

}
