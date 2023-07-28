/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package org.eclipse.osgi.internal.framework;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;

class XMLParsingServiceFactory implements ServiceFactory<Object> {
	private final boolean isSax;
	private final boolean setTccl;

	public XMLParsingServiceFactory(boolean isSax, boolean setTccl) {
		this.isSax = isSax;
		this.setTccl = setTccl;
	}

	@Override
	public Object getService(Bundle bundle, ServiceRegistration<Object> registration) {
		if (!setTccl || bundle == null)
			return createService();
		/*
		 * Set the TCCL while creating jaxp factory instances to the
		 * requesting bundles class loader.  This is needed to
		 * work around bug 285505.  There are issues if multiple
		 * xerces implementations are available on the bundles class path
		 *
		 * The real issue is that the ContextFinder will only delegate
		 * to the framework class loader in this case.  This class
		 * loader forces the requesting bundle to be delegated to for
		 * TCCL loads.
		 */
		final ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			BundleWiring wiring = bundle.adapt(BundleWiring.class);
			ClassLoader cl = wiring == null ? null : wiring.getClassLoader();
			if (cl != null)
				Thread.currentThread().setContextClassLoader(cl);
			return createService();
		} finally {
			Thread.currentThread().setContextClassLoader(savedClassLoader);
		}
	}

	private Object createService() {
		if (isSax) {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			try {
				// ignore DOCTYPE:
				factory.setFeature("http://xml.org/sax/features/external-general-entities", false); //$NON-NLS-1$
				factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false); //$NON-NLS-1$
				factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			return factory;
		}
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			// completely disable external entities declarations:
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false); //$NON-NLS-1$
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		return factory;
	}

	@Override
	public void ungetService(Bundle bundle, ServiceRegistration<Object> registration, Object service) {
		// Do nothing.
	}
}