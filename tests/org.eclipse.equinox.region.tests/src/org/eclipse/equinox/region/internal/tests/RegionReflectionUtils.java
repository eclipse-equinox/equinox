/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.region.internal.tests;

import static org.junit.Assert.fail;

import java.lang.reflect.*;
import java.util.Set;
import org.eclipse.equinox.region.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.bundle.*;
import org.osgi.framework.hooks.resolver.ResolverHook;

public class RegionReflectionUtils {
	static private final ClassLoader regionCL = Region.class.getClassLoader();

	static private final String BundleIdBasedRegion = "org.eclipse.equinox.internal.region.BundleIdBasedRegion";

	static private final String StandardBundleIdToRegionMapping = "org.eclipse.equinox.internal.region.StandardBundleIdToRegionMapping";
	static private final String StandardBundleIdToRegionMapping_associateBundleWithRegion = "associateBundleWithRegion";
	static private final String StandardBundleIdToRegionMapping_getRegion = "getRegion";
	private static final String StandardBundleIdToRegionMapping_dissociateBundleFromRegion = "dissociateBundleFromRegion";
	private static final String StandardBundleIdToRegionMapping_isBundleAssociatedWithRegion = "isBundleAssociatedWithRegion";
	private static final String StandardBundleIdToRegionMapping_getBundleIds = "getBundleIds";
	private static final String StandardBundleIdToRegionMapping_clear = "clear";

	private static final String StandardRegionDigraph = "org.eclipse.equinox.internal.region.StandardRegionDigraph";

	private static final String StandardRegionFilterBuilder = "org.eclipse.equinox.internal.region.StandardRegionFilterBuilder";

	private static final String RegionBundleCollisionHook = "org.eclipse.equinox.internal.region.hook.RegionBundleCollisionHook";

	private static final String RegionBundleEventHook = "org.eclipse.equinox.internal.region.hook.RegionBundleEventHook";

	private static final String RegionBundleFindHook = "org.eclipse.equinox.internal.region.hook.RegionBundleFindHook";

	private static final String RegionResolverHook = "org.eclipse.equinox.internal.region.hook.RegionResolverHook";

	private static final String RegionServiceEventHook = "org.eclipse.equinox.internal.region.hook.RegionServiceEventHook";

	private static final String RegionServiceFindHook = "org.eclipse.equinox.internal.region.hook.RegionServiceFindHook";

	private static final String BundleIdToRegionMapping = "org.eclipse.equinox.internal.region.BundleIdToRegionMapping";

	public static Object newStandardBundleIdToRegionMapping() {
		Class<?> clazz = loadRegionImplClass(StandardBundleIdToRegionMapping);
		return newInstance(clazz);
	}

	private static Object newInstance(Class<?> clazz) {
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			fail(e.getMessage());
		}
		return null;
	}

	private static Class<?> loadRegionImplClass(String name) {
		try {
			return regionCL.loadClass(name);
		} catch (ClassNotFoundException e) {
			fail(e.getMessage());
		}
		return null;
	}

	public static Region newBundleIdBasedRegion(String regionName, RegionDigraph regionDigraph,
			Object bundleIdToRegionMapping, BundleContext bundleContext, ThreadLocal<Region> threadLocal) {
		Class<?> bundleIdBasedRegionClazz = loadRegionImplClass(BundleIdBasedRegion);
		Class<?> bundleIdToRegionMappingClazz = loadRegionImplClass(BundleIdToRegionMapping);
		Class<?>[] classParams = new Class<?>[] { String.class, RegionDigraph.class, bundleIdToRegionMappingClazz,
				BundleContext.class, ThreadLocal.class };
		Object[] constructorArgs = new Object[] { regionName, regionDigraph, bundleIdToRegionMapping, bundleContext,
				threadLocal };
		return (Region) newInstance(bundleIdBasedRegionClazz, classParams, constructorArgs);
	}

	private static Object newInstance(Class<?> clazz, Class<?>[] parameterTypes, Object... constructorArgs) {
		try {
			Constructor<?> constructor = clazz.getConstructor(parameterTypes);
			return constructor.newInstance(constructorArgs);
		} catch (SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException
				| IllegalAccessException | InvocationTargetException e) {
			fail(e.getMessage());
		}
		return null;
	}

	public static void associateBundleWithRegion(Object bundleIdToRegionMapping, long bundleId, Region region) {
		callMethod(bundleIdToRegionMapping, StandardBundleIdToRegionMapping_associateBundleWithRegion,
				new Class[] { long.class, Region.class }, bundleId, region);
	}

	public static void dissociateBundleFromRegion(Object bundleIdToRegionMapping, long bundleId, Region region) {
		callMethod(bundleIdToRegionMapping, StandardBundleIdToRegionMapping_dissociateBundleFromRegion,
				new Class[] { long.class, Region.class }, bundleId, region);
	}

	private static Object callMethod(Object target, String methodName, Class<?>[] paramTypes, Object... paramArgs) {
		try {
			Method method = target.getClass().getMethod(methodName, paramTypes);
			return method.invoke(target, paramArgs);
		} catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException
				| InvocationTargetException e) {
			fail(e.getMessage());
		}
		return null;
	}

	public static Region getRegion(Object bundleIdToRegionMapping, long testBundleId) {
		return (Region) callMethod(bundleIdToRegionMapping, StandardBundleIdToRegionMapping_getRegion,
				new Class<?>[] { long.class }, testBundleId);
	}

	public static boolean isBundleAssociatedWithRegion(Object bundleIdToRegionMapping, long testBundleId,
			Region region) {
		return (Boolean) callMethod(bundleIdToRegionMapping,
				StandardBundleIdToRegionMapping_isBundleAssociatedWithRegion,
				new Class<?>[] { long.class, Region.class }, testBundleId, region);
	}

	@SuppressWarnings("unchecked")
	public static Set<Long> getBundleIds(Object bundleIdToRegionMapping, Region region) {
		return (Set<Long>) callMethod(bundleIdToRegionMapping, StandardBundleIdToRegionMapping_getBundleIds,
				new Class<?>[] { Region.class }, region);
	}

	public static void clear(Object bundleIdToRegionMapping) {
		callMethod(bundleIdToRegionMapping, StandardBundleIdToRegionMapping_clear, null);
	}

	public static RegionDigraph newStandardRegionDigraph(BundleContext systemBundleContext,
			ThreadLocal<Region> threadLocal) {
		Class<?> standardRegionDigraphClazz = loadRegionImplClass(StandardRegionDigraph);
		Class<?>[] classParams = new Class<?>[] { BundleContext.class, ThreadLocal.class };
		Object[] constructorArgs = new Object[] { systemBundleContext, threadLocal };
		return (RegionDigraph) newInstance(standardRegionDigraphClazz, classParams, constructorArgs);
	}

	public static RegionDigraph newStandardRegionDigraph() {
		Class<?> standardRegionDigraphClazz = loadRegionImplClass(StandardRegionDigraph);
		Class<?>[] classParams = new Class<?>[] { standardRegionDigraphClazz };
		Object[] constructorArgs = new Object[] { null };
		return (RegionDigraph) newInstance(standardRegionDigraphClazz, classParams, constructorArgs);
	}

	public static RegionFilterBuilder newStandardRegionFilterBuilder() {
		Class<?> standardRegionFilterBuilderClazz = loadRegionImplClass(StandardRegionFilterBuilder);
		return (RegionFilterBuilder) newInstance(standardRegionFilterBuilderClazz);
	}

	public static CollisionHook newRegionBundleCollisionHook(RegionDigraph digraph, ThreadLocal<Region> threadLocal) {
		Class<?> regionBundleCollisionHook = loadRegionImplClass(RegionBundleCollisionHook);
		Class<?>[] classParams = new Class<?>[] { RegionDigraph.class, ThreadLocal.class };
		Object[] constructorArgs = new Object[] { digraph, threadLocal };
		return (CollisionHook) newInstance(regionBundleCollisionHook, classParams, constructorArgs);
	}

	public static EventHook newRegionBundleEventHook(RegionDigraph digraph, ThreadLocal<Region> threadLocal,
			long bundleId) {
		Class<?> regionBundleEventHook = loadRegionImplClass(RegionBundleEventHook);
		Class<?>[] classParams = new Class<?>[] { RegionDigraph.class, ThreadLocal.class, long.class };
		Object[] constructorArgs = new Object[] { digraph, threadLocal, bundleId };
		return (EventHook) newInstance(regionBundleEventHook, classParams, constructorArgs);
	}

	public static FindHook newRegionBundleFindHook(RegionDigraph digraph, long bundleId) {
		Class<?> regionBundleFindHook = loadRegionImplClass(RegionBundleFindHook);
		Class<?>[] classParams = new Class<?>[] { RegionDigraph.class, long.class };
		Object[] constructorArgs = new Object[] { digraph, bundleId };
		return (FindHook) newInstance(regionBundleFindHook, classParams, constructorArgs);
	}

	public static ResolverHook newRegionResolverHook(RegionDigraph digraph) {
		Class<?> regionResolverHook = loadRegionImplClass(RegionResolverHook);
		Class<?>[] classParams = new Class<?>[] { RegionDigraph.class };
		Object[] constructorArgs = new Object[] { digraph };
		return (ResolverHook) newInstance(regionResolverHook, classParams, constructorArgs);
	}

	public static org.osgi.framework.hooks.service.EventHook newRegionServiceEventHook(RegionDigraph digraph) {
		Class<?> regionServiceEventHook = loadRegionImplClass(RegionServiceEventHook);
		Class<?>[] classParams = new Class<?>[] { RegionDigraph.class };
		Object[] constructorArgs = new Object[] { digraph };
		return (org.osgi.framework.hooks.service.EventHook) newInstance(regionServiceEventHook, classParams,
				constructorArgs);
	}

	public static org.osgi.framework.hooks.service.FindHook newRegionServiceFindHook(RegionDigraph digraph) {
		Class<?> regionServiceFindHook = loadRegionImplClass(RegionServiceFindHook);
		Class<?>[] classParams = new Class<?>[] { RegionDigraph.class };
		Object[] constructorArgs = new Object[] { digraph };
		return (org.osgi.framework.hooks.service.FindHook) newInstance(regionServiceFindHook, classParams,
				constructorArgs);
	}
}
