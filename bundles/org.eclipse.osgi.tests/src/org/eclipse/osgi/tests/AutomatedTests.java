/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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

import org.eclipse.osgi.tests.bundles.BundleTests;
import org.eclipse.osgi.tests.debugoptions.DebugOptionsTestCase;
import org.eclipse.osgi.tests.eventmgr.EventManagerTests;
import org.eclipse.osgi.tests.filter.BundleContextFilterTests;
import org.eclipse.osgi.tests.filter.FrameworkUtilFilterTests;
import org.eclipse.osgi.tests.hooks.framework.AllFrameworkHookTests;
import org.eclipse.osgi.tests.internal.plugins.InstallTests;
import org.eclipse.osgi.tests.listeners.ExceptionHandlerTests;
import org.eclipse.osgi.tests.misc.MiscTests;
import org.eclipse.osgi.tests.permissions.AdminPermissionTests;
import org.eclipse.osgi.tests.permissions.PackagePermissionTests;
import org.eclipse.osgi.tests.permissions.ServicePermissionTests;
import org.eclipse.osgi.tests.serviceregistry.ServiceRegistryTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ org.eclipse.osgi.tests.container.AllTests.class, AllFrameworkHookTests.class, InstallTests.class,
		org.eclipse.osgi.tests.eclipseadaptor.AllTests.class, org.eclipse.osgi.tests.services.resolver.AllTests.class,
		DebugOptionsTestCase.class, org.eclipse.equinox.log.test.AllTests.class,
		org.eclipse.osgi.tests.security.SecurityTestSuite.class, org.eclipse.osgi.tests.appadmin.AllTests.class,
		ExceptionHandlerTests.class, org.eclipse.osgi.tests.configuration.AllTests.class,
		org.eclipse.osgi.tests.services.datalocation.AllTests.class, org.eclipse.osgi.tests.util.AllTests.class,
		MiscTests.class, BundleTests.class, ServiceRegistryTests.class, EventManagerTests.class,
		BundleContextFilterTests.class, FrameworkUtilFilterTests.class, AdminPermissionTests.class,
		ServicePermissionTests.class, PackagePermissionTests.class,
		org.eclipse.osgi.tests.securityadmin.AllSecurityAdminTests.class,
		org.eclipse.osgi.tests.resource.AllTests.class })

public class AutomatedTests {
}
