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

import java.util.Enumeration;

/**
 * @author Ivan Dimitrov
 * @author Pavlin Dobrev
 * @version 1.0
 */

public interface Tag {
	/**
	 * Returns an attribute value for an attribute with a given name (aAttrName)
	 * 
	 * @param aAttrName
	 *            the name of attribute
	 * @return Attribute value for attribute with name given by attrName or null
	 *         if there is no such an attribute
	 */
	public String getAttribute(String aAttrName);

	/**
	 * Returns an Enumeration of all the attributes' names for attributes belong
	 * to the tag object
	 * 
	 * @return Enumeration that contains the attribute names of the attributes
	 *         for this tag. If there is no attributes then the method returns
	 *         Empty enumeration
	 */
	public Enumeration getAttributeNames();

	/**
	 * Returns an Enumeration that contains the attribute values for the
	 * attributes that belong to the tag object
	 * 
	 * @return EEnumeration that contains the attribute values for the
	 *         attributes that belong to the tag object. If there is no
	 *         attributes then the method returns Empty enumeration
	 */
	public Enumeration getAttributeValues();

	/**
	 * Returns the name of the tag
	 * 
	 * @return the name of the tag
	 */
	public String getName();

	/**
	 * Returns a tag content
	 * 
	 * @return tag content. If the tag does not have a content, the method
	 *         returns empty string
	 */
	public String getContent();

	/**
	 * Returns the line in the XML file where the tag definition starts
	 * 
	 * @return the line in the XML file where the tag definition starts
	 */
	public int getLine();

	/**
	 * Returns the number of the child tags of this tag
	 * 
	 * @return child tags number
	 */
	public int size();

	/**
	 * Returns the child tag at a given position in order that is equivalent to
	 * the order of child tags defined in the XML file
	 * 
	 * @param aPosition
	 *            child tag's position
	 * @return the child tag at a given position
	 */
	public Tag getTagAt(int aPosition);

	/**
	 * Returns the content of a child tag with index position equals to aPos but
	 * only if its name is equal to aName. Otherwise the method throws an
	 * exception
	 * 
	 * @param aPos
	 *            index of the tag in its parent list
	 * @param aName
	 *            the name of the tag
	 * @return tag content
	 * @throws NullPointerException
	 *             if there is no tag on the given position
	 * @throws IllegalArgumentException
	 *             if the name of the tag on the given position does not match
	 *             the requested name
	 */
	public String getContent(int aPos, String aName);
}
