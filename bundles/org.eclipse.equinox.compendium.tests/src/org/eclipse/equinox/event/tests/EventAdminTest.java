/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.event.tests;

import java.util.Dictionary;
import java.util.Hashtable;
import junit.framework.TestCase;
import org.eclipse.equinox.compendium.tests.Activator;
import org.osgi.framework.*;
import org.osgi.service.event.*;

public class EventAdminTest extends TestCase {
	private EventAdmin eventAdmin;
	private ServiceReference eventAdminReference;

	protected void setUp() throws Exception {
		Activator.getBundle(Activator.BUNDLE_EVENT).start();
		eventAdminReference = Activator.getBundleContext().getServiceReference(EventAdmin.class.getName());
		eventAdmin = (EventAdmin) Activator.getBundleContext().getService(eventAdminReference);
	}

	protected void tearDown() throws Exception {
		Activator.getBundleContext().ungetService(eventAdminReference);
		Activator.getBundle(Activator.BUNDLE_EVENT).stop();
	}

	/*
	 * Ensures EventAdmin does not deliver an event published on topic "a/b/c" 
	 * to an EventHandler listening to topic a/b/c/*.
	 * 
	 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=325064.
	 */
	public void testEventDeliveryForWildcardTopic1() {
		Dictionary properties = new Hashtable();
		properties.put(EventConstants.EVENT_TOPIC, "a/b/c/*"); //$NON-NLS-1$
		BundleContext bundleContext = Activator.getBundleContext();
		EventHandlerHelper handler = new EventHandlerHelper();
		ServiceRegistration handlerRegistration = bundleContext.registerService(EventHandler.class, handler, properties);
		Event event = new Event("a/b/c", (Dictionary) null); //$NON-NLS-1$
		eventAdmin.sendEvent(event);
		assertNull("Received event published to topic 'a/b/c' while listening to 'a/b/c/*'", handler.lastEvent()); //$NON-NLS-1$
		handlerRegistration.unregister();
	}

	/*
	 * Ensures EventAdmin does not deliver an event published on topic "a/b" to 
	 * an EventHandler listening to topic a/b/c/*.
	 */
	public void testEventDeliveryForWildcardTopic2() {
		Dictionary properties = new Hashtable();
		properties.put(EventConstants.EVENT_TOPIC, "a/b/c/*"); //$NON-NLS-1$
		BundleContext bundleContext = Activator.getBundleContext();
		EventHandlerHelper handler = new EventHandlerHelper();
		ServiceRegistration handlerRegistration = bundleContext.registerService(EventHandler.class, handler, properties);
		Event event = new Event("a/b", (Dictionary) null); //$NON-NLS-1$
		eventAdmin.sendEvent(event);
		assertNull("Received event published to topic 'a/b' while listening to 'a/b/c/*'", handler.lastEvent()); //$NON-NLS-1$
		handlerRegistration.unregister();
	}

	/*
	 * Ensures EventAdmin does not deliver an event published on topic "a" to 
	 * an EventHandler listening to topic a/b/c/*.
	 */
	public void testEventDeliveryForWildcardTopic3() {
		Dictionary properties = new Hashtable();
		properties.put(EventConstants.EVENT_TOPIC, "a/b/c/*"); //$NON-NLS-1$
		BundleContext bundleContext = Activator.getBundleContext();
		EventHandlerHelper handler = new EventHandlerHelper();
		ServiceRegistration handlerRegistration = bundleContext.registerService(EventHandler.class, handler, properties);
		Event event = new Event("a", (Dictionary) null); //$NON-NLS-1$
		eventAdmin.sendEvent(event);
		assertNull("Received event published to topic 'a' while listening to 'a/b/c/*'", handler.lastEvent()); //$NON-NLS-1$
		handlerRegistration.unregister();
	}

	/*
	 * Ensures EventAdmin delivers an event published on topic "a/b/c/d" to an 
	 * EventHandler listening to topic "a/b/c/*".
	 */
	public void testEventDeliveryForWildcardTopic4() {
		Dictionary properties = new Hashtable();
		properties.put(EventConstants.EVENT_TOPIC, "a/b/c/*"); //$NON-NLS-1$
		BundleContext bundleContext = Activator.getBundleContext();
		EventHandlerHelper handler = new EventHandlerHelper();
		ServiceRegistration handlerRegistration = bundleContext.registerService(EventHandler.class, handler, properties);
		Event event = new Event("a/b/c/d", (Dictionary) null); //$NON-NLS-1$
		eventAdmin.sendEvent(event);
		assertNotNull("Did not receive event published to topic 'a/b/c/d' while listening to 'a/b/c/*'", handler.lastEvent()); //$NON-NLS-1$
		handlerRegistration.unregister();
	}

	/*
	 * Ensures EventAdmin delivers an event published on topic "a/b/c/d/e" to 
	 * an EventHandler listening to topic "a/b/c/*".
	 */
	public void testEventDeliveryForWildcardTopic5() {
		Dictionary properties = new Hashtable();
		properties.put(EventConstants.EVENT_TOPIC, "a/b/c/*"); //$NON-NLS-1$
		BundleContext bundleContext = Activator.getBundleContext();
		EventHandlerHelper handler = new EventHandlerHelper();
		ServiceRegistration handlerRegistration = bundleContext.registerService(EventHandler.class, handler, properties);
		Event event = new Event("a/b/c/d/e", (Dictionary) null); //$NON-NLS-1$
		eventAdmin.sendEvent(event);
		assertNotNull("Did not receive event published to topic 'a/b/c/d/e' while listening to 'a/b/c/*'", handler.lastEvent()); //$NON-NLS-1$
		handlerRegistration.unregister();
	}

	/*
	 * Ensures EventAdmin delivers an event published on topic "a/b/c/d/e/f" to 
	 * an EventHandler listening to topic "a/b/c/*".
	 */
	public void testEventDeliveryForWildcardTopic6() {
		Dictionary properties = new Hashtable();
		properties.put(EventConstants.EVENT_TOPIC, "a/b/c/*"); //$NON-NLS-1$
		BundleContext bundleContext = Activator.getBundleContext();
		EventHandlerHelper handler = new EventHandlerHelper();
		ServiceRegistration handlerRegistration = bundleContext.registerService(EventHandler.class, handler, properties);
		Event event = new Event("a/b/c/d/e/f", (Dictionary) null); //$NON-NLS-1$
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
	public void testEventDeliveryForWildcardTopic7() {
		Dictionary properties = new Hashtable();
		properties.put(EventConstants.EVENT_TOPIC, new String[] {"a/b/c", "a/b/c/*"}); //$NON-NLS-1$ //$NON-NLS-2$
		BundleContext bundleContext = Activator.getBundleContext();
		EventHandlerHelper handler = new EventHandlerHelper();
		ServiceRegistration handlerRegistration = bundleContext.registerService(EventHandler.class, handler, properties);
		Event event = new Event("a/b/c", (Dictionary) null); //$NON-NLS-1$
		eventAdmin.sendEvent(event);
		assertNotNull("Did not receive event published to topic 'a/b/c' while listening to 'a/b/c'", handler.clearLastEvent()); //$NON-NLS-1$
		event = new Event("a/b/c/d", (Dictionary) null); //$NON-NLS-1$
		eventAdmin.sendEvent(event);
		assertNotNull("Did not receive event published to topic 'a/b/c/d' while listening to 'a/b/c/*'", handler.lastEvent()); //$NON-NLS-1$
		handlerRegistration.unregister();
	}
}
