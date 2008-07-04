/*******************************************************************************
 * Copyright (c) 2008 Heiko Seeberger and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Heiko Seeberger - initial implementation
 *******************************************************************************/

package org.eclipse.equinox.weaving.internal.caching;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.equinox.service.weaving.ICachingService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * <p>
 * {@link ICachingService} instantiated by {@link SingletonCachingService} for
 * each bundle.
 * </p>
 * <p>
 * The maximum number of classes cached in memory before writing to disk is
 * definded via the system property
 * <code>org.aspectj.osgi.service.caching.maxCachedClasses</code>. The
 * default is 20.
 * </p>
 * 
 * @author Heiko Seeberger
 */
public class BundleCachingService extends BaseCachingService {

    private static final int BUFFER_SIZE = 8 * 1024;
    private static final Integer MAX_CACHED_CLASSES =
        Integer.getInteger("org.aspectj.osgi.service.caching.maxCachedClasses",
            20);

    private final Bundle bundle;
    private final BundleContext bundleContext;

    private File cache;
    private final String cacheKey;

    private final Map<String, byte[]> cachedClasses =
        new HashMap<String, byte[]>(MAX_CACHED_CLASSES + 10);


    /**
     * @param bundleContext
     *            Must not be null!
     * @param bundle
     *            Must not be null!
     * @param key
     *            Must not be null!
     * @throws IllegalArgumentException
     *             if given bundleContext or bundle is null.
     */
    public BundleCachingService(final BundleContext bundleContext,
        final Bundle bundle, final String key) {

        if (bundleContext == null) {
            throw new IllegalArgumentException(
                "Argument \"bundleContext\" must not be null!");
        }
        if (bundle == null) {
            throw new IllegalArgumentException(
                "Argument \"bundle\" must not be null!");
        }
        if (key == null) {
            throw new IllegalArgumentException(
                "Argument \"key\" must not be null!");
        }

        this.bundleContext = bundleContext;
        this.bundle = bundle;
        this.cacheKey = hashNamespace(key);

        initCache();
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#findStoredClass(java.lang.String,
     *      java.net.URL, java.lang.String)
     */
    @Override
    public byte[] findStoredClass(final String namespace,
        final URL sourceFileURL, final String name) {

        if (name == null) {
            throw new IllegalArgumentException(
                "Argument \"name\" must not be null!");
        }

        byte[] storedClass = null;
        if (cache != null) {
        	File cachedBytecodeFile = new File(cache, name);
        	if (cachedBytecodeFile.exists()) {
                storedClass = read(name, cachedBytecodeFile);
        	}
        }

        if (Log.isDebugEnabled()) {
            Log.debug(MessageFormat.format("for [{0}]: {1} {2}", bundle
                .getSymbolicName(), ((storedClass != null) ? "Found"
                : "Found NOT"), name));
        }
        return storedClass;
    }

    /**
     * Writes the remaining cache to disk.
     */
    public void stop() {
        if (cache != null) {
            for (final String name : cachedClasses.keySet()) {
                write(name);
            }
        }
    }

    /**
     * @see org.eclipse.equinox.service.weaving.ICachingService#storeClass(java.lang.String,
     *      java.net.URL, java.lang.Class, byte[])
     */
    @Override
    public boolean storeClass(final String namespace, final URL sourceFileURL,
        final Class clazz, final byte[] classbytes) {

        if (clazz == null) {
            throw new IllegalArgumentException(
                "Argument \"clazz\" must not be null!");
        }
        if (classbytes == null) {
            throw new IllegalArgumentException(
                "Argument \"classbytes\" must not be null!");
        }

        if (cache == null) {
            return false;
        }

        cachedClasses.put(clazz.getName(), classbytes);
        if (cachedClasses.size() > MAX_CACHED_CLASSES) {

            final Iterator<String> names = cachedClasses.keySet().iterator();
            while (names.hasNext()) {
                write(names.next());
                names.remove();
            }
        }
        return true;
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
            md = MessageDigest.getInstance("MD5");
        }
        catch (final NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        final byte[] bytes = md.digest(namespace.getBytes());
        final StringBuffer result = new StringBuffer();
        for (final byte b : bytes) {
            int num;
            if (b < 0) {
                num = b + 256;
            }
            else {
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

    private void initCache() {
        boolean isInitialized = false;
        final File dataFile = bundleContext.getDataFile("");
        if (dataFile != null) {
            cache =
                new File(new File(dataFile, cacheKey), bundle.getSymbolicName());
            if (!cache.exists()) {
                isInitialized = cache.mkdirs();
            }
            else {
                isInitialized = true;
            }
        }
        if (!isInitialized) {
            Log.error("Cannot initialize cache!", null);
        }
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
					readcount = in.read(classbytes, bytesread, length - bytesread);
					if (readcount <= 0) /* if we didn't read anything */
						break; /* leave the loop */
				}
			} else /* BundleEntry does not know its own length! */{
				length = BUFFER_SIZE;
				classbytes = new byte[length];
				readloop: while (true) {
					for (; bytesread < length; bytesread += readcount) {
						readcount = in.read(classbytes, bytesread, length - bytesread);
						if (readcount <= 0) /* if we didn't read anything */
							break readloop; /* leave the loop */
					}
					byte[] oldbytes = classbytes;
					length += BUFFER_SIZE;
					classbytes = new byte[length];
					System.arraycopy(oldbytes, 0, classbytes, 0, bytesread);
				}
			}
			if (classbytes.length > bytesread) {
				byte[] oldbytes = classbytes;
				classbytes = new byte[bytesread];
				System.arraycopy(oldbytes, 0, classbytes, 0, bytesread);
			}
			return classbytes;
        }
        catch (final IOException e) {
            Log.error(MessageFormat.format(
                "for [{0}]: Cannot read [1] from cache!", bundle
                    .getSymbolicName(), name), e);
            return null;
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (final IOException e) {
                    Log.error(MessageFormat.format(
                        "for [{0}]: Cannot close cache file for [1]!", bundle
                            .getSymbolicName(), name), e);
                }
            }
        }
    }

    private void write(final String name) {
        // TODO Think about synchronization !!!
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(cache, name));
            out.write(cachedClasses.get(name));
            out.close();
            if (Log.isDebugEnabled()) {
                Log.debug(MessageFormat.format(
                    "for [{0}]: Written {1} to cache.", bundle
                        .getSymbolicName(), name));
            }
        }
        catch (final IOException e) {
            Log.error(MessageFormat.format(
                "for [{0}]: Cannot write [1] to cache!", bundle
                    .getSymbolicName(), name), e);
        }
        finally {
            if (out != null) {
                try {
                    out.close();
                }
                catch (final IOException e) {
                    Log.error(MessageFormat.format(
                        "for [{0}]: Cannot close cache file for [1]!", bundle
                            .getSymbolicName(), name), e);
                }
            }
        }
    }
}
