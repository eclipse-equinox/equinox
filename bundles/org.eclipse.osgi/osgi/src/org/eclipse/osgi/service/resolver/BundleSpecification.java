/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;


/**
 * A representation of one bundle import constraint as seen in a 
 * bundle manifest and managed by a state and resolver.
 */
public interface BundleSpecification extends VersionConstraint {

	public boolean isExported();

	public boolean isOptional();

}
