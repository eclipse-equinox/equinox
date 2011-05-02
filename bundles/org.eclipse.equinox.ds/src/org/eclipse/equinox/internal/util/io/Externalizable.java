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
package org.eclipse.equinox.internal.util.io;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface for custom serialization. Classes that implements this interface
 * must have public empty constructor so deserialization can be done.
 * 
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface Externalizable {

	/**
	 * Use this method for serialization of object state.
	 */
	public void writeObject(OutputStream oStream) throws Exception;

	/**
	 * Use this method to deserializion of object state.
	 */
	public void readObject(InputStream iStream) throws Exception;

}
