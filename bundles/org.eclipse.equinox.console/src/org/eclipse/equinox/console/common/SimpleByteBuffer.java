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

package org.eclipse.equinox.console.common;

/**
 * This is a helper class, which buffers one line of input. It provides
 * for simple line editing - insertion, deletion, left and right movement, deletion through the
 * backspace key.
 */
public class SimpleByteBuffer {

    private static int INITAL_SIZE = 13;

    private byte[] buffer;
    private int pos = 0;
    private int size = 0;

    public SimpleByteBuffer() {
        buffer = new byte[INITAL_SIZE];
    }

    public void add(final int b) {
        if (size >= buffer.length) {
            rezize();
        }
        buffer[size++] = (byte) b;
    }

    private void rezize() {
        final byte[] newbuffeer = new byte[buffer.length << 1];
        System.arraycopy(buffer, 0, newbuffeer, 0, buffer.length);
        buffer = newbuffeer;
    }

    public void insert(int b) {
        if (size >= buffer.length) {
            rezize();
        }
        final int forCopy = size - pos;
        if (forCopy > 0) {
            System.arraycopy(buffer, pos, buffer, pos + 1, forCopy);
        }
        buffer[pos++] = (byte) b;
        size++;
    }

    public int goRight() {
        if (pos < size) {
            return buffer[pos++] & 0xFF;
        }
        return -1;
    }

    public boolean goLeft() {
        if (pos > 0) {
            pos--;
            return true;
        }
        return false;
    }

    public void delete() {
        if (pos < size) {
            final int forCopy = size - pos;
            System.arraycopy(buffer, pos + 1, buffer, pos, forCopy);
            size--;
        }
    }

    public boolean backSpace() {
        if (pos > 0 && size > 0) {
            final int forCopy = size - pos;
            System.arraycopy(buffer, pos, buffer, pos - 1, forCopy);
            size--;
            pos--;
            return true;
        }
        return false;
    }

    public void delAll() {
        pos = 0;
        size = 0;
    }

    public byte[] getCurrentData() {
        byte[] res = new byte[size];
        System.arraycopy(buffer, 0, res, 0, size);
        pos = 0;
        size = 0;
        return res;
    }

    public void set(byte[] newData) {
        pos = 0;
        size = 0;
        if (newData != null) {
            for (byte data : newData) {
                insert(data);
            }
        }
    }

    public int getPos() {
        return pos;
    }

    public byte[] copyCurrentData() {
        byte[] res = new byte[size];
        System.arraycopy(buffer, 0, res, 0, size);
        return res;
    }

    public int getCurrentChar() {
        if (pos < size) {
            return buffer[pos] & 0xFF;
        } else {
            return -1;
        }
    }

    public int getSize() {
        return size;
    }

    public int resetPos() {
        int res = pos;
        pos = 0;
        return res;
    }

    public void replace(int b) {
        if (pos == size) {
            insert(b);
        } else {
            buffer[pos++] = (byte) b;
        }
    }
}
