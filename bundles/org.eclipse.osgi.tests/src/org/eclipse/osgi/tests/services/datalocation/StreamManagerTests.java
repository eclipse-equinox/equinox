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
import junit.framework.TestSuite;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.storagemanager.ManagedOutputStream;
import org.eclipse.osgi.storagemanager.StorageManager;
import org.eclipse.osgi.tests.OSGiTest;

public class StreamManagerTests extends OSGiTest {
	StorageManager manager1;
	StorageManager manager2;
	File base;
	String reliableFile;

	/**
	 * Constructs a test case with the given name.
	 */
	public StreamManagerTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(StreamManagerTests.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		base = new File(Platform.getConfigurationLocation().getURL().getPath(), "StreamManagerTests");
		manager1 = null;
		manager2 = null;
		reliableFile = System.getProperty("osgi.useReliableFiles");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if (manager1 != null)
			manager1.close();
		if (manager2 != null)
			manager2.close();
		rm(base);
		if (reliableFile == null)
			System.getProperties().remove("osgi.useReliableFiles");
		else
			System.setProperty("osgi.useReliableFiles", reliableFile);
	}
	
	private void rm(File file) {
		if (file.isDirectory()) {
			File[] list = file.listFiles();
			if (list != null) {
				for (int idx=0; idx<list.length; idx++) {
					rm(list[idx]);
				}
			}
		}
		file.delete();
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

	
	/**
	 * This tests that FM will keep a backup version of a reliableFile and that
	 * corrupting the reliableFile will recover the previous contents.
	 * 
	 */
	public void testReliableFile() {
		String fileName = "testReliableFile.txt";
		File file1 = new File(base, fileName+".1");
		File file2 = new File(base, fileName+".2");
		File file3 = new File(base, fileName+".3");
		String contents1 = "test reliable file cOntents #1";
		String contents2 = "test reliable file cOntents #2";
		try {
			System.setProperty("osgi.useReliableFiles", "true"); // force reliable files
			manager1 = new StorageManager(base, null);
			manager1.open(true);
			ManagedOutputStream fmos = manager1.getOutputStream(fileName);
			assertNotNull(fmos);
			fmos.write(contents1.getBytes());
			fmos.close();
			
			// Write verion2 of the file
			fmos = manager1.getOutputStream(fileName);
			assertNotNull(fmos);
			fmos.write(contents2.getBytes());
			fmos.close();
			assertTrue(file1.exists());
			assertTrue(file2.exists());
			assertTrue(!file3.exists());
			manager1.close();
			manager1 = null;
			
			//now, open new manager, verify file contents are #2
			System.setProperty("osgi.useReliableFiles", "true"); // force reliable files
			manager2 = new StorageManager(base, null);
			manager2.open(true);
			InputStream is = manager2.getInputStream(fileName);
			assertNotNull(is);
			assertEquals(contents2, getInputStreamContents(is));
			manager2.close();
			manager2 = null;
			
			// need to sleep, FAT32 doesn't have too fine granularity in timestamps
			try {
				Thread.sleep(5000);
			} catch(InterruptedException e) {/*ignore*/}
			//now, corrupt version 2 of the file
			RandomAccessFile raf = new RandomAccessFile(file2, "rw");
			raf.seek(20);
			raf.write('0'); // change 'O' to '0'
			raf.close();

			System.setProperty("osgi.useReliableFiles", "true"); // force reliable files
			manager1 = new StorageManager(base, null);
			manager1.open(true);
			
			//request any valid stream available
			is = manager1.getInputStream(fileName);
			assertNotNull(is);
			assertEquals(contents1, getInputStreamContents(is));
			
			//now request only the primary file
			try {
				InputStream[] isSet = manager1.getInputStreamSet(new String[] {fileName});
				for (int i = 0; i < isSet.length; i++) {
					if (isSet[i] != null)
						isSet[i].close();
				}
				fail("getInputStreamSet was successful");
			} catch(IOException e) {
				//good
			}
			
			//now, corrupt version 1 of the file
			raf = new RandomAccessFile(file1, "rw");
			raf.seek(20);
			raf.write('0'); // change 'O' to '0'
			raf.close();
			
			// get input stream should fail
			try {
				is = manager1.getInputStream(fileName);
				fail("get input stream succedded");
			} catch(IOException e) {
				//good
			}
			manager1.close();
			manager1 = null;
		} catch(IOException e) {
			fail("unexepected exception", e);
		}
	}
	
	
	/**
	 * This tests if migration from a prior (non-ReliableFile) .fileTable
	 * to the current .fileTable is correct.
	 *
	 */
	public void testMigration() {
		File testDir = new File(base, "testMigrationManager");
		File managerDir = new File(testDir, ".manager");
		String fileName = "testMigration.txt";
		File file2 = new File(testDir, fileName+".2");
		File file5 = new File(testDir, fileName+".5");
		File fileTable = new File(managerDir, ".fileTable");
		File fileTable1 = new File(managerDir, ".fileTable.1");
		File fileTable2 = new File(managerDir, ".fileTable.2");
		File fileTable3 = new File(managerDir, ".fileTable.3");
		String contents1 = "test reliable file contents #1";
		String contents2 = "test reliable file contents #2";
		String contents3 = "test reliable file contents #3";
		try {
			// create a .fileTable and a normal file
			managerDir.mkdirs();
			writeToFile(fileTable, "#safe table\n"+fileName+"=2\n");
			writeToFile(file2, contents1);
			manager1 = new StorageManager(testDir, null);
			manager1.open(true);
			File test = manager1.lookup(fileName, false);
			assertNotNull(test);
			assertTrue(test.exists());
			
			// update a new file
			File testFile = manager1.createTempFile(fileName);
			writeToFile(testFile, contents2);
			manager1.update(new String[] {fileName}, new String[] {testFile.getName()});
			// again
			testFile = manager1.createTempFile(fileName);
			writeToFile(testFile, contents3);
			manager1.update(new String[] {fileName}, new String[] {testFile.getName()});
			// again
			testFile = manager1.createTempFile(fileName);
			writeToFile(testFile, contents1);
			manager1.update(new String[] {fileName}, new String[] {testFile.getName()});
			manager1.close();
			manager1= null;
			
			String[] files = managerDir.list();
			assertEquals(4, files.length);
			//original file never gets deleted
			assertTrue(fileTable.exists());
			//behaves like a reliableFile?
			assertFalse(fileTable1.exists());
			assertTrue(fileTable2.exists());
			assertTrue(fileTable3.exists());
			//files are as expected?
			files = testDir.list();
			assertEquals(2, files.length);
			assertTrue(file5.exists());
			
			manager2 = new StorageManager(testDir, null);
			manager2.open(true);
			testFile = manager2.lookup(fileName, false);
			assertNotNull(testFile);
			assertTrue(testFile.exists());
			assertTrue(testFile.getName().endsWith(".5"));
			manager2.close();
			manager2=null;
			
		} catch(IOException e) {
			fail("unexepected exception", e);
		}
	}

	/**
	 * This tests that an output stream abort behave as expected.
	 *
	 */
	public void testAbort() {
		testAbort(true);
		testAbort(false);
	}
	
	private void testAbort(boolean reliable) {
		String fileName;
		if (reliable)
			fileName = "abortFileReliable.txt";
		else 
			fileName = "abortFileStd.txt";
		File file1 = new File(base, fileName+".1");
		File file2 = new File(base, fileName+".2");
		File file3 = new File(base, fileName+".3");
		String contents1 = "test reliable file contents #1";
		String contents2 = "test reliable file contents #2";
		try {
			System.setProperty("osgi.useReliableFiles", "true"); // force reliable files
			manager1 = new StorageManager(base, null);
			manager1.open(true);
			//create version 1
			ManagedOutputStream smos = manager1.getOutputStream(fileName);
			smos.write(contents1.getBytes());
			smos.close();
			
			//start creating version 2
			smos = manager1.getOutputStream(fileName);
			smos.write(contents2.getBytes());
			smos.abort();
			smos.close(); // shouldn't cause exception, check!
			
			// now see if we're still on version #1
			assertEquals(1, manager1.getId(fileName));
			// check contents also
			InputStream is = manager1.getInputStream(fileName);
			assertNotNull(is);
			assertEquals(contents1, getInputStreamContents(is));
			manager1.close();
			manager1=null;
			
			// open a new manager & check the same thing to ensure the database is correct
			System.setProperty("osgi.useReliableFiles", "true"); // force reliable files
			manager2 = new StorageManager(base, null);
			manager2.open(true);
			//create version 1
			// now see if we're still on version #1
			assertEquals(1, manager2.getId(fileName));
			// check contents also
			is = manager2.getInputStream(fileName);
			assertNotNull(is);
			assertEquals(contents1, getInputStreamContents(is));
			manager2.close();
			manager2=null;
			assertTrue(file1.exists());
			assertFalse(file2.exists());
			assertFalse(file3.exists());
		} catch(IOException e) {
			fail("unexepected exception", e);
		}
	}

	
	/**
	 * This tests if getting an output stream-set work properly.
	 *
	 */
	public void testGetOutputStreamSet() {
		testGetOutputStreamSet(true);
		testGetOutputStreamSet(false);
	}
	
	private void testGetOutputStreamSet(boolean reliable) {
		File mgrDir;
		if (reliable)
			mgrDir = new File(base, "getSetReliable");
		else
			mgrDir = new File(base, "getSetStd");
		String fileName1 = "testSet1.txt";
		String fileName2 = "testSet2.txt";
		File file1_1 = new File(mgrDir, fileName1+".1");
		File file1_2 = new File(mgrDir, fileName1+".2");
		File file2_1 = new File(mgrDir, fileName2+".1");
		File file2_2 = new File(mgrDir, fileName2+".2");
		String contents1 = "test reliable file contents #1";
		String contents2 = "test reliable file contents #2";
		try {
			System.setProperty("osgi.useReliableFiles", reliable ? "true" : "false"); // force reliable files
			manager1 = new StorageManager(mgrDir, null);
			manager1.open(true);
			ManagedOutputStream[] outs = manager1.getOutputStreamSet(new String[] {fileName1, fileName2});
			assertNotNull(outs);
			assertEquals(2, outs.length);
			
			outs[0].write(contents1.getBytes());
			outs[1].write(contents2.getBytes());
			outs[1].close();
			assertFalse(file1_1.exists());
			assertFalse(file2_1.exists());
			outs[0].close();
			assertTrue(file1_1.exists());
			assertTrue(file2_1.exists());
			
			outs = manager1.getOutputStreamSet(new String[] {fileName1, fileName2});
			assertNotNull(outs);
			
			outs[0].write("new data #1".getBytes());
			outs[1].write("new data #2".getBytes());
			outs[0].close();
			assertFalse(file1_2.exists());
			assertFalse(file2_2.exists());
			outs[1].close();
			assertTrue(file1_2.exists());
			assertTrue(file2_2.exists());
			manager1.close();
			manager1 = null;
			
			if (reliable) {
				// verify FM thinks they are reliable
				assertTrue(file1_1.exists());
				assertTrue(file2_1.exists());
			} else {
				// verify FM thinks they are not reliable
				assertFalse(file1_1.exists());
				assertFalse(file2_1.exists());				
			}
		
		} catch(IOException e) {
			fail("unexepected exception", e);
		}
	}
	
	/**
	 * This tests if aborting a managed stream-set works as expected
	 *
	 */
	public void testAbortStreamSet() {
		testAbortSet(true);
		testAbortSet(false);
	}
	
	private void testAbortSet(boolean reliable) {
		File mgrDir;
		if (reliable)
			mgrDir = new File(base, "abortSetReliable");
		else
			mgrDir = new File(base, "abortSetStd");
		String fileName1 = "test1.txt";
		String fileName2 = "test2.txt";
		String fileName3 = "test3.txt";
		String fileName4 = "test4.txt";
		String contents1 = "test reliable file contents #1";
		String contents2 = "test reliable file contents #2";
		try {
			mgrDir.mkdirs();
			String[] list = mgrDir.list();
			assertEquals(0, list.length);
			System.setProperty("osgi.useReliableFiles", reliable ? "true" : "false"); // force reliable files
			manager1 = new StorageManager(mgrDir, null);
			manager1.open(true);
			ManagedOutputStream[] outs = manager1.getOutputStreamSet(new String[] {fileName1, fileName2, fileName3, fileName4});
			assertNotNull(outs);
			
			outs[0].write(contents1.getBytes());
			outs[1].write(contents2.getBytes());
			outs[2].write(contents2.getBytes());
			outs[3].write(contents1.getBytes());
			//sanity check
			list = mgrDir.list();
			assertEquals(5, list.length);
			outs[2].close();
			outs[1].abort();
			outs[0].close(); //noop
			outs[3].close(); //noop
			outs[2].close(); //noop
			outs[1].close(); //noop
			list = mgrDir.list();
			assertEquals(1, list.length);
			assertNull(manager1.lookup(fileName1, false));
			assertNull(manager1.lookup(fileName2, false));
			assertNull(manager1.lookup(fileName3, false));
			assertNull(manager1.lookup(fileName4, false));
			manager1.close();
			manager1 = null;
			
			// open a new manager & check the same thing to ensure the database is correct
			System.setProperty("osgi.useReliableFiles", reliable ? "true" : "false"); // force reliable files
			manager2 = new StorageManager(mgrDir, null);
			manager2.open(true);
			//create version 1
			assertNull(manager2.lookup(fileName1, false));
			assertNull(manager2.lookup(fileName2, false));
			assertNull(manager2.lookup(fileName3, false));
			assertNull(manager2.lookup(fileName4, false));
			list = mgrDir.list();
			assertEquals(1, list.length);
			manager2.close();
			manager2 = null;
		
		} catch(IOException e) {
			fail("unexepected exception", e);
		}
	}
	
}
