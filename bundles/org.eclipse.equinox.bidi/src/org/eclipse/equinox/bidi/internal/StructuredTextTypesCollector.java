/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
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
package org.eclipse.equinox.bidi.internal;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.equinox.bidi.custom.StructuredTextTypeHandler;
import org.eclipse.equinox.bidi.internal.consumable.*;

/**
 * Provides services related to registered structured text handlers.
 */
public class StructuredTextTypesCollector implements IRegistryEventListener {

	private static final String EXT_POINT = "org.eclipse.equinox.bidi.bidiTypes"; //$NON-NLS-1$

	private static final String CE_NAME = "typeDescription"; //$NON-NLS-1$
	private static final String ATTR_TYPE = "type"; //$NON-NLS-1$
	private static final String ATTR_HANDLER = "class"; //$NON-NLS-1$

	private Map<String, StructuredTextTypeHandler> types;
	private Map<String, IConfigurationElement> factories;

	static private StructuredTextTypesCollector instance = new StructuredTextTypesCollector();

	private StructuredTextTypesCollector() {
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		if (registry != null)
			registry.addListener(this, EXT_POINT);
	}

	/**
	 * @return a static instance.
	 */
	static public StructuredTextTypesCollector getInstance() {
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
	public StructuredTextTypeHandler getHandler(String type) {
		if (types == null)
			read();
		Object handler = types.get(type);
		if (handler instanceof StructuredTextTypeHandler)
			return (StructuredTextTypeHandler) handler;
		return null;
	}

	private void read() {
		if (types == null)
			types = new HashMap<>();
		else
			types.clear();

		if (factories == null)
			factories = new HashMap<String, IConfigurationElement>();
		else
			factories.clear();

		IExtensionRegistry registry = RegistryFactory.getRegistry();
		if (registry == null) {
			types.putAll(getDefaultTypeHandlers());
			return;
		}

		IExtensionPoint extPoint = registry.getExtensionPoint(EXT_POINT);
		IExtension[] extensions = extPoint.getExtensions();

		for (IExtension extension : extensions) {
			IConfigurationElement[] confElements = extension.getConfigurationElements();
			for (IConfigurationElement confElement : confElements) {
				if (!CE_NAME.equals(confElement.getName()))
					StructuredTextActivator.logError("BiDi types: unexpected element name " + confElement.getName(), new IllegalArgumentException()); //$NON-NLS-1$
				String type = confElement.getAttribute(ATTR_TYPE);
				Object handler;
				try {
					handler = confElement.createExecutableExtension(ATTR_HANDLER);
				} catch (CoreException e) {
					StructuredTextActivator.logError("BiDi types: unable to create handler for " + type, e); //$NON-NLS-1$
					continue;
				}
				if (handler instanceof StructuredTextTypeHandler) {
					types.put(type, (StructuredTextTypeHandler) handler);
					factories.put(type, confElement);
				}
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
	 *         to structured text type handler (value type: {@link StructuredTextTypeHandler}).
	 */
	public static Map<String, StructuredTextTypeHandler> getDefaultTypeHandlers() {
		Map<String, StructuredTextTypeHandler> types = new LinkedHashMap<String, StructuredTextTypeHandler>();

		types.put(StructuredTextTypeHandlerFactory.COMMA_DELIMITED, new StructuredTextComma());
		types.put(StructuredTextTypeHandlerFactory.EMAIL, new StructuredTextEmail());
		types.put(StructuredTextTypeHandlerFactory.FILE, new StructuredTextFile());
		types.put(StructuredTextTypeHandlerFactory.JAVA, new StructuredTextJava());
		types.put(StructuredTextTypeHandlerFactory.REGEX, new StructuredTextRegex());
		types.put(StructuredTextTypeHandlerFactory.SQL, new StructuredTextSql());
		types.put(StructuredTextTypeHandlerFactory.UNDERSCORE, new StructuredTextUnderscore());
		types.put(StructuredTextTypeHandlerFactory.URL, new StructuredTextURL());
		types.put(StructuredTextTypeHandlerFactory.XPATH, new StructuredTextXPath());

		return Collections.unmodifiableMap(types);
	}
}
