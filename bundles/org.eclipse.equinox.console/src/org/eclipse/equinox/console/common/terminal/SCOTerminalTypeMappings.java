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

public class SCOTerminalTypeMappings extends TerminalTypeMappings {
	
	public SCOTerminalTypeMappings() {
		super();
		
		BACKSPACE = -1;
		DEL = 127;
	}

	@Override
	public void setKeypadMappings() {
		escapesToKey.put("[H", KEYS.HOME);
		escapesToKey.put("F", KEYS.END);
		escapesToKey.put("[L", KEYS.INS);
		escapesToKey.put("[I", KEYS.PGUP);
		escapesToKey.put("[G", KEYS.PGDN);
	}
}
