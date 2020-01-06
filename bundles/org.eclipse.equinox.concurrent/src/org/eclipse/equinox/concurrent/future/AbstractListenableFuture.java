/*******************************************************************************
 * Copyright (c) 2010, 2013 Composent, Inc. and others.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.concurrent.future;

/**
 * @since 1.1
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractListenableFuture extends AbstractFuture implements
		IListenableFuture {

	public abstract void addListener(IProgressRunnable progressRunnable,
			IExecutor executor);

}
