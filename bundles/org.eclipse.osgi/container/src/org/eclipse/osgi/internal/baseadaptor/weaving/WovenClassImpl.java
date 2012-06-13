/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.baseadaptor.weaving;

import java.security.*;
import java.util.*;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.internal.baseadaptor.AdaptorUtil;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.internal.serviceregistry.HookContext;
import org.eclipse.osgi.internal.serviceregistry.ServiceRegistry;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;
import org.osgi.framework.hooks.weaving.*;
import org.osgi.framework.wiring.BundleWiring;

public final class WovenClassImpl implements WovenClass, HookContext {
	private final static byte FLAG_HOOKCALLED = 0x01;
	private final static byte FLAG_HOOKSCOMPLETE = 0x02;
	private final static byte FLAG_WEAVINGCOMPLETE = 0x04;
	private final static String weavingHookName = WeavingHook.class.getName();
	private final String className;
	private final BundleEntry entry;
	private final List<String> dynamicImports;
	private final ProtectionDomain domain;
	private final BundleLoader loader;
	final ServiceRegistry registry;
	private final Map<ServiceRegistration<?>, Boolean> blackList;
	private byte[] validBytes;
	private byte[] resultBytes;
	private byte hookFlags = 0;
	private Throwable error;
	private ServiceRegistration<?> errorHook;
	private Class<?> clazz;

	public WovenClassImpl(String className, byte[] bytes, BundleEntry entry, ProtectionDomain domain, BundleLoader loader, ServiceRegistry registry, Map<ServiceRegistration<?>, Boolean> blacklist) {
		super();
		this.className = className;
		this.validBytes = this.resultBytes = bytes;
		this.entry = entry;
		this.dynamicImports = new DynamicImportList(this);
		this.domain = domain;
		this.loader = loader;
		this.registry = registry;
		this.blackList = blacklist;
	}

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
			sm.checkPermission(new AdminPermission(loader.getBundle(), AdminPermission.WEAVE));
	}

	public List<String> getDynamicImports() {
		if ((hookFlags & FLAG_HOOKSCOMPLETE) == 0)
			return dynamicImports;
		// being called outside of weave; return unmodified list
		return Collections.unmodifiableList(dynamicImports);
	}

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
	}

	public String getClassName() {
		return className;
	}

	public ProtectionDomain getProtectionDomain() {
		return domain;
	}

	public Class<?> getDefinedClass() {
		return clazz;
	}

	public BundleWiring getBundleWiring() {
		return loader.getLoaderProxy().getBundleDescription().getWiring();
	}

	public void call(final Object hook, ServiceRegistration<?> hookRegistration) throws Exception {
		if (error != null)
			return; // do not call any other hooks once an error has occurred.
		if (hook instanceof WeavingHook) {
			if (blackList.containsKey(hookRegistration))
				return; // black listed hook
			if ((hookFlags & FLAG_HOOKCALLED) == 0) {
				hookFlags |= FLAG_HOOKCALLED;
				// only do this check on the first weaving hook call
				if (!validBytes(validBytes)) {
					validBytes = AdaptorUtil.getBytes(entry.getInputStream(), (int) entry.getSize(), 8 * 1024);
				}
			}
			try {
				((WeavingHook) hook).weave(this);
			} catch (WeavingException e) {
				error = e;
				errorHook = hookRegistration;
				// do not blacklist on weaving exceptions
			} catch (Throwable t) {
				error = t; // save the error to fail later
				errorHook = hookRegistration;
				// put the registration on the black list
				blackList.put(hookRegistration, Boolean.TRUE);
			}
		}
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

	public String getHookMethodName() {
		return "weave"; //$NON-NLS-1$
	}

	public String getHookClassName() {
		return weavingHookName;
	}

	byte[] callHooks() throws Throwable {
		SecurityManager sm = System.getSecurityManager();
		byte[] wovenBytes = null;
		List<String> newImports = null;
		try {
			if (sm == null) {
				registry.notifyHooksPrivileged(this);
			} else {
				try {
					AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
						public Object run() {
							registry.notifyHooksPrivileged(WovenClassImpl.this);
							return null;
						}
					});
				} catch (PrivilegedActionException e) {
					throw (RuntimeException) e.getException();
				}
			}
		} finally {
			if ((hookFlags & FLAG_HOOKCALLED) != 0) {
				wovenBytes = resultBytes;
				newImports = dynamicImports;
				setHooksComplete();
			}
		}

		if (error != null)
			throw error;

		if (newImports != null) {
			// add any new dynamic imports
			for (String newImport : newImports) {
				try {
					ManifestElement[] importElements = ManifestElement.parseHeader(Constants.IMPORT_PACKAGE, newImport);
					loader.addDynamicImportPackage(importElements);
				} catch (BundleException e) {
					// should not have happened; checked at add.
				}
			}
		}

		return wovenBytes;
	}

	public String toString() {
		return className;
	}

	public ServiceRegistration<?> getErrorHook() {
		return errorHook;
	}
}
