/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.dependencies;


public class Element {
	private Object id;
	private Object versionId;
	private Dependency[] dependencies;
	private boolean singleton;
	private Object userObject;

	public Element(Object id, Object versionId, Dependency[] dependencies, boolean singleton, Object userObject) {
		Assert.isNotNull(id);
		Assert.isNotNull(versionId);
		Assert.isNotNull(dependencies);
		this.id = id;
		this.versionId = versionId;
		this.dependencies = dependencies;
		this.singleton = singleton;
		this.userObject = userObject;
	}

	public Object getId() {
		return id;
	}

	public Object getVersionId() {
		return versionId;
	}
	/** @return a non-null reference */
	public Dependency[] getDependencies() {
		return dependencies;
	}
	/** may return null */
	public Dependency getDependency(Object id) {
		for (int i = 0; i < dependencies.length; i++)
			if (dependencies[i].getRequiredObjectId().equals(id))
				return dependencies[i];
		return null;
	}

	public boolean isSingleton() {
		return singleton;
	}

	public Object getUserObject() {
		return userObject;
	}

	public String toString() {
		return this.id + "_" + this.versionId; //$NON-NLS-1$
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Element))
			return false;
		Element other = (Element) obj;
		return (other.userObject != null && other.userObject.equals(this.userObject)) || (this.id.equals(other.id) && this.versionId.equals(other.versionId) && other.userObject == null && this.userObject == null);
	}

	public int hashCode() {
		return (id.hashCode() << 16) | (versionId.hashCode() & 0xFFFF);
	}
}