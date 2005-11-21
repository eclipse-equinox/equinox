/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

// This object is used to keep track on a contributor basis of the extension and extension points being contributed.
// It is mainly used on removal so we can quickly  find objects to remove.
// Each contribution is made in the context of a namespace.  
public class Contribution implements KeyedElement {
	static final int[] EMPTY_CHILDREN = new int[] {0, 0};

	//The registry that owns this object
	protected ExtensionRegistry registry;

	// The actual contributor of the contribution.
	protected long contributorId;

	// The namespace containing this contribution.
	protected String namespace = null;

	// Id of the namespace owner (might be same or different from the contributorId).
	protected long namespaceOwnerId = -1;

	// This array stores the identifiers of both the extension points and the extensions.
	// The array has always a minimum size of 2.
	// The first element of the array is the number of extension points and the second the number of extensions. 
	// [numberOfExtensionPoints, numberOfExtensions, extensionPoint#1, extensionPoint#2, extensionPoint..., ext#1, ext#2, ext#3, ... ].
	// The size of the array is 2 + (numberOfExtensionPoints +  numberOfExtensions).
	private int[] children = EMPTY_CHILDREN;
	static final public byte EXTENSION_POINT = 0;
	static final public byte EXTENSION = 1;

	protected Contribution(long contributorId, ExtensionRegistry registry) {
		this.contributorId = contributorId;
		this.registry = registry;

		// resolve namespace owner and namespace
		namespaceOwnerId = registry.getNamespaceOwnerId(contributorId);
		namespace = registry.getNamespace(contributorId);
	}

	void setRawChildren(int[] children) {
		this.children = children;
	}

	protected long getContributorId() {
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

	public String getNamespace() {
		return namespace;
	}

	public String toString() {
		return "Contribution: " + contributorId + " in namespace" + getNamespace(); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected long getNamespaceOwnerId() {
		return namespaceOwnerId;
	}

	//Implements the KeyedElement interface
	public int getKeyHashCode() {
		return getKey().hashCode();
	}

	public Object getKey() {
		return new Long(contributorId);
	}

	public boolean compare(KeyedElement other) {
		return contributorId == ((Contribution) other).contributorId;
	}
}
