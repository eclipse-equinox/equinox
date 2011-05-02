/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Matthew Webster           Eclipse 3.2 changes     
 *   Martin Lippert            extracted caching service factory
 *   Martin Lippert            caching of generated classes
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching.j9;

import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import com.ibm.oti.shared.HelperAlreadyDefinedException;
import com.ibm.oti.shared.Shared;
import com.ibm.oti.shared.SharedClassURLHelper;

import org.eclipse.equinox.service.weaving.CacheEntry;
import org.eclipse.equinox.service.weaving.ICachingService;
import org.osgi.framework.Bundle;

public class CachingService implements ICachingService {

    SharedClassURLHelper urlhelper;

    private final Bundle bundle;

    private final ClassLoader classLoader;

    private final String partition;

    public CachingService(final ClassLoader loader, final Bundle bundle,
            final String key) {
        if (CachingServicePlugin.DEBUG)
            System.out.println("> CachingService.<init>() bundle="
                    + bundle.getSymbolicName() + ", loader=" + loader
                    + ", key='" + key + "'");
        this.bundle = bundle;
        this.classLoader = loader;
        this.partition = hashNamespace(key + bundle.getBundleId()
                + bundle.getLastModified());
        try {
            urlhelper = Shared.getSharedClassHelperFactory().getURLHelper(
                    classLoader);
        } catch (final HelperAlreadyDefinedException e) {
            e.printStackTrace();
        }
        if (CachingServicePlugin.DEBUG)
            System.out.println("< CachingService.<init>() partition='"
                    + partition + "', urlhelper=" + urlhelper);
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#canCacheGeneratedClasses()
     */
    public boolean canCacheGeneratedClasses() {
        return false;
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#findStoredClass(java.lang.String,
     *      java.net.URL, java.lang.String)
     */
    public CacheEntry findStoredClass(final String namespace,
            final URL sourceFileURL, final String name) {
        final byte[] bytes = urlhelper.findSharedClass(partition,
                sourceFileURL, name);
        if (CachingServicePlugin.DEBUG && bytes != null)
            System.out.println("- CachingService.findStoredClass() bundle="
                    + bundle.getSymbolicName() + ", name=" + name + ", url="
                    + sourceFileURL + ", bytes=" + bytes);

        if (bytes != null) {
            return new CacheEntry(true, bytes);
        } else {
            return new CacheEntry(false, null);
        }
    }

    /**
     * Hash the shared class namespace using MD5
     * 
     * @param keyToHash
     * @return the MD5 version of the input string
     */
    public String hashNamespace(final String namespace) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        final byte[] bytes = md.digest(namespace.getBytes());
        final StringBuffer result = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            final byte b = bytes[i];
            int num;
            if (b < 0) {
                num = b + 256;
            } else {
                num = b;
            }
            String s = Integer.toHexString(num);
            while (s.length() < 2) {
                s = "0" + s;
            }
            result.append(s);
        }
        return new String(result);
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#stop()
     */
    public void stop() {
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#storeClass(java.lang.String,
     *      java.net.URL, java.lang.Class, byte[])
     */
    public boolean storeClass(final String namespace, final URL sourceFileURL,
            final Class<?> clazz, final byte[] classbytes) {
        final boolean success = urlhelper.storeSharedClass(partition,
                sourceFileURL, clazz);
        if (CachingServicePlugin.DEBUG && success)
            System.out.println("- CachingService.storeClass() bundle="
                    + bundle.getSymbolicName() + ", clazz=" + clazz + ", url="
                    + sourceFileURL);
        return success;
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#storeClassAndGeneratedClasses(java.lang.String,
     *      java.net.URL, java.lang.Class, byte[], java.util.Map)
     */
    public boolean storeClassAndGeneratedClasses(final String namespace,
            final URL sourceFileUrl, final Class<?> clazz,
            final byte[] classbytes, final Map<String, byte[]> generatedClasses) {
        return false;
    }

}
