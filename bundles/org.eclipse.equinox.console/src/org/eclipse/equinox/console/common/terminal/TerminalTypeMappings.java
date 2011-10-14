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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.equinox.console.common.KEYS;

/**
 * This is the base class for all supported terminal types. 
 * It contains the escape sequences, common for all mappings. 
 */
public abstract class TerminalTypeMappings {
	protected Map<String, KEYS> escapesToKey;
	protected String[] escapes;
	protected byte BACKSPACE;
	protected byte DEL;
	
	public TerminalTypeMappings() {
		escapesToKey = new HashMap<String, KEYS>();
        escapesToKey.put("[A", KEYS.UP); //$NON-NLS-1$
        escapesToKey.put("[B", KEYS.DOWN); //$NON-NLS-1$
        escapesToKey.put("[C", KEYS.RIGHT); //$NON-NLS-1$
        escapesToKey.put("[D", KEYS.LEFT); //$NON-NLS-1$
        escapesToKey.put("[G", KEYS.CENTER); //$NON-NLS-1$
        setKeypadMappings();
        createEscapes();
	}
	
	public Map<String, KEYS> getEscapesToKey() {
		return escapesToKey;
	}
	
	public String[] getEscapes() {
        if (escapes != null) {
        	String[] copy = new String[escapes.length];
        	System.arraycopy(escapes, 0, copy, 0, escapes.length);
        	return copy;
        } else {
            return null;
        }
	}
	
	public byte getBackspace() {
		return BACKSPACE;
	}
	
	public byte getDel() {
		return DEL;
	}
	
	public abstract void setKeypadMappings();
	
	private void createEscapes() {
		escapes = new String[escapesToKey.size()];
        Object[] temp = escapesToKey.keySet().toArray();
        for (int i = 0; i < escapes.length; i++) {
            escapes[i] = (String) temp[i];
        }
	}
}
