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
