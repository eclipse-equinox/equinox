/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

import org.eclipse.core.runtime.spi.RegistryContributor;

/**
 * The contributor factory creates new registry contributors for use in a simple
 * registry based on the String representation of the determining object.
 * <p>
 * This class can be used without OSGi running.
 * </p>
 * <p>
 * This class can not be extended or instantiated by clients.
 * </p>
 * 
 * @since org.eclipse.equinox.registry 3.2
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public final class ContributorFactorySimple {

	/**
	 * Creates registry contributor object based on a determining object.The
	 * determining object must not be <code>null</code>.
	 *
	 * @param determiningObject object associated with the contribution
	 * @return new registry contributor based on the determining object
	 */
	public static IContributor createContributor(Object determiningObject) {
		String id = determiningObject.toString();
		return new RegistryContributor(id, id, null, null);
	}
}
