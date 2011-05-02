/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.util.xml;

/**
 * The XMLReader invokes methods of this listener either when the tag is just
 * opened or when the tag is just closed.
 * 
 * @author Ivan Dimitrov
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface ExTagListener {

	/**
	 * Invoked when a tag is opened
	 * 
	 * @param aTag
	 *            tag
	 * @throws IllegalArgumentException
	 */
	public void startTag(Tag aTag) throws IllegalArgumentException;

	/**
	 * Invoked when a tag is closed
	 * 
	 * @param aTag
	 *            tag
	 * @throws IllegalArgumentException
	 */
	public void endTag(Tag aTag) throws IllegalArgumentException;
}
