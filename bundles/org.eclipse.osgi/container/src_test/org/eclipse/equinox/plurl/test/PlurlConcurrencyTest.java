/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.plurl.test;

import static org.junit.Assert.assertNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.equinox.plurl.impl.PlurlImpl;
import org.junit.Test;

public class PlurlConcurrencyTest {
	@Test
	public void testConcurrentMWERun() throws InterruptedException {
		new PlurlImpl().install();

		for (int i = 0; i < 1000; i++) {
			List<Thread> threads = new ArrayList<>();
			List<AtomicReference<Throwable>> errors = new ArrayList<>();

			for (int j = 0; j < 10; j++) {
				AtomicReference<Throwable> error = new AtomicReference<>();
				errors.add(error);

				Thread thread = new Thread(() -> {
					try {
						URL url = this.getClass().getClassLoader()
								.getResource(this.getClass().getName().replace(".", "/") + ".class"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						url.getContent();
					} catch (Throwable t) {
						error.set(t);
					}
				});
				threads.add(thread);
				thread.start();
			}

			for (Thread thread : threads) {
				thread.join();
			}

			for (int j = 0; j < errors.size(); j++) {
				Throwable t = errors.get(j).get();
				assertNull("Thread threw an error: " + j, t); //$NON-NLS-1$
			}
		}
	}
}
