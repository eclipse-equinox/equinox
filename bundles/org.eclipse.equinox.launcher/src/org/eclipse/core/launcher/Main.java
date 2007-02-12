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
 * Main class for backwards compatibility.
 * The real Main class is now org.eclipse.equinox.launcher.Main
 */
public class Main {

	/**
	 * Pass our args along to the real Main class.
	 * @param args
	 */
	public static void main(String[] args) {
		org.eclipse.equinox.launcher.Main.main(args);
	}

}
