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


public class Dependency {
	private int changedMark;
	private Object requiredObjectId;
	private Object requiredVersionId;
	private IMatchRule matchRule;
	private boolean optional;
	private Object resolvedVersionId;
	private Object userObject;

	public Dependency(Object requiredObjectId, IMatchRule matchRule, Object requiredVersionId, boolean optional, Object userObject) {
		Assert.isNotNull(requiredObjectId);
		Assert.isNotNull(matchRule);
		this.requiredObjectId = requiredObjectId;
		this.requiredVersionId = requiredVersionId;
		this.matchRule = requiredVersionId == null ? new UnspecifiedVersionMatchRule() : matchRule;
		this.optional = optional;
		this.userObject = userObject;
	}

	/**
	 * @see IDependency#getMatchRule()
	 */
	public IMatchRule getMatchRule() {
		return this.matchRule;
	}

	/**
	 * @see IDependency#getRequiredObjectId()
	 */
	public Object getRequiredObjectId() {
		return this.requiredObjectId;
	}

	/**
	 * @see IDependency#getRequiredVersionId()
	 */
	public Object getRequiredVersionId() {
		return this.requiredVersionId;
	}

	/**
	 * @see IDependency#isOptional()
	 */
	public boolean isOptional() {
		return this.optional;
	}

	/**
	 * @see IDependency#getResolvedVersionId()
	 */
	public Object getResolvedVersionId() {
		return resolvedVersionId;
	}

	public void resolve(Object resolvedVersionId, int changedMark) {
		if ((resolvedVersionId == null && this.resolvedVersionId == null) || (resolvedVersionId != null && resolvedVersionId.equals(this.resolvedVersionId)))
			return;
		this.resolvedVersionId = resolvedVersionId;
		this.changedMark = changedMark;
	}

	public int getChangedMark() {
		return changedMark;
	}

	public String toString() {
		return " -> " + getRequiredObjectId() + "_" + getRequiredVersionId() + " (" + getMatchRule() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	public Object getUserObject() {
		return userObject;
	}

	class UnspecifiedVersionMatchRule implements IMatchRule {
		public boolean isSatisfied(Object required, Object available) {
			return true;
		}
	}
}