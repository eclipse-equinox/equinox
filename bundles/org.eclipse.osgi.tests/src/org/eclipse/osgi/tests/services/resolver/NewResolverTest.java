/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.services.resolver;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.BundleException;

public class NewResolverTest extends AbstractStateTest {

	public NewResolverTest(String testName) {
		super(testName);
	}
	
	public static Test suite() {
		return new TestSuite(NewResolverTest.class);
	}
	
	public void testSkeleton() throws BundleException {
		State state = buildEmptyState();
		StateDelta delta = state.resolve();
	}
	
	public void testBasicScenario1() throws BundleException {
		State state = buildEmptyState();
		
		final String MAN_A = "Bundle-SymbolicName: A\n" + 
		                     "Export-Package: servlet; specification-version=2.1";
		BundleDescription bA = state.getFactory().createBundleDescription(parseManifest(MAN_A),
																		"org.eclipse.basic1A", 0);
		state.addBundle(bA);
		
		final String MAN_B = "Bundle-SymbolicName: B\n" + 
        					 "Import-Package: servlet; specification-version=2.1";
		BundleDescription bB = state.getFactory().createBundleDescription(parseManifest(MAN_B),
													"org.eclipse.basic1B", 1);
		state.addBundle(bB);

		StateDelta delta = state.resolve();
		
		BundleDescription b0 = state.getBundle(0);
		assertNotNull("0.1", b0);
		assertFullyResolved("0.2", b0);
		
		BundleDescription b1 = state.getBundle(1);
		assertNotNull("0.3", b1);
		assertFullyResolved("0.4", b1);
	}
}


