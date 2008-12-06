/*******************************************************************************
 * Copyright (c) 2008 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Martin Lippert               initial implementation      
 *******************************************************************************/

package org.eclipse.equinox.service.weaving;

import org.osgi.framework.Bundle;

public interface ICachingServiceFactory {

    public ICachingService createCachingService(ClassLoader classLoader,
            Bundle bundle, String key);

}
