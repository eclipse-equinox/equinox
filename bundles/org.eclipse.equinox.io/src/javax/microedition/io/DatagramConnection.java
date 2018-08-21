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

public abstract interface DatagramConnection extends javax.microedition.io.Connection {

	public abstract int getMaximumLength() throws java.io.IOException;

	public abstract int getNominalLength() throws java.io.IOException;

	public abstract javax.microedition.io.Datagram newDatagram(byte[] var0, int var1) throws java.io.IOException;

	public abstract javax.microedition.io.Datagram newDatagram(byte[] var0, int var1, java.lang.String var2) throws java.io.IOException;

	public abstract javax.microedition.io.Datagram newDatagram(int var0) throws java.io.IOException;

	public abstract javax.microedition.io.Datagram newDatagram(int var0, java.lang.String var1) throws java.io.IOException;

	public abstract void receive(javax.microedition.io.Datagram var0) throws java.io.IOException;

	public abstract void send(javax.microedition.io.Datagram var0) throws java.io.IOException;

}
