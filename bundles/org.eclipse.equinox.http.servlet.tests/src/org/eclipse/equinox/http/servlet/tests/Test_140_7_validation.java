/*******************************************************************************
 * Copyright (c) Mar. 28, 2019 Liferay, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Liferay, Inc. - tests
 ******************************************************************************/

package org.eclipse.equinox.http.servlet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContextListener;

import org.eclipse.equinox.http.servlet.testbase.BaseTest;
import org.eclipse.equinox.http.servlet.tests.util.MockSCL;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.runtime.dto.DTOConstants;
import org.osgi.service.http.runtime.dto.FailedListenerDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

public class Test_140_7_validation extends BaseTest {

	@Test
	public void test_140_7_validation() {
		BundleContext context = getBundleContext();

		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "true");
		ServiceRegistration<?> sr = context.registerService(ServletContextListener.class,
				new MockSCL(new AtomicReference<>()), properties);
		registrations.add(sr);

		ListenerDTO listenerDTO = getListenerDTOByServiceId(DEFAULT, getServiceId(sr));
		assertNotNull(listenerDTO);

		properties.remove(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER);
		sr.setProperties(properties);

		listenerDTO = getListenerDTOByServiceId(DEFAULT, getServiceId(sr));
		assertNull(listenerDTO);

		properties.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER, "blah");
		sr.setProperties(properties);

		FailedListenerDTO failedListenerDTO = getFailedListenerDTOByServiceId(getServiceId(sr));
		assertNotNull(failedListenerDTO);
		assertEquals(DTOConstants.FAILURE_REASON_VALIDATION_FAILED, failedListenerDTO.failureReason);

		listenerDTO = getListenerDTOByServiceId(DEFAULT, getServiceId(sr));
		assertNull(listenerDTO);
	}

}
