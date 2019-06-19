/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Lazar Kirchev, SAP AG - initial contribution
 ******************************************************************************/

package org.eclipse.equinox.console.common;

import org.junit.Assert;
import org.junit.Test;

public class SimpleByteBufferTests {

	@Test
	public void testBuffer() throws Exception {
		SimpleByteBuffer buffer = new SimpleByteBuffer();
		buffer.add('a');
		buffer.add('b');
		buffer.add('c');
		buffer.add('d');

		Assert.assertTrue("Wrong buffer size; expected 4, actual " + buffer.getSize(), buffer.getSize() == 4);

		check(buffer, new byte[] { 'a', 'b', 'c', 'd' });

		byte[] data = buffer.getCurrentData();
		byte[] expected = new byte[] { 'a', 'b', 'c', 'd' };

		Assert.assertTrue("Data not as expected: expected length " + expected.length + ", actual length " + data.length,
			data.length == expected.length);

		for (int i = 0; i < data.length; i++) {
			Assert.assertEquals("Incorrect data read. Position " + i + ", expected " + expected[i] + ", read " + data[i], expected[i], data[i]);
		}

		buffer.insert('a');
		buffer.insert('b');
		buffer.insert('c');
		buffer.insert('d');

		int pos = buffer.getPos();
		buffer.goLeft();
		int newPos = buffer.getPos();
		Assert.assertEquals("Error while moving left; old pos: " + pos + ", new pos: ", pos - 1, newPos);

		buffer.insert('e');
		check(buffer, new byte[] { 'a', 'b', 'c', 'e', 'd' });

		buffer.goLeft();
		buffer.delete();
		check(buffer, new byte[] { 'a', 'b', 'c', 'd' });

		pos = buffer.getPos();
		buffer.goRight();
		newPos = buffer.getPos();
		Assert.assertEquals("Error while moving right; old pos: " + pos + ", new pos: ", pos + 1, newPos);

		buffer.backSpace();
		check(buffer, new byte[] { 'a', 'b', 'c' });

		buffer.delAll();
		Assert.assertTrue("Bytes in buffer not correctly deleted", (buffer.getSize() == 0) && (buffer.getPos() == 0));

		buffer.set(new byte[] { 'a', 'b', 'c', 'd' });
		check(buffer, new byte[] { 'a', 'b', 'c', 'd' });

		data = buffer.copyCurrentData();
		Assert.assertArrayEquals("Buffer copy does not work properly", new byte[] { 'a', 'b', 'c', 'd' }, data);

		buffer.goLeft();
		buffer.replace('e');
		check(buffer, new byte[] { 'a', 'b', 'c', 'e' });

		buffer.resetPos();
		Assert.assertTrue("Resetting position does not work properly", buffer.getPos() == 0);

		Assert.assertEquals("Wrong current char", 'a', buffer.getCurrentChar());
	}

	private void check(SimpleByteBuffer buffer, byte[] expected) throws Exception {
		byte[] data = buffer.copyCurrentData();

		Assert.assertTrue("Data not as expected: expected length " + expected.length + ", actual length " + data.length,
			data.length == expected.length);

		for (int i = 0; i < data.length; i++) {
			Assert.assertEquals("Incorrect data read. Position " + i + ", expected " + expected[i] + ", read " + data[i], expected[i], data[i]);
		}
	}

}
