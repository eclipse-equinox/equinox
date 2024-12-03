/*******************************************************************************
 * Copyright (c) 2004, 2022 IBM Corporation and others.
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
package org.eclipse.osgi.tests;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ //
		org.eclipse.osgi.tests.container.AllTests.class, //
		org.eclipse.osgi.tests.hooks.framework.AllFrameworkHookTests.class, //
		org.eclipse.osgi.tests.internal.plugins.InstallTests.class, //
		org.eclipse.osgi.tests.eclipseadaptor.AllTests.class, //
		org.eclipse.osgi.tests.services.resolver.AllTests.class, //
		org.eclipse.osgi.tests.debugoptions.DebugOptionsTestCase.class, //
		org.eclipse.equinox.log.test.AllTests.class, //
		org.eclipse.osgi.tests.security.SecurityTestSuite.class, //
		org.eclipse.osgi.tests.appadmin.AllTests.class, //
		org.eclipse.osgi.tests.listeners.ExceptionHandlerTests.class, //
		org.eclipse.osgi.tests.configuration.AllTests.class, //
		org.eclipse.osgi.tests.services.datalocation.AllTests.class, //
		org.eclipse.osgi.tests.util.AllTests.class, //
		org.eclipse.osgi.tests.misc.MiscTests.class, //
		org.eclipse.osgi.tests.bundles.BundleTests.class, //
		org.eclipse.osgi.tests.serviceregistry.AllTests.class, //
		org.eclipse.osgi.tests.eventmgr.EventManagerTests.class, //
		org.eclipse.osgi.tests.filter.FilterTests.class, //
		org.eclipse.osgi.tests.permissions.AllTests.class, //
		org.eclipse.osgi.tests.securityadmin.AllSecurityAdminTests.class, //
		org.eclipse.osgi.tests.resource.AllTests.class, //
		org.eclipse.osgi.tests.url.AllTests.class })
public class AutomatedTests {
}
