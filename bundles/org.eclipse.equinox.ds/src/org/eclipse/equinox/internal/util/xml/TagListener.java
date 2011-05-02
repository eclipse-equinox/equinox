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
 * This interface is used while parsing xml files. It's method is invoked on
 * every closing tag.
 * 
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface TagListener {

	/**
	 * Invoked when a tag has been closed
	 * 
	 * @param tag
	 *            tag with its content and its subtags
	 * @throws IllegalArgumentException
	 *             may be thrown while proccessing the tag structure
	 */
	public void useTag(TagClass tag) throws IllegalArgumentException;

}
