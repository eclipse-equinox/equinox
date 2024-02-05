/*******************************************************************************
 * Copyright (c) 2010, 2020 SAP AG and others.
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

public class ANSITerminalTypeMappings extends TerminalTypeMappings {

	public ANSITerminalTypeMappings() {
		super();
		BACKSPACE = 8;
		DEL = 127;
	}

	@Override
	public void setKeypadMappings() {
		escapesToKey.put("[1~", KEYS.HOME); //$NON-NLS-1$
		escapesToKey.put("[4~", KEYS.END); //$NON-NLS-1$
		escapesToKey.put("[5~", KEYS.PGUP); //$NON-NLS-1$
		escapesToKey.put("[6~", KEYS.PGDN); //$NON-NLS-1$
		escapesToKey.put("[2~", KEYS.INS); //$NON-NLS-1$
		escapesToKey.put("[3~", KEYS.DEL); //$NON-NLS-1$
	}
}
