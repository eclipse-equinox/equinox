/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.loader;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import org.eclipse.osgi.framework.util.KeyedElement;

public abstract class PackageSource implements KeyedElement {
	protected String id;

	public PackageSource(String id) {
		// others depend on the id being interned; see SingleSourcePackage.equals
		this.id = id.intern();
	}

	public String getId() {
		return id;
	}

	public abstract SingleSourcePackage[] getSuppliers();

	public boolean compare(KeyedElement other) {
		return id.equals(((PackageSource) other).getId());
	}

	public int getKeyHashCode() {
		return id.hashCode();
	}

	public Object getKey() {
		return id;
	}

	public boolean isNullSource() {
		return false;
	}

	public boolean isFriend(String symbolicName) {
		return true;
	}

	public abstract Class<?> loadClass(String name) throws ClassNotFoundException;

	public abstract URL getResource(String name);

	public abstract Enumeration<URL> getResources(String name) throws IOException;

	//TODO See how this relates with FilteredSourcePackage. Overwriting or doing a double dispatch might be good.
	// This is intentionally lenient; we don't force all suppliers to match (only one)
	// it is better to get class cast exceptions in split package cases than miss an event
	public boolean hasCommonSource(PackageSource other) {
		if (other == null)
			return false;
		if (this == other)
			return true;
		SingleSourcePackage[] suppliers1 = getSuppliers();
		SingleSourcePackage[] suppliers2 = other.getSuppliers();
		if (suppliers1 == null || suppliers2 == null)
			return false;
		// This will return true if the specified source has at least one
		// of the suppliers of this source.
		for (int i = 0; i < suppliers1.length; i++)
			for (int j = 0; j < suppliers2.length; j++)
				if (suppliers2[j].equals(suppliers1[i]))
					return true;
		return false;
	}

	public abstract Collection<String> listResources(String path, String filePattern);
}
