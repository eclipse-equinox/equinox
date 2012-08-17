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
import java.util.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.*;

/**
 * This class is <strong>not</strong> thread safe. Instances must not be
 * shared across multiple threads.
 */
class StateWriter {

	// objectTable will be a hashmap of objects. The objects will be things
	// like BundleDescription, ExportPackageDescription, Version etc.. The integer
	// index value will be used in the cache to allow cross-references in the
	// cached state.
	private final Map<Object, Integer> objectTable = new HashMap<Object, Integer>();

	private final List<Object> forcedWrite = new ArrayList<Object>();

	private int addToObjectTable(Object object) {
		Integer cur = objectTable.get(object);
		if (cur != null)
			return cur.intValue();
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
		int index = addToObjectTable(object);
		out.writeByte(StateReader.OBJECT);
		out.writeInt(index);
		return false;
	}

	private void writeStateDeprecated(StateImpl state, DataOutputStream out) throws IOException {
		out.write(StateReader.STATE_CACHE_VERSION);
		if (writePrefix(state, out))
			return;
		out.writeLong(state.getTimeStamp());
		// write the platform property keys
		String[] platformPropKeys = state.getPlatformPropertyKeys();
		writePlatformProp(platformPropKeys, out);
		Dictionary<Object, Object>[] propSet = state.getPlatformProperties();
		out.writeInt(propSet.length);
		for (int i = 0; i < propSet.length; i++) {
			Dictionary<Object, Object> props = propSet[i];
			out.writeInt(platformPropKeys.length);
			for (int j = 0; j < platformPropKeys.length; j++)
				writePlatformProp(props.get(platformPropKeys[j]), out);
		}
		BundleDescription[] bundles = state.getBundles();
		StateHelperImpl.getInstance().sortBundles(bundles);
		out.writeInt(bundles.length);
		if (bundles.length == 0)
			return;
		for (int i = 0; i < bundles.length; i++)
			writeBundleDescription(bundles[i], out, false);
		out.writeBoolean(state.isResolved());
		// save the lazy data offset
		out.writeInt(out.size());
		for (int i = 0; i < bundles.length; i++)
			writeBundleDescriptionLazyData(bundles[i], out);
	}

	public void saveState(StateImpl state, File stateFile, File lazyFile) throws IOException {
		DataOutputStream outLazy = null;
		DataOutputStream outState = null;
		FileOutputStream fosLazy = null;
		FileOutputStream fosState = null;
		synchronized (state.monitor) {
			try {
				BundleDescription[] bundles = state.getBundles();
				StateHelperImpl.getInstance().sortBundles(bundles);
				// need to prime the object table with all bundles
				// this allows us to write only indexes to bundles in the lazy data
				for (int i = 0; i < bundles.length; i++) {
					addToObjectTable(bundles[i]);
					if (bundles[i].getHost() != null)
						addToObjectTable(bundles[i].getHost());
				}
				// first write the lazy data to get the offsets and sizes to the lazy data
				fosLazy = new FileOutputStream(lazyFile);
				outLazy = new DataOutputStream(new BufferedOutputStream(fosLazy));
				for (int i = 0; i < bundles.length; i++)
					writeBundleDescriptionLazyData(bundles[i], outLazy);
				// now write the state data
				fosState = new FileOutputStream(stateFile);
				outState = new DataOutputStream(new BufferedOutputStream(fosState));
				outState.write(StateReader.STATE_CACHE_VERSION);
				if (writePrefix(state, outState))
					return;
				outState.writeLong(state.getTimeStamp());
				// write the platform property keys
				String[] platformPropKeys = state.getPlatformPropertyKeys();
				writePlatformProp(platformPropKeys, outState);
				// write the platform property values
				Dictionary<Object, Object>[] propSet = state.getPlatformProperties();
				outState.writeInt(propSet.length);
				for (int i = 0; i < propSet.length; i++) {
					Dictionary<Object, Object> props = propSet[i];
					outState.writeInt(platformPropKeys.length);
					for (int j = 0; j < platformPropKeys.length; j++)
						writePlatformProp(props.get(platformPropKeys[j]), outState);
				}
				outState.writeInt(bundles.length);
				for (int i = 0; i < bundles.length; i++)
					// write out each bundle with the force flag set to make sure
					// the data is written at least once in the non-lazy state data
					writeBundleDescription(bundles[i], outState, true);
				// write the DisabledInfos
				DisabledInfo[] infos = state.getDisabledInfos();
				outState.writeInt(infos.length);
				for (int i = 0; i < infos.length; i++)
					writeDisabledInfo(infos[i], outState);
				outState.writeBoolean(state.isResolved());
			} finally {
				if (outLazy != null) {
					try {
						outLazy.flush();
						fosLazy.getFD().sync();
					} catch (IOException e) {
						// do nothing, we tried
					}
					try {
						outLazy.close();
					} catch (IOException e) {
						// do nothing
					}
				}
				if (outState != null) {
					try {
						outState.flush();
						fosState.getFD().sync();
					} catch (IOException e) {
						// do nothing, we tried
					}
					try {
						outState.close();
					} catch (IOException e) {
						// do nothing
					}
				}
			}
		}
	}

	private void writePlatformProp(Object obj, DataOutputStream out) throws IOException {
		if (!(obj instanceof String) && !(obj instanceof String[]))
			out.writeByte(StateReader.NULL);
		else {
			out.writeByte(StateReader.OBJECT);
			if (obj instanceof String) {
				out.writeInt(1);
				writeStringOrNull((String) obj, out);
			} else {
				String[] props = (String[]) obj;
				out.writeInt(props.length);
				for (int i = 0; i < props.length; i++)
					writeStringOrNull(props[i], out);
			}
		}
	}

	/*
	 * The force flag is used when writing the non-lazy state data.  This forces the data to be
	 * written once even if the object exists in the object table.
	 * This is needed because we want to write the lazy data first but we only want
	 * to include indexes to the actual bundles in the lazy data.  To do this we
	 * prime the object table with all the bundles first.  Then we write the
	 * lazy data.  Finally we write the non-lazy data and force a write of the
	 * bundles data once even if the bundle is in the object table.
	 */
	private void writeBundleDescription(BundleDescription bundle, DataOutputStream out, boolean force) throws IOException {
		if (force && !forcedWrite.contains(bundle)) {
			int index = addToObjectTable(bundle);
			out.writeByte(StateReader.OBJECT);
			out.writeInt(index);
			forcedWrite.add(bundle);
		} else if (writePrefix(bundle, out))
			return;
		// first write out non-lazy loaded data
		out.writeLong(bundle.getBundleId()); // ID must be the first thing
		writeBaseDescription(bundle, out);
		out.writeInt(((BundleDescriptionImpl) bundle).getLazyDataOffset());
		out.writeInt(((BundleDescriptionImpl) bundle).getLazyDataSize());
		out.writeBoolean(bundle.isResolved());
		out.writeBoolean(bundle.isSingleton());
		out.writeBoolean(bundle.hasDynamicImports());
		out.writeBoolean(bundle.attachFragments());
		out.writeBoolean(bundle.dynamicFragments());
		writeList(out, (String[]) ((BundleDescriptionImpl) bundle).getDirective(Constants.MANDATORY_DIRECTIVE));
		writeMap(out, bundle.getAttributes());
		writeMap(out, ((BundleDescriptionImpl) bundle).getArbitraryDirectives());
		writeHostSpec((HostSpecificationImpl) bundle.getHost(), out, force);

		List<BundleDescription> dependencies = ((BundleDescriptionImpl) bundle).getBundleDependencies();
		out.writeInt(dependencies.size());
		for (Iterator<BundleDescription> iter = dependencies.iterator(); iter.hasNext();)
			writeBundleDescription(iter.next(), out, force);
		// the rest is lazy loaded data
	}

	private void writeBundleDescriptionLazyData(BundleDescription bundle, DataOutputStream out) throws IOException {
		int dataStart = out.size(); // save the offset of lazy data start
		int index = getFromObjectTable(bundle);
		((BundleDescriptionImpl) bundle).setLazyDataOffset(out.size());
		out.writeInt(index);

		writeStringOrNull(bundle.getLocation(), out);
		writeStringOrNull(bundle.getPlatformFilter(), out);

		ExportPackageDescription[] exports = bundle.getExportPackages();
		out.writeInt(exports.length);
		for (int i = 0; i < exports.length; i++)
			writeExportPackageDesc((ExportPackageDescriptionImpl) exports[i], out);

		ImportPackageSpecification[] imports = bundle.getImportPackages();
		out.writeInt(imports.length);
		for (int i = 0; i < imports.length; i++)
			writeImportPackageSpec((ImportPackageSpecificationImpl) imports[i], out);

		BundleSpecification[] requiredBundles = bundle.getRequiredBundles();
		out.writeInt(requiredBundles.length);
		for (int i = 0; i < requiredBundles.length; i++)
			writeBundleSpec((BundleSpecificationImpl) requiredBundles[i], out);

		ExportPackageDescription[] selectedExports = bundle.getSelectedExports();
		if (selectedExports == null) {
			out.writeInt(0);
		} else {
			out.writeInt(selectedExports.length);
			for (int i = 0; i < selectedExports.length; i++)
				writeExportPackageDesc((ExportPackageDescriptionImpl) selectedExports[i], out);
		}

		ExportPackageDescription[] substitutedExports = bundle.getSubstitutedExports();
		if (substitutedExports == null) {
			out.writeInt(0);
		} else {
			out.writeInt(substitutedExports.length);
			for (int i = 0; i < substitutedExports.length; i++)
				writeExportPackageDesc((ExportPackageDescriptionImpl) substitutedExports[i], out);
		}

		ExportPackageDescription[] resolvedImports = bundle.getResolvedImports();
		if (resolvedImports == null) {
			out.writeInt(0);
		} else {
			out.writeInt(resolvedImports.length);
			for (int i = 0; i < resolvedImports.length; i++)
				writeExportPackageDesc((ExportPackageDescriptionImpl) resolvedImports[i], out);
		}

		BundleDescription[] resolvedRequires = bundle.getResolvedRequires();
		if (resolvedRequires == null) {
			out.writeInt(0);
		} else {
			out.writeInt(resolvedRequires.length);
			for (int i = 0; i < resolvedRequires.length; i++)
				writeBundleDescription(resolvedRequires[i], out, false);
		}

		String[] ees = bundle.getExecutionEnvironments();
		out.writeInt(ees.length);
		for (int i = 0; i < ees.length; i++)
			writeStringOrNull(ees[i], out);

		Map<String, Long> dynamicStamps = ((BundleDescriptionImpl) bundle).getDynamicStamps();
		if (dynamicStamps == null)
			out.writeInt(0);
		else {
			out.writeInt(dynamicStamps.size());
			for (Iterator<String> pkgs = dynamicStamps.keySet().iterator(); pkgs.hasNext();) {
				String pkg = pkgs.next();
				writeStringOrNull(pkg, out);
				out.writeLong(dynamicStamps.get(pkg).longValue());
			}
		}

		GenericDescription[] genericCapabilities = bundle.getGenericCapabilities();
		if (genericCapabilities == null)
			out.writeInt(0);
		else {
			out.writeInt(genericCapabilities.length);
			for (int i = 0; i < genericCapabilities.length; i++)
				writeGenericDescription(genericCapabilities[i], out);
		}

		GenericSpecification[] genericRequires = bundle.getGenericRequires();
		if (genericRequires == null)
			out.writeInt(0);
		else {
			out.writeInt(genericRequires.length);
			for (int i = 0; i < genericRequires.length; i++)
				writeGenericSpecification((GenericSpecificationImpl) genericRequires[i], out);
		}

		GenericDescription[] selectedCapabilities = bundle.getSelectedGenericCapabilities();
		if (selectedCapabilities == null)
			out.writeInt(0);
		else {
			out.writeInt(selectedCapabilities.length);
			for (int i = 0; i < selectedCapabilities.length; i++)
				writeGenericDescription(selectedCapabilities[i], out);
		}

		GenericDescription[] resolvedCapabilities = bundle.getResolvedGenericRequires();
		if (resolvedCapabilities == null)
			out.writeInt(0);
		else {
			out.writeInt(resolvedCapabilities.length);
			for (int i = 0; i < resolvedCapabilities.length; i++)
				writeGenericDescription(resolvedCapabilities[i], out);
		}

		writeNativeCode(bundle.getNativeCodeSpecification(), out);

		writeMap(out, ((BundleDescriptionImpl) bundle).getWiresInternal());
		// save the size of the lazy data
		((BundleDescriptionImpl) bundle).setLazyDataSize(out.size() - dataStart);
	}

	private void writeDisabledInfo(DisabledInfo disabledInfo, DataOutputStream out) throws IOException {
		writeStringOrNull(disabledInfo.getPolicyName(), out);
		writeStringOrNull(disabledInfo.getMessage(), out);
		writeBundleDescription(disabledInfo.getBundle(), out, false);
	}

	private void writeBundleSpec(BundleSpecificationImpl bundle, DataOutputStream out) throws IOException {
		if (writePrefix(bundle, out))
			return;
		writeVersionConstraint(bundle, out);
		writeBundleDescription((BundleDescription) bundle.getSupplier(), out, false);
		out.writeBoolean(bundle.isExported());
		out.writeBoolean(bundle.isOptional());
		writeMap(out, bundle.getAttributes());
		writeMap(out, bundle.getArbitraryDirectives());
	}

	private void writeExportPackageDesc(ExportPackageDescriptionImpl exportPackageDesc, DataOutputStream out) throws IOException {
		if (writePrefix(exportPackageDesc, out))
			return;
		writeBaseDescription(exportPackageDesc, out);
		writeBundleDescription(exportPackageDesc.getExporter(), out, false);
		writeMap(out, exportPackageDesc.getAttributes());
		writeMap(out, exportPackageDesc.getDirectives());
		writeMap(out, exportPackageDesc.getArbitraryDirectives());
		writeExportPackageDesc((ExportPackageDescriptionImpl) exportPackageDesc.getFragmentDeclaration(), out);
	}

	private void writeGenericDescription(GenericDescription description, DataOutputStream out) throws IOException {
		if (writePrefix(description, out))
			return;
		writeBaseDescription(description, out);
		writeBundleDescription(description.getSupplier(), out, false);
		writeStringOrNull(description.getType() == GenericDescription.DEFAULT_TYPE ? null : description.getType(), out);
		Dictionary<String, Object> attrs = description.getAttributes();
		Map<String, Object> mapAttrs = new HashMap<String, Object>(attrs.size());
		for (Enumeration<String> keys = attrs.keys(); keys.hasMoreElements();) {
			String key = keys.nextElement();
			mapAttrs.put(key, attrs.get(key));
		}
		writeMap(out, mapAttrs);
		Map<String, String> directives = description.getDeclaredDirectives();
		writeMap(out, directives);
		writeGenericDescription((GenericDescription) ((BaseDescriptionImpl) description).getFragmentDeclaration(), out);
	}

	private void writeGenericSpecification(GenericSpecificationImpl specification, DataOutputStream out) throws IOException {
		if (writePrefix(specification, out))
			return;
		writeVersionConstraint(specification, out);
		writeStringOrNull(specification.getType() == GenericDescription.DEFAULT_TYPE ? null : specification.getType(), out);
		GenericDescription[] suppliers = specification.getSuppliers();
		out.writeInt(suppliers == null ? 0 : suppliers.length);
		if (suppliers != null)
			for (int i = 0; i < suppliers.length; i++)
				writeGenericDescription(suppliers[i], out);
		out.writeInt(specification.getResolution());
		writeStringOrNull(specification.getMatchingFilter(), out);
		writeMap(out, specification.getAttributes());
		writeMap(out, specification.getArbitraryDirectives());
	}

	private void writeNativeCode(NativeCodeSpecification nativeCodeSpecification, DataOutputStream out) throws IOException {
		if (nativeCodeSpecification == null) {
			out.writeBoolean(false);
			return;
		}
		out.writeBoolean(true);
		out.writeBoolean(nativeCodeSpecification.isOptional());
		NativeCodeDescription[] nativeDescs = nativeCodeSpecification.getPossibleSuppliers();
		int numDescs = nativeDescs == null ? 0 : nativeDescs.length;
		out.writeInt(numDescs);
		int supplierIndex = -1;
		for (int i = 0; i < numDescs; i++) {
			if (nativeDescs[i] == nativeCodeSpecification.getSupplier())
				supplierIndex = i;
			writeNativeCodeDescription(nativeDescs[i], out);
		}
		out.writeInt(supplierIndex);
	}

	private void writeNativeCodeDescription(NativeCodeDescription nativeCodeDescription, DataOutputStream out) throws IOException {
		writeBaseDescription(nativeCodeDescription, out);
		writeBundleDescription(nativeCodeDescription.getSupplier(), out, false);
		Filter filter = nativeCodeDescription.getFilter();
		writeStringOrNull(filter == null ? null : filter.toString(), out);
		writeStringArray(nativeCodeDescription.getLanguages(), out);
		writeStringArray(nativeCodeDescription.getNativePaths(), out);
		writeStringArray(nativeCodeDescription.getOSNames(), out);
		writeVersionRanges(nativeCodeDescription.getOSVersions(), out);
		writeStringArray(nativeCodeDescription.getProcessors(), out);
		out.writeBoolean(nativeCodeDescription.hasInvalidNativePaths());
	}

	private void writeVersionRanges(VersionRange[] ranges, DataOutputStream out) throws IOException {
		out.writeInt(ranges == null ? 0 : ranges.length);
		if (ranges == null)
			return;
		for (int i = 0; i < ranges.length; i++)
			writeVersionRange(ranges[i], out);
	}

	private void writeStringArray(String[] strings, DataOutputStream out) throws IOException {
		out.writeInt(strings == null ? 0 : strings.length);
		if (strings == null)
			return;
		for (int i = 0; i < strings.length; i++)
			writeStringOrNull(strings[i], out);
	}

	private void writeMap(DataOutputStream out, Map<String, ?> source) throws IOException {
		if (source == null) {
			out.writeInt(0);
		} else {
			out.writeInt(source.size());
			Iterator<String> iter = source.keySet().iterator();
			while (iter.hasNext()) {
				String key = iter.next();
				Object value = source.get(key);
				writeStringOrNull(key, out);
				if (value instanceof String) {
					out.writeByte(0);
					writeStringOrNull((String) value, out);
				} else if (value instanceof String[]) {
					out.writeByte(1);
					writeList(out, (String[]) value);
				} else if (value instanceof Boolean) {
					out.writeByte(2);
					out.writeBoolean(((Boolean) value).booleanValue());
				} else if (value instanceof Integer) {
					out.writeByte(3);
					out.writeInt(((Integer) value).intValue());
				} else if (value instanceof Long) {
					out.writeByte(4);
					out.writeLong(((Long) value).longValue());
				} else if (value instanceof Double) {
					out.writeByte(5);
					out.writeDouble(((Double) value).doubleValue());
				} else if (value instanceof Version) {
					out.writeByte(6);
					writeVersion((Version) value, out);
				} else if ("java.net.URI".equals(value.getClass().getName())) { //$NON-NLS-1$
					out.writeByte(7);
					writeStringOrNull(value.toString(), out);
				} else if (value instanceof List) {
					writeList(out, (List<?>) value);
				}
			}
		}
	}

	private void writeList(DataOutputStream out, List<?> list) throws IOException {
		byte type = getListType(list);
		if (type == -2)
			return; // don't understand the list type
		out.writeByte(8);
		out.writeByte(type);
		out.writeInt(list.size());
		for (Object value : list) {
			switch (type) {
				case 0 :
					writeStringOrNull((String) value, out);
					break;
				case 3 :
					out.writeInt(((Integer) value).intValue());
					break;
				case 4 :
					out.writeLong(((Long) value).longValue());
					break;
				case 5 :
					out.writeDouble(((Double) value).doubleValue());
					break;
				case 6 :
					writeVersion((Version) value, out);
					break;
				case 7 :
					writeStateWire((StateWire) value, out);
				default :
					break;
			}
		}
	}

	private void writeStateWire(StateWire wire, DataOutputStream out) throws IOException {
		VersionConstraint requirement = wire.getDeclaredRequirement();
		if (requirement instanceof ImportPackageSpecificationImpl) {
			out.writeByte(0);
			writeImportPackageSpec((ImportPackageSpecificationImpl) requirement, out);
		} else if (requirement instanceof BundleSpecificationImpl) {
			out.writeByte(1);
			writeBundleSpec((BundleSpecificationImpl) requirement, out);
		} else if (requirement instanceof HostSpecificationImpl) {
			out.writeByte(2);
			writeHostSpec((HostSpecificationImpl) requirement, out, false);
		} else if (requirement instanceof GenericSpecificationImpl) {
			out.writeByte(3);
			writeGenericSpecification((GenericSpecificationImpl) requirement, out);
		} else
			throw new IllegalArgumentException("Unknown requiement type: " + requirement.getClass());

		BaseDescription capability = wire.getDeclaredCapability();
		if (capability instanceof BundleDescription)
			writeBundleDescription((BundleDescription) capability, out, false);
		else if (capability instanceof ExportPackageDescriptionImpl)
			writeExportPackageDesc((ExportPackageDescriptionImpl) capability, out);
		else if (capability instanceof GenericDescription)
			writeGenericDescription((GenericDescription) capability, out);
		else
			throw new IllegalArgumentException("Unknown capability type: " + requirement.getClass());

		writeBundleDescription(wire.getRequirementHost(), out, false);
		writeBundleDescription(wire.getCapabilityHost(), out, false);
	}

	private byte getListType(List<?> list) {
		if (list.size() == 0)
			return -1;
		Object type = list.get(0);
		if (type instanceof String)
			return 0;
		if (type instanceof Integer)
			return 3;
		if (type instanceof Long)
			return 4;
		if (type instanceof Double)
			return 5;
		if (type instanceof Version)
			return 6;
		if (type instanceof StateWire)
			return 7;
		return -2;
	}

	private void writeList(DataOutputStream out, String[] list) throws IOException {
		if (list == null) {
			out.writeInt(0);
		} else {
			out.writeInt(list.length);
			for (int i = 0; i < list.length; i++)
				writeStringOrNull(list[i], out);
		}
	}

	private void writeBaseDescription(BaseDescription rootDesc, DataOutputStream out) throws IOException {
		writeStringOrNull(rootDesc.getName(), out);
		writeVersion(rootDesc.getVersion(), out);
	}

	private void writeImportPackageSpec(ImportPackageSpecificationImpl importPackageSpec, DataOutputStream out) throws IOException {
		if (writePrefix(importPackageSpec, out))
			return;
		writeVersionConstraint(importPackageSpec, out);
		// TODO this is a hack until the state dynamic loading is cleaned up
		// we should only write the supplier if we are resolved
		if (importPackageSpec.getBundle().isResolved())
			writeExportPackageDesc((ExportPackageDescriptionImpl) importPackageSpec.getSupplier(), out);
		else
			out.writeByte(StateReader.NULL);

		writeStringOrNull(importPackageSpec.getBundleSymbolicName(), out);
		writeVersionRange(importPackageSpec.getBundleVersionRange(), out);
		writeMap(out, importPackageSpec.getAttributes());
		writeMap(out, importPackageSpec.getDirectives());
		writeMap(out, importPackageSpec.getArbitraryDirectives());
	}

	private void writeHostSpec(HostSpecificationImpl host, DataOutputStream out, boolean force) throws IOException {
		if (host != null && force && !forcedWrite.contains(host)) {
			int index = addToObjectTable(host);
			out.writeByte(StateReader.OBJECT);
			out.writeInt(index);
			forcedWrite.add(host);
		} else if (writePrefix(host, out))
			return;
		writeVersionConstraint(host, out);
		BundleDescription[] hosts = host.getHosts();
		if (hosts == null) {
			out.writeInt(0);
			return;
		}
		out.writeInt(hosts.length);
		for (int i = 0; i < hosts.length; i++)
			writeBundleDescription(hosts[i], out, force);
		writeMap(out, host.getAttributes());
		writeMap(out, host.getArbitraryDirectives());
	}

	// called by writers for VersionConstraintImpl subclasses
	private void writeVersionConstraint(VersionConstraint constraint, DataOutputStream out) throws IOException {
		writeStringOrNull(constraint.getName(), out);
		writeVersionRange(constraint.getVersionRange(), out);
	}

	private void writeVersion(Version version, DataOutputStream out) throws IOException {
		if (version == null || version.equals(Version.emptyVersion)) {
			out.writeByte(StateReader.NULL);
			return;
		}
		out.writeByte(StateReader.OBJECT);
		out.writeInt(version.getMajor());
		out.writeInt(version.getMinor());
		out.writeInt(version.getMicro());
		writeQualifier(version.getQualifier(), out);
	}

	private void writeVersionRange(VersionRange versionRange, DataOutputStream out) throws IOException {
		if (versionRange == null || versionRange.equals(VersionRange.emptyRange)) {
			out.writeByte(StateReader.NULL);
			return;
		}
		out.writeByte(StateReader.OBJECT);
		writeVersion(versionRange.getMinimum(), out);
		out.writeBoolean(versionRange.getIncludeMinimum());
		writeVersion(versionRange.getMaximum(), out);
		out.writeBoolean(versionRange.getIncludeMaximum());
	}

	private boolean writeIndex(Object object, DataOutputStream out) throws IOException {
		if (object == null) {
			out.writeByte(StateReader.NULL);
			return true;
		}
		int index = getFromObjectTable(object);
		if (index == -1)
			return false;
		out.writeByte(StateReader.INDEX);
		out.writeInt(index);
		return true;
	}

	public void saveStateDeprecated(StateImpl state, DataOutputStream output) throws IOException {
		try {
			writeStateDeprecated(state, output);
		} finally {
			output.close();
		}
	}

	private void writeStringOrNull(String string, DataOutputStream out) throws IOException {
		if (string == null)
			out.writeByte(StateReader.NULL);
		else {
			byte[] data = string.getBytes(StateReader.UTF_8);

			if (data.length > 65535) {
				out.writeByte(StateReader.LONG_STRING);
				out.writeInt(data.length);
				out.write(data);
			} else {
				out.writeByte(StateReader.OBJECT);
				out.writeUTF(string);
			}
		}
	}

	private void writeQualifier(String string, DataOutputStream out) throws IOException {
		if (string != null && string.length() == 0)
			string = null;
		writeStringOrNull(string, out);
	}
}
