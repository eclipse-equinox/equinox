/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.link.postponed;

import java.util.Dictionary;
import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;

public class PostPonedCondition implements Condition {

	static public Condition getCondition(final Bundle bundle, final ConditionInfo info) {
		return new PostPonedCondition();
	}

	public boolean isMutable() {
		return true;
	}

	public boolean isPostponed() {
		return true;
	}

	public boolean isSatisfied() {
		return true;
	}

	public boolean isSatisfied(Condition[] conditions, Dictionary context) {
		return true;
	}

}
