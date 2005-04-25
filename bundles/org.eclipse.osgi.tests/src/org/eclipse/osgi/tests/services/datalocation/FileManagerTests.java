/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.services.datalocation;

import java.io.*;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.datalocation.FileManager;

public class FileManagerTests extends TestCase {
	FileManager manager1;
	FileManager manager2;
	File base;

	/**
	 * Constructs a test case with the given name.
	 */
	public FileManagerTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(FileManagerTests.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		base = new File(Platform.getConfigurationLocation().getURL().getPath(), "FileManagerTests");
		manager1 = null;
		manager2 = null;
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if (manager1 != null)
			manager1.close();
		if (manager2 != null)
			manager2.close();
	}

	
	
	/**
	 * Open a filemanager in readonly and ensure files are
	 * not created.
	 */
	public void testReadOnly() {
		File testDir = new File(base, "readOnlyManager");
		String fileName = "testReadOnly";
		testDir.mkdirs();
		String[] files = testDir.list();
		assertEquals(files.length, 0);
		manager1 = new FileManager(testDir, null, true);
		try {
			manager1.open(true);
		} catch(IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		files = testDir.list();
		assertEquals(files.length, 0);
		try {
			manager1.add(fileName);
			fail("add succedded");
		} catch(IOException e) {
			//good
		}
		
		try {
			manager1.lookup(fileName, true);
			fail("lookup succedded");
		} catch(IOException e) {
			//good
		}
		
		try {
			manager1.createTempFile(fileName);
			fail("create temp file succedded");
		} catch(IOException e) {
			//good
		}
		files = testDir.list();
		assertEquals(files.length, 0);
		manager1.close();
		manager1 = null;

		try {
			// lets create a file in a writable file manager
			manager2 = new FileManager(testDir, null, false);
			manager2.open(true);
			manager2.lookup(fileName, true);
			File tmpFile = manager2.createTempFile(fileName);
			writeToFile(tmpFile, "This file exists");
			manager2.update(new String[] {fileName}, new String[] {tmpFile.getName()});
			manager2.close();
			manager2 = null;
		} catch(IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}		
	}
	
	
	void writeToFile(File file, String str) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		try {
			fos.write(str.getBytes());
			fos.flush();
			fos.getFD().sync();
		} finally {
			fos.close();
		}
	}

	
	private String getInputStreamContents(InputStream is) throws IOException {
		StringBuffer sb = new StringBuffer();
		byte[] data = new byte[64];
		int len;
		try {
			while ((len = is.read(data)) != -1) {
				sb.append(new String(data, 0, len));
			} 
		} finally {
			is.close();
		}
		return sb.toString();
	}
	
	
	/**
	 * This tests a FM update where the a specific file is managed with a 
	 * revision number of (n) and an update is requested while there already
	 * exists a file with revision number (n+1). FM should skip that generation 
	 * number and update the database with file version set to (n+2). This test is
	 * possible if a power loss occurs between FM rename() and FM save().
	 */
	public void testExistingVersion() {
		String testFile = "testExistingVersion.txt";
		manager1 = new FileManager(base, null);
		try {
			manager1.open(false);
			File file1 = new File(base, testFile+".1");
			File file2 = new File(base, testFile+".2");
			File file3 = new File(base, testFile+".3");
			File file4 = new File(base, testFile+".4");
			if (file1.exists() || file2.exists() || file3.exists() || file4.exists()) {
				fail("Test files already exists.");
				return;
			}
			// create file version (1)
			manager1.add(testFile);
			File file = manager1.createTempFile(testFile);
			writeToFile(file, "contents irrelevant");
			manager1.update(new String[] {testFile}, new String[] {file.getName()});
			if (!file1.exists() || file2.exists() || file3.exists() || file4.exists()) {
				fail("Failed to create a single test file");
				return;
			}
			
			// create file version (2) outside filemanager
			writeToFile(file2, "file2 exists");

			// update another file after generation 2 already exists...
			file = manager1.createTempFile(testFile);
			writeToFile(file, "file 3 contents");
			manager1.update(new String[] {testFile}, new String[] {file.getName()});
			if (!file3.exists() || file4.exists()) {
				fail("Failed to skip existing filemanager file.");
				return;
			}
			
			// open a new manager, ensure a lookup results in file version (3)
			manager2 = new FileManager(base, null);
			manager2.open(true);
			file = manager2.lookup(testFile, false);
			if (file == null) {
				fail("Unable to lookup exising file");
				return;
			}
			assertTrue(file.getName().endsWith(".3"));
			assertTrue(file.exists());
			FileInputStream fis = new FileInputStream(file);
			assertEquals(getInputStreamContents(fis), "file 3 contents");
			
			manager2.close();
			manager2=null;
			
			manager1.close();
			manager1=null;
		} catch(IOException e) {
			fail(e.getMessage());
		}
	}

	
	/**
	 * This tests that FM apis throw exceptions if FM has not yet been opened
	 * or if FM has been closed.  
	 */
	public void testNotOpen() {
		String permanentFile = "testNotOpen.txt";
		String scratchFile = "testNotOpenScratch";
		manager1 = new FileManager(base, null);
		// create a permanent file and a managed scratch file
		try {
			manager1.open(true);
			manager1.add(permanentFile);
			File tmpFile = manager1.createTempFile(permanentFile);
			this.writeToFile(tmpFile, "File exists");
			manager1.update(new String[] {permanentFile}, new String[] {tmpFile.getName()});
			manager1.add(scratchFile);
		} catch(IOException e) {
			fail(e.getMessage());
		}
		
		// create a new manager, and try making calls
		manager2 = new FileManager(base, null);
		checkOpen(false, permanentFile, scratchFile);
		// open the manager, try again
		try {
			manager2.open(true);
		} catch(IOException e) {
			fail(e.getMessage());
		}
		checkOpen(true, permanentFile, scratchFile);
		// close manager, try again
		manager2.close();
		checkOpen(false, permanentFile, scratchFile);
		manager2 = null;
		
		manager1.close();
		manager1 = null;
	}

	void checkOpen(boolean open, String permanentFile, String scratchFile) {
		// check add()
		try {
			manager2.add("failFile");
			if (!open)
				fail("add did not fail.");
			manager2.remove("failFile");
		} catch(IOException e) {
			if (open)
				fail(e.getMessage());
		}
		// check lookup()
		try {
			// expect TEST2 to exist
			manager2.lookup(permanentFile, false);
			if (!open)
				fail("lookup did not fail.");
		} catch(IOException e) {
			if (open)
				fail(e.getMessage());
		}
		// check update, first create a real file to add
		File tmpFile;
		try {
			tmpFile = manager2.createTempFile("openTest");
			writeToFile(tmpFile, "contents irrelevant");
		} catch (IOException e) {
			fail(e.getMessage());
			tmpFile = null;
		}	
		if (tmpFile != null) {
			try {
				manager2.update(new String[] {permanentFile}, new String[] {tmpFile.getName()});
				if (!open)
					fail("update did not fail.");
			} catch(IOException e) {
				if (open)
					fail(e.getMessage());
			}
		}
		// check remove()
		try {
			manager2.remove(scratchFile);
			if (!open)
				fail("remove did not fail");
			else // add the file back if expected to complete
				manager2.add(scratchFile);
			} catch(IOException e) {
			if (open)
				fail(e.getMessage());
		}
	}
	
	
	/**
	 * This tests FM remove() then add().  On a remove(), the file can not be deleted as it may
	 * be in use by another FM. Then add() the file back and see if the version number written to
	 * is (n+1) where (n) is the version when remove() called.  This is likely if framework uses
	 * -clean where on clean we remove the file, then on exit we add the file back.  Make sure 
	 * this is orderly.
	 * 
	 * Currently is is a known issues that remove() will never delete any old manager contents. 
	 */
	public void testRemoveThenAdd() {
		String fileName = "testRemoveThenAdd.txt";
		File file1 = new File(base, fileName+".1");
		File file2 = new File(base, fileName+".2");
		File file3 = new File(base, fileName+".3");
		manager1 = new FileManager(base, null);
		// create a permanent file
		try {
			manager1.open(true);
			manager1.add(fileName);
			// create version (1)
			File tmpFile = manager1.createTempFile(fileName);
			writeToFile(tmpFile, "File exists");
			manager1.update(new String[] {fileName}, new String[] {tmpFile.getName()});
			// do it again, now version (2)
			tmpFile = manager1.createTempFile(fileName);
			writeToFile(tmpFile, "File exists #2");
			manager1.update(new String[] {fileName}, new String[] {tmpFile.getName()});
			manager1.close(); // force a cleanup
			manager1 = null;
			// sanity check
			if (file1.exists() || !file2.exists() || file3.exists())
				fail("Failed creating a file revision");
			
			manager2 = new FileManager(base, null);
			manager2.open(true);
			manager2.remove(fileName);
			// check lookup & getInputStream
			File testFile = manager2.lookup(fileName, false);
			assertNull(testFile);
			manager2.add(fileName);
			testFile = manager2.lookup(fileName, false);
			assertNotNull(testFile);
			assertTrue(testFile.getName().endsWith(".0"));
			// write new file, ensure it version 3
			tmpFile = manager2.createTempFile(fileName);
			writeToFile(tmpFile, "File exists #3");
			manager2.update(new String[] {fileName}, new String[] {tmpFile.getName()});
			testFile = manager2.lookup(fileName, false);
			assertNotNull(testFile);
			assertTrue(testFile.getName().endsWith(".3"));
			assertTrue(file3.exists());
			
			// open a new manager, ensure that the database was updated
			// by checking version is also #3
			manager1 = new FileManager(base, null);
			manager1.open(true);
			testFile = manager1.lookup(fileName, false);
			assertNotNull(testFile);
			assertTrue(testFile.getName().endsWith(".3"));
			
			manager1.close();
			manager1 = null;
			manager2.close();
			manager2 = null;
		} catch(IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	/**
	 * Test multiple FM do not cleanup any old files as they may be in use.
	 */
	public void testMultipleFileManagers() {
		String fileName = "testMultipleFileManagers.txt";
		File file1 = new File(base, fileName+".1");
		File file2 = new File(base, fileName+".2");
		manager1 = new FileManager(base, null);
		try {
			manager1.open(true);
			File file = manager1.lookup(fileName, true);
			assertNotNull(file);
			file = manager1.createTempFile(fileName);
			writeToFile(file, "test contents #1");
			manager1.update(new String[] {fileName}, new String[] {file.getName()});
			
			// ensure file is version #1
			file = manager1.lookup(fileName, false);
			assertNotNull(file);
			assertTrue(file.getName().endsWith(".1"));
			assertTrue(file1.exists());
			
			//new fileMangager using version #1
			manager2 = new FileManager(base, null);
			manager2.open(true);
			// sanity check
			file = manager2.lookup(fileName, false);
			assertNotNull(file);
			assertTrue(file.getName().endsWith(".1"));
			assertTrue(file1.exists() && !file2.exists());
			
			// back to manager #1, update file again, close
			file = manager1.createTempFile(fileName);
			writeToFile(file, "test contents #2");
			manager1.update(new String[] {fileName}, new String[] {file.getName()});
			//sanity check
			assertTrue(file1.exists() && file2.exists());
			manager1.close();
			manager1 = null;
			// both files better still exists
			assertTrue(file1.exists() && file2.exists());
			
			// manager #2
			// sanity check
			file = manager2.lookup(fileName, false);
			assertNotNull(file);
			assertTrue(file.getName().endsWith(".1"));
			// close manager2, cleanup should occur
			manager2.close();
			manager2 = null;
			assertTrue(!file1.exists() && file2.exists());
			
			// new manager1, does it get version 1?
			manager1 = new FileManager(base, null);
			manager1.open(true);
			file = manager1.lookup(fileName, false);
			assertNotNull(file);
			assertTrue(file.getName().endsWith(".2"));
			manager1.close();		
			manager1 = null;
		} catch(IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	/**
	 * This test will verify that a FM will fail if a lock is held
	 */
	public void testJavaIOLocking() {
		if (!"win32".equalsIgnoreCase(System.getProperty("osgi.os")))
			// this is a Windows-only test
			return;
		String fileName = "testJavaIOLocking";
		File lockFile = new File(new File(base,".manager"),".fileTableLock");
		assertTrue(lockFile.exists());
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(lockFile);
			// we hold the lock, lets open a FM
			manager1 = new FileManager(base, "java.io");
			try {
				manager1.open(true); // wait for lock
				fail("open with lock succedded");
			} catch(IOException e) {
				//good
			}
			
			manager1.open(false); // don't wait, should work
			try {
				manager1.add(fileName);
				fail("add succedded");
			} catch(IOException e) {
				//good
			}
			//sanity check, file should not be managed
			assertNull(manager1.lookup(fileName, false));
			manager1.close();
			manager1 = null;
		} catch(IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			try {
				if (fos != null)
					fos.close();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}
