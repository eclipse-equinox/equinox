/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.bidi.internal;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.bidi.STextTypeHandlerFactory;
import org.eclipse.equinox.bidi.custom.STextTypeHandler;
import org.eclipse.equinox.bidi.internal.consumable.*;

/**
 * Provides services related to registered structured text handlers.
 */
public class STextTypesCollector implements IRegistryEventListener {

	private static final String EXT_POINT = "org.eclipse.equinox.bidi.bidiTypes"; //$NON-NLS-1$

	private static final String CE_NAME = "typeDescription"; //$NON-NLS-1$
	private static final String ATTR_TYPE = "type"; //$NON-NLS-1$
	private static final String ATTR_HANDLER = "class"; //$NON-NLS-1$

	private Map types;
	private Map factories;

	static private STextTypesCollector instance = new STextTypesCollector();

	private STextTypesCollector() {
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		if (registry != null)
			registry.addListener(this, EXT_POINT);
	}

	/**
	 * @return a static instance.
	 */
	static public STextTypesCollector getInstance() {
		return instance;
	}

	/**
	 * @return a list of all the registered structured text handler types.
	 */
	public String[] getTypes() {
		if (types == null)
			read();
		int size = types.size();
		String[] result = new String[size];
		types.keySet().toArray(result);
		return result;
	}

	/**
	 * @param type the identifier for a structured text handler.
	 * 
	 * @return the structured text handler instance.
	 */
	public STextTypeHandler getHandler(String type) {
		if (types == null)
			read();
		Object handler = types.get(type);
		if (handler instanceof STextTypeHandler)
			return (STextTypeHandler) handler;
		return null;
	}

	private void read() {
		if (types == null)
			types = new HashMap();
		else
			types.clear();

		if (factories == null)
			factories = new HashMap();
		else
			factories.clear();

		IExtensionRegistry registry = RegistryFactory.getRegistry();
		if (registry == null) {
			types.putAll(getDefaultTypeHandlers());
			return;
		}

		IExtensionPoint extPoint = registry.getExtensionPoint(EXT_POINT);
		IExtension[] extensions = extPoint.getExtensions();

		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] confElements = extensions[i].getConfigurationElements();
			for (int j = 0; j < confElements.length; j++) {
				if (!CE_NAME.equals(confElements[j].getName()))
					STextActivator.logError("BiDi types: unexpected element name " + confElements[j].getName(), new IllegalArgumentException()); //$NON-NLS-1$
				String type = confElements[j].getAttribute(ATTR_TYPE);
				Object handler;
				try {
					handler = confElements[j].createExecutableExtension(ATTR_HANDLER);
				} catch (CoreException e) {
					STextActivator.logError("BiDi types: unable to create handler for " + type, e); //$NON-NLS-1$
					continue;
				}
				types.put(type, handler);
				factories.put(type, confElements[j]);
			}
		}
	}

	public void added(IExtension[] extensions) {
		types = null;
		factories = null;
	}

	public void removed(IExtension[] extensions) {
		types = null;
		factories = null;
	}

	public void added(IExtensionPoint[] extensionPoints) {
		types = null;
		factories = null;
	}

	public void removed(IExtensionPoint[] extensionPoints) {
		types = null;
		factories = null;
	}

	/**
	 * Returns the default structured text type handlers. These handlers are
	 * also supported without OSGi running.
	 * 
	 * @return a map from structured text type handler identifier (key type: {@link String})
	 *         to structured text type handler (value type: {@link STextTypeHandler}).
	 */
	public static Map getDefaultTypeHandlers() {
		Map types = new LinkedHashMap();

		types.put(STextTypeHandlerFactory.COMMA_DELIMITED, new STextComma());
		types.put(STextTypeHandlerFactory.EMAIL, new STextEmail());
		types.put(STextTypeHandlerFactory.FILE, new STextFile());
		types.put(STextTypeHandlerFactory.JAVA, new STextJava());
		types.put(STextTypeHandlerFactory.MATH, new STextMath());
		types.put(STextTypeHandlerFactory.PROPERTY, new STextProperty());
		types.put(STextTypeHandlerFactory.REGEXP, new STextRegex());
		types.put(STextTypeHandlerFactory.SQL, new STextSql());
		types.put(STextTypeHandlerFactory.SYSTEM_USER, new STextSystem());
		types.put(STextTypeHandlerFactory.UNDERSCORE, new STextUnderscore());
		types.put(STextTypeHandlerFactory.URL, new STextURL());
		types.put(STextTypeHandlerFactory.XPATH, new STextXPath());

		return Collections.unmodifiableMap(types);
	}
}
