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

package org.eclipse.equinox.internal.util.event;

/**
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class Queue {

	protected Object[] queue;
	protected int first, last = -1;
	protected int initial, count;
	protected int increment;
	protected int decrement;

	public Queue(int size) {
		queue = new Object[initial = size];
		increment = initial / 2;
		decrement = increment + (increment / 2);
	}

	public void put(Object element) {
		if (count == queue.length)
			resize(true);
		queue[++last == queue.length ? last = 0 : last] = element;
		count++;
	}

	public void unget(Object element) {
		if (count == queue.length)
			resize(true);
		queue[--first == -1 ? first = queue.length - 1 : first] = element;
		count++;
	}

	public Object get() {
		if (count == 0)
			return null;
		if (queue.length > initial && queue.length - count > decrement)
			resize(false);
		Object element = queue[first];
		queue[first++] = null;
		if (first == queue.length)
			first = 0;
		count--;
		return element;
	}

	public void clear() {
		if (queue.length > initial) {
			queue = new Object[initial];
			count = 0;
		} else
			for (; count > 0; count--) {
				queue[first++] = null;
				if (first == queue.length)
					first = 0;
			}
		first = 0;
		last = -1;
	}

	public int size() {
		return count;
	}

	protected void resize(boolean up) {
		Object[] tmp = new Object[queue.length + (up ? increment : -increment)];
		if (first <= last)
			System.arraycopy(queue, first, tmp, 0, count);
		else {
			int count1 = queue.length - first;
			if (count1 > 0)
				System.arraycopy(queue, first, tmp, 0, count1);
			if (count > count1)
				System.arraycopy(queue, 0, tmp, count1, count - count1);
		}
		queue = tmp;
		first = 0;
		last = count - 1;

		increment = queue.length / 2;
		decrement = increment + (increment / 2);
	}
}
