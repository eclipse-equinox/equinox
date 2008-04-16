/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.resolver;

/**
 * This class represents a native code specification.  A 
 * native code specification is different from other 
 * specification constraints which typically are resolved against
 * suppliers provided by other bundles.   A native code 
 * specification supplies it own suppliers which are matched
 * against the platform properties at resolve time and the 
 * supplier with the best match is selected.
 * <p>
 * This interface is not intended to be implemented by clients.  The
 * {@link StateObjectFactory} should be used to construct instances.
 * </p>
 * @since 3.4
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface NativeCodeSpecification extends VersionConstraint {
	/**
	 * Returns the list of possible suppliers to this native code specification.  When 
	 * this native code specification is resolved one of the possible suppliers
	 * will be selected and returned by {@link VersionConstraint#getSupplier()}.
	 * @return the list of possible suppliers.
	 */
	public NativeCodeDescription[] getPossibleSuppliers();

	/**
	 * Returns whether or not this native code specification is optional.
	 * 
	 * @return whether this specification is optional
	 */
	public boolean isOptional();
}
