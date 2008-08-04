/*******************************************************************************
 * Copyright (c) 2008 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Martin Lippert              initial implementation      
 *******************************************************************************/

package org.eclipse.equinox.service.weaving;

public class CacheEntry {

    private final byte[] cachedBytes;

    private final boolean dontWeave;

    public CacheEntry(final boolean dontWeave, final byte[] cachedBytes) {
        this.dontWeave = dontWeave;
        this.cachedBytes = cachedBytes;
    }

    public boolean dontWeave() {
        return dontWeave;
    }

    public byte[] getCachedBytes() {
        return cachedBytes;
    }

}
