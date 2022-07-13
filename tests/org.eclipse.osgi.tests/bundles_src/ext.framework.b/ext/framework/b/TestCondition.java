/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package ext.framework.b;

import java.util.Dictionary;
import java.util.HashMap;
import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;

public class TestCondition implements Condition {

	private final String id;
	private final boolean mutable;
	private boolean curMutable;
	private final boolean postponed;
	private boolean curPostponed;
	private final boolean satisfied;
	private boolean curSatisfied;
	private final Bundle bundle;

	private static final HashMap conditionIDs = new HashMap();

	private TestCondition(String id, boolean mutable, boolean postponed, boolean satisfied, Bundle bundle) {
		this.id = id;
		this.mutable = this.curMutable = mutable;
		this.postponed = this.curPostponed = postponed;
		this.satisfied = this.curSatisfied = satisfied;
		this.bundle = bundle;
	}

	static public Condition getCondition(final Bundle bundle, ConditionInfo info) {
		if (!TestCondition.class.getName().equals(info.getType()))
			throw new IllegalArgumentException("ConditionInfo must be of type \"" + TestCondition.class.getName() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		String[] args = info.getArgs();
		if (args.length != 4)
			throw new IllegalArgumentException("Illegal number of args: " + args.length); //$NON-NLS-1$
		String identity = args[0] + '_' + bundle.getBundleId();
		boolean mut = Boolean.valueOf(args[1]).booleanValue();
		boolean post = Boolean.valueOf(args[2]).booleanValue();
		boolean sat = Boolean.valueOf(args[3]).booleanValue();
		synchronized (conditionIDs) {
			TestCondition condition = (TestCondition) conditionIDs.get(identity);
			if (condition == null) {
				condition = new TestCondition(identity, mut, post, sat, bundle);
				conditionIDs.put(identity, condition);
			}
			return condition;
		}
	}

	static public TestCondition getTestCondition(String id) {
		synchronized (conditionIDs) {
			return (TestCondition) conditionIDs.get(id);
		}
	}

	static public void clearConditions() {
		synchronized (conditionIDs) {
			conditionIDs.clear();
		}
	}

	public boolean isMutable() {
		return curMutable;
	}

	public boolean isPostponed() {
		return curPostponed;
	}

	public boolean isSatisfied() {
		return curSatisfied;
	}

	public boolean isSatisfied(Condition[] conditions, Dictionary context) {
		if (!isPostponed())
			throw new IllegalStateException("Should not call isSatisfied(Condition[] conditions, Dictionary context)"); //$NON-NLS-1$
		for (Condition condition : conditions) {
			Boolean isSatisfied = (Boolean) context.get(condition);
			if (isSatisfied == null) {
				isSatisfied = Boolean.valueOf(condition.isSatisfied());
				context.put(condition, isSatisfied);
			}
			if (!isSatisfied.booleanValue())
				return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TestCondition))
			return false;
		TestCondition otherCondition = (TestCondition) o;
		return id.equals(otherCondition.id) && postponed == otherCondition.postponed && satisfied == otherCondition.satisfied && mutable == otherCondition.mutable && bundle == otherCondition.bundle;
	}

	public void setMutable(boolean mutable) {
		this.curMutable = mutable;
	}

	public void setPostponed(boolean isPostponed) {
		this.curPostponed = isPostponed;
	}

	public void setSatisfied(boolean isSatisfied) {
		this.curSatisfied = isSatisfied;
	}

	public Bundle getBundle() {
		return bundle;
	}

	@Override
	public String toString() {
		return id + '-' + postponed + '-' + mutable + '-' + satisfied;
	}
}
