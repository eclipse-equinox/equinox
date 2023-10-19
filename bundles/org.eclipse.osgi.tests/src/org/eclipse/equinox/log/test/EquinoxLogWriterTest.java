/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Copyright (c) 2023 Robert Bosch GmbH - https://github.com/eclipse-equinox/equinox/issues/221
 *******************************************************************************/
package org.eclipse.equinox.log.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogListener;

/**
 * Tests verifying that EquinoxLogWriter does not close the writer for a file
 * log on every log invocation but only when the log file changes.
 * 
 * See https://github.com/eclipse-equinox/equinox/issues/221
 */
public class EquinoxLogWriterTest {

	/**
	 * Temporary folder rule used to create a log file for the EquinoxLogWriter
	 */
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	/**
	 * Helper class to capture the state of the internal writer used by the
	 * EquinoxLogWriter
	 */
	private static final class WriterState {

		private boolean isClosed;
		private boolean isFlushed;

		public WriterState() {
			reset();
		}

		public void reset() {
			isClosed = false;
			isFlushed = false;
		}

		/**
		 * @return true, if the writer is closed
		 */
		public boolean isClosed() {
			return isClosed;
		}

		public void setClosed(boolean closed) {
			this.isClosed = closed;
		}

		/**
		 * @return true, if {@link Writer#flush()} got called
		 */
		public boolean isFlushed() {
			return isFlushed;
		}

		public void setFlushed(boolean flushed) {
			this.isFlushed = flushed;
		}

	}

	/**
	 * Given: A {@link FrameworkLog} configured to log to a file</br>
	 * When: {@link FrameworkLog#log(FrameworkLogEntry)} is invoked multiple
	 * times</br>
	 * Then: Every log invocation is synchronously written and flushed but the
	 * internally used {@link Writer} is never closed
	 * 
	 * @throws IOException
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	@Test
	public void testLogDoesNotCloseWriter() throws IOException, NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException {
		// retrieve the framework log
		ServiceReference logReference = OSGiTestsActivator.getContext().getServiceReference(FrameworkLog.class);
		FrameworkLog log = (FrameworkLog) OSGiTestsActivator.getContext().getService(logReference);

		// create and set the log file
		File tempFile = tempFolder.newFile(this.getClass().getSimpleName() + ".log");
		final File originalFile = log.getFile();
		log.setFile(tempFile, false);

		// create the dummy writer which makes use of a writer state
		// to capture invocations (mockito is not available in this test bundle)
		final WriterState state = new WriterState();
		Writer dummyWriter = new Writer() {

			@Override
			public void write(char[] cbuf, int off, int len) {
				// not needed
			}

			@Override
			public void flush() {
				state.setFlushed(true);
			}

			@Override
			public void close() {
				state.setClosed(true);
			}

		};

		// usually, either setFile or setWriter is called on the framework log
		// for this test, we need to verify invocations on the writer instance used
		// for file logging. therefore, we need to set the writer instance using
		// reflection, as usually the writer is created automatically by the
		// EquinoxLogWriter when the target is a file

		// retrieve the field which may be the EquinoxLogWriter
		Optional<Field> logWriter = Stream.of(log.getClass().getDeclaredFields())
				.filter(a -> LogListener.class.isAssignableFrom(a.getType())).findFirst();
		assertThat(logWriter.isPresent(), is(true));

		// retrieve the log writer instance
		Field equinoxLogWriterField = logWriter.get();
		equinoxLogWriterField.setAccessible(true);
		Object equinoxLogWriter = equinoxLogWriterField.get(log);

		// retrieve the internal writer member of the EquinoxLogWriter
		Field internalWriter = equinoxLogWriter.getClass().getDeclaredField("writer");
		internalWriter.setAccessible(true);
		Object originalWriter = internalWriter.get(equinoxLogWriter);

		try {
			// set the dummy writer instance
			internalWriter.set(equinoxLogWriter, dummyWriter);

			// log a few messages and verify the writer is never closed
			for (int i = 1; i <= 3; i++) {
				log.log(new FrameworkLogEntry("some.bundle", 1, 0, "Log Attempt: " + i, 0, null, null));
				assertThat(state.isFlushed(), is(true));
				assertThat(state.isClosed(), is(false));
				state.reset();
			}

			// changing the log file closes the current writer
			log.setFile(null, false);
			assertThat(state.isClosed(), is(true));
		} finally {
			// set the original writer instance again
			internalWriter.set(equinoxLogWriter, originalWriter);
			// set the original log file again
			log.setFile(originalFile, true);
			// unget service reference
			OSGiTestsActivator.getContext().ungetService(logReference);
		}
	}
}
