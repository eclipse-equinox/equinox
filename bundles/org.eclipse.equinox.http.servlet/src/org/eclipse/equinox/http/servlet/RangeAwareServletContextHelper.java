/*******************************************************************************
 * Copyright (c) Jan 20, 2018 Liferay, Inc.
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

package org.eclipse.equinox.http.servlet;

import org.osgi.framework.Bundle;
import org.osgi.service.http.context.ServletContextHelper;

/**
 * A custom servlet context helper type providing support for predicting the
 * need for ranged content responses based on the content type and the user
 * agent.
 *
 * @since 1.5
 */
public abstract class RangeAwareServletContextHelper extends ServletContextHelper {

	public RangeAwareServletContextHelper() {
		super();
	}

	public RangeAwareServletContextHelper(Bundle bundle) {
		super(bundle);
	}

	/**
	 * Return true if the content type should result in a ranged content response
	 * based on the user agent. The user agent value is obtained from the
	 * {@code User-Agent} request header.
	 * <p>
	 * This mechanism is only applicable if the browser didn't make a range request
	 * for a known ranged content type.
	 *
	 * @param contentType the content type of the request
	 * @param userAgent   the value obtained from the "User-Agent" header
	 */
	public boolean rangeableContentType(String contentType, String userAgent) {
		return false;
	}

}
