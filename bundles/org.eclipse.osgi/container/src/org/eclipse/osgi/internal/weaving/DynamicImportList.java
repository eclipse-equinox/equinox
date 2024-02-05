/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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
package org.eclipse.osgi.internal.weaving;

import java.util.*;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Constants;
import org.osgi.framework.PackagePermission;

/**
 * A list of DynamicImport-Package statements that are to be used for adding new
 * dynamic imports to a bundle class loader.
 */
public class DynamicImportList extends AbstractList<String> implements RandomAccess {
	// the collection of valid DynamicImport-Package statments.
	private final List<String> imports = new ArrayList<>(0);
	private final WovenClassImpl wovenClass;

	public DynamicImportList(WovenClassImpl wovenClass) {
		super();
		this.wovenClass = wovenClass;
	}

	@Override
	public String get(int index) {
		return imports.get(index);
	}

	@Override
	public int size() {
		return imports.size();
	}

	@Override
	public String set(int index, String element) {
		wovenClass.checkPermission();
		validateSyntaxAndCheckPackagePermission(element);
		return imports.set(index, element);
	}

	@Override
	public void add(int index, String element) {
		wovenClass.checkPermission();
		validateSyntaxAndCheckPackagePermission(element);
		imports.add(index, element);
	}

	@Override
	public String remove(int index) {
		wovenClass.checkPermission();
		return imports.remove(index);
	}

	private void validateSyntaxAndCheckPackagePermission(String dynamicImportPackageDescription) {
		ManifestElement[] clauses;
		// Validate the syntax of imports that are added.
		try {
			clauses = ManifestElement.parseHeader(Constants.IMPORT_PACKAGE, dynamicImportPackageDescription);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		SecurityManager sm = System.getSecurityManager();
		if (sm == null)
			return;
		// Security is enabled. Ensure the weaver has import package permission
		// for each dynamic import added.
		for (ManifestElement clause : clauses)
			for (String pkg : clause.getValueComponents())
				sm.checkPermission(new PackagePermission(pkg, PackagePermission.IMPORT));
	}
}
