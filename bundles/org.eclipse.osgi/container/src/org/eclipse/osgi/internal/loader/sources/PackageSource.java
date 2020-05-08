/*******************************************************************************
 * Copyright (c) 2003, 2014 IBM Corporation and others.
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
package org.eclipse.osgi.internal.loader.sources;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.container.ModuleWiring;
import org.eclipse.osgi.internal.framework.EquinoxBundle;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.service.packageadmin.PackageAdmin;

public abstract class PackageSource {
	protected final String id;

	public PackageSource(String id) {
		// others depend on the id being interned; see SingleSourcePackage.equals
		this.id = id.intern();
	}

	public String getId() {
		return id;
	}

	public abstract SingleSourcePackage[] getSuppliers();

	public boolean compare(PackageSource other) {
		return id.equals(other.getId());
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	public boolean isNullSource() {
		return false;
	}

	public abstract Class<?> loadClass(String name) throws ClassNotFoundException;

	public abstract URL getResource(String name);

	public abstract Enumeration<URL> getResources(String name) throws IOException;

	//TODO See how this relates with FilteredSourcePackage. Overwriting or doing a double dispatch might be good.
	// This is intentionally lenient; we don't force all suppliers to match (only one)
	// it is better to get class cast exceptions in split package cases than miss an event
	public boolean hasCommonSource(PackageSource other) {
		if (other == null)
			return false;
		if (this == other)
			return true;
		SingleSourcePackage[] suppliers1 = getSuppliers();
		SingleSourcePackage[] suppliers2 = other.getSuppliers();
		if (suppliers1 == null || suppliers2 == null)
			return false;
		// This will return true if the specified source has at least one
		// of the suppliers of this source.
		for (SingleSourcePackage supplier1 : suppliers1) {
			for (SingleSourcePackage supplier2 : suppliers2) {
				if (supplier2.equals(supplier1)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(id).append(" -> "); //$NON-NLS-1$
		SingleSourcePackage[] suppliers = getSuppliers();
		if (suppliers == null) {
			return builder.append(String.valueOf(null)).toString();
		}
		builder.append('[');
		for (int i = 0; i < suppliers.length; i++) {
			if (i > 0) {
				builder.append(',');
			}
			builder.append(suppliers[i].getLoader());
		}
		builder.append(']');
		return builder.toString();
	}

	public abstract Collection<String> listResources(String path, String filePattern);

	/**
	 * Used by ServiceReferenceImpl for isAssignableTo
	 * @param registrant Bundle registering service
	 * @param client Bundle desiring to use service
	 * @param className class name to use
	 * @param serviceClass class of original service object
	 * @param container the equinox container
	 * @return true if assignable given package wiring
	 */
	public static boolean isServiceAssignableTo(Bundle registrant, Bundle client, String className,
			Class<?> serviceClass, boolean checkInternal, EquinoxContainer container) {
		// 1) if the registrant == client always return true
		if (registrant == client) {
			return true;
		}
		// 2) get the package name from the specified className
		String pkgName = BundleLoader.getPackageName(className);
		if (pkgName.startsWith("java.")) { //$NON-NLS-1$
			return true;
		}

		BundleLoader producerBL = getBundleLoader(registrant);
		if (producerBL == null) {
			return false;
		}
		BundleLoader consumerBL = getBundleLoader(client);
		if (consumerBL == null) {
			return false;
		}
		// 3) for the specified bundle, find the wiring for the package.  If no wiring is found return true
		PackageSource consumerSource = getSourceFromLoader(consumerBL, pkgName, className, checkInternal);
		if (consumerSource == null) {
			// confirmed no source for consumer
			return true;
		}
		// if boot delegate just return true
		if (container.isBootDelegationPackage(pkgName)) {
			return true;
		}

		// 4) For the registrant bundle, find the wiring for the package.
		PackageSource producerSource = getSourceFromLoader(producerBL, pkgName, className, checkInternal);
		if (producerSource == null) {
			// confirmed no local class either; now check service object
			if (serviceClass != null && ServiceFactory.class.isAssignableFrom(serviceClass)) {
				@SuppressWarnings("deprecation")
				Bundle bundle = container.getPackageAdmin().getBundle(serviceClass);
				if (bundle != null && bundle != registrant) {
					// in this case we have a wacky ServiceFactory that is doing something we cannot
					// verify if it is correct. Instead of failing we allow the assignment and hope
					// for the best
					// bug 326918
					return true;
				}
			}
			// 5) If no wiring is found for the registrant bundle then find the wiring for
			// the classloader of the service object. If no wiring is found return false.
			producerSource = getPackageSource(serviceClass, pkgName, className, checkInternal,
					container.getPackageAdmin());
			if (producerSource == null) {
				return false;
			}
		}
		// 6) If the two wirings found are equal then return true; otherwise return false.
		return producerSource.hasCommonSource(consumerSource);
	}

	private static PackageSource getSourceFromLoader(BundleLoader loader, String pkgName, String className,
			boolean checkInternal) {
		PackageSource source = loader.getPackageSource(pkgName);
		if (source != null || !checkInternal) {
			return source;
		}
		try {
			if (loader.findLocalClass(className) != null) {
				// create a source that represents the private package
				return (new SingleSourcePackage(pkgName, loader));
			}
		} catch (ClassNotFoundException e) {
			// ignore
		}
		return null;
	}

	private static PackageSource getPackageSource(Class<?> serviceClass, String pkgName, String className,
			boolean checkInternal,
			@SuppressWarnings("deprecation") PackageAdmin packageAdmin) {
		if (serviceClass == null) {
			return null;
		}
		@SuppressWarnings("deprecation")
		Bundle serviceBundle = packageAdmin.getBundle(serviceClass);
		if (serviceBundle == null) {
			return null;
		}
		BundleLoader producerBL = getBundleLoader(serviceBundle);
		if (producerBL == null) {
			return null;
		}
		PackageSource producerSource = getSourceFromLoader(producerBL, pkgName, className, checkInternal);
		if (producerSource != null) {
			return producerSource;
		}
		// try the interfaces
		Class<?>[] interfaces = serviceClass.getInterfaces();
		// note that getInterfaces never returns null
		for (Class<?> intf : interfaces) {
			producerSource = getPackageSource(intf, pkgName, className, checkInternal, packageAdmin);
			if (producerSource != null) {
				return producerSource;
			}
		}
		// try super class
		return getPackageSource(serviceClass.getSuperclass(), pkgName, className, checkInternal, packageAdmin);
	}

	private static BundleLoader getBundleLoader(Bundle bundle) {
		ModuleRevision producer = ((EquinoxBundle) bundle).getModule().getCurrentRevision();
		ModuleWiring producerWiring = producer.getWiring();
		return producerWiring == null ? null : (BundleLoader) producerWiring.getModuleLoader();
	}
}
