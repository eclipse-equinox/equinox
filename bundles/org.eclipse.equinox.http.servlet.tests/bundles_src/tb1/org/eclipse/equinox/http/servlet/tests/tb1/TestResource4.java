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

import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.ServletException;

import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.eclipse.equinox.http.servlet.tests.tb.AbstractTestResource;
import org.eclipse.equinox.http.servlet.tests.util.BaseFilter;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.NamespaceException;

/**
 * @author Raymond Augé
 */
public class TestResource4 extends AbstractTestResource {

	@Override
	public void activate(ComponentContext componentContext) throws ServletException, NamespaceException {
		ExtendedHttpService service = (ExtendedHttpService) getHttpService();
		service.registerResources(regexAlias(), getName(), null);
		service.registerFilter(regexAlias(), f1, new Hashtable<>(), null);
		service.registerFilter(regexAlias(), f2, new Hashtable<>(), null);
		Hashtable<String, String> hashtable = new Hashtable<>();
		hashtable.put("filter-priority", "1");
		service.registerFilter(regexAlias(), f3, hashtable, null);
	}

	@Override
	public void deactivate() {
		ExtendedHttpService service = (ExtendedHttpService) getHttpService();
		service.unregister(regexAlias());
		service.unregisterFilter(f1);
		service.unregisterFilter(f2);
		service.unregisterFilter(f3);
	}

	private Filter f1 = new BaseFilter('c');
	private Filter f2 = new BaseFilter('b');
	private Filter f3 = new BaseFilter('d');

}
