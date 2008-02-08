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

package org.eclipse.equinox.transforms;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

public class ProxyStreamTransformer extends StreamTransformer {

	private Method method;
	private Object object;

	public ProxyStreamTransformer(Object object) throws SecurityException, NoSuchMethodException {
		this.object = object;
		method = object.getClass().getMethod("getInputStream", new Class[] {InputStream.class, URL.class});
		Class returnType = method.getReturnType();
		if (!returnType.equals(InputStream.class))
			throw new NoSuchMethodException();
		
	}

	public InputStream getInputStream(InputStream inputStream,
			URL transformerUrl) throws IOException {
		try {
			return (InputStream) method.invoke(object, new Object[] {inputStream, transformerUrl});
		} catch (IllegalArgumentException e) {
			throw new IOException(e.getMessage());
		} catch (IllegalAccessException e) {
			throw new IOException(e.getMessage());
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof IOException)
				throw (IOException)e.getCause();
		}
		return null;
	}

	public Object getTransformer() {
		return object;
	}
}
