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

public class SimpleTests extends TestCase {
	FileManager manager1;
	FileManager manager2;
	File base;
	static String TEST1 = "test.txt";

	/**
	 * Constructs a test case with the given name.
	 */
	public SimpleTests(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(SimpleTests.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		base = new File(Platform.getConfigurationLocation().getURL().getPath());
		manager1 = new FileManager(base, null);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testAddRemove() {
		try {
		manager1.open(true);
		assertEquals(null, manager1.lookup(TEST1, false));
		assertEquals(-1, manager1.getId(TEST1));

		manager1.add(TEST1);
		assertEquals(new File(base, TEST1 + ".0"), manager1.lookup(TEST1, false));
		assertEquals(0, manager1.getId(TEST1));

		manager1.remove(TEST1);
		assertEquals(null, manager1.lookup(TEST1, false));
		assertEquals(-1, manager1.getId(TEST1));
		} catch(IOException e) {
			//No exception can occurs since all the lookup calls are done with false
			e.printStackTrace();
		}
	}

//	public void testUpdate1() throws IOException {
//		manager1.add(TEST1);
//		update(manager1, TEST1 + ".new");
//		assertEquals(new File(base, TEST1), manager1.lookup(TEST1, false));
//		assertEquals(2, manager1.getId(TEST1));
//		assertTrue(manager1.getTimeStamp(TEST1) != 0);
//	}
//
//	/*
//	 * should be run after testing single update
//	 */
//	public void testUpdate2() throws IOException {
//		long oldStamp = manager1.getTimeStamp(TEST1);
//		update(manager1, TEST1 + ".new2");
//		assertEquals(new File(base, TEST1), manager1.lookup(TEST1));
//		assertEquals(3, manager1.getId(TEST1));
//		assertTrue(manager1.getTimeStamp(TEST1) != 0);
//		assertTrue(new File(base, TEST1 + ".2").exists());
//		assertTrue(oldStamp != manager1.getTimeStamp(TEST1));
//	}
//
//	/*
//	 * should be run after testing update2
//	 */
//	public void testUpdate3() throws IOException {
//		manager2 = new FileManager(base);
//		assertEquals(new File(base, TEST1), manager2.lookup(TEST1));
//		assertEquals(3, manager2.getId(TEST1));
//		assertTrue(manager2.getTimeStamp(TEST1) != 0);
//		assertTrue(new File(base, TEST1 + ".2").exists());
//
//		update(manager2, TEST1 + ".new3");
//		assertEquals(new File(base, TEST1), manager2.lookup(TEST1));
//		assertEquals(4, manager2.getId(TEST1));
//		assertTrue(manager2.getTimeStamp(TEST1) != 0);
//		assertTrue(new File(base, TEST1 + ".3").exists());
//
//		assertNotSame(new File(base, TEST1), manager1.lookup(TEST1));
//		assertEquals(new File(base, TEST1 + ".3"), manager1.lookup(TEST1));
//		assertEquals(3, manager1.getId(TEST1));
//		assertTrue(manager1.getTimeStamp(TEST1) != 0);
//		assertTrue(manager1.getTimeStamp(TEST1) != manager2.getTimeStamp(TEST1));
//	}

	private void update(FileManager manager, String filename) throws IOException {
		writeFile(new File(base, filename));
		manager.update(new String[] {TEST1}, new String[] {filename});
	}

	private void writeFile(File filename) {
		try {
			FileOutputStream out = new FileOutputStream(filename);
			out.write(("test - " + System.currentTimeMillis()).getBytes());
			out.close();
		} catch (IOException e) {
		}
	}
}
