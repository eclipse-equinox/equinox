/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.bundles;

import java.util.ArrayList;

public class TestResults {
	ArrayList events = new ArrayList();

	synchronized public void addEvent(Object event) {
		events.add(event);
		notifyAll();
	}

	synchronized public Object[] getResults(int expectedResultsNumber) {
		long initialTime = System.currentTimeMillis();
		while (events.size() < expectedResultsNumber) {
			int currentSize = events.size();
			try {
				wait(5000);
			} catch (InterruptedException e) {
				// do nothing
			}
			if (currentSize == events.size() && (System.currentTimeMillis() - initialTime) >= 5000)
				break; // no new events occurred; break out
		}
		Object[] result = events.toArray();
		events.clear();
		return result;
	}
}