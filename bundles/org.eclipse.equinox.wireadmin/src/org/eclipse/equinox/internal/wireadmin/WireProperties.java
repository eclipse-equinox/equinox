/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
