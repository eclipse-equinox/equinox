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

import java.util.*;

import org.eclipse.core.dependencies.*;

public class ElementSet implements IElementSet {
	private DependencySystem system;
	private Object id;
	// # of iteration this element set was last visited 
	transient private int visitedMark;
	//	# of iteration this element set was last changed
	transient private int changedMark;
	//	does this element set need to be recomputed? (the value is one of {UP_TO_DATE,SATISFACTION,SELECTION,RESOLUTION}) 	
	transient private int needingUpdate;
	private int singletonsCount;
	private Collection requiring;
	private Collection required;
	private Map available;
	private Set satisfied;
	private Set selected;
	private Set resolved;
	private Map dependencyCounters;

	public ElementSet(Object id, DependencySystem system) {
		this.id = id;
		this.system = system;
		this.setNeedingUpdate(DependencySystem.SATISFACTION);
		this.available = new HashMap();
		this.satisfied = Collections.EMPTY_SET;
		this.selected = Collections.EMPTY_SET;
		this.resolved = Collections.EMPTY_SET;
		this.required = new LinkedList();
		this.requiring = new LinkedList();
		this.dependencyCounters = new HashMap();
	}

	public IDependencySystem getSystem() {
		return system;
	}

	/** @return false if there is at least one version that does not allow concurrency. */
	public boolean allowsConcurrency() {
		return singletonsCount == 0;
	}

	void addElement(IElement element) {
		if (this.available.containsKey(element.getVersionId()))
			return;
		this.setNeedingUpdate(DependencySystem.SATISFACTION);
		this.available.put(element.getVersionId(), element);
		IDependency[] dependencies = element.getDependencies();
		for (int i = 0; i < dependencies.length; i++)
			this.addRequired(dependencies[i].getRequiredObjectId());
		if (element.isSingleton())
			this.singletonsCount++;
		system.recordElementStatusChanged(element, IElementChange.ADDED);
	}

	void removeElement(IElement element) {
		removeElement(element.getVersionId());
	}

	void removeElement(Object versionId) {
		IElement toRemove = (IElement) this.available.remove(versionId);
		if (toRemove == null)
			return;
		this.markNeedingUpdate(DependencySystem.SATISFACTION);
		IDependency[] dependencies = toRemove.getDependencies();
		for (int i = 0; i < dependencies.length; i++)
			removeRequired(dependencies[i].getRequiredObjectId());
		// if does not allow concurrency, decrement preventingConcurrencyCount			
		if (toRemove.isSingleton())
			this.singletonsCount--;
		int change = IElementChange.REMOVED;
		//		if (resolved.contains(toRemove))
		//			change |= IElementChange.UNRESOLVED;
		system.recordElementStatusChanged(toRemove, change);
	}

	public Object getId() {
		return id;
	}

	public boolean isRoot() {
		return required.isEmpty();
	}

	public Set getAvailable() {
		return new HashSet(available.values());
	}

	public Collection getRequired() {
		return required;
	}

	public Collection getRequiring() {
		return requiring;
	}

	public Set getResolved() {
		return resolved;
	}

	public void resolveDependency(IElement dependent, IDependency dependency, Object resolvedVersionId) {
		dependency.resolve(resolvedVersionId, this.visitedMark);
	}

	public void setResolved(Set newResolved) {
		this.setNeedingUpdate(DependencySystem.UP_TO_DATE);

		//TODO: this may well be optimized... 
		// maybe just a pre-requisite changed
		for (Iterator resolvedIter = this.resolved.iterator(); resolvedIter.hasNext();) {
			IElement resolvedElement = (IElement) resolvedIter.next();
			IDependency[] dependencies = resolvedElement.getDependencies();
			for (int i = 0; i < dependencies.length; i++)
				if (dependencies[i].getChangedMark() == this.getVisitedMark()) {
					system.recordElementStatusChanged(resolvedElement, IElementChange.LINKAGE_CHANGED);
					break;
				}
		}
		if (newResolved.equals(this.resolved))
			return;
		this.setChangedMark(visitedMark);
		Collection oldResolved = this.resolved;
		this.resolved = Collections.unmodifiableSet(newResolved);
		system.recordDependencyChanged(oldResolved, newResolved, available);
	}

	public Set getSelected() {
		return selected;
	}

	public void setSelected(Set selected) {
		this.setNeedingUpdate(DependencySystem.RESOLUTION);
		if (selected.equals(this.selected))
			return;
		this.setChangedMark(visitedMark);
		this.selected = Collections.unmodifiableSet(selected);
	}

	public Set getSatisfied() {
		return satisfied;
	}

	public void setSatisfied(Set satisfied) {
		this.setNeedingUpdate(DependencySystem.SELECTION);
		if (satisfied.equals(this.satisfied))
			return;
		this.setChangedMark(visitedMark);
		this.satisfied = Collections.unmodifiableSet(satisfied);
	}

	public String toString() {
		return this.id + ": " + available; //$NON-NLS-1$
	}

	public boolean equals(Object elementSet) {
		return ((IElementSet) elementSet).getId().equals(this.id);
	}

	public int hashCode() {
		return this.id.hashCode();
	}

	class DependencyCounter {
		int value;
	}

	private void addRequired(Object requiredId) {
		this.setNeedingUpdate(DependencySystem.SATISFACTION);
		ElementSet requiredNode = (ElementSet) system.getElementSet(requiredId);
		DependencyCounter counter = (DependencyCounter) this.dependencyCounters.get(requiredId);
		if (counter == null) {
			this.dependencyCounters.put(requiredId, counter = new DependencyCounter());
			// links requiring and required element sets in both directions
			this.required.add(requiredNode);
			requiredNode.requiring.add(this);
			requiredNode.setNeedingUpdate(Math.min(requiredNode.getNeedingUpdate(), DependencySystem.SELECTION));
		}
		counter.value++;
	}

	private void removeRequired(Object requiredId) {
		ElementSet requiredNode = (ElementSet) system.getElementSet(requiredId);
		DependencyCounter counter = (DependencyCounter) this.dependencyCounters.get(requiredId);
		if (counter == null) {
			if (system.inDebugMode())
				System.err.println("Trying to remove non-existent dependency: " + this.id + " -> " + requiredId); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		counter.value--;
		if (counter.value == 0) {
			this.dependencyCounters.remove(requiredId);
			// removes links between requiring and required element sets
			this.required.remove(requiredNode);
			requiredNode.requiring.remove(this);
			requiredNode.setNeedingUpdate(DependencySystem.SELECTION);
		}
	}

	int getRequiringCount() {
		return requiring.size();
	}

	int getElementCount() {
		return available.size();
	}

	int getVisitedMark() {
		return visitedMark;
	}

	void setVisitedMark(int mark) {
		this.visitedMark = mark;
	}

	int getChangedMark() {
		return changedMark;
	}

	private void setChangedMark(int mark) {
		this.changedMark = mark;
	}

	void markNeedingUpdate(int order) {
		setNeedingUpdate(order);
	}

	boolean isNeedingUpdate(int order) {
		return getNeedingUpdate() <= order;
	}

	IElement getElement(Object versionId) {
		return (IElement) this.available.get(versionId);
	}

	public Collection getRequiringElements(Object versionId) {
		Collection result = new LinkedList();
		for (Iterator requiringSetsIter = requiring.iterator(); requiringSetsIter.hasNext();) {
			ElementSet requiringSet = (ElementSet) requiringSetsIter.next();
			for (Iterator iter = requiringSet.getResolved().iterator(); iter.hasNext();) {
				Element element = (Element) iter.next();
				IDependency requisite = element.getDependency(this.id);
				if (requisite != null && versionId.equals(requisite.getResolvedVersionId()))
					result.add(element);
			}
		}
		return result;
	}

	public void unresolve(IElement element, int mark) {
		setVisitedMark(mark);
		if (!resolved.contains(element))
			return;
		Set newResolved = new HashSet(resolved);
		newResolved.remove(element);
		resolved = Collections.unmodifiableSet(newResolved);

		IDependency[] dependencies = element.getDependencies();
		// unresolved dependencies
		for (int i = 0; i < dependencies.length; i++)
			resolveDependency(element, dependencies[i], null);
		setChangedMark(mark);
		setNeedingUpdate(DependencySystem.SATISFACTION);
	}

	private void setNeedingUpdate(int needingUpdate) {
		this.needingUpdate = needingUpdate;
	}

	private int getNeedingUpdate() {
		return needingUpdate;
	}
}