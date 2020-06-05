/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Alex Blewitt - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * {@code ServiceCaller} provides functional methods for invoking OSGi services
 * in two different ways
 * <ul>
 * <li> Single invocations which happen only once or very rarely. 
 * In this case, maintaining a cache of the service is not worth the overhead.</li>
 * <li> Multiple invocations that happen often and rapidly. In this case, maintaining
 * a cache of the service is worth the overhead.</li>
 * </ul>
 * 
 * For single invocations of a service the static method
 * {@link ServiceCaller#callOnce(Class, Class, Consumer)} can be used.
 * This method will wrap a call to the consumer of the service with
 * the necessary OSGi service registry calls to ensure the service
 * exists and will do the proper get and release service operations
 * surround the calls to the service. By wrapping a call around the
 * service we can ensure that it is correctly released after use.
 * <p>
 * Single invocation example:
 * <pre>
 * ServiceCaller.callOnce(MyClass.class, ILog.class, (logger) -> logger.info("All systems go!"));
 * </pre>
 * Note that it is generally more efficient to use a long-running service
 * utility, such as {@link ServiceTracker} or declarative services, but there
 * are cases where a single one-shot lookup is preferable, especially if the
 * service is not required after use. Examples might include logging unlikely
 * conditions or processing debug options that are only read once.
 *
 * 
 * This allows boilerplate code to be reduced at call sites, which would
 * otherwise have to do something like:
 * 
 * <pre>
 * Bundle bundle = FrameworkUtil.getBundle(BadExample.class);
 * BundleContext context = bundle == null ? null : bundle.getBundleContext();
 * ServiceReference&lt;Service&gt; reference = context == null ? null : context.getServiceReference(serviceType);
 * try {
 *   Service service = reference == null ? null : context.getService(reference);
 *   if (service != null)
 *     consumer.accept(service);
 * } finally {
 *   context.ungetService(reference);
 * }
 * </pre>
 * For cases where a service is used much more often a {@code ServiceCaller} instance
 * can be used to cache and track the available service. This may be useful for cases
 * that cannot use declarative services and that want to avoid using something like
 * a {@link ServiceTracker} that does not easily allow for lazy instantiation of the service
 * instance.  For example, if logging is used more often then something like the following
 * could be used:
 * <pre>
 * static final ServiceCaller&lt;ILog&gt; log = new ServiceCaller(MyClass.class, ILog.class);
 * static void info(String msg) {
 *   log.call(logger -> logger.info(msg);
 * }
 * </pre>
 * 
 * Note that this class is intended for simple service usage patterns only.  More advanced cases that
 * require tracking of service ranking or additional service property matching must use other
 * mechanisms such as the {@link ServiceTracker} or declarative services.
 * 
 * @param <Service> the service type for this caller
 * @since 3.13
 */
public class ServiceCaller<Service> {
	/**
	 * Calls an OSGi service by dynamically looking it up and passing it to the given consumer.
	 * 
	 * If not running under OSGi, the caller bundle is not active or the service is not available, return false.
	 * Any exception thrown by the consumer is rethrown by this method.
	 * If the service is found, call the service and return true.
	 * @param caller a class from the bundle that will use service
	 * @param serviceType the OSGi service type to look up
	 * @param consumer the consumer of the OSGi service
	 * @param <Service> the OSGi service type to look up
	 * @return true if the OSGi service was located and called successfully, false otherwise
	 * @throws NullPointerException if any of the parameters are null
	 */
	public static <Service> boolean callOnce(Class<?> caller, Class<Service> serviceType, Consumer<Service> consumer) {
		return new ServiceCaller<>(caller, serviceType).getCallUnget(consumer);
	}

	class ReferenceAndService implements SynchronousBundleListener, ServiceListener {
		final BundleContext context;
		final ServiceReference<Service> ref;
		final Service instance;

		public ReferenceAndService(final BundleContext context, ServiceReference<Service> ref, Service instance) {
			this.context = context;
			this.ref = ref;
			this.instance = instance;
		}

		void unget() {
			untrack();
			try {
				context.ungetService(ref);
			} catch (IllegalStateException e) {
				// ignore; just trying to cleanup but context is not valid now
			}
		}

		@Override
		public void bundleChanged(BundleEvent e) {
			if (bundle.equals(e.getBundle()) && e.getType() == BundleEvent.STOPPING) {
				untrack();
			}
		}

		@Override
		public void serviceChanged(ServiceEvent e) {
			if (e.getType() == ServiceEvent.UNREGISTERING) {
				untrack();
			}
			if (filter != null && e.getType() == ServiceEvent.MODIFIED_ENDMATCH) {
				untrack();
			}
		}

		// must hold monitor on ServiceCaller.this when calling track
		Optional<ReferenceAndService> track() {
			try {
				ServiceCaller.this.service = this;
				// Filter specific to this service reference ID
				context.addServiceListener(this, "(&" //$NON-NLS-1$
						+ "(objectClass=" + serviceType.getName() + ")" // //$NON-NLS-1$ //$NON-NLS-2$
						+ "(service.id=" + ref.getProperty(Constants.SERVICE_ID) + ")" // //$NON-NLS-1$ //$NON-NLS-2$
						+ (filter == null ? "" : filter) // //$NON-NLS-1$
						+ ")"); //$NON-NLS-1$
				context.addBundleListener(this);
				if (ref.getBundle() == null || context.getBundle() == null && ServiceCaller.this.service == this) {
					// service should have been untracked but we may have missed the event
					// before we could added the listeners
					untrack();
					return Optional.empty();
				}
			} catch (InvalidSyntaxException e) {
				// really should never happen with our own filter above.
				ServiceCaller.this.service = null;
				throw new IllegalStateException(e);
			} catch (IllegalStateException e) {
				// bundle was stopped before we could get listeners added/removed
				ServiceCaller.this.service = null;
				return Optional.empty();
			}
			return Optional.of(this);
		}

		void untrack() {
			synchronized (ServiceCaller.this) {
				if (ServiceCaller.this.service == this) {
					ServiceCaller.this.service = null;
				}
				try {
					context.removeServiceListener(this);
					context.removeBundleListener(this);
				} catch (IllegalStateException e) {
					// context is invalid;
					// ignore - the listeners already got cleaned up
				}
			}
		}
	}

	final Bundle bundle;
	final Class<Service> serviceType;
	final String filter;
	volatile ReferenceAndService service = null;

	/**
	 * Creates a {@code ServiceCaller} instance for invoking an OSGi
	 * service many times with a consumer function.
	 * @param caller a class from the bundle that will consume the service
	 * @param serviceType the OSGi service type to look up
	 */
	public ServiceCaller(Class<?> caller, Class<Service> serviceType) {
		this(caller, serviceType, null);
	}

	/**
	 * Creates a {@code ServiceCaller} instance for invoking an OSGi
	 * service many times with a consumer function.
	 * @param caller a class from the bundle that will consume the service
	 * @param serviceType the OSGi service type to look up
	 * @param filter the service filter used to look up the service.  May be {@code null}.
	 */
	public ServiceCaller(Class<?> caller, Class<Service> serviceType, String filter) {
		this.serviceType = Objects.requireNonNull(serviceType);
		this.bundle = Objects.requireNonNull(FrameworkUtil.getBundle(Objects.requireNonNull(caller)));
		this.filter = filter;
		if (filter != null) {
			try {
				FrameworkUtil.createFilter(filter);
			} catch (InvalidSyntaxException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	private boolean getCallUnget(Consumer<Service> consumer) {
		return getCurrent().map((r) -> {
			try {
				consumer.accept(r.instance);
				return Boolean.TRUE;
			} finally {
				r.unget();
			}
		}).orElse(Boolean.FALSE);
	}

	private BundleContext getContext() {
		if (System.getSecurityManager() != null) {
			return AccessController.doPrivileged((PrivilegedAction<BundleContext>) () -> bundle.getBundleContext());
		}
		return bundle.getBundleContext();
	}

	/**
	 * Calls an OSGi service by dynamically looking it up and passing it to the given consumer.
	 * If not running under OSGi, the caller bundle is not active or the service is not available, return false.
	 * Any exception thrown by the consumer is rethrown by this method.
	 * Subsequent calls to this method will attempt to reuse the previously acquired service instance until one
	 * of the following occurs:
	 * <ul>
	 * <li>The {@link #unget()} method is called.</li>
	 * <li>The service is unregistered.</li>
	 * <li>The caller bundle is stopped.</li>
	 * </ul>
	 * 
	 * After one of these conditions occur subsequent calls to this method will try to acquire the
	 * another service instance.
	 * @param consumer the consumer of the OSGi service
	 * @return true if the OSGi service was located and called successfully, false otherwise
	 */
	public boolean call(Consumer<Service> consumer) {
		return trackCurrent().map((r) -> {
			consumer.accept(r.instance);
			return Boolean.TRUE;
		}).orElse(Boolean.FALSE);
	}

	private Optional<ReferenceAndService> trackCurrent() {
		ReferenceAndService current = service;
		if (current != null) {
			return Optional.of(current);
		}
		return getCurrent().flatMap((r) -> {
			synchronized (ServiceCaller.this) {
				if (service != null) {
					// another thread beat us
					// unget this instance and return existing
					r.unget();
					return Optional.of(service);
				}
				return r.track();
			}
		});

	}

	private Optional<ReferenceAndService> getCurrent() {
		BundleContext context = getContext();
		return getServiceReference(context).map((r) -> {
			Service current = context.getService(r);
			return current == null ? null : new ReferenceAndService(context, r, current);
		});
	}

	private Optional<ServiceReference<Service>> getServiceReference(BundleContext context) {
		if (context == null) {
			return Optional.empty();
		}
		if (filter == null) {
			return Optional.ofNullable(context.getServiceReference(serviceType));
		}
		try {
			return context.getServiceReferences(serviceType, filter).stream().findFirst();
		} catch (InvalidSyntaxException e) {
			// should not happen; filter was checked at construction
			return Optional.empty();
		}
	}

	/**
	 * Releases the cached service object, if it exists.
	 * Another invocation of {@link #call(Consumer)} will
	 * lazily get the service instance again and cache the new
	 * instance if found.
	 */
	public void unget() {
		ReferenceAndService current = service;
		if (current != null) {
			current.unget();
		}
	}
}
