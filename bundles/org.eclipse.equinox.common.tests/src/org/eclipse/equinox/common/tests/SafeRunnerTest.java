/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.common.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ISafeRunnableWithResult;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SafeRunner;
import org.junit.Test;

/**
 * Tests for {@link SafeRunner}.
 */
public class SafeRunnerTest {

	/**
	 * Ensures that cancellation exceptions are handled
	 */
	@Test
	public void testOperationCanceledExceptionAreHandled() {
		try {
			SafeRunner.run(() -> {
				throw new OperationCanceledException();
			});
		} catch (OperationCanceledException e) {
			fail("OperationCanceledException unexpectedly caught.");
		}
	}

	@Test
	public void testAssertionErrorIsCaught() {
		assertExceptionHandled(new AssertionError());
	}

	@Test
	public void testLinkageErrorIsCaught() {
		assertExceptionHandled(new LinkageError());
	}

	@Test
	public void testRuntimeExceptionIsCaught() {
		assertExceptionHandled(new RuntimeException());
	}

	@Test
	public void testRethrowsError() {
		assertExceptionRethrown(new Error());
	}

	@Test
	public void testRethrowsOutOfMemoryError() {
		assertExceptionRethrown(new OutOfMemoryError());
	}

	@Test
	public void testNull() {
		assertThrows(RuntimeException.class, () -> SafeRunner.run(null));
	}

	/**
	 * Ensures that exceptions are propagated when the safe runner re-throws it
	 */
	@Test
	public void testRethrow() {
		assertThrows(IllegalArgumentException.class, () -> {
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					if (exception instanceof IllegalArgumentException) {
						throw (IllegalArgumentException) exception;
					}
				}

				@Override
				public void run() throws Exception {
					throw new IllegalArgumentException();
				}
			});
		});
	}

	@Test
	public void testWithResult() {
		assertEquals("TestRun", SafeRunner.run(() -> "TestRun"));
	}

	@Test
	public void testWithResultReturnsNullOnException() {
		ISafeRunnableWithResult<String> code = () -> {
			throw new IllegalArgumentException();
		};
		assertNull(SafeRunner.run(code));
	}

	private void assertExceptionRethrown(Throwable current) {
		Throwable caughtException = assertThrows(Throwable.class, () -> {
			SafeRunner.run(() -> {
				if (current instanceof Exception) {
					throw (Exception) current;
				} else if (current instanceof Error) {
					throw (Error) current;
				}
			});
		});
		assertEquals("Unexpected exception.", current, caughtException);
	}

	private void assertExceptionHandled(Throwable throwable) {
		final Throwable[] handled = new Throwable[1];
		SafeRunner.run(new ISafeRunnable() {
			@Override
			public void handleException(Throwable exception) {
				handled[0] = exception;
			}

			@Override
			public void run() throws Exception {
				if (throwable instanceof Exception) {
					throw (Exception) throwable;
				} else if (throwable instanceof Error) {
					throw (Error) throwable;
				}
			}
		});
		assertEquals("Unexpected exception.", throwable, handled[0]);
	}
}
