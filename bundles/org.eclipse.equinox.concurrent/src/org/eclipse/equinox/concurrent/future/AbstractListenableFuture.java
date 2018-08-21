/*******************************************************************************
.
. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License 2.0 which accompanies this distribution, and is
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0
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
