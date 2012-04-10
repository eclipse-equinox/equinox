/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.io.*;
import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.osgi.framework.util.ObjectPool;
import org.eclipse.osgi.framework.util.SecureAction;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.*;

/**
 * This class is internally threadsafe and supports client locking. Clients must <strong>not</strong> hold the monitor for
 * any {@link StateImpl} or {@link BundleDescriptionImpl} object when calling into the public methods of this class to prevent
 * possible deadlock.
 */
final class StateReader {
	public static final String STATE_FILE = ".state"; //$NON-NLS-1$
	public static final String LAZY_FILE = ".lazy"; //$NON-NLS-1$
	public static final String UTF_8 = "UTF-8"; //$NON-NLS-1$
	private static final int BUFFER_SIZE_LAZY = 4096;
	private static final int BUFFER_SIZE_FULLYREAD = 16384;
	private static final SecureAction secureAction = AccessController.doPrivileged(SecureAction.createSecureAction());

	// objectTable will be a hashmap of objects. The objects will be things
	// like BundleDescription, ExportPackageDescription, Version etc.. The integer
	// index value will be used in the cache to allow cross-references in the
	// cached state.
	final Map<Integer, Object> objectTable = Collections.synchronizedMap(new HashMap<Integer, Object>());

	private volatile File stateFile;
	private volatile File lazyFile;

	private volatile boolean lazyLoad = true;
	private volatile int numBundles;
	private volatile boolean accessedFlag = false;

	public static final byte STATE_CACHE_VERSION = 38;
	public static final byte NULL = 0;
	public static final byte OBJECT = 1;
	public static final byte INDEX = 2;
	public static final byte LONG_STRING = 3;

	public StateReader() //TODO - deprecated
	{
		lazyLoad = false;
	}

	public StateReader(File stateDirectory) {
		if (!stateDirectory.exists())
			stateDirectory.mkdirs();
		this.stateFile = new File(stateDirectory, STATE_FILE);
		this.lazyFile = new File(stateDirectory, LAZY_FILE);
		this.lazyLoad = false;
	}

	public StateReader(File stateFile, File lazyFile, boolean lazyLoad) {
		this.stateFile = stateFile;
		this.lazyFile = lazyFile;
		this.lazyLoad = lazyLoad;
	}

	private void addToObjectTable(Object object, int index) {
		objectTable.put(new Integer(index), object);
	}

	private Object getFromObjectTable(int index) {
		Object result = objectTable.get(new Integer(index));
		if (result == null)
			throw new IllegalStateException("Expected to find an object at table index: " + index); //$NON-NLS-1$
		return result;
	}

	private boolean readState(StateImpl state, long expectedTimestamp) throws IOException {
		DataInputStream in = new DataInputStream(new BufferedInputStream(secureAction.getFileInputStream(stateFile), BUFFER_SIZE_FULLYREAD));
		DataInputStream lazyIn = null;
		try {
			if (in.readByte() != STATE_CACHE_VERSION)
				return false;
			byte tag = readTag(in);
			if (tag != OBJECT)
				return false;
			int index = in.readInt();
			long timestampRead = in.readLong();
			if (expectedTimestamp >= 0 && timestampRead != expectedTimestamp)
				return false;
			addToObjectTable(state, index);
			// read the platform property keys
			String[] platformPropKeys = (String[]) readPlatformProp(in);
			state.addPlatformPropertyKeys(platformPropKeys);
			int numSets = in.readInt();
			Dictionary<?, ?>[] platformProps = new Dictionary[numSets];
			for (int i = 0; i < numSets; i++) {
				Hashtable<Object, Object> props = new Hashtable<Object, Object>(platformPropKeys.length);
				int numProps = in.readInt();
				for (int j = 0; j < numProps; j++) {
					Object value = readPlatformProp(in);
					if (value != null && j < platformPropKeys.length)
						props.put(platformPropKeys[j], value);
				}
				platformProps[i] = props;
			}
			state.setPlatformProperties(platformProps, false);
			numBundles = in.readInt();
			for (int i = 0; i < numBundles; i++) {
				BundleDescriptionImpl bundle = readBundleDescription(in);
				state.basicAddBundle(bundle);
				if (bundle.isResolved())
					state.addResolvedBundle(bundle);
			}
			// read the DisabledInfos
			int numDisableInfos = in.readInt();
			for (int i = 0; i < numDisableInfos; i++) {
				DisabledInfo info = readDisabledInfo(in);
				state.addDisabledInfo(info);
			}
			state.setTimeStamp(timestampRead);
			state.setResolved(in.readBoolean());
			if (lazyLoad)
				return true;
			//read in from lazy data file; using the fully read buffer size because we are reading the complete file in.
			lazyIn = new DataInputStream(new BufferedInputStream(secureAction.getFileInputStream(lazyFile), BUFFER_SIZE_FULLYREAD));
			for (int i = 0; i < numBundles; i++)
				readBundleDescriptionLazyData(lazyIn, 0);
		} finally {
			in.close();
			if (lazyIn != null)
				try {
					lazyIn.close();
				} catch (IOException e) {
					// ignore
				}
		}
		return true;
	}

	private boolean readStateDeprecated(StateImpl state, DataInputStream in, long expectedTimestamp) throws IOException {
		if (in.readByte() != STATE_CACHE_VERSION)
			return false;
		byte tag = readTag(in);
		if (tag != OBJECT)
			return false;
		int index = in.readInt();
		long timestampRead = in.readLong();
		if (expectedTimestamp >= 0 && timestampRead != expectedTimestamp)
			return false;
		addToObjectTable(state, index);
		// read the platform property keys
		String[] platformPropKeys = (String[]) readPlatformProp(in);
		state.addPlatformPropertyKeys(platformPropKeys);
		int numSets = in.readInt();
		Dictionary<?, ?>[] platformProps = new Dictionary[numSets];
		for (int i = 0; i < numSets; i++) {
			Hashtable<Object, Object> props = new Hashtable<Object, Object>(platformPropKeys.length);
			int numProps = in.readInt();
			for (int j = 0; j < numProps; j++) {
				Object value = readPlatformProp(in);
				if (value != null && j < platformPropKeys.length)
					props.put(platformPropKeys[j], value);
			}
			platformProps[i] = props;
		}
		state.setPlatformProperties(platformProps);
		numBundles = in.readInt();
		if (numBundles == 0)
			return true;
		for (int i = 0; i < numBundles; i++) {
			BundleDescriptionImpl bundle = readBundleDescription(in);
			state.basicAddBundle(bundle);
			if (bundle.isResolved())
				state.addResolvedBundle(bundle);
		}
		state.setTimeStamp(timestampRead);
		state.setResolved(in.readBoolean());
		in.readInt(); // skip past the old offset
		if (lazyLoad)
			return true;
		for (int i = 0; i < numBundles; i++)
			readBundleDescriptionLazyData(in, 0);
		return true;
	}

	private Object readPlatformProp(DataInputStream in) throws IOException {
		byte type = in.readByte();
		if (type == NULL)
			return null;
		int num = in.readInt();
		if (num == 1)
			return readString(in, false);
		String[] result = new String[num];
		for (int i = 0; i < result.length; i++)
			result[i] = readString(in, false);
		return result;
	}

	private BundleDescriptionImpl readBundleDescription(DataInputStream in) throws IOException {
		byte tag = readTag(in);
		if (tag == NULL)
			return null;
		if (tag == INDEX)
			return (BundleDescriptionImpl) getFromObjectTable(in.readInt());
		// first read in non-lazy loaded data
		BundleDescriptionImpl result = new BundleDescriptionImpl();
		addToObjectTable(result, in.readInt());

		result.setBundleId(in.readLong());
		readBaseDescription(result, in);
		result.setLazyDataOffset(in.readInt());
		result.setLazyDataSize(in.readInt());
		result.setStateBit(BundleDescriptionImpl.RESOLVED, in.readBoolean());
		result.setStateBit(BundleDescriptionImpl.SINGLETON, in.readBoolean());
		result.setStateBit(BundleDescriptionImpl.HAS_DYNAMICIMPORT, in.readBoolean());
		result.setStateBit(BundleDescriptionImpl.ATTACH_FRAGMENTS, in.readBoolean());
		result.setStateBit(BundleDescriptionImpl.DYNAMIC_FRAGMENTS, in.readBoolean());
		String[] mandatory = readList(in);
		if (mandatory != null)
			result.setDirective(Constants.MANDATORY_DIRECTIVE, mandatory);
		result.setAttributes(readMap(in));
		result.setArbitraryDirectives(readMap(in));
		result.setHost(readHostSpec(in));

		// set the bundle dependencies from imports and requires and hosts.
		int numDeps = in.readInt();
		if (numDeps > 0) {
			BundleDescription[] deps = new BundleDescription[numDeps];
			for (int i = 0; i < numDeps; i++)
				deps[i] = readBundleDescription(in);
			result.addDependencies(deps, false); // no need to check dups; we already know there are none when we resolved (bug 152900)
		}
		// No need to set the dependencies between fragment and hosts; that was already done in the above loop (bug 152900)
		// but we do need to set the dependencies between hosts and fragment.
		HostSpecificationImpl hostSpec = (HostSpecificationImpl) result.getHost();
		if (hostSpec != null) {
			BundleDescription[] hosts = hostSpec.getHosts();
			if (hosts != null) {
				for (int i = 0; i < hosts.length; i++)
					((BundleDescriptionImpl) hosts[i]).addDependency(result, false);
			}
		}
		// the rest is lazy loaded data
		result.setFullyLoaded(false);
		return result;
	}

	private BundleDescriptionImpl readBundleDescriptionLazyData(DataInputStream in, int skip) throws IOException {
		if (skip > 0)
			in.skipBytes(skip);
		int index = in.readInt();
		BundleDescriptionImpl result = (BundleDescriptionImpl) getFromObjectTable(index);
		if (result.isFullyLoaded()) {
			in.skipBytes(result.getLazyDataSize() - 4); // skip to the end subtract 4 for the int read already
			return result;
		}

		result.setLocation(readString(in, false));
		result.setPlatformFilter(readString(in, false));

		int exportCount = in.readInt();
		if (exportCount > 0) {
			ExportPackageDescription[] exports = new ExportPackageDescription[exportCount];
			for (int i = 0; i < exports.length; i++)
				exports[i] = readExportPackageDesc(in);
			result.setExportPackages(exports);
		}

		int importCount = in.readInt();
		if (importCount > 0) {
			ImportPackageSpecification[] imports = new ImportPackageSpecification[importCount];
			for (int i = 0; i < imports.length; i++)
				imports[i] = readImportPackageSpec(in);
			result.setImportPackages(imports);
		}

		int requiredBundleCount = in.readInt();
		if (requiredBundleCount > 0) {
			BundleSpecification[] requiredBundles = new BundleSpecification[requiredBundleCount];
			for (int i = 0; i < requiredBundles.length; i++)
				requiredBundles[i] = readBundleSpec(in);
			result.setRequiredBundles(requiredBundles);
		}

		int selectedCount = in.readInt();
		if (selectedCount > 0) {
			ExportPackageDescription[] selected = new ExportPackageDescription[selectedCount];
			for (int i = 0; i < selected.length; i++)
				selected[i] = readExportPackageDesc(in);
			result.setSelectedExports(selected);
		}

		int substitutedCount = in.readInt();
		if (substitutedCount > 0) {
			ExportPackageDescription[] selected = new ExportPackageDescription[substitutedCount];
			for (int i = 0; i < selected.length; i++)
				selected[i] = readExportPackageDesc(in);
			result.setSubstitutedExports(selected);
		}

		int resolvedCount = in.readInt();
		if (resolvedCount > 0) {
			ExportPackageDescription[] resolved = new ExportPackageDescription[resolvedCount];
			for (int i = 0; i < resolved.length; i++)
				resolved[i] = readExportPackageDesc(in);
			result.setResolvedImports(resolved);
		}

		int resolvedRequiredCount = in.readInt();
		if (resolvedRequiredCount > 0) {
			BundleDescription[] resolved = new BundleDescription[resolvedRequiredCount];
			for (int i = 0; i < resolved.length; i++)
				resolved[i] = readBundleDescription(in);
			result.setResolvedRequires(resolved);
		}

		int eeCount = in.readInt();
		if (eeCount > 0) {
			String[] ee = new String[eeCount];
			for (int i = 0; i < ee.length; i++)
				ee[i] = readString(in, false);
			result.setExecutionEnvironments(ee);
		}

		int dynamicPkgCnt = in.readInt();
		if (dynamicPkgCnt > 0) {
			HashMap<String, Long> dynamicStamps = new HashMap<String, Long>(dynamicPkgCnt);
			for (int i = 0; i < dynamicPkgCnt; i++) {
				String pkg = readString(in, false);
				Long stamp = new Long(in.readLong());
				dynamicStamps.put(pkg, stamp);
			}
			result.setDynamicStamps(dynamicStamps);
		}

		int genericCapCnt = in.readInt();
		if (genericCapCnt > 0) {
			GenericDescription[] capabilities = new GenericDescription[genericCapCnt];
			for (int i = 0; i < capabilities.length; i++)
				capabilities[i] = readGenericDescription(in);
			result.setGenericCapabilities(capabilities);
		}

		int genericReqCnt = in.readInt();
		if (genericReqCnt > 0) {
			GenericSpecification[] reqs = new GenericSpecification[genericReqCnt];
			for (int i = 0; i < reqs.length; i++)
				reqs[i] = readGenericSpecification(in);
			result.setGenericRequires(reqs);
		}

		int selectedGenCapCnt = in.readInt();
		if (selectedGenCapCnt > 0) {
			GenericDescription[] capabilities = new GenericDescription[selectedGenCapCnt];
			for (int i = 0; i < capabilities.length; i++)
				capabilities[i] = readGenericDescription(in);
			result.setSelectedCapabilities(capabilities);
		}

		int resolvedGenCapCnt = in.readInt();
		if (resolvedGenCapCnt > 0) {
			GenericDescription[] capabilities = new GenericDescription[resolvedGenCapCnt];
			for (int i = 0; i < capabilities.length; i++)
				capabilities[i] = readGenericDescription(in);
			result.setResolvedCapabilities(capabilities);
		}

		result.setNativeCodeSpecification(readNativeCode(in));

		@SuppressWarnings("rawtypes")
		Map raw = readMap(in);
		result.setStateWires(raw);

		result.setFullyLoaded(true); // set fully loaded before setting the dependencies
		// No need to add bundle dependencies for hosts, imports or requires;
		// This is done by readBundleDescription
		return result;
	}

	private BundleSpecificationImpl readBundleSpec(DataInputStream in) throws IOException {
		byte tag = readTag(in);
		if (tag == NULL)
			return null;
		if (tag == INDEX)
			return (BundleSpecificationImpl) getFromObjectTable(in.readInt());
		BundleSpecificationImpl result = new BundleSpecificationImpl();
		int tableIndex = in.readInt();
		addToObjectTable(result, tableIndex);
		readVersionConstraint(result, in);
		result.setSupplier(readBundleDescription(in));
		result.setExported(in.readBoolean());
		result.setOptional(in.readBoolean());
		result.setAttributes(readMap(in));
		result.setArbitraryDirectives(readMap(in));
		return result;
	}

	private ExportPackageDescriptionImpl readExportPackageDesc(DataInputStream in) throws IOException {
		byte tag = readTag(in);
		if (tag == NULL)
			return null;
		if (tag == INDEX)
			return (ExportPackageDescriptionImpl) getFromObjectTable(in.readInt());
		ExportPackageDescriptionImpl exportPackageDesc = new ExportPackageDescriptionImpl();
		int tableIndex = in.readInt();
		addToObjectTable(exportPackageDesc, tableIndex);
		readBaseDescription(exportPackageDesc, in);
		exportPackageDesc.setExporter(readBundleDescription(in));
		exportPackageDesc.setAttributes(readMap(in));
		exportPackageDesc.setDirectives(readMap(in));
		exportPackageDesc.setArbitraryDirectives(readMap(in));
		exportPackageDesc.setFragmentDeclaration(readExportPackageDesc(in));
		return exportPackageDesc;
	}

	private DisabledInfo readDisabledInfo(DataInputStream in) throws IOException {
		return new DisabledInfo(readString(in, false), readString(in, false), readBundleDescription(in));
	}

	private Map<String, Object> readMap(DataInputStream in) throws IOException {
		int count = in.readInt();
		if (count == 0)
			return null;
		HashMap<String, Object> result = new HashMap<String, Object>(count);
		for (int i = 0; i < count; i++) {
			String key = readString(in, false);
			Object value = null;
			byte type = in.readByte();
			if (type == 0)
				value = readString(in, false);
			else if (type == 1)
				value = readList(in);
			else if (type == 2)
				value = in.readBoolean() ? Boolean.TRUE : Boolean.FALSE;
			else if (type == 3)
				value = new Integer(in.readInt());
			else if (type == 4)
				value = new Long(in.readLong());
			else if (type == 5)
				value = new Double(in.readDouble());
			else if (type == 6)
				value = readVersion(in);
			else if (type == 7) {
				value = readString(in, false);
				try {
					Class<?> uriClazz = Class.forName("java.net.URI"); //$NON-NLS-1$
					Constructor<?> constructor = uriClazz.getConstructor(new Class[] {String.class});
					value = constructor.newInstance(new Object[] {value});
				} catch (ClassNotFoundException e) {
					// oh well cannot support; just use the string
				} catch (RuntimeException e) { // got some reflection exception
					throw e;
				} catch (Exception e) {
					throw new RuntimeException(e.getMessage(), e);
				}
			} else if (type == 8) {
				int listType = in.readByte();
				int size = in.readInt();
				List<Object> list = new ArrayList<Object>(size);
				for (int j = 0; j < size; j++) {
					switch (listType) {
						case 0 :
							list.add(readString(in, false));
							break;
						case 3 :
							list.add(new Integer(in.readInt()));
							break;
						case 4 :
							list.add(new Long(in.readLong()));
							break;
						case 5 :
							list.add(new Double(in.readDouble()));
							break;
						case 6 :
							list.add(readVersion(in));
							break;
						case 7 :
							list.add(readStateWire(in));
							break;
						default :
							throw new IOException("Invalid type: " + listType); //$NON-NLS-1$
					}
				}
				value = list;
			}
			result.put(key, value);
		}
		return result;
	}

	private Object readStateWire(DataInputStream in) throws IOException {
		VersionConstraintImpl requirement;
		BundleDescription requirementHost;
		BaseDescription capability;
		BundleDescription capabilityHost;

		byte wireType = in.readByte();
		switch (wireType) {
			case 0 :
				requirement = readImportPackageSpec(in);
				capability = readExportPackageDesc(in);
				break;
			case 1 :
				requirement = readBundleSpec(in);
				capability = readBundleDescription(in);
				break;
			case 2 :
				requirement = readHostSpec(in);
				capability = readBundleDescription(in);
				break;
			case 3 :
				requirement = readGenericSpecification(in);
				capability = readGenericDescription(in);
				break;
			default :
				throw new IOException("Invalid wire type: " + wireType); //$NON-NLS-1$
		}

		requirementHost = readBundleDescription(in);
		capabilityHost = readBundleDescription(in);

		if (requirement.getBundle() == null) {
			// Need to fix up dynamic imports added by weaving hook (bug 359394)
			requirement.setBundle(requirementHost);
		}
		return new StateWire(requirementHost, requirement, capabilityHost, capability);
	}

	private String[] readList(DataInputStream in) throws IOException {
		int count = in.readInt();
		if (count == 0)
			return null;
		String[] result = new String[count];
		for (int i = 0; i < count; i++)
			result[i] = readString(in, false);
		return result;
	}

	private void readBaseDescription(BaseDescriptionImpl root, DataInputStream in) throws IOException {
		root.setName(readString(in, false));
		root.setVersion(readVersion(in));
	}

	private ImportPackageSpecificationImpl readImportPackageSpec(DataInputStream in) throws IOException {
		byte tag = readTag(in);
		if (tag == NULL)
			return null;
		if (tag == INDEX)
			return (ImportPackageSpecificationImpl) getFromObjectTable(in.readInt());
		ImportPackageSpecificationImpl result = new ImportPackageSpecificationImpl();
		int tableIndex = in.readInt();
		addToObjectTable(result, tableIndex);
		readVersionConstraint(result, in);
		result.setSupplier(readExportPackageDesc(in));
		result.setBundleSymbolicName(readString(in, false));
		result.setBundleVersionRange(readVersionRange(in));
		result.setAttributes(readMap(in));
		result.setDirectives(readMap(in));
		result.setArbitraryDirectives(readMap(in));
		return result;
	}

	private HostSpecificationImpl readHostSpec(DataInputStream in) throws IOException {
		byte tag = readTag(in);
		if (tag == NULL)
			return null;
		if (tag == INDEX)
			return (HostSpecificationImpl) getFromObjectTable(in.readInt());
		HostSpecificationImpl result = new HostSpecificationImpl();
		int tableIndex = in.readInt();
		addToObjectTable(result, tableIndex);
		readVersionConstraint(result, in);
		int hostCount = in.readInt();
		if (hostCount > 0) {
			BundleDescription[] hosts = new BundleDescription[hostCount];
			for (int i = 0; i < hosts.length; i++)
				hosts[i] = readBundleDescription(in);
			result.setHosts(hosts);
		}
		result.setAttributes(readMap(in));
		result.setArbitraryDirectives(readMap(in));
		return result;
	}

	private GenericDescription readGenericDescription(DataInputStream in) throws IOException {
		byte tag = readTag(in);
		if (tag == NULL)
			return null;
		if (tag == INDEX)
			return (GenericDescription) getFromObjectTable(in.readInt());
		int tableIndex = in.readInt();
		GenericDescriptionImpl result = new GenericDescriptionImpl();
		addToObjectTable(result, tableIndex);
		readBaseDescription(result, in);
		result.setSupplier(readBundleDescription(in));
		result.setType(readString(in, false));
		Map<String, Object> mapAttrs = readMap(in);
		Dictionary<String, Object> attrs = new Hashtable<String, Object>();
		if (mapAttrs != null) {
			for (Iterator<String> keys = mapAttrs.keySet().iterator(); keys.hasNext();) {
				String key = keys.next();
				attrs.put(key, mapAttrs.get(key));
			}
		}
		result.setAttributes(attrs);
		Map directives = readMap(in);
		if (directives != null)
			result.setDirectives(directives);
		result.setFragmentDeclaration(readGenericDescription(in));
		return result;
	}

	private GenericSpecificationImpl readGenericSpecification(DataInputStream in) throws IOException {
		byte tag = readTag(in);
		if (tag == NULL)
			return null;
		if (tag == INDEX)
			return (GenericSpecificationImpl) getFromObjectTable(in.readInt());
		GenericSpecificationImpl result = new GenericSpecificationImpl();
		int tableIndex = in.readInt();
		addToObjectTable(result, tableIndex);
		readVersionConstraint(result, in);
		result.setType(readString(in, false));
		int num = in.readInt();
		GenericDescription[] suppliers = num == 0 ? null : new GenericDescription[num];
		for (int i = 0; i < num; i++)
			suppliers[i] = readGenericDescription(in);
		result.setSupplers(suppliers);
		result.setResolution(in.readInt());
		try {
			result.setMatchingFilter(readString(in, false), false);
		} catch (InvalidSyntaxException e) {
			// do nothing this filter was tested before
		}
		result.setAttributes(readMap(in));
		result.setArbitraryDirectives(readMap(in));
		return result;
	}

	private NativeCodeSpecification readNativeCode(DataInputStream in) throws IOException {
		if (!in.readBoolean())
			return null;
		NativeCodeSpecificationImpl result = new NativeCodeSpecificationImpl();
		result.setOptional(in.readBoolean());
		int numNativeDesc = in.readInt();
		NativeCodeDescriptionImpl[] nativeDescs = new NativeCodeDescriptionImpl[numNativeDesc];
		for (int i = 0; i < numNativeDesc; i++)
			nativeDescs[i] = readNativeCodeDescription(in);
		result.setPossibleSuppliers(nativeDescs);
		int supplierIndex = in.readInt();
		if (supplierIndex >= 0)
			result.setSupplier(nativeDescs[supplierIndex]);
		return result;
	}

	private NativeCodeDescriptionImpl readNativeCodeDescription(DataInputStream in) throws IOException {
		NativeCodeDescriptionImpl result = new NativeCodeDescriptionImpl();
		readBaseDescription(result, in);
		result.setSupplier(readBundleDescription(in));
		try {
			result.setFilter(readString(in, false));
		} catch (InvalidSyntaxException e) {
			// do nothing, this filter was tested before
		}
		result.setLanguages(readStringArray(in));
		result.setNativePaths(readStringArray(in));
		result.setOSNames(readStringArray(in));
		result.setOSVersions(readVersionRanges(in));
		result.setProcessors(readStringArray(in));
		result.setInvalidNativePaths(in.readBoolean());
		return result;
	}

	private VersionRange[] readVersionRanges(DataInputStream in) throws IOException {
		int num = in.readInt();
		if (num == 0)
			return null;
		VersionRange[] result = new VersionRange[num];
		for (int i = 0; i < num; i++)
			result[i] = readVersionRange(in);
		return result;
	}

	private String[] readStringArray(DataInputStream in) throws IOException {
		int num = in.readInt();
		if (num == 0)
			return null;
		String[] result = new String[num];
		for (int i = 0; i < num; i++)
			result[i] = readString(in, false);
		return result;
	}

	// called by readers for VersionConstraintImpl subclasses
	private void readVersionConstraint(VersionConstraintImpl version, DataInputStream in) throws IOException {
		version.setName(readString(in, false));
		version.setVersionRange(readVersionRange(in));
	}

	private Version readVersion(DataInputStream in) throws IOException {
		byte tag = readTag(in);
		if (tag == NULL)
			return Version.emptyVersion;
		int majorComponent = in.readInt();
		int minorComponent = in.readInt();
		int serviceComponent = in.readInt();
		String qualifierComponent = readString(in, false);
		Version result = (Version) ObjectPool.intern(new Version(majorComponent, minorComponent, serviceComponent, qualifierComponent));
		//Version result = new Version(majorComponent, minorComponent, serviceComponent, qualifierComponent);
		return result;
	}

	private VersionRange readVersionRange(DataInputStream in) throws IOException {
		byte tag = readTag(in);
		if (tag == NULL)
			return null;
		return new VersionRange(readVersion(in), in.readBoolean(), readVersion(in), in.readBoolean());
	}

	/**
	 * expectedTimestamp is the expected value for the timestamp. or -1, if
	 * 	no checking should be performed 
	 */
	public synchronized boolean loadStateDeprecated(StateImpl state, DataInputStream input, long expectedTimestamp) throws IOException {
		try {
			return readStateDeprecated(state, input, expectedTimestamp);
		} finally {
			input.close();
		}
	}

	/**
	 * expectedTimestamp is the expected value for the timestamp. or -1, if
	 * 	no checking should be performed 
	 */
	public synchronized boolean loadState(StateImpl state, long expectedTimestamp) throws IOException {
		return readState(state, expectedTimestamp);
	}

	private String readString(DataInputStream in, boolean intern) throws IOException {
		byte type = in.readByte();
		if (type == NULL)
			return null;

		if (type == LONG_STRING) {
			int length = in.readInt();
			byte[] data = new byte[length];
			in.readFully(data);
			String string = new String(data, UTF_8);

			if (intern)
				return string.intern();
			return (String) ObjectPool.intern(string);
		}

		if (intern)
			return in.readUTF().intern();
		return (String) ObjectPool.intern(in.readUTF());
	}

	private byte readTag(DataInputStream in) throws IOException {
		return in.readByte();
	}

	private DataInputStream openLazyFile() throws IOException {
		if (lazyFile == null)
			throw new IOException(); // TODO error message here!
		return new DataInputStream(new BufferedInputStream(secureAction.getFileInputStream(lazyFile), BUFFER_SIZE_LAZY));
	}

	boolean isLazyLoaded() {
		return lazyLoad;
	}

	boolean getAccessedFlag() {
		return accessedFlag;
	}

	void setAccessedFlag(boolean accessedFlag) {
		this.accessedFlag = accessedFlag;
	}

	void fullyLoad() {
		setAccessedFlag(true);
		DataInputStream in = null;
		try {
			in = openLazyFile();
			for (int i = 0; i < numBundles; i++)
				readBundleDescriptionLazyData(in, 0);
		} catch (IOException ioe) {
			throw new RuntimeException(ioe.getMessage(), ioe); // TODO need error message here
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					// nothing we can do now
				}
		}
	}

	void fullyLoad(BundleDescriptionImpl target) throws IOException {
		setAccessedFlag(true);
		DataInputStream in = null;
		try {
			in = openLazyFile();
			// get the set of bundles that must be loaded according to dependencies
			List<BundleDescriptionImpl> toLoad = new ArrayList<BundleDescriptionImpl>();
			addDependencies(target, toLoad);
			int skipBytes[] = getSkipBytes(toLoad);
			// look for the lazy data of the toLoad list
			for (int i = 0; i < skipBytes.length; i++)
				readBundleDescriptionLazyData(in, skipBytes[i]);
		} finally {
			if (in != null)
				in.close();
		}
	}

	private void addDependencies(BundleDescriptionImpl target, List<BundleDescriptionImpl> toLoad) {
		if (toLoad.contains(target) || target.isFullyLoaded())
			return;
		Iterator<BundleDescriptionImpl> load = toLoad.iterator();
		int i = 0;
		while (load.hasNext()) {
			// insert the target into the list sorted by lazy data offsets
			BundleDescriptionImpl bundle = load.next();
			if (target.getLazyDataOffset() < bundle.getLazyDataOffset())
				break;
			i++;
		}
		if (i >= toLoad.size())
			toLoad.add(target);
		else
			toLoad.add(i, target);
		List<BundleDescription> deps = target.getBundleDependencies();
		for (Iterator<BundleDescription> iter = deps.iterator(); iter.hasNext();)
			addDependencies((BundleDescriptionImpl) iter.next(), toLoad);
	}

	private int[] getSkipBytes(List<BundleDescriptionImpl> toLoad) {
		int[] skipBytes = new int[toLoad.size()];
		for (int i = 0; i < skipBytes.length; i++) {
			BundleDescriptionImpl current = toLoad.get(i);
			if (i == 0) {
				skipBytes[i] = current.getLazyDataOffset();
				continue;
			}
			BundleDescriptionImpl previous = toLoad.get(i - 1);
			skipBytes[i] = current.getLazyDataOffset() - previous.getLazyDataOffset() - previous.getLazyDataSize();
		}
		return skipBytes;
	}

	void flushLazyObjectCache() {
		for (Iterator<Entry<Integer, Object>> entries = objectTable.entrySet().iterator(); entries.hasNext();) {
			Map.Entry<Integer, Object> entry = entries.next();
			Object value = entry.getValue();
			if (value instanceof ExportPackageDescription || value instanceof GenericDescription || value instanceof ImportPackageSpecification || value instanceof BundleSpecification || value instanceof GenericSpecification)
				entries.remove();
		}
	}
}
