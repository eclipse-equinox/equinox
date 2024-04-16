/*******************************************************************************
 * Copyright (c) 2010, 2021 IBM Corporation and others.
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
package org.eclipse.osgi.internal.weaving;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.eclipse.osgi.container.ModuleRevision;
import org.eclipse.osgi.internal.framework.EquinoxContainer;
import org.eclipse.osgi.internal.hookregistry.ClassLoaderHook;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.loader.classpath.ClasspathEntry;
import org.eclipse.osgi.internal.permadmin.BundlePermissions;
import org.eclipse.osgi.internal.serviceregistry.HookContext;
import org.eclipse.osgi.internal.serviceregistry.ServiceRegistry;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.StorageUtil;
import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.hooks.weaving.WovenClassListener;
import org.osgi.framework.wiring.BundleWiring;

public final class WovenClassImpl implements WovenClass, HookContext<WeavingHook> {
	private final static byte FLAG_HOOKCALLED = 0x01;
	private final static byte FLAG_HOOKSCOMPLETE = 0x02;
	private final static byte FLAG_WEAVINGCOMPLETE = 0x04;
	private final String className;
	private final BundleEntry entry;
	private final List<String> dynamicImports;
	private final ClasspathEntry classpathEntry;
	private final BundleLoader loader;
	final ServiceRegistry registry;
	private final Map<ServiceRegistration<?>, Boolean> deniedHooks;
	private byte[] validBytes;
	private byte[] resultBytes;
	private byte hookFlags = 0;
	private Throwable error;
	private ServiceRegistration<?> errorHook;
	private Class<?> clazz;
	private int state;
	final EquinoxContainer container;

	public WovenClassImpl(String className, byte[] bytes, BundleEntry entry, ClasspathEntry classpathEntry,
			BundleLoader loader, EquinoxContainer container, Map<ServiceRegistration<?>, Boolean> deniedHooks) {
		super();
		this.className = className;
		this.validBytes = this.resultBytes = bytes;
		this.entry = entry;
		this.dynamicImports = new DynamicImportList(this);
		this.classpathEntry = classpathEntry;
		this.loader = loader;
		this.registry = container.getServiceRegistry();
		this.container = container;
		this.deniedHooks = deniedHooks;
		setState(TRANSFORMING);
	}

	@Override
	public byte[] getBytes() {
		if ((hookFlags & FLAG_HOOKSCOMPLETE) == 0) {
			checkPermission();
			return validBytes; // return raw bytes until complete
		}
		// we have called all hooks; someone is calling outside of weave call
		// need to be safe and copy the bytes.
		byte[] current = validBytes;
		byte[] results = new byte[current.length];
		System.arraycopy(current, 0, results, 0, current.length);
		return results;
	}

	@Override
	public void setBytes(byte[] newBytes) {
		checkPermission();
		if (newBytes == null)
			throw new NullPointerException("newBytes cannot be null."); //$NON-NLS-1$
		if ((hookFlags & FLAG_HOOKSCOMPLETE) != 0)
			// someone is calling this outside of weave
			throw new IllegalStateException("Weaving has completed already."); //$NON-NLS-1$
		this.resultBytes = this.validBytes = newBytes;
	}

	void checkPermission() {
		SecurityManager sm = System.getSecurityManager();
		if (sm != null)
			sm.checkPermission(new AdminPermission(loader.getWiring().getBundle(), AdminPermission.WEAVE));
	}

	@Override
	public List<String> getDynamicImports() {
		if ((hookFlags & FLAG_HOOKSCOMPLETE) == 0)
			return dynamicImports;
		// being called outside of weave; return unmodified list
		return Collections.unmodifiableList(dynamicImports);
	}

	@Override
	public boolean isWeavingComplete() {
		return (hookFlags & FLAG_WEAVINGCOMPLETE) != 0;
	}

	private void setHooksComplete() {
		// create a copy of the bytes array that noone has a reference to
		byte[] original = validBytes;
		validBytes = new byte[original.length];
		System.arraycopy(original, 0, validBytes, 0, original.length);
		hookFlags |= FLAG_HOOKSCOMPLETE;
	}

	void setWeavingCompleted(Class<?> clazz) {
		// weaving has completed; save the class and mark complete
		this.clazz = clazz;
		hookFlags |= FLAG_WEAVINGCOMPLETE;
		// Only notify listeners if weaving hooks were called.
		if ((hookFlags & FLAG_HOOKCALLED) == 0)
			return;
		// Only notify listeners if they haven't already been notified of
		// the terminal TRANSFORMING_FAILED state.
		if (error != null)
			return;
		// If clazz is null, a class definition failure occurred.
		setState(clazz == null ? DEFINE_FAILED : DEFINED);
		notifyWovenClassListeners();
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public ProtectionDomain getProtectionDomain() {
		return classpathEntry.getDomain();
	}

	@Override
	public Class<?> getDefinedClass() {
		return clazz;
	}

	@Override
	public BundleWiring getBundleWiring() {
		return loader.getWiring();
	}

	@Override
	public void call(final WeavingHook hook, ServiceRegistration<WeavingHook> hookRegistration) throws Exception {
		if (error != null)
			return; // do not call any other hooks once an error has occurred.

		if (skipRegistration(hookRegistration)) {
			// Note we double check denied hooks here just
			// in case another thread denied the hook since the first check
			return;
		}
		if ((hookFlags & FLAG_HOOKCALLED) == 0) {
			hookFlags |= FLAG_HOOKCALLED;
			// only do this check on the first weaving hook call
			if (!validBytes(validBytes)) {
				validBytes = StorageUtil.getBytes(entry.getInputStream(), (int) entry.getSize(), 8 * 1024);
			}
		}
		try {
			hook.weave(this);
		} catch (WeavingException e) {
			error = e;
			errorHook = hookRegistration;
			// do not deny the hook on weaving exceptions
		} catch (Throwable t) {
			error = t; // save the error to fail later
			errorHook = hookRegistration;
			// deny the registration
			deniedHooks.put(hookRegistration, Boolean.TRUE);
		}
	}

	@Override
	public boolean skipRegistration(ServiceRegistration<?> hookRegistration) {
		return deniedHooks.containsKey(hookRegistration);
	}

	private boolean validBytes(byte[] checkBytes) {
		if (checkBytes == null || checkBytes.length < 4)
			return false;
		if ((checkBytes[0] & 0xCA) != 0xCA)
			return false;
		if ((checkBytes[1] & 0xFE) != 0xFE)
			return false;
		if ((checkBytes[2] & 0xBA) != 0xBA)
			return false;
		if ((checkBytes[3] & 0xBE) != 0xBE)
			return false;
		return true;
	}

	private void notifyWovenClassListeners() {
		final HookContext<WovenClassListener> context = (hook, hookRegistration) -> {
			try {
				hook.modified(WovenClassImpl.this);
			} catch (Exception e) {
				WovenClassImpl.this.container.getEventPublisher().publishFrameworkEvent(FrameworkEvent.ERROR,
						hookRegistration.getReference().getBundle(), e);
			}
		};
		if (System.getSecurityManager() == null)
			registry.notifyHooksPrivileged(WovenClassListener.class, "modified", context); //$NON-NLS-1$
		else {
			try {
				AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
					registry.notifyHooksPrivileged(WovenClassListener.class, "modified", context); //$NON-NLS-1$
					return null;
				});
			} catch (PrivilegedActionException e) {
				throw (RuntimeException) e.getException();
			}
		}
	}

	byte[] callHooks() throws Throwable {
		SecurityManager sm = System.getSecurityManager();
		byte[] wovenBytes = null;
		List<String> newImports = null;
		boolean rejected = false;
		try {
			if (sm == null) {
				registry.notifyHooksPrivileged(WeavingHook.class, "weave", this); //$NON-NLS-1$
			} else {
				try {
					AccessController.doPrivileged((PrivilegedExceptionAction<Void>) () -> {
						registry.notifyHooksPrivileged(WeavingHook.class, "weave", WovenClassImpl.this); //$NON-NLS-1$
						return null;
					});
				} catch (PrivilegedActionException e) {
					throw (RuntimeException) e.getException();
				}
			}
		} finally {
			if ((hookFlags & FLAG_HOOKCALLED) != 0) {
				for (ClassLoaderHook classLoaderHook : container.getConfiguration().getHookRegistry()
						.getClassLoaderHooks()) {
					rejected |= classLoaderHook.rejectTransformation(className, resultBytes, classpathEntry, entry,
							loader.getModuleClassLoader().getClasspathManager());
				}
				if (!rejected) {
					wovenBytes = resultBytes;
					newImports = dynamicImports;
				}
				setHooksComplete();
				// Make sure setHooksComplete() has been called. The woven class
				// must be immutable in TRANSFORMED or TRANSFORMING_FAILED.
				// If error is not null, a weaving hook threw an exception.
				setState(error == null ? TRANSFORMED : TRANSFORMING_FAILED);
				// only notify listeners if the transformation was not rejected
				if (!rejected) {
					notifyWovenClassListeners();
				}
			}
		}

		if (error != null)
			throw error;

		if (newImports != null) {
			// add any new dynamic imports
			for (String newImport : newImports) {
				try {
					ManifestElement[] importElements = ManifestElement.parseHeader(Constants.IMPORT_PACKAGE, newImport);
					// Grant implied import package permissions for all dynamic
					// import packages to the woven bundle.
					addImpliedImportPackagePermissions(importElements);
					loader.addDynamicImportPackage(importElements);
				} catch (BundleException e) {
					// should not have happened; checked at add.
				}
			}
		}

		return wovenBytes;
	}

	private void addImpliedImportPackagePermissions(ManifestElement[] importElements) {
		ProtectionDomain wovenDomain = ((Generation) ((ModuleRevision) getBundleWiring().getRevision())
				.getRevisionInfo()).getDomain();
		if (wovenDomain != null) {
			// security is enabled; add the permissions
			for (ManifestElement clause : importElements)
				for (String pkg : clause.getValueComponents())
					((BundlePermissions) wovenDomain.getPermissions())
							.addWovenPermission(new PackagePermission(pkg, PackagePermission.IMPORT));
		}
	}

	@Override
	public String toString() {
		return className;
	}

	public ServiceRegistration<?> getErrorHook() {
		return errorHook;
	}

	@Override
	public int getState() {
		return state;
	}

	private void setState(int value) {
		state = value;
	}
}
