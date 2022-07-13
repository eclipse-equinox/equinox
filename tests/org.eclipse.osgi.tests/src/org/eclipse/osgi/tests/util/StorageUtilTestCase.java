/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
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
package org.eclipse.osgi.tests.util;

import java.io.File;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.osgi.storage.StorageUtil;
import org.junit.Test;

public class StorageUtilTestCase extends CoreTest {

	@Test
	public void testRegularWindowsFileName() {
		String[] validFilenames = { //
				"something\\somethingelse", // normal file
				"something/somethingelse", // normal file
				"COM1anything", // normal file
				"COM1/anything", // illegal directory name but normal file
				".temp", // It is acceptable to specify a period as the first character of a
							// name. For example, ".temp".
				"COM56", // there is no predefined NT namespace for COM56.
		};
		for (String validFilename : validFilenames) {
			assertFalse(validFilename, StorageUtil.isReservedFileName(new File(validFilename)));
		}
		// test reserved names according to
		// https://docs.microsoft.com/en-us/windows/win32/fileio/naming-a-file
		String[] invalidFilenames = { //
				"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
				"LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9", // reserved names
				"COM1.anything", "NUL.txt", // "Also avoid these names followed immediately by an extension; for
											// example, NUL.txt is not recommended"
				"COM1", "com1", "coM1"// case insensitive
		};
		boolean isWindows = Platform.getOS().equals(Platform.OS_WIN32);
		for (String invalidFilename : invalidFilenames) {
			assertTrue(invalidFilename, isWindows == StorageUtil.isReservedFileName(new File(invalidFilename)));
		}
	}

}
