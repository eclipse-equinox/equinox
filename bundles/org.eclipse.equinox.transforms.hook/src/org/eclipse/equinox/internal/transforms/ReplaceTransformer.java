/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.transforms;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Basic default transformer that simply replaces a matching resource with
 * another.
 */
class ReplaceTransformer {

	static final String TYPE = "replace"; //$NON-NLS-1$
	private TransformerHook hook;

	ReplaceTransformer(TransformerHook hook) {
		this.hook = hook;
	}

	public InputStream getInputStream(InputStream inputStream, final URL transformerUrl) {
		try {
			return transformerUrl.openStream();
		} catch (IOException e) {
			hook.log(FrameworkLogEntry.WARNING, String.format("Can't replace resource with %s", transformerUrl), e); //$NON-NLS-1$
			return null;
		}
	}

	public static void register(BundleContext context, TransformerHook transformerHook) {
		context.registerService(Object.class, new ReplaceTransformer(transformerHook),
				FrameworkUtil.asDictionary(Map.of(TransformTuple.TRANSFORMER_TYPE, TYPE)));
	}
}
