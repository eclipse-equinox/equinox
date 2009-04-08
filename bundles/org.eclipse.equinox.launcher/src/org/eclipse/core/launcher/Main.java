/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrew Niefer - IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.launcher;

/**
 * This class exists only for backwards compatibility.
 * The real Main class is now org.eclipse.equinox.launcher.Main.
 * <p>
 * <b>Note:</b> This class should not be referenced programmatically by
 * other Java code. This class exists only for the purpose of launching Eclipse
 * from the command line. To launch Eclipse programmatically, use 
 * org.eclipse.core.runtime.adaptor.EclipseStarter. The fields and methods
 * on this class are not API.
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class Main {

	/**
	 * Pass our args along to the real Main class.
	 * 
	 * @param args the given arguments
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public static void main(String[] args) {
		org.eclipse.equinox.launcher.Main.main(args);
	}

}
