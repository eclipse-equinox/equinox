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
