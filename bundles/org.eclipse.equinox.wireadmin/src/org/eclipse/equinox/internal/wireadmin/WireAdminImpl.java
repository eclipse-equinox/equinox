/*******************************************************************************
 * Copyright (c) 1997, 2009 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.wireadmin;

import java.io.IOException;
import java.util.*;
import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.wireadmin.*;

/**
 * Wire Admin service implementation
 * 
 * @author Pavlin Dobrev
 * @author Stoyan Boshev
 * @version 1.0
 */
class WireAdminImpl implements WireAdmin, ManagedServiceFactory, ServiceListener {

	private EventDispatcher evtDisp;

	private static final String PID_PREFIX = "WA_GENERATED_PID_";
	private static final String FACTORY_PID = "equinox.wireadmin.fpid";

	private BundleContext bc;

	ConfigurationAdmin cm;

	private Hashtable wires; // maps wire pid to the WireImpl object
	private ServiceRegistration regWireAdmin;
	private ServiceRegistration regManagedFactory;

	private Vector waitForUpdate = new Vector();

	long counter = System.currentTimeMillis();

	/**
	 * Constructs an <code>WireAdminImpl</code> object, which provides
	 * Framework with methods for manipulating a <code>Wire</code> objects.
	 * Initialazed are hashtables for storing created wires, wires per
	 * <code>Producer</code>, and wires per <code>Consumer</code>.
	 * 
	 * @param bc
	 *            is the BundleContext object used for interaction with
	 *            Framework.
	 */

	WireAdminImpl(BundleContext bc, ConfigurationAdmin cm) {
		this.bc = bc;
		this.cm = cm;
		wires = new Hashtable();
		evtDisp = new EventDispatcher(bc, this);

		Hashtable props = new Hashtable(2, 1.0f);
		props.put(Constants.SERVICE_PID, FACTORY_PID);
		props.put("service.factoryPid", FACTORY_PID);

		if (cm != null) {
			try {
				Configuration[] all = cm.listConfigurations("(service.factoryPid=" + FACTORY_PID + ")");
				if (all != null) {
					WireImpl wire;

					for (int i = 0; i < all.length; i++) {
						Dictionary properties = all[i].getProperties();
						String pid = (String) properties.get(WireConstants.WIREADMIN_PID);

						if (pid == null) {
							pid = all[i].getPid();
							properties.put(WireConstants.WIREADMIN_PID, pid);
						}

						wire = new WireImpl(bc, this, properties);
						wires.put(pid, wire);
						wire.start();
					}
				}
			} catch (IOException ioe) {
				/* blocking won't be made */
			} catch (InvalidSyntaxException ise) {
				/* syntax is valid */
			}
		}

		// register as ManagedServiceFactory after loading the current config
		// values and creating the wires !!!!!!!!
		// This will not lead to bugs because of updating wire props while wire
		// is not preloaded
		regManagedFactory = bc.registerService(ManagedServiceFactory.class.getName(), this, props);
		regWireAdmin = bc.registerService(WireAdmin.class.getName(), this, props);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.wireadmin.WireAdmin#createWire(java.lang.String,
	 *      java.lang.String, java.util.Dictionary)
	 */
	public Wire createWire(String producerPID, String consumerPID, Dictionary props) {
		return createWire(producerPID, consumerPID, props, null);
	}

	private Wire createWire(String producerPID, String consumerPID, Dictionary props, String pid) {
		if (Activator.LOG_DEBUG) {
			Activator.log.debug(0, 10004, producerPID + " / " + consumerPID + " / =" + props, null, false);
		}

		if ((pid == null) && (producerPID == null || consumerPID == null)) {
			throw new IllegalArgumentException("PIDs can not be null");
		}

		if (props == null) {
			props = new Hashtable(7, 1.0f);
		} else if (caseVariants(props)) {
			throw new IllegalArgumentException("Illegal wire properties. Two or more keys with the same value, or incorrect key type!");
		}

		if (pid == null) {
			// put the keys
			props.put(WireConstants.WIREADMIN_PRODUCER_PID, producerPID);
			props.put(WireConstants.WIREADMIN_CONSUMER_PID, consumerPID);
		}
		WireImpl wire = null;

		if (pid != null) {
			wire = new WireImpl(bc, this, props);
			wires.put(pid, wire);
			wire.start();
		} else if (cm != null) {
			try {
				Configuration config = cm.createFactoryConfiguration(FACTORY_PID);
				props.put(WireConstants.WIREADMIN_PID, config.getPid());
				wire = new WireImpl(bc, this, props);
				/* Object oldWire = */wires.put(config.getPid(), wire);
				// if (oldWire != null) {
				// System.out.println("\n\n@@@@@@@@@@@@@@@@@@ Old wire lost!!!!
				// Wire is "+oldWire);
				// System.out.println("@@@@@@@@@@@@@@@@@@ New Wire is "+wire);
				// }
				wire.start();
				waitForUpdate.addElement(wire);
				config.update(props);
			} catch (IOException ioe) {
				if (Activator.LOG_DEBUG) {
					Activator.log.debug(0, 10005, null, ioe, false);
				}
			}
		} else {
			String wirePID = getNextPID();
			props.put(WireConstants.WIREADMIN_PID, wirePID);
			props.put("service.factoryPid", "equinox.wireadmin.fpid");
			wire = new WireImpl(bc, this, props);
			wires.put(wirePID, wire);
			wire.start();

			Activator.log.info(Activator.PREFIX + "CM not available! The created wire from Producer=" + producerPID + " and Consumer=" + consumerPID + " won't be presistently stored!");

		}

		notifyListeners(wire, WireAdminEvent.WIRE_CREATED, null);
		return wire;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.wireadmin.WireAdmin#updateWire(org.osgi.service.wireadmin.Wire,
	 *      java.util.Dictionary)
	 */
	public void updateWire(Wire wire, Dictionary properties) {
		WireImpl wireImpl = (WireImpl) wire;

		if (wireImpl == null || !wireImpl.isValid) {// fix #1064
			return;
		}

		if (properties == null) {
			properties = new Hashtable(7, 1.0f);
		}

		for (Enumeration en = properties.keys(); en.hasMoreElements();) {
			if (!(en.nextElement() instanceof String)) {
				throw new IllegalArgumentException("Illegal keys, must be String type");
			}
		}

		if (caseVariants(properties)) {
			if (Activator.LOG_DEBUG) {
				Activator.log.debug(0, 10006, null, null, false);
			}
			throw new IllegalArgumentException("Found case variants in properties' keys");
		}

		String wirePID = (String) wire.getProperties().get(WireConstants.WIREADMIN_PID);

		if ((cm != null) && (wirePID.charAt(0) != 'W')) {
			// CM is available and this wire was not created
			// when the CM was not available

			wireImpl.setProperties(properties);
			wires.put(wirePID, wire);
			waitForUpdate.addElement(wire);

			try {
				Configuration conf = cm.getConfiguration(wirePID);
				conf.update(properties);
			} catch (IOException ioe) {
				Activator.log.error(Activator.PREFIX + "I/O error updating configuration!", ioe);
			}

		} else {
			// either CM is not available, or
			// this wire was created when CM was not available
			wires.remove(wirePID);
			wireImpl.setProperties(properties);
			wires.put(wirePID, wire);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.wireadmin.WireAdmin#deleteWire(org.osgi.service.wireadmin.Wire)
	 */
	public void deleteWire(Wire wire) {
		WireImpl wireImpl = (WireImpl) wire;
		try {
			if (cm != null) {
				Configuration current = cm.getConfiguration(wireImpl.getWirePID());
				if (current != null) {
					current.delete();
				}
			} else {
				disconnectWire(wireImpl);
			}
		} catch (IOException ioe) {
			Activator.log.error(Activator.PREFIX + "I/O error getting a configuration!", ioe);
		}
	}

	private Wire[] getWires(String pid, boolean isProducer) throws InvalidSyntaxException {
		if (isProducer) {
			return getWires('(' + WireConstants.WIREADMIN_PRODUCER_PID + '=' + WireImpl.escapeSpecialCharacters(pid) + ')');
		}

		return getWires('(' + WireConstants.WIREADMIN_CONSUMER_PID + '=' + WireImpl.escapeSpecialCharacters(pid) + ')');
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.wireadmin.WireAdmin#getWires(java.lang.String)
	 */
	public Wire[] getWires(String filterString) throws InvalidSyntaxException {
		if (filterString == null) {
			return getAllWires();
		}

		Filter filter = bc.createFilter(filterString);

		if (filter == null) {
			return getAllWires();
		}

		Vector tmp = new Vector();

		synchronized (wires) {
			for (Enumeration en = wires.elements(); en.hasMoreElements();) {
				Wire wire = (Wire) en.nextElement();
				Dictionary wireProps = wire.getProperties();

				if (wire.isValid() && filter.match(wireProps)) {
					tmp.addElement(wire);
				}
			}
		}

		if (tmp.size() == 0) {
			return null;
		}

		Wire[] allWires = new Wire[tmp.size()];
		tmp.copyInto(allWires);
		return allWires;
	}

	private Wire[] getAllWires() {
		if (wires.isEmpty()) {
			return null;
		}

		Vector tmp = new Vector();

		synchronized (wires) {
			for (Enumeration en = wires.elements(); en.hasMoreElements();) {
				Wire wire = (Wire) en.nextElement();
				if (wire.isValid()) {
					tmp.addElement(wire);
				}
			}
		}

		if (tmp.size() == 0) {
			return null;
		}

		Wire[] allWires = new Wire[tmp.size()];
		tmp.copyInto(allWires);
		return allWires;
	}

	private void disconnectWire(WireImpl wire) {
		if (wires == null) {
			return;
		}
		Object result = wires.remove(wire.getWirePID());// Property(WireConstants.WIREADMIN_PID));
		if (result != null) { // the wire existed
			boolean wasConnected = wire.isConnected();
			wire.stop();
			if (wasConnected) {
				notifyListeners(wire, WireAdminEvent.WIRE_DISCONNECTED, null);
			}
			notifyListeners(wire, WireAdminEvent.WIRE_DELETED, null);
		}
	}

	Wire[] getConnected(String key, String value) {
		if (key == null || value == null || wires.isEmpty()) {
			return null;
		}

		Vector connected = new Vector();

		synchronized (wires) {
			for (Enumeration en = wires.elements(); en.hasMoreElements();) {
				WireImpl w = (WireImpl) en.nextElement();
				if (w.isValid() && w.isConnected() && value.equals(w.getProperties().get(key))) {
					connected.addElement(w);
				}
			}
		}

		if (connected.isEmpty()) {
			return null;
		}

		Wire[] cw = new Wire[connected.size()];
		connected.copyInto(cw);
		return cw;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.cm.ManagedServiceFactory#deleted(java.lang.String)
	 */
	public void deleted(String pid) {
		if (Activator.LOG_DEBUG) {
			Activator.log.debug(0, 10007, pid, null, false);
		}

		if (wires == null) {
			return;
		}
		WireImpl wire = (WireImpl) wires.get(pid);
		disconnectWire(wire);
		if (Activator.LOG_DEBUG) {
			Activator.log.debug(0, 10008, null, null, false);
		}
	}

	public String getName() {
		return "WireAdmin Configuration Factory";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.cm.ManagedServiceFactory#updated(java.lang.String,
	 *      java.util.Dictionary)
	 */
	public void updated(String pid, Dictionary properties) throws ConfigurationException {
		if (Activator.LOG_DEBUG) {
			Activator.log.debug(0, 10009, pid, null, false);
			Activator.log.debug(Activator.PREFIX + " " + properties, null);
		}

		String consumerPID = (String) properties.get(WireConstants.WIREADMIN_CONSUMER_PID);

		if (consumerPID == null) {
			throw new ConfigurationException(WireConstants.WIREADMIN_CONSUMER_PID, "is not provided");
		}

		String producerPID = (String) properties.get(WireConstants.WIREADMIN_PRODUCER_PID);

		if (producerPID == null) {
			throw new ConfigurationException(WireConstants.WIREADMIN_PRODUCER_PID, "is not provided");
		}

		properties.put(WireConstants.WIREADMIN_PID, pid);

		if (wires == null) {
			return;
		}

		WireImpl wire = (WireImpl) wires.get(pid);

		if (wire != null) {
			if (waitForUpdate.contains(wire)) {
				// skip the first update after the wire was created
				// or after the wire properties were updated
				waitForUpdate.removeElement(wire);
			} else {
				// updating properties
				if (Activator.LOG_DEBUG) {
					Activator.log.debug(0, 10011, wire.toString(), null, false);
				}
				wire.setProperties(properties);
			}
		} else {
			createWire(producerPID, consumerPID, properties, pid);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
	 */
	public void serviceChanged(ServiceEvent event) {
		int type = event.getType();
		ServiceReference ref = event.getServiceReference();

		if (type == ServiceEvent.UNREGISTERING) {
			// presume that the unregistered service is a WireAdminListener
			// service
			evtDisp.removeListener(ref);
			return;
		}

		Object service = bc.getService(ref);

		if (type == ServiceEvent.REGISTERED) {
			if (service instanceof WireAdminListener) {
				evtDisp.addListener(ref, bc.getService(ref));
			}

			if (service instanceof Producer) {
				Wire[] wires = null;
				try {
					wires = getWires((String) ref.getProperty(Constants.SERVICE_PID), true);
				} catch (InvalidSyntaxException ise) {
					/* syntax is valid */
				}
				boolean doNotify = true;
				if (wires != null) {
					// there are wires, which may become connected
					for (int i = 0; i < wires.length; i++) {
						if (((WireImpl) wires[i]).consumerRef != null) {
							// there is wire which will become (or is) connected
							doNotify = false;
						}
					}
				}
				if (doNotify) {
					notifyConsumerProducer(new NotificationEvent((Producer) service, null, null, null));
				}
			}

			if (service instanceof Consumer) {
				Wire[] wires = null;
				try {
					wires = getWires((String) ref.getProperty(Constants.SERVICE_PID), false);
				} catch (InvalidSyntaxException ise) {
					/* syntax is valid */
				}

				boolean doNotify = true;
				if (wires != null) {
					// there are wires, which may become connected
					for (int i = 0; i < wires.length; i++) {
						if (((WireImpl) wires[i]).producerRef != null) {
							// there is wire which will become (or is) connected
							doNotify = false;
						}
					}
				}
				if (doNotify) {
					notifyConsumerProducer(new NotificationEvent(null, (Consumer) service, null, null));
				}
			}
		}
	}

	// Utility methods
	/**
	 * Sends a <code>WireAdminEvent</code> to all services registered as
	 * <code>WireAdminListener</code>
	 * 
	 * @param type
	 *            is the type of the <code>WireAdminEvent</code> that must be
	 *            sent.
	 */
	void notifyListeners(Wire src, int type, Throwable t) {
		if (regWireAdmin == null) {
			return;
		}
		evtDisp.addEvent(new WireAdminEvent(regWireAdmin.getReference(), type, src, t));
	}

	void notifyConsumerProducer(NotificationEvent ne) {
		evtDisp.addNotificationEvent(ne);
	}

	void unregister() {

		if (regWireAdmin != null) {
			regWireAdmin.unregister();
			regWireAdmin = null;
		}
		if (regManagedFactory != null) {
			regManagedFactory.unregister();
			regManagedFactory = null;
		}

		// disconnect all running wires
		synchronized (wires) {
			for (Enumeration en = wires.elements(); en.hasMoreElements();) {
				disconnectWire((WireImpl) en.nextElement());
			}
		}

		// stop event dispatcher
		evtDisp.terminate();

		cm = null;
		wires.clear();
		wires = null;
		bc = null;
	}

	private static boolean caseVariants(Dictionary props) {
		int k = 0;
		int size = props.size();
		String[] keys = new String[size];

		try {
			for (Enumeration en = props.keys(); en.hasMoreElements();) {
				keys[k] = (String) en.nextElement();
				k++;
			}
		} catch (ClassCastException cce) {
			return true;
		}
		int j;

		for (int i = 0; i < size; i++) {
			for (j = i + 1; j < size; j++) {
				if (keys[i].equalsIgnoreCase(keys[j])) {
					return true;
				}
			}
		}
		return false;
	}

	private final String getNextPID() {
		String nextPID = PID_PREFIX + counter++;
		while (wires.get(nextPID) != null) {
			nextPID = PID_PREFIX + counter++;
		}
		return nextPID;
	}

	boolean hasAConnectedWire(boolean isProducer, String pid) {
		String cPid;
		synchronized (wires) {
			for (Enumeration en = wires.elements(); en.hasMoreElements();) {
				WireImpl wire = (WireImpl) en.nextElement();

				if (wire.isValid() && wire.isConnected()) {
					cPid = (String) wire.getProperties().get(isProducer ? WireConstants.WIREADMIN_PRODUCER_PID : WireConstants.WIREADMIN_CONSUMER_PID);
					if (cPid.equals(pid)) {
						return true;
					}
				}
			}
		}
		// none of the wires in wich this service takes part
		// will become connected after it's registering
		return false;
	}
}
