/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.internal.transforms;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * A proxy stream transformer is a transformer instance that relies on reflection to obtain the "getInputStream" method from an underlying object.  
 * This class is useful due to the restrictions in the builder that prevent transformer providers from directly implementing {@link StreamTransformer} due to visibility and builder issues related to referring to classes within fragments.
 */
public class ProxyStreamTransformer extends StreamTransformer {

	private Method method;
	private Object object;

	/**
	 * Create a new proxy transformer based on the given object.
	 * @param object the object to proxy
	 * @throws SecurityException thrown if there is an issue utilizing the reflection methods
	 * @throws NoSuchMethodException thrown if the provided object does not have a "getInputStream" method that takes an {@link InputStream} and an {@link URL}
	 */
	public ProxyStreamTransformer(Object object) throws SecurityException, NoSuchMethodException {
		this.object = object;
		method = object.getClass().getMethod("getInputStream", new Class[] {InputStream.class, URL.class}); //$NON-NLS-1$
		Class returnType = method.getReturnType();
		if (!returnType.equals(InputStream.class))
			throw new NoSuchMethodException();

	}

	public InputStream getInputStream(InputStream inputStream, URL transformerUrl) throws IOException {
		try {
			return (InputStream) method.invoke(object, new Object[] {inputStream, transformerUrl});
		} catch (IllegalArgumentException e) {
			throw new IOException(e.getMessage());
		} catch (IllegalAccessException e) {
			throw new IOException(e.getMessage());
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException) e.getCause();
		}
		return null;
	}

	/**
	 * Get the object that is being proxied.
	 * @return the object.  Never <code>null</code>.
	 */
	public Object getTransformer() {
		return object;
	}
}
