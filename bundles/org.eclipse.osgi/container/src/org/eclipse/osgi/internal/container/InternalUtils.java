/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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
package org.eclipse.osgi.internal.container;

import java.security.Permission;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundlePermission;
import org.osgi.framework.CapabilityPermission;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

public class InternalUtils {

	/**
	 * Coerce the generic type of a list from List<BundleCapability>
	 * to List<Capability>
	 * @param l List to be coerced.
	 * @return l coerced to List<Capability>
	 */
	@SuppressWarnings("unchecked")
	public static List<Capability> asListCapability(List<? extends Capability> l) {
		return (List<Capability>) l;
	}

	/**
	 * Coerce the generic type of a list from List<BundleRequirement>
	 * to List<Requirement>
	 * @param l List to be coerced.
	 * @return l coerced to List<Requirement>
	 */
	@SuppressWarnings("unchecked")
	public static List<Requirement> asListRequirement(List<? extends Requirement> l) {
		return (List<Requirement>) l;
	}

	/**
	 * Coerce the generic type of a list from List<? extends BundleCapability>
	 * to List<BundleCapability>
	 * @param l List to be coerced.
	 * @return l coerced to List<BundleCapability>
	 */
	@SuppressWarnings("unchecked")
	public static List<BundleCapability> asListBundleCapability(List<? extends BundleCapability> l) {
		return (List<BundleCapability>) l;
	}

	/**
	 * Coerce the generic type of a list from List<? extends BundleRequirement>
	 * to List<BundleRequirement>
	 * @param l List to be coerced.
	 * @return l coerced to List<BundleRequirement>
	 */
	@SuppressWarnings("unchecked")
	public static List<BundleRequirement> asListBundleRequirement(List<? extends BundleRequirement> l) {
		return (List<BundleRequirement>) l;
	}

	/**
	 * Coerce the generic type of a list from List<? extends BundleWire>
	 * to List<BundleWire>
	 * @param l List to be coerced.
	 * @return l coerced to List<BundleWire>
	 */
	@SuppressWarnings("unchecked")
	public static List<BundleWire> asListBundleWire(List<? extends BundleWire> l) {
		return (List<BundleWire>) l;
	}

	/**
	 * Coerce the generic type of a list from List<? extends BundleWire>
	 * to List<BundleWire>
	 * @param l List to be coerced.
	 * @return l coerced to List<BundleWire>
	 */
	@SuppressWarnings("unchecked")
	public static List<Wire> asListWire(List<? extends Wire> l) {
		return (List<Wire>) l;
	}

	/**
	 * Coerce the generic type of a list from List<? extends BundleRevision>
	 * to List<BundleRevision>
	 * @param l List to be coerced.
	 * @return l coerced to List<BundleRevision>
	 */
	@SuppressWarnings("unchecked")
	public static List<BundleRevision> asListBundleRevision(List<? extends BundleRevision> l) {
		return (List<BundleRevision>) l;
	}

	/**
	 * Coerce the generic type of a collection from Collection<? extends Resource>
	 * to Collection<Resource>
	 * @param c List to be coerced.
	 * @return c coerced to Collection<Resource>
	 */
	@SuppressWarnings("unchecked")
	public static Collection<Resource> asCollectionResource(Collection<? extends Resource> c) {
		return (Collection<Resource>) c;
	}

	/**
	 * Coerce the generic type of a collection from Collection<? extends BundleWiring>
	 * to Collection<BundleWiring>
	 * @param c List to be coerced.
	 * @return c coerced to Collection<BundleWiring>
	 */
	@SuppressWarnings("unchecked")
	public static Collection<BundleWiring> asCollectionBundleWiring(Collection<? extends BundleWiring> c) {
		return (Collection<BundleWiring>) c;
	}

	public static void filterCapabilityPermissions(Collection<? extends BundleCapability> capabilities) {
		if (System.getSecurityManager() == null) {
			return;
		}
		for (Iterator<? extends BundleCapability> iCapabilities = capabilities.iterator(); iCapabilities.hasNext();) {
			BundleCapability capability = iCapabilities.next();
			Permission permission = getProvidePermission(capability);
			Bundle provider = capability.getRevision().getBundle();
			if (provider != null && !provider.hasPermission(permission)) {
				iCapabilities.remove();
			}
		}
	}

	public static Permission getRequirePermission(BundleCapability candidate) {
		String name = candidate.getNamespace();
		if (PackageNamespace.PACKAGE_NAMESPACE.equals(name)) {
			return new PackagePermission(getPermisionName(candidate), candidate.getRevision().getBundle(), PackagePermission.IMPORT);
		}
		if (HostNamespace.HOST_NAMESPACE.equals(name)) {
			return new BundlePermission(getPermisionName(candidate), BundlePermission.FRAGMENT);
		}
		if (BundleNamespace.BUNDLE_NAMESPACE.equals(name)) {
			return new BundlePermission(getPermisionName(candidate), BundlePermission.REQUIRE);
		}
		return new CapabilityPermission(name, candidate.getAttributes(), candidate.getRevision().getBundle(), CapabilityPermission.REQUIRE);
	}

	public static Permission getProvidePermission(BundleCapability candidate) {
		String name = candidate.getNamespace();
		if (PackageNamespace.PACKAGE_NAMESPACE.equals(name)) {
			return new PackagePermission(getPermisionName(candidate), PackagePermission.EXPORTONLY);
		}
		if (HostNamespace.HOST_NAMESPACE.equals(name)) {
			return new BundlePermission(getPermisionName(candidate), BundlePermission.HOST);
		}
		if (BundleNamespace.BUNDLE_NAMESPACE.equals(name)) {
			return new BundlePermission(getPermisionName(candidate), BundlePermission.PROVIDE);
		}
		return new CapabilityPermission(name, CapabilityPermission.PROVIDE);
	}

	private static String getPermisionName(BundleCapability candidate) {
		Object name = candidate.getAttributes().get(candidate.getNamespace());
		if (name instanceof String) {
			return (String) name;
		}
		if (name instanceof Collection) {
			Collection<?> names = (Collection<?>) name;
			return names.isEmpty() ? "unknown" : names.iterator().next().toString(); //$NON-NLS-1$
		}
		return "unknown"; //$NON-NLS-1$
	}

	public static String newUUID(EquinoxConfiguration config) {
		// Note that we use simple Random to improve
		// performance and the Framework UUID generation does not require
		// a full SecureRandom seed.
		boolean useSecureRandom = "true".equals(config.getConfiguration(EquinoxConfiguration.PROP_SECURE_UUID)); //$NON-NLS-1$
		byte[] uuidBytes = new byte[16];
		if (useSecureRandom) {
			new SecureRandom().nextBytes(uuidBytes);
		} else {
			new Random().nextBytes(uuidBytes);
		}
		// clear the version bits - set to use version 4
		uuidBytes[6] &= 0x0f;
		uuidBytes[6] |= 0x40;
		// clear the variant bits - set to use IETF variant
		uuidBytes[8] &= 0x3f;
		uuidBytes[8] |= 0x80;
		// split into the most and least significant bits
		long mostSignificantBits = 0;
		long leastSignificantBits = 0;
		for (int i = 0; i < 8; i++) {
			mostSignificantBits = (mostSignificantBits << 8) | (uuidBytes[i] & 0xff);
		}
		for (int i = 8; i < 16; i++) {
			leastSignificantBits = (leastSignificantBits << 8) | (uuidBytes[i] & 0xff);
		}
		return new UUID(mostSignificantBits, leastSignificantBits).toString();
	}

}
