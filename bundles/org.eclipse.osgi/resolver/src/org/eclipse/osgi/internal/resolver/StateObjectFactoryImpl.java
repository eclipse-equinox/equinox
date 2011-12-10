/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.io.*;
import java.util.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.internal.module.ResolverImpl;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.storagemanager.StorageManager;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.*;

public class StateObjectFactoryImpl implements StateObjectFactory {

	/**
	 * @deprecated
	 */
	public BundleDescription createBundleDescription(Dictionary<String, String> manifest, String location, long id) throws BundleException {
		return createBundleDescription(null, manifest, location, id);
	}

	public BundleDescription createBundleDescription(State state, Dictionary<String, String> manifest, String location, long id) throws BundleException {
		BundleDescriptionImpl result = (BundleDescriptionImpl) StateBuilder.createBundleDescription((StateImpl) state, manifest, location);
		result.setBundleId(id);
		return result;
	}

	/**
	 * @deprecated
	 */
	public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, ImportPackageSpecification[] imports, ExportPackageDescription[] exports, String[] providedPackages, boolean singleton) {
		return createBundleDescription(id, symbolicName, version, location, required, host, imports, exports, providedPackages, singleton, true, true, null, null, null, null);
	}

	/**
	 * @deprecated
	 */
	public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, ImportPackageSpecification[] imports, ExportPackageDescription[] exports, String[] providedPackages, boolean singleton, boolean attachFragments, boolean dynamicFragments, String platformFilter, String executionEnvironment, GenericSpecification[] genericRequires, GenericDescription[] genericCapabilities) {
		// bug 154137 we need to parse the executionEnvironment param; no need to check for null, ManifestElement does that for us.
		return createBundleDescription(id, symbolicName, version, location, required, host, imports, exports, singleton, attachFragments, dynamicFragments, platformFilter, ManifestElement.getArrayFromList(executionEnvironment), genericRequires, genericCapabilities);
	}

	public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, ImportPackageSpecification[] imports, ExportPackageDescription[] exports, boolean singleton, boolean attachFragments, boolean dynamicFragments, String platformFilter, String[] executionEnvironments, GenericSpecification[] genericRequires, GenericDescription[] genericCapabilities) {
		return createBundleDescription(id, symbolicName, version, location, required, host, imports, exports, singleton, attachFragments, dynamicFragments, platformFilter, executionEnvironments, genericRequires, genericCapabilities, null);
	}

	public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, ImportPackageSpecification[] imports, ExportPackageDescription[] exports, boolean singleton, boolean attachFragments, boolean dynamicFragments, String platformFilter, String[] executionEnvironments, GenericSpecification[] genericRequires, GenericDescription[] genericCapabilities, NativeCodeSpecification nativeCode) {
		BundleDescriptionImpl bundle = new BundleDescriptionImpl();
		bundle.setBundleId(id);
		bundle.setSymbolicName(symbolicName);
		bundle.setVersion(version);
		bundle.setLocation(location);
		bundle.setRequiredBundles(required);
		bundle.setHost(host);
		bundle.setImportPackages(imports);
		bundle.setExportPackages(exports);
		bundle.setStateBit(BundleDescriptionImpl.SINGLETON, singleton);
		bundle.setStateBit(BundleDescriptionImpl.ATTACH_FRAGMENTS, attachFragments);
		bundle.setStateBit(BundleDescriptionImpl.DYNAMIC_FRAGMENTS, dynamicFragments);
		bundle.setPlatformFilter(platformFilter);
		bundle.setExecutionEnvironments(executionEnvironments);
		bundle.setGenericRequires(genericRequires);
		bundle.setGenericCapabilities(genericCapabilities);
		bundle.setNativeCodeSpecification(nativeCode);
		return bundle;
	}

	public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, ImportPackageSpecification[] imports, ExportPackageDescription[] exports, String platformFilter, String[] executionEnvironments, GenericSpecification[] genericRequires, GenericDescription[] genericCapabilities, NativeCodeSpecification nativeCode) {
		BundleDescriptionImpl bundle = new BundleDescriptionImpl();
		bundle.setBundleId(id);

		try {
			ManifestElement[] symbolicNameElements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
			if (symbolicNameElements.length > 0) {
				ManifestElement bsnElement = symbolicNameElements[0];
				bundle.setSymbolicName(bsnElement.getValue());
				bundle.setStateBit(BundleDescriptionImpl.SINGLETON, "true".equals(bsnElement.getDirective(Constants.SINGLETON_DIRECTIVE))); //$NON-NLS-1$
				String fragmentAttachment = bsnElement.getDirective(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE);
				if (fragmentAttachment != null) {
					if (fragmentAttachment.equals(Constants.FRAGMENT_ATTACHMENT_RESOLVETIME)) {
						bundle.setStateBit(BundleDescriptionImpl.ATTACH_FRAGMENTS, true);
						bundle.setStateBit(BundleDescriptionImpl.DYNAMIC_FRAGMENTS, false);
					} else if (fragmentAttachment.equals(Constants.FRAGMENT_ATTACHMENT_NEVER)) {
						bundle.setStateBit(BundleDescriptionImpl.ATTACH_FRAGMENTS, false);
						bundle.setStateBit(BundleDescriptionImpl.DYNAMIC_FRAGMENTS, false);
					}
				}
				bundle.setDirective(Constants.MANDATORY_DIRECTIVE, ManifestElement.getArrayFromList(bsnElement.getDirective(Constants.MANDATORY_DIRECTIVE)));
				bundle.setAttributes(StateBuilder.getAttributes(bsnElement, StateBuilder.DEFINED_BSN_MATCHING_ATTRS));
				bundle.setArbitraryDirectives(StateBuilder.getDirectives(bsnElement, StateBuilder.DEFINED_BSN_DIRECTIVES));
			}
		} catch (BundleException e) {
			throw (IllegalArgumentException) new IllegalArgumentException("Illegal symbolic name: " + symbolicName).initCause(e); //$NON-NLS-1$
		}

		bundle.setVersion(version);
		bundle.setLocation(location);
		bundle.setRequiredBundles(required);
		bundle.setHost(host);
		bundle.setImportPackages(imports);
		bundle.setExportPackages(exports);
		bundle.setPlatformFilter(platformFilter);
		bundle.setExecutionEnvironments(executionEnvironments);
		bundle.setGenericRequires(genericRequires);
		GenericDescription[] includeIdentity = new GenericDescription[genericCapabilities == null ? 1 : genericCapabilities.length + 1];
		if (genericCapabilities != null)
			System.arraycopy(genericCapabilities, 0, includeIdentity, 1, genericCapabilities.length);
		includeIdentity[0] = StateBuilder.createOsgiIdentityCapability(bundle);
		bundle.setGenericCapabilities(includeIdentity);
		bundle.setNativeCodeSpecification(nativeCode);
		return bundle;
	}

	public BundleDescription createBundleDescription(BundleDescription original) {
		BundleDescriptionImpl bundle = new BundleDescriptionImpl();
		bundle.setBundleId(original.getBundleId());
		bundle.setSymbolicName(original.getSymbolicName());
		bundle.setVersion(original.getVersion());
		bundle.setLocation(original.getLocation());
		BundleSpecification[] originalRequired = original.getRequiredBundles();
		BundleSpecification[] newRequired = new BundleSpecification[originalRequired.length];
		for (int i = 0; i < newRequired.length; i++)
			newRequired[i] = createBundleSpecification(originalRequired[i]);
		bundle.setRequiredBundles(newRequired);
		ExportPackageDescription[] originalExports = original.getExportPackages();
		ExportPackageDescription[] newExports = new ExportPackageDescription[originalExports.length];
		for (int i = 0; i < newExports.length; i++)
			newExports[i] = createExportPackageDescription(originalExports[i]);
		bundle.setExportPackages(newExports);
		ImportPackageSpecification[] originalImports = original.getImportPackages();
		ImportPackageSpecification[] newImports = new ImportPackageSpecification[originalImports.length];
		for (int i = 0; i < newImports.length; i++)
			newImports[i] = createImportPackageSpecification(originalImports[i]);
		bundle.setImportPackages(newImports);
		if (original.getHost() != null)
			bundle.setHost(createHostSpecification(original.getHost()));
		bundle.setStateBit(BundleDescriptionImpl.SINGLETON, original.isSingleton());
		bundle.setStateBit(BundleDescriptionImpl.ATTACH_FRAGMENTS, original.attachFragments());
		bundle.setStateBit(BundleDescriptionImpl.DYNAMIC_FRAGMENTS, original.dynamicFragments());
		bundle.setStateBit(BundleDescriptionImpl.HAS_DYNAMICIMPORT, original.hasDynamicImports());
		bundle.setPlatformFilter(original.getPlatformFilter());
		bundle.setExecutionEnvironments(original.getExecutionEnvironments());
		bundle.setGenericCapabilities(createGenericCapabilities(original.getGenericCapabilities()));
		bundle.setGenericRequires(createGenericRequires(original.getGenericRequires()));
		bundle.setNativeCodeSpecification(createNativeCodeSpecification(original.getNativeCodeSpecification()));
		bundle.setAttributes(original.getAttributes());
		if (original instanceof BundleDescriptionImpl) {
			bundle.setDirective(Constants.MANDATORY_DIRECTIVE, ((BundleDescriptionImpl) original).getDirective(Constants.MANDATORY_DIRECTIVE));
			bundle.setArbitraryDirectives(((BundleDescriptionImpl) original).getArbitraryDirectives());
		}
		return bundle;
	}

	private NativeCodeSpecification createNativeCodeSpecification(NativeCodeSpecification original) {
		if (original == null)
			return null;
		NativeCodeSpecificationImpl result = new NativeCodeSpecificationImpl();
		result.setName(original.getName());
		result.setOptional(original.isOptional());
		NativeCodeDescription[] originalDescriptions = original.getPossibleSuppliers();
		NativeCodeDescriptionImpl[] newDescriptions = new NativeCodeDescriptionImpl[originalDescriptions.length];
		for (int i = 0; i < originalDescriptions.length; i++) {
			newDescriptions[i] = new NativeCodeDescriptionImpl();
			newDescriptions[i].setName(originalDescriptions[i].getName());
			newDescriptions[i].setNativePaths(originalDescriptions[i].getNativePaths());
			newDescriptions[i].setProcessors(originalDescriptions[i].getProcessors());
			newDescriptions[i].setOSNames(originalDescriptions[i].getOSNames());
			newDescriptions[i].setOSVersions(originalDescriptions[i].getOSVersions());
			newDescriptions[i].setLanguages(originalDescriptions[i].getLanguages());
			try {
				newDescriptions[i].setFilter(originalDescriptions[i].getFilter() == null ? null : originalDescriptions[i].getFilter().toString());
			} catch (InvalidSyntaxException e) {
				// this is already tested from the orginal filter
			}
		}
		result.setPossibleSuppliers(newDescriptions);
		return result;
	}

	private GenericDescription[] createGenericCapabilities(GenericDescription[] genericCapabilities) {
		if (genericCapabilities == null || genericCapabilities.length == 0)
			return null;
		GenericDescription[] result = new GenericDescription[genericCapabilities.length];
		for (int i = 0; i < genericCapabilities.length; i++) {
			GenericDescriptionImpl cap = new GenericDescriptionImpl();
			cap.setType(genericCapabilities[i].getType());
			cap.setAttributes(genericCapabilities[i].getAttributes());
			cap.setDirectives(genericCapabilities[i].getDeclaredDirectives());
			result[i] = cap;
		}
		return result;
	}

	private GenericSpecification[] createGenericRequires(GenericSpecification[] genericRequires) {
		if (genericRequires == null || genericRequires.length == 0)
			return null;
		GenericSpecification[] result = new GenericSpecification[genericRequires.length];
		for (int i = 0; i < genericRequires.length; i++) {
			GenericSpecificationImpl req = new GenericSpecificationImpl();
			req.setName(genericRequires[i].getName());
			req.setType(genericRequires[i].getType());
			req.setResolution(req.getResolution());
			try {
				req.setMatchingFilter(genericRequires[i].getMatchingFilter(), false);
			} catch (InvalidSyntaxException e) {
				// do nothing; this filter should already have been tested
			}
			if (genericRequires[i] instanceof GenericSpecificationImpl) {
				req.setAttributes(((GenericSpecificationImpl) genericRequires[i]).getAttributes());
				req.setArbitraryDirectives(((GenericSpecificationImpl) genericRequires[i]).getArbitraryDirectives());
			}
			result[i] = req;
		}
		return result;
	}

	public BundleSpecification createBundleSpecification(String requiredSymbolicName, VersionRange requiredVersionRange, boolean export, boolean optional) {
		BundleSpecificationImpl bundleSpec = new BundleSpecificationImpl();
		bundleSpec.setName(requiredSymbolicName);
		bundleSpec.setVersionRange(requiredVersionRange);
		bundleSpec.setExported(export);
		bundleSpec.setOptional(optional);
		return bundleSpec;
	}

	public BundleSpecification createBundleSpecification(BundleSpecification original) {
		BundleSpecificationImpl bundleSpec = new BundleSpecificationImpl();
		bundleSpec.setName(original.getName());
		bundleSpec.setVersionRange(original.getVersionRange());
		bundleSpec.setExported(original.isExported());
		bundleSpec.setOptional(original.isOptional());
		if (original instanceof BundleSpecificationImpl) {
			bundleSpec.setAttributes(((BundleSpecificationImpl) original).getAttributes());
			bundleSpec.setArbitraryDirectives(((BundleSpecificationImpl) original).getArbitraryDirectives());
		}
		return bundleSpec;
	}

	public HostSpecification createHostSpecification(String hostSymbolicName, VersionRange versionRange) {
		HostSpecificationImpl hostSpec = new HostSpecificationImpl();
		hostSpec.setName(hostSymbolicName);
		hostSpec.setVersionRange(versionRange);
		return hostSpec;
	}

	public HostSpecification createHostSpecification(HostSpecification original) {
		HostSpecificationImpl hostSpec = new HostSpecificationImpl();
		hostSpec.setName(original.getName());
		hostSpec.setVersionRange(original.getVersionRange());
		if (original instanceof HostSpecificationImpl) {
			hostSpec.setAttributes(((HostSpecificationImpl) original).getAttributes());
			hostSpec.setArbitraryDirectives(((HostSpecificationImpl) original).getArbitraryDirectives());
		}
		return hostSpec;
	}

	public ImportPackageSpecification createImportPackageSpecification(String packageName, VersionRange versionRange, String bundleSymbolicName, VersionRange bundleVersionRange, Map<String, ?> directives, Map<String, ?> attributes, BundleDescription importer) {
		ImportPackageSpecificationImpl packageSpec = new ImportPackageSpecificationImpl();
		packageSpec.setName(packageName);
		packageSpec.setVersionRange(versionRange);
		packageSpec.setBundleSymbolicName(bundleSymbolicName);
		packageSpec.setBundleVersionRange(bundleVersionRange);
		packageSpec.setDirectives(directives);
		packageSpec.setAttributes(attributes);
		packageSpec.setBundle(importer);
		return packageSpec;
	}

	public ImportPackageSpecification createImportPackageSpecification(ImportPackageSpecification original) {
		ImportPackageSpecificationImpl packageSpec = new ImportPackageSpecificationImpl();
		packageSpec.setName(original.getName());
		packageSpec.setVersionRange(original.getVersionRange());
		packageSpec.setBundleSymbolicName(original.getBundleSymbolicName());
		packageSpec.setBundleVersionRange(original.getBundleVersionRange());
		packageSpec.setDirectives(original.getDirectives());
		packageSpec.setAttributes(original.getAttributes());
		if (original instanceof ImportPackageSpecificationImpl) {
			packageSpec.setArbitraryDirectives(((ImportPackageSpecificationImpl) original).getArbitraryDirectives());
		}
		return packageSpec;
	}

	public ExportPackageDescription createExportPackageDescription(ExportPackageDescription original) {
		ExportPackageDescriptionImpl exportPackage = new ExportPackageDescriptionImpl();
		exportPackage.setName(original.getName());
		exportPackage.setVersion(original.getVersion());
		exportPackage.setDirectives(original.getDirectives());
		exportPackage.setAttributes(original.getAttributes());
		exportPackage.setArbitraryDirectives(((ExportPackageDescriptionImpl) original).getArbitraryDirectives());
		return exportPackage;
	}

	public ExportPackageDescription createExportPackageDescription(String packageName, Version version, Map<String, ?> directives, Map<String, ?> attributes, boolean root, BundleDescription exporter) {
		ExportPackageDescriptionImpl exportPackage = new ExportPackageDescriptionImpl();
		exportPackage.setName(packageName);
		exportPackage.setVersion(version);
		exportPackage.setDirectives(directives);
		exportPackage.setAttributes(attributes);
		exportPackage.setExporter(exporter);
		return exportPackage;
	}

	/**
	 * @deprecated
	 */
	public GenericDescription createGenericDescription(String name, String type, Version version, Map<String, ?> attributes) {
		return createGenericDescription(name, type, version, attributes, null, null);
	}

	public GenericDescription createGenericDescription(String type, Map<String, ?> attributes, Map<String, String> directives, BundleDescription supplier) {
		return createGenericDescription(null, type, null, attributes, directives, supplier);
	}

	private GenericDescription createGenericDescription(String name, String type, Version version, Map<String, ?> attributes, Map<String, String> directives, BundleDescription supplier) {
		GenericDescriptionImpl result = new GenericDescriptionImpl();
		result.setType(type);
		Dictionary<String, Object> attrs = attributes == null ? new Hashtable<String, Object>() : new Hashtable<String, Object>(attributes);
		if (version != null) {
			Object versionObj = attrs.get(Constants.VERSION_ATTRIBUTE);
			if (!(versionObj instanceof Version) && version != null)
				attrs.put(Constants.VERSION_ATTRIBUTE, version);
		}
		if (name != null) {
			Object nameObj = attrs.get(result.getType());
			if (!(nameObj instanceof String))
				attrs.put(result.getType(), name);
		}
		result.setAttributes(attrs);
		result.setDirectives(directives);
		result.setSupplier(supplier);
		return result;
	}

	public GenericSpecification createGenericSpecification(String name, String type, String matchingFilter, boolean optional, boolean multiple) throws InvalidSyntaxException {
		GenericSpecificationImpl result = new GenericSpecificationImpl();
		result.setName(name);
		result.setType(type);
		result.setMatchingFilter(matchingFilter, true);
		int resolution = 0;
		if (optional)
			resolution |= GenericSpecification.RESOLUTION_OPTIONAL;
		if (multiple)
			resolution |= GenericSpecification.RESOLUTION_MULTIPLE;
		result.setResolution(resolution);
		return result;
	}

	public NativeCodeDescription createNativeCodeDescription(String[] nativePaths, String[] processors, String[] osNames, VersionRange[] osVersions, String[] languages, String filter) throws InvalidSyntaxException {
		NativeCodeDescriptionImpl result = new NativeCodeDescriptionImpl();
		result.setName(Constants.BUNDLE_NATIVECODE);
		result.setNativePaths(nativePaths);
		result.setProcessors(processors);
		result.setOSNames(osNames);
		result.setOSVersions(osVersions);
		result.setLanguages(languages);
		result.setFilter(filter);
		return result;
	}

	public NativeCodeSpecification createNativeCodeSpecification(NativeCodeDescription[] nativeCodeDescriptions, boolean optional) {
		NativeCodeSpecificationImpl result = new NativeCodeSpecificationImpl();
		result.setName(Constants.BUNDLE_NATIVECODE);
		result.setOptional(optional);
		result.setPossibleSuppliers(nativeCodeDescriptions);
		return result;
	}

	public SystemState createSystemState(BundleContext context) {
		SystemState state = new SystemState(context);
		state.setFactory(this);
		return state;
	}

	/**
	 * @deprecated
	 */
	public State createState() {
		return internalCreateState();
	}

	public State createState(boolean createResolver) {
		State result = internalCreateState();
		if (createResolver)
			result.setResolver(new ResolverImpl(false));
		return result;
	}

	public State createState(State original) {
		StateImpl newState = internalCreateState();
		newState.setTimeStamp(original.getTimeStamp());
		BundleDescription[] bundles = original.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			BundleDescription newBundle = createBundleDescription(bundles[i]);
			newState.basicAddBundle(newBundle);
			DisabledInfo[] infos = original.getDisabledInfos(bundles[i]);
			for (int j = 0; j < infos.length; j++)
				newState.addDisabledInfo(new DisabledInfo(infos[j].getPolicyName(), infos[j].getMessage(), newBundle));
		}
		newState.setResolved(false);
		newState.setPlatformProperties(original.getPlatformProperties());
		return newState;
	}

	private StateImpl internalCreateState() {
		StateImpl state = new UserState();
		state.setFactory(this);
		return state;
	}

	public SystemState readSystemState(BundleContext context, File stateFile, File lazyFile, boolean lazyLoad, long expectedTimeStamp) throws IOException {
		StateReader reader = new StateReader(stateFile, lazyFile, lazyLoad);
		SystemState restoredState = new SystemState(context);
		restoredState.setReader(reader);
		restoredState.setFactory(this);
		if (!reader.loadState(restoredState, expectedTimeStamp))
			return null;
		return restoredState;
	}

	/**
	 * @deprecated
	 */
	public State readState(InputStream stream) throws IOException {
		return internalReadStateDeprecated(internalCreateState(), new DataInputStream(stream), -1);
	}

	/**
	 * @deprecated
	 */
	public State readState(DataInputStream stream) throws IOException {
		return internalReadStateDeprecated(internalCreateState(), stream, -1);
	}

	public State readState(File stateDirectory) throws IOException {
		return internalReadState(internalCreateState(), stateDirectory, -1);
	}

	private State internalReadStateDeprecated(StateImpl toRestore, DataInputStream stream, long expectedTimestamp) throws IOException {
		StateReader reader = new StateReader();
		if (!reader.loadStateDeprecated(toRestore, stream, expectedTimestamp))
			return null;
		return toRestore;
	}

	private State internalReadState(StateImpl toRestore, File stateDirectory, long expectedTimestamp) throws IOException {
		File stateFile = new File(stateDirectory, StateReader.STATE_FILE);
		File lazyFile = new File(stateDirectory, StateReader.LAZY_FILE);
		if (!stateFile.exists() || !lazyFile.exists()) {
			StorageManager storageManager = new StorageManager(stateDirectory, "none", true); //$NON-NLS-1$
			try {
				// if the directory is pointing at the configuration directory then the base files will not exist
				storageManager.open(true);
				// try using the storage manager to find the managed state files (bug 143255)
				File managedState = storageManager.lookup(StateReader.STATE_FILE, false);
				File managedLazy = storageManager.lookup(StateReader.LAZY_FILE, false);
				if (managedState != null && managedLazy != null) {
					stateFile = managedState;
					lazyFile = managedLazy;
				}
			} finally {
				storageManager.close();
			}
		}
		StateReader reader = new StateReader(stateFile, lazyFile, false);
		if (!reader.loadState(toRestore, expectedTimestamp))
			return null;
		return toRestore;
	}

	/**
	 * @deprecated
	 */
	public void writeState(State state, DataOutputStream stream) throws IOException {
		internalWriteStateDeprecated(state, stream);
	}

	public void writeState(State state, File stateDirectory) throws IOException {
		if (stateDirectory == null)
			throw new IOException();
		StateWriter writer = new StateWriter();
		File stateFile = new File(stateDirectory, StateReader.STATE_FILE);
		File lazyFile = new File(stateDirectory, StateReader.LAZY_FILE);
		writer.saveState((StateImpl) state, stateFile, lazyFile);
	}

	/**
	 * @deprecated
	 */
	public void writeState(State state, OutputStream stream) throws IOException {
		internalWriteStateDeprecated(state, new DataOutputStream(stream));
	}

	public void writeState(State state, File stateFile, File lazyFile) throws IOException {
		StateWriter writer = new StateWriter();
		writer.saveState((StateImpl) state, stateFile, lazyFile);
	}

	public void internalWriteStateDeprecated(State state, DataOutputStream stream) throws IOException {
		if (state.getFactory() != this)
			throw new IllegalArgumentException();
		StateWriter writer = new StateWriter();
		writer.saveStateDeprecated((StateImpl) state, stream);
	}

	@SuppressWarnings("unchecked")
	public List<BundleSpecification> createBundleSpecifications(String declaration) {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, declaration);
			if (elements == null)
				return Collections.EMPTY_LIST;
			List<BundleSpecification> result = new ArrayList<BundleSpecification>(elements.length);
			for (ManifestElement element : elements)
				result.add(StateBuilder.createRequiredBundle(element));
			return result;
		} catch (BundleException e) {
			throw (IllegalArgumentException) new IllegalArgumentException("Declaration is invalid: " + declaration).initCause(e); //$NON-NLS-1$
		}
	}

	@SuppressWarnings("unchecked")
	public List<HostSpecification> createHostSpecifications(String declaration) {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(Constants.FRAGMENT_HOST, declaration);
			if (elements == null)
				return Collections.EMPTY_LIST;
			List<HostSpecification> result = new ArrayList<HostSpecification>(elements.length);
			for (ManifestElement element : elements)
				result.add(StateBuilder.createHostSpecification(element, null));
			return result;
		} catch (BundleException e) {
			throw (IllegalArgumentException) new IllegalArgumentException("Declaration is invalid: " + declaration).initCause(e); //$NON-NLS-1$
		}
	}

	@SuppressWarnings("unchecked")
	public List<ImportPackageSpecification> createImportPackageSpecifications(String declaration) {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(Constants.IMPORT_PACKAGE, declaration);
			if (elements == null)
				return Collections.EMPTY_LIST;
			List<ImportPackageSpecification> result = new ArrayList<ImportPackageSpecification>(elements.length);
			for (ManifestElement element : elements)
				StateBuilder.addImportPackages(element, result, 2, false);
			return result;
		} catch (BundleException e) {
			throw (IllegalArgumentException) new IllegalArgumentException("Declaration is invalid: " + declaration).initCause(e); //$NON-NLS-1$
		}
	}

	@SuppressWarnings("unchecked")
	public List<GenericDescription> createGenericDescriptions(String declaration) {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(Constants.PROVIDE_CAPABILITY, declaration);
			if (elements == null)
				return Collections.EMPTY_LIST;
			return StateBuilder.createOSGiCapabilities(elements, new ArrayList<GenericDescription>(elements.length), (Integer) null);
		} catch (BundleException e) {
			throw (IllegalArgumentException) new IllegalArgumentException("Declaration is invalid: " + declaration).initCause(e); //$NON-NLS-1$
		}
	}

	@SuppressWarnings("unchecked")
	public List<GenericSpecification> createGenericSpecifications(String declaration) {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(Constants.REQUIRE_CAPABILITY, declaration);
			if (elements == null)
				return Collections.EMPTY_LIST;
			return StateBuilder.createOSGiRequires(elements, new ArrayList<GenericSpecification>(elements.length));
		} catch (BundleException e) {
			throw (IllegalArgumentException) new IllegalArgumentException("Declaration is invalid: " + declaration).initCause(e); //$NON-NLS-1$
		}
	}

	@SuppressWarnings("unchecked")
	public List<ExportPackageDescription> createExportPackageDescriptions(String declaration) {
		try {
			ManifestElement[] elements = ManifestElement.parseHeader(Constants.IMPORT_PACKAGE, declaration);
			if (elements == null)
				return Collections.EMPTY_LIST;
			List<ExportPackageDescription> result = new ArrayList<ExportPackageDescription>(elements.length);
			for (ManifestElement element : elements)
				StateBuilder.addExportPackages(element, result, false);
			return result;
		} catch (BundleException e) {
			throw (IllegalArgumentException) new IllegalArgumentException("Declaration is invalid: " + declaration).initCause(e); //$NON-NLS-1$
		}
	}
}
