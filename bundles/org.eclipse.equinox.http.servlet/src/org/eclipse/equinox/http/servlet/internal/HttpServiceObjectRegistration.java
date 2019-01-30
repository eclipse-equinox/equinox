/*******************************************************************************
 * Copyright (c) 2014, 2019 Liferay, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Liferay, Inc. - initial API and implementation and/or initial
 *                    documentation
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;

public class HttpServiceObjectRegistration {
	public final Object serviceKey;
	public final ServiceRegistration<?> registration;
	public final Bundle bundle;
	public HttpServiceObjectRegistration(
		Object serviceKey, ServiceRegistration<?> registration,
		Bundle bundle) {
		this.serviceKey = serviceKey;
		this.registration = registration;
		this.bundle = bundle;
	}
}
