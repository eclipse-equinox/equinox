/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.wireadmin;

import java.util.Hashtable;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

class WireProperties extends Hashtable {
	
	static final long serialVersionUID = -8836718065933570367L;

	public synchronized Object put(Object key, Object value) {
		throw new RuntimeException("Unsupported operation");
	}

	public synchronized Object remove(Object key) {
		throw new RuntimeException("Unsupported operation");
	}

	Object put0(Object key, Object value) {
		return super.put(key, value);
	}

	Object remove0(Object key) {
		return super.remove(key);
	}
}
