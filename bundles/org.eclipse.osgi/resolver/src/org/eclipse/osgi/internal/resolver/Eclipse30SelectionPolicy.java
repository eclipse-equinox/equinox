/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.resolver;

import java.util.*;

import org.eclipse.core.dependencies.*;

/**
 * This is a variant of Eclipse 2.1 resolution policy that takes into account the current state
 * of the element in a sense that resolved elements tend to stay resolved (the "least perturbation mode").
 */
public class Eclipse30SelectionPolicy implements ISelectionPolicy {
	public Set selectMultiple(IElementSet elementSet) {
		Set selected = new HashSet();
		// pre-select all elements that were previously resolved and are satisfied		
		Collection previouslyResolvedSet = elementSet.getResolved();
		for (Iterator iter = previouslyResolvedSet.iterator(); iter.hasNext(); ) {
			IElement previouslyResolved = (IElement) iter.next();
			// if the previously resolved element is satisfied, select it
			if (elementSet.getSatisfied().contains(previouslyResolved))
				selected.add(previouslyResolved);
		}
		// may want to select others		
		for (Iterator requiringIter = elementSet.getRequiring().iterator(); requiringIter.hasNext();) {
			IElementSet requiringNode = (IElementSet) requiringIter.next();
			Collection requiringNodeSelectedVersions = requiringNode.getSelected();
			// loop through the selected versions (one if it is a non-library plug-in) of this requiring plug-in
			for (Iterator requiringVersionsIter = requiringNodeSelectedVersions.iterator(); requiringVersionsIter.hasNext();) {
				IElement requiringSelectedVersion = (IElement) requiringVersionsIter.next();
				// the selected version may not require this element set (but it still can be selected)
				IDependency requiringSelectedVersionDependency = requiringSelectedVersion.getDependency(elementSet.getId());
				if (requiringSelectedVersionDependency == null)
					continue;
				// find the best version for this pre-requisite
				IElement bestVersion = null;
				for (Iterator satisfiedIter = elementSet.getSatisfied().iterator(); satisfiedIter.hasNext();) {
					IElement satisfiedVersion = (IElement) satisfiedIter.next();
					boolean satisfiesDependency = requiringSelectedVersionDependency.getMatchRule().isSatisfied(requiringSelectedVersionDependency.getRequiredVersionId(), satisfiedVersion.getVersionId());
					if (satisfiesDependency) {
						boolean betterThanBest = bestVersion == null || elementSet.getSystem().compare(satisfiedVersion.getVersionId(),bestVersion.getVersionId()) > 0;
						if (betterThanBest)
							bestVersion = satisfiedVersion;
					}
				}
				if (bestVersion != null)
					selected.add(bestVersion);
			}
		}
		// if none of the versions are required (or satisfy any selected re-requisites), pick the highest
		if (selected.isEmpty()) {
			IElement bestVersion = null;
			for (Iterator satisfiedIter = elementSet.getSatisfied().iterator(); satisfiedIter.hasNext();) {
				IElement satisfiedVersion = (IElement) satisfiedIter.next();
				boolean betterThanBest = bestVersion == null || elementSet.getSystem().compare(satisfiedVersion.getVersionId(),bestVersion.getVersionId()) > 0;
				if (betterThanBest)
					bestVersion = satisfiedVersion;
			}
			selected = Collections.singleton(bestVersion);
		}		
		return selected;
	}
	public IElement selectSingle(IElementSet elementSet) {
		Collection previouslyResolvedSet = elementSet.getResolved();
		// we already have one element resolved
		if (!previouslyResolvedSet.isEmpty()) {
			// TODO: need a cheaper way to obtain the single element
			IElement previouslyResolved = (IElement) previouslyResolvedSet.iterator().next();
			// if it is satisfied, just return it
			if (elementSet.getSatisfied().contains(previouslyResolved))
				return previouslyResolved;
		}
		
		// none of its versions are required by other element sets - so just pick the highest
		if (elementSet.getRequiring().isEmpty()) {
			// otherwise, pick the highest version
			IElement highest = null;
			for (Iterator satisfiedIter = elementSet.getSatisfied().iterator(); satisfiedIter.hasNext();) {
				IElement satisfiedVersion = (IElement) satisfiedIter.next();
				if (highest == null || elementSet.getSystem().compare(satisfiedVersion.getVersionId(), highest.getVersionId()) > 0)
					highest = satisfiedVersion;
			}
			return highest;
		}

		// let's pick the highest that satisfies all or the highest required
		IElement highest = null;
		int highestStatus = 0; // 0 - not required, 1 - satisfy some, 2 - satisfy all mandatory, 3 - satisfy all
		for (Iterator satisfiedIter = elementSet.getSatisfied().iterator(); satisfiedIter.hasNext();) {
			boolean satisfiesAllMandatory = true;
			boolean satisfiesAll = true;
			boolean isRequired = false;
			IElement satisfiedVersion = (IElement) satisfiedIter.next();
			for (Iterator requiringIter = elementSet.getRequiring().iterator(); requiringIter.hasNext();) {
				IElementSet requiringNode = (IElementSet) requiringIter.next();
				Collection requiringNodeSelectedVersions = requiringNode.getSelected();
				// loop through the selected versions (one if it is a non-library plug-in) of this requiring plug-in
				for (Iterator requiringVersionsIter = requiringNodeSelectedVersions.iterator(); requiringVersionsIter.hasNext();) {
					IElement requiringSelectedVersion = (IElement) requiringVersionsIter.next();
					// the selected version may not require this element set (but it still can be selected)
					IDependency requiringSelectedVersionDep = requiringSelectedVersion.getDependency(elementSet.getId());
					if (requiringSelectedVersionDep != null) {
						boolean satisfiesDependency = requiringSelectedVersionDep.getMatchRule().isSatisfied(requiringSelectedVersionDep.getRequiredVersionId(), satisfiedVersion.getVersionId());
						isRequired |= satisfiesDependency;
						satisfiesAll &= satisfiesDependency;
						satisfiesAllMandatory &= (satisfiesDependency | requiringSelectedVersionDep.isOptional());
					}
				}
			}
			int status = satisfiesAll ? 3 : (satisfiesAllMandatory ? 2 : (isRequired ? 1 : 0));
			boolean higherThanHighest = highest == null || elementSet.getSystem().compare(satisfiedVersion.getVersionId(), highest.getVersionId()) > 0;
			if (status > highestStatus || (status == highestStatus && higherThanHighest)) {
				highest = satisfiedVersion;
				highestStatus = status;
			}
		}
		return highest;		
	}		
	

}
