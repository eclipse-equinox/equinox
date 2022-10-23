/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
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
 *     Alexander Fedorov (ArSysOp) - documentation improvements
 *     Hannes Wellmann - Add ServiceUsageScope and use() methods
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.lang.StackWalker.Option;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.eclipse.core.internal.runtime.CommonMessages;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * {@code ServiceCaller} provides functional methods for invoking OSGi services
 * in two different ways
 * <ul>
 * <li>Single invocations which happen only once or very rarely. In this case,
 * maintaining a cache of the service is not worth the overhead.</li>
 * <li>Multiple invocations that happen often and rapidly. In this case,
 * maintaining a cache of the service is worth the overhead.</li>
 * </ul>
 * <p>
 * For single invocations of a service the static method
 * {@link #callOnce(Class, Class, Consumer)} or {@link #use(Class)} can be used.
 * The former method will wrap a call to the consumer of the service with the
 * necessary OSGi service registry calls to ensure the service exists and will
 * do the proper get and release service operations surround the calls to the
 * service. The latter method returns a {@link AutoCloseable auto-closable}
 * {@link ServiceUsageScope} that is intended to be used within a
 * try-with-resources block and gets the service-instances when requested the
 * first time and releases all obtained services when being closed. Both methods
 * ensure that the service is correctly released after use. In contrast to
 * {@code callOnce()} the {@code use()} methods and the returned
 * {@link ServiceUsageScope} allow to return values obtained from a service in
 * an simple way, to propagate checked exceptions thrown during the interaction
 * with the service and allow to obtain all instances of the specified Service
 * type.
 * </p>
 * <p>
 * Single invocation example:
 * </p>
 * Using {@code callOnce()}:
 * 
 * <pre>
 * ServiceCaller.callOnce(MyClass.class, ILog.class, logger -&gt; logger.info("All systems go!"));
 * </pre>
 * 
 * Using {@code use()}:
 * 
 * <pre>
 * try (var service = ServiceCaller.use(ILog.class)) {
 * 	logger.first().orElseThrow().info("All systems go!");
 * Or
 * 	logger.all().forEach(l -&gt; l.info("Status message to each ILog impl."));
 * }
 * </pre>
 * <p>
 * Note that it is generally more efficient to use a long-running service
 * utility, such as {@link ServiceTracker} or declarative services, but there
 * are cases where a single one-shot lookup is preferable, especially if the
 * service is not required after use. Examples might include logging unlikely
 * conditions or processing debug options that are only read once.
 * </p>
 * <p>
 * This allows boilerplate code to be reduced at call sites, which would
 * otherwise have to do something like:
 * </p>
 * 
 * <pre>
 * Bundle bundle = FrameworkUtil.getBundle(BadExample.class);
 * BundleContext context = bundle == null ? null : bundle.getBundleContext();
 * ServiceReference&lt;Service&gt; reference = context == null ? null : context.getServiceReference(serviceType);
 * try {
 * 	Service service = reference == null ? null : context.getService(reference);
 * 	if (service != null)
 * 		consumer.accept(service);
 * } finally {
 * 	context.ungetService(reference);
 * }
 * </pre>
 * <p>
 * For cases where a service is used much more often a {@code ServiceCaller}
 * instance can be used to cache and track the available service. This may be
 * useful for cases that cannot use declarative services and that want to avoid
 * using something like a {@link ServiceTracker} that does not easily allow for
 * lazy instantiation of the service instance. For example, if logging is used
 * more often then something like the following could be used:
 * </p>
 * 
 * <pre>
 * static final ServiceCaller&lt;ILog&gt; log = new ServiceCaller(MyClass.class, ILog.class);
 * 
 * static void info(String msg) {
 * 	log.call(logger -&gt; logger.info(msg));
 * }
 * </pre>
 * <p>
 * Note that this class is intended for simple service usage patterns only. More
 * advanced cases should use other mechanisms such as the {@link ServiceTracker}
 * or declarative services.
 * </p>
 * 
 * @param <S> the service type for this caller
 * @since 3.13
 */
public class ServiceCaller<S> {

	private static final StackWalker STACK_WALKER = StackWalker.getInstance(Option.RETAIN_CLASS_REFERENCE);

	/**
	 * Returns a {@link ServiceUsageScope} that provides all services of the given
	 * type available in the caller-class' bundle-context.
	 * 
	 * @param <S>     the type of service
	 * @param service the services' type
	 * @return a {@code ServiceUsageScope} for all services available in the
	 *         caller's bundle-context
	 * @since 3.19
	 * 
	 */
	public static <S> ServiceUsageScope<S> use(Class<S> service) {
		Class<?> callerClass = STACK_WALKER.getCallerClass();
		return use(service, null, getBundleContext(callerClass));
	}

	/**
	 * Returns a {@link ServiceUsageScope} that provides all services of the given
	 * type available in the caller-class' bundle-context, that match the given
	 * filter.
	 * 
	 * @param <S>     the type of service
	 * @param service the services' type
	 * @param filter  the filter all provided service must match
	 * @return a {@code ServiceUsageScope} for all services available in the
	 *         caller's bundle-context, that match the filter
	 * @since 3.19
	 */
	public static <S> ServiceUsageScope<S> use(Class<S> service, Filter filter) {
		Class<?> callerClass = STACK_WALKER.getCallerClass();
		return use(service, filter, getBundleContext(callerClass));
	}

	private static BundleContext getBundleContext(Class<?> caller) {
		Bundle bundle = FrameworkUtil.getBundle(caller);
		if (bundle == null) {
			throw new IllegalStateException(NLS.bind(CommonMessages.serviceCaller_bundleUnavailable, caller));
		}
		BundleContext ctx = bundle.getBundleContext();
		if (ctx == null) {
			throw new IllegalStateException(NLS.bind(CommonMessages.serviceCaller_bundleUnavailable, bundle));
		}
		return ctx;
	}

	/**
	 * Returns a {@link ServiceUsageScope} that provides all services of the given
	 * type available in the given {@link BundleContext}.
	 * 
	 * @param <S>     the type of service
	 * @param service the services' type
	 * @param context the bundle-context in which services are looked-up
	 * @return a {@code ServiceUsageScope} for all services available in the context
	 * @since 3.19
	 */
	public static <S> ServiceUsageScope<S> use(Class<S> service, BundleContext context) {
		return new ServiceUsageScope<>(service, null, context);
	}

	/**
	 * Returns a {@link ServiceUsageScope} that provides all services of the given
	 * type available in the given {@link BundleContext}, that match the given
	 * filter.
	 * 
	 * @param <S>     the type of service
	 * @param service the services' type
	 * @param filter  the filter all provided service must match
	 * @param context the bundle-context in which services are looked-up
	 * @return a {@code ServiceUsageScope} for all services available in the
	 *         caller's bundle-context, that match the filter
	 * @since 3.19
	 */
	public static <S> ServiceUsageScope<S> use(Class<S> service, Filter filter, BundleContext context) {
		return new ServiceUsageScope<>(service, filter, context);
	}

	/**
	 * A scope that provides access to the (optionally filtered) set of services of
	 * a type available in a context.
	 * <p>
	 * The {@link BundleContext#getServiceReferences(String, String) service
	 * references are obtained} upon creation, while the actual service objects are
	 * only {@link BundleContext#getService(ServiceReference) get} lazy on the first
	 * request (i.e. when {@link #first()} is called or the service object is
	 * consumed by the Stream returned by {@link #all()}). Services unregistered in
	 * the meantime are discarded and newly registered services are not considered.
	 * </p>
	 * <p>
	 * All {@code get} service objects are cached for subsequent requests and are
	 * {@link BundleContext#ungetService(ServiceReference) unget} only when this
	 * scope is {@link ServiceUsageScope#close() closed}.
	 * </p>
	 * <p>
	 * Instances of this class are not thread-safe.
	 * <p>
	 * 
	 * @param <S> the type of services provided by this scope
	 * @since 3.19
	 */
	public static final class ServiceUsageScope<S> implements AutoCloseable {
		@SuppressWarnings("rawtypes")
		private static final ServiceReference[] NO_REFERENCES = new ServiceReference[0];
		@SuppressWarnings("rawtypes")
		private static final ServiceInstance[] NO_INSTANCES = new ServiceInstance[0];

		private final BundleContext context;
		private final ServiceReference<S>[] references;
		private final ServiceInstance<S>[] instances;

		private Optional<S> first;

		@SuppressWarnings("unchecked")
		ServiceUsageScope(Class<S> service, Filter filter, BundleContext ctx) {
			this.context = Objects.requireNonNull(ctx);
			String filterStr = filter != null ? filter.toString() : null;
			ServiceReference<S>[] refs;
			try {
				refs = (ServiceReference<S>[]) ctx.getServiceReferences(service.getName(), filterStr);
			} catch (InvalidSyntaxException e) {
				throw new AssertionError("Illegal filter object passed", e); //$NON-NLS-1$
			}
			if (refs == null) {
				this.references = NO_REFERENCES;
				this.instances = NO_INSTANCES;
				this.first = Optional.empty();
			} else {
				Arrays.sort(refs, Comparator.reverseOrder());
				this.references = refs;
				this.instances = new ServiceInstance[references.length];
			}
		}

		/**
		 * Returns an optional holding the service object ranked highest or an empty
		 * optional if none is available.
		 * <p>
		 * The service object is {@link BundleContext#getService get} on the first
		 * invocation and cached for subsequent invocations of this method.
		 * </p>
		 * 
		 * @return the optional first instance of this scope's service
		 */
		public Optional<S> first() {
			if (first == null) {
				first = all().findFirst();
			}
			return first;
		}

		/**
		 * Returns a sorted {@code Stream} of all service objects available in this
		 * {@code ServiceUsageScope}.
		 * <p>
		 * The stream is sorted in descending order with respect to the
		 * {@link ServiceReference#compareTo(Object) instance's ServiceReference}, which
		 * places higher ranked services in the beginning of the stream.
		 * </p>
		 * 
		 * Service objects are {@link BundleContext#getService(ServiceReference) get}
		 * upon the first execution of the terminal operation for an element and then
		 * cached for subsequent invocations of this method.
		 * 
		 * @return a sorted stream of service objects
		 */
		public Stream<S> all() {
			if (references.length == 0) {
				return Stream.empty();
			}
			return allInstances().map(ServiceInstance::getService);
		}

		/**
		 * Returns a sorted {@code Stream} of all {@link ServiceInstance
		 * ServiceInstances} available in this {@code ServiceUsageScope}.
		 * <p>
		 * The stream is sorted in descending order with respect to the
		 * {@link ServiceReference#compareTo(Object) instance's ServiceReference}, which
		 * places higher ranked services in the beginning of the stream.
		 * </p>
		 * 
		 * Service objects are {@link BundleContext#getService(ServiceReference) get}
		 * upon the first execution of the terminal operation for an element and then
		 * cached for subsequent invocations of this method.
		 * 
		 * @return a sorted stream of service objects
		 */
		public Stream<ServiceInstance<S>> allInstances() {
			if (references.length == 0) {
				return Stream.empty();
			}
			return IntStream.range(0, references.length).mapToObj(i -> {
				if (instances[i] == null) {
					S s = context.getService(references[i]);
					instances[i] = s != null ? new ServiceInstance<>(s, references[i]) : null;
				}
				return instances[i];
			}).filter(Objects::nonNull).sorted();
		}

		/**
		 * Closes this scope and {@link BundleContext#ungetService(ServiceReference)
		 * ungets} all service objects get and cached in this scope. This method is
		 * invoked automatically on objects managed by the {@code try}-with-resources
		 * statement.
		 */
		@Override
		public void close() { // do not throw (checked) exception
			// TODO: prevent future usages?! Null context or references?
			RuntimeException exception = null;
			for (ServiceReference<S> serviceReference : references) {
				if (serviceReference != null) {
					try {
						context.ungetService(serviceReference);
					} catch (RuntimeException e) {
						if (exception == null) {
							exception = new IllegalStateException();
						}
						exception.addSuppressed(e);
					}
				}
			}
			if (exception != null) {
				throw exception;
			}
		}
	}

	/**
	 * Representation of a service object and the properties it was registered with.
	 * 
	 * @param <S> the service's type of this instance
	 * @since 3.19
	 */
	public static final class ServiceInstance<S> {
		private final S instance;
		private final ServiceReference<S> reference;

		ServiceInstance(S instance, ServiceReference<S> reference) {
			this.instance = instance;
			this.reference = reference;
		}

		/**
		 * Returns the actual service object.
		 * 
		 * @return the service object
		 */
		public S getService() {
			return instance;
		}

		/**
		 * Returns a copy of the properties of the service referenced by this
		 * {@code ServiceInstance} object.
		 * 
		 * @return Returns a copy of the properties of the service referenced by this
		 *         {@code ServiceInstance} object
		 * @see ServiceReference#getProperties()
		 */
		public Dictionary<String, Object> getProperties() {
			return reference.getProperties();
		}

		/**
		 * Returns the property value to which the specified property key is mapped in
		 * the properties Dictionary object of the service referenced by this
		 * {@code ServiceInstance} object.
		 * 
		 * @param key The property key
		 * @return The property value to which the key is mapped; {@code null} if there
		 *         is no property named after the key
		 * @see ServiceReference#getProperty(String)
		 */
		public Object getProperty(String key) {
			return reference.getProperty(key);
		}

		/**
		 * Returns the set of keys in the properties {@code Dictionary} object of the
		 * service referenced by this {@code ServiceInstance} object.
		 * 
		 * @return The Set of property keys
		 * @see ServiceReference#getPropertyKeys()
		 */
		public Set<String> getPropertyKeys() {
			return Set.of(reference.getPropertyKeys());
		}
	}

	/**
	 * Calls an OSGi service by dynamically looking it up and passing it to the
	 * given consumer.
	 * <p>
	 * If not running under OSGi, the caller bundle is not active or the service is
	 * not available, return false. If the service is found, call the service and
	 * return true.
	 * </p>
	 * <p>
	 * Any runtime exception thrown by the consumer is rethrown by this method. If
	 * the consumer throws a checked exception, it can be propagated using a
	 * <em>sneakyThrow</em> inside a try/catch block:
	 * </p>
	 * 
	 * <pre>
	 * callOnce(MyClass.class, Callable.class, (callable) -&gt; {
	 *   try {
	 *     callable.call();
	 *   } catch (Exception e) {
	 *     sneakyThrow(e);
	 *   }
	 * });
	 * ...
	 * {@literal @}SuppressWarnings("unchecked")
	 * static &lt;E extends Throwable&gt; void sneakyThrow(Throwable e) throws E {
	 *   throw (E) e;
	 * }
	 * </pre>
	 * 
	 * @param caller      a class from the bundle that will use service
	 * @param serviceType the OSGi service type to look up
	 * @param consumer    the consumer of the OSGi service
	 * @param <S>         the OSGi service type to look up
	 * @return true if the OSGi service was located and called successfully, false
	 *         otherwise
	 * @throws NullPointerException  if any of the parameters are {@code null}
	 * @throws IllegalStateException if the bundle associated with the caller class
	 *                               cannot be determined
	 */
	public static <S> boolean callOnce(Class<?> caller, Class<S> serviceType, Consumer<S> consumer) {
		return callOnce(caller, serviceType, null, consumer);
	}

	/**
	 * As {@link #callOnce(Class, Class, Consumer)} with an additional OSGi filter.
	 * 
	 * @param caller      a class from the bundle that will use service
	 * @param serviceType the OSGi service type to look up
	 * @param consumer    the consumer of the OSGi service
	 * @param filter      an OSGi filter to restrict the services found
	 * @param <S>         the OSGi service type to look up
	 * @return true if the OSGi service was located and called successfully, false
	 *         otherwise
	 * @throws NullPointerException  if any of the parameters are {@code null}
	 * @throws IllegalStateException if the bundle associated with the caller class
	 *                               cannot be determined
	 */
	public static <S> boolean callOnce(Class<?> caller, Class<S> serviceType, String filter, Consumer<S> consumer) {
		return new ServiceCaller<>(caller, serviceType, filter).getCurrent().map(r -> {
			try {
				consumer.accept(r.instance);
				return Boolean.TRUE;
			} finally {
				r.unget();
			}
		}).orElse(Boolean.FALSE);
	}

	private static int getRank(ServiceReference<?> ref) {
		Object rank = ref.getProperty(Constants.SERVICE_RANKING);
		if (rank instanceof Integer) {
			return ((Integer) rank).intValue();
		}
		return 0;
	}

	private class ReferenceAndService implements SynchronousBundleListener, ServiceListener {
		final BundleContext context;
		final ServiceReference<S> ref;
		final S instance;
		final int rank;

		public ReferenceAndService(final BundleContext context, ServiceReference<S> ref, S instance) {
			this.context = context;
			this.ref = ref;
			this.instance = instance;
			this.rank = getRank(ref);
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
				unget();
			}
		}

		@Override
		public void serviceChanged(ServiceEvent e) {
			if (requiresUnget(e)) {
				unget();
			}
		}

		private boolean requiresUnget(ServiceEvent e) {
			if (e.getServiceReference().equals(ref)) {
				return (e.getType() == ServiceEvent.UNREGISTERING)
						|| (filter != null && e.getType() == ServiceEvent.MODIFIED_ENDMATCH)
						|| (e.getType() == ServiceEvent.MODIFIED && getRank(ref) != rank);
				// if rank changed: untrack to force a new ReferenceAndService with new rank
			}
			return e.getType() == ServiceEvent.MODIFIED && getRank(e.getServiceReference()) > rank;
		}

		// must hold monitor on ServiceCaller.this when calling track
		Optional<ReferenceAndService> track() {
			try {
				ServiceCaller.this.service = this;
				context.addServiceListener(this, "(&" //$NON-NLS-1$
						+ "(objectClass=" + serviceType.getName() + ")" // //$NON-NLS-1$ //$NON-NLS-2$
						+ (filter == null ? "" : filter) // //$NON-NLS-1$
						+ ")"); //$NON-NLS-1$
				context.addBundleListener(this);
				if ((ref.getBundle() == null || context.getBundle() == null) && ServiceCaller.this.service == this) {
					// service should have been untracked but we may have missed the event
					// before we could added the listeners
					unget();
				}
				if (getRank(ref) != rank) {
					// ranking has changed; unget to force reget in case the ranking is not the
					// highest
					unget();
				}
			} catch (InvalidSyntaxException e) {
				// really should never happen with our own filter above.
				ServiceCaller.this.service = null;
				throw new IllegalStateException(e);
			} catch (IllegalStateException e) {
				// bundle was stopped before we could get listeners added/removed
				ServiceCaller.this.service = null;
			}
			// Note that we always return this ReferenceAndService
			// even for cases where the instance was unget
			// It is way complicated to try again and
			// even if we did the returned value can become
			// stale right after return.
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

	private final Bundle bundle;
	private final Class<S> serviceType;
	private final String filter;
	private volatile ReferenceAndService service = null;

	/**
	 * Creates a {@code ServiceCaller} instance for invoking an OSGi service many
	 * times with a consumer function.
	 * 
	 * @param caller      a class from the bundle that will consume the service
	 * @param serviceType the OSGi service type to look up
	 * @throws NullPointerException  if any of the parameters are {@code null}
	 * @throws IllegalStateException if the bundle associated with the caller class
	 *                               cannot be determined
	 */
	public ServiceCaller(Class<?> caller, Class<S> serviceType) {
		this(caller, serviceType, null);
	}

	/**
	 * Creates a {@code ServiceCaller} instance for invoking an OSGi service many
	 * times with a consumer function.
	 * 
	 * @param caller      a class from the bundle that will consume the service
	 * @param serviceType the OSGi service type to look up
	 * @param filter      the service filter used to look up the service. May be
	 *                    {@code null}.
	 * @throws NullPointerException  if any of the parameters are {@code null}
	 * @throws IllegalStateException if the bundle associated with the caller class
	 *                               cannot be determined
	 */
	public ServiceCaller(Class<?> caller, Class<S> serviceType, String filter) {
		this.serviceType = Objects.requireNonNull(serviceType);
		this.bundle = Optional.of(Objects.requireNonNull(caller)).map(FrameworkUtil::getBundle)
				.orElseThrow(IllegalStateException::new);
		this.filter = filter;
		if (filter != null) {
			try {
				FrameworkUtil.createFilter(filter);
			} catch (InvalidSyntaxException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	private BundleContext getContext() {
		if (System.getSecurityManager() != null) {
			return AccessController.doPrivileged((PrivilegedAction<BundleContext>) () -> bundle.getBundleContext());
		}
		return bundle.getBundleContext();
	}

	/**
	 * Calls an OSGi service by dynamically looking it up and passing it to the
	 * given consumer. If not running under OSGi, the caller bundle is not active or
	 * the service is not available, return false. Any runtime exception thrown by
	 * the consumer is rethrown by this method. (For handling checked exceptions,
	 * see {@link #callOnce(Class, Class, Consumer)} for a solution.) Subsequent
	 * calls to this method will attempt to reuse the previously acquired service
	 * instance until one of the following occurs:
	 * <ul>
	 * <li>The {@link #unget()} method is called.</li>
	 * <li>The service is unregistered.</li>
	 * <li>The service properties change such that this {@code ServiceCaller} filter
	 * no longer matches.
	 * <li>The caller bundle is stopped.</li>
	 * <li>The service rankings have changed.</li>
	 * </ul>
	 * 
	 * After one of these conditions occur subsequent calls to this method will try
	 * to acquire the another service instance.
	 * 
	 * @param consumer the consumer of the OSGi service
	 * @return true if the OSGi service was located and called successfully, false
	 *         otherwise
	 */
	public boolean call(Consumer<S> consumer) {
		return trackCurrent().map(r -> {
			consumer.accept(r.instance);
			return Boolean.TRUE;
		}).orElse(Boolean.FALSE);
	}

	/**
	 * Return the currently available service.
	 * 
	 * @return the currently available service or empty if the service cannot be
	 *         found.
	 */
	public Optional<S> current() {
		return trackCurrent().map(r -> r.instance);
	}

	private Optional<ReferenceAndService> trackCurrent() {
		ReferenceAndService current = service;
		if (current != null) {
			return Optional.of(current);
		}
		return getCurrent().flatMap(r -> {
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
		return getServiceReference(context).map(r -> {
			S current = context.getService(r);
			return current == null ? null : new ReferenceAndService(context, r, current);
		});
	}

	private Optional<ServiceReference<S>> getServiceReference(BundleContext context) {
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
	 * Releases the cached service object, if it exists. Another invocation of
	 * {@link #call(Consumer)} will lazily get the service instance again and cache
	 * the new instance if found.
	 */
	public void unget() {
		ReferenceAndService current = service;
		if (current != null) {
			current.unget();
		}
	}
}
