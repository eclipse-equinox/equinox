/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.serviceregistry;

import java.security.*;
import java.util.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.eventmgr.*;
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.framework.Constants;
import org.osgi.framework.hooks.service.*;

/**
 * The Service Registry. This class is the main control point for service 
 * layer operations in the framework.
 * 
 * @ThreadSafe
 */
public class ServiceRegistry {
	public static final int SERVICEEVENT = 3;

	private static final String findHookName = FindHook.class.getName();
	private static final String eventHookName = EventHook.class.getName();
	private static final String listenerHookName = ListenerHook.class.getName();

	/** Published services by class name. 
	 * Map&lt;String,List&lt;ServiceRegistrationImpl&gt;&gt;
	 * The List&lt;ServiceRegistrationImpl&gt;s are sorted.
	 */
	/* @GuardedBy("this") */
	private final Map/*<String,List<ServiceRegistrationImpl>>*/publishedServicesByClass;
	/** All published services. 
	 * List&lt;ServiceRegistrationImpl&gt;.
	 * The List&lt;ServiceRegistrationImpl&gt;s are sorted.
	 */
	/* @GuardedBy("this") */
	private final List/*<ServiceRegistrationImpl>*/allPublishedServices;
	/** Published services by BundleContextImpl.  
	 * Map&lt;BundleContextImpl,List&lt;ServiceRegistrationImpl&gt;&gt;.
	 * The List&lt;ServiceRegistrationImpl&gt;s are NOT sorted.
	 */
	/* @GuardedBy("this") */
	private final Map/*<BundleContextImpl,List<ServiceRegistrationImpl>>*/publishedServicesByContext;
	/** next free service id. */
	/* @GuardedBy("this") */
	private long serviceid;
	/** Active Service Listeners.
	 * Map&lt;BundleContextImpl,CopyOnWriteIdentityMap&lt;ServiceListener,FilteredServiceListener&gt;&gt;.
	 */
	/* @GuardedBy("serviceEventListeners") */
	private final Map/*<BundleContextImpl,CopyOnWriteIdentityMap<ServiceListener,FilteredServiceListener>>*/serviceEventListeners;

	/** initial capacity of the main data structure */
	private static final int initialCapacity = 50;
	/** initial capacity of the nested data structure */
	private static final int initialSubCapacity = 10;
	/** framework which created this service registry */
	private final Framework framework;

	/**
	 * Initializes the internal data structures of this ServiceRegistry.
	 *
	 */
	public ServiceRegistry(Framework framework) {
		this.framework = framework;
		serviceid = 1;
		publishedServicesByClass = new HashMap(initialCapacity);
		publishedServicesByContext = new HashMap(initialCapacity);
		allPublishedServices = new ArrayList(initialCapacity);
		serviceEventListeners = new HashMap(initialCapacity);
	}

	/**
	 * Registers the specified service object with the specified properties
	 * under the specified class names into the Framework. A
	 * <code>ServiceRegistrationImpl</code> object is returned. The
	 * <code>ServiceRegistrationImpl</code> object is for the private use of the
	 * bundle registering the service and should not be shared with other
	 * bundles. The registering bundle is defined to be the context bundle.
	 * Other bundles can locate the service by using either the
	 * {@link #getServiceReferences} or {@link #getServiceReference} method.
	 * 
	 * <p>
	 * A bundle can register a service object that implements the
	 * {@link ServiceFactory} interface to have more flexibility in providing
	 * service objects to other bundles.
	 * 
	 * <p>
	 * The following steps are required to register a service:
	 * <ol>
	 * <li>If <code>service</code> is not a <code>ServiceFactory</code>,
	 * an <code>IllegalArgumentException</code> is thrown if
	 * <code>service</code> is not an <code>instanceof</code> all the
	 * classes named.
	 * <li>The Framework adds these service properties to the specified
	 * <code>Dictionary</code> (which may be <code>null</code>): a property
	 * named {@link Constants#SERVICE_ID} identifying the registration number of
	 * the service and a property named {@link Constants#OBJECTCLASS} containing
	 * all the specified classes. If any of these properties have already been
	 * specified by the registering bundle, their values will be overwritten by
	 * the Framework.
	 * <li>The service is added to the Framework service registry and may now
	 * be used by other bundles.
	 * <li>A service event of type {@link ServiceEvent#REGISTERED} is fired.
	 * <li>A <code>ServiceRegistration</code> object for this registration is
	 * returned.
	 * </ol>
	 * 
	 * @param context The BundleContext of the registering bundle.
	 * @param clazzes The class names under which the service can be located.
	 *        The class names in this array will be stored in the service's
	 *        properties under the key {@link Constants#OBJECTCLASS}.
	 * @param service The service object or a <code>ServiceFactory</code>
	 *        object.
	 * @param properties The properties for this service. The keys in the
	 *        properties object must all be <code>String</code> objects. See
	 *        {@link Constants} for a list of standard service property keys.
	 *        Changes should not be made to this object after calling this
	 *        method. To update the service's properties the
	 *        {@link ServiceRegistration#setProperties} method must be called.
	 *        The set of properties may be <code>null</code> if the service
	 *        has no properties.
	 * 
	 * @return A <code>ServiceRegistrationImpl</code> object for use by the bundle
	 *         registering the service to update the service's properties or to
	 *         unregister the service.
	 * 
	 * @throws java.lang.IllegalArgumentException If one of the following is
	 *         true:
	 *         <ul>
	 *         <li><code>service</code> is <code>null</code>.
	 *         <li><code>service</code> is not a <code>ServiceFactory</code>
	 *         object and is not an instance of all the named classes in
	 *         <code>clazzes</code>.
	 *         <li><code>properties</code> contains case variants of the same
	 *         key name.
	 *         </ul>
	 * 
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         <code>ServicePermission</code> to register the service for all
	 *         the named classes and the Java Runtime Environment supports
	 *         permissions.
	 * 
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * 
	 * @see ServiceRegistration
	 * @see ServiceFactory
	 */
	public ServiceRegistrationImpl registerService(BundleContextImpl context, String[] clazzes, Object service, Dictionary properties) {
		if (service == null) {
			if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
				Debug.println("Service object is null"); //$NON-NLS-1$
			}

			throw new IllegalArgumentException(Msg.SERVICE_ARGUMENT_NULL_EXCEPTION);
		}

		int size = clazzes.length;

		if (size == 0) {
			if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
				Debug.println("Classes array is empty"); //$NON-NLS-1$
			}

			throw new IllegalArgumentException(Msg.SERVICE_EMPTY_CLASS_LIST_EXCEPTION);
		}

		/* copy the array so that changes to the original will not affect us. */
		List copy = new ArrayList(size);
		// intern the strings and remove duplicates
		for (int i = 0; i < size; i++) {
			String clazz = clazzes[i].intern();
			if (!copy.contains(clazz)) {
				copy.add(clazz);
			}
		}
		size = copy.size();
		clazzes = (String[]) copy.toArray(new String[size]);

		/* check for ServicePermissions. */
		checkRegisterServicePermission(clazzes);

		if (!(service instanceof ServiceFactory)) {
			String invalidService = checkServiceClass(clazzes, service);
			if (invalidService != null) {
				if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
					Debug.println("Service object is not an instanceof " + invalidService); //$NON-NLS-1$
				}
				throw new IllegalArgumentException(NLS.bind(Msg.SERVICE_NOT_INSTANCEOF_CLASS_EXCEPTION, invalidService));
			}
		}

		ServiceRegistrationImpl registration = new ServiceRegistrationImpl(this, context, clazzes, service);
		registration.register(properties);
		if (copy.contains(listenerHookName)) {
			notifyNewListenerHook(registration);
		}
		return registration;
	}

	/**
	 * Returns an array of <code>ServiceReferenceImpl</code> objects. The returned
	 * array of <code>ServiceReferenceImpl</code> objects contains services that
	 * were registered under the specified class, match the specified filter
	 * criteria, and the packages for the class names under which the services
	 * were registered match the context bundle's packages as defined in
	 * {@link ServiceReference#isAssignableTo(Bundle, String)}.
	 * 
	 * <p>
	 * The list is valid at the time of the call to this method, however since
	 * the Framework is a very dynamic environment, services can be modified or
	 * unregistered at anytime.
	 * 
	 * <p>
	 * <code>filter</code> is used to select the registered service whose
	 * properties objects contain keys and values which satisfy the filter. See
	 * {@link Filter} for a description of the filter string syntax.
	 * 
	 * <p>
	 * If <code>filter</code> is <code>null</code>, all registered services
	 * are considered to match the filter. If <code>filter</code> cannot be
	 * parsed, an {@link InvalidSyntaxException} will be thrown with a human
	 * readable message where the filter became unparsable.
	 * 
	 * <p>
	 * The following steps are required to select a set of
	 * <code>ServiceReferenceImpl</code> objects:
	 * <ol>
	 * <li>If the filter string is not <code>null</code>, the filter string
	 * is parsed and the set <code>ServiceReferenceImpl</code> objects of
	 * registered services that satisfy the filter is produced. If the filter
	 * string is <code>null</code>, then all registered services are
	 * considered to satisfy the filter.
	 * <li>If the Java Runtime Environment supports permissions, the set of
	 * <code>ServiceReferenceImpl</code> objects produced by the previous step is
	 * reduced by checking that the caller has the
	 * <code>ServicePermission</code> to get at least one of the class names
	 * under which the service was registered. If the caller does not have the
	 * correct permission for a particular <code>ServiceReferenceImpl</code>
	 * object, then it is removed from the set.
	 * <li>If <code>clazz</code> is not <code>null</code>, the set is
	 * further reduced to those services that are an <code>instanceof</code>
	 * and were registered under the specified class. The complete list of
	 * classes of which a service is an instance and which were specified when
	 * the service was registered is available from the service's
	 * {@link Constants#OBJECTCLASS} property.
	 * <li>The set is reduced one final time by cycling through each
	 * <code>ServiceReference</code> object and calling
	 * {@link ServiceReference#isAssignableTo(Bundle, String)} with the context
	 * bundle and each class name under which the <code>ServiceReference</code>
	 * object was registered. For any given <code>ServiceReferenceImpl</code>
	 * object, if any call to
	 * {@link ServiceReference#isAssignableTo(Bundle, String)} returns
	 * <code>false</code>, then it is removed from the set of
	 * <code>ServiceReferenceImpl</code> objects.
	 * <li>An array of the remaining <code>ServiceReferenceImpl</code> objects is
	 * returned.
	 * </ol>
	 * 
	 * @param context The BundleContext of the requesting bundle.
	 * @param clazz The class name with which the service was registered or
	 *        <code>null</code> for all services.
	 * @param filterstring The filter criteria.
	 * @param allservices True if the bundle called getAllServiceReferences.
	 * @return An array of <code>ServiceReferenceImpl</code> objects or
	 *         <code>null</code> if no services are registered which satisfy
	 *         the search.
	 * @throws InvalidSyntaxException If <code>filter</code> contains an
	 *         invalid filter string that cannot be parsed.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 */
	public ServiceReferenceImpl[] getServiceReferences(final BundleContextImpl context, final String clazz, final String filterstring, final boolean allservices) throws InvalidSyntaxException {
		if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
			Debug.println((allservices ? "getAllServiceReferences(" : "getServiceReferences(") + clazz + ", \"" + filterstring + "\")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		Filter filter = (filterstring == null) ? null : context.createFilter(filterstring);
		List references = changeRegistrationsToReferences(lookupServiceRegistrations(clazz, filter));
		for (Iterator iter = references.iterator(); iter.hasNext();) {
			ServiceReferenceImpl reference = (ServiceReferenceImpl) iter.next();
			if (allservices || isAssignableTo(context, reference)) {
				try { /* test for permission to get the service */
					checkGetServicePermission(reference);
				} catch (SecurityException se) {
					iter.remove();
				}
			} else {
				iter.remove();
			}
		}

		final Collection shrinkable = new ShrinkableCollection(references);
		notifyFindHooks(context, clazz, filterstring, allservices, shrinkable);

		int size = references.size();
		if (size == 0) {
			return null;
		}
		return (ServiceReferenceImpl[]) references.toArray(new ServiceReferenceImpl[size]);
	}

	/**
	 * Returns a <code>ServiceReference</code> object for a service that
	 * implements and was registered under the specified class.
	 * 
	 * <p>
	 * This <code>ServiceReference</code> object is valid at the time of the
	 * call to this method, however as the Framework is a very dynamic
	 * environment, services can be modified or unregistered at anytime.
	 * 
	 * <p>
	 * This method is the same as calling
	 * {@link BundleContext#getServiceReferences(String, String)} with a
	 * <code>null</code> filter string. It is provided as a convenience for
	 * when the caller is interested in any service that implements the
	 * specified class.
	 * <p>
	 * If multiple such services exist, the service with the highest ranking (as
	 * specified in its {@link Constants#SERVICE_RANKING} property) is returned.
	 * <p>
	 * If there is a tie in ranking, the service with the lowest service ID (as
	 * specified in its {@link Constants#SERVICE_ID} property); that is, the
	 * service that was registered first is returned.
	 * 
	 * @param context The BundleContext of the requesting bundle.
	 * @param clazz The class name with which the service was registered.
	 * @return A <code>ServiceReference</code> object, or <code>null</code>
	 *         if no services are registered which implement the named class.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 */
	public ServiceReferenceImpl getServiceReference(BundleContextImpl context, String clazz) {
		if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
			Debug.println("getServiceReference(" + clazz + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		try {
			ServiceReferenceImpl[] references = getServiceReferences(context, clazz, null, false);

			if (references != null) {
				// Since we maintain the registrations in a sorted List, the first element is always the
				// correct one to return.
				return references[0];
			}
		} catch (InvalidSyntaxException e) {
			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				Debug.println("InvalidSyntaxException w/ null filter" + e.getMessage()); //$NON-NLS-1$
				Debug.printStackTrace(e);
			}
		}

		return null;
	}

	/**
	 * Returns the specified service object for a service.
	 * <p>
	 * A bundle's use of a service is tracked by the bundle's use count of that
	 * service. Each time a service's service object is returned by
	 * {@link #getService(ServiceReference)} the context bundle's use count for
	 * that service is incremented by one. Each time the service is released by
	 * {@link #ungetService(ServiceReference)} the context bundle's use count
	 * for that service is decremented by one.
	 * <p>
	 * When a bundle's use count for a service drops to zero, the bundle should
	 * no longer use that service.
	 * 
	 * <p>
	 * This method will always return <code>null</code> when the service
	 * associated with this <code>reference</code> has been unregistered.
	 * 
	 * <p>
	 * The following steps are required to get the service object:
	 * <ol>
	 * <li>If the service has been unregistered, <code>null</code> is
	 * returned.
	 * <li>The context bundle's use count for this service is incremented by
	 * one.
	 * <li>If the context bundle's use count for the service is currently one
	 * and the service was registered with an object implementing the
	 * <code>ServiceFactory</code> interface, the
	 * {@link ServiceFactory#getService(Bundle, ServiceRegistration)} method is
	 * called to create a service object for the context bundle. This service
	 * object is cached by the Framework. While the context bundle's use count
	 * for the service is greater than zero, subsequent calls to get the
	 * services's service object for the context bundle will return the cached
	 * service object. <br>
	 * If the service object returned by the <code>ServiceFactory</code>
	 * object is not an <code>instanceof</code> all the classes named when the
	 * service was registered or the <code>ServiceFactory</code> object throws
	 * an exception, <code>null</code> is returned and a Framework event of
	 * type {@link FrameworkEvent#ERROR} containing a {@link ServiceException}
	 * describing the error is fired.
	 * <li>The service object for the service is returned.
	 * </ol>
	 * 
	 * @param context The BundleContext of the requesting bundle.
	 * @param reference A reference to the service.
	 * @return A service object for the service associated with
	 *         <code>reference</code> or <code>null</code> if the service is
	 *         not registered, the service object returned by a
	 *         <code>ServiceFactory</code> does not implement the classes
	 *         under which it was registered or the <code>ServiceFactory</code>
	 *         threw an exception.
	 * @throws java.lang.SecurityException If the caller does not have the
	 *         <code>ServicePermission</code> to get the service using at
	 *         least one of the named classes the service was registered under
	 *         and the Java Runtime Environment supports permissions.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * @see #ungetService(ServiceReference)
	 * @see ServiceFactory
	 */
	public Object getService(BundleContextImpl context, ServiceReferenceImpl reference) {
		/* test for permission to get the service */
		checkGetServicePermission(reference);

		return reference.getRegistration().getService(context);
	}

	/**
	 * Releases the service object referenced by the specified
	 * <code>ServiceReference</code> object. If the context bundle's use count
	 * for the service is zero, this method returns <code>false</code>.
	 * Otherwise, the context bundle's use count for the service is decremented
	 * by one.
	 * 
	 * <p>
	 * The service's service object should no longer be used and all references
	 * to it should be destroyed when a bundle's use count for the service drops
	 * to zero.
	 * 
	 * <p>
	 * The following steps are required to unget the service object:
	 * <ol>
	 * <li>If the context bundle's use count for the service is zero or the
	 * service has been unregistered, <code>false</code> is returned.
	 * <li>The context bundle's use count for this service is decremented by
	 * one.
	 * <li>If the context bundle's use count for the service is currently zero
	 * and the service was registered with a <code>ServiceFactory</code>
	 * object, the
	 * {@link ServiceFactory#ungetService(Bundle, ServiceRegistration, Object)}
	 * method is called to release the service object for the context bundle.
	 * <li><code>true</code> is returned.
	 * </ol>
	 * 
	 * @param context The BundleContext of the requesting bundle.
	 * @param reference A reference to the service to be released.
	 * @return <code>false</code> if the context bundle's use count for the
	 *         service is zero or if the service has been unregistered;
	 *         <code>true</code> otherwise.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 * @see #getService
	 * @see ServiceFactory
	 */
	public boolean ungetService(BundleContextImpl context, ServiceReferenceImpl reference) {
		ServiceRegistrationImpl registration = reference.getRegistration();

		return registration.ungetService(context);
	}

	/**
	 * Returns this bundle's <code>ServiceReference</code> list for all
	 * services it has registered or <code>null</code> if this bundle has no
	 * registered services.
	 * 
	 * <p>
	 * If the Java runtime supports permissions, a <code>ServiceReference</code>
	 * object to a service is included in the returned list only if the caller
	 * has the <code>ServicePermission</code> to get the service using at
	 * least one of the named classes the service was registered under.
	 * 
	 * <p>
	 * The list is valid at the time of the call to this method, however, as the
	 * Framework is a very dynamic environment, services can be modified or
	 * unregistered at anytime.
	 * 
	 * @param context The BundleContext of the requesting bundle.
	 * @return An array of <code>ServiceReference</code> objects or
	 *         <code>null</code>.
	 * @throws java.lang.IllegalStateException If this bundle has been
	 *         uninstalled.
	 * @see ServiceRegistration
	 * @see ServiceReference
	 * @see ServicePermission
	 */
	public ServiceReferenceImpl[] getRegisteredServices(BundleContextImpl context) {
		List references = changeRegistrationsToReferences(lookupServiceRegistrations(context));
		for (Iterator iter = references.iterator(); iter.hasNext();) {
			ServiceReferenceImpl reference = (ServiceReferenceImpl) iter.next();
			try { /* test for permission to get the service */
				checkGetServicePermission(reference);
			} catch (SecurityException se) {
				iter.remove();
			}
		}

		int size = references.size();
		if (size == 0) {
			return null;
		}
		return (ServiceReferenceImpl[]) references.toArray(new ServiceReferenceImpl[size]);
	}

	/**
	 * Returns this bundle's <code>ServiceReference</code> list for all
	 * services it is using or returns <code>null</code> if this bundle is not
	 * using any services. A bundle is considered to be using a service if its
	 * use count for that service is greater than zero.
	 * 
	 * <p>
	 * If the Java Runtime Environment supports permissions, a
	 * <code>ServiceReference</code> object to a service is included in the
	 * returned list only if the caller has the <code>ServicePermission</code>
	 * to get the service using at least one of the named classes the service
	 * was registered under.
	 * <p>
	 * The list is valid at the time of the call to this method, however, as the
	 * Framework is a very dynamic environment, services can be modified or
	 * unregistered at anytime.
	 * 
	 * @param context The BundleContext of the requesting bundle.
	 * @return An array of <code>ServiceReference</code> objects or
	 *         <code>null</code>.
	 * @throws java.lang.IllegalStateException If this bundle has been
	 *         uninstalled.
	 * @see ServiceReference
	 * @see ServicePermission
	 */
	public ServiceReferenceImpl[] getServicesInUse(BundleContextImpl context) {
		Map servicesInUse = context.getServicesInUseMap();
		if (servicesInUse == null) {
			return null;
		}

		List references;
		synchronized (servicesInUse) {
			if (servicesInUse.size() == 0) {
				return null;
			}
			references = changeRegistrationsToReferences(new ArrayList(servicesInUse.keySet()));
		}
		for (Iterator iter = references.iterator(); iter.hasNext();) {
			ServiceReferenceImpl reference = (ServiceReferenceImpl) iter.next();
			try { /* test for permission to get the service */
				checkGetServicePermission(reference);
			} catch (SecurityException se) {
				iter.remove();
			}
		}

		int size = references.size();
		if (size == 0) {
			return null;
		}
		return (ServiceReferenceImpl[]) references.toArray(new ServiceReferenceImpl[size]);
	}

	/**
	 * Called when the BundleContext is closing to unregister all services
	 * currently registered by the bundle.
	 * 
	 * @param context The BundleContext of the closing bundle.
	 */
	public void unregisterServices(BundleContextImpl context) {
		List registrations = lookupServiceRegistrations(context);
		for (Iterator iter = registrations.iterator(); iter.hasNext();) {
			ServiceRegistrationImpl registration = (ServiceRegistrationImpl) iter.next();
			try {
				registration.unregister();
			} catch (IllegalStateException e) {
				/* already unregistered */
			}
		}
		removeServiceRegistrations(context); // remove empty list
	}

	/**
	 * Called when the BundleContext is closing to unget all services
	 * currently used by the bundle.
	 * 
	 * @param context The BundleContext of the closing bundle.
	 */
	public void releaseServicesInUse(BundleContextImpl context) {
		Map servicesInUse = context.getServicesInUseMap();
		if (servicesInUse == null) {
			return;
		}
		List registrations;
		synchronized (servicesInUse) {
			if (servicesInUse.size() == 0) {
				return;
			}
			registrations = new ArrayList(servicesInUse.keySet());
		}
		if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
			Debug.println("Releasing services"); //$NON-NLS-1$
		}
		for (Iterator iter = registrations.iterator(); iter.hasNext();) {
			ServiceRegistrationImpl registration = (ServiceRegistrationImpl) iter.next();
			registration.releaseService(context);
		}
	}

	/**
	 * Add a new Service Listener for a bundle.
	 * 
	 * @param context Context of bundle adding listener.
	 * @param listener Service Listener to be added.
	 * @param filter Filter string for listener or null.
	 * @throws InvalidSyntaxException If the filter string is invalid.
	 */
	public void addServiceListener(BundleContextImpl context, ServiceListener listener, String filter) throws InvalidSyntaxException {
		if (Debug.DEBUG && Debug.DEBUG_EVENTS) {
			String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(listener)); //$NON-NLS-1$
			Debug.println("addServiceListener[" + context.getBundleImpl() + "](" + listenerName + ", \"" + filter + "\")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}

		FilteredServiceListener filteredListener = new FilteredServiceListener(context, listener, filter);
		FilteredServiceListener oldFilteredListener;
		synchronized (serviceEventListeners) {
			Map listeners = (Map) serviceEventListeners.get(context);
			if (listeners == null) {
				listeners = new CopyOnWriteIdentityMap();
				serviceEventListeners.put(context, listeners);
			}
			oldFilteredListener = (FilteredServiceListener) listeners.put(listener, filteredListener);
		}

		if (oldFilteredListener != null) {
			oldFilteredListener.markRemoved();
			Collection removedListeners = Collections.singletonList(oldFilteredListener);
			notifyListenerHooks(removedListeners, false);
		}

		Collection addedListeners = Collections.singletonList(filteredListener);
		notifyListenerHooks(addedListeners, true);
	}

	/**
	 * Remove a Service Listener for a bundle.
	 * 
	 * @param context Context of bundle removing listener.
	 * @param listener Service Listener to be removed.
	 */
	public void removeServiceListener(BundleContextImpl context, ServiceListener listener) {
		if (Debug.DEBUG && Debug.DEBUG_EVENTS) {
			String listenerName = listener.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(listener)); //$NON-NLS-1$
			Debug.println("removeServiceListener[" + context.getBundleImpl() + "](" + listenerName + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		FilteredServiceListener oldFilteredListener;
		synchronized (serviceEventListeners) {
			Map listeners = (Map) serviceEventListeners.get(context);
			if (listeners == null) {
				return; // this context has no listeners to begin with
			}
			oldFilteredListener = (FilteredServiceListener) listeners.remove(listener);
		}

		if (oldFilteredListener == null) {
			return;
		}
		oldFilteredListener.markRemoved();
		Collection removedListeners = Collections.singletonList(oldFilteredListener);
		notifyListenerHooks(removedListeners, false);
	}

	/**
	 * Remove all Service Listener for a bundle.
	 * 
	 * @param context Context of bundle removing all listeners.
	 */
	public void removeAllServiceListeners(BundleContextImpl context) {
		Map removedListenersMap;
		synchronized (serviceEventListeners) {
			removedListenersMap = (Map) serviceEventListeners.remove(context);
		}
		if ((removedListenersMap == null) || (removedListenersMap.size() == 0)) {
			return;
		}
		Collection removedListeners = removedListenersMap.values();
		for (Iterator iter = removedListeners.iterator(); iter.hasNext();) {
			FilteredServiceListener oldFilteredListener = (FilteredServiceListener) iter.next();
			oldFilteredListener.markRemoved();
		}
		notifyListenerHooks(removedListeners, false);
	}

	/**
	 * Deliver a ServiceEvent.
	 * 
	 * @param event The ServiceEvent to deliver.
	 */
	void publishServiceEvent(final ServiceEvent event) {
		if (System.getSecurityManager() == null) {
			publishServiceEventPrivileged(event);
		} else {
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					publishServiceEventPrivileged(event);
					return null;
				}
			});
		}
	}

	void publishServiceEventPrivileged(final ServiceEvent event) {
		/* Build the listener snapshot */
		Map /*<BundleContextImpl,Set<Map.Entry<Object,Object>>>*/listenerSnapshot;
		synchronized (serviceEventListeners) {
			listenerSnapshot = new HashMap(serviceEventListeners.size());
			for (Iterator iter = serviceEventListeners.entrySet().iterator(); iter.hasNext();) {
				Map.Entry entry = (Map.Entry) iter.next();
				BundleContextImpl context = (BundleContextImpl) entry.getKey();
				Map listeners = (Map) entry.getValue();
				if (!listeners.isEmpty()) {
					listenerSnapshot.put(context, listeners.entrySet());
				}
			}
		}

		/* shrink the snapshot.
		 * keySet returns a Collection which cannot be added to and
		 * removals from that collection will result in removals of the
		 * entry from the snapshot.
		 */
		Collection/*<BundleContextImpl>*/shrinkable = listenerSnapshot.keySet();
		notifyEventHooksPrivileged(event, shrinkable);
		if (listenerSnapshot.isEmpty()) {
			return;
		}

		/* deliver the event to the snapshot */
		ListenerQueue queue = framework.newListenerQueue();
		for (Iterator iter = listenerSnapshot.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			EventDispatcher dispatcher = (BundleContextImpl) entry.getKey();
			Set listeners = (Set) entry.getValue();
			queue.queueListeners(listeners, dispatcher);
		}
		queue.dispatchEventSynchronous(SERVICEEVENT, event);
	}

	/**
	 * Return the next available service id.
	 * 
	 * @return next service id.
	 */
	synchronized long getNextServiceId() {
		long id = serviceid;
		serviceid++;
		return id;
	}

	/**
	 * Add the ServiceRegistrationImpl to the data structure.
	 * 
	 * @param context The BundleContext of the bundle registering the service.
	 * @param registration The new ServiceRegistration.
	 */
	/* @GuardedBy("this") */
	void addServiceRegistration(BundleContextImpl context, ServiceRegistrationImpl registration) {
		// Add the ServiceRegistrationImpl to the list of Services published by BundleContextImpl.
		List contextServices = (List) publishedServicesByContext.get(context);
		if (contextServices == null) {
			contextServices = new ArrayList(initialSubCapacity);
			publishedServicesByContext.put(context, contextServices);
		}
		// The list is NOT sorted, so we just add
		contextServices.add(registration);

		// Add the ServiceRegistrationImpl to the list of Services published by Class Name.
		String[] clazzes = registration.getClasses();
		int insertIndex;
		for (int i = 0, size = clazzes.length; i < size; i++) {
			String clazz = clazzes[i];

			List services = (List) publishedServicesByClass.get(clazz);

			if (services == null) {
				services = new ArrayList(initialSubCapacity);
				publishedServicesByClass.put(clazz, services);
			}

			// The list is sorted, so we must find the proper location to insert
			insertIndex = -Collections.binarySearch(services, registration) - 1;
			services.add(insertIndex, registration);
		}

		// Add the ServiceRegistrationImpl to the list of all published Services.
		// The list is sorted, so we must find the proper location to insert
		insertIndex = -Collections.binarySearch(allPublishedServices, registration) - 1;
		allPublishedServices.add(insertIndex, registration);
	}

	/**
	 * Modify the ServiceRegistrationImpl in the data structure.
	 * 
	 * @param context The BundleContext of the bundle registering the service.
	 * @param registration The modified ServiceRegistration.
	 */
	/* @GuardedBy("this") */
	void modifyServiceRegistration(BundleContextImpl context, ServiceRegistrationImpl registration) {
		// The list of Services published by BundleContextImpl is not sorted, so
		// we do not need to modify it.

		// Remove the ServiceRegistrationImpl from the list of Services published by Class Name
		// and then add at the correct index.
		String[] clazzes = registration.getClasses();
		int insertIndex;
		for (int i = 0, size = clazzes.length; i < size; i++) {
			String clazz = clazzes[i];
			List services = (List) publishedServicesByClass.get(clazz);
			services.remove(registration);
			// The list is sorted, so we must find the proper location to insert
			insertIndex = -Collections.binarySearch(services, registration) - 1;
			services.add(insertIndex, registration);
		}

		// Remove the ServiceRegistrationImpl from the list of all published Services
		// and then add at the correct index.
		allPublishedServices.remove(registration);
		// The list is sorted, so we must find the proper location to insert
		insertIndex = -Collections.binarySearch(allPublishedServices, registration) - 1;
		allPublishedServices.add(insertIndex, registration);
	}

	/**
	 * Remove the ServiceRegistrationImpl from the data structure.
	 * 
	 * @param context The BundleContext of the bundle registering the service.
	 * @param registration The ServiceRegistration to remove.
	 */
	/* @GuardedBy("this") */
	void removeServiceRegistration(BundleContextImpl context, ServiceRegistrationImpl registration) {
		// Remove the ServiceRegistrationImpl from the list of Services published by BundleContextImpl.
		List contextServices = (List) publishedServicesByContext.get(context);
		if (contextServices != null) {
			contextServices.remove(registration);
		}

		// Remove the ServiceRegistrationImpl from the list of Services published by Class Name.
		String[] clazzes = registration.getClasses();
		for (int i = 0, size = clazzes.length; i < size; i++) {
			String clazz = clazzes[i];
			List services = (List) publishedServicesByClass.get(clazz);
			services.remove(registration);
			if (services.isEmpty()) { // remove empty list
				publishedServicesByClass.remove(clazz);
			}
		}

		// Remove the ServiceRegistrationImpl from the list of all published Services.
		allPublishedServices.remove(registration);
	}

	/**
	 * Lookup Service Registrations in the data structure by class name and filter.
	 * 
	 * @param clazz The class name with which the service was registered or
	 *        <code>null</code> for all services.
	 * @param filter The filter criteria.
	 * @return List<ServiceRegistrationImpl>
	 */
	private List lookupServiceRegistrations(String clazz, Filter filter) {
		List result;
		synchronized (this) {
			if (clazz == null) { /* all services */
				result = allPublishedServices;
			} else {
				/* services registered under the class name */
				result = (List) publishedServicesByClass.get(clazz);
			}

			if ((result == null) || (result.size() == 0)) {
				return Collections.EMPTY_LIST;
			}

			result = new ArrayList(result); /* make a new list since we don't want to change the real list */
		}

		if (filter == null) {
			return result;
		}

		for (Iterator iter = result.iterator(); iter.hasNext();) {
			ServiceRegistrationImpl registration = (ServiceRegistrationImpl) iter.next();
			if (!filter.match(registration.getReferenceImpl())) {
				iter.remove();
			}
		}
		return result;
	}

	/**
	 * Lookup Service Registrations in the data structure by BundleContext.
	 * 
	 * @param context The BundleContext for which to return Service Registrations.
	 * @return List<ServiceRegistrationImpl>
	 */
	private synchronized List lookupServiceRegistrations(BundleContextImpl context) {
		List result = (List) publishedServicesByContext.get(context);

		if ((result == null) || (result.size() == 0)) {
			return Collections.EMPTY_LIST;
		}

		return new ArrayList(result); /* make a new list since we don't want to change the real list */
	}

	/**
	 * Remove Service Registrations in the data structure by BundleContext.
	 * 
	 * @param context The BundleContext for which to remove Service Registrations.
	 */
	private synchronized void removeServiceRegistrations(BundleContextImpl context) {
		publishedServicesByContext.remove(context);
	}

	/**
	 * Modify a List<ServiceRegistrationImpl> in place to a List<ServiceReferenceImpl>.
	 * 
	 * @param result The input List<ServiceRegistrationImpl>.
	 * @return result which has been changed to List<ServiceReferenceImpl>
	 */
	private static List changeRegistrationsToReferences(List result) {
		for (ListIterator iter = result.listIterator(); iter.hasNext();) {
			ServiceRegistrationImpl registration = (ServiceRegistrationImpl) iter.next();
			iter.set(registration.getReferenceImpl()); /* replace the registration with its reference */
		}
		return result;
	}

	/**
	 * Check for permission to register a service.
	 * 
	 * The caller must have permission for ALL names.
	 */
	private static void checkRegisterServicePermission(String[] names) {
		SecurityManager sm = System.getSecurityManager();
		if (sm == null) {
			return;
		}
		for (int i = 0, len = names.length; i < len; i++) {
			sm.checkPermission(new ServicePermission(names[i], ServicePermission.REGISTER));
		}
	}

	/**
	 * Check for permission to get a service.
	 */
	private static void checkGetServicePermission(ServiceReference reference) {
		SecurityManager sm = System.getSecurityManager();
		if (sm == null) {
			return;
		}
		sm.checkPermission(new ServicePermission(reference, ServicePermission.GET));
	}

	/**
	 * Check for permission to listen to a service.
	 */
	static boolean hasListenServicePermission(ServiceEvent event, BundleContextImpl context) {
		ProtectionDomain domain = context.getBundleImpl().getProtectionDomain();
		if (domain == null) {
			return true;
		}

		return domain.implies(new ServicePermission(event.getServiceReference(), ServicePermission.GET));
	}

	/** 
	 * Return the name of the class that is not satisfied by the service object. 
	 * @param clazzes Array of class names.
	 * @param serviceObject Service object.
	 * @return The name of the class that is not satisfied by the service object.
	 */
	static String checkServiceClass(final String[] clazzes, final Object serviceObject) {
		ClassLoader cl = (ClassLoader) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return serviceObject.getClass().getClassLoader();
			}
		});
		for (int i = 0, len = clazzes.length; i < len; i++) {
			try {
				Class serviceClazz = cl == null ? Class.forName(clazzes[i]) : cl.loadClass(clazzes[i]);
				if (!serviceClazz.isInstance(serviceObject))
					return clazzes[i];
			} catch (ClassNotFoundException e) {
				//This check is rarely done
				if (extensiveCheckServiceClass(clazzes[i], serviceObject.getClass()))
					return clazzes[i];
			}
		}
		return null;
	}

	private static boolean extensiveCheckServiceClass(String clazz, Class serviceClazz) {
		if (clazz.equals(serviceClazz.getName()))
			return false;
		Class[] interfaces = serviceClazz.getInterfaces();
		for (int i = 0, len = interfaces.length; i < len; i++)
			if (!extensiveCheckServiceClass(clazz, interfaces[i]))
				return false;
		Class superClazz = serviceClazz.getSuperclass();
		if (superClazz != null)
			if (!extensiveCheckServiceClass(clazz, superClazz))
				return false;
		return true;
	}

	static boolean isAssignableTo(BundleContextImpl context, ServiceReferenceImpl reference) {
		Bundle bundle = context.getBundleImpl();
		String[] clazzes = reference.getClasses();
		for (int i = 0, len = clazzes.length; i < len; i++)
			if (!reference.isAssignableTo(bundle, clazzes[i]))
				return false;
		return true;
	}

	/**
	 * Call the registered FindHook services to allow them to inspect and possibly shrink the result.
	 * The FindHook must be called in order: descending by service.ranking, then ascending by service.id.
	 * This is the natural order for ServiceReference.
	 * 
	 * @param context The context of the bundle getting the service references.
	 * @param clazz The class name used to search for the service references.
	 * @param filterstring The filter used to search for the service references.
	 * @param allservices True if getAllServiceReferences called.
	 * @param result The result to return to the caller which may have been shrunk by the FindHooks.
	 */
	private void notifyFindHooks(final BundleContextImpl context, final String clazz, final String filterstring, final boolean allservices, final Collection result) {
		if (System.getSecurityManager() == null) {
			notifyFindHooksPrivileged(context, clazz, filterstring, allservices, result);
		} else {
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					notifyFindHooksPrivileged(context, clazz, filterstring, allservices, result);
					return null;
				}
			});
		}
	}

	void notifyFindHooksPrivileged(BundleContextImpl context, String clazz, String filterstring, boolean allservices, Collection result) {
		BundleContextImpl systemBundleContext = framework.getSystemBundleContext();
		if (systemBundleContext == null) { // if no system bundle context, we are done!
			return;
		}

		if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
			Debug.println("notifyFindHooks(" + context.getBundleImpl() + "," + clazz + "," + filterstring + "," + allservices + "," + result + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		}

		List hooks = lookupServiceRegistrations(findHookName, null);
		// Since the list is already sorted, we don't need to sort the list to call the hooks
		// in the proper order.

		for (Iterator iter = hooks.iterator(); iter.hasNext();) {
			ServiceRegistrationImpl registration = (ServiceRegistrationImpl) iter.next();
			Object findHook = registration.getService(systemBundleContext);
			if (findHook == null) { // if the hook is null
				continue;
			}
			try {
				if (findHook instanceof FindHook) { // if the hook is usable
					((FindHook) findHook).find(context, clazz, filterstring, allservices, result);
				}
			} catch (Throwable t) {
				if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
					Debug.println(findHook + ".find() exception: " + t.getMessage()); //$NON-NLS-1$
					Debug.printStackTrace(t);
				}
				// allow the adaptor to handle this unexpected error
				framework.getAdaptor().handleRuntimeError(t);
				ServiceException se = new ServiceException(NLS.bind(Msg.SERVICE_FACTORY_EXCEPTION, findHook.getClass().getName(), "find"), t); //$NON-NLS-1$ 
				framework.publishFrameworkEvent(FrameworkEvent.ERROR, registration.getBundle(), se);
			} finally {
				registration.ungetService(systemBundleContext);
			}
		}
	}

	/**
	 * Call the registered EventHook services to allow them to inspect and possibly shrink the result.
	 * The EventHooks must be called in order: descending by service.ranking, then ascending by service.id.
	 * This is the natural order for ServiceReference.
	 * 
	 * @param event The service event to be delivered.
	 * @param result The result to return to the caller which may have been shrunk by the EventHooks.
	 */
	private void notifyEventHooksPrivileged(ServiceEvent event, Collection result) {
		BundleContextImpl systemBundleContext = framework.getSystemBundleContext();
		if (systemBundleContext == null) { // if no system bundle context, we are done!
			return;
		}

		if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
			Debug.println("notifyEventHooks(" + event.getType() + ":" + event.getServiceReference() + "," + result + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
		}

		List hooks = lookupServiceRegistrations(eventHookName, null);
		// Since the list is already sorted, we don't need to sort the list to call the hooks
		// in the proper order.

		for (Iterator iter = hooks.iterator(); iter.hasNext();) {
			ServiceRegistrationImpl registration = (ServiceRegistrationImpl) iter.next();
			Object eventHook = registration.getService(systemBundleContext);
			if (eventHook == null) { // if the hook is null
				continue;
			}
			try {
				if (eventHook instanceof EventHook) { // if the hook is usable
					((EventHook) eventHook).event(event, result);
				}
			} catch (Throwable t) {
				if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
					Debug.println(eventHook + ".event() exception: " + t.getMessage()); //$NON-NLS-1$
					Debug.printStackTrace(t);
				}
				// allow the adaptor to handle this unexpected error
				framework.getAdaptor().handleRuntimeError(t);
				ServiceException se = new ServiceException(NLS.bind(Msg.SERVICE_FACTORY_EXCEPTION, eventHook.getClass().getName(), "event"), t); //$NON-NLS-1$ 
				framework.publishFrameworkEvent(FrameworkEvent.ERROR, registration.getBundle(), se);
			} finally {
				registration.ungetService(systemBundleContext);
			}
		}
	}

	/**
	 * Call a newly registered ListenerHook service to provide the current collection of
	 * service listeners.
	 * 
	 * @param registration The newly registered ListenerHook service.
	 */
	private void notifyNewListenerHook(final ServiceRegistrationImpl registration) {
		if (System.getSecurityManager() == null) {
			notifyNewListenerHookPrivileged(registration);
		} else {
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					notifyNewListenerHookPrivileged(registration);
					return null;
				}
			});
		}

	}

	void notifyNewListenerHookPrivileged(ServiceRegistrationImpl registration) {
		BundleContextImpl systemBundleContext = framework.getSystemBundleContext();
		if (systemBundleContext == null) { // if no system bundle context, we are done!
			return;
		}

		if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
			Debug.println("notifyNewListenerHook(" + registration + ")"); //$NON-NLS-1$ //$NON-NLS-2$ 
		}

		Collection addedListeners = new ArrayList(initialCapacity);
		synchronized (serviceEventListeners) {
			for (Iterator iter = serviceEventListeners.values().iterator(); iter.hasNext();) {
				Map listeners = (Map) iter.next();
				if (!listeners.isEmpty()) {
					addedListeners.addAll(listeners.values());
				}
			}
		}
		addedListeners = Collections.unmodifiableCollection(addedListeners);

		Object listenerHook = registration.getService(systemBundleContext);
		if (listenerHook == null) { // if the hook is null
			return;
		}
		try {
			if (listenerHook instanceof ListenerHook) { // if the hook is usable
				((ListenerHook) listenerHook).added(addedListeners);
			}
		} catch (Throwable t) {
			if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
				Debug.println(listenerHook + ".added() exception: " + t.getMessage()); //$NON-NLS-1$
				Debug.printStackTrace(t);
			}
			// allow the adaptor to handle this unexpected error
			framework.getAdaptor().handleRuntimeError(t);
			ServiceException se = new ServiceException(NLS.bind(Msg.SERVICE_FACTORY_EXCEPTION, listenerHook.getClass().getName(), "event"), t); //$NON-NLS-1$ 
			framework.publishFrameworkEvent(FrameworkEvent.ERROR, registration.getBundle(), se);
		} finally {
			registration.ungetService(systemBundleContext);
		}
	}

	/**
	 * Call the registered ListenerHook services to notify them of newly added or removed service listeners.
	 * The ListenerHook must be called in order: descending by service.ranking, then ascending by service.id.
	 * This is the natural order for ServiceReference.
	 * 
	 * @param listeners An unmodifiable collection of ListenerInfo objects.
	 * @param added <code>true</code> if the specified listeners are being added. <code>false</code>
	 * if they are being removed.
	 */
	private void notifyListenerHooks(final Collection listeners, final boolean added) {
		if (System.getSecurityManager() == null) {
			notifyListenerHooksPrivileged(listeners, added);
		} else {
			AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					notifyListenerHooksPrivileged(listeners, added);
					return null;
				}
			});
		}

	}

	void notifyListenerHooksPrivileged(Collection listeners, boolean added) {
		BundleContextImpl systemBundleContext = framework.getSystemBundleContext();
		if (systemBundleContext == null) { // if no system bundle context, we are done!
			return;
		}

		if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
			Debug.println("notifyListenerHooks(" + listeners + "," + (added ? "added" : "removed") + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}

		List hooks = lookupServiceRegistrations(listenerHookName, null);
		// Since the list is already sorted, we don't need to sort the list to call the hooks
		// in the proper order.

		for (Iterator iter = hooks.iterator(); iter.hasNext();) {
			ServiceRegistrationImpl registration = (ServiceRegistrationImpl) iter.next();
			Object listenerHook = registration.getService(systemBundleContext);
			if (listenerHook == null) { // if the hook is null
				continue;
			}
			try {
				if (listenerHook instanceof ListenerHook) { // if the hook is usable
					if (added) {
						((ListenerHook) listenerHook).added(listeners);
					} else {
						((ListenerHook) listenerHook).removed(listeners);
					}
				}
			} catch (Throwable t) {
				if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
					Debug.println(listenerHook + "." + (added ? "added" : "removed") + "() exception: " + t.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					Debug.printStackTrace(t);
				}
				// allow the adaptor to handle this unexpected error
				framework.getAdaptor().handleRuntimeError(t);
				ServiceException se = new ServiceException(NLS.bind(Msg.SERVICE_FACTORY_EXCEPTION, listenerHook.getClass().getName(), "event"), t); //$NON-NLS-1$ 
				framework.publishFrameworkEvent(FrameworkEvent.ERROR, registration.getBundle(), se);
			} finally {
				registration.ungetService(systemBundleContext);
			}
		}
	}
}
