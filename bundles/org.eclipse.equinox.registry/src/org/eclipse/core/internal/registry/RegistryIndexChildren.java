/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

public class RegistryIndexChildren {

	static final int[] EMPTY_ARRAY = new int[0];

	private int[] children;

	public RegistryIndexChildren() {
		children = EMPTY_ARRAY;
	}

	public RegistryIndexChildren(int[] children) {
		this.children = children;
	}

	public int[] getChildren() {
		return children;
	}

	public int findChild(int id) {
		for (int i = 0; i < children.length; i++) {
			if (children[i] == id)
				return i;
		}
		return -1;
	}

	public boolean unlinkChild(int id) {
		int index = findChild(id);
		if (index == -1)
			return false; // there is no such element

		// copy the array except one element at index
		int[] result = new int[children.length - 1];
		System.arraycopy(children, 0, result, 0, index);
		System.arraycopy(children, index + 1, result, index, children.length - index - 1);
		children = result;
		return true;
	}

	public boolean linkChild(int id) {
		if (children.length == 0) {
			children = new int[] {id};
			return true;
		}

		// add new element at the end
		int[] result = new int[children.length + 1];
		System.arraycopy(children, 0, result, 0, children.length);
		result[children.length] = id;
		children = result;
		return true;
	}

	public boolean linkChildren(int[] IDs) {
		if (children.length == 0) {
			children = IDs;
			return true;
		}
		int[] result = new int[children.length + IDs.length];
		System.arraycopy(children, 0, result, 0, children.length);
		System.arraycopy(IDs, 0, result, children.length, IDs.length);
		children = result;
		return true;
	}

	public boolean unlinkChildren(int[] IDs) {
		if (children.length == 0)
			return (IDs.length == 0);

		int size = children.length;
		for (int i = 0; i < IDs.length; i++) {
			int index = findChild(IDs[i]);
			if (index != -1) {
				children[i] = -1;
				size--;
			}
		}
		if (size == 0) {
			children = EMPTY_ARRAY;
			return true;
		}
		int[] result = new int[size];
		int pos = 0;
		for (int i = 0; i < children.length; i++) {
			if (children[i] == -1)
				continue;
			result[pos] = children[i];
			pos++;
		}
		return true;
	}
}
