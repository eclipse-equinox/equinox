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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.bidi.custom.STextProcessor;

public class STextTypesCollector implements IRegistryEventListener {

	private static final String EXT_POINT = "org.eclipse.equinox.bidi.bidiTypes"; //$NON-NLS-1$

	private static final String CE_NAME = "typeDescription"; //$NON-NLS-1$
	private static final String ATTR_TYPE = "type"; //$NON-NLS-1$
	private static final String ATTR_PROCESSOR = "class"; //$NON-NLS-1$

	private Map types;
	private Map factories;

	static private STextTypesCollector instance = new STextTypesCollector();

	private STextTypesCollector() {
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		registry.addListener(this, EXT_POINT);
	}

	static public STextTypesCollector getInstance() {
		return instance;
	}

	public String[] getTypes() {
		if (types == null)
			read();
		int size = types.size();
		String[] result = new String[size];
		types.keySet().toArray(result);
		return result;
	}

	public STextProcessor getProcessor(String type) {
		if (types == null)
			read();
		Object processor = types.get(type);
		if (processor instanceof STextProcessor)
			return (STextProcessor) processor;
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
		IExtensionPoint extPoint = registry.getExtensionPoint(EXT_POINT);
		IExtension[] extensions = extPoint.getExtensions();

		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] confElements = extensions[i].getConfigurationElements();
			for (int j = 0; j < confElements.length; j++) {
				if (CE_NAME != confElements[j].getName())
					STextActivator.logError("BiDi types: unexpected element name " + confElements[j].getName(), new IllegalArgumentException()); //$NON-NLS-1$
				String type = confElements[j].getAttribute(ATTR_TYPE);
				Object processor;
				try {
					processor = confElements[j].createExecutableExtension(ATTR_PROCESSOR);
				} catch (CoreException e) {
					STextActivator.logError("BiDi types: unable to create processor for " + type, e); //$NON-NLS-1$
					continue;
				}
				types.put(type, processor);
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
}
