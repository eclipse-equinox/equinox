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

package org.eclipse.osgi.internal.resolver;

import java.util.*;
import org.eclipse.core.internal.dependencies.*;

public class Eclipse21SelectionPolicy implements ISelectionPolicy {
	public Set selectMultiple(ElementSet elementSet) {
		Set selected = new HashSet();
		for (Iterator requiringIter = elementSet.getRequiring().iterator(); requiringIter.hasNext();) {
			ElementSet requiringNode = (ElementSet) requiringIter.next();
			Collection requiringNodeSelectedVersions = requiringNode.getSelected();
			// loop through the selected versions (one if it is a non-library plug-in) of this requiring plug-in
			for (Iterator requiringVersionsIter = requiringNodeSelectedVersions.iterator(); requiringVersionsIter.hasNext();) {
				Element requiringSelectedVersion = (Element) requiringVersionsIter.next();
				// the selected version may not require this element set (but it still can be selected)
				Dependency requiringSelectedVersionDependency = requiringSelectedVersion.getDependency(elementSet.getId());
				if (requiringSelectedVersionDependency == null)
					continue;
				// find the best version for this pre-requisite
				Element bestVersion = null;
				for (Iterator satisfiedIter = elementSet.getSatisfied().iterator(); satisfiedIter.hasNext();) {
					Element satisfiedVersion = (Element) satisfiedIter.next();
					boolean satisfiesDependency = requiringSelectedVersionDependency.getMatchRule().isSatisfied(requiringSelectedVersionDependency.getUserObject(), satisfiedVersion.getVersionId());
					if (satisfiesDependency) {
						boolean betterThanBest = bestVersion == null || elementSet.getSystem().compare(satisfiedVersion.getVersionId(), bestVersion.getVersionId()) > 0;
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
			Element bestVersion = null;
			for (Iterator satisfiedIter = elementSet.getSatisfied().iterator(); satisfiedIter.hasNext();) {
				Element satisfiedVersion = (Element) satisfiedIter.next();
				boolean betterThanBest = bestVersion == null || elementSet.getSystem().compare(satisfiedVersion.getVersionId(), bestVersion.getVersionId()) > 0;
				if (betterThanBest)
					bestVersion = satisfiedVersion;
			}
			selected = Collections.singleton(bestVersion);
		}
		return selected;
	}

	public Element selectSingle(ElementSet elementSet) {
		// none of its versions are required by other element sets - so just pick the highest
		if (elementSet.getRequiring().isEmpty()) {
			// otherwise, pick the highest version
			Element highest = null;
			for (Iterator satisfiedIter = elementSet.getSatisfied().iterator(); satisfiedIter.hasNext();) {
				Element satisfiedVersion = (Element) satisfiedIter.next();
				if (highest == null || elementSet.getSystem().compare(satisfiedVersion.getVersionId(), highest.getVersionId()) > 0)
					highest = satisfiedVersion;
			}
			return highest;
		}

		// let's pick the highest that satisfies all or the highest required
		Element highest = null;
		int highestStatus = 0; // 0 - not required, 1 - satisfy some, 2 - satisfy all mandatory, 3 - satisfy all
		for (Iterator satisfiedIter = elementSet.getSatisfied().iterator(); satisfiedIter.hasNext();) {
			boolean satisfiesAllMandatory = true;
			boolean satisfiesAll = true;
			boolean isRequired = false;
			Element satisfiedVersion = (Element) satisfiedIter.next();
			for (Iterator requiringIter = elementSet.getRequiring().iterator(); requiringIter.hasNext();) {
				ElementSet requiringNode = (ElementSet) requiringIter.next();
				Collection requiringNodeSelectedVersions = requiringNode.getSelected();
				// loop through the selected versions (one if it is a non-library plug-in) of this requiring plug-in
				for (Iterator requiringVersionsIter = requiringNodeSelectedVersions.iterator(); requiringVersionsIter.hasNext();) {
					Element requiringSelectedVersion = (Element) requiringVersionsIter.next();
					// the selected version may not require this element set (but it still can be selected)
					Dependency requiringSelectedVersionDep = requiringSelectedVersion.getDependency(elementSet.getId());
					if (requiringSelectedVersionDep != null) {
						boolean satisfiesDependency = requiringSelectedVersionDep.getMatchRule().isSatisfied(requiringSelectedVersionDep.getUserObject(), satisfiedVersion.getVersionId());
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