/*******************************************************************************
 * Copyright (c) 2018 Liferay, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Liferay, Inc. - Bug 530063 - CNFE when session replication
 *                    is used with equinox.http.servlet in bridge mode
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.session;

/**
 * The Http Whiteboard runtime registers a service of this type to allow an
 * external actor to invalidate a session. This requirement is typical of the
 * bridge deployment scenario where the host may need to control session
 * invalidation.
 * <p>
 * <b>Note:</b> This class is part of an interim SPI that is still under
 * development and expected to change significantly before reaching stability.
 * It is being made available at this stage to solicit feedback from pioneering
 * adopters on the understanding that any code that uses this SPI will almost
 * certainly be broken (repeatedly) as the SPI evolves.
 * </p>
 * 
 * @since 1.5
 */
public interface HttpSessionInvalidator {

	/**
	 * Invalidate a session. If no session matching the id is found, nothing
	 * happens. Optionally attempt to invalidate the parent (container) session.
	 *
	 * @param sessionId        the session id to invalidate
	 * @param invalidateParent if true, attempt to invalidate the parent (container)
	 *                         session
	 */
	public void invalidate(String sessionId, boolean invalidateParent);

}
