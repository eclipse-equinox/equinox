/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.core.runtime.preferences;

import org.osgi.service.prefs.BackingStoreException;

/**
 * This interface is implemented by objects that visit preference nodes.
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>
 * class Visitor implements IPreferenceNodeVisitor {
 *    public boolean visit(IEclipsePreferences node) {
 *       // your code here
 *       return true;
 *    }
 * }
 * IEclipsePreferences root = ...;
 * root.accept(new Visitor());
 * </pre>
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @see IEclipsePreferences#accept(IPreferenceNodeVisitor)
 * @since 3.0
 */
public interface IPreferenceNodeVisitor {

	/**
	 * Visits the given preference node.
	 *
	 * @param node the node to visit
	 * @return <code>true</code> if the node's children should be visited;
	 *         <code>false</code> if they should be skipped
	 * @throws BackingStoreException if this operation cannot be completed due to a
	 *                               failure in the backing store, or inability to
	 *                               communicate with it.
	 */
	public boolean visit(IEclipsePreferences node) throws BackingStoreException;
}
