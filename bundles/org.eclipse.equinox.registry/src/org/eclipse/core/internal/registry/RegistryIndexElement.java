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

public class RegistryIndexElement implements KeyedElement {

	// The key on which indexing is done
	final protected String key;

	// Extension points matching the key
	private RegistryIndexChildren extensionPoints;

	// Extensions matching the key
	private RegistryIndexChildren extensions;

	public RegistryIndexElement(String key) {
		this.key = key;
	}
	
	public RegistryIndexElement(String key, int[] extensionPoints, int[] extensions) {
		this.key = key;
		this.extensionPoints = new RegistryIndexChildren(extensionPoints);
		this.extensions = new RegistryIndexChildren(extensions);
	}

	protected int[] getExtensions() {
		if (extensions == null)
			return RegistryIndexChildren.EMPTY_ARRAY;
		return extensions.getChildren();
	}

	protected int[] getExtensionPoints() {
		if (extensionPoints == null)
			return RegistryIndexChildren.EMPTY_ARRAY;
		return extensionPoints.getChildren();
	}

	public boolean updateExtension(int id, boolean add) {
		if (extensions == null)
			extensions = new RegistryIndexChildren();

		if (add)
			return extensions.linkChild(id);
		else
			return extensions.unlinkChild(id);
	}

	public boolean updateExtensions(int[] IDs, boolean add) {
		if (extensions == null)
			extensions = new RegistryIndexChildren();

		if (add)
			return extensions.linkChildren(IDs);
		else
			return extensions.unlinkChildren(IDs);
	}

	public boolean updateExtensionPoint(int id, boolean add) {
		if (extensionPoints == null)
			extensionPoints = new RegistryIndexChildren();

		if (add)
			return extensionPoints.linkChild(id);
		else
			return extensionPoints.unlinkChild(id);
	}

	public boolean updateExtensionPoints(int[] IDs, boolean add) {
		if (extensionPoints == null)
			extensionPoints = new RegistryIndexChildren();

		if (add)
			return extensionPoints.linkChildren(IDs);
		else
			return extensionPoints.unlinkChildren(IDs);
	}

	//Implements the KeyedElement interface
	public int getKeyHashCode() {
		return getKey().hashCode();
	}

	public Object getKey() {
		return key;
	}

	public boolean compare(KeyedElement other) {
		return key.equals(((RegistryIndexElement) other).key);
	}
}
