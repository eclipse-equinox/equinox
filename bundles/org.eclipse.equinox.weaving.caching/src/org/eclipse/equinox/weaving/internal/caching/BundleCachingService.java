/*******************************************************************************
 * Copyright (c) 2008, 2009 Heiko Seeberger and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Heiko Seeberger - initial implementation
 *     Martin Lippert - asynchronous cache writing
 *     Martin Lippert - caching of generated classes
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.eclipse.equinox.service.weaving.CacheEntry;
import org.eclipse.equinox.service.weaving.ICachingService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * <p>
 * {@link ICachingService} instantiated by {@link CachingServiceFactory} for
 * each bundle.
 * </p>
 * <p>
 * 
 * @author Heiko Seeberger
 * @author Martin Lippert
 */
public class BundleCachingService implements ICachingService {

    private static final int READ_BUFFER_SIZE = 8 * 1024;

    private final Bundle bundle;

    private File cacheDirectory;

    private final String cacheKey;

    private final BlockingQueue<CacheItem> cacheWriterQueue;

    /**
     * @param bundleContext Must not be null!
     * @param bundle Must not be null!
     * @param key Must not be null!
     * @param cacheWriterQueue The queue for items to be written to the cache,
     *            must not be null
     * @throws IllegalArgumentException if given bundleContext or bundle is
     *             null.
     */
    public BundleCachingService(final BundleContext bundleContext,
            final Bundle bundle, final String key,
            final BlockingQueue<CacheItem> cacheWriterQueue) {

        if (bundleContext == null) {
            throw new IllegalArgumentException(
                    "Argument \"bundleContext\" must not be null!"); //$NON-NLS-1$
        }
        if (bundle == null) {
            throw new IllegalArgumentException(
                    "Argument \"bundle\" must not be null!"); //$NON-NLS-1$
        }
        if (key == null) {
            throw new IllegalArgumentException(
                    "Argument \"key\" must not be null!"); //$NON-NLS-1$
        }

        this.bundle = bundle;
        this.cacheKey = hashNamespace(key);
        this.cacheWriterQueue = cacheWriterQueue;

        final File dataFile = bundleContext.getDataFile(cacheKey);
        if (dataFile != null) {
            final String bundleCacheDir = bundle.getBundleId()
                    + "-" + bundle.getLastModified(); //$NON-NLS-1$
            cacheDirectory = new File(dataFile, bundleCacheDir);
        } else {
            Log.error("Cannot initialize cache!", null); //$NON-NLS-1$
        }
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#canCacheGeneratedClasses()
     */
    public boolean canCacheGeneratedClasses() {
        return true;
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#findStoredClass(java.lang.String,
     *      java.net.URL, java.lang.String)
     */
    public CacheEntry findStoredClass(final String namespace,
            final URL sourceFileURL, final String name) {

        if (name == null) {
            throw new IllegalArgumentException(
                    "Argument \"name\" must not be null!"); //$NON-NLS-1$
        }

        byte[] storedClass = null;
        boolean isCached = false;

        if (cacheDirectory != null) {
            final File cachedBytecodeFile = new File(cacheDirectory, name);
            storedClass = read(name, cachedBytecodeFile);
            isCached = storedClass != null;
        }

        if (Log.isDebugEnabled()) {
            Log.debug(MessageFormat.format("for [{0}]: {1} {2}", bundle //$NON-NLS-1$
                    .getSymbolicName(), ((storedClass != null) ? "Found" //$NON-NLS-1$
                    : "Found NOT"), name)); //$NON-NLS-1$
        }
        return new CacheEntry(isCached, storedClass);
    }

    /**
     * Writes the remaining cache to disk.
     */
    public void stop() {
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#storeClass(java.lang.String,
     *      java.net.URL, java.lang.Class, byte[])
     */
    public boolean storeClass(final String namespace, final URL sourceFileURL,
            final Class<?> clazz, final byte[] classbytes) {
        if (clazz == null) {
            throw new IllegalArgumentException(
                    "Argument \"clazz\" must not be null!"); //$NON-NLS-1$
        }
        if (classbytes == null) {
            throw new IllegalArgumentException(
                    "Argument \"classbytes\" must not be null!"); //$NON-NLS-1$
        }
        if (cacheDirectory == null) {
            return false;
        }

        final CacheItem item = new CacheItem(classbytes, cacheDirectory
                .getAbsolutePath(), clazz.getName());

        return this.cacheWriterQueue.offer(item);
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#storeClassAndGeneratedClasses(java.lang.String,
     *      java.net.URL, java.lang.Class, byte[], java.util.Map)
     */
    public boolean storeClassAndGeneratedClasses(final String namespace,
            final URL sourceFileUrl, final Class<?> clazz,
            final byte[] classbytes, final Map<String, byte[]> generatedClasses) {

        final CacheItem item = new CacheItem(classbytes, cacheDirectory
                .getAbsolutePath(), clazz.getName(), generatedClasses);

        return this.cacheWriterQueue.offer(item);
    }

    /**
     * Hash the shared class namespace using MD5
     * 
     * @param keyToHash
     * @return the MD5 version of the input string
     */
    private String hashNamespace(final String namespace) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
        } catch (final NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        final byte[] bytes = md.digest(namespace.getBytes());
        final StringBuffer result = new StringBuffer();
        for (final byte b : bytes) {
            int num;
            if (b < 0) {
                num = b + 256;
            } else {
                num = b;
            }
            String s = Integer.toHexString(num);
            while (s.length() < 2) {
                s = "0" + s; //$NON-NLS-1$
            }
            result.append(s);
        }
        return new String(result);
    }

    private byte[] read(final String name, final File file) {
        int length = (int) file.length();

        InputStream in = null;
        try {
            byte[] classbytes = new byte[length];
            int bytesread = 0;
            int readcount;

            in = new FileInputStream(file);

            if (length > 0) {
                classbytes = new byte[length];
                for (; bytesread < length; bytesread += readcount) {
                    readcount = in.read(classbytes, bytesread, length
                            - bytesread);
                    if (readcount <= 0) /* if we didn't read anything */
                    break; /* leave the loop */
                }
            } else /* BundleEntry does not know its own length! */{
                length = READ_BUFFER_SIZE;
                classbytes = new byte[length];
                readloop: while (true) {
                    for (; bytesread < length; bytesread += readcount) {
                        readcount = in.read(classbytes, bytesread, length
                                - bytesread);
                        if (readcount <= 0) /* if we didn't read anything */
                        break readloop; /* leave the loop */
                    }
                    final byte[] oldbytes = classbytes;
                    length += READ_BUFFER_SIZE;
                    classbytes = new byte[length];
                    System.arraycopy(oldbytes, 0, classbytes, 0, bytesread);
                }
            }
            if (classbytes.length > bytesread) {
                final byte[] oldbytes = classbytes;
                classbytes = new byte[bytesread];
                System.arraycopy(oldbytes, 0, classbytes, 0, bytesread);
            }
            return classbytes;
        } catch (final IOException e) {
            Log.debug(MessageFormat.format(
                    "for [{0}]: Cannot read [1] from cache!", bundle //$NON-NLS-1$
                            .getSymbolicName(), name));
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException e) {
                    Log.error(MessageFormat.format(
                            "for [{0}]: Cannot close cache file for [1]!", //$NON-NLS-1$
                            bundle.getSymbolicName(), name), e);
                }
            }
        }
    }

}
