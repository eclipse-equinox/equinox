/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.io.File;
import java.io.InputStream;
import java.security.*;
import java.util.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.eventmgr.EventListeners;
import org.eclipse.osgi.framework.eventmgr.EventSource;
import org.osgi.framework.*;

/**
 * Bundle's execution context.
 *
 * This object is given out to bundles and wraps the internal
 * BundleContext object. It is destroyed when a bundle is stopped.
 */

public class BundleContext implements org.osgi.framework.BundleContext, EventSource {

	/** true if the bundle context is still valid */
	private boolean valid;

	/** Bundle object this context is associated with. */
	// This slot is accessed directly by the Framework instead of using
	// the getBundle() method because the Framework needs access to the bundle
	// even when the context is invalid while the close method is being called.
	protected BundleHost bundle;

	/** Internal framework object. */
	protected Framework framework;

	/** Services that bundle has used. Key is ServiceReference,
	    Value is ServiceUse */
	protected Hashtable servicesInUse;

	/** Listener list for bundle's BundleListeners */
	protected EventListeners bundleEvent;

	/** Listener list for bundle's SynchronousBundleListeners */
	protected EventListeners bundleEventSync;

	/** Listener list for bundle's ServiceListeners */
	protected EventListeners serviceEvent;

	/** Listener list for bundle's FrameworkListeners */
	protected EventListeners frameworkEvent;

	/** The current instantiation of the activator. */
	protected BundleActivator activator;

	/** private object for locking */
	protected Object contextLock = new Object();

	/**
	 * Construct a BundleContext which wrappers the framework for a
	 * bundle
	 *
	 * @param bundle The bundle we are wrapping.
	 */
	protected BundleContext(BundleHost bundle) {
		this.bundle = bundle;
		valid = true;
		framework = bundle.framework;
		bundleEvent = null;
		bundleEventSync = null;
		serviceEvent = null;
		frameworkEvent = null;
		servicesInUse = null;
		activator = null;
	}

	/**
	 * Destroy the wrapper. This is called when the bundle is stopped.
	 *
	 */
	protected void close() {
		valid = false; /* invalidate context */

		if (serviceEvent != null) {
			framework.serviceEvent.removeListener(this);
			serviceEvent = null;
		}
		if (frameworkEvent != null) {
			framework.frameworkEvent.removeListener(this);
			frameworkEvent = null;
		}
		if (bundleEvent != null) {
			framework.bundleEvent.removeListener(this);
			bundleEvent = null;
		}
		if (bundleEventSync != null) {
			framework.bundleEventSync.removeListener(this);
			bundleEventSync = null;
		}

		/* service's registered by the bundle, if any, are unregistered. */

		Vector registeredServices = null;
		ServiceReference[] publishedReferences = null;
		int regSize = 0;
		synchronized (framework.serviceRegistry) {
			registeredServices = framework.serviceRegistry.lookupServiceReferences(this);
			if (registeredServices != null) {
				regSize = registeredServices.size();
			}

			if (regSize > 0) {
				if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
					Debug.println("Unregistering services");
				}

				publishedReferences = new ServiceReference[regSize];
				registeredServices.copyInto(publishedReferences);
			}
		}

		for (int i = 0; i < regSize; i++) {
			try {
				publishedReferences[i].registration.unregister();
			} catch (IllegalStateException e) {
				/* already unregistered */
			}
		}

		registeredServices = null;

		/* service's used by the bundle, if any, are released. */
		if (servicesInUse != null) {
			int usedSize;
			ServiceReference[] usedRefs = null;

			synchronized (servicesInUse) {
				usedSize = servicesInUse.size();

				if (usedSize > 0) {
					if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
						Debug.println("Releasing services");
					}

					usedRefs = new ServiceReference[usedSize];

					Enumeration enum = servicesInUse.keys();
					for (int i = 0; i < usedSize; i++) {
						usedRefs[i] = (ServiceReference) enum.nextElement();
					}
				}
			}

			for (int i = 0; i < usedSize; i++) {
				usedRefs[i].registration.releaseService(this);
			}

			servicesInUse = null;
		}
		
		bundle = null;
	}

	/**
	 * Retrieve the value of the named environment property.
	 *
	 * @param key The name of the requested property.
	 * @return The value of the requested property, or <code>null</code> if
	 * the property is undefined.
	 */
	public String getProperty(String key) {
		SecurityManager sm = System.getSecurityManager();

		if (sm != null) {
			sm.checkPropertyAccess(key);
		}

		return (framework.getProperty(key));
	}

	/**
	 * Retrieve the Bundle object for the context bundle.
	 *
	 * @return The context bundle's Bundle object.
	 */
	public org.osgi.framework.Bundle getBundle() {
		checkValid();

		return (bundle);
	}

	/**
	 * Install a bundle from a location.
	 *
	 * The bundle is obtained from the location
	 * parameter as interpreted by the framework
	 * in an implementation dependent way. Typically, location
	 * will most likely be a URL.
	 *
	 * @param location The location identifier of the bundle to install.
	 * @return The Bundle object of the installed bundle.
	 */
	public org.osgi.framework.Bundle installBundle(String location) throws BundleException {
		framework.checkAdminPermission();
		checkValid();
		return framework.installBundle(location);
	}

	/**
	 * Install a bundle from an InputStream.
	 *
	 * <p>This method performs all the steps listed in
	 * {@link #installBundle(java.lang.String)}, except the
	 * bundle's content will be read from the InputStream.
	 * The location identifier specified will be used
	 * as the identity of the bundle.
	 *
	 * @param location The location identifier of the bundle to install.
	 * @param in The InputStream from which the bundle will be read.
	 * @return The Bundle of the installed bundle.
	 */
	public org.osgi.framework.Bundle installBundle(String location, InputStream in) throws BundleException {
		framework.checkAdminPermission();

		checkValid();

		return framework.installBundle(location, in);
	}

	/**
	 * Retrieve the bundle that has the given unique identifier.
	 *
	 * @param id The identifier of the bundle to retrieve.
	 * @return A Bundle object, or <code>null</code>
	 * if the identifier doesn't match any installed bundle.
	 */
	public org.osgi.framework.Bundle getBundle(long id) {
		return (framework.getBundle(id));
	}

	/**
	 * Retrieve the bundle that has the given location.
	 *
	 * @param location The location string of the bundle to retrieve.
	 * @return A Bundle object, or <code>null</code>
	 * if the location doesn't match any installed bundle.
	 */
	public Bundle getBundleByLocation(String location) {
		return (framework.getBundleByLocation(location));
	}

	/**
	 * Retrieve a list of all installed bundles.
	 * The list is valid at the time
	 * of the call to getBundles, but the framework is a very dynamic
	 * environment and bundles can be installed or uninstalled at anytime.
	 *
	 * @return An array of {@link Bundle} objects, one
	 * object per installed bundle.
	 */
	public org.osgi.framework.Bundle[] getBundles() {
		return framework.getAllBundles();
	}

	/**
	 * Add a service listener with a filter.
	 * {@link ServiceListener}s are notified when a service has a lifecycle
	 * state change.
	 * See {@link #getServiceReferences(String, String) getServiceReferences}
	 * for a description of the filter syntax.
	 * The listener is added to the context bundle's list of listeners.
	 * See {@link #getBundle() getBundle()}
	 * for a definition of context bundle.
	 *
	 * <p>The listener is called if the filter criteria is met.
	 * To filter based upon the class of the service, the filter
	 * should reference the "objectClass" property.
	 * If the filter paramater is <code>null</code>, all services
	 * are considered to match the filter.
	 * <p>If the Java runtime environment supports permissions, then additional
	 * filtering is done.
	 * {@link Bundle#hasPermission(Object) Bundle.hasPermission} is called for the
	 * bundle which defines the listener to validate that the listener has the
	 * {@link ServicePermission} permission to <code>"get"</code> the service
	 * using at least one of the named classes the service was registered under.
	 *
	 * @param listener The service listener to add.
	 * @param filter The filter criteria.
	 * @exception InvalidSyntaxException If the filter parameter contains
	 * an invalid filter string which cannot be parsed.
	 * @see ServiceEvent
	 * @see ServiceListener
	 * @exception java.lang.IllegalStateException
	 * If the context bundle {@link <a href="#context_valid">has stopped</a>}.
	 */
	public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
		checkValid();

		if (Debug.DEBUG && Debug.DEBUG_EVENTS) {
			String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(listener.hashCode());
			Debug.println("addServiceListener[" + bundle + "](" + listenerName + ", \"" + filter + "\")");
		}

		ServiceListener filteredListener = (filter == null) ? listener : new FilteredServiceListener(filter, listener);

		synchronized (framework.serviceEvent) {
			if (serviceEvent == null) {
				serviceEvent = new EventListeners();
				framework.serviceEvent.addListener(this, this);
			} else {
				serviceEvent.removeListener(listener);
			}

			serviceEvent.addListener(listener, filteredListener);
		}
	}

	/**
	 * Add a service listener.
	 *
	 * <p>This method is the same as calling
	 * {@link #addServiceListener(ServiceListener, String)}
	 * with filter set to <code>null</code>.
	 *
	 * @see #addServiceListener(ServiceListener, String)
	 */
	public void addServiceListener(ServiceListener listener) {
		try {
			addServiceListener(listener, null);
		} catch (InvalidSyntaxException e) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("InvalidSyntaxException w/ null filter" + e.getMessage());
				Debug.printStackTrace(e);
			}
		}
	}

	/**
	 * Remove a service listener.
	 * The listener is removed from the context bundle's list of listeners.
	 * See {@link #getBundle() getBundle()}
	 * for a definition of context bundle.
	 *
	 * <p>If this method is called with a listener which is not registered,
	 * then this method does nothing.
	 *
	 * @param listener The service listener to remove.
	 * @exception java.lang.IllegalStateException
	 * If the context bundle {@link <a href="#context_valid">has stopped</a>}.
	 */
	public void removeServiceListener(ServiceListener listener) {
		checkValid();

		if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
			String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(listener.hashCode());
			Debug.println("removeServiceListener[" + bundle + "](" + listenerName + ")");
		}

		if (serviceEvent != null) {
			synchronized (framework.serviceEvent) {
				serviceEvent.removeListener(listener);
			}
		}
	}

	/**
	 * Add a bundle listener.
	 * {@link BundleListener}s are notified when a bundle has a lifecycle
	 * state change.
	 * The listener is added to the context bundle's list of listeners.
	 * See {@link #getBundle() getBundle()}
	 * for a definition of context bundle.
	 *
	 * @param listener The bundle listener to add.
	 * @exception java.lang.IllegalStateException
	 * If the context bundle {@link <a href="#context_valid">has stopped</a>}.
	 * @see BundleEvent
	 * @see BundleListener
	 */
	public void addBundleListener(BundleListener listener) {
		checkValid();

		if (Debug.DEBUG && Debug.DEBUG_EVENTS) {
			String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(listener.hashCode());
			Debug.println("addBundleListener[" + bundle + "](" + listenerName + ")");
		}

		if (listener instanceof SynchronousBundleListener) {
			framework.checkAdminPermission();

			synchronized (framework.bundleEventSync) {
				if (bundleEventSync == null) {
					bundleEventSync = new EventListeners();
					framework.bundleEventSync.addListener(this, this);
				} else {
					bundleEventSync.removeListener(listener);
				}

				bundleEventSync.addListener(listener, listener);
			}
		} else {
			synchronized (framework.bundleEvent) {
				if (bundleEvent == null) {
					bundleEvent = new EventListeners();
					framework.bundleEvent.addListener(this, this);
				} else {
					bundleEvent.removeListener(listener);
				}

				bundleEvent.addListener(listener, listener);
			}
		}
	}

	/**
	 * Remove a bundle listener.
	 * The listener is removed from the context bundle's list of listeners.
	 * See {@link #getBundle() getBundle()}
	 * for a definition of context bundle.
	 *
	 * <p>If this method is called with a listener which is not registered,
	 * then this method does nothing.
	 *
	 * @param listener The bundle listener to remove.
	 * @exception java.lang.IllegalStateException
	 * If the context bundle {@link <a href="#context_valid">has stopped</a>}.
	 */
	public void removeBundleListener(BundleListener listener) {
		checkValid();

		if (Debug.DEBUG && Debug.DEBUG_EVENTS) {
			String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(listener.hashCode());
			Debug.println("removeBundleListener[" + bundle + "](" + listenerName + ")");
		}

		if (listener instanceof SynchronousBundleListener) {
			framework.checkAdminPermission();

			if (bundleEventSync != null) {
				synchronized (framework.bundleEventSync) {
					bundleEventSync.removeListener(listener);
				}
			}
		} else {
			if (bundleEvent != null) {
				synchronized (framework.bundleEvent) {
					bundleEvent.removeListener(listener);
				}
			}
		}
	}

	/**
	 * Add a general framework listener.
	 * {@link FrameworkListener}s are notified of general framework events.
	 * The listener is added to the context bundle's list of listeners.
	 * See {@link #getBundle() getBundle()}
	 * for a definition of context bundle.
	 *
	 * @param listener The framework listener to add.
	 * @exception java.lang.IllegalStateException
	 * If the context bundle {@link <a href="#context_valid">has stopped</a>}.
	 * @see FrameworkEvent
	 * @see FrameworkListener
	 */
	public void addFrameworkListener(FrameworkListener listener) {
		checkValid();

		if (Debug.DEBUG && Debug.DEBUG_EVENTS) {
			String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(listener.hashCode());
			Debug.println("addFrameworkListener[" + bundle + "](" + listenerName + ")");
		}

		synchronized (framework.frameworkEvent) {
			if (frameworkEvent == null) {
				frameworkEvent = new EventListeners();
				framework.frameworkEvent.addListener(this, this);
			} else {
				frameworkEvent.removeListener(listener);
			}

			frameworkEvent.addListener(listener, listener);
		}
	}

	/**
	 * Remove a framework listener.
	 * The listener is removed from the context bundle's list of listeners.
	 * See {@link #getBundle() getBundle()}
	 * for a definition of context bundle.
	 *
	 * <p>If this method is called with a listener which is not registered,
	 * then this method does nothing.
	 *
	 * @param listener The framework listener to remove.
	 * @exception java.lang.IllegalStateException
	 * If the context bundle {@link <a href="#context_valid">has stopped</a>}.
	 */
	public void removeFrameworkListener(FrameworkListener listener) {
		checkValid();

		if (Debug.DEBUG && Debug.DEBUG_EVENTS) {
			String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(listener.hashCode());
			Debug.println("removeFrameworkListener[" + bundle + "](" + listenerName + ")");
		}

		if (frameworkEvent != null) {
			synchronized (framework.frameworkEvent) {
				frameworkEvent.removeListener(listener);
			}
		}
	}

	/**
	 * Register a service with multiple names.
	 * This method registers the given service object with the given properties
	 * under the given class names.
	 * A {@link ServiceRegistration} object is returned.
	 * The {@link ServiceRegistration} object is for the private use of the bundle
	 * registering the service and should not be shared with other bundles.
	 * The registering bundle is defined to be the context bundle.
	 * See {@link #getBundle()} for a definition of context bundle.
	 * Other bundles can locate the service by using either the
	 * {@link #getServiceReferences getServiceReferences} or
	 * {@link #getServiceReference getServiceReference} method.
	 *
	 * <p>A bundle can register a service object that implements the
	 * {@link ServiceFactory} interface to
	 * have more flexiblity in providing service objects to different
	 * bundles.
	 *
	 * <p>The following steps are followed to register a service:
	 * <ol>
	 * <li>If the service parameter is not a {@link ServiceFactory},
	 * an <code>IllegalArgumentException</code> is thrown if the
	 * service parameter is not an <code>instanceof</code>
	 * all the classes named.
	 * <li>The service is added to the framework's service registry
	 * and may now be used by other bundles.
	 * <li>A {@link ServiceEvent} of type {@link ServiceEvent#REGISTERED}
	 * is synchronously sent.
	 * <li>A {@link ServiceRegistration} object for this registration
	 * is returned.
	 * </ol>
	 *
	 * @param clazzes The class names under which the service can be located.
	 *                The class names in this array will be stored in the service's
	 *                properties under the key "objectClass".
	 * @param service The service object or a {@link ServiceFactory} object.
	 * @param properties The properties for this service.
	 *        The keys in the properties object must all be Strings.
	 *        Changes should not be made to this object after calling this method.
	 *        To update the service's properties call the
	 *        {@link ServiceRegistration#setProperties ServiceRegistration.setProperties}
	 *        method.
	 *        This parameter may be <code>null</code> if the service has no properties.
	 * @return A {@link ServiceRegistration} object for use by the bundle
	 *        registering the service to update the
	 *        service's properties or to unregister the service.
	 * @exception java.lang.IllegalArgumentException If one of the following is true:
	 * <ul>
	 * <li>The service parameter is null.
	 * <li>The service parameter is not a {@link ServiceFactory} and is not an
	 * <code>instanceof</code> all the named classes in the clazzes parameter.
	 * </ul>
	 * @exception java.lang.SecurityException If the caller does not have
	 * {@link ServicePermission} permission to "register" the service for
	 * all the named classes
	 * and the Java runtime environment supports permissions.
	 * @exception java.lang.IllegalStateException
	 * If the context bundle {@link <a href="#context_valid">has stopped</a>}.
	 * @see ServiceRegistration
	 * @see ServiceFactory
	 */
	public org.osgi.framework.ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
		checkValid();

		if (service == null) {
			if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
				Debug.println("Service object is null");
			}

			throw new NullPointerException(Msg.formatter.getString("SERVICE_ARGUMENT_NULL_EXCEPTION"));
		}

		int size = clazzes.length;

		if (size == 0) {
			if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
				Debug.println("Classes array is empty");
			}

			throw new IllegalArgumentException(Msg.formatter.getString("SERVICE_EMPTY_CLASS_LIST_EXCEPTION"));
		}

		/* copy the array so that changes to the original will not affect us. */
		String[] copy = new String[clazzes.length];
		for (int i = 0; i < clazzes.length; i++) {
			copy[i] = new String(clazzes[i].getBytes());
		}
		clazzes = copy;

		/* check for ServicePermissions. */
		framework.checkRegisterServicePermission(clazzes);

		if (!(service instanceof ServiceFactory)) {
			PackageAdmin packageAdmin = framework.packageAdmin;
			for (int i = 0; i < size; i++) {
				Class clazz = packageAdmin.loadServiceClass(clazzes[i],bundle);
				if (clazz == null) {
					if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
						Debug.println(clazzes[i] + " class not found");
					}
					throw new IllegalArgumentException(Msg.formatter.getString("SERVICE_CLASS_NOT_FOUND_EXCEPTION", clazzes[i]));
				}

				if (!clazz.isInstance(service)) {
					if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
						Debug.println("Service object is not an instanceof " + clazzes[i]);
					}
					throw new IllegalArgumentException(Msg.formatter.getString("SERVICE_NOT_INSTANCEOF_CLASS_EXCEPTION", clazzes[i]));
				}
			}
		}

		return (createServiceRegistration(clazzes, service, properties));
	}

	/**
	 * Create a new ServiceRegistration object. This method is used so that it may be overridden
	 * by a secure implementation.
	 *
	 * @param clazzes The class names under which the service can be located.
	 * @param service The service object or a {@link ServiceFactory} object.
	 * @param properties The properties for this service.
	 * @return A {@link ServiceRegistration} object for use by the bundle.
	 */
	protected ServiceRegistration createServiceRegistration(String[] clazzes, Object service, Dictionary properties) {
		return (new ServiceRegistration(this, clazzes, service, properties));
	}

	/**
	 * Register a service with a single name.
	 * This method registers the given service object with the given properties
	 * under the given class name.
	 *
	 * <p>This method is otherwise identical to
	 * {@link #registerService(java.lang.String[], java.lang.Object, java.util.Dictionary)}
	 * and is provided as a convenience when the service parameter will only be registered
	 * under a single class name.
	 *
	 * @see #registerService(java.lang.String[], java.lang.Object, java.util.Dictionary)
	 */
	public org.osgi.framework.ServiceRegistration registerService(String clazz, Object service, Dictionary properties) {
		String[] clazzes = new String[] { clazz };

		return (registerService(clazzes, service, properties));
	}

	/**
	 * Returns a list of <tt>ServiceReference</tt> objects. This method returns a list of
	 * <tt>ServiceReference</tt> objects for services which implement and were registered under
	 * the specified class and match the specified filter criteria.
	 *
	 * <p>The list is valid at the time of the call to this method, however as the Framework is
	 * a very dynamic environment, services can be modified or unregistered at anytime.
	 *
	 * <p><tt>filter</tt> is used to select the registered service whose
	 * properties objects contain keys and values which satisfy the filter.
	 * See {@link Filter}for a description of the filter string syntax.
	 *
	 * <p>If <tt>filter</tt> is <tt>null</tt>, all registered services
	 * are considered to match the filter.
	 * <p>If <tt>filter</tt> cannot be parsed, an {@link InvalidSyntaxException}will
	 * be thrown with a human readable message where the filter became unparsable.
	 *
	 * <p>The following steps are required to select a service:
	 * <ol>
	 * <li>If the Java Runtime Environment supports permissions, the caller is checked for the
	 * <tt>ServicePermission</tt> to get the service with the specified class.
	 * If the caller does not have the correct permission, <tt>null</tt> is returned.
	 * <li>If the filter string is not <tt>null</tt>, the filter string is
	 * parsed and the set of registered services which satisfy the filter is
	 * produced.
	 * If the filter string is <tt>null</tt>, then all registered services
	 * are considered to satisfy the filter.
	 * <li>If <code>clazz</code> is not <tt>null</tt>, the set is further reduced to
	 * those services which are an <tt>instanceof</tt> and were registered under the specified class.
	 * The complete list of classes of which a service is an instance and which
	 * were specified when the service was registered is available from the
	 * service's {@link Constants#OBJECTCLASS}property.
	 * <li>An array of <tt>ServiceReference</tt> to the selected services is returned.
	 * </ol>
	 *
	 * @param clazz The class name with which the service was registered, or
	 * <tt>null</tt> for all services.
	 * @param filter The filter criteria.
	 * @return An array of <tt>ServiceReference</tt> objects, or
	 * <tt>null</tt> if no services are registered which satisfy the search.
	 * @exception InvalidSyntaxException If <tt>filter</tt> contains
	 * an invalid filter string which cannot be parsed.
	 */
	public org.osgi.framework.ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		checkValid();

		if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
			Debug.println("getServiceReferences(" + clazz + ", \"" + filter + "\")");
		}

		return (framework.getServiceReferences(clazz, filter));
	}

	/**
	 * Get a service reference.
	 * Retrieves a {@link ServiceReference} for a service
	 * which implements the named class.
	 *
	 * <p>This reference is valid at the time
	 * of the call to this method, but since the framework is a very dynamic
	 * environment, services can be modified or unregistered at anytime.
	 *
	 * <p>This method is provided as a convenience for when the caller is
	 * interested in any service which implements a named class. This method is
	 * the same as calling {@link #getServiceReferences getServiceReferences}
	 * with a <code>null</code> filter string but only a single {@link ServiceReference}
	 * is returned.
	 *
	 * @param clazz The class name which the service must implement.
	 * @return A {@link ServiceReference} object, or <code>null</code>
	 * if no services are registered which implement the named class.
	 * @see #getServiceReferences
	 */
	public org.osgi.framework.ServiceReference getServiceReference(String clazz) {
		checkValid();

		if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
			Debug.println("getServiceReference(" + clazz + ")");
		}

		try {
			ServiceReference[] references = framework.getServiceReferences(clazz, null);

			if (references != null) {
				int index = 0;

				int length = references.length;

				if (length > 1) /* if more than one service, select highest ranking */ {
					int rankings[] = new int[length];
					int count = 0;
					int maxRanking = Integer.MIN_VALUE;

					for (int i = 0; i < length; i++) {
						int ranking = references[i].getRanking();

						rankings[i] = ranking;

						if (ranking > maxRanking) {
							index = i;
							maxRanking = ranking;
							count = 1;
						} else {
							if (ranking == maxRanking) {
								count++;
							}
						}
					}

					if (count > 1) /* if still more than one service, select lowest id */ {
						long minId = Long.MAX_VALUE;

						for (int i = 0; i < length; i++) {
							if (rankings[i] == maxRanking) {
								long id = references[i].getId();

								if (id < minId) {
									index = i;
									minId = id;
								}
							}
						}
					}
				}

				return (references[index]);
			}
		} catch (InvalidSyntaxException e) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("InvalidSyntaxException w/ null filter" + e.getMessage());
				Debug.printStackTrace(e);
			}
		}

		return (null);
	}

	/**
	 * Get a service's service object.
	 * Retrieves the service object for a service.
	 * A bundle's use of a service is tracked by a
	 * use count. Each time a service's service object is returned by
	 * {@link #getService}, the context bundle's use count for the service
	 * is incremented by one. Each time the service is release by
	 * {@link #ungetService}, the context bundle's use count
	 * for the service is decremented by one.
	 * When a bundle's use count for a service
	 * drops to zero, the bundle should no longer use the service.
	 * See {@link #getBundle()} for a definition of context bundle.
	 *
	 * <p>This method will always return <code>null</code> when the
	 * service associated with this reference has been unregistered.
	 *
	 * <p>The following steps are followed to get the service object:
	 * <ol>
	 * <li>If the service has been unregistered,
	 * <code>null</code> is returned.
	 * <li>The context bundle's use count for this service is incremented by one.
	 * <li>If the context bundle's use count for the service is now one and
	 * the service was registered with a {@link ServiceFactory},
	 * the {@link ServiceFactory#getService ServiceFactory.getService} method
	 * is called to create a service object for the context bundle.
	 * This service object is cached by the framework.
	 * While the context bundle's use count for the service is greater than zero,
	 * subsequent calls to get the services's service object for the context bundle
	 * will return the cached service object.
	 * <br>If the service object returned by the {@link ServiceFactory}
	 * is not an <code>instanceof</code>
	 * all the classes named when the service was registered or
	 * the {@link ServiceFactory} throws an exception,
	 * <code>null</code> is returned and a
	 * {@link FrameworkEvent} of type {@link FrameworkEvent#ERROR} is broadcast.
	 * <li>The service object for the service is returned.
	 * </ol>
	 *
	 * @param reference A reference to the service whose service object is desired.
	 * @return A service object for the service associated with this
	 * reference, or <code>null</code> if the service is not registered.
	 * @exception java.lang.SecurityException If the caller does not have
	 * {@link ServicePermission} permission to "get" the service
	 * using at least one of the named classes the service was registered under
	 * and the Java runtime environment supports permissions.
	 * @exception java.lang.IllegalStateException
	 * If the context bundle {@link <a href="#context_valid">has stopped</a>}.
	 * @see #ungetService
	 * @see ServiceFactory
	 */
	public Object getService(org.osgi.framework.ServiceReference reference) {
		checkValid();

		if (servicesInUse == null) {
			synchronized (contextLock) {
				if (servicesInUse == null) {
					servicesInUse = new Hashtable(17);	//TODO Interesting size...
				}
			}
		}

		ServiceRegistration registration = ((ServiceReference) reference).registration;

		framework.checkGetServicePermission(registration.clazzes);

		return registration.getService(BundleContext.this);
	}

	/**
	 * Unget a service's service object.
	 * Releases the service object for a service.
	 * If the context bundle's use count for the service is zero, this method
	 * returns <code>false</code>. Otherwise, the context bundle's use count for the
	 * service is decremented by one.
	 * See {@link #getBundle()} for a definition of context bundle.
	 *
	 * <p>The service's service object
	 * should no longer be used and all references to it should be destroyed
	 * when a bundle's use count for the service
	 * drops to zero.
	 *
	 * <p>The following steps are followed to unget the service object:
	 * <ol>
	 * <li>If the context bundle's use count for the service is zero or
	 * the service has been unregistered,
	 * <code>false</code> is returned.
	 * <li>The context bundle's use count for this service is decremented by one.
	 * <li>If the context bundle's use count for the service is now zero and
	 * the service was registered with a {@link ServiceFactory},
	 * the {@link ServiceFactory#ungetService ServiceFactory.ungetService} method
	 * is called to release the service object for the context bundle.
	 * <li><code>true</code> is returned.
	 * </ol>
	 *
	 * @param reference A reference to the service to be released.
	 * @return <code>false</code> if the context bundle's use count for the service
	 *         is zero or if the service has been unregistered,
	 *         otherwise <code>true</code>.
	 * @exception java.lang.IllegalStateException
	 * If the context bundle {@link <a href="#context_valid">has stopped</a>}.
	 * @see #getService
	 * @see ServiceFactory
	 */
	public boolean ungetService(org.osgi.framework.ServiceReference reference) {
		checkValid();

		ServiceRegistration registration = ((ServiceReference) reference).registration;

		return registration.ungetService(BundleContext.this);
	}

	/**
	 * Creates a <code>File</code> object for a file in the
	 * persistent storage area provided for the bundle by the framework.
	 * If the adaptor does not have file system support, this method will
	 * return <code>null</code>.
	 *
	 * <p>A <code>File</code> object for the base directory of the
	 * persistent storage area provided for the context bundle by the framework
	 * can be obtained by calling this method with the empty string ("")
	 * as the parameter.
	 * See {@link #getBundle()} for a definition of context bundle.
	 *
	 * <p>If the Java runtime environment supports permissions,
	 * the framework the will ensure that the bundle has
	 * <code>java.io.FilePermission</code> with actions
	 * "read","write","execute","delete" for all files (recursively) in the
	 * persistent storage area provided for the context bundle by the framework.
	 *
	 * @param filename A relative name to the file to be accessed.
	 * @return A <code>File</code> object that represents the requested file or
	 * <code>null</code> if the adaptor does not have file system support.
	 * @exception java.lang.IllegalStateException
	 * If the context bundle {@link <a href="#context_valid">has stopped</a>}.
	 */
	public File getDataFile(String filename) {
		checkValid();

		return (framework.getDataFile(bundle, filename));
	}

	/**
	 * Call bundle's BundleActivator.start()
	 * This method is called by Bundle.startWorker to start the bundle.
	 *
	 * @exception org.osgi.framework.BundleException if
	 *            the bundle has a class that implements the BundleActivator interface,
	 *            but Framework couldn't instantiate it, or the BundleActivator.start()
	 *            method failed
	 */
	protected void start() throws BundleException {
		activator = bundle.loadBundleActivator();

		if (activator != null) {
			try {
				startActivator(activator);
			} catch (BundleException be) {
				activator = null;
				throw be;
			}
		}

		/* activator completed successfully. We must use this
		   same activator object when we stop this bundle. */
	}

	/**
	 * Calls the start method of a BundleActivator.
	 * @param bundleActivator that activator to start
	 */
	protected void startActivator(final BundleActivator bundleActivator) throws BundleException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws Exception {
					if (bundleActivator != null) {
						/* Start the bundle synchronously */
						bundleActivator.start(BundleContext.this);
					}
					return null;
				}
			});
		} catch (Throwable t) {
			if (t instanceof PrivilegedActionException) {
				t = ((PrivilegedActionException) t).getException();
			}

			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.printStackTrace(t);
			}

			String clazz = null;
			clazz = bundleActivator.getClass().getName();

			throw new BundleException(Msg.formatter.getString("BUNDLE_ACTIVATOR_EXCEPTION", new Object[] { clazz, "start", bundle.getSymbolicName() == null ? "" + bundle.getBundleId() : bundle.getSymbolicName() }), t);
		}

	}

	/**
	 * Call bundle's BundleActivator.stop()
	 * This method is called by Bundle.stopWorker to stop the bundle.
	 *
	 * @exception org.osgi.framework.BundleException if
	 *            the bundle has a class that implements the BundleActivator interface,
	 *            and the BundleActivator.stop() method failed
	 */
	protected void stop() throws BundleException {
		try {
			AccessController.doPrivileged(new PrivilegedExceptionAction() {
				public Object run() throws Exception {
					if (activator != null) {
						/* Stop the bundle synchronously */
						activator.stop(BundleContext.this);
					}
					return null;
				}
			});
		} catch (Throwable t) {
			if (t instanceof PrivilegedActionException) {
				t = ((PrivilegedActionException) t).getException();
			}

			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.printStackTrace(t);
			}

			String clazz = (activator == null) ? "" : activator.getClass().getName();

			throw new BundleException(Msg.formatter.getString("BUNDLE_ACTIVATOR_EXCEPTION", new Object[] { clazz, "stop", bundle.getSymbolicName() == null ? "" + bundle.getBundleId() : bundle.getSymbolicName() }, t));
		} finally {
			activator = null;
		}
	}

	/**
	 * Provides a list of {@link ServiceReference}s for the services
	 * registered by this bundle
	 * or <code>null</code> if the bundle has no registered
	 * services.
	 *
	 * <p>The list is valid at the time
	 * of the call to this method, but the framework is a very dynamic
	 * environment and services can be modified or unregistered at anytime.
	 *
	 * @return An array of {@link ServiceReference} or <code>null</code>.
	 * @exception java.lang.IllegalStateException If the
	 * bundle has been uninstalled.
	 * @see ServiceRegistration
	 * @see ServiceReference
	 */
	protected ServiceReference[] getRegisteredServices() {
		ServiceReference[] references = null;

		synchronized (framework.serviceRegistry) {
			Vector services = framework.serviceRegistry.lookupServiceReferences(this);
			if (services == null) {
				return null;
			}

			for (int i = services.size() - 1; i >= 0; i--) {
				ServiceReference ref = (ServiceReference) services.elementAt(i);
				String[] classes = ref.getClasses();
				try { /* test for permission to the classes */
					framework.checkGetServicePermission(classes);
				} catch (SecurityException se) {
					services.removeElementAt(i);
				}
			}

			if (services.size() > 0) {
				references = new ServiceReference[services.size()];
				services.toArray(references);
			}
		}
		return (references);

	}

	/**
	 * Provides a list of {@link ServiceReference}s for the
	 * services this bundle is using,
	 * or <code>null</code> if the bundle is not using any services.
	 * A bundle is considered to be using a service if the bundle's
	 * use count for the service is greater than zero.
	 *
	 * <p>The list is valid at the time
	 * of the call to this method, but the framework is a very dynamic
	 * environment and services can be modified or unregistered at anytime.
	 *
	 * @return An array of {@link ServiceReference} or <code>null</code>.
	 * @exception java.lang.IllegalStateException If the
	 * bundle has been uninstalled.
	 * @see ServiceReference
	 */
	protected ServiceReference[] getServicesInUse() {
		if (servicesInUse == null) {
			return (null);
		}

		synchronized (servicesInUse) {
			int size = servicesInUse.size();

			if (size == 0) {
				return (null);
			}

			ServiceReference[] references = new ServiceReference[size];
			int refcount = 0;

			Enumeration enum = servicesInUse.keys();

			for (int i = 0; i < size; i++) {
				ServiceReference reference = (ServiceReference) enum.nextElement();

				try {
					framework.checkGetServicePermission(reference.registration.clazzes);
				} catch (SecurityException se) {
					continue;
				}

				references[refcount] = reference;
				refcount++;
			}

			if (refcount < size) {
				if (refcount == 0) {
					return (null);
				}

				ServiceReference[] refs = references;
				references = new ServiceReference[refcount];

				System.arraycopy(refs, 0, references, 0, refcount);
			}

			return (references);
		}
	}

	/**
	 * Bottom level event dispatcher for the BundleContext.
	 *
	 * @param originalListener listener object registered under.
	 * @param l listener to call (may be filtered).
	 * @param action Event class type
	 * @param object Event object
	 */
	public void dispatchEvent(Object originalListener, Object l, int action, Object object) {
		// save the bundle ref to a local variable 
		// to avoid interference from another thread closing this context
		Bundle tmpBundle = bundle;
		try {
			if (isValid()) /* if context still valid */ {
				switch (action) {
					case Framework.BUNDLEEVENT :
					case Framework.BUNDLEEVENTSYNC :
						{
							BundleListener listener = (BundleListener) l;

							if (Debug.DEBUG && Debug.DEBUG_EVENTS) {
								String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(listener.hashCode());
								Debug.println("dispatchBundleEvent[" + tmpBundle + "](" + listenerName + ")");
							}

							listener.bundleChanged((BundleEvent) object);
							break;
						}

					case Framework.SERVICEEVENT :
						{
							ServiceEvent event = (ServiceEvent) object;

							if (hasListenServicePermission(event)) {
								ServiceListener listener = (ServiceListener) l;

								if (Debug.DEBUG && Debug.DEBUG_EVENTS) {
									String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(listener.hashCode());
									Debug.println("dispatchServiceEvent[" + tmpBundle + "](" + listenerName + ")");
								}

								listener.serviceChanged(event);
							}

							break;
						}

					case Framework.FRAMEWORKEVENT :
						{
							FrameworkListener listener = (FrameworkListener) l;

							if (Debug.DEBUG && Debug.DEBUG_EVENTS) {
								String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(listener.hashCode());
								Debug.println("dispatchFrameworkEvent[" + tmpBundle + "](" + listenerName + ")");
							}

							listener.frameworkEvent((FrameworkEvent) object);
							break;
						}
				}
			}
		} catch (Throwable t) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("Exception in bottom level event dispatcher: " + t.getMessage());
				Debug.printStackTrace(t);
			}

			publisherror : {
				if (action == Framework.FRAMEWORKEVENT) {
					FrameworkEvent event = (FrameworkEvent) object;
					if (event.getType() == FrameworkEvent.ERROR) {
						break publisherror; // avoid infinite loop
					}
				}

				framework.publishFrameworkEvent(FrameworkEvent.ERROR, tmpBundle, t);
			}
		}
	}

	/**
	 * Check for permission to listen to a service.
	 */
	protected boolean hasListenServicePermission(ServiceEvent event) {
		ProtectionDomain domain = bundle.getProtectionDomain();

		if (domain != null) {
			ServiceReference reference = (ServiceReference) event.getServiceReference();

			String[] names = reference.registration.clazzes;

			int len = names.length;

			for (int i = 0; i < len; i++) {
				if (domain.implies(new ServicePermission(names[i], ServicePermission.GET))) {
					return true;
				}
			}

			return false;
		}

		return (true);
	}

	/**
	 * Construct a Filter object. This filter object may be used
	 * to match a ServiceReference or a Dictionary.
	 * See Filter
	 * for a description of the filter string syntax.
	 *
	 * @param filter The filter string.
	 * @return A Filter object encapsulating the filter string.
	 * @exception InvalidSyntaxException If the filter parameter contains
	 * an invalid filter string which cannot be parsed.
	 */
	public org.osgi.framework.Filter createFilter(String filter) throws InvalidSyntaxException {
		checkValid();

		return (new Filter(filter));
	}

	/**
	 * This method checks that the context is still valid. If the context is
	 * no longer valid, an IllegalStateException is thrown.
	 *
	 * @exception java.lang.IllegalStateException
	 * If the context bundle has stopped.
	 */
	protected void checkValid() {
		if (!isValid()) {
			throw new IllegalStateException(Msg.formatter.getString("BUNDLE_CONTEXT_INVALID_EXCEPTION"));
		}
	}
	
	/**
	 * This method checks that the context is still valid. 
	 *
	 * @return true if the context is still valid; false otherwise
	 */
	protected boolean isValid()
	{
		return valid;
	}
}
