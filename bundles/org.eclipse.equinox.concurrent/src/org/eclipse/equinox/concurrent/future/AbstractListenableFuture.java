/*******************************************************************************
 * Copyright (c) 2010, 2013 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
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
