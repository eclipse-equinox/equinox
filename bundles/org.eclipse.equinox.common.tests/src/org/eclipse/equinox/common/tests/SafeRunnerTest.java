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

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ISafeRunnableWithResult;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.tests.harness.CoreTest;

/**
 * Tests for {@link SafeRunner}.
 */
public class SafeRunnerTest extends CoreTest {

	/**
	 * Ensures that cancellation exceptions are handled
	 */
	public void testOperationCanceledExceptionAreHandled() {
		try {
			SafeRunner.run(() -> {
				throw new OperationCanceledException();
			});
		} catch (OperationCanceledException e) {
			fail("OperationCanceledException unexpectedly caught.", e);
		}
	}

	public void testAssertionErrorIsCaught() {
		assertExceptionHandled(new AssertionError());
	}

	public void testLinkageErrorIsCaught() {
		assertExceptionHandled(new LinkageError());
	}

	public void testRuntimeExceptionIsCaught() {
		assertExceptionHandled(new RuntimeException());
	}

	public void testRethrowsError() {
		assertExceptionRethrown(new Error());
	}

	public void testRethrowsOutOfMemoryError() {
		assertExceptionRethrown(new OutOfMemoryError());
	}

	public void testNull() {
		try {
			SafeRunner.run(null);
			fail("1.0");
		} catch (RuntimeException e) {
			// expected
		}
	}

	/**
	 * Ensures that exceptions are propagated when the safe runner re-throws it
	 */
	public void testRethrow() {
		IllegalArgumentException caughtException = null;
		try {
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
		} catch (IllegalArgumentException e) {
			caughtException = e;
		}
		assertNotNull("Cathed exception expected.", caughtException);

	}

	public void testWithResult() {
		assertEquals("TestRun", SafeRunner.run(() -> "TestRun"));
	}

	public void testWithResultReturnsNullOnException() {
		ISafeRunnableWithResult<String> code = () -> {
			throw new IllegalArgumentException();
		};
		assertNull(SafeRunner.run(code));
	}

	private void assertExceptionRethrown(Throwable current) {
		Throwable caughtException = null;
		try {
			SafeRunner.run(new ISafeRunnable() {

				@Override
				public void run() throws Exception {
					if (current instanceof Exception) {
						throw (Exception) current;
					} else if (current instanceof Error) {
						throw (Error) current;
					}
				}
			});
		} catch (Throwable t) {
			caughtException = t;
		}
		assertEquals("Unexpected exception.", current, caughtException);
	}

	private void assertExceptionHandled(Throwable throwable) {
		final Throwable[] handled = new Throwable[1];
		try {
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
		} catch (Throwable t) {
			fail("Exception unexpectedly caught.", t);
		}
		assertEquals("Unexpected exception.", throwable, handled[0]);
	}
}