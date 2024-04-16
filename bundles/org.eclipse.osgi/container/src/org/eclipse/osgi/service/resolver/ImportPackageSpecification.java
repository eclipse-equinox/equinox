/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
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
package org.eclipse.osgi.service.resolver;

import java.util.Map;

/**
 * A representation of one package import constraint as seen in a bundle
 * manifest and managed by a state and resolver.
 * <p>
 * This interface is not intended to be implemented by clients. The
 * {@link StateObjectFactory} should be used to construct instances.
 * </p>
 * 
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ImportPackageSpecification extends VersionConstraint {
	/**
	 * The static resolution directive value.
	 */
	public static final String RESOLUTION_STATIC = "static"; //$NON-NLS-1$
	/**
	 * The optional resolution directive value.
	 */
	public static final String RESOLUTION_OPTIONAL = "optional"; //$NON-NLS-1$
	/**
	 * The dynamic resolution directive value.
	 */
	public static final String RESOLUTION_DYNAMIC = "dynamic"; //$NON-NLS-1$

	/**
	 * Returns the symbolic name of the bundle this import package must be resolved
	 * to.
	 * 
	 * @return the symbolic name of the bundle this import pacakge must be resolved
	 *         to. A value of <code>null</code> indicates any symbolic name.
	 */
	public String getBundleSymbolicName();

	/**
	 * Returns the version range which this import package may be resolved to.
	 * 
	 * @return the version range which this import package may be resolved to.
	 */
	public VersionRange getBundleVersionRange();

	/**
	 * Returns the arbitrary attributes which this import package may be resolved
	 * to.
	 * 
	 * @return the arbitrary attributes which this import package may be resolved
	 *         to.
	 */
	public Map<String, Object> getAttributes();

	/**
	 * Returns the directives that control this import package.
	 * 
	 * @return the directives that control this import package.
	 */
	public Map<String, Object> getDirectives();

	/**
	 * Returns the specified directive that control this import package.
	 * 
	 * @return the specified directive that control this import package.
	 */
	public Object getDirective(String key);
}
