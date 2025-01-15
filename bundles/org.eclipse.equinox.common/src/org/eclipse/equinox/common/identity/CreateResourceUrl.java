/*******************************************************************************
 * Copyright (c) 2024 ArSysOp.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Alexander Fedorov (ArSysOp) - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.common.identity;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.Objects;

/**
 * Provides an URL with "platform" schema to be used for resource access.
 * <p/>
 * 
 * JFace usage:
 * 
 * <pre>
 * ImageIdentity EXAMPLE
 * ImageRegistry registry
 * registry.put(EXAMPLE.id(), ImageDescriptor.createFromURL(new CreateResourceUrl(EXAMPLE).get()));
 * ...
 * Image image = registry.get(EXAMPLE.id());
 *  *
 * </pre>
 * 
 * EMF usage:
 * 
 * <pre>
 * ImageIdentity EXAMPLE
 * 
 * public Object getImage(Object object) {
 * 	return overlayImage(object, new CreateResourceUrl(EXAMPLE).get());
 * }
 * </pre>
 * 
 */
public final class CreateResourceUrl {

	private final String raw;

	public CreateResourceUrl(ResourceUrl identity) {
		this(identity.url());
	}

	public CreateResourceUrl(String raw) {
		this.raw = Objects.requireNonNull(raw);
	}

	/**
	 * Returns an {@link URL} to access given {@link ResourceUrl}
	 * 
	 * @return {@link URL} to access resource
	 * @throws MissingResourceException in case of malformed URL
	 */
	public URL get() throws MissingResourceException {
		try {
			return new URL(raw);
		} catch (MalformedURLException e) {
			throw new MissingResourceException("Invalid resource url specification", getClass().getName(), raw); //$NON-NLS-1$
		}
	}

}
