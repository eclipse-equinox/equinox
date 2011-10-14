/*******************************************************************************
 * Copyright (c) 2010 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation  
 *******************************************************************************/

package org.eclipse.equinox.console.common;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This class wraps the actual output stream (e.g., a socket output stream) and is responsible for
 * buffering and flushing the characters to the actual output stream.
 */
public class ConsoleOutputStream extends OutputStream {

    /**
     * A size of the used buffer.
     */
    public static final int BUFFER_SIZE = 2048;
    public final static byte CR = (byte) '\r';
    public final static byte LF = (byte) '\n';

    OutputStream out;
    OutputStream oldOut;

    private boolean isEcho = true;
    private boolean queueing = false;
    private byte prevByte;
    private byte[] buffer;
    private int pos;

    /**
     * Initiates with instance of the output stream to which it will send data. Here it writes to
     * a socket output stream.
     *
     * @param out OutputStream for console output
     */
    public ConsoleOutputStream(OutputStream out) {
        this.out = out;
        buffer = new byte[BUFFER_SIZE];
        pos = 0;
    }

    /**
     * An implementation of the corresponding abstract method in OutputStream.
     */
    public synchronized void write(int i) throws IOException {

        if (!queueing) {
            if (isEcho) {
                if (i == '\r' || i == '\0') {
                    queueing = true;
                    prevByte = (byte) i;
                } else if (i == '\n') {
                    add(CR);
                    add(LF);
                } else {
                    add(i);
                }
            }
        } else { // awaiting '\n' AFTER '\r', and '\b' AFTER '\0'
            if (prevByte == '\0' && i == '\b') {
                isEcho = !isEcho;
            } else if (isEcho) {
                if (prevByte == '\r' && i == '\n') {
                    add(CR);
                    add(LF);
                } else {
                    add(CR);
                    add(LF);
                    add(i);
                }
            }

            queueing = false;
            flush();
        }

    }

    /**
     * Empties the buffer and sends data to the socket output stream.
     *
     * @throws IOException
     */
    public synchronized void flush() throws IOException {
        if (pos > 0) {
            try {
				out.write(buffer, 0, pos);
				out.flush();
			} finally {
				pos = 0;
			}
            
        }
    }

    /**
     * Adds a variable of type integer to the buffer.
     *
     * @param i integer to add
     * @throws java.io.IOException if there are problems adding the integer
     */
    private void add(int i) throws IOException {
        buffer[pos] = (byte) i;
        pos++;

        if (pos == buffer.length) {
            flush();
        }
    }

    /**
     * Closes this OutputStream.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        out.close();
    }

    /**
     * Substitutes the output stream. The old one is stored so that it can be restored later.
     *
     * @param newOut new output stream to use.
     */
    public void setOutput(OutputStream newOut) {
        if (newOut != null) {
            oldOut = out;
            out = newOut;
        } else {
            out = oldOut;
        }

    }
}
