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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.osgi.service.resolver.*;

class StateReader {

	// objectTable will be a hashmap of objects. The objects will be things
	// like a plugin descriptor, extension, extension point, etc. The integer
	// index value will be used in the cache to allow cross-references in the
	// cached registry.
	protected List objectTable = new ArrayList();

	public static final byte STATE_CACHE_VERSION = 5;
	public static final byte NULL = 0;
	public static final byte OBJECT = 1;
	public static final byte INDEX = 2;

	private int addToObjectTable(Object object) {
		objectTable.add(object);
		// return the index of the object just added (i.e. size - 1)
		return (objectTable.size() - 1);
	}

	private boolean readState(StateImpl state, DataInputStream in, long expectedTimestamp) throws IOException {
		if (in.readByte() != STATE_CACHE_VERSION)
			return false;
		byte tag = readTag(in);
		if (tag != OBJECT)
			return false;
		long timestampRead = in.readLong();
		if (expectedTimestamp >= 0 && timestampRead != expectedTimestamp)
			return false;
		addToObjectTable(state);
		int length = in.readInt();
		if (length == 0)
			return true;
		for (int i = 0; i < length; i++)
			state.basicAddBundle(readBundleDescription(in));
		state.setTimeStamp(timestampRead);
		state.setResolved(in.readBoolean());
		if (!state.isResolved())
			return true;
		int resolvedLength = in.readInt();
		for(int i = 0;i < resolvedLength;i++)
			state.addResolvedBundle(readBundleDescription(in));
		return true;
	}
	private BundleDescriptionImpl readBundleDescription(DataInputStream in) throws IOException {
		byte tag = readTag(in);
		if (tag == NULL)
			return null;
		if (tag == INDEX)
			return (BundleDescriptionImpl) objectTable.get(in.readInt());
		BundleDescriptionImpl result = new BundleDescriptionImpl();
		addToObjectTable(result);		
		result.setBundleId(in.readLong());		
		result.setUniqueId(readString(in, false));
		result.setLocation(readString(in, false));
		result.setState(in.readInt());
		result.setVersion(readVersion(in));
		result.setHost(readHostSpec(in));
		int packageCount = in.readInt();
		if (packageCount > 0) {
			PackageSpecification[] packages = new PackageSpecification[packageCount];
			for (int i = 0; i < packages.length; i++)
				packages[i] = readPackageSpec(in);
			
			result.setPackages(packages);
		}
		int providedPackageCount = in.readInt();
		if (providedPackageCount > 0) {
			String[] providedPackages = new String[providedPackageCount];
			for (int i = 0; i < providedPackages.length; i++)
				providedPackages[i] = in.readUTF();
			result.setProvidedPackages(providedPackages);
		}
		int requiredBundleCount = in.readInt();
		if (requiredBundleCount > 0) {
			BundleSpecification[] requiredBundles = new BundleSpecification[requiredBundleCount];
			for (int i = 0; i < requiredBundles.length; i++)
				requiredBundles[i] = readBundleSpec(in);
			result.setRequiredBundles(requiredBundles);
		}
		result.setSingleton(in.readBoolean());
		return result;
	}
	private BundleSpecificationImpl readBundleSpec(DataInputStream in) throws IOException {
		BundleSpecificationImpl result = new BundleSpecificationImpl();
		readVersionConstraint(result, in);
		result.setExported(in.readBoolean());
		result.setOptional(in.readBoolean());
		return result;
	}
	private PackageSpecificationImpl readPackageSpec(DataInputStream in) throws IOException {
		PackageSpecificationImpl result = new PackageSpecificationImpl();
		readVersionConstraint(result, in);
		result.setExport(in.readBoolean());
		return result;
	}
	private HostSpecificationImpl readHostSpec(DataInputStream in) throws IOException {
		byte tag = readTag(in);
		if (tag == NULL)
			return null;
		HostSpecificationImpl result = new HostSpecificationImpl();
		readVersionConstraint(result, in);
		result.setReloadHost(in.readBoolean());
		return result;
	}
	// called by readers for VersionConstraintImpl subclasses
	private void readVersionConstraint(VersionConstraintImpl version, DataInputStream in) throws IOException {
		version.setName(readString(in, false));
		version.setVersionSpecification(readVersion(in));
		version.setMatchingRule(in.readByte());
		version.setActualVersion(readVersion(in));
		version.setSupplier(readBundleDescription(in));
	}
	private Version readVersion(DataInputStream in) throws IOException {
		byte tag = readTag(in);
		if (tag == NULL)
			return null;
		if (tag == INDEX)
			return (Version) objectTable.get(in.readInt());
		int majorComponent = in.readInt();
		int minorComponent = in.readInt();
		int serviceComponent = in.readInt();
		String qualifierComponent = readString(in, false);
		Version result = new Version(majorComponent, minorComponent, serviceComponent, qualifierComponent);
		addToObjectTable(result);
		return result;		
	}
	public final boolean loadState(StateImpl state, DataInputStream input, long expectedTimestamp) throws IOException {
		try {
			return readState(state, input, expectedTimestamp);
		} finally {
			input.close();
		}
	}
	public final boolean loadState(StateImpl state, DataInputStream input) throws IOException {
		return loadState(state, input, -1);
	}
	private String readString(DataInputStream in, boolean intern) throws IOException {
		byte type = in.readByte();
		if (type == NULL)
			return null;
		if (intern)
			return in.readUTF().intern();
		else
			return in.readUTF();
	}
	private byte readTag(DataInputStream in) throws IOException {
		return in.readByte();
	}
}