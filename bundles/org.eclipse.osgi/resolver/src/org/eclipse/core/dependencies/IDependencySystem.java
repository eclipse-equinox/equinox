/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.dependencies;

import java.util.List;

/** 
 * Not to be implemented by clients.
 */

public interface IDependencySystem {
	public class CyclicSystemException extends Exception {
		public CyclicSystemException(String message) {
			super(message);
		}
	}

	public IResolutionDelta resolve() throws CyclicSystemException;

	public IResolutionDelta resolve(boolean produceDelta) throws CyclicSystemException;

	/**
	 * Returns the delta for the last resolution operation (<code>null</code> if never 
	 * resolved or if last resolved with delta production disabled).
	 */
	public IResolutionDelta getLastDelta();

	public void addElements(IElement[] elementsToAdd);

	public void addElement(IElement element);

	public void removeElements(IElement[] elementsToRemove);

	public void removeElement(IElement element);

	public long getElementCount();

	// returns all resolved elements ordered by pre-requisites
	public List getResolved();

	public IElement getElement(Object id, Object identifier);

	public IElementSet getElementSet(Object id);

	// factory methods - for the case the client does not have its own implementations for dependencies and elements
	public IElement createElement(Object id, Object versionId, IDependency[] dependencies, boolean singleton, Object userObject);

	public IDependency createDependency(Object requiredObjectId, IMatchRule satisfactionRule, Object requiredVersionId, boolean optional, Object userObject);

	// global access to system version comparator
	public int compare(Object obj1, Object obj2);

	public void removeElement(Object id, Object versionId);

	/**
	 * Forces a set of elements to be unresolved. All dependencies the elements may 
	 * have as resolved are also unresolved. Also, any elements currently depending on
	 * the given elements will NOT be automatically unresolved. No delta is generated. 
	 * These changes are only temporary, and do not affect the outcome of the next 
	 * resolution (besides the generated delta).   
	 */
	public void unresolve(IElement[] elements);
}