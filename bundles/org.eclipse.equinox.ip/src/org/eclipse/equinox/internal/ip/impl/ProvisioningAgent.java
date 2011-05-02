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
package org.eclipse.equinox.internal.ip.impl;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.eclipse.equinox.internal.ip.ProvisioningInfoProvider;
import org.eclipse.equinox.internal.ip.ProvisioningStorage;
import org.eclipse.equinox.internal.util.timer.Timer;
import org.eclipse.equinox.internal.util.timer.TimerListener;
import org.osgi.framework.*;
import org.osgi.service.provisioning.ProvisioningService;

/**
 * Class implementing provisioning management based on OSGi RFC 27. On start
 * this <I>BundleActivator</I> gets its manifest headers
 * <I>ProvisioningAgent.providerS</I> and <I>ProvisioningAgent.STORAGE</I>.
 * Using them it decides which <I>ProvisioningInfoProvider</I>-s and which
 * <I>ProvisioningStorage</I> are packed in the bundle. It make instances them.
 * To be succesfull instantiation they MUST have constructor without parameters.
 * All packed in bundle providers that implement <I>BundleActivator</I>. Their
 * methods <I>start(BundleContext)</I> are invoked after instantiation. (On
 * stop their <I>stop(BundleContext)</I>-s are invoked). ProvisioningStorage is
 * used for saving provisioning service data. If no such storage thatas are lost
 * on restart of bundle. Any fail in provider/storage instantiation, providers
 * casting to <I>BundleActivator</I> ends with exception in bundle start and
 * bundle will not reach state ACTIVE.
 * 
 * Also it starts DiscoveryAgent which is prosyst's feature. It is constructed
 * using values of system properties: "equinox.multicast.host" as host and
 * "equinox.multicast.port" as port. If one of these properties is null no
 * instance of DiscoveryAgent is made.
 * 
 * @author Avgustin Marinov
 * @author Pavlin Dobrev
 * @version 1.0
 */

public class ProvisioningAgent implements BundleActivator, ProvisioningService, ServiceListener, FrameworkListener, TimerListener {

	/**
	 * This manifest header determines the packed into the provisioning agent
	 * bundle storage (if such exists) that is to be setarted.
	 */
	public final static String STORAGE = "Prv-Storage";

	/**
	 * This manifest header determines URL Handlers packed into the provisioning
	 * agent bundle that are to be started.
	 */
	public final static String URL_HANDLERS = "Url-Handlers";

	/** This system property that determines multicast host for discoverer agent. */
	public final static String MULTICAST_HOST = "equinox.provisioning.multicast.host";

	/** This system property taht determines multicast port for discoverer agent. */
	public final static String MULTICAST_PORT = "equinox.provisioning.multicast.port";

	/**
	 * This system property determines if provisioning can use HTTP transport
	 * (assumed as unsecured) for provisioning data assignments.
	 */
	public final static String HTTP_ALLOWED = "equinox.provisioning.httpprv.allowed";

	/**
	 * This system property determines if provisioning must waits until the
	 * framework is started.
	 */
	public final static String WAIT_FW_START = "equinox.provisioning.prv.fwstart";

	/**
	 * This system property determines if provisioning should try to make
	 * provisioning on every start
	 */
	public final static String REPROVISIONING_ON_START = "equinox.provisioning.reprovision.onstart";

	/**
	 * Close Zip after reading.
	 */
	public final static String CLOSE_ZIP = "equinox.provisioning.close.zip";

	/**
	 * This system property determines if provisioning agent should print debug
	 * and error information on the console.
	 */
	public final static String DEBUG = "equinox.provisioning.debug";

	/**
	 * This system property determines if provisioning agent should print debug
	 * and error information on the console.
	 */
	public final static String REMOTE_DEBUG = "equinox.provisioning.remote.debug";

	/** BundleContext used for interactions with framework. */
	public static BundleContext bc;

	/** Provisioning data. */
	private ProvisioningData info;
	/** Registration of ProvisioningService. */
	private ServiceRegistration sreg;
	/**
	 * Configuration store used for data storing. It is null after start if no
	 * storage packed in bundle.
	 */
	private ProvisioningStorage storage;

	/** If HTTP protocol is allows for provisioning data assignments. */
	private boolean httpAllowed;

	/**
	 * If storage is inner packed but it is not inner provider, and is
	 * BundleActivator and its start method has been invoked, it must be
	 * stopped.
	 */
	private boolean destroyStorageOnStop = false;

	/**
	 * Contains providers packed in bundle. It can be null after start if no
	 * providers packed in bundle.
	 */
	private Vector providers;

	/**
	 * Contains URL handlers packed in bundle. It can be null after start if no
	 * providers packed in bundle.
	 */
	private Vector urlHandlers;

	/**
	 * DiscoveryAgent instantiated by this object on start(BundleContext). It is
	 * null after start id host or port arenot set in manifest.
	 */
	private Runnable da;

	/** If there is need to wait for start of framework. */
	private boolean wfs;
	/** If the information is added and provistioning assignements are allowed. */
	private boolean active;
	/**
	 * Flag if the start data is processed from frameworkEvent or from
	 * start(BundleContext) method.
	 */
	private boolean startProcessed;
	/** If to do reprovisiong */
	private boolean reprovision;
	/**
	 * If the service is already registered. Used to be avided duplicated
	 * registration on start.
	 */
	private boolean registered;
	/** If to close zip after reading */
	private boolean closeZip;

	// =================================================================================//
	private static final int PROVISIONING = 1;

	private static final String HAS_FAILED_PROVISIONG = "!@#$_hasFailedPrv";

	private boolean reAfterPrvFailureDisabled;
	private int a;
	private int b;
	private int changePeriod;
	private int maxPeriod;

	private Timer timer;
	private long nextProvisioningAfter;
	private int times;
	// =================================================================================//

	public final static int ERROR_UNKNOWN = 0;
	public final static int ERROR_LOAD_STORE_DATA = 1;
	public final static int ERROR_MALFORMED_URL = 2;
	public final static int ERROR_IO_EXCEPTION = 3;
	public final static int ERROR_CORRUPTED_ZIP = 4;

	/**
	 * Invoked by framework on bundle start.
	 * 
	 * @param bc
	 *            bundle context
	 * @exception java.lang.Exception
	 *                mostly when manifest dont match to packed
	 *                providers/storage or their implementation do not match
	 *                expectations.
	 */
	public void start(BundleContext bc) throws Exception {
		ProvisioningAgent.bc = bc;
		active = false;
		startProcessed = false;
		wfs = true;
		if (bc.getProperty(WAIT_FW_START) != null)
			if (bc.getProperty(WAIT_FW_START).equals("false"))
				wfs = false;
		httpAllowed = true;
		if (bc.getProperty(HTTP_ALLOWED) != null)
			if (bc.getProperty(HTTP_ALLOWED).equals("false"))
				httpAllowed = false;
		reprovision = getBoolean(ProvisioningAgent.REPROVISIONING_ON_START);
		closeZip = getBoolean(CLOSE_ZIP);

		// =================================================================================//
		reAfterPrvFailureDisabled = getBoolean("equinox.provisioning.provisioning.reAfterPrvFailure.disabled");
		a = getInteger("equinox.provisioning.provisioning.reAfterPrvFailure.a", 60000);
		b = getInteger("equinox.provisioning.provisioning.reAfterPrvFailure.b", 60000);
		changePeriod = getInteger("equinox.provisioning.provisioning.reAfterPrvFailure.changePeriod", 300000);
		maxPeriod = getInteger("equinox.provisioning.provisioning.reAfterPrvFailure.maxperiod", 3600000);
		nextProvisioningAfter = a;
		// =================================================================================//
		Log.j9workAround = getBoolean("equinox.provisioning.j9.2.0.workaround");
		Log.debug = getBoolean(DEBUG);
		Log.remoteDebug = getBoolean(REMOTE_DEBUG);
		Log.sendTrace = getBoolean("equinox.provisioning.send.trace");
		Log.prvSrv = this;

		org.eclipse.equinox.internal.util.ref.Log log = new org.eclipse.equinox.internal.util.ref.Log(bc);
		log.setDebug(true); // always log to LogService!
		log.setPrintOnConsole(Log.debug);
		Log.log = log;

		try {
			start0(bc);
		} catch (Exception exc) {
			Log.log.close();
			Log.log = null;
			throw exc;
		}
	}

	private void start0(BundleContext bc) throws Exception {
		Log.debug("Starting provisioning agent ...");

		Bundle thisBundle = bc.getBundle();

		// Starts URL handlers packed into the provisioning bundle
		String urlHandlersHeader = (String) thisBundle.getHeaders().get(URL_HANDLERS);
		if (urlHandlersHeader != null) {
			StringTokenizer strTok = new StringTokenizer(urlHandlersHeader, ", ");
			urlHandlers = new Vector(strTok.countTokens());
			while (strTok.hasMoreTokens()) {
				try {
					BundleActivator handler = (BundleActivator) Class.forName(strTok.nextToken().trim()).newInstance();
					handler.start(bc);
					urlHandlers.addElement(handler);
				} catch (Exception e) {
					Log.debug("Can't instantiate or start a handler!");
					throw e;
				}
			}
			urlHandlersHeader = null;
		}

		// Gets inner storage activator class name (if inner storage exists).
		String storageName = (String) thisBundle.getHeaders().get(STORAGE);
		if (storageName != null) {
			storageName.trim();
		}

		// Registers configuration providers packed into the bundle
		String providersHeader = (String) thisBundle.getHeaders().get(ProvisioningInfoProvider.PROVIDERS);
		if (providersHeader != null) {
			providers = new Vector(5);
			StringTokenizer strTok = new StringTokenizer(providersHeader, ",; ");
			while (strTok.hasMoreTokens()) {
				String providerName = strTok.nextToken().trim();
				strTok.nextToken(); // Skips ranking
				Object provider = Class.forName(providerName).newInstance();
				if (provider instanceof BundleActivator) {
					((BundleActivator) provider).start(bc);
				}
				if (providerName.equals(storageName)) {
					storage = (ProvisioningStorage) provider;
					destroyStorageOnStop = true;
				}
				providers.addElement(provider);
			}
		}

		if (storage == null && storageName != null && storageName.length() != 0) {
			try {
				storage = (ProvisioningStorage) Class.forName(storageName).newInstance();
				if (storage instanceof BundleActivator) {
					((BundleActivator) storage).start(bc);
					destroyStorageOnStop = true;
				}
			} catch (Exception e) {
				Log.debug("Can't instantiate or start storage \"" + storage + "\"!");
				throw e;
			}
		}

		info = new ProvisioningData();

		if (storage == null) {
			synchronized (this) {
				bc.addServiceListener(this, "(|" + '(' + Constants.OBJECTCLASS + '=' + ProvisioningStorage.class.getName() + ")" + '(' + Constants.OBJECTCLASS + '=' + ProvisioningInfoProvider.class.getName() + ")" + '(' + Constants.OBJECTCLASS + '=' + Timer.class.getName() + ')' + ")");

				storage = getStorage();
			}
		} else {
			bc.addServiceListener(this, "(|" + '(' + Constants.OBJECTCLASS + '=' + ProvisioningInfoProvider.class.getName() + ')' + '(' + Constants.OBJECTCLASS + '=' + Timer.class.getName() + ')' + ")");
		}

		synchronized (this) {
			if (timer == null) {
				ServiceReference sRef = bc.getServiceReference(Timer.class.getName());
				if (sRef != null) {
					timer = (Timer) bc.getService(sRef);
				}
			}
		}

		if (storage != null) {
			Log.debug("Loads from " + storage + " storage.");
			try {
				Dictionary storedInfo = storage.getStoredInfo();
				if (storedInfo != null && storedInfo.size() != 0) {
					info.add(storedInfo);
				}
			} catch (Exception e) { // NPE or storage specific exception can be
				// thrown
				Log.debug(e);
				setError(ERROR_LOAD_STORE_DATA, e.toString());
				if (destroyStorageOnStop) {
					Log.debug("Warning: the storage could be unavailable!");
				} else {
					storage = null;
				}
			}
		}

		if (getHasFailedPrv() && !reAfterPrvFailureDisabled) {
			reprovision = true;
		}

		boolean hasLoadedInfo = false;

		ServiceReference[] srefs = bc.getServiceReferences(ProvisioningInfoProvider.class.getName(), null);
		if (srefs != null) {
			sort(srefs);
			for (int i = 0; i < srefs.length; i++) {
				ProvisioningInfoProvider provider = ((ProvisioningInfoProvider) bc.getService(srefs[i]));
				try {
					synchronized (info) {
						if (!info.providers.contains(provider)) {
							Log.debug("Loads from " + provider + " provider.");
							info.providers.addElement(provider);
							Dictionary toAdd = provider.init(this);
							if (toAdd != null && toAdd.size() != 0) {
								String prvref = (String) toAdd.get(ProvisioningService.PROVISIONING_REFERENCE);
								if (prvref != null && prvref.trim().length() != 0) { // reference
									// is
									// changed
									// by
									// loader
									reprovision = true;
								}
								info.add(toAdd);
								hasLoadedInfo = true;
							}
						}
					}
				} catch (Exception e) {
					Log.debug(e);
				}
			}
		}

		if (!isFrameworkStarted()) {
			bc.addFrameworkListener(this);
		} else {
			wfs = false;
		}

		if (hasLoadedInfo) {
			store();
		}
		active = true;
		processStart();

		// Join GW to a multicast host.
		try {
			String host = "225.0.0.0";
			if (bc.getProperty(MULTICAST_HOST) != null)
				host = bc.getProperty(MULTICAST_HOST);
			String port = Integer.toString(getInteger(MULTICAST_PORT, 7777));
			if (host.length() != 0 && port.length() != 0) {
				Class dscAgentClass = Class.forName("org.eclipse.equinox.internal.ip.impl.dscagent.DiscoveryAgent");
				Constructor constr = dscAgentClass.getConstructor(new Class[] {String.class, int.class, BundleContext.class, ProvisioningAgent.class});
				new Thread(da = (Runnable) constr.newInstance(new Object[] {host, new Integer(Integer.parseInt(port)), bc, this}), "Discovery Agent").start();
			} // "info" is already initialized
		} catch (Throwable t) {
			Log.debug("Can't create discovery agent!");
		}
		Log.debug("Provisioning agent started ...");
	}

	/**
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bc) throws Exception {
		Log.debug("Stopping provisionig agent ...");

		if (timer != null) {
			try {
				timer.removeListener(this, PROVISIONING);
			} catch (Throwable _) {
			}
			timer = null;
		}

		registered = false;

		try {
			bc.removeServiceListener(this);
		} catch (Exception _) {
		}

		try {
			bc.removeFrameworkListener(this);
		} catch (Exception _) {
		}

		if (sreg != null) {
			try {
				sreg.unregister();
			} catch (Exception _) {
			}
			sreg = null;
		} // Unregisters ProvisioningService

		if (da != null) {
			try {
				Method close = da.getClass().getMethod("close", new Class[] {});
				close.invoke(da, new Object[0]);
			} catch (Exception e) {
				Log.debug(e);
			}
			da = null;
		} // Closes DiscoveryAgent

		if (storage != null) {
			if (destroyStorageOnStop) {
				try {
					if (storage instanceof BundleActivator) {
						((BundleActivator) storage).stop(bc);
					}
				} catch (Exception e) {
					Log.debug(e);
				}
				destroyStorageOnStop = false;
			}
			storage = null;
		} // Stores data

		if (providers != null) {
			for (int i = providers.size(); i-- > 0;) {
				try {
					((BundleActivator) providers.elementAt(i)).stop(bc);
				} catch (Exception e) {
					Log.debug(e);
				}
			}
			providers = null;
		} // Stops providers

		if (urlHandlers != null) {
			for (int i = urlHandlers.size(); i-- > 0;) {
				try {
					((BundleActivator) urlHandlers.elementAt(i)).stop(bc);
				} catch (Exception e) {
					Log.debug(e);
				}
			}
			urlHandlers = null;
		} // Stops providers

		info = null;
		Log.debug("Provisioning agent stopped ...");
		Log.log.close();
		Log.log = null;
	}

	public Dictionary getInformation() {
		return info;
	}

	public void setInformation(Dictionary info) {
		boolean refChanged = info.get(ProvisioningService.PROVISIONING_REFERENCE) != null;
		synchronized (this.info) {
			Integer version = (Integer) this.info.get(ProvisioningService.PROVISIONING_UPDATE_COUNT);
			Integer newVersion = new Integer(version.intValue() + 1);
			this.info.set(info);
			incrementUC(newVersion);
		}
		modify();
		updated(refChanged);
	}

	public void addInformation(Dictionary info) {
		addInformation(info, null);
	}

	private static final ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private static final byte[] buffer = new byte[1024];

	private static byte[] readStream(InputStream is) throws IOException {
		synchronized (buffer) {
			baos.reset();
			int read;
			while ((read = is.read(buffer, 0, buffer.length)) != -1) {
				baos.write(buffer, 0, read);
			}
			return baos.toByteArray();
		}
	}

	private Bundle installBundle(String name, InputStream is) {
		Bundle bundle = null;
		try {
			bundle = getBundle(name);
			if (bundle == null) { /* install the bundle */
				if (Log.debug)
					Log.debug("Installing management bundle '" + name + "'");
				bundle = bc.installBundle(name, is);
			} else { /* just update it */
				if (Log.debug)
					Log.debug("Updating management bundle '" + name + "'");
				bundle.update(is);
			}
		} catch (Throwable t) {
			setHasFailedPrv(true);
			Log.debug("WARNING: Failed to install management bundle '" + name + "'", t);
		}
		return bundle;
	}

	public void addInformation(ZipInputStream zis) {
		Log.debug("Add Information form ZIS.");
		Hashtable entries = new Hashtable(2);//cache for unprocessed entries 
		boolean manifestFound = false;
		Dictionary info = new Hashtable(5);
		Dictionary entriesFromHeader = null;
		Dictionary extraFileds = null;
		Vector bundlesToStart = new Vector(5);
		String header = null;
		try {
			ZipEntry ze;
			String name, type;
			/* read the rest of the entries */
			while ((ze = zis.getNextEntry()) != null) {
				/* read name */
				name = ze.getName();
				if (name.endsWith("/")) {// path entry
					zis.closeEntry();
					continue;

				}
				if (name.charAt(0) == '/')
					name = name.substring(1);
				/* read extra */
				byte[] extra = ze.getExtra();
				type = extra == null ? null : new String(extra).toLowerCase();
				if (extra != null && !"META-INF/MANIFEST.MF".equals(name)) {
					if (extraFileds == null) {
						extraFileds = new Hashtable(3, 3);
					}
					extraFileds.put(name, type);
				}//the extra field is null or the entry is the manifest
				if (!manifestFound) {
					if ("META-INF/MANIFEST.MF".equals(name)) {//the entry is the manifest
						manifestFound = true;
						header = getHeaderFromManifest(zis);
						entriesFromHeader = filterAttributes(TYPE, parseEntries(header));
					} else {//no manifest yet, so cache the entry
						System.out.println("---put : " + name);
						entries.put(name, readStream(zis));
					}
				} else {//the manifest is found so we process the entry
					processEntry(extraFileds, name, null, zis, info, entriesFromHeader, bundlesToStart);
				}
				zis.closeEntry();
			}

			/*process the cached entries*/
			Enumeration names = entries.keys();
			while (names.hasMoreElements()) {
				processEntry(extraFileds, name = (String) names.nextElement(), (byte[]) entries.get(name), null, info, entriesFromHeader, bundlesToStart);
			}
		} catch (Throwable e) {
			this.info.setError(ERROR_CORRUPTED_ZIP, e.toString());
			Log.debug("Error reading provisioning package", e);
			setHasFailedPrv(true);
		}

		/* close the zip file */
		if (closeZip) {
			try {
				zis.close();
			} catch (Exception _) {
			}
		}

		/* update info and start all required bundles */
		addInformation(info, bundlesToStart); // bundle should
	}

	private void processEntry(Dictionary extraFileds, String name, byte[] content, InputStream is, Dictionary info, Dictionary entriesFromHeader, Vector bundlesToStart) throws IOException {
		/* 
		* first try the InitialProvisioning-Entries header
		* if the zip file had a manifest entry
		*/
		String type = entriesFromHeader == null ? null : (String) entriesFromHeader.get(name);
		/* 
		 * If there is no value in the InitialProvisioning-Entries header for that path
		 * try to initialize the type from the entry's extra field.
		 * If this ZIP entry field is present, the Initial Provisioning service should not
		 * look further, even if the extra field contains an erroneous value.
		 */
		if (type == null) {
			if (extraFileds != null) {
				type = (String) extraFileds.get(name);
			}
		}
		/*
		* if type is still null try to to initialize it 
		* according to the extension of the entry's name 
		*/
		if (type == null) {
			type = getMIMEfromExtension(name);
		}

		/* process entry */
		if (Log.debug) {
			Log.debug("Processing entry '" + name + "' of type " + type);
		}
		if (MIME_BUNDLE.equals(type) || MIME_BUNDLE_ALT.equals(type)) {
			installBundle(name, content == null ? new ISWrapper(is) : new ISWrapper(new ByteArrayInputStream(content)));
		} else if (MIME_BYTE_ARRAY.equals(type)) {
			info.put(name, content == null ? readStream(is) : content);
		} else if (MIME_STRING.equals(type)) {
			String value = getUTF8String(content == null ? readStream(is) : content);
			info.put(name, value);
			/*
			 * FIXME: actually there can be only ONE key of that type! - so why
			 * using vector
			 */
			if (PROVISIONING_START_BUNDLE.equals(name)) {
				/* Make management agent bundle deployment. Sets java.security.AllPermission
				 * if PermissionAdmin is available..*/
				try {
					grantAllPermissions(value);
				} catch (Throwable e) {
					Log.debug("Failed to grant all permissions", e);
				}
				bundlesToStart.addElement(value);
			}
		} else if (MIME_BUNDLE_URL.equals(type)) {
			String value = getUTF8String(content == null ? readStream(is) : content);
			installBundle(name, new URL(value).openStream());
		} else {
			this.info.setError(ERROR_CORRUPTED_ZIP, //
					"Unknown MIME type (" + type + ") for entry '" + name + "'");
			setHasFailedPrv(true);
		}
	}

	public void serviceChanged(ServiceEvent se) {
		Object service = bc.getService(se.getServiceReference());
		if (service instanceof ProvisioningStorage) {
			if (se.getType() == ServiceEvent.REGISTERED) {
				synchronized (this) {
					if (storage == null) { // If there is a storage it won't be
						// replaced.
						storage = (ProvisioningStorage) service;
					} else {
						return;
					}
				}
				try {
					Object oldUC = info.get(ProvisioningService.PROVISIONING_UPDATE_COUNT);
					int iOldUC = 0;
					if (oldUC != null && oldUC instanceof Integer) {
						iOldUC = ((Integer) oldUC).intValue();
					}

					Dictionary toAdd = storage.getStoredInfo();
					Object newUC = toAdd.get(ProvisioningService.PROVISIONING_UPDATE_COUNT);
					int iNewUC = 0;
					if (newUC != null && newUC instanceof Integer) {
						iNewUC = ((Integer) newUC).intValue();
					}

					boolean refChanged = toAdd.get(ProvisioningService.PROVISIONING_REFERENCE) != null && (iNewUC == 0 || reprovision);

					boolean increment = iNewUC > iOldUC; // In this case it
					// is assummed that
					// is most probably
					// that this
					// is the original version but storage is started after the
					// provisioning bundle.

					synchronized (info) {
						info.set(toAdd);
						if (increment) {
							incrementUC(new Integer(iNewUC));
							modify();
						}
					}

					updated(refChanged);
				} catch (Exception e) {
					Log.debug(e);
				}
				Log.debug("Storage is available.");
			} else if (se.getType() == ServiceEvent.UNREGISTERING) {
				if (storage == bc.getService(se.getServiceReference())) {
					Log.debug("Storage is removed!");
					try {
						storage.store(info);
					} catch (Exception e) {
						Log.debug("Can't store provisioning info!", e);
					}
					storage = null; // No more than one Storage service should
					// be registered on the FW
				}
			}
		}

		if (service instanceof ProvisioningInfoProvider) { // a Storage could
			// be a Provider
			if (se.getType() == ServiceEvent.REGISTERED) {
				ProvisioningInfoProvider provider = (ProvisioningInfoProvider) service;
				Log.debug("Loads from " + provider + " provider.");
				try {
					if (!info.providers.contains(provider)) {
						Dictionary toAdd = provider.init(this);
						boolean refChanged = false;
						if (toAdd != null) {
							String prvref = (String) toAdd.get(ProvisioningService.PROVISIONING_REFERENCE);
							if (prvref != null && prvref.trim().length() != 0) { // reference
								// is
								// changed
								// by
								// loader
								refChanged = true;
							}
						}
						boolean added = true;
						synchronized (info) {
							if (!info.providers.contains(provider)) {
								info.providers.addElement(provider);
								info.add(toAdd);
								if (refChanged) {
									reprovision = true;
								}
								Integer version = (Integer) info.get(ProvisioningService.PROVISIONING_UPDATE_COUNT);
								Integer newVersion = new Integer(version.intValue() + 1);
								incrementUC(newVersion);
							} else {
								added = false;
							}
						}
						if (added) {
							modify();
							updated(refChanged);
						}
					}
				} catch (Exception e) {
					Log.debug(e);
				}
			} else if (se.getType() == ServiceEvent.UNREGISTERING) {
				info.providers.removeElement(service);
			}
		}

		if (service instanceof Timer) { // timer is expected to be single
			// service
			switch (se.getType()) {
				case ServiceEvent.REGISTERED : {
					synchronized (this) {
						if (timer == null) {
							timer = (Timer) service;
							if (getHasFailedPrv() && !reAfterPrvFailureDisabled) {
								try {
									timer.notifyAfterMillis(this, nextPrvAfter(), PROVISIONING); // TO DO - Won't be
									// updated with newer
									// method - it will not
									// run on older FW
								} catch (Exception e) {
									Log.debug(e);
								}
							}
						}
					}
					break;
				}
				case ServiceEvent.UNREGISTERING : {
					timer = null;
					break;
				}
			}
		}
	}

	public synchronized void timer(int event) {
		Log.debug("Timer event " + event);
		try {
			switch (event) {
				case PROVISIONING : {
					Log.debug("Remake failed provisioning.");
					if (getHasFailedPrv()) {
						processPrvAssignment();
					}
					break;
				}
			}
		} catch (Exception e) {
			Log.debug(e);
		}
	}

	public void frameworkEvent(FrameworkEvent event) {
		if (event.getType() == FrameworkEvent.STARTED) {
			wfs = false;
			processStart();
		}
	}

	public boolean getHttpAllowed() {
		return httpAllowed;
	}

	// Synchronize frameworkEvent and start(BundleContext) initial reactions
	private void processStart() {
		synchronized (this) {
			try {
				Log.debug("Reprovision = " + reprovision + ", update counter = " + info.get(ProvisioningService.PROVISIONING_UPDATE_COUNT) + ", provisioning reference = " + info.get(ProvisioningService.PROVISIONING_REFERENCE));
				if ((reprovision || ((Integer) info.get(ProvisioningService.PROVISIONING_UPDATE_COUNT)).intValue() == 0) && info.get(ProvisioningService.PROVISIONING_REFERENCE) != null) {
					if (!startProcessed) {
						if (processPrvAssignment()) {
							startProcessed = true;
						}
					}
				}
			} catch (Throwable t) {
				Log.debug(t);
			}
		}

		synchronized (this) {
			if (active && !wfs && !registered) {
				registered = true;
			} else {
				return;
			}
		}
		Log.debug("Registering ProvisioningService.");
		sreg = bc.registerService(ProvisioningService.class.getName(), this, getRegProps());
	}

	/**
	 * This is called by ProvisioningData when one puts new
	 * <I>ProvisioningService.PROVISIONING_UPDATE_COUNT</I>.
	 * 
	 * @param refChanged
	 *            if ProvisioningService.PROVISIONING_REFERENCE changed
	 */
	private void updated(boolean refUpdated) {
		Log.debug("ProvisioingDictionary updated. Reference updated = " + refUpdated);

		store();

		synchronized (this) {
			if (refUpdated) {
				if (!processPrvAssignment()) {
					reprovision = true;
				}
			}
		}
	}

	private void store() {
		if (storage != null) {
			try {
				Dictionary info = this.info;
				if (info != null) {
					storage.store(info);
				}
			} catch (Exception e) {
				Log.debug(e);
			}
		} else {
			Log.debug("Warning: No storage available.");
		}
	}

	private boolean processPrvAssignment() {
		if (active && !wfs) {
			new Thread() {
				public void run() {
					try {
						process();
					} catch (Exception e) {
						Log.debug(e);
					}
				}
			}.start();
			return true;
		}
		return false;
	}

	/**
	 * Make provisioning data assignement to nextRef.
	 * 
	 * @param nextRef
	 *            next reference.
	 */
	synchronized void process() {
		if (info == null)
			return; // the bundle is stopped
		setHasFailedPrv(false);
		String ref = (String) info.get(ProvisioningService.PROVISIONING_REFERENCE);
		Log.debug("Reference = \"" + ref + '"');
		if (ref != null && (ref = ref.trim()).length() != 0) {
			String spid = (String) info.get(ProvisioningService.PROVISIONING_SPID);
			Log.debug("Setup from \"" + ref + "\", SPID = " + spid);

			if (!httpAllowed) {
				if (ref.startsWith("http://")) {
					Log.debug("Won't make setup to " + ref + " because http is forbidden!");
					setError(ERROR_MALFORMED_URL, "Provisioning reference is a HTTP URL, but non-secure HTTP is forbidden!");
					setHasFailedPrv(true);
					return;
				}
			}

			if (!ref.startsWith("file:") && ref.indexOf("service_platform_id") == -1) {
				if (ref.indexOf('?') == -1) {
					ref += '?';
				} else {
					ref += '&';
				}
				ref += "service_platform_id" + '=' + URLEncoder.encode(spid == null ? "" : spid);
			}
			Log.debug("Setup url = \"" + ref);

			URLConnection conn = null;

			try {
				URL url = new URL(ref);
				InputStream is = null;

				try {
					conn = url.openConnection();
					if (conn == null) {
						throw new IOException("Can't open connection to " + url + "!");
					}
					conn.setRequestProperty("Connection", "close");

					conn.connect();
					String error = conn.getHeaderField("error"); // Such
					// error
					// message
					// is not by
					// specification
					// and is
					// returned
					// by
					// Provisioining
					// Service
					// Backend
					if (error == null) {
						if ((conn instanceof HttpURLConnection) && ((HttpURLConnection) conn).getResponseCode() != HttpURLConnection.HTTP_OK) {
							String errorMsg = "Warning! ResponseCode = " + ((HttpURLConnection) conn).getResponseCode() + "!";
							Log.debug(errorMsg);
							setError(ERROR_IO_EXCEPTION, errorMsg);
						} else {
							is = conn.getInputStream();
						}
					} else {
						setError(ERROR_IO_EXCEPTION, error);
						Log.debug("Error from Backend: " + error + "! Setup failed!");
					}
				} catch (IOException e) {
					setError(ERROR_IO_EXCEPTION, e.toString());
					Log.debug(e);
				}

				if (is == null) {
					setHasFailedPrv(true);
				} else {
					try {
						ZipInputStream zis = new ZipInputStream(is);
						try {
							addInformation(zis); // the addInformation method
							// closes stream
							Log.debug("Setup ended.");
						} finally {
							if (!closeZip) {
								try {
									zis.close();
								} catch (Exception _) {
								}
							}
						}
					} catch (Exception e) {
						setError(ERROR_CORRUPTED_ZIP, e.toString());
						setHasFailedPrv(true);
						Log.debug(e);
					}
				}
			} catch (IOException e) { // new URL ---> MalformedURLException
				setError(ERROR_MALFORMED_URL, "Invalid Provisioning Reference: " + ref);
			} finally {
				if (conn != null && conn instanceof HttpURLConnection) {
					try {
						((HttpURLConnection) conn).disconnect();
					} catch (Exception _) {
					}
				}
			}
		}

		if (getHasFailedPrv()) {
			if (!reAfterPrvFailureDisabled) {
				try {
					if (timer != null) {
						timer.notifyAfterMillis(this, nextPrvAfter(), PROVISIONING); // TO DO - Won't be updated with
						// newer method - it will not
						// run on older FW
					}
				} catch (Exception e) {
					Log.debug(e);
				}
			}
		} else {
			times = 0;
			nextProvisioningAfter = a;
		}
	}

	private synchronized long nextPrvAfter() {
		times++;
		if (nextProvisioningAfter < maxPeriod && nextProvisioningAfter * times > changePeriod) {
			nextProvisioningAfter += b;
			times = 0;
		}
		Log.debug("Next bootstrap after " + nextProvisioningAfter + "ms");
		return nextProvisioningAfter;
	}

	// bundlesToStart is null on addInfo from provider/storage, and != null on
	// add from ZIS
	// pending error must be cleared only in second case!
	private void addInformation(Dictionary info, Vector bundlesToStart) {
		boolean refChanged = info.get(ProvisioningService.PROVISIONING_REFERENCE) != null;
		synchronized (this.info) {
			this.info.add(info);
			Integer version = (Integer) this.info.get(ProvisioningService.PROVISIONING_UPDATE_COUNT);
			Integer newVersion = new Integer(version.intValue() + 1);
			incrementUC(newVersion);
		}

		Log.debug("Bundles to start: " + bundlesToStart);
		if (bundlesToStart != null) {
			for (int i = 0; i < bundlesToStart.size(); i++) {
				Object next = bundlesToStart.elementAt(i);
				try {
					if (next instanceof Bundle) {
						((Bundle) next).start();
					} else {
						Bundle b = getBundle((String) next);
						if (b != null) {
							b.start();
						} else {
							Log.debug("Can't find '" + next + "' bundle to start it!");
						}
					}
				} catch (Exception e) {
					Log.debug("Exception while starting " + (next instanceof Bundle ? ((Bundle) next).getLocation() : next), e);
					setHasFailedPrv(true);
					return;
				}
			}

			clearError();
		}

		modify();
		updated(refChanged);
	}

	private boolean isFrameworkStarted() {
		if (!wfs)
			return true;
		Bundle system = bc.getBundle(0);
		return system != null ? system.getState() == Bundle.ACTIVE : true;
	}

	/**
	 * Make management agent bundle deployment. Sets java.security.AllPermission
	 * if PermissionAdmin is available..
	 * 
	 * @param maRef
	 *            management agent reference.
	 */
	private void grantAllPermissions(String location) {
		try {
			ServiceReference sref = bc.getServiceReference("org.osgi.service.permissionadmin.PermissionAdmin"); // the
			// org.osgi.service.permissionadmin is not imported for R1
			if (sref != null) {
				Method method = Class.forName("org.osgi.service.permissionadmin.PermissionAdmin").getMethod("setPermissions", new Class[] {String.class, Class.forName("[Lorg.osgi.service.permissionadmin.PermissionInfo;")});
				Object[] allPerms = (Object[]) Array.newInstance(Class.forName("org.osgi.service.permissionadmin.PermissionInfo"), 1);
				Constructor constr = Class.forName("org.osgi.service.permissionadmin.PermissionInfo").getConstructor(new Class[] {String.class, String.class, String.class});
				allPerms[0] = constr.newInstance(new String[] {"java.security.AllPermission", "", ""});
				method.invoke(bc.getService(sref), new Object[] {location, allPerms});
			}
		} catch (Exception e) {
			Log.debug(e);
		}
	}

	private ProvisioningStorage getStorage() {
		ServiceReference sref = bc.getServiceReference(ProvisioningStorage.class.getName());
		return sref != null ? (ProvisioningStorage) bc.getService(sref) : null;
	}

	private Bundle getBundle(String location) {
		Bundle[] bundles = bc.getBundles();
		for (int i = bundles.length; i-- > 0;) {
			if (location.equalsIgnoreCase(bundles[i].getLocation())) {
				return bundles[i];
			}
		}
		return null;
	}

	private void incrementUC(Integer uc) {
		if (!active) {
			return;
		}
		info.putUC(uc);
	}

	private void modify() {
		if (!active || sreg == null) {
			return;
		}
		try {
			sreg.setProperties(getRegProps());
		} catch (Exception e) {
		}
	}

	private Dictionary getRegProps() {
		Hashtable prvsprops = new Hashtable(1, 1.0F);
		prvsprops.put("Vendor", "ProSyst");
		prvsprops.put(ProvisioningService.PROVISIONING_UPDATE_COUNT, info.get(ProvisioningService.PROVISIONING_UPDATE_COUNT));
		return prvsprops;
	}

	private void sort(ServiceReference[] srefs) {
		for (int i = srefs.length - 1; i > 0; i--) {
			for (int j = 0; j < i; j++) {
				if (less(srefs[j + 1], srefs[j])) {
					ServiceReference temp = srefs[j];
					srefs[j] = srefs[j + 1];
					srefs[j + 1] = temp;
				}
			}
		}
	}

	private boolean less(ServiceReference sref1, ServiceReference sref2) {
		return getRanking(sref1) < getRanking(sref2);
	}

	private int getRanking(ServiceReference sref) {
		Integer ranking = (Integer) sref.getProperty(Constants.SERVICE_RANKING);
		return ranking == null ? 0 : ranking.intValue();
	}

	private String getUTF8String(byte[] body) {
		try {
			return new String(body, "UTF-8");
		} catch (Exception _0) {
			try {
				return new String(body, "UTF8"); // for personal java
				// problems
			} catch (Exception _1) {
				return new String(body);
			}
		}
	}

	private void setError(int code, String message) {
		ProvisioningData data = this.info;
		if (data == null) {
			return;
		}

		data.setError(code, message);

		Integer version = (Integer) data.get(ProvisioningService.PROVISIONING_UPDATE_COUNT);
		Integer newVersion = new Integer(version.intValue() + 1);
		info.putUC(newVersion);

		modify();
	}

	private void clearError() {
		ProvisioningData data = this.info;
		if (data == null) {
			return;
		}

		data.clearError();
	}

	private boolean getHasFailedPrv() {
		return "true".equals(info.get(HAS_FAILED_PROVISIONG));
	}

	private void setHasFailedPrv(boolean hasFailedPrv) {
		if (getHasFailedPrv() != hasFailedPrv) {
			info.putPrivate(HAS_FAILED_PROVISIONG, hasFailedPrv ? "true" : null);
			store();
		}
	}

	/**
	 * Parses the InitialProvisioning-Entries manifest header.
	 * @param header the contents of the InitialProvisioning-Entries manifest header, if any
	 * @return Dictionary which maps entry paths to Dictionary representing the attributes
	 * 		   or null, if no header is found.
	 */
	private static Dictionary parseEntries(String header) {
		Dictionary entries = null;
		Dictionary attributes = null;
		header = removeWhiteSpaces(header);
		if (header == null || header.length() == 0)
			return null;
		int begin = 0, end = 1, length = header.length();
		boolean quoted = false;
		String path, attribute, value;

		entry: while (end != -1 && begin < length - 1) {
			end = readToken(header, begin, false, false); //read the path
			if (end == -1)
				break; //end of header, or only path

			switch (header.charAt(end)) {
				case ';' ://end of path , read attribute
					if (begin == end) {
						end = readToken(header, end + 1, false, true);
						begin = end + 1;
						continue;
					}
					path = header.substring(begin, end);
					begin = end + 1;
					//read the attributes
					while (end != -1 && begin < length - 1) {
						end = readToken(header, begin, quoted, false);
						if (end == -1)
							break entry;
						if (header.charAt(end) != '=' || begin == end) {
							//invalid syntax
							end = readToken(header, begin, false, true); //read the attribute name
							begin = end + 1;
							continue entry;
						}
						attribute = header.substring(begin, end);
						if (header.charAt(begin) == '\"') {
							quoted = true;
							begin = end + 2;
						} else
							begin = end + 1;

						if (begin >= length - 1)
							break entry;

						end = readToken(header, begin, quoted, false); //read the attribute value
						if (end == -1) {
							if (quoted) {
								//invalid syntax
								end = readToken(header, begin, false, true);
								begin = end + 1;
								continue entry;
							}
							//end of header or last attribute-value
							value = header.substring(begin, length);
							if (attributes == null)
								attributes = new Hashtable(2, 3);
							attributes.put(attribute, value);
							begin = end + 1;
							break;
						}
						switch (header.charAt(end)) {
							case ',' : //end of parameters list
								value = header.substring(begin, end);
								if (attributes == null)
									attributes = new Hashtable(2, 3);
								attributes.put(attribute, value);
								if (entries == null)
									entries = new Hashtable(3, 3);
								entries.put(path, attributes);
								attributes = null;
								continue entry;
							case ';' : //another parameter
								value = header.substring(begin, end);
								if (attributes == null)
									attributes = new Hashtable(2, 3);
								attributes.put(attribute, value);
								begin = end + 1;
								continue;
							case '\"' : //endof quoted part
								quoted = false;
								value = header.substring(begin, end - 1);
								if (attributes == null)
									attributes = new Hashtable(2, 3);
								attributes.put(attribute, value);
								begin = end + 1;
								continue;
							default : //invalid syntax
								end = readToken(header, begin, false, true);
								begin = end + 1;
								continue entry;
						}
					}
					break;
				case ',' : //no description
					if (begin == end)
						begin = end + 2;
					else
						begin = end + 1;
					continue;
				default :// invalid syntax
					end = readToken(header, end + 1, false, true);
					begin = end + 1;
					continue;
			}

			if (attributes != null) {
				if (entries == null)
					entries = new Hashtable(3, 3);
				entries.put(path, attributes);
				attributes = null;
			}
		}
		return entries;
	}

	/**
	 * Reads the next token from the manifest header.
	 * @param token the valiue of the manifest header.
	 * @param begin index of the char from which to start
	 * @param quoted if the consequent part of teh string is guoted
	 * @param skipInvalidPath if true all chars till the first comma are skipped
	 * @return the index of the first character from the string which is 
	 *         different from the expected, or -1 if not found
	 */
	private static int readToken(String token, int begin, boolean quoted, boolean skipInvalidPath) {
		char c = 0;
		int len = token.length();
		if (begin >= len)
			return -1;
		if (quoted) {
			while ((c = token.charAt(begin)) != '\"') {
				if (++begin == len)
					return -1;
			}
			return begin;
		}
		if (skipInvalidPath) {
			while ((c = token.charAt(begin)) != ',') {
				if (++begin == len)
					return -1;
			}
			return begin;
		}
		while ((c = token.charAt(begin)) >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '/' || c == '.' || c == ':') {
			if (++begin == len)
				return -1;
		}
		return begin;
	}

	static final String TYPE = "type";

	/**
	 * Filters the Dictionary returned from {@link #parseEntries(String)}.
	 * Returns Dictionary which maps entry paths to values of the attribute,
	 * which name is given as argument.
	 */
	private static Dictionary filterAttributes(String attribute, Dictionary entries) {
		if (entries == null)
			return null;
		Dictionary filtered = null;
		Enumeration paths = entries.keys();
		Dictionary attributes = null;
		String path = null, type = null, mime = null;
		while (paths.hasMoreElements()) {
			path = (String) paths.nextElement();
			attributes = (Dictionary) entries.get(path);
			if ((type = (String) attributes.get(attribute)) != null && (mime = typeToMIME(type)) != null) {
				if (filtered == null)
					filtered = new Hashtable(3, 3);
				filtered.put(path, mime);
			}
		}
		return filtered;
	}

	/**
	 * Maps type to an appropriate mime type constant
	 * @param type the value of the type attribute from the InitialProvisioning-Entries header
	 * @return the mime type for the string type or null if type is not a valid type
	 */
	private static String typeToMIME(String type) {
		if ("text".equals(type))
			return ProvisioningService.MIME_STRING;
		if ("binary".equals(type))
			return ProvisioningService.MIME_BYTE_ARRAY;
		if ("bundle".equals(type))
			return ProvisioningService.MIME_BUNDLE;
		if ("bundle-url".equals(type))
			return ProvisioningService.MIME_BUNDLE_URL;
		return null;
	}

	/** 
	 * @param filename the name of the zip entry 
	 * @return the MIME type of the entry according to its extension 
	 *         or null if the mime type cannot be defined
	 */
	static String getMIMEfromExtension(String filename) {
		int index = filename.lastIndexOf(".");
		//no extension -> we cannot identify the type
		if (index == -1)
			return null;
		String extension = filename.substring(index + 1);
		if (extension.equals("jar"))
			return ProvisioningService.MIME_BUNDLE;
		if (extension.equals("txt"))
			return ProvisioningService.MIME_STRING;
		if (extension.equals("url"))
			return ProvisioningService.MIME_BUNDLE_URL;
		return ProvisioningService.MIME_BYTE_ARRAY;
	}

	/**
	 * @param is the InputStream containing the information in the manifest
	 * @return the contents of the InitialProvisioning-Entries header if any or null otherwise 
	 */
	private static String getHeaderFromManifest(InputStream is) {
		boolean blank = false;
		StringBuffer header = new StringBuffer();
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line = null;
		boolean loop = true;
		try {
			while ((line = br.readLine()) != null && loop) {
				if (line.length() == 0) {
					if (blank) {
						break;
					}
					blank = true;
					continue;
				} else {
					blank = false;
				}
				if (line.startsWith(ProvisioningService.INITIALPROVISIONING_ENTRIES)) {
					header.append(removeWhiteSpaces(line.substring(ProvisioningService.INITIALPROVISIONING_ENTRIES.length() + 1)));
					line = br.readLine();//next line
					while (loop = (line.length() != 0 && Character.isWhitespace(line.charAt(0)))) {
						header.append(removeWhiteSpaces(line));
						line = br.readLine();
					}
				}
			}
		} catch (IOException ioe) {
			return null;
		}
		return (header.length() == 0 ? null : header.toString());
	}

	private static String removeWhiteSpaces(String s) {
		if (s == null)
			return null;
		char curr;
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isWhitespace(curr = s.charAt(i))) {
				sb.append(curr);
			}
		}
		return sb.toString();
	}

	private static class ISWrapper extends InputStream {

		private InputStream is;

		ISWrapper(InputStream is) {
			this.is = is;
		}

		public int read() throws IOException {
			return is.read();
		}

		public int read(byte[] src, int off, int len) throws IOException {
			return is.read(src, off, len);
		}

		public void close() {
		}
	}

	public static boolean getBoolean(String property) {
		String prop = (bc != null) ? bc.getProperty(property) : System.getProperty(property);
		return ((prop != null) && prop.equalsIgnoreCase("true"));
	}

	public static int getInteger(String property, int defaultValue) {
		String prop = (bc != null) ? bc.getProperty(property) : System.getProperty(property);
		if (prop != null) {
			try {
				return Integer.decode(prop).intValue();
			} catch (NumberFormatException e) {
				//do nothing
			}
		}
		return defaultValue;
	}
}
