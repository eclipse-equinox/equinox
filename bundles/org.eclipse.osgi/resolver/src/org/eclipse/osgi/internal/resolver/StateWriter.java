/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.service.resolver.*;

class StateWriter {

	// TODO not sure this comment applies anymore
	// objectTable will be a hashmap of objects. The objects will be things
	// like a plugin descriptor, extension, extension point, etc. The integer
	// index value will be used in the cache to allow cross-references in the
	// cached registry.
	protected Map objectTable = new HashMap();

	public static final byte NULL = 0;
	public static final byte OBJECT = 1;
	public static final byte INDEX = 2;

	private int addToObjectTable(Object object) {
		objectTable.put(object, new Integer(objectTable.size()));
		// return the index of the object just added (i.e. size - 1)
		return (objectTable.size() - 1);
	}

	private int getFromObjectTable(Object object) {
		if (objectTable != null) {
			Object objectResult = objectTable.get(object);
			if (objectResult != null) {
				return ((Integer) objectResult).intValue();
			}
		}
		return -1;
	}

	private boolean writePrefix(Object object, DataOutputStream out) throws IOException {
		if (writeIndex(object, out))
			return true;
		// add this object to the object table first
		addToObjectTable(object);
		out.writeByte(OBJECT);
		return false;
	}

	private void writeState(StateImpl state, DataOutputStream out) throws IOException {
		out.write(StateReader.STATE_CACHE_VERSION);
		if (writePrefix(state, out))
			return;
		out.writeLong(state.getTimeStamp());
		BundleDescription[] bundles = state.getBundles();
		out.writeInt(bundles.length);
		if (bundles.length == 0)
			return;
		for (int i = 0; i < bundles.length; i++)
			writeBundleDescription((BundleDescriptionImpl) bundles[i], out);
		out.writeBoolean(state.isResolved());
		if (!state.isResolved())
			return;
		BundleDescription[] resolvedBundles = state.getResolvedBundles();
		out.writeInt(resolvedBundles.length);
		for (int i = 0; i < resolvedBundles.length; i++)
			writeBundleDescription((BundleDescriptionImpl) resolvedBundles[i], out);
	}

	private void writeBundleDescription(BundleDescriptionImpl bundle, DataOutputStream out) throws IOException {
		if (writePrefix(bundle, out))
			return;
		out.writeLong(bundle.getBundleId());
		writeStringOrNull(bundle.getSymbolicName(), out);
		writeStringOrNull(bundle.getLocation(), out);
		out.writeInt(bundle.getState());
		writeVersion(bundle.getVersion(), out);
		writeHostSpec((HostSpecificationImpl) bundle.getHost(), out);

		PackageSpecification[] packages = bundle.getPackages();
		out.writeInt(packages.length);
		for (int i = 0; i < packages.length; i++)
			writePackageSpec((PackageSpecificationImpl) packages[i], out);

		String[] providedPackages = bundle.getProvidedPackages();
		out.writeInt(providedPackages.length);
		for (int i = 0; i < providedPackages.length; i++)
			out.writeUTF(providedPackages[i]);

		BundleSpecification[] requiredBundles = bundle.getRequiredBundles();
		out.writeInt(requiredBundles.length);
		for (int i = 0; i < requiredBundles.length; i++)
			writeBundleSpec((BundleSpecificationImpl) requiredBundles[i], out);

		out.writeBoolean(bundle.isSingleton());
	}

	private void writeBundleSpec(BundleSpecificationImpl bundle, DataOutputStream out) throws IOException {
		writeVersionConstraint(bundle, out);
		out.writeBoolean(bundle.isExported());
		out.writeBoolean(bundle.isOptional());
	}

	private void writePackageSpec(PackageSpecificationImpl packageSpec, DataOutputStream out) throws IOException {
		writeVersionConstraint(packageSpec, out);
		out.writeBoolean(packageSpec.isExported());
	}

	private void writeHostSpec(HostSpecificationImpl host, DataOutputStream out) throws IOException {
		if (host == null) {
			out.writeByte(NULL);
			return;
		}
		out.writeByte(OBJECT);
		writeVersionConstraint(host, out);
		out.writeBoolean(host.reloadHost());
	}

	// called by writers for VersionConstraintImpl subclasses
	private void writeVersionConstraint(VersionConstraintImpl version, DataOutputStream out) throws IOException {
		writeStringOrNull(version.getName(), out);
		writeVersionRange(version.getVersionRange(), out);
		out.writeByte(version.getMatchingRule());
		writeVersion(version.getActualVersion(), out);
		writeBundleDescription((BundleDescriptionImpl) version.getSupplier(), out);
	}

	private void writeVersion(Version version, DataOutputStream out) throws IOException {
		// TODO: should assess whether avoiding sharing versions would be good
		if (writePrefix(version, out))
			return;
		out.writeInt(version.getMajorComponent());
		out.writeInt(version.getMinorComponent());
		out.writeInt(version.getMicroComponent());
		writeStringOrNull(version.getQualifierComponent(), out);
		out.writeBoolean(version.isInclusive());
	}

	private void writeVersionRange(VersionRange versionRange, DataOutputStream out) throws IOException {
		writeVersion(versionRange.getMinimum(), out);
		writeVersion(versionRange.getMaximum(), out);
	}

	private boolean writeIndex(Object object, DataOutputStream out) throws IOException {
		if (object == null) {
			out.writeByte(NULL);
			return true;
		}
		int index = getFromObjectTable(object);
		if (index == -1)
			return false;
		out.writeByte(INDEX);
		out.writeInt(index);
		return true;
	}

	public void saveState(StateImpl state, DataOutputStream output) throws IOException {
		try {
			writeState(state, output);
		} finally {
			output.close();
		}
	}

	private void writeStringOrNull(String string, DataOutputStream out) throws IOException {
		if (string == null)
			out.writeByte(NULL);
		else {
			out.writeByte(OBJECT);
			out.writeUTF(string);
		}
	}
}