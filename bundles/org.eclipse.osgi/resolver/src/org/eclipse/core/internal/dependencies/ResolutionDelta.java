/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.internal.dependencies;

import java.util.HashMap;
import java.util.Map;

/**
 * Collects changes that happened during a resolution operation. 
 * Not to be implemented by clients.
 * @see ElementChange
 */
public class ResolutionDelta {

	class ElementIdentifier {
		private Object id;
		private Object userObject;
		private Object versionId;

		ElementIdentifier(Object id, Object versionId, Object userObject) {
			this.id = id;
			this.versionId = versionId;
			this.userObject = userObject;
		}

		public boolean equals(Object anObject) {
			if (!(anObject instanceof ElementIdentifier))
				return false;
			ElementIdentifier change = (ElementIdentifier) anObject;
			return (change.userObject != null && change.userObject.equals(this.userObject)) || (this.id.equals(change.id) && this.versionId.equals(change.versionId) && change.userObject == null && this.userObject == null);
		}

		public int hashCode() {
			return (id.hashCode() << 16) | (versionId.hashCode() & 0xFFFF);
		}
	}

	private Map changes;

	ResolutionDelta() {
		this.changes = new HashMap();
	}

	public ElementChange[] getAllChanges() {
		return (ElementChange[]) changes.values().toArray(new ElementChange[changes.size()]);
	}

	public ElementChange getChange(Object id, Object versionId, Object userObject) {
		return (ElementChange) changes.get(new ElementIdentifier(id, versionId, userObject));
	}

	/**
	 * Record a new status change.
	 */
	void recordChange(Element element, int kind) {
		// check if a change has already been recorded for the element
		ElementChange existingChange = this.getChange(element.getId(), element.getVersionId(), element.getUserObject());
		// if not, just record it and we are done
		if (existingChange == null) {
			this.changes.put(new ElementIdentifier(element.getId(), element.getVersionId(), element.getUserObject()), new ElementChange(element, kind));
			return;
		}
		// a removal cancels any existing addition
		if (kind == ElementChange.REMOVED)
			if (existingChange.getKind() == ElementChange.ADDED) {
				// if it was just an addition, just forget the change
				this.changes.remove(new ElementIdentifier(element.getId(), element.getVersionId(), element.getUserObject()));
				return;
			} else if ((existingChange.getKind() & ElementChange.ADDED) != 0) {
				// if it was an addition among other things, forget the addition bit, and ensure the removal bit is set 
				existingChange.setKind((existingChange.getKind() & ~ElementChange.ADDED) | ElementChange.REMOVED);
				return;
			}
		// otherwise, just update the new status for the existing change object 
		existingChange.setKind(existingChange.getKind() | kind);
	}

	public String toString() {
		return changes.values().toString();
	}
}