/*******************************************************************************
 * Copyright (c) 2009 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Martin Lippert               initial implementation
 *******************************************************************************/

package org.eclipse.equinox.weaving.aspectj;

import org.aspectj.weaver.loadtime.definition.Definition;
import org.osgi.framework.Bundle;

/**
 * The AspectAdmin gives you detailed information about bundles providing
 * aspects, which aspects are provided, which aspects are exported and the
 * defined aspect policies for exported packages.
 * 
 * This service is used by the weaver to determine which aspects to weave into
 * which bundles. It can also be used to implement some basic management
 * features for the AspectJ-based weaving of Equinox Aspects.
 * 
 * @author Martin Lippert
 */
public interface AspectAdmin {

    /**
     * The name of the header to determine where to look for the aop.xml files
     * inside a bundle Bundles can use this header to specify the location where
     * their aop.xml is located
     */
    public static final String AOP_CONTEXT_LOCATION_HEADER = "Eclipse-AspectContext"; //$NON-NLS-1$

    /**
     * This is the default value for the location of the aop.xml file inside a
     * bundle
     */
    public static final String DEFAULT_AOP_CONTEXT_LOCATION = "META-INF/aop.xml"; //$NON-NLS-1$

    /**
     * Policy to indicate that the aspects of this package should only be woven
     * if the importer explicitly asks for it
     */
    public static final int OPT_IN_POLICY = 1;

    /**
     * Policy to indicate that the aspects of this package should automatically
     * be woven if the importer does not prohibit it
     */
    public static final int OPT_OUT_POLICY = 2;

    /**
     * Returns the cached aspect definition for the given bundle, if the bundle
     * has an aspect definition
     * 
     * @param bundle The bundle for which the aspect definition should be
     *            returned
     * @return The parsed and cached aspect definition for the given bundle or
     *         null, if the bundle doesn't contain an aspect definition
     */
    public Definition getAspectDefinition(final Bundle bundle);

    /**
     * Gives information on which aspect policy is defined for the given package
     * of the given bundle
     * 
     * @param bundle The bundle which contains the aspects for the given package
     * @param packageName The name of the package that contains the aspects
     * @return OPT_IN_POLICY or OPT_OUT_POLICY (where OPT_OUT_POLICY is the
     *         defailt case)
     */
    public int getAspectPolicy(Bundle bundle, String packageName);

    /**
     * Returns the definitions of aspects whose packages are exported and
     * therefore visible to the outside of the bundle.
     * 
     * @param bundle The bundle for which the visible aspects should be
     *            calculated
     * @return The definition for the exported aspects or null, if no aspect is
     *         visible
     */
    public Definition getExportedAspectDefinitions(final Bundle bundle);

}
