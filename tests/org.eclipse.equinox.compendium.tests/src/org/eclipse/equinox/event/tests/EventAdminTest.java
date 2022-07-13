/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others
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
package org.eclipse.equinox.event.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Dictionary;
import java.util.Hashtable;
import org.eclipse.equinox.compendium.tests.Activator;
import org.junit.*;
import org.osgi.framework.*;
import org.osgi.service.event.*;

public class EventAdminTest {
	private EventAdmin eventAdmin;
	private ServiceReference<EventAdmin> eventAdminReference;

	@Before
	public void setUp() throws Exception {
		eventAdminReference = Activator.getBundleContext().getServiceReference(EventAdmin.class);
		eventAdmin = Activator.getBundleContext().getService(eventAdminReference);
	}

	@After
	public void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(eventAdminReference);
	}

	/*
	 * Ensures EventAdmin does not deliver an event published on topic "a/b/c" 
	 * to an EventHandler listening to topic a/b/c/*.
	 * 
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=325064.
	 */
	@Test
	public void testEventDeliveryForWildcardTopic1() {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EventConstants.EVENT_TOPIC, "a/b/c/*"); //$NON-NLS-1$
		BundleContext bundleContext = Activator.getBundleContext();
		EventHandlerHelper handler = new EventHandlerHelper();
		ServiceRegistration<EventHandler> handlerRegistration = bundleContext.registerService(EventHandler.class, handler, properties);
		Event event = new Event("a/b/c", (Dictionary<String, Object>) null); //$NON-NLS-1$
		eventAdmin.sendEvent(event);
		assertNull("Received event published to topic 'a/b/c' while listening to 'a/b/c/*'", handler.lastEvent()); //$NON-NLS-1$
		handlerRegistration.unregister();
	}

	/*
	 * Ensures EventAdmin does not deliver an event published on topic "a/b" to 
	 * an EventHandler listening to topic a/b/c/*.
	 */
	@Test
	public void testEventDeliveryForWildcardTopic2() {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EventConstants.EVENT_TOPIC, "a/b/c/*"); //$NON-NLS-1$
		BundleContext bundleContext = Activator.getBundleContext();
		EventHandlerHelper handler = new EventHandlerHelper();
		ServiceRegistration<EventHandler> handlerRegistration = bundleContext.registerService(EventHandler.class, handler, properties);
		Event event = new Event("a/b", (Dictionary<String, Object>) null); //$NON-NLS-1$
		eventAdmin.sendEvent(event);
		assertNull("Received event published to topic 'a/b' while listening to 'a/b/c/*'", handler.lastEvent()); //$NON-NLS-1$
		handlerRegistration.unregister();
	}

	/*
	 * Ensures EventAdmin does not deliver an event published on topic "a" to 
	 * an EventHandler listening to topic a/b/c/*.
	 */
	@Test
	public void testEventDeliveryForWildcardTopic3() {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EventConstants.EVENT_TOPIC, "a/b/c/*"); //$NON-NLS-1$
		BundleContext bundleContext = Activator.getBundleContext();
		EventHandlerHelper handler = new EventHandlerHelper();
		ServiceRegistration<EventHandler> handlerRegistration = bundleContext.registerService(EventHandler.class, handler, properties);
		Event event = new Event("a", (Dictionary<String, Object>) null); //$NON-NLS-1$
		eventAdmin.sendEvent(event);
		assertNull("Received event published to topic 'a' while listening to 'a/b/c/*'", handler.lastEvent()); //$NON-NLS-1$
		handlerRegistration.unregister();
	}

	/*
	 * Ensures EventAdmin delivers an event published on topic "a/b/c/d" to an 
	 * EventHandler listening to topic "a/b/c/*".
	 */
	@Test
	public void testEventDeliveryForWildcardTopic4() {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EventConstants.EVENT_TOPIC, "a/b/c/*"); //$NON-NLS-1$
		BundleContext bundleContext = Activator.getBundleContext();
		EventHandlerHelper handler = new EventHandlerHelper();
		ServiceRegistration<EventHandler> handlerRegistration = bundleContext.registerService(EventHandler.class, handler, properties);
		Event event = new Event("a/b/c/d", (Dictionary<String, Object>) null); //$NON-NLS-1$
		eventAdmin.sendEvent(event);
		assertNotNull("Did not receive event published to topic 'a/b/c/d' while listening to 'a/b/c/*'", handler.lastEvent()); //$NON-NLS-1$
		handlerRegistration.unregister();
	}

	/*
	 * Ensures EventAdmin delivers an event published on topic "a/b/c/d/e" to 
	 * an EventHandler listening to topic "a/b/c/*".
	 */
	@Test
	public void testEventDeliveryForWildcardTopic5() {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EventConstants.EVENT_TOPIC, "a/b/c/*"); //$NON-NLS-1$
		BundleContext bundleContext = Activator.getBundleContext();
		EventHandlerHelper handler = new EventHandlerHelper();
		ServiceRegistration<EventHandler> handlerRegistration = bundleContext.registerService(EventHandler.class, handler, properties);
		Event event = new Event("a/b/c/d/e", (Dictionary<String, Object>) null); //$NON-NLS-1$
		eventAdmin.sendEvent(event);
		assertNotNull("Did not receive event published to topic 'a/b/c/d/e' while listening to 'a/b/c/*'", handler.lastEvent()); //$NON-NLS-1$
		handlerRegistration.unregister();
	}

	/*
	 * Ensures EventAdmin delivers an event published on topic "a/b/c/d/e/f" to 
	 * an EventHandler listening to topic "a/b/c/*".
	 */
	@Test
	public void testEventDeliveryForWildcardTopic6() {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EventConstants.EVENT_TOPIC, "a/b/c/*"); //$NON-NLS-1$
		BundleContext bundleContext = Activator.getBundleContext();
		EventHandlerHelper handler = new EventHandlerHelper();
		ServiceRegistration<EventHandler> handlerRegistration = bundleContext.registerService(EventHandler.class, handler, properties);
		Event event = new Event("a/b/c/d/e/f", (Dictionary<String, Object>) null); //$NON-NLS-1$
		eventAdmin.sendEvent(event);
		assertNotNull("Did not receive event published to topic 'a/b/c/d/e/f' while listening to 'a/b/c/*'", handler.lastEvent()); //$NON-NLS-1$
		handlerRegistration.unregister();
	}

	/*
	 * Ensures EventAdmin delivers an event published to topics "a/b/c" and 
	 * "a/b/c/d" to an EventHandler listening to topics "a/b/c" and "a/b/c/*".
	 * 
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=325064.
	 */
	@Test
	public void testEventDeliveryForWildcardTopic7() {
		Dictionary<String, Object> properties = new Hashtable<String, Object>();
		properties.put(EventConstants.EVENT_TOPIC, new String[] {"a/b/c", "a/b/c/*"}); //$NON-NLS-1$ //$NON-NLS-2$
		BundleContext bundleContext = Activator.getBundleContext();
		EventHandlerHelper handler = new EventHandlerHelper();
		ServiceRegistration<EventHandler> handlerRegistration = bundleContext.registerService(EventHandler.class, handler, properties);
		Event event = new Event("a/b/c", (Dictionary<String, Object>) null); //$NON-NLS-1$
		eventAdmin.sendEvent(event);
		assertNotNull("Did not receive event published to topic 'a/b/c' while listening to 'a/b/c'", handler.clearLastEvent()); //$NON-NLS-1$
		event = new Event("a/b/c/d", (Dictionary<String, Object>) null); //$NON-NLS-1$
		eventAdmin.sendEvent(event);
		assertNotNull("Did not receive event published to topic 'a/b/c/d' while listening to 'a/b/c/*'", handler.lastEvent()); //$NON-NLS-1$
		handlerRegistration.unregister();
	}
}
