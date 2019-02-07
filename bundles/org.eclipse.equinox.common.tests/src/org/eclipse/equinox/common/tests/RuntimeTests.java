/*******************************************************************************
 *  Copyright (c) 2018 Julian Honnen
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Julian Honnen - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.common.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@SuppressWarnings("deprecation")
@RunWith(Suite.class)
@SuiteClasses({
	CoreExceptionTest.class,
	OperationCanceledExceptionTest.class,
	PathTest.class,
	PluginVersionIdentifierTest.class,
	ProgressMonitorWrapperTest.class,
	QualifiedNameTest.class,
	SafeRunnerTest.class,
	StatusTest.class,
	SubMonitorSmallTicksTest.class,
	SubMonitorTest.class,
	SubProgressTest.class,
	URIUtilTest.class,
	URLTest.class
})
public class RuntimeTests {
	// intentionally left blank
}
