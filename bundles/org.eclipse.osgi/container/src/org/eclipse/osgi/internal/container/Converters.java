/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.container;

import java.util.Collection;
import java.util.List;
import org.osgi.framework.wiring.*;
import org.osgi.resource.*;

public class Converters {

	/**
	 * Coerce the generic type of a list from List<BundleCapability>
	 * to List<Capability>
	 * @param l List to be coerced.
	 * @return l coerced to List<Capability>
	 */
	@SuppressWarnings("unchecked")
	public static List<Capability> asListCapability(List<? extends Capability> l) {
		return (List<Capability>) l;
	}

	/**
	 * Coerce the generic type of a list from List<BundleRequirement>
	 * to List<Requirement>
	 * @param l List to be coerced.
	 * @return l coerced to List<Requirement>
	 */
	@SuppressWarnings("unchecked")
	public static List<Requirement> asListRequirement(List<? extends Requirement> l) {
		return (List<Requirement>) l;
	}

	/**
	 * Coerce the generic type of a list from List<? extends BundleCapability>
	 * to List<BundleCapability>
	 * @param l List to be coerced.
	 * @return l coerced to List<BundleCapability>
	 */
	@SuppressWarnings("unchecked")
	public static List<BundleCapability> asListBundleCapability(List<? extends BundleCapability> l) {
		return (List<BundleCapability>) l;
	}

	/**
	 * Coerce the generic type of a list from List<? extends BundleRequirement>
	 * to List<BundleRequirement>
	 * @param l List to be coerced.
	 * @return l coerced to List<BundleRequirement>
	 */
	@SuppressWarnings("unchecked")
	public static List<BundleRequirement> asListBundleRequirement(List<? extends BundleRequirement> l) {
		return (List<BundleRequirement>) l;
	}

	/**
	 * Coerce the generic type of a list from List<? extends BundleWire>
	 * to List<BundleWire>
	 * @param l List to be coerced.
	 * @return l coerced to List<BundleWire>
	 */
	@SuppressWarnings("unchecked")
	public static List<BundleWire> asListBundleWire(List<? extends BundleWire> l) {
		return (List<BundleWire>) l;
	}

	/**
	 * Coerce the generic type of a list from List<? extends BundleWire>
	 * to List<BundleWire>
	 * @param l List to be coerced.
	 * @return l coerced to List<BundleWire>
	 */
	@SuppressWarnings("unchecked")
	public static List<Wire> asListWire(List<? extends Wire> l) {
		return (List<Wire>) l;
	}

	/**
	 * Coerce the generic type of a list from List<? extends BundleRevision>
	 * to List<BundleRevision>
	 * @param l List to be coerced.
	 * @return l coerced to List<BundleRevision>
	 */
	@SuppressWarnings("unchecked")
	public static List<BundleRevision> asListBundleRevision(List<? extends BundleRevision> l) {
		return (List<BundleRevision>) l;
	}

	/**
	 * Coerce the generic type of a collection from Collection<? extends Resource>
	 * to Collection<Resource>
	 * @param c List to be coerced.
	 * @return c coerced to Collection<Resource>
	 */
	@SuppressWarnings("unchecked")
	public static Collection<Resource> asCollectionResource(Collection<? extends Resource> c) {
		return (Collection<Resource>) c;
	}
}
