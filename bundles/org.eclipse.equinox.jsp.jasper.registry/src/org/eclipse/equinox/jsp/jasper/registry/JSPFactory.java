/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.jsp.jasper.registry;

import java.util.Hashtable;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.jsp.jasper.registry.Activator;
import org.eclipse.equinox.jsp.jasper.JspServlet;
import org.osgi.framework.Bundle;

/**
 * <p> 
 * The JSPFactory can be used in conjunction with org.eclipse.equinox.http.registry and the Servlets extension point
 * to allow the use of JSPs declaratively with the extension registry.
 * </p>
 * <p>
 * JSPFactory will accept a "path" parameter corresponding to the base path in the bundle to look up JSP resources.
 * This parameter can be set using the ":" separator approach or by xml parameter.
 * </p>
 * e.g. class="org.eclipse.equinox.jsp.jasper.registry.JSPFactory:/A/PATH" or &lt;parameter name="path" value="/A/PATH"/&gt;
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class JSPFactory implements IExecutableExtensionFactory, IExecutableExtension {

	private IConfigurationElement config;
	private String bundleResourcePath;

	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		this.config = config;
		if (data != null) {
			if (data instanceof String)
				bundleResourcePath = (String) data;
			else if (data instanceof Hashtable) {
				bundleResourcePath = (String) ((Hashtable) data).get("path"); //$NON-NLS-1$
			}
		}
	}

	public Object create() throws CoreException {
		Bundle b = Activator.getBundle(config.getContributor().getName()); //check for null and illegal state exception
		String alias = config.getAttribute("alias"); //$NON-NLS-1$
		if (alias != null && alias.indexOf("/*.") == alias.lastIndexOf('/')) { //$NON-NLS-1$
			alias = alias.substring(0, alias.lastIndexOf('/'));
			if (alias.length() == 0)
				alias = "/"; //$NON-NLS-1$
		}
		return new JspServlet(b, bundleResourcePath, alias);
	}
}
