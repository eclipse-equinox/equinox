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

import java.io.*;
import org.eclipse.equinox.internal.util.xml.impl.XMLParserImpl;

/**
 * <p>
 * Class used for reading of xml files, creating tree structure of 'TagClass'
 * for each xml tag. When reader reaches a closed tag it notifies a given
 * 'TagListener' and sends the last tag to it. If closing tag does not
 * correspond with the last open IllegalArgumentException is thrown. There is a
 * debug property 'xml.debug' used to dump an Exceptions occurred while
 * operation is running.
 * </p>
 * 
 * <p>
 * The parser, in general, is a simple XML parser that implements
 * &quot;Recursive descent&quot; parsing method.
 * 
 * Known limitations:<br>
 * 
 * <pre>
 *  Currently this XMLParser does not support the following special tags:
 *  1. &lt;?TAG_NAME ..... ?&gt;   or also &quot;Processing Instructions&quot;
 *  2. &lt;!DOCTYPE .... &gt;
 *  3. &lt;!ELEMENT .... &gt;
 *  4. &lt;!ATTLIST .... &gt;
 *  5. &lt;!ENTITY .... &gt;
 * </pre>
 * 
 * <br>
 * The parser skippes these tags (it searches for '>' symbol and closes the
 * 'special' tag).<br>
 * 
 * @author Ivan Dimitrov
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class XMLParser {

	/**
	 * Parses a XML file given through aInputStream and during the parsing
	 * notifies aListener for close-tag and open-tag events <br>
	 * <br>
	 * <b>Note: The XMLReader does not close the passed Reader or InputStream
	 * 
	 * @param aInputStream
	 *            an InputStream to read the XML file from
	 * @param aListener
	 *            ExTagListener that will be notified on close-tag and open-tag
	 *            events
	 * @param aLevel
	 *            indicates the tag level that the listener will be invoked for.
	 *            For example if the XML is:<br>
	 * 
	 * <pre>
	 *  &lt;a&gt;
	 *    &lt;b&gt;
	 *      &lt;c /&gt;
	 *    &lt;/b&gt;
	 *  &lt;/a&gt;
	 * </pre>
	 * 
	 * <br>
	 *            and the passed aLevel is 2 then the listener will be invoked
	 *            only for tags that have level 2, i.e. in our example the
	 *            listener will be invoked only for tag &lt;b&gt;<br>
	 *            <ul>
	 *            <li>Value less than 0 indicates &quot;invoke listener for all
	 *            tags no matter what are their levels&quot;</li>
	 *            <li>Value of 0 indicates that the listener must not be
	 *            invoked in general no matter what is the tag level</li>
	 *            <li>Value greater than 0 indicates the tag level that the
	 *            listener will be invoked for</li>
	 *            description
	 * @throws IOException
	 *             if some IO error occurs when reading the XML file or if a
	 *             parser error occurs.
	 */
	public static void parseXML(InputStream aInputStream, ExTagListener aListener, int aLevel) throws IOException {
		XMLParserImpl xml = new XMLParserImpl(aInputStream, aListener);
		xml.setLevel(aLevel);
		xml.parseXML();
	}

	/**
	 * Parses a XML file given through aReader and during the parsing notifies
	 * aListener for close-tag and open-tag events <br>
	 * <br>
	 * <b>Note: The XMLReader does not close the passed Reader or InputStream
	 * 
	 * @param aReader
	 *            aReader to read the XML file from
	 * @param aListener
	 *            ExTagListener that will be notified on close-tag and open-tag
	 *            events
	 * @param aLevel
	 *            see parseXML(Reader aReader, TagListener aListener, int aLevel
	 *            description
	 * @throws IOException
	 *             if some IO error occurs when reading the XML file or if a
	 *             parser error occurs.
	 */
	public static void parseXML(Reader aReader, ExTagListener aListener, int aLevel) throws IOException {
		XMLParserImpl xml = new XMLParserImpl(aReader, aListener);
		xml.setLevel(aLevel);
		xml.parseXML();
	}
}
