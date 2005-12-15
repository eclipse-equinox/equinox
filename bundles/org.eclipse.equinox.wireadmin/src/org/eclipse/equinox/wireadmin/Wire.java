/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.wireadmin;

import java.util.*;
import org.osgi.framework.*;
import org.osgi.service.wireadmin.*;
import org.osgi.util.tracker.ServiceTracker;

public class Wire implements org.osgi.service.wireadmin.Wire {

	protected boolean valid = false;
	protected boolean connected = false;

	protected ServiceTracker consumerTracker;
	protected ServiceTracker producerTracker;

	protected Producer producer = null;
	protected Consumer consumer = null;

	protected Class[] flavors = null;
	protected Class[] producerFlavors = null;

	protected Object lastValue;
	protected long lastTime = -1;

	protected Dictionary properties;
	protected WireAdmin wireadmin;

	private String pid;
	private String producerPID;
	private String consumerPID;

	private boolean producerFilterExists;
	private Filter wireFilter;

	protected Wire(String pid, String producerPID, String consumerPID, Dictionary props, WireAdmin wireadmin) {
		this.pid = pid;
		this.producerPID = producerPID;
		this.consumerPID = consumerPID;

		this.wireadmin = wireadmin;
		setProperties(props);

		String consumerFilterString = "(&(objectClass=org.osgi.service.wireadmin.Consumer)(service.pid=" + consumerPID + "))";//$NON-NLS-1$ //$NON-NLS-2$
		String producerFilterString = "(&(objectClass=org.osgi.service.wireadmin.Producer)(service.pid=" + producerPID + "))";//$NON-NLS-1$ //$NON-NLS-2$
		Filter consumerFilter = null;
		Filter producerFilter = null;
		wireFilter = (Filter) properties.get(WireConstants.WIREADMIN_FILTER);
		try {
			consumerFilter = wireadmin.context.createFilter(consumerFilterString);
			producerFilter = wireadmin.context.createFilter(producerFilterString);
		} catch (InvalidSyntaxException ex) {
			ex.printStackTrace();//FIXME
		}

		ConsumerCustomizer consumerCustomizer = new ConsumerCustomizer(wireadmin.context, this);
		ProducerCustomizer producerCustomizer = new ProducerCustomizer(wireadmin.context, this);

		consumerTracker = new ServiceTracker(wireadmin.context, consumerFilter, consumerCustomizer);
		producerTracker = new ServiceTracker(wireadmin.context, producerFilter, producerCustomizer);

		valid = true;
	}

	/**
	 * Return the state of this <tt>Wire</tt> object.
	 *
	 * <p>A connected <tt>Wire</tt> must always be disconnected before
	 * becoming invalid.
	 *
	 * @return <tt>false</tt> if this <tt>Wire</tt> is invalid because it
	 * has been deleted via {@link WireAdmin#deleteWire};
	 * <tt>true</tt> otherwise.
	 */
	/**
	 * Return the state of this <tt>Wire</tt> object.
	 *
	 * <p>A connected <tt>Wire</tt> must always be disconnected before
	 * becoming invalid.
	 *
	 * @return <tt>false</tt> if this <tt>Wire</tt> is invalid because it
	 * has been deleted via {@link WireAdmin#deleteWire};
	 * <tt>true</tt> otherwise.
	 */
	public boolean isValid() {
		return (valid);
	}

	/**
	 * Return the connection state of this <tt>Wire</tt> object.
	 *
	 * <p>A <tt>Wire</tt> is connected after the Wire Admin service receives
	 * notification that the <tt>Producer</tt> service and
	 * the <tt>Consumer</tt> service for this <tt>Wire</tt> object are both registered.
	 * This method will return <tt>true</tt> prior to notifying the <tt>Producer</tt>
	 * and <tt>Consumer</tt> services via calls
	 * to their respective <tt>consumersConnected</tt> and <tt>producersConnected</tt>
	 * methods.
	 * <p>A <tt>WireAdminEvent</tt> of type {@link WireAdminEvent#WIRE_CONNECTED}
	 * must be broadcast by the Wire Admin service when
	 * the <tt>Wire</tt> becomes connected.
	 *
	 * <p>A <tt>Wire</tt> object
	 * is disconnected when either the <tt>Consumer</tt> or <tt>Producer</tt>
	 * service is unregistered or the <tt>Wire</tt> object is deleted.
	 * <p>A <tt>WireAdminEvent</tt> of type {@link WireAdminEvent#WIRE_DISCONNECTED}
	 * must be broadcast by the Wire Admin service when
	 * the <tt>Wire</tt> becomes disconnected.
	 *
	 * @return <tt>true</tt> if both the <tt>Producer</tt> and <tt>Consumer</tt>
	 * for this <tt>Wire</tt> object are connected to the <tt>Wire</tt> object;
	 * <tt>false</tt> otherwise.
	 */

	public boolean isConnected() {
		return (connected);
	}

	/**
	 * Return the list of data types understood by the
	 * <tt>Consumer</tt> service connected to this <tt>Wire</tt> object. Note that
	 * subclasses of the classes in this list are allowed as well.
	 *
	 * <p>The list is the value of the {@link Consumer#WIREADMIN_CONSUMER_FLAVORS}
	 * service property of the
	 * <tt>Consumer</tt> service object connected to this object. If no such
	 * property was registered, this method must return null.
	 *
	 * @return An array containing the list of classes understood by the
	 * <tt>Consumer</tt> service or <tt>null</tt> if the <tt>Consumer</tt> object is
	 * not currently a registered service, or the <tt>Wire</tt> has been disconnected,
	 * or the consumer did not register a {@link #WIREADMIN_CONSUMER_FLAVORS} property.
	 */
	public Class[] getFlavors() {
		return (flavors);
	}

	protected void setConsumerProperties(ServiceReference consumerReference) {
		if (consumerReference != null) {
			try {
				flavors = (Class[]) consumerReference.getProperty(WireConstants.WIREADMIN_CONSUMER_FLAVORS);
			} catch (ClassCastException ex) {
				// log error???
			}
		}
	}

	/**
	 * Update the value.
	 *
	 * <p>This methods is called by the <tt>Producer</tt> service to
	 * notify the <tt>Consumer</tt> service connected to this <tt>Wire</tt> object
	 * of an updated value.
	 * <p>If the properties of this <tt>Wire</tt> object contain a
	 * {@link Constants#WIREADMIN_FILTER} property,
	 * then filtering is performed on this <tt>Wire</tt>.
	 * If the <tt>Producer</tt> service connected to this <tt>Wire</tt>
	 * object was registered with the service
	 * property {@link Constants#WIREADMIN_PRODUCER_FILTERS}, the
	 * <tt>Producer</tt> service will perform the filter the value according to the rules specified
	 * for the filter. Otherwise, this <tt>Wire</tt> object
	 * will filter the value according to the rules specified for the filter.
	 * <p>If no filtering is done, or the filter indicates the updated value should
	 * be delivered to the <tt>Consumer</tt> service, then
	 * this <tt>Wire</tt> object must call
	 * the {@link Consumer#updated} method with the updated value.
	 * If this <tt>Wire</tt> object is not connected, then the <tt>Consumer</tt>
	 * service must not be called.
	 *
	 * <p>A <tt>WireAdminEvent</tt> of type {@link WireAdminEvent#WIRE_TRACE}
	 * must be broadcast by the Wire Admin service after
	 * the <tt>Consumer</tt> service has been successfully called.
	 *
	 * @param value The updated value. The value should be an instance of
	 * one of the types returned by {@link #getFlavors}.
	 * @see Constants#WIREADMIN_FILTER
	 */
	//FIXME -- wire filter
	public void update(Object value) {
		wireadmin.checkAlive();
		synchronized (this) {
			int length = flavors.length;
			boolean correctFlavors = false;
			for (int i = 0; i < length; i++) {
				if (flavors[i].isInstance(value)) {
					correctFlavors = true;
					break;
				}
			}
			if (!correctFlavors) {
				return;
			}
			if (consumer != null) {
				//if the producer filter property is set, the producer will do the filtering
				//FIXME - check conditions correctly
				if (producerFilterExists || wireFilter == null) {
					try {
						consumer.updated(this, value);
						lastValue = value;
						//lastTime = time;
						wireadmin.eventProducer.generateEvent(WireAdminEvent.WIRE_TRACE, this, null);
					} catch (Exception ex) {
						wireadmin.eventProducer.generateEvent(WireAdminEvent.CONSUMER_EXCEPTION, this, ex);
					}
				} else {
					long time = Calendar.getInstance().getTime().getTime();
					//get object properties
					Hashtable valueProps = new Hashtable(10);
					valueProps.put(WireConstants.WIREVALUE_CURRENT, value);
					valueProps.put(WireConstants.WIREVALUE_PREVIOUS, lastValue);
					valueProps.put(WireConstants.WIREVALUE_ELAPSED, new Long(time - lastTime)); //???LONG???
					try {
						// value-lastValue;
						if (value instanceof Short) {
							int result = ((Short) value).shortValue() - ((Short) lastValue).shortValue();
							valueProps.put(WireConstants.WIREVALUE_DELTA_RELATIVE, new Integer(result));
							if (result < 0) {
								result = result * -1;
							}
							valueProps.put(WireConstants.WIREVALUE_DELTA_ABSOLUTE, new Integer(result));
						}
						if (value instanceof Integer) {
							int result = ((Integer) value).intValue() - ((Integer) lastValue).intValue();
							valueProps.put(WireConstants.WIREVALUE_DELTA_RELATIVE, new Integer(result));
							if (result < 0) {
								result = result * -1;
							}
							valueProps.put(WireConstants.WIREVALUE_DELTA_ABSOLUTE, new Integer(result));
						}
						if (value instanceof Long) {
							long result = ((Long) value).longValue() - ((Long) lastValue).longValue();
							valueProps.put(WireConstants.WIREVALUE_DELTA_RELATIVE, new Long(result));
							if (result < 0) {
								result = result * -1;
							}
							valueProps.put(WireConstants.WIREVALUE_DELTA_ABSOLUTE, new Long(result));
						}
						if (value instanceof Double) {
							long result = ((Long) value).longValue() - ((Long) lastValue).longValue();
							valueProps.put(WireConstants.WIREVALUE_DELTA_RELATIVE, new Long(result));
							if (result < 0) {
								result = result * -1;
							}
							valueProps.put(WireConstants.WIREVALUE_DELTA_ABSOLUTE, new Long(result));
						}
					} catch (NumberFormatException ex) {
						//we don't have two numeric values so do nothing
					}
				}
			}
		}
	}

	/**
	 * Poll for an updated value.
	 *
	 * <p>This methods is normally called by the <tt>Consumer</tt> service to
	 * request an updated value from the <tt>Producer</tt> service
	 * connected to this <tt>Wire</tt> object.
	 * This <tt>Wire</tt> object will call
	 * the {@link Producer#polled} method to obtain an updated value.
	 * If this <tt>Wire</tt> object is not connected, then the <tt>Producer</tt>
	 * service must not be called.
	 *
	 * <p>A <tt>WireAdminEvent</tt> of type {@link WireAdminEvent#WIRE_TRACE}
	 * must be broadcast by the Wire Admin service after
	 * the <tt>Producer</tt> service has been successfully called.
	 *
	 * @return An updated value whose type should be one of the types
	 * returned by {@link #getFlavors} or <tt>null</tt> if
	 * the <tt>Wire</tt> object is not connected,
	 * the <tt>Producer</tt> service threw an exception, or
	 * the <tt>Producer</tt> service returned a value which is not an instance of
	 * one of the types returned by {@link #getFlavors}.
	 */
	public synchronized Object poll() {
		wireadmin.checkAlive();
		if (producer != null) {
			try {
				synchronized (this) {
					Object value = producer.polled(this);
					wireadmin.eventProducer.generateEvent(WireAdminEvent.WIRE_TRACE, this, null);
					if (checkFlavor(value)) {
						lastValue = value;
						return (value);
					}
					return (null);
				}
			} catch (Exception ex) {
				wireadmin.eventProducer.generateEvent(WireAdminEvent.PRODUCER_EXCEPTION, this, ex);
				return (null);
			}
		}
		return (null);

	}

	/**
	 * Return the last value sent through this <tt>Wire</tt> object.
	 *
	 * <p>The returned value is the most recent, valid value passed to the
	 * {@link #update} method or returned by the {@link #poll} method
	 * of this object. If filtering is applied by the <tt>Wire</tt> object,
	 * this is still the value as set by the <tt>Producer</tt> service.
	 *
	 * @return The last value passed though this <tt>Wire</tt> object
	 * or <tt>null</tt> if no valid values have been passed.
	 */

	public Object getLastValue() {
		return (lastValue);
	}

	/**
	 * Return the wire properties for this <tt>Wire</tt> object.
	 *
	 * @return The properties for this <tt>Wire</tt> object.
	 * The returned <tt>Dictionary</tt> must be read only.
	 */

	public Dictionary getProperties() {
		return (new ReadOnlyDictionary(properties));
	}

	protected void connect() {
		connected = true;
		wireadmin.notifyConsumer(consumer, consumerPID);
		wireadmin.notifyProducer(producer, producerPID);

		//send event
		wireadmin.eventProducer.generateEvent(WireAdminEvent.WIRE_CONNECTED, this, null);
	}

	protected void disconnect() {
		connected = false;
		wireadmin.notifyConsumer(consumer, consumerPID);
		wireadmin.notifyProducer(producer, producerPID);

		//send event
		wireadmin.eventProducer.generateEvent(WireAdminEvent.WIRE_DISCONNECTED, this, null);
	}

	protected void destroy() {
		if (consumer != null) {
			removeConsumer();
		}
		if (producer != null) {
			removeProducer();
		}
		consumerTracker.close();
		producerTracker.close();
		valid = false;
	}

	protected void setConsumer(Consumer consumer, ServiceReference reference) {
		this.consumer = consumer;
		setConsumerProperties(reference);
		if (producer != null) {
			connect();
		}
	}

	protected void setProducer(Producer producer, ServiceReference reference) {
		this.producer = producer;
		setProducerProperties(reference);
		if (consumer != null) {
			connect();
		}
	}

	protected void setProducerProperties(ServiceReference reference) {
		producerFlavors = (Class[]) reference.getProperty(WireConstants.WIREADMIN_PRODUCER_FLAVORS);
		String[] keys = reference.getPropertyKeys();
		//need to find out if WIREADMIN_PRODUCER_FILTERS key exists
		if (reference.getProperty(WireConstants.WIREADMIN_PRODUCER_FILTERS) != null) {
			producerFilterExists = true;
		}
		/* 
		 for(int i=0;i<keys.length;i++)
		 {
		 if(keys[i] == (WireConstants.WIREADMIN_PRODUCER_FILTERS))
		 {	
		 producerFilterExists = true;
		 return;
		 }
		 }  */
	}

	protected void removeProducer() {
		producerFilterExists = false;
		producerFlavors = null;

		if (connected) {
			//The consumer is now "not connected" by this wire
			disconnect();
		}
		producer = null;
	}

	protected void removeConsumer() {

		if (connected) {
			disconnect();
		}
		consumer = null;
		flavors = null;
	}

	protected String getPid() {
		return pid;
	}

	private boolean checkFlavor(Object value) {
		if (flavors == null) {
			return (true);
		}
		for (int i = 0; i < flavors.length; i++) {
			if (value.getClass().isInstance(flavors[i])) {
				return (true);
			}
		}
		return (false);
	}

	/**
	 * Return the calculated scope of this <tt>Wire</tt> object.
	 *
	 * The purpose of the <tt>Wire</tt> object's scope is to allow a Producer
	 * and/or Consumer service to produce/consume different types
	 * over a single <tt>Wire</tt> object (this was deemed necessary for efficiency
	 * reasons). Both the Consumer service and the
	 * Producer service must set an array of scope names (their scope) with
	 * the service registration property <tt>WIREADMIN_PRODUCER_SCOPE</tt>, or <tt>WIREADMIN_CONSUMER_SCOPE</tt> when they can
	 * produce multiple types. If a Producer service can produce different types, it should set this property
	 * to the array of scope names it can produce, the Consumer service
	 * must set the array of scope names it can consume. The scope of a <tt>Wire</tt>
	 * object is defined as the intersection of permitted scope names of the
	 * Producer service and Consumer service.
	 * <p>If neither the Consumer, or the Producer service registers scope names with its
	 * service registration, then the <tt>Wire</tt> object's scope must be <tt>null</tt>.
	 * <p>The <tt>Wire</tt> object's scope must not change when a Producer or Consumer services
	 * modifies its scope.
	 * <p>A scope name is permitted for a Producer service when the registering bundle has
	 * <tt>WirePermission[PRODUCE]</tt>, and for a Consumer service when
	 * the registering bundle has <tt>WirePermission[CONSUME]</tt>.<p>
	 * If either Consumer service or Producer service has not set a <tt>WIREADMIN_*_SCOPE</tt> property, then
	 * the returned value must be <tt>null</tt>.<p>
	 * If the scope is set, the <tt>Wire</tt> object must enforce the scope names when <tt>Envelope</tt> objects are
	 * used as a parameter to update or returned from the <tt>poll</tt> method. The <tt>Wire</tt> object must then
	 * remove all <tt>Envelope</tt> objects with a scope name that is not permitted.
	 *
	 * @return A list of permitted scope names or null if the Produce or Consumer service has set no scope names.
	 */
	public String[] getScope() {
		return null;
	}

	/**
	 * Return true if the given name is in this <tt>Wire</tt> object's scope.
	 *
	 * @param name The scope name
	 * @return true if the name is listed in the permitted scope names
	 */
	public boolean hasScope(String name) {
		return false;
	}

	protected void setProperties(Dictionary newprops) {
		newprops.put(WireConstants.WIREADMIN_PID, pid);
		newprops.put(WireConstants.WIREADMIN_PRODUCER_PID, producerPID);
		newprops.put(WireConstants.WIREADMIN_CONSUMER_PID, consumerPID);
		properties = newprops;
	}

	protected void init() {
		consumerTracker.open();
		producerTracker.open();
	}

}
