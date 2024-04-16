/*******************************************************************************
 * Copyright (c) 2014 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 436698
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests.tb1;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;

import org.eclipse.equinox.http.servlet.tests.tb.AbstractTestResource;
import org.eclipse.equinox.http.servlet.tests.util.BaseFilter;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

/**
 * @author Raymond Augé
 */
public class TestResource5 extends AbstractTestResource {

	private final Collection<ServiceRegistration<?>> registrations = new ArrayList<>();

	@Override
	public void activate(ComponentContext componentContext) {
		Dictionary<String, String> resourceProps = new Hashtable<>();
		resourceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN, regexAlias());
		resourceProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX, getName());
		registrations
				.add(componentContext.getBundleContext().registerService(TestResource5.class, this, resourceProps));

		Dictionary<String, Object> filterProps = new Hashtable<>();
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F1");
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, regexAlias());
		registrations.add(componentContext.getBundleContext().registerService(Filter.class, f1, filterProps));

		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F2");
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, regexAlias());
		registrations.add(componentContext.getBundleContext().registerService(Filter.class, f2, filterProps));

		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME, "F3");
		filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN, regexAlias());
		filterProps.put(Constants.SERVICE_RANKING, 1);
		registrations.add(componentContext.getBundleContext().registerService(Filter.class, f3, filterProps));
	}

	@Override
	public void deactivate() {
		for (ServiceRegistration<?> registration : registrations) {
			registration.unregister();
		}
	}

	Filter f1 = new BaseFilter('c');
	Filter f2 = new BaseFilter('b');
	Filter f3 = new BaseFilter('d');
}
