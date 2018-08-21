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

public abstract interface Datagram extends java.io.DataInput, java.io.DataOutput {

	public abstract java.lang.String getAddress();

	public abstract byte[] getData();

	public abstract int getLength();

	public abstract int getOffset();

	public abstract void reset();

	public abstract void setAddress(javax.microedition.io.Datagram var0);

	public abstract void setAddress(java.lang.String var0) throws java.io.IOException;

	public abstract void setData(byte[] var0, int var1, int var2);

	public abstract void setLength(int var0);

}
