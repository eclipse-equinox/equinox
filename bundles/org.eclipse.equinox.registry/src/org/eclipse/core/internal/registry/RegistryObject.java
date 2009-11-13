/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

/**
 * An object which has the general characteristics of all the nestable elements
 * in a plug-in manifest.
 */
public abstract class RegistryObject implements KeyedElement {
	//Object identifier
	private int objectId = RegistryObjectManager.UNKNOWN;
	//The children of the element
	protected int[] children = RegistryObjectManager.EMPTY_INT_ARRAY;

	// The field combines offset, persistence flag, and no offset flag
	private int extraDataOffset = EMPTY_MASK;

	// it is assumed that int has 32 bits (bits #0 to #31);
	// bits #0 - #29 are the offset (limited to about 1Gb)
	// bit #30 - persistance flag
	// bit #31 - registry object has no extra data offset
	// the bit#31 is a sign bit; bit#30 is the highest mantissa bit
	static final int EMPTY_MASK = 0x80000000; // only taking bit #31
	static final int PERSIST_MASK = 0x40000000; // only taking bit #30
	static final int OFFSET_MASK = 0x3FFFFFFF; // all bits but #30, #31

	//The registry that owns this object
	protected ExtensionRegistry registry;

	protected RegistryObject(ExtensionRegistry registry, boolean persist) {
		this.registry = registry;
		setPersist(persist);
	}

	void setRawChildren(int[] values) {
		children = values;
	}

	//This can not return null. It returns the singleton empty array or an array 
	protected int[] getRawChildren() {
		return children;
	}

	void setObjectId(int value) {
		objectId = value;
	}

	protected int getObjectId() {
		return objectId;
	}

	//Implementation of the KeyedElement interface
	public int getKeyHashCode() {
		return objectId;
	}

	public Object getKey() {
		return new Integer(objectId);
	}

	public boolean compare(KeyedElement other) {
		return objectId == ((RegistryObject) other).objectId;
	}

	protected boolean shouldPersist() {
		return (extraDataOffset & PERSIST_MASK) == PERSIST_MASK;
	}

	private void setPersist(boolean persist) {
		if (persist)
			extraDataOffset |= PERSIST_MASK;
		else
			extraDataOffset &= ~PERSIST_MASK;
	}

	protected boolean noExtraData() {
		return (extraDataOffset & EMPTY_MASK) == EMPTY_MASK;
	}

	// Convert no extra data to -1 on output
	protected int getExtraDataOffset() {
		if (noExtraData())
			return -1;
		return extraDataOffset & OFFSET_MASK;
	}

	// Accept -1 as "no extra data" on input
	protected void setExtraDataOffset(int offset) {
		if (offset == -1) {
			extraDataOffset &= ~OFFSET_MASK; // clear all offset bits
			extraDataOffset |= EMPTY_MASK;
			return;
		}

		if ((offset & OFFSET_MASK) != offset)
			throw new IllegalArgumentException("Registry object: extra data offset is out of range"); //$NON-NLS-1$

		extraDataOffset &= ~(OFFSET_MASK | EMPTY_MASK); // clear all offset bits; mark as non-empty
		extraDataOffset |= (offset & OFFSET_MASK); // set all offset bits
	}

	protected String getLocale() {
		return registry.getLocale();
	}
}
