/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import java.util.EventListener;
import java.util.Map;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.registry.IConfigurationElement;

/**
 * This interfaces provides a bridge between the "old" org.eclipse.core.runitime elements
 * and "new" org.eclipse.equinox.registry elements. While registry internally operates
 * with "new" elements, listeners might expect "old" element types.
 * 
 * The interface provides backward compatibility. It is not expected to be implemented
 * or extended by clients.
 *  
 * @since org.eclipse.equinox.registry 1.0
 */
public interface ICompatibilityStrategy {
	/**
	 * Implement to ADD an additional listener invocation mechanism.
	 * (Default meachnism is executed first, this implementation is executed second.)
	 *  
	 * @param listener - the listener to be invoked
	 * @param deltas - event deltas
	 * @param filter - the listener filter string
	 */
	public void invokeListener(EventListener listener, Map deltas, String filter);

	/**
	 * Implement to ADD an additional initialization path for the CreateExecutableExtensions.
	 * (Default meachnism is executed first, this implementation is executed second.)
	 * @param newClassInstance - class just created for an executable extension
	 * @param confElement - ConfigurationElement that triggered creation of this element
	 * @param propertyName - name of the attribute describing class to be created
	 * @param initData - initialization data
	 * @throws CoreException
	 */
	public void setInitializationData(Object newClassInstance, IConfigurationElement confElement, String propertyName, Object initData) throws CoreException;
	
	
	/**
	 * Implement to ADD an additional creation path for the CreateExecutableExtensions. 
	 * (Default meachnism is executed first, this implementation is executed second.)
	 * 
	 * @param result - object being created
	 * @return object returned by the IExecutableExtensionFactory.create() method
	 */
	public Object create(Object result) throws CoreException ;
}
