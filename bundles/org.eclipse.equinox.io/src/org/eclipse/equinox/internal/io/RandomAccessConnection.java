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
package org.eclipse.equinox.internal.io;

import java.io.*;
import javax.microedition.io.InputConnection;
import javax.microedition.io.OutputConnection;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface RandomAccessConnection extends InputConnection, OutputConnection, DataInput, DataOutput {

	public long length() throws IOException;

	public long getFilePointer() throws IOException;

	public boolean isSelected();

	public int read() throws IOException;

	public int read(byte[] buff, int off, int len) throws IOException;

	public void seek(long len) throws IOException;

	public void write(int b) throws IOException;

	public void write(byte[] buff, int off, int len) throws IOException;

	public void flush() throws IOException;
}
