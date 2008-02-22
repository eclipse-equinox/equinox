/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.lang.reflect.Array;
import java.util.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.util.Headers;
import org.osgi.framework.*;

/**
 * A registered service.
 *
 * The framework returns a ServiceRegistration object when a
 * {@link BundleContextImpl#registerService(String, Object, Dictionary) BundleContext.registerService}
 * method is successful. This object is for the private use of
 * the registering bundle and should not be shared with other bundles.
 * <p>The ServiceRegistration object may be used to update the properties
 * for the service or to unregister the service.
 *
 * <p>If the ServiceRegistration is garbage collected the framework may remove
 * the service. This implies that if a
 * bundle wants to keep its service registered, it should keep the
 * ServiceRegistration object referenced.
 */
public class ServiceRegistrationImpl implements ServiceRegistration {
	/** Reference to this registration. */
	/* @GuardedBy("registrationLock") */
	private ServiceReferenceImpl reference;

	/** Internal framework object. */
	final Framework framework;

	/** context which registered this service. */
	final BundleContextImpl context;

	/** bundle which registered this service. */
	final AbstractBundle bundle;

	/** list of contexts using the service.
	/* @GuardedBy("registrationLock") */
	private ArrayList contextsUsing;

	/** service classes for this registration. */
	final String[] clazzes;

	/** service object for this registration. */
	final Object service;

	/** properties for this registration. */
	/* @GuardedBy("registrationLock") */
	private Properties properties;

	/** service id. */
	/* @GuardedBy("registrationLock") */
	private long serviceid;

	/** service ranking. */
	/* @GuardedBy("registrationLock") */
	private int serviceranking;

	/* internal object to use for synchronization */
	private final Object registrationLock = new Object();

	/** The registration state */
	/* @GuardedBy("registrationLock") */
	private int state = REGISTERED;
	private static final int REGISTERED = 0x00;
	private static final int UNREGISTERING = 0x01;
	private static final int UNREGISTERED = 0x02;

	/**
	 * Construct a ServiceRegistration and register the service
	 * in the framework's service registry.
	 *
	 */
	protected ServiceRegistrationImpl(BundleContextImpl context, String[] clazzes, Object service) {
		this.context = context;
		this.bundle = context.bundle;
		this.framework = context.framework;
		this.clazzes = clazzes; /* must be set before calling createProperties. */
		this.service = service;
		synchronized (registrationLock) {
			this.contextsUsing = null;
			/* We leak this from the constructor here, but it is ok
			 * because the ServiceReferenceImpl constructor only
			 * stores the value in a final field without
			 * otherwise using it.
			 */
			this.reference = new ServiceReferenceImpl(this);
		}
	}

	/**
	 * Call after constructing this object to complete the registration.
	 */
	void register(Dictionary props) {
		final ServiceReference ref;
		synchronized (framework.serviceRegistry) {
			context.checkValid();
			synchronized (registrationLock) {
				ref = reference; /* used to publish event outside sync */
				serviceid = framework.getNextServiceId(); /* must be set before calling createProperties. */
				this.properties = createProperties(props); /* must be valid after unregister is called. */
			}
			if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
				Debug.println("registerService[" + bundle + "](" + this + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			framework.serviceRegistry.publishService(context, this);
		}

		/* must not hold the registrations lock when this event is published */
		framework.publishServiceEvent(ServiceEvent.REGISTERED, ref);
	}

	/**
	 * Unregister the service.
	 * Remove a service registration from the framework's service
	 * registry.
	 * All {@link ServiceReferenceImpl} objects for this registration
	 * can no longer be used to interact with the service.
	 *
	 * <p>The following steps are followed to unregister a service:
	 * <ol>
	 * <li>The service is removed from the framework's service
	 * registry so that it may no longer be used.
	 * {@link ServiceReferenceImpl}s for the service may no longer be used
	 * to get a service object for the service.
	 * <li>A {@link ServiceEvent} of type {@link ServiceEvent#UNREGISTERING}
	 * is synchronously sent so that bundles using this service
	 * may release their use of the service.
	 * <li>For each bundle whose use count for this service is greater
	 * than zero:
	 * <ol>
	 * <li>The bundle's use count for this service is set to zero.
	 * <li>If the service was registered with a {@link ServiceFactory},
	 * the {@link ServiceFactory#ungetService ServiceFactory.ungetService} method
	 * is called to release the service object for the bundle.
	 * </ol>
	 * </ol>
	 *
	 * @exception java.lang.IllegalStateException If
	 * this ServiceRegistration has already been unregistered.
	 * @see BundleContextImpl#ungetService
	 */
	public void unregister() {
		final ServiceReference ref;
		synchronized (framework.serviceRegistry) {
			synchronized (registrationLock) {
				if (state != REGISTERED) /* in the process of unregisterING */
				{
					throw new IllegalStateException(Msg.SERVICE_ALREADY_UNREGISTERED_EXCEPTION);
				}

				/* remove this object from the service registry */
				if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
					Debug.println("unregisterService[" + bundle + "](" + this + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}

				framework.serviceRegistry.unpublishService(context, this);

				state = UNREGISTERING; /* mark unregisterING */
				ref = reference; /* used to publish event outside sync */
			}
		}

		/* must not hold the registrationLock when this event is published */
		framework.publishServiceEvent(ServiceEvent.UNREGISTERING, ref);

		int size = 0;
		BundleContextImpl[] users = null;

		synchronized (registrationLock) {
			/* we have published the ServiceEvent, now mark the service fully unregistered */
			state = UNREGISTERED;

			if (contextsUsing != null) {
				size = contextsUsing.size();

				if (size > 0) {
					if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
						Debug.println("unregisterService: releasing users"); //$NON-NLS-1$
					}
					users = (BundleContextImpl[]) contextsUsing.toArray(new BundleContextImpl[size]);
				}
			}
		}

		/* must not hold the registrationLock while releasing services */
		for (int i = 0; i < size; i++) {
			releaseService(users[i]);
		}

		synchronized (registrationLock) {
			contextsUsing = null;

			reference = null; /* mark registration dead */
		}

		/* The properties field must remain valid after unregister completes. */
	}

	/**
	 * Returns a {@link ServiceReferenceImpl} object for this registration.
	 * The {@link ServiceReferenceImpl} object may be shared with other bundles.
	 *
	 * @exception java.lang.IllegalStateException If
	 * this ServiceRegistration has already been unregistered.
	 * @return A {@link ServiceReferenceImpl} object.
	 */
	public org.osgi.framework.ServiceReference getReference() {
		/* use reference instead of unregistered so that ServiceFactorys, called
		 * by releaseService after the registration is unregistered, can
		 * get the ServiceReference. Note this technically may violate the spec
		 * but makes more sense.
		 */
		synchronized (registrationLock) {
			if (reference == null) {
				throw new IllegalStateException(Msg.SERVICE_ALREADY_UNREGISTERED_EXCEPTION);
			}

			return reference;
		}
	}

	/**
	 * Update the properties associated with this service.
	 *
	 * <p>The key "objectClass" cannot be modified by this method. It's
	 * value is set when the service is registered.
	 *
	 * <p>The following steps are followed to modify a service's properties:
	 * <ol>
	 * <li>The service's properties are replaced with the provided properties.
	 * <li>A {@link ServiceEvent} of type {@link ServiceEvent#MODIFIED}
	 * is synchronously sent.
	 * </ol>
	 *
	 * @param props The properties for this service.
	 *        Changes should not be made to this object after calling this method.
	 *        To update the service's properties this method should be called again.
	 * @exception java.lang.IllegalStateException If
	 * this ServiceRegistration has already been unregistered.
	 *
	 * @exception IllegalArgumentException If the <tt>properties</tt>
	 * parameter contains case variants of the same key name.
	 */
	public void setProperties(Dictionary props) {
		final ServiceReference ref;
		synchronized (registrationLock) {
			if (state != REGISTERED) /* in the process of unregistering */
			{
				throw new IllegalStateException(Msg.SERVICE_ALREADY_UNREGISTERED_EXCEPTION);
			}

			ref = reference; /* used to publish event outside sync */
			this.properties = createProperties(props);
		}

		/* must not hold the registrationLock when this event is published */
		framework.publishServiceEvent(ServiceEvent.MODIFIED, ref);
	}

	/**
	 * Construct a properties object from the dictionary for this
	 * ServiceRegistration.
	 *
	 * @param props The properties for this service.
	 * @return A Properties object for this ServiceRegistration.
	 */
	/* @GuardedBy("registrationLock") */
	private Properties createProperties(Dictionary props) {
		Properties properties = new Properties(props);

		properties.set(Constants.OBJECTCLASS, clazzes, true);
		properties.set(Constants.SERVICE_ID, new Long(serviceid), true);
		properties.setReadOnly();
		Object ranking = properties.getProperty(Constants.SERVICE_RANKING);

		serviceranking = (ranking instanceof Integer) ? ((Integer) ranking).intValue() : 0;

		return properties;
	}

	/**
	 * Return the properties object. This is for framework internal use only.
	 * @return The service registration's properties.
	 */
	Properties getProperties() {
		synchronized (registrationLock) {
			return properties;
		}
	}

	/**
	 * Get the value of a service's property.
	 *
	 * <p>This method will continue to return property values after the
	 * service has been unregistered. This is so that references to
	 * unregistered service can be interrogated.
	 * (For example: ServiceReference objects stored in the log.)
	 *
	 * @param key Name of the property.
	 * @return Value of the property or <code>null</code> if there is
	 * no property by that name.
	 */
	protected Object getProperty(String key) {
		synchronized (registrationLock) {
			return properties.getProperty(key);
		}
	}

	/**
	 * Get the list of key names for the service's properties.
	 *
	 * <p>This method will continue to return the keys after the
	 * service has been unregistered. This is so that references to
	 * unregistered service can be interrogated.
	 * (For example: ServiceReference objects stored in the log.)
	 *
	 * @return The list of property key names.
	 */
	protected String[] getPropertyKeys() {
		synchronized (registrationLock) {
			return properties.getPropertyKeys();
		}
	}

	/**
	 * Return the service id for this service.
	 * @return The service id for this service.
	 */
	long getId() {
		synchronized (registrationLock) {
			return serviceid;
		}
	}

	/**
	 * Return the service ranking for this service.
	 * @return The service ranking for this service.
	 */
	int getRanking() {
		synchronized (registrationLock) {
			return serviceranking;
		}
	}

	/**
	 * Return the bundle which registered the service.
	 *
	 * <p>This method will always return <code>null</code> when the
	 * service has been unregistered. This can be used to
	 * determine if the service has been unregistered.
	 *
	 * @return The bundle which registered the service.
	 */
	protected AbstractBundle getBundle() {
		synchronized (registrationLock) {
			if (reference == null) {
				return null;
			}

			return bundle;
		}
	}

	/**
	 * Get a service object for the using BundleContext.
	 *
	 * @param user BundleContext using service.
	 * @return Service object
	 */
	protected Object getService(BundleContextImpl user) {
		synchronized (registrationLock) {
			if (state == UNREGISTERED) { /* service unregistered */
				return null;
			}
		}
		if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
			Debug.println("getService[" + user.bundle + "](" + this + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		Map servicesInUse = user.getServicesInUseMap();
		if (servicesInUse == null) { /* user is closed */
			user.checkValid(); /* throw exception */
		}
		/* Use a while loop to support retry if a call to a ServiceFactory fails */
		while (true) {
			ServiceUse use;
			boolean added = false;
			/* Obtain the ServiceUse object for this service by bundle user */
			synchronized (servicesInUse) {
				user.checkValid();
				use = (ServiceUse) servicesInUse.get(this);
				if (use == null) {
					/* if this is the first use of the service
					 * optimistically record this service is being used. */
					use = new ServiceUse(user, this);
					added = true;
					synchronized (registrationLock) {
						servicesInUse.put(this, use);
						if (contextsUsing == null) {
							contextsUsing = new ArrayList(10);
						}
						contextsUsing.add(user);
					}
				}
			}

			/* Obtain and return the service object */
			synchronized (use) {
				/* if another thread removed the ServiceUse, then
				 * go back to the top and start again */
				synchronized (servicesInUse) {
					user.checkValid();
					if (servicesInUse.get(this) != use) {
						continue;
					}
				}
				Object serviceObject = use.getService();
				/* if the service factory failed to return an object and
				 * we created the service use, then remove the 
				 * optimistically added ServiceUse. */
				if ((serviceObject == null) && added) {
					synchronized (servicesInUse) {
						synchronized (registrationLock) {
							servicesInUse.remove(this);
							contextsUsing.remove(user);
						}
					}
				}
				return serviceObject;
			}
		}
	}

	/**
	 * Unget a service for the using BundleContext.
	 *
	 * @param user BundleContext using service.
	 * @return <code>false</code> if the context bundle's use count for the service
	 *         is zero or if the service has been unregistered,
	 *         otherwise <code>true</code>.
	 */
	protected boolean ungetService(BundleContextImpl user) {
		synchronized (registrationLock) {
			if (state == UNREGISTERED) {
				return false;
			}
		}

		if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
			Debug.println("ungetService[" + user.bundle + "](" + this + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		Map servicesInUse = user.getServicesInUseMap();
		if (servicesInUse == null) {
			return false;
		}

		ServiceUse use;
		synchronized (servicesInUse) {
			use = (ServiceUse) servicesInUse.get(this);
			if (use == null) {
				return false;
			}
		}

		synchronized (use) {
			if (use.ungetService()) {
				/* use count is now zero */
				synchronized (servicesInUse) {
					synchronized (registrationLock) {
						servicesInUse.remove(this);
						contextsUsing.remove(user);
					}
				}
			}
		}
		return true;
	}

	/**
	 * Release the service for the using BundleContext.
	 *
	 * @param user BundleContext using service.
	 */
	protected void releaseService(BundleContextImpl user) {
		synchronized (registrationLock) {
			if (reference == null) { /* registration dead */
				return;
			}
		}

		if (Debug.DEBUG && Debug.DEBUG_SERVICES) {
			Debug.println("releaseService[" + user.bundle + "](" + this + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		Map servicesInUse = user.getServicesInUseMap();
		if (servicesInUse == null) {
			return;
		}
		ServiceUse use;
		synchronized (servicesInUse) {
			synchronized (registrationLock) {
				use = (ServiceUse) servicesInUse.remove(this);
				if (use == null) {
					return;
				}
				// contextsUsing may have been nulled out by use.releaseService() if the registrant bundle
				// is listening for events and unregisters the service
				if (contextsUsing != null)
					contextsUsing.remove(user);
			}
		}
		synchronized (use) {
			use.releaseService();
		}
	}

	/**
	 * Return the list of bundle which are using this service.
	 *
	 * @return Array of Bundles using this service.
	 */
	protected AbstractBundle[] getUsingBundles() {
		synchronized (registrationLock) {
			if (state == UNREGISTERED) /* service unregistered */
				return null;

			if (contextsUsing == null)
				return null;

			int size = contextsUsing.size();
			if (size == 0)
				return null;

			/* Copy list of BundleContext into an array of Bundle. */
			AbstractBundle[] bundles = new AbstractBundle[size];
			for (int i = 0; i < size; i++)
				bundles[i] = ((BundleContextImpl) contextsUsing.get(i)).bundle;

			return bundles;
		}
	}

	/**
	 * Return a String representation of this object.
	 *
	 * @return String representation of this object.
	 */
	public String toString() {
		String[] clazzes = this.clazzes;
		int size = clazzes.length;
		StringBuffer sb = new StringBuffer(50 * size);

		sb.append('{');

		for (int i = 0; i < size; i++) {
			if (i > 0) {
				sb.append(", "); //$NON-NLS-1$
			}
			sb.append(clazzes[i]);
		}

		sb.append("}="); //$NON-NLS-1$
		sb.append(getProperties().toString());

		return sb.toString();
	}

	/**
	 * Hashtable for service properties.
	 */
	static class Properties extends Headers {
		/**
		 * Create a properties object for the service.
		 *
		 * @param props The properties for this service.
		 */
		private Properties(int size, Dictionary props) {
			super(size);

			if (props != null) {
				synchronized (props) {
					Enumeration keysEnum = props.keys();

					while (keysEnum.hasMoreElements()) {
						Object key = keysEnum.nextElement();

						if (key instanceof String) {
							String header = (String) key;

							setProperty(header, props.get(header));
						}
					}
				}
			}
		}

		/**
		 * Create a properties object for the service.
		 *
		 * @param props The properties for this service.
		 */
		protected Properties(Dictionary props) {
			this((props == null) ? 2 : props.size() + 2, props);
		}

		/**
		 * Get a clone of the value of a service's property.
		 *
		 * @param key header name.
		 * @return Clone of the value of the property or <code>null</code> if there is
		 * no property by that name.
		 */
		protected Object getProperty(String key) {
			return (cloneValue(get(key)));
		}

		/**
		 * Get the list of key names for the service's properties.
		 *
		 * @return The list of property key names.
		 */
		protected synchronized String[] getPropertyKeys() {
			int size = size();

			String[] keynames = new String[size];

			Enumeration keysEnum = keys();

			for (int i = 0; i < size; i++) {
				keynames[i] = (String) keysEnum.nextElement();
			}

			return (keynames);
		}

		/**
		 * Put a clone of the property value into this property object.
		 *
		 * @param key Name of property.
		 * @param value Value of property.
		 * @return previous property value.
		 */
		protected synchronized Object setProperty(String key, Object value) {
			return (set(key, cloneValue(value)));
		}

		/**
		 * Attempt to clone the value if necessary and possible.
		 *
		 * For some strange reason, you can test to see of an Object is
		 * Cloneable but you can't call the clone method since it is
		 * protected on Object!
		 *
		 * @param value object to be cloned.
		 * @return cloned object or original object if we didn't clone it.
		 */
		protected static Object cloneValue(Object value) {
			if (value == null)
				return null;
			if (value instanceof String) /* shortcut String */
				return (value);
			if (value instanceof Number) /* shortcut Number */
				return value;
			if (value instanceof Character) /* shortcut Character */
				return value;
			if (value instanceof Boolean) /* shortcut Boolean */
				return value;

			Class clazz = value.getClass();
			if (clazz.isArray()) {
				// Do an array copy
				Class type = clazz.getComponentType();
				int len = Array.getLength(value);
				Object clonedArray = Array.newInstance(type, len);
				System.arraycopy(value, 0, clonedArray, 0, len);
				return clonedArray;
			}
			// must use reflection because Object clone method is protected!!
			try {
				return (clazz.getMethod("clone", null).invoke(value, null)); //$NON-NLS-1$
			} catch (Exception e) {
				/* clone is not a public method on value's class */
			} catch (Error e) {
				/* JCL does not support reflection; try some well known types */
				if (value instanceof Vector)
					return (((Vector) value).clone());
				if (value instanceof Hashtable)
					return (((Hashtable) value).clone());
			}
			return (value);
		}

		public synchronized String toString() {
			String keys[] = getPropertyKeys();

			int size = keys.length;

			StringBuffer sb = new StringBuffer(20 * size);

			sb.append('{');

			int n = 0;
			for (int i = 0; i < size; i++) {
				String key = keys[i];
				if (!key.equals(Constants.OBJECTCLASS)) {
					if (n > 0)
						sb.append(", "); //$NON-NLS-1$

					sb.append(key);
					sb.append('=');
					Object value = get(key);
					if (value.getClass().isArray()) {
						sb.append('[');
						int length = Array.getLength(value);
						for (int j = 0; j < length; j++) {
							if (j > 0)
								sb.append(',');
							sb.append(Array.get(value, j));
						}
						sb.append(']');
					} else {
						sb.append(value);
					}
					n++;
				}
			}

			sb.append('}');

			return (sb.toString());
		}
	}
}
