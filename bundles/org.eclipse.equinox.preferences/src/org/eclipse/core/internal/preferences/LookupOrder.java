/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.core.internal.preferences;

/**
 * Object used to store the look-up order for preference
 * scope searching.
 *
 * @since 3.0
 */
public class LookupOrder {

	private String[] order;

	LookupOrder(String[] order) {
		for (String o : order) {
			if (o == null) {
				throw new IllegalArgumentException();
			}
		}
		this.order = order;
	}

	public String[] getOrder() {
		return order;
	}
}
