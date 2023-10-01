/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     Alexander Fedorov (ArSysOp) - Bug 561712
 *******************************************************************************/
package org.eclipse.equinox.common.tests;

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.tests.harness.CoreTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

public class StatusTest extends CoreTest {

	private int status1Severity = IStatus.OK;
	private String status1PluginId = "org.eclipse.core.tests.runtime";
	private int status1Code = -20;
	private String status1Message = "Something was canceled";
	private Exception status1Exception = new OperationCanceledException();
	private Status status1;

	private int status2Severity = IStatus.ERROR;
	private String status2PluginId = " ";
	private int status2Code = IStatus.OK;
	private String status2Message = "";
	private Exception status2Exception = null;
	private Status status2;

	private String multistatus1PluginId = "org.eclipse.core.tests.multistatus1";
	private int multistatus1Code = 20;
	private IStatus[] multistatus1Children = new IStatus[0];
	private String multistatus1Message = "Multistatus #1 message";
	private Throwable multistatus1Exception = new OperationCanceledException();
	private MultiStatus multistatus1;

	private MultiStatus multistatus2;

	public StatusTest() {
		super(null);
	}

	public StatusTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() {
		status1 = new Status(status1Severity, status1PluginId, status1Code, status1Message, status1Exception);
		status2 = new Status(status2Severity, status2PluginId, status2Code, status2Message, status2Exception);

		multistatus1 = new MultiStatus(multistatus1PluginId, multistatus1Code, multistatus1Children,
				multistatus1Message, multistatus1Exception);
		multistatus2 = new MultiStatus(" ", 45, new Status[0], "", null);
		multistatus2.add(status1);
		multistatus2.add(status1);
	}

	public void testMultiStatusReturnValues() {
		assertEquals("1.1", multistatus1PluginId, multistatus1.getPlugin());
		assertEquals("1.2", multistatus1Code, multistatus1.getCode());
		assertTrue("1.3", Arrays.equals(multistatus1Children, multistatus1.getChildren()));
		assertEquals("1.4", multistatus1Message, multistatus1.getMessage());
		assertEquals("1.5", multistatus1Exception, multistatus1.getException());
		assertTrue("1.6", multistatus1.isMultiStatus());
		assertEquals("1.7", IStatus.OK, multistatus1.getSeverity());
		assertTrue("1.8", multistatus1.isOK());
		assertEquals("1.9", false, status1.matches(IStatus.ERROR | IStatus.WARNING | IStatus.INFO));
	}

	public void testSingleStatusReturnValues() {
		assertEquals("1.0", status1Severity, status1.getSeverity());
		assertEquals("1.1", status1PluginId, status1.getPlugin());
		assertEquals("1.2", status1Code, status1.getCode());
		assertEquals("1.3", status1Message, status1.getMessage());
		assertEquals("1.4", status1Exception, status1.getException());
		assertEquals("1.5", 0, status1.getChildren().length);
		assertEquals("1.6", false, status1.isMultiStatus());
		assertEquals("1.7", status1Severity == IStatus.OK, status1.isOK());
		assertEquals("1.8", status1.matches(IStatus.ERROR | IStatus.WARNING | IStatus.INFO), !status1.isOK());

		assertEquals("2.0", status2Severity, status2.getSeverity());
		assertEquals("2.1", status2PluginId, status2.getPlugin());
		assertEquals("2.2", status2Code, status2.getCode());
		assertEquals("2.3", status2Message, status2.getMessage());
		assertEquals("2.4", status2Exception, status2.getException());
		assertEquals("2.5", 0, status2.getChildren().length);
		assertEquals("2.6", false, status2.isMultiStatus());
		assertEquals("2.7", status2Severity == IStatus.OK, status2.isOK());
		assertEquals("2.8", status2.matches(IStatus.ERROR), !status2.isOK());
	}

	public void testAdd() {

		multistatus1.add(status1);
		assertEquals("1.0", status1, (multistatus1.getChildren())[0]);

		multistatus1.add(multistatus2);
		assertEquals("1.1", multistatus2, (multistatus1.getChildren())[1]);

		multistatus1.add(multistatus1);
		assertEquals("1.2", multistatus1, (multistatus1.getChildren())[2]);

	}

	public void testSingleFromClass() throws ClassNotFoundException, IOException, BundleException {
		assertEquals("org.eclipse.equinox.common.tests", new Status(IStatus.WARNING, StatusTest.class, "").getPlugin());
		assertEquals("org.eclipse.equinox.common", new Status(IStatus.ERROR, IStatus.class, "", null).getPlugin());
		assertEquals("java.lang.String", new Status(IStatus.WARNING, String.class, 0, "", null).getPlugin());
		assertEquals("unknown", new Status(IStatus.WARNING, (Class<?>) null, "").getPlugin());
		assertEquals(TestClass.class.getName(),
				new Status(IStatus.WARNING, installNoBSNBundle().loadClass(TestClass.class.getName()), "").getPlugin());
	}

	public void testMultiFromClass() throws ClassNotFoundException, IOException, BundleException {
		assertEquals("org.eclipse.equinox.common", new MultiStatus(IStatus.class, 0, "").getPlugin());
		assertEquals("org.eclipse.equinox.common.tests", new MultiStatus(StatusTest.class, 0, "").getPlugin());
		assertEquals("java.lang.String", new MultiStatus(String.class, 0, new Status[0], "", null).getPlugin());
		assertEquals("unknown", new MultiStatus((Class<?>) null, 0, "").getPlugin());
		assertEquals(TestClass.class.getName(),
				new MultiStatus(installNoBSNBundle().loadClass(TestClass.class.getName()), 0, "").getPlugin());
	}

	private Bundle installNoBSNBundle() throws IOException, BundleException {
		BundleContext bc = FrameworkUtil.getBundle(getClass()).getBundleContext();
		return installTestClassBundle(bc);
	}

	public static synchronized Bundle installTestClassBundle(BundleContext bc) throws IOException, BundleException {
		File noNameBSNFile = bc.getDataFile("noNameBSN.jar");
		if (!noNameBSNFile.isFile()) {
			noNameBSNFile.delete();
			URI noNameBSNJar = URI.create("jar:" + noNameBSNFile.toURI().toASCIIString());

			try (FileSystem zipfs = FileSystems.newFileSystem(noNameBSNJar,
					Collections.singletonMap("create", "true"))) {
				URL testClassURL = StatusTest.class.getResource("TestClass.class");
				Path testClassPath = zipfs.getPath(testClassURL.getPath().substring(1));
				// copy a file into the zip file
				Files.createDirectories(testClassPath.getParent());
				Files.copy(testClassURL.openStream(), testClassPath);
			}
		}
		return bc.installBundle(noNameBSNFile.toURI().toASCIIString());
	}

	public void testAddAll() {

		multistatus1.add(status2);
		multistatus1.addAll(multistatus2);
		Status[] array = new Status[3];
		array[0] = status2;
		array[1] = status1;
		array[2] = status1;

		assertTrue("1.0", multistatus1.getChildren().length == 3);
		assertTrue("1.1", Arrays.equals(array, multistatus1.getChildren()));

		multistatus1.add(multistatus2);
		multistatus1.addAll(multistatus1);
		Status[] array2 = new Status[8];
		array2[0] = status2;
		array2[1] = status1;
		array2[2] = status1;
		array2[3] = multistatus2;
		array2[4] = status2;
		array2[5] = status1;
		array2[6] = status1;
		array2[7] = multistatus2;

		assertTrue("2.0", multistatus1.getChildren().length == 8);
		assertTrue("2.1", Arrays.equals(array2, multistatus1.getChildren()));

	}

	public void testIsOK() {

		assertTrue("1.0", multistatus2.isOK());

		multistatus1.add(status2);
		multistatus1.addAll(multistatus2);
		assertTrue("1.1", !multistatus1.isOK());

	}

	public void testMerge() {

		multistatus1.merge(status2);
		multistatus1.merge(multistatus2);
		Status[] array = new Status[3];
		array[0] = status2;
		array[1] = status1;
		array[2] = status1;

		assertTrue("1.0", multistatus1.getChildren().length == 3);
		assertTrue("1.1", Arrays.equals(array, multistatus1.getChildren()));

		multistatus1.merge(multistatus1);
		Status[] array2 = new Status[6];
		array2[0] = status2;
		array2[1] = status1;
		array2[2] = status1;
		array2[3] = status2;
		array2[4] = status1;
		array2[5] = status1;

		assertTrue("2.0", multistatus1.getChildren().length == 6);
		assertTrue("2.1", Arrays.equals(array2, multistatus1.getChildren()));

		multistatus2.add(multistatus1);
		assertTrue("3.0", !multistatus2.isOK());
		multistatus2.merge(multistatus2.getChildren()[2]);
		assertTrue("3.1", multistatus2.getChildren().length == 9);

	}

	public void testInfo() {
		IStatus info = Status.info("message");
		assertEquals(IStatus.INFO, info.getSeverity());
		assertEquals("message", info.getMessage());
		assertEquals(IStatus.OK, info.getCode());
		assertEquals("org.eclipse.equinox.common.tests", info.getPlugin());
		assertNull(info.getException());
		assertArrayEquals(new IStatus[] {}, info.getChildren());
	}

	public void testWarning() {
		IStatus warning = Status.warning("message");
		assertEquals(IStatus.WARNING, warning.getSeverity());
		assertEquals("message", warning.getMessage());
		assertEquals(IStatus.OK, warning.getCode());
		assertEquals("org.eclipse.equinox.common.tests", warning.getPlugin());
		assertNull(warning.getException());
		assertArrayEquals(new IStatus[] {}, warning.getChildren());
	}

	public void testWarningWithException() {

		Status warningWithException = Status.warning("message", new Exception("exception"));
		assertEquals(IStatus.WARNING, warningWithException.getSeverity());
		assertEquals("message", warningWithException.getMessage());
		assertEquals(IStatus.OK, warningWithException.getCode());
		assertEquals("org.eclipse.equinox.common.tests", warningWithException.getPlugin());
		assertEquals("exception", warningWithException.getException().getMessage());
		assertArrayEquals(new IStatus[] {}, warningWithException.getChildren());
	}

	public void testError() {
		IStatus error = Status.error("message");
		assertEquals(IStatus.ERROR, error.getSeverity());
		assertEquals("message", error.getMessage());
		assertEquals(IStatus.OK, error.getCode());
		assertEquals("org.eclipse.equinox.common.tests", error.getPlugin());
		assertNull(error.getException());
		assertArrayEquals(new IStatus[] {}, error.getChildren());
	}

	public void testErrorWithException() {
		IStatus errorWithException = Status.error("message", new Exception("exception"));
		assertEquals(IStatus.ERROR, errorWithException.getSeverity());
		assertEquals("message", errorWithException.getMessage());
		assertEquals(IStatus.OK, errorWithException.getCode());
		assertEquals("org.eclipse.equinox.common.tests", errorWithException.getPlugin());
		assertEquals("exception", errorWithException.getException().getMessage());
		assertArrayEquals(new IStatus[] {}, errorWithException.getChildren());
	}

}
