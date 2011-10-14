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

public class VT320TerminalTypeMappings extends TerminalTypeMappings {
	
	public VT320TerminalTypeMappings() {
		super();
        BACKSPACE = 8;
        DEL = 127;
	}

	@Override
	public void setKeypadMappings() {
		escapesToKey.put("[H", KEYS.HOME);
		escapesToKey.put("[F", KEYS.END);
        escapesToKey.put("[5~", KEYS.PGUP); //$NON-NLS-1$
        escapesToKey.put("[6~", KEYS.PGDN); //$NON-NLS-1$
        escapesToKey.put("[2~", KEYS.INS); //$NON-NLS-1$
        escapesToKey.put("[3~", KEYS.DEL); //$NON-NLS-1$
		
	}
}
