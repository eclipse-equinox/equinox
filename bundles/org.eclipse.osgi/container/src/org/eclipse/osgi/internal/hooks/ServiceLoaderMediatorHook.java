package org.eclipse.osgi.internal.hooks;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.loader.ModuleClassLoader;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

public class ServiceLoaderMediatorHook extends ClassLoaderHook {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Supplier<Optional<Class<?>>> getCallerClassComputer() {
		try { // The Java-9+ way
			Class<?> stackWalkerClass = Class.forName("java.lang.StackWalker"); //$NON-NLS-1$
			Class stackWalkerOption = Class.forName("java.lang.StackWalker$Option"); //$NON-NLS-1$
			Enum<?> retainClassReference = Enum.valueOf(stackWalkerOption, "RETAIN_CLASS_REFERENCE"); //$NON-NLS-1$
			Object stackWalker = stackWalkerClass.getMethod("getInstance", Set.class, int.class).invoke(null, //$NON-NLS-1$
					new HashSet<>(Arrays.asList(retainClassReference)), 15);

			Method stackWalkerWalk = stackWalkerClass.getMethod("walk", Function.class); //$NON-NLS-1$
			Class<?> stackFrameClass = Class.forName("java.lang.StackWalker$StackFrame"); //$NON-NLS-1$
			Method stackFrameGetDeclaringClass = stackFrameClass.getMethod("getDeclaringClass"); //$NON-NLS-1$

			Function<Stream<?>, Optional<Class<?>>> function = frames -> frames.<Class<?>>map(f -> {
				try {
					return (Class<?>) stackFrameGetDeclaringClass.invoke(f);
				} catch (ReflectiveOperationException e) {
					return null;
				}
			}).filter(Objects::nonNull).limit(15)
					.filter(c -> c == ServiceLoader.class || c.getEnclosingClass() == ServiceLoader.class).findFirst();

			return () -> {
				try {
					return (Optional<Class<?>>) stackWalkerWalk.invoke(stackWalker, function);
				} catch (ReflectiveOperationException e) {
					return Optional.empty();
				}
			};
		} catch (ReflectiveOperationException e) { // ignore
		}
		try { // Try the Java-1.8 way
			Class<?> reflectionClass = Class.forName("sun.reflect.Reflection"); //$NON-NLS-1$
			Method getCallerClass = Objects.requireNonNull(reflectionClass.getMethod("getCallerClass", int.class)); //$NON-NLS-1$
			return () -> IntStream.range(0, 15).<Class<?>>mapToObj(i -> {
				try {
					return (Class<?>) getCallerClass.invoke(null, i);
				} catch (ReflectiveOperationException e) {
					return null;
				}
			}).filter(Objects::nonNull)
					.filter(c -> c == ServiceLoader.class || c.getEnclosingClass() == ServiceLoader.class).findFirst();

		} catch (ReflectiveOperationException e) { // ignore an try Java-8 way
		}
		throw new AssertionError("Neither the Java-8 nor the Java-9+ way to obtain the caller class is available"); //$NON-NLS-1$
	}

	private static final String SERVICE_NAME_PREFIX = "META-INF/services/"; //$NON-NLS-1$

	private final ThreadLocal<String> latestServiceName = new ThreadLocal<>();

	private final Supplier<Optional<Class<?>>> callerClass = getCallerClassComputer();

	@Override
	public Enumeration<URL> preFindResources(String name, ModuleClassLoader classLoader) throws FileNotFoundException {
		if (name.startsWith(SERVICE_NAME_PREFIX)) {
			Stream<URL> serviceProviderFiles = searchProviders(name, classLoader, cl -> {
				try {
					Enumeration<URL> resources = cl.getResources(name);
					return Collections.list(resources).stream();
				} catch (IOException e) {
					return Stream.empty();
				}
			});
			if (serviceProviderFiles != null) {
				latestServiceName.set(name);
				List<URL> services = serviceProviderFiles.collect(Collectors.toList());
				return Collections.enumeration(services);
			}
		}
		return null;
	}

	@Override
	public Class<?> preFindClass(String name, ModuleClassLoader classLoader) throws ClassNotFoundException {
		String serviceName = latestServiceName.get(); // This cannot be removed if subsequent class load other
														// providers for the same service
		if (serviceName != null) {
			Stream<Class<?>> services = searchProviders(serviceName, classLoader, cl -> {
				try {
					return Stream.of(cl.loadClass(name));
				} catch (ClassNotFoundException | NoClassDefFoundError e) { // ignore
					return Stream.empty();
				}
			});
			if (services != null) {
				Optional<Class<?>> findFirst = services.findFirst();
				return findFirst.orElse(null);
			}
		}
		return null;
	}

	private <T> Stream<T> searchProviders(String name, ModuleClassLoader classLoader,
			Function<ClassLoader, Stream<T>> mapper) {
		BundleWiring wiring = classLoader.getBundle().adapt(BundleWiring.class);
		if (requiresServiceLoaderProcessor(wiring) && isCalledFromServiceLoader()) {
			return Stream.concat(Stream.of(classLoader), providerClassLoaders(name, wiring)).flatMap(mapper);
		}
		return null;
	}

	private boolean requiresServiceLoaderProcessor(BundleWiring wiring) {
		List<BundleWire> requiredWires = wiring.getRequiredWires("osgi.extender"); //$NON-NLS-1$
		if (requiredWires.isEmpty()) {
			return false;
		}
		return requiredWires.stream().anyMatch(w -> w.getRequirement().getDirectives().getOrDefault("filter", "") //$NON-NLS-1$//$NON-NLS-2$
				.contains("(osgi.extender=osgi.serviceloader.processor)") //$NON-NLS-1$
				&& w.getProvider().getBundle().getBundleId() == Constants.SYSTEM_BUNDLE_ID);
	}

	private boolean isCalledFromServiceLoader() {
		Optional<Class<?>> caller = callerClass.get();
		// TODO: If Java-9+ is required one day, just use the following code
//		ClassLoader thisLoader = ServiceLoaderMediatorHook.class.getClassLoader();
//		Optional<Class<?>> caller = WALKER.walk(frames -> frames.<Class<?>>map(f -> f.getDeclaringClass())
//				.dropWhile(c -> c.getName().startsWith("org.eclipse.osgi") && c.getClassLoader() == thisLoader) //$NON-NLS-1$
//				.findFirst());
		return caller.isPresent() && caller.get().getEnclosingClass() == ServiceLoader.class;
	}

	private static Stream<ClassLoader> providerClassLoaders(String name, BundleWiring wiring) {
		List<BundleWire> requiredWires = wiring.getRequiredWires("osgi.serviceloader"); //$NON-NLS-1$
		if (!requiredWires.isEmpty()) {
			String serviceName = name.substring(SERVICE_NAME_PREFIX.length());
			return requiredWires.stream().filter(wire -> {
				BundleRequirement requirement = wire.getRequirement();
				String filterDirective = requirement.getDirectives().get("filter"); //$NON-NLS-1$
				Object serviceLoaderAttribute = wire.getCapability().getAttributes().get("osgi.serviceloader"); //$NON-NLS-1$
				return serviceName.equals(serviceLoaderAttribute)
						&& ("(osgi.serviceloader=" + serviceName + ")").equals(filterDirective); //$NON-NLS-1$ //$NON-NLS-2$
			}).map(wire -> wire.getProviderWiring().getClassLoader());

		}
		return Stream.empty();
	}

}
