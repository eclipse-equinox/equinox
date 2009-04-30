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
     * Header for aspect bundle manifest files to indicate whether a bundle is
     * readily compiled with AJDT, for example. This tells Equinox Aspects that
     * the bundle does not need to be woven if it refers to just its own
     * aspects.
     */
    public static final String AOP_BUNDLE_FINISHED_HEADER = "Eclipse-AspectBundle"; //$NON-NLS-1$

    /**
     * The value for the aspect bundle header to indicate that there is no
     * weaving necessary to finish the aspects of the bundle itself
     */
    public static final String AOP_BUNDLE_FINISHED_VALUE = "finished"; //$NON-NLS-1$

    /**
     * This is the default value for the location of the aop.xml file inside a
     * bundle
     */
    public static final String AOP_CONTEXT_DEFAULT_LOCATION = "META-INF/aop.xml"; //$NON-NLS-1$

    /**
     * The name of the header to determine where to look for the aop.xml files
     * inside a bundle Bundles can use this header to specify the location where
     * their aop.xml is located
     */
    public static final String AOP_CONTEXT_LOCATION_HEADER = "Eclipse-AspectContext"; //$NON-NLS-1$

    /**
     * directive for the policy to apply aspects from imported or required
     * bundles
     */
    public static final String ASPECT_APPLY_POLICY_DIRECTIVE = "apply-aspects"; //$NON-NLS-1$

    /**
     * apply policy is false in this case, do not apply aspects for weaving
     */
    public static final int ASPECT_APPLY_POLICY_FALSE = 2;

    /**
     * apply policy is not defined
     */
    public static final int ASPECT_APPLY_POLICY_NOT_DEFINED = 0;

    /**
     * apply policy is true, so apply aspects for weaving
     */
    public static final int ASPECT_APPLY_POLICY_TRUE = 1;

    /**
     * directive to declare the aspect policy. possible values are "opt-in" or
     * "opt-out"
     */
    public static final String ASPECT_POLICY_DIRECTIVE = "aspect-policy"; //$NON-NLS-1$

    /**
     * policy directive value to tell the weaver that clients have explicitly to
     * ask for those aspects to be applied
     */
    public static final String ASPECT_POLICY_DIRECTIVE_OPT_IN = "opt-in"; //$NON-NLS-1$

    /**
     * policy directive value to tell the weaver that clients will get those
     * aspects applied automatically unless they ask for not applying them
     */
    public static final String ASPECT_POLICY_DIRECTIVE_OPT_OUT = "opt-out"; //$NON-NLS-1$

    /**
     * This indicates that there is no aspects policy defined
     */
    public static final int ASPECT_POLICY_NOT_DEFINED = 0;

    /**
     * Policy to indicate that the aspects of this package should only be woven
     * if the importer explicitly asks for it
     */
    public static final int ASPECT_POLICY_OPT_IN = 1;

    /**
     * Policy to indicate that the aspects of this package should automatically
     * be woven if the importer does not prohibit it
     */
    public static final int ASPECT_POLICY_OPT_OUT = 2;

    /**
     * directive to declare the exported aspects. The values should list the
     * aspect class names without the package
     */
    public static final String ASPECTS_ATTRIBUTE = "aspects"; //$NON-NLS-1$

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

    /**
     * Calculates the set of aspects to be woven if the given imported package
     * is wired to the given bundle (with the given policy on applying aspects)
     * 
     * @param bundle The bundle from which the given package is imported
     * @param packageName The name of the package that is imported
     * @param applyAspectsPolicy the policy for applying visible aspects for
     *            weaving
     * @return The set of aspects that should be woven from the given imported
     *         package
     */
    public Definition resolveImportedPackage(final Bundle bundle,
            String packageName, final int applyAspectsPolicy);

    /**
     * Calculates the set of aspects to be woven if the given bundle is declared
     * as a required bundle (with the given policy on applying aspects)
     * 
     * @param bundle The bundle which is required and might export aspects that
     *            should be woven
     * @param applyAspectsPolicy the policy for applying visible aspects for
     *            weaving
     * @return The set of aspects that should be woven from the given required
     *         bundle
     */
    public Definition resolveRequiredBundle(final Bundle bundle,
            final int applyAspectsPolicy);

}
