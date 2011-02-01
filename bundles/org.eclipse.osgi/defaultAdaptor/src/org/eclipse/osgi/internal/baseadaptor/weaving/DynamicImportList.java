/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.baseadaptor.weaving;

import java.util.*;
import org.eclipse.osgi.internal.resolver.StateBuilder;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Constants;

/**
 * A list of DynamicImport-Package statements that are to be used for adding new 
 * dynamic imports to a bundle class loader.
 *
 */
public class DynamicImportList extends AbstractList<String> implements RandomAccess {
	// the collection of valid DynamicImport-Package statments.
	private final List<String> imports = new ArrayList<String>(0);
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
		validateSyntax(element);
		return imports.set(index, element);
	}

	@Override
	public void add(int index, String element) {
		wovenClass.checkPermission();
		validateSyntax(element);
		imports.add(index, element);
	}

	@Override
	public String remove(int index) {
		wovenClass.checkPermission();
		return imports.remove(index);
	}

	private void validateSyntax(String imported) {
		// validate the syntax of imports that are added.
		ManifestElement[] importElements;
		try {
			importElements = ManifestElement.parseHeader(Constants.IMPORT_PACKAGE, imported);

			// validate the syntax is correct
			StateBuilder.checkImportExportSyntax(Constants.IMPORT_PACKAGE, importElements, false, false, false);
			// validate we can create an import spec out of it.
			List<ImportPackageSpecification> dynamicImportSpecs = new ArrayList<ImportPackageSpecification>(importElements.length);
			for (ManifestElement dynamicImportElement : importElements)
				StateBuilder.addImportPackages(dynamicImportElement, dynamicImportSpecs, 2, true);
		} catch (Throwable t) {
			IllegalArgumentException exception = new IllegalArgumentException();
			exception.initCause(t);
			throw exception;
		}
		return;
	}
}
