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

import org.eclipse.equinox.console.common.KEYS;
import org.eclipse.equinox.console.common.terminal.TerminalTypeMappings;

public class ANSITerminalTypeMappings extends TerminalTypeMappings {
	
	public ANSITerminalTypeMappings() {
		super();
        BACKSPACE = 8;
        DEL = 127;
	}
	
	public void setKeypadMappings() {
		escapesToKey.put("[1~", KEYS.HOME); //$NON-NLS-1$
        escapesToKey.put("[4~", KEYS.END); //$NON-NLS-1$
        escapesToKey.put("[5~", KEYS.PGUP); //$NON-NLS-1$
        escapesToKey.put("[6~", KEYS.PGDN); //$NON-NLS-1$
        escapesToKey.put("[2~", KEYS.INS); //$NON-NLS-1$
        escapesToKey.put("[3~", KEYS.DEL); //$NON-NLS-1$
	}
}
