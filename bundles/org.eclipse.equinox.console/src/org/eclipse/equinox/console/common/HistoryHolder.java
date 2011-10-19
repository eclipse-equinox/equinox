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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * A helper class, which implements history.
 */
public class HistoryHolder {

    private static final int MAX = 100;
    private final byte[][] history;
    private int size;
    private int pos;

    public HistoryHolder() {
        history = new byte[MAX][];
    }

    public synchronized void reset() {
        size = 0;
        pos = 0;
        for (int i = 0; i < MAX; i++) {
            history[i] = null;
        }
    }

    public synchronized void add(byte[] data) {
        try {
            data = new String(data, "US-ASCII").trim().getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {

        }
        if (data.length == 0) {
            pos = size;
            return;
        }
        for (int i = 0; i < size; i++) {
            if (Arrays.equals(history[i], data)) {
                System.arraycopy(history, i + 1, history, i, size - i - 1);
                history[size - 1] = data;
                pos = size;
                return;
            }
        }
        if (size >= MAX) {
            System.arraycopy(history, 1, history, 0, size - 1);
            size--;
        }
        history[size++] = data;
        pos = size;
    }

    public synchronized byte[] next() {
        if (pos >= size - 1) {
            return null;
        }
        return history[++pos];
    }

    public synchronized byte[] last() {
        if (size > 0) {
            pos = size - 1;
            return history[pos];
        } else {
            return null;
        }
    }

    public synchronized byte[] first() {
        if (size > 0) {
            pos = 0;
            return history[pos];
        } else {
            return null;
        }
    }

    public synchronized byte[] prev() {
        if (size == 0) {
            return null;
        }
        if (pos == 0) {
            return history[pos];
        } else {
            return history[--pos];
        }
    }
}
