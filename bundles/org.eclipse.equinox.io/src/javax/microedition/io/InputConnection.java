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
package javax.microedition.io;

public abstract interface InputConnection extends javax.microedition.io.Connection {

	public abstract java.io.DataInputStream openDataInputStream() throws java.io.IOException;

	public abstract java.io.InputStream openInputStream() throws java.io.IOException;

}
