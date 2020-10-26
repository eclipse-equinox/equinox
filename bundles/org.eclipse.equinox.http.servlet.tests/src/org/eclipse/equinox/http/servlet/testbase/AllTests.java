/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
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
package org.eclipse.equinox.http.servlet.testbase;

import org.eclipse.equinox.http.servlet.tests.AuthenticationTest;
import org.eclipse.equinox.http.servlet.tests.Bug500783_Test;
import org.eclipse.equinox.http.servlet.tests.Bug562440_Test;
import org.eclipse.equinox.http.servlet.tests.Bug562843_2_Test;
import org.eclipse.equinox.http.servlet.tests.Bug562843_Test;
import org.eclipse.equinox.http.servlet.tests.Bug564747_Test;
import org.eclipse.equinox.http.servlet.tests.ContextHelperCustomizerTests;
import org.eclipse.equinox.http.servlet.tests.DispatchingTest;
import org.eclipse.equinox.http.servlet.tests.PreprocessorTestCase;
import org.eclipse.equinox.http.servlet.tests.ServletTest;
import org.eclipse.equinox.http.servlet.tests.TestHttpServiceAndErrorPage;
import org.eclipse.equinox.http.servlet.tests.TestHttpServiceAndNamedServlet;
import org.eclipse.equinox.http.servlet.tests.TestUpload;
import org.eclipse.equinox.http.servlet.tests.TestUploadWithParameter;
import org.eclipse.equinox.http.servlet.tests.Test_140_11_3;
import org.eclipse.equinox.http.servlet.tests.Test_140_2_17to22;
import org.eclipse.equinox.http.servlet.tests.Test_140_2_26to27;
import org.eclipse.equinox.http.servlet.tests.Test_140_2_39to41;
import org.eclipse.equinox.http.servlet.tests.Test_140_2_6_getResourcePaths;
import org.eclipse.equinox.http.servlet.tests.Test_140_4_11to13;
import org.eclipse.equinox.http.servlet.tests.Test_140_4_14to15;
import org.eclipse.equinox.http.servlet.tests.Test_140_4_16;
import org.eclipse.equinox.http.servlet.tests.Test_140_4_17to22;
import org.eclipse.equinox.http.servlet.tests.Test_140_4_1_22to23;
import org.eclipse.equinox.http.servlet.tests.Test_140_4_26to31;
import org.eclipse.equinox.http.servlet.tests.Test_140_4_42to44;
import org.eclipse.equinox.http.servlet.tests.Test_140_4_9;
import org.eclipse.equinox.http.servlet.tests.Test_140_6_1;
import org.eclipse.equinox.http.servlet.tests.Test_140_6_20to21_commonProperties;
import org.eclipse.equinox.http.servlet.tests.Test_140_7_validation;
import org.eclipse.equinox.http.servlet.tests.Test_140_9_ServletContextDTO_custom_listener;
import org.eclipse.equinox.http.servlet.tests.Test_140_9_ServletContextDTO_default_listener;
import org.eclipse.equinox.http.servlet.tests.Test_table_140_1_HTTP_WHITEBOARD_CONTEXT_NAME_bindUsingContextSelect;
import org.eclipse.equinox.http.servlet.tests.Test_table_140_1_HTTP_WHITEBOARD_CONTEXT_NAME_tieGoesToOldest;
import org.eclipse.equinox.http.servlet.tests.Test_table_140_1_HTTP_WHITEBOARD_CONTEXT_PATH_type;
import org.eclipse.equinox.http.servlet.tests.Test_table_140_4_HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED;
import org.eclipse.equinox.http.servlet.tests.Test_table_140_4_HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED_validate;
import org.eclipse.equinox.http.servlet.tests.Test_table_140_4_HTTP_WHITEBOARD_SERVLET_ERROR_PAGE_4xx;
import org.eclipse.equinox.http.servlet.tests.Test_table_140_4_HTTP_WHITEBOARD_SERVLET_ERROR_PAGE_exception;
import org.eclipse.equinox.http.servlet.tests.Test_table_140_5_HTTP_WHITEBOARD_FILTER_DISPATCHER_error;
import org.eclipse.equinox.http.servlet.tests.Test_table_140_5_HTTP_WHITEBOARD_FILTER_DISPATCHER_request;
import org.eclipse.equinox.http.servlet.tests.Test_table_140_5_HTTP_WHITEBOARD_FILTER_PATTERN;
import org.eclipse.equinox.http.servlet.tests.Test_table_140_5_HTTP_WHITEBOARD_FILTER_REGEX;
import org.eclipse.equinox.http.servlet.tests.Test_table_140_6_HTTP_WHITEBOARD_RESOURCE_validation;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	AuthenticationTest.class,
	DispatchingTest.class,
	PreprocessorTestCase.class,
	ServletTest.class,
	Test_140_11_3.class,
	Test_140_2_17to22.class,
	Test_140_2_26to27.class,
	Test_140_2_39to41.class,
	Test_140_2_6_getResourcePaths.class,
	Test_140_4_11to13.class,
	Test_140_4_14to15.class,
	Test_140_4_16.class,
	Test_140_4_17to22.class,
	Test_140_4_1_22to23.class,
	Test_140_4_26to31.class,
	Test_140_4_42to44.class,
	Test_140_4_9.class,
	Test_140_6_1.class,
	Test_140_6_20to21_commonProperties.class,
	Test_140_7_validation.class,
	Test_140_9_ServletContextDTO_custom_listener.class,
	Test_140_9_ServletContextDTO_default_listener.class,
	Test_table_140_1_HTTP_WHITEBOARD_CONTEXT_NAME_bindUsingContextSelect.class,
	Test_table_140_1_HTTP_WHITEBOARD_CONTEXT_NAME_tieGoesToOldest.class,
	Test_table_140_1_HTTP_WHITEBOARD_CONTEXT_PATH_type.class,
	Test_table_140_4_HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED.class,
	Test_table_140_4_HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED_validate.class,
	Test_table_140_4_HTTP_WHITEBOARD_SERVLET_ERROR_PAGE_4xx.class,
	Test_table_140_4_HTTP_WHITEBOARD_SERVLET_ERROR_PAGE_exception.class,
	Test_table_140_5_HTTP_WHITEBOARD_FILTER_DISPATCHER_error.class,
	Test_table_140_5_HTTP_WHITEBOARD_FILTER_DISPATCHER_request.class,
	Test_table_140_5_HTTP_WHITEBOARD_FILTER_PATTERN.class,
	Test_table_140_5_HTTP_WHITEBOARD_FILTER_REGEX.class,
	Test_table_140_6_HTTP_WHITEBOARD_RESOURCE_validation.class,
	TestHttpServiceAndErrorPage.class,
	TestHttpServiceAndNamedServlet.class,
	TestUpload.class,
	TestUploadWithParameter.class,
	ContextHelperCustomizerTests.class,
	Bug500783_Test.class,
	Bug562843_Test.class,
	Bug562843_2_Test.class,
	Bug564747_Test.class,
	Bug562440_Test.class
})
public class AllTests {
	// see @SuiteClasses
}
