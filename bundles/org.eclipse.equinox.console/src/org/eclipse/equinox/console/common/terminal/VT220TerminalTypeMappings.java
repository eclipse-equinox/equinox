/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation 
 *******************************************************************************/

package org.eclipse.equinox.console.common.terminal;

/**
 * For the supported escape sequences, the VT220 and XTERM sequences are one and
 * the same.
 */
public class VT220TerminalTypeMappings extends ANSITerminalTypeMappings {

	public VT220TerminalTypeMappings() {
		super();

		BACKSPACE = 127;
		DEL = -1;
	}
}
