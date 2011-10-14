/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation 
 *******************************************************************************/

package org.eclipse.equinox.console.common.terminal;

/**
 * For the supported escape sequences, the VT220 and XTERM sequences
 * are one and the same.
 *
 */
public class VT220TerminalTypeMappings extends ANSITerminalTypeMappings {
	
	public VT220TerminalTypeMappings() {
		super();
		
		BACKSPACE = 127;
		DEL = -1;
	}
}
