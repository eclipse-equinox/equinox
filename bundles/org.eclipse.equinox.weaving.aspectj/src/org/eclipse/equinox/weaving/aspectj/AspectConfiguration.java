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

import java.util.Arrays;
import java.util.List;

import org.aspectj.weaver.loadtime.definition.Definition;
import org.osgi.framework.Bundle;

/**
 * An aspect configuration object describes for one specific bundle which
 * aspects from which other bundles are declared to be woven into the bundle.
 * 
 * @author Martin Lippert
 */
public class AspectConfiguration {

    private final Definition[] aspectDefinitions;

    private final Bundle bundle;

    private final String fingerprint;

    /**
     * Creates a new aspect configuration object for the given bundle.
     * 
     * @param bundle The host bundle this configuration belongs to
     * @param aspectDefinitions The set of aspect definitions for the weaver
     * @param fingerprint The fingerprint of the defined aspects
     */
    public AspectConfiguration(final Bundle bundle,
            final Definition[] aspectDefinitions, final String fingerprint) {
        this.bundle = bundle;
        this.fingerprint = fingerprint;
        this.aspectDefinitions = aspectDefinitions;
    }

    /**
     * @return The set of aspect configurations to be used by the weaver
     */
    public List<Definition> getAspectDefinitions() {
        return Arrays.asList(aspectDefinitions);
    }

    /**
     * @return The host bundle this aspect configuration belongs to
     */
    public Bundle getBundle() {
        return bundle;
    }

    /**
     * @return A short version of the different aspects being woven into this
     *         bundle to be used as unique identifier for aspect configurations
     */
    public String getFingerprint() {
        return fingerprint;
    }

}
