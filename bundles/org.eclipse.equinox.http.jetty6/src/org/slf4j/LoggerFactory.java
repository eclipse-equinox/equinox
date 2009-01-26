/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.slf4j;

//NOTE: This class is not a real SLF4J implementation and MUST NOT be exported!
//It is a place-holder to allow overriding the default logging done in Jetty
//See org.mortbay.log.Log and org.mortbay.log.Slf4jLog 
public class LoggerFactory {

	public static Logger getLogger(String name) {
		Logger logger = Logger.getRootLogger();
		return (Logger) logger.getLogger(name);
	}
}
