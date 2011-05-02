/*******************************************************************************
 * Copyright (c) 1997, 2008 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ip.dscagent;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.*;
import java.util.Dictionary;
import org.eclipse.equinox.internal.ip.ProvisioningInfoProvider;
import org.eclipse.equinox.internal.ip.impl.Log;
import org.eclipse.equinox.internal.ip.impl.ProvisioningAgent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.provisioning.ProvisioningService;

/**
 * Agent who make FW available for UDM multicast discovery. It joins
 * provisioning agent to a MulticastSocket and waits for ping replies. The
 * response contains data that describes gateway. HTTP port is determined using
 * next algorithm: <BR>
 * <OL>
 * <LI>If <I>"equinox.provisioning.gwhttp.port"</I> property is set it is used
 * for HTTP port value.</LI>
 * <LI>Else if there has <I>org.osgi.service.HttpService</I> is registered and
 * the default such service has registration property "openPort", value of this
 * property is assumed as port. </LI>
 * <LI>Else HTTP port is assumed to be "80" (the default HTTP port).</LI>
 * </OL>
 * 
 * @author Avgustin Marinov
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class DiscoveryAgent implements Runnable {

	/**
	 * This property can be used when HTTP service is not a prosyst's
	 * implementation or HTTP is not always, which gives a port with
	 * registration property "openPort", available.
	 */
	public static final String HTTP_PORT = "equinox.provisioning.gwhttp.port";

	/**
	 * This property can be used when HTTPS service is not a prosyst's
	 * implementation, HTTPS is old prosyst's https implementation, or http is
	 * not always, which gives a port with registration property
	 * "secureOpenPort", or HTTPS is not always available.
	 */
	public static final String HTTPS_PORT = "equinox.provisioning.gwhttps.port";

	/**
	 * Packet Timeout
	 */
	public static final String TIMEOUT = "equinox.provisioning.packet.timeout";

	/**
	 * Separator used in string representing gateway. It separates different
	 * parts of that string.
	 */
	public static final char SEPARATOR = '#';

	/**
	 * The String that corresponds to lack of sPID.
	 */
	public static final String NULL = new String(new byte[] {1});

	/** Reference to provisioning agent. */
	private ProvisioningAgent prvAgent;

	/** If discoverer service is active. */
	private boolean active = true;

	// Net staff
	/** The multicast socket. */
	private MulticastSocket mcsocket;
	/** The group. */
	private InetAddress group;

	/**
	 * Bundles context used for determining of HTTP port in ProSyst HTTPS
	 * Service Implementation.
	 */
	private BundleContext bc;

	/**
	 * Constructs instance of Discovery agent.
	 * 
	 * @param address
	 *            address of multicast host.
	 * @param port
	 *            port of multicast host.
	 * @param bc
	 *            bundle context used to be accessed framework.
	 * @param prvAgent
	 *            the provisioning agent
	 * @throws UnknownHostException
	 *             when address cannot be resolved
	 * @throws IOException
	 *             on I/O error, when accessing the given address
	 */
	public DiscoveryAgent(String address, int port, BundleContext bc, ProvisioningAgent prvAgent) throws UnknownHostException, IOException {
		group = InetAddress.getByName(address);
		mcsocket = new MulticastSocket(port);
		this.bc = bc;
		this.prvAgent = prvAgent;
		mcsocket.joinGroup(group);
		mcsocket.setSoTimeout(ProvisioningAgent.getInteger(TIMEOUT, 10000));
		Log.debug("Discovery Agent has joined to multicast socket " + address + ":" + port + ".");
	}

	/**
	 * Until close() method is invoked this method accepts packages broadcasted
	 * to multicast host.
	 */
	public void run() {
		byte[] buffer = new byte[256];
		DatagramPacket request = new DatagramPacket(buffer, buffer.length);
		Log.debug("Discovery Agent starting listening.");
		int errors = 0;
		while (active) {
			try {
				mcsocket.receive(request);
				/* It is ping send by the backend discoverer */
				if ("equinox.provisioning.ping".equals(new String(request.getData(), 0, request.getLength()))) {
					byte[] data = getResponse();
					if (data != null) {
						DatagramPacket response = new DatagramPacket(data, data.length, group, request.getPort());
						/* response.setData(data); */
						mcsocket.send(response);
					}
				}
				request.setLength(buffer.length); /* Restore packet length */
			} catch (InterruptedIOException _) {
			} catch (IOException e) {
				if (errors++ > 5) {
					Log.debug("Seventh unexpected exception. Discoverer will be closed!", e);
					return;
				}
			}
		}
	}

	/**
	 * Closes discoverer agent
	 */
	public void close() {
		try {
			active = false;
			mcsocket.leaveGroup(group);
			mcsocket.close();
		} catch (Exception e) {
			Log.debug(e);
		}
	}

	/**
	 * Encodes some valuable gateways parameters into string. The format is as
	 * follows:<BR>
	 * <I>&lt;spid&gt;#&lt;host&gt;#&lt;http port&gt;#&lt;ready&gt;#&lt;others
	 * info&gt;</I><BR>
	 * where:<BR>
	 * <OL>
	 * <LI><I>sPID</I> is service platform id</LI>
	 * <LI><I>host</I> is service platform host</LI>
	 * <LI><I>HTTP port</I> is service platform HTTP port</LI>
	 * <LI><I>ready</I> is service platform is ready with deploying management
	 * agent bundle</LI>
	 * <LI><I>others info</I> is service platform others info in format:
	 * <I>{&lt;key0&gt;=&lt;value0&gt;,&lt;key1&gt;=&lt;value1&gt;...}</I></LI>
	 * </OL>
	 * 
	 * @return string representation of gateway data as byte array.
	 */
	private byte[] getResponse() {
		String httpPort = ProvisioningAgent.bc.getProperty(HTTP_PORT);
		String httpsPort = ProvisioningAgent.bc.getProperty(HTTPS_PORT);

		// try to determine from service
		if (httpPort == null || httpsPort == null) {
			// Not using HttpService.class.getName(), because we don't need to
			// import
			// it.
			ServiceReference sref = bc.getServiceReference("org.osgi.service.http.HttpService");
			// HTTP Service is available - explore it
			if (httpPort == null)
				httpPort = getPortProperty(sref, "openPort");
			if (httpsPort == null)
				httpsPort = getPortProperty(sref, "secureOpenPort");
		}

		Dictionary info = prvAgent.getInformation();
		if (info == null) {
			return null;
		}

		StringBuffer buff = new StringBuffer();
		String spid = (String) info.get(ProvisioningService.PROVISIONING_SPID);
		buff.append(spid == null || spid.length() == 0 ? NULL : spid);
		buff.append(SEPARATOR);
		String host = null;
		try {
			host = InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			host = "unknown";
		}
		buff.append(host);
		buff.append(SEPARATOR);
		buff.append(httpPort);
		buff.append(SEPARATOR);
		buff.append(httpsPort);
		buff.append(SEPARATOR);
		buff.append(info.get(ProvisioningInfoProvider.MANAGER_URL) != null);
		buff.append(SEPARATOR);
		buff.append('{');
		buff.append(ProvisioningService.PROVISIONING_REFERENCE);
		buff.append('=');
		buff.append(info.get(ProvisioningService.PROVISIONING_REFERENCE));
		buff.append(',');
		buff.append(ProvisioningService.PROVISIONING_START_BUNDLE);
		buff.append('=');
		buff.append(info.get(ProvisioningService.PROVISIONING_START_BUNDLE));
		buff.append('}');
		buff.append(SEPARATOR);
		buff.append(getFlag());
		Log.debug("Discoverer agent sends gw info : " + buff);
		return buff.toString().getBytes();
	}

	public int getFlag() {
		int flag = 0;
		if (prvAgent.getHttpAllowed()) {
			try {
				new URL("http://").openConnection();
				flag |= 0x01;
			} catch (Exception e) {
			}
		}

		try {
			new URL("rsh://").openConnection();
			flag |= 0x02;
		} catch (Exception e) {
		}

		try {
			new URL("https://").openConnection();
			flag |= 0x04;
		} catch (Exception e) {
		}

		return flag;
	}

	private static final String getPortProperty(ServiceReference ref, String property) {
		Object ret = ref != null ? ref.getProperty(property) : null;
		return ret == null ? "-1" : "" + ret;
	}

}
