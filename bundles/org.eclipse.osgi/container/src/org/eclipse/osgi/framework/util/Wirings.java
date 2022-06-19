/*******************************************************************************
 *  Copyright (c) 2022, 2022 Hannes Wellmann and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     Hannes Wellmann - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.framework.util;

import static java.util.function.Predicate.not;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.osgi.container.ModuleContainer;
import org.eclipse.osgi.internal.framework.SystemBundleActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;

public class Wirings { // Wiring Util

	private Wirings() {
	}

	// TODO: check usage

	public static Stream<Bundle> getBundles(String symbolicName) {
		FrameworkWiring fkw = SystemBundleActivator.getSystemBundle().adapt(FrameworkWiring.class);

		String filter = "(" + IdentityNamespace.IDENTITY_NAMESPACE + "=" + symbolicName + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		Requirement identity = ModuleContainer.createRequirement(IdentityNamespace.IDENTITY_NAMESPACE,
				Map.of(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter), Map.of());

		return fkw.findProviders(identity).stream().map(c -> c.getRevision().getBundle())
				// a sanity check in case this is an old revision
				.filter(b -> symbolicName.equals(b.getSymbolicName()));
	}

	public static Predicate<Bundle> inState(int... states) {
		int mask = Arrays.stream(states).reduce((f1, f2) -> f1 | f2).orElse(0);
		return b -> (b.getState() & mask) != 0;
	}

	public static Optional<Bundle> getBundle(String symbolicName) {
		return getBundles(symbolicName).findAny();
	}

	public static Optional<Bundle> getAtLeastResolvedBundle(String symbolicName) {
		return Wirings.getBundles(symbolicName) //
				.filter(not(inState(Bundle.INSTALLED, Bundle.UNINSTALLED))).findFirst();
	}

	public static boolean isFragment(Bundle bundle) {
		BundleRevision revision = bundle.adapt(BundleRevision.class);
		return (revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0;
	}

	public static List<Bundle> getFragments(Bundle host) { // TODO: or return stream?
		return getProvidedBundles(host, HostNamespace.HOST_NAMESPACE);
	}

	public static List<Bundle> getHosts(Bundle fragment) { // TODO: or return stream?
		return getRequiredBundles(fragment, HostNamespace.HOST_NAMESPACE);
	}

	public static List<Bundle> getRequiredBundles(Bundle bundle, String namespace) {
		return getWiredBundles(bundle, namespace, BundleWiring::getRequiredWires, w -> w.getCapability().getRevision());
	}

	public static List<Bundle> getProvidedBundles(Bundle bundle, String namespace) {
		return getWiredBundles(bundle, namespace, BundleWiring::getProvidedWires,
				w -> w.getRequirement().getRevision());
	}

	private static List<Bundle> getWiredBundles(Bundle bundle, String namespace,
			BiFunction<BundleWiring, String, List<BundleWire>> getWires,
			Function<BundleWire, BundleRevision> getRevision) {
		return getWires(bundle, namespace, getWires).stream() //
				.map(getRevision).map(BundleRevision::getBundle) //
				.collect(Collectors.toList());
	}

	public static List<BundleWire> getRequiredWires(Bundle bundle, String namespace) {
		return getWires(bundle, namespace, BundleWiring::getRequiredWires);
	}

	public static List<BundleWire> getProvidedWires(Bundle bundle, String namespace) {
		return getWires(bundle, namespace, BundleWiring::getProvidedWires);
	}

	private static List<BundleWire> getWires(Bundle bundle, String namespace,
			BiFunction<BundleWiring, String, List<BundleWire>> getWires) {
		BundleWiring wiring = bundle.adapt(BundleWiring.class);
		if (wiring == null) {
			return Collections.emptyList();
		}
		List<BundleWire> wires = getWires.apply(wiring, namespace);
		return wires != null ? wires : Collections.emptyList();
	}
}
