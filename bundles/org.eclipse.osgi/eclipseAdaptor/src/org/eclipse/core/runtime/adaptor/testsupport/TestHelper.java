/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.adaptor.testsupport;

import org.eclipse.core.runtime.adaptor.EnvironmentInfo;

/**
 * Exposes methods that are not visible to other bundles for testing 
 * purposes.  
 */
public class TestHelper {
	public static String guessOS(String osName) {
		return EnvironmentInfo.guessOS(osName);
	}
	public static String guessWS(String os) {
		return EnvironmentInfo.guessWS(os);
	}
}
