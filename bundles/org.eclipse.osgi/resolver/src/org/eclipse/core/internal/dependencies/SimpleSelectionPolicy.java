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

import java.util.*;

/**
 * Simple selection policy.
 */

public class SimpleSelectionPolicy implements ISelectionPolicy {
	public Set selectMultiple(ElementSet elementSet) {
		// all satisfied are selected
		return new HashSet(elementSet.getSatisfied());
	}

	public Element selectSingle(ElementSet elementSet) {
		// just pick the satisfied element with the highest version
		Element highest = null;
		for (Iterator satisfiedIter = elementSet.getSatisfied().iterator(); satisfiedIter.hasNext();) {
			Element satisfiedVersion = (Element) satisfiedIter.next();
			if (highest == null || elementSet.getSystem().compare(satisfiedVersion.getVersionId(), highest.getVersionId()) > 0)
				highest = satisfiedVersion;
		}
		return highest;
	}
}