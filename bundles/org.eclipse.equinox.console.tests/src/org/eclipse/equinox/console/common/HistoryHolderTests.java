/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Lazar Kirchev, SAP AG - initial contribution
 ******************************************************************************/

package org.eclipse.equinox.console.common;

import org.junit.Assert;
import org.junit.Test;

import org.eclipse.equinox.console.supportability.HistoryHolder;

public class HistoryHolderTests {

	@Test
    public void test() {
        HistoryHolder historyHolder = new HistoryHolder();
        byte[] line1 = new byte[] { 'a', 'b', 'c', 'd' };
        byte[] line2 = new byte[] { 'x', 'y', 'z' };
        byte[] line3 = new byte[] { 'k', 'l', 'm', 'n' };

        historyHolder.add(line1);
        historyHolder.add(line2);
        historyHolder.add(line3);

        byte[] first = historyHolder.first();
        Assert.assertEquals("Wrong length of first member", line1.length, first.length);
        Assert.assertArrayEquals("Wrong first member", line1, first);

        byte[] last = historyHolder.last();
        Assert.assertEquals("Wrong length of last member", line3.length, last.length);
        Assert.assertArrayEquals("Wrong last member", line3, last);

        byte[] prev = historyHolder.prev();
        Assert.assertEquals("Wrong length of previous member", line2.length, prev.length);
        Assert.assertArrayEquals("Wrong previous member", line2, prev);

        byte[] next = historyHolder.next();
        Assert.assertEquals("Wrong length of next member", line3.length, next.length);
        Assert.assertArrayEquals("Wrong next member", line3, next);

        historyHolder.first();
        historyHolder.add(new byte[] {});
        byte[] current = historyHolder.prev();
        Assert.assertEquals("Wrong length of next member", line3.length, current.length);
        Assert.assertArrayEquals("Wrong next member", line3, current);

        historyHolder.first();
        historyHolder.add(line1);
        current = historyHolder.prev();
        Assert.assertEquals("Wrong length of next member", line1.length, current.length);
        Assert.assertArrayEquals("Wrong next member", line1, current);
        Assert.assertArrayEquals("Second line should now be first", line2, historyHolder.first());

        historyHolder.reset();
        Assert.assertNull("History should be empty", historyHolder.first());
        Assert.assertNull("History should be empty", historyHolder.last());
        Assert.assertNull("History should be empty", historyHolder.next());
        Assert.assertNull("History should be empty", historyHolder.prev());
    }

}
