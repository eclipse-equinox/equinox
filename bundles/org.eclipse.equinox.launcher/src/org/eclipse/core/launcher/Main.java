/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
 * This is a TEMPORARY class to facilitate self hosting of this launcher startup jar
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		org.eclipse.equinox.launcher.Main.main(args);
	}

}
