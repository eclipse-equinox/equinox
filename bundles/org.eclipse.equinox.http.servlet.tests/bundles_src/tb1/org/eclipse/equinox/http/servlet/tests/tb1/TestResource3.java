/*******************************************************************************
 * Copyright (c) 2014 Raymond Augé and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests.tb1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletException;

import org.eclipse.equinox.http.servlet.tests.tb.AbstractTestResource;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Raymond Augé
 */
public class TestResource3 extends AbstractTestResource {
	private final Collection<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
	@Override
	public void activate(ComponentContext componentContext) throws ServletException, NamespaceException {
		Dictionary<String, String> resourceProps = new Hashtable<String, String>();
		resourceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, regexAlias());
		resourceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, getName());
		registrations.add(componentContext.getBundleContext().registerService(TestResource3.class, this, resourceProps));
	}

	@Override
	public void deactivate() {
		for (ServiceRegistration<?> registration : registrations) {
			registration.unregister();
		}
	}
}
