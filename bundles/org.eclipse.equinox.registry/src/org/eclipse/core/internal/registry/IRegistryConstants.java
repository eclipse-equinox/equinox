/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

public interface IRegistryConstants {

	/**
	 * The unique identifier constant (value "<code>org.eclipse.core.runtime</code>")
	 * of the Core Runtime (pseudo-) plug-in.
	 */
	public static final String RUNTIME_NAME = "org.eclipse.core.runtime"; //$NON-NLS-1$

	// command line options
	public static final String NO_REGISTRY_CACHE = "-noregistrycache"; //$NON-NLS-1$	
	public static final String NO_LAZY_REGISTRY_CACHE_LOADING = "-noLazyRegistryCacheLoading"; //$NON-NLS-1$		
	public static final String MULTI_LANGUAGE = "-registryMultiLanguage"; //$NON-NLS-1$		

	// System options
	public static final String PROP_NO_LAZY_CACHE_LOADING = "eclipse.noLazyRegistryCacheLoading"; //$NON-NLS-1$
	public static final String PROP_CHECK_CONFIG = "osgi.checkConfiguration"; //$NON-NLS-1$
	public static final String PROP_NO_REGISTRY_CACHE = "eclipse.noRegistryCache"; //$NON-NLS-1$
	public static final String PROP_DEFAULT_REGISTRY = "eclipse.createRegistry"; //$NON-NLS-1$
	public static final String PROP_REGISTRY_NULL_USER_TOKEN = "eclipse.registry.nulltoken"; //$NON-NLS-1$
	public static final String PROP_MULTI_LANGUAGE = "eclipse.registry.MultiLanguage"; //$NON-NLS-1$

	// OSGI system properties
	public static final String PROP_NL = "osgi.nl"; //$NON-NLS-1$
	public static final String PROP_OS = "osgi.os"; //$NON-NLS-1$
	public static final String PROP_WS = "osgi.ws"; //$NON-NLS-1$

	/**
	 * Specific error code supplied to the Status objects 
	 */
	static final int PLUGIN_ERROR = 1;
}
