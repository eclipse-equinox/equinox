/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.completion.common;

import java.util.Map;

/**
 * This is the interface for providing tab completion.
 */
public interface Completer {
	/**
	 * Returns the possible candidates for completion for the passed string.
	 * 
	 * @param buffer text to be completed
	 * @param cursor current position in the text
	 * @return map of candidate completions, and on which position in the buffer starts the completion
	 */
	public Map<String, Integer> getCandidates(String buffer, int cursor);
}
