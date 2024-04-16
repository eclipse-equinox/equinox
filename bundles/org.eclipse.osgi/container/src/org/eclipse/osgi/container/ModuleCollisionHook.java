/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package org.eclipse.osgi.container;

import java.util.Collection;

/**
 * Hook used to determine if a module revision being installed or updated will
 * cause a collision
 * 
 * @since 3.10
 */
public interface ModuleCollisionHook {

	/**
	 * Specifies a module install operation is being performed.
	 */
	int INSTALLING = 1;

	/**
	 * Specifies a module update operation is being performed.
	 */
	int UPDATING = 2;

	/**
	 * Filter bundle collisions hook method. This method is called during the
	 * install or update operation. The operation type will be {@link #INSTALLING
	 * installing} or {@link #UPDATING updating}. Depending on the operation type
	 * the target module and the collision candidate collection are the following:
	 * <ul>
	 * <li>{@link #INSTALLING installing} - The target is the module associated
	 * which is performing the install operation. The collision candidate collection
	 * contains the existing modules installed which have a current revision with
	 * the same symbolic name and version as the module being installed.
	 * <li>{@link #UPDATING updating} - The target is the module being updated. The
	 * collision candidate collection contains the existing modules installed which
	 * have a current revision with the same symbolic name and version as the
	 * content the target module is being updated to.
	 * </ul>
	 * This method can filter the collection of collision candidates by removing
	 * potential collisions. For the specified operation to succeed, the collection
	 * of collision candidates must be empty when this method returns.
	 *
	 * @param operationType       The operation type. Must be the value of
	 *                            {@link #INSTALLING installing} or {@link #UPDATING
	 *                            updating}.
	 * @param target              The target module used to determine what collision
	 *                            candidates to filter.
	 * @param collisionCandidates The collection of collision candidates.
	 */
	void filterCollisions(int operationType, Module target, Collection<Module> collisionCandidates);
}
