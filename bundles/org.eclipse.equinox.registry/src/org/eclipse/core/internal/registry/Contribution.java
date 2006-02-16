/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.InvalidRegistryObjectException;

// This object is used to keep track on a contributor basis of the extension and extension points being contributed.
// It is mainly used on removal so we can quickly  find objects to remove.
// Each contribution is made in the context of a namespace.  
public class Contribution implements KeyedElement {
	static final int[] EMPTY_CHILDREN = new int[] {0, 0};

	//The registry that owns this object
	protected ExtensionRegistry registry;

	// The actual contributor of the contribution
	final protected String contributorId;

	// Value is derived from the contributorId and cached for performance
	private String defaultNamespace = null;

	// indicates if this contribution needs to be saved in the registry cache
	protected boolean persist;

	// This array stores the identifiers of both the extension points and the extensions.
	// The array has always a minimum size of 2.
	// The first element of the array is the number of extension points and the second the number of extensions. 
	// [numberOfExtensionPoints, numberOfExtensions, extensionPoint#1, extensionPoint#2, extensionPoint..., ext#1, ext#2, ext#3, ... ].
	// The size of the array is 2 + (numberOfExtensionPoints +  numberOfExtensions).
	private int[] children = EMPTY_CHILDREN;
	static final public byte EXTENSION_POINT = 0;
	static final public byte EXTENSION = 1;

	protected Contribution(String contributorId, ExtensionRegistry registry, boolean persist) {
		this.contributorId = contributorId;
		this.registry = registry;
		this.persist = persist;
	}

	void mergeContribution(Contribution addContribution) {
		Assert.isTrue(contributorId.equals(addContribution.contributorId));
		Assert.isTrue(registry == addContribution.registry);

		// persist?
		// Old New Result
		//  F   F   F
		//  F   T   T	=> needs to be adjusted
		//  T   F   T 
		//  T   T   T
		if (shouldPersist() != addContribution.shouldPersist())
			persist = true;

		int[] existing = getRawChildren();
		int[] addition = addContribution.getRawChildren();

		int extensionPoints = existing[EXTENSION_POINT] + addition[EXTENSION_POINT];
		int extensions = existing[EXTENSION] + addition[EXTENSION];
		int[] allChildren = new int[2 + extensionPoints + extensions];

		allChildren[EXTENSION_POINT] = extensionPoints;
		System.arraycopy(existing, 2, allChildren, 2, existing[EXTENSION_POINT]);
		System.arraycopy(addition, 2, allChildren, 2 + existing[EXTENSION_POINT], addition[EXTENSION_POINT]);
		allChildren[EXTENSION] = extensions;
		System.arraycopy(existing, 2 + existing[EXTENSION_POINT], allChildren, 2 + extensionPoints, existing[EXTENSION]);
		System.arraycopy(addition, 2 + addition[EXTENSION_POINT], allChildren, 2 + extensionPoints + existing[EXTENSION], addition[EXTENSION]);

		children = allChildren;
	}

	void setRawChildren(int[] children) {
		this.children = children;
	}

	protected String getContributorId() {
		return contributorId;
	}

	protected int[] getRawChildren() {
		return children;
	}

	protected int[] getExtensions() {
		int[] results = new int[children[EXTENSION]];
		System.arraycopy(children, 2 + children[EXTENSION_POINT], results, 0, children[EXTENSION]);
		return results;
	}

	protected int[] getExtensionPoints() {
		int[] results = new int[children[EXTENSION_POINT]];
		System.arraycopy(children, 2, results, 0, children[EXTENSION_POINT]);
		return results;
	}

	public String getDefaultNamespace() {
		if (defaultNamespace == null)
			defaultNamespace = registry.getObjectManager().getContributor(contributorId).getName();
		return defaultNamespace;
	}

	public String toString() {
		return "Contribution: " + contributorId + " in namespace" + getDefaultNamespace(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	//Implements the KeyedElement interface
	public int getKeyHashCode() {
		return getKey().hashCode();
	}

	public Object getKey() {
		return contributorId;
	}

	public boolean compare(KeyedElement other) {
		return contributorId.equals(((Contribution) other).contributorId);
	}

	public boolean shouldPersist() {
		return persist;
	}

	public void unlinkChild(int id) {
		// find index of the child being unlinked:
		int index = -1;
		for (int i = 2; i < children.length; i++) {
			if (children[i] == id) {
				index = i;
				break;
			}
		}
		if (index == -1)
			throw new InvalidRegistryObjectException();

		// copy all array except one element at index
		int[] result = new int[children.length - 1];
		System.arraycopy(children, 0, result, 0, index);
		System.arraycopy(children, index + 1, result, index, children.length - index - 1);

		// fix sizes
		if (index < children[EXTENSION_POINT] + 2)
			result[EXTENSION_POINT]--;
		else
			result[EXTENSION]--;

		children = result;
	}

	/**
	 * Contribution is empty if it has no children.
	 */
	public boolean isEmpty() {
		return (children[EXTENSION_POINT] == 0 || children[EXTENSION] == 0);
	}

	/**
	 * Find if this contribution has a children with ID = id.
	 * @param id possible ID of the child
	 * @return true: contribution has this child
	 */
	public boolean hasChild(int id) {
		for (int i = 2; i < children.length; i++) {
			if (children[i] == id)
				return true;
		}
		return false;
	}
}
