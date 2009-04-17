/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *     IBM Corporation - improvements and ongoing maintenance
 *******************************************************************************/
package org.eclipse.equinox.http.jetty;

import java.util.Dictionary;

/**
 * Jetty Customizer allows one to customize Jetty contexts and connectors.
 * <p>
 * This abstract class must be extended by clients which wish to customize
 * the created Jetty contexts or connectors further.
 * </p>
 * @since 1.1
 */
public abstract class JettyCustomizer {

	/**
	 * Called by the framework when the Jetty context has been created
	 * and initialized.
	 * <p>
	 * Implementors may perform additional configuration of the Jetty context.
	 * However, they must be aware that changing certain central functionalities
	 * of the context such as class loading are already configured by the 
	 * framework. Changing those may cause breakage and thus must be avoided.
	 * </p>
	 * @param context 
	 *             	the Jetty context; in case of Jetty 6 the context is of 
	 *             	type <code>org.mortbay.jetty.servlet.Context</code>
	 * @param settings
	 * 				the settings as passed to {@link JettyConfigurator#startServer(String, Dictionary)}
	 * @return context
	 *             	the customized context; in case of Jetty 6 the context is of 
	 *             	type <code>org.mortbay.jetty.servlet.Context</code>
	 */
	public Object customizeContext(Object context, Dictionary settings) {
		return context;
	}

	/**
	 * Called by the framework when the Jetty Http Connector has been created
	 * and initialized.
	 * <p>
	 * Implementors may perform additional configuration of the Jetty Connector.
	 * </p>
	 * @param connector 
	 *             	the Jetty connector; in case of Jetty 6 the context is of 
	 *             	type <code>org.mortbay.jetty.Connector</code>
	 * @param settings
	 * 				the settings as passed to {@link JettyConfigurator#startServer(String, Dictionary)}
	 * @return connector
	 *             	the customized connector; in case of Jetty 6 the connector is of 
	 *             	type <code>org.mortbay.jetty.Connector</code>
	 */
	public Object customizeHttpConnector(Object connector, Dictionary settings) {
		return connector;
	}

	/**
	 * Called by the framework when the Jetty Https Connector has been created
	 * and initialized.
	 * <p>
	 * Implementors may perform additional configuration of the Jetty Connector.
	 * </p>
	 * @param connector 
	 *             	the Jetty connector; in case of Jetty 6 the connector is of 
	 *             	type <code>org.mortbay.jetty.Connector</code>
	 * @param settings
	 * 				the settings as passed to {@link JettyConfigurator#startServer(String, Dictionary)}
	 * @return connector
	 *             	the customized connector; in case of Jetty 6 the connector is of 
	 *             	type <code>org.mortbay.jetty.Connector</code>
	 */
	public Object customizeHttpsConnector(Object connector, Dictionary settings) {
		return connector;
	}
}
