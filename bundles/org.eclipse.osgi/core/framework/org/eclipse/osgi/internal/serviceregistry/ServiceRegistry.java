/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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
import org.eclipse.osgi.framework.internal.core.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.framework.Constants;

/**
 * The Service Registry. This class is the main control point for service 
 * layer operations in the framework.
 * 
 * @ThreadSafe
 */
public class ServiceRegistry {
	public static final String PROP_SCOPE_SERVICE_EVENTS = "osgi.scopeServiceEvents"; //$NON-NLS-1$
	public static final boolean scopeEvents = Boolean.valueOf(FrameworkProperties.getProperty(PROP_SCOPE_SERVICE_EVENTS, "true")).booleanValue(); //$NON-NLS-1$

	/** Published services by class name. Key is a String class name; Value is a ArrayList of ServiceRegistrations */
	/* @GuardedBy("this") */
	private final HashMap/*<String,ArrayList<ServiceRegistrationImpl>>*/publishedServicesByClass;
	/** All published services. Value is ServiceRegistrations */
	/* @GuardedBy("this") */
	private final ArrayList/*<ServiceRegistrationImpl>*/allPublishedServices;
	/** Published services by BundleContext.  Key is a BundleContext; Value is a ArrayList of ServiceRegistrations*/
	/* @GuardedBy("this") */
	private final HashMap/*<BundleContextImpl,ArrayList<ServiceRegistrationImpl>*/publishedServicesByContext;
	/** next free service id. */
	/* @GuardedBy("this") */
	private long serviceid;
	/** initial capacity of the data structure */
	private static final int initialCapacity = 50;

	/**
	 * Initializes the internal data structures of this ServiceRegistry.
	 *
	 */
	public ServiceRegistry() {
		serviceid = 1;
		publishedServicesByClass = new HashMap(initialCapacity);
		publishedServicesByContext = new HashMap(initialCapacity);
		allPublishedServices = new ArrayList(initialCapacity);
	}

	/**
	 * Registers the specified service object with the specified properties
	 * under the specified class names into the Framework. A
	 * <code>ServiceRegistration</code> object is returned. The
	 * <code>ServiceRegistration</code> object is for the private use of the
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
	 * @return A <code>ServiceRegistration</code> object for use by the bundle
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
		String[] copy = new String[clazzes.length];
		// doing this the hard way so we can intern the strings
		for (int i = clazzes.length - 1; i >= 0; i--)
			copy[i] = clazzes[i].intern();
		clazzes = copy;

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
		return registration;

	}

	/**
	 * Returns an array of <code>ServiceReference</code> objects. The returned
	 * array of <code>ServiceReference</code> objects contains services that
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
	 * <code>ServiceReference</code> objects:
	 * <ol>
	 * <li>If the filter string is not <code>null</code>, the filter string
	 * is parsed and the set <code>ServiceReference</code> objects of
	 * registered services that satisfy the filter is produced. If the filter
	 * string is <code>null</code>, then all registered services are
	 * considered to satisfy the filter.
	 * <li>If the Java Runtime Environment supports permissions, the set of
	 * <code>ServiceReference</code> objects produced by the previous step is
	 * reduced by checking that the caller has the
	 * <code>ServicePermission</code> to get at least one of the class names
	 * under which the service was registered. If the caller does not have the
	 * correct permission for a particular <code>ServiceReference</code>
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
	 * object was registered. For any given <code>ServiceReference</code>
	 * object, if any call to
	 * {@link ServiceReference#isAssignableTo(Bundle, String)} returns
	 * <code>false</code>, then it is removed from the set of
	 * <code>ServiceReference</code> objects.
	 * <li>An array of the remaining <code>ServiceReference</code> objects is
	 * returned.
	 * </ol>
	 * 
	 * @param context The BundleContext of the requesting bundle.
	 * @param clazz The class name with which the service was registered or
	 *        <code>null</code> for all services.
	 * @param filterstring The filter criteria.
	 * @param allservices True if the bundle called getAllServiceReferences.
	 * @return An array of <code>ServiceReference</code> objects or
	 *         <code>null</code> if no services are registered which satisfy
	 *         the search.
	 * @throws InvalidSyntaxException If <code>filter</code> contains an
	 *         invalid filter string that cannot be parsed.
	 * @throws java.lang.IllegalStateException If this BundleContext is no
	 *         longer valid.
	 */
	public ServiceReferenceImpl[] getServiceReferences(BundleContextImpl context, String clazz, String filterstring, boolean allservices) throws InvalidSyntaxException {
		if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
			Debug.println((allservices ? "getAllServiceReferences(" : "getServiceReferences(") + clazz + ", \"" + filterstring + "\")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		if (clazz != null) {
			try /* test for permission to get clazz */{
				checkGetServicePermission(clazz);
			} catch (SecurityException se) {
				return null;
			}
		}
		Filter filter = (filterstring == null) ? null : context.createFilter(filterstring);
		List references = null;
		synchronized (this) {
			references = lookupServiceReferences(clazz, filter);
			Iterator iter = references.iterator();
			while (iter.hasNext()) {
				ServiceReferenceImpl reference = (ServiceReferenceImpl) iter.next();
				if (allservices || isAssignableTo(context, reference)) {
					if (clazz == null) {
						try { /* test for permission to the classes */
							checkGetServicePermission(reference.getClasses());
						} catch (SecurityException se) {
							iter.remove();
						}
					}
				} else {
					iter.remove();
				}
			}
		}

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
	 * @see #getServiceReferences(String, String)
	 */
	public ServiceReferenceImpl getServiceReference(BundleContextImpl context, String clazz) {
		if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
			Debug.println("getServiceReference(" + clazz + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		try {
			ServiceReferenceImpl[] references = getServiceReferences(context, clazz, null, false);

			if (references != null) {
				int index = 0;

				int length = references.length;

				if (length > 1) /* if more than one service, select highest ranking */{
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

					if (count > 1) /* if still more than one service, select lowest id */{
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

				return references[index];
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
		ServiceRegistrationImpl registration = reference.getRegistration();

		checkGetServicePermission(registration.getClasses());

		return registration.getService(context);
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
	public synchronized ServiceReferenceImpl[] getRegisteredServices(BundleContextImpl context) {
		List references = lookupServiceReferences(context);
		ListIterator iter = references.listIterator();
		while (iter.hasNext()) {
			ServiceReferenceImpl reference = (ServiceReferenceImpl) iter.next();
			try { /* test for permission to the classes */
				checkGetServicePermission(reference.getClasses());
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
		synchronized (servicesInUse) {
			int size = servicesInUse.size();

			if (size == 0) {
				return null;
			}

			ServiceReferenceImpl[] references = new ServiceReferenceImpl[size];
			int refcount = 0;

			Iterator regsIter = servicesInUse.keySet().iterator();

			for (int i = 0; i < size; i++) {
				ServiceRegistrationImpl registration = (ServiceRegistrationImpl) regsIter.next();

				try {
					checkGetServicePermission(registration.getClasses());
				} catch (SecurityException se) {
					continue;
				}

				references[refcount] = registration.getReferenceImpl();
				refcount++;
			}

			if (refcount < size) {
				if (refcount == 0) {
					return null;
				}

				ServiceReferenceImpl[] refs = references;
				references = new ServiceReferenceImpl[refcount];

				System.arraycopy(refs, 0, references, 0, refcount);
			}

			return references;
		}
	}

	/**
	 * Called when the BundleContext is closing to unregister all services
	 * currently registered by the bundle.
	 * 
	 * @param context The BundleContext of the closing bundle.
	 */
	public void unregisterServices(BundleContextImpl context) {
		List registrations;
		synchronized (this) {
			registrations = lookupServiceRegistrations(context);
		}
		ListIterator iter = registrations.listIterator();
		while (iter.hasNext()) {
			ServiceRegistrationImpl registration = (ServiceRegistrationImpl) iter.next();
			try {
				registration.unregister();
			} catch (IllegalStateException e) {
				/* already unregistered */
			}
		}
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
		int usedSize;
		ServiceRegistrationImpl[] usedServices;
		synchronized (servicesInUse) {
			usedSize = servicesInUse.size();
			if (usedSize == 0) {
				return;
			}

			if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
				Debug.println("Releasing services"); //$NON-NLS-1$
			}

			usedServices = (ServiceRegistrationImpl[]) servicesInUse.keySet().toArray(new ServiceRegistrationImpl[usedSize]);
		}

		for (int i = 0; i < usedSize; i++) {
			usedServices[i].releaseService(context);
		}
	}

	/**
	 * Return the next available service id.
	 * 
	 * @return next service id.
	 */
	/* @GuardedBy("this") */
	long getNextServiceId() {
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
		// Add the ServiceRegistration to the list of Services published by BundleContext.
		ArrayList contextServices = (ArrayList) publishedServicesByContext.get(context);
		if (contextServices == null) {
			contextServices = new ArrayList(10);
			publishedServicesByContext.put(context, contextServices);
		}
		contextServices.add(registration);

		// Add the ServiceRegistration to the list of Services published by Class Name.
		String[] clazzes = registration.getClasses();
		int size = clazzes.length;

		for (int i = 0; i < size; i++) {
			String clazz = clazzes[i];

			ArrayList services = (ArrayList) publishedServicesByClass.get(clazz);

			if (services == null) {
				services = new ArrayList(10);
				publishedServicesByClass.put(clazz, services);
			}

			services.add(registration);
		}

		// Add the ServiceRegistration to the list of all published Services.
		allPublishedServices.add(registration);
	}

	/**
	 * Remove the ServiceRegistrationImpl from the data structure.
	 * 
	 * @param context The BundleContext of the bundle registering the service.
	 * @param registration The ServiceRegistration to remove.
	 */
	/* @GuardedBy("this") */
	void removeServiceRegistration(BundleContextImpl context, ServiceRegistrationImpl serviceReg) {
		// Remove the ServiceRegistration from the list of Services published by BundleContext.
		ArrayList contextServices = (ArrayList) publishedServicesByContext.get(context);
		if (contextServices != null) {
			contextServices.remove(serviceReg);
		}

		// Remove the ServiceRegistration from the list of Services published by Class Name.
		String[] clazzes = serviceReg.getClasses();
		int size = clazzes.length;

		for (int i = 0; i < size; i++) {
			String clazz = clazzes[i];
			ArrayList services = (ArrayList) publishedServicesByClass.get(clazz);
			services.remove(serviceReg);
		}

		// Remove the ServiceRegistration from the list of all published Services.
		allPublishedServices.remove(serviceReg);
	}

	/**
	 * Lookup Service References in the data structure by class name and filter.
	 * 
	 * @param clazz The class name with which the service was registered or
	 *        <code>null</code> for all services.
	 * @param filter The filter criteria.
	 */
	/* @GuardedBy("this") */
	private List lookupServiceReferences(String clazz, Filter filter) {
		ArrayList result;
		if (clazz == null) { /* all services */
			result = allPublishedServices;
		} else {
			/* services registered under the class name */
			result = (ArrayList) publishedServicesByClass.get(clazz);
			if (result == null) {
				return Collections.EMPTY_LIST;
			}
		}

		result = new ArrayList(result); /* make a new list since we don't want to change the real list */

		ListIterator iter = result.listIterator();
		while (iter.hasNext()) {
			ServiceRegistrationImpl registration = (ServiceRegistrationImpl) iter.next();
			ServiceReferenceImpl reference = registration.getReferenceImpl();
			if ((filter == null) || filter.match(reference)) {
				iter.set(reference); /* replace the registration with its reference */
			} else {
				iter.remove();
			}
		}
		return result;
	}

	/**
	 * Lookup Service Registrations in the data structure by BundleContext.
	 * 
	 * @param context The BundleContext for which to return Service Registrations.
	 */
	/* @GuardedBy("this") */
	private List lookupServiceRegistrations(BundleContextImpl context) {
		ArrayList result = (ArrayList) publishedServicesByContext.get(context);

		if (result == null) {
			return Collections.EMPTY_LIST;
		}

		result = new ArrayList(result); /* make a new list since we don't want to change the real list */
		return result;
	}

	/**
	 * Lookup Service References in the data structure by BundleContext.
	 * 
	 * @param context The BundleContext for which to return Service References.
	 */
	/* @GuardedBy("this") */
	private List lookupServiceReferences(BundleContextImpl context) {
		List result = lookupServiceRegistrations(context);

		ListIterator iter = result.listIterator();
		while (iter.hasNext()) {
			ServiceRegistrationImpl registration = (ServiceRegistrationImpl) iter.next();
			ServiceReferenceImpl reference = registration.getReferenceImpl();
			iter.set(reference); /* replace the registration with its reference */
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
		if (sm != null) {
			int len = names.length;
			for (int i = 0; i < len; i++) {
				sm.checkPermission(new ServicePermission(names[i], ServicePermission.REGISTER));
			}
		}
	}

	/**
	 * Check for permission to get a service.
	 */
	private static void checkGetServicePermission(String name) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			sm.checkPermission(new ServicePermission(name, ServicePermission.GET));
		}
	}

	/**
	 * Check for permission to get a service.
	 * 
	 * The caller must have permission for at least ONE name.
	 */
	private static void checkGetServicePermission(String[] names) {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null) {
			SecurityException se = null;
			int len = names.length;
			for (int i = 0; i < len; i++) {
				try {
					sm.checkPermission(new ServicePermission(names[i], ServicePermission.GET));
					return;
				} catch (SecurityException e) {
					se = e;
				}
			}
			throw se;
		}
	}

	/**
	 * Check for permission to listen to a service.
	 */
	static boolean hasListenServicePermission(ServiceEvent event, BundleContextImpl context) {
		ProtectionDomain domain = context.getBundleImpl().getProtectionDomain();
		if (domain == null) {
			return true;
		}

		ServiceReferenceImpl reference = (ServiceReferenceImpl) event.getServiceReference();
		String[] names = reference.getClasses();
		int len = names.length;
		for (int i = 0; i < len; i++) {
			if (domain.implies(new ServicePermission(names[i], ServicePermission.GET))) {
				return true;
			}
		}

		return false;
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
		for (int i = 0; i < clazzes.length; i++) {
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
		for (int i = 0; i < interfaces.length; i++)
			if (!extensiveCheckServiceClass(clazz, interfaces[i]))
				return false;
		Class superClazz = serviceClazz.getSuperclass();
		if (superClazz != null)
			if (!extensiveCheckServiceClass(clazz, superClazz))
				return false;
		return true;
	}

	static boolean isAssignableTo(BundleContextImpl context, ServiceReferenceImpl reference) {
		if (!scopeEvents)
			return true;
		Bundle bundle = context.getBundleImpl();
		String[] clazzes = reference.getClasses();
		for (int i = 0; i < clazzes.length; i++)
			if (!reference.isAssignableTo(bundle, clazzes[i]))
				return false;
		return true;
	}
}
