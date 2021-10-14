/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
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
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.RandomAccess;
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

public class InternalUtils {

	/**
	 * Returns a mutable wrapped around the given list, that creates a copy of the
	 * given list only if the list is about to be modified.
	 * <p>
	 * This method assumes that the given List is immutable and a
	 * {@link RandomAccess} list.
	 * </p>
	 * 
	 * @param list the list to be copied.
	 * @return an effectively mutable and lazy copy of the given list.
	 */
	public static <T> List<T> asCopy(List<? extends T> list) {
		if (list == null) {
			return null;
		}
		if (!(list instanceof RandomAccess)) {
			throw new IllegalArgumentException("Only RandomAccess lists are supported"); //$NON-NLS-1$
		}
		return new CopyOnFirstWriteList<>(list);
	}

	private static final class CopyOnFirstWriteList<T> extends AbstractList<T> implements RandomAccess {
		private List<T> copy;
		private boolean copied = false;

		CopyOnFirstWriteList(List<? extends T> list) {
			copy = asList(list);
		}

		@Override
		public T get(int index) {
			return copy.get(index);
		}

		@Override
		public int size() {
			return copy.size();
		}

		@Override
		public void add(int index, T element) {
			ensureCopied();
			copy.add(index, element);
			modCount++;
		}

		@Override
		public T remove(int index) {
			ensureCopied();
			T removed = copy.remove(index);
			modCount++;
			return removed;
		}

		@Override
		public T set(int index, T element) {
			ensureCopied();
			T set = copy.set(index, element);
			modCount++;
			return set;
		}

		private void ensureCopied() {
			if (!copied) {
				copy = new ArrayList<>(copy);
				copied = true;
			}
		}
	}

	/**
	 * Coerce the generic type of a list from List<? extends T> to List<T>
	 * 
	 * @param l List to be coerced.
	 * @return l coerced to List<Capability>
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> asList(List<? extends T> l) {
		return (List<T>) l;
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

	public static <E> Enumeration<E> asEnumeration(Iterator<E> it) {
		return new Enumeration<E>() {
			@Override
			public boolean hasMoreElements() {
				return it.hasNext();
			}

			@Override
			public E nextElement() {
				return it.next();
			}
		};
	}
}
