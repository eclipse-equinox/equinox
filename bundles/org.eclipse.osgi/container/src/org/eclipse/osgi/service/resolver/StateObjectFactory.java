/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
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
package org.eclipse.osgi.service.resolver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

/**
 * A factory for states and their component objects.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @since 3.1
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface StateObjectFactory {

	/**
	 * The default object factory that can be used to create, populate and resolve
	 * states.  This is particularly useful when using the resolver outside the context
	 * of a running Equinox framework.
	 */
	public static final StateObjectFactory defaultFactory = new StateObjectFactoryProxy();

	/**
	 * Creates an empty state. The returned state does not have an
	 * attached resolver.
	 *
	 * @return the created state
	 * @deprecated use {@link #createState(boolean) }
	 */
	public State createState();

	/**
	 * Creates an empty state with or without a resolver.
	 *
	 * @param resolver true if the created state should be initialized with a resolver.
	 * @return the created state
	 * @since 3.2
	 */
	public State createState(boolean resolver);

	/**
	 * Creates a new state that is a copy of the given state. The returned state
	 * will contain copies of all bundle descriptions in the given state.
	 * The user objects from the original bundle descriptions is not copied and
	 * no data pertaining to resolution is copied.  The returned state will have a
	 * new resolver attached to it.
	 *
	 * @param state a state to be copied
	 * @return the created state
	 */
	public State createState(State state);

	/**
	 * Creates a bundle description from the given parameters.
	 *
	 * @param id id for the bundle
	 * @param symbolicName symbolic name for the bundle (may be
	 * <code>null</code>)
	 * @param version version for the bundle (may be <code>null</code>)
	 * @param location location for the bundle (may be <code>null</code>)
	 * @param required version constraints for all required bundles (may be
	 * <code>null</code>)
	 * @param host version constraint specifying the host for the bundle to be
	 * created. Should be <code>null</code> if the bundle is not a fragment
	 * @param imports version constraints for all packages imported
	 * (may be <code>null</code>)
	 * @param exports package descriptions of all the exported packages
	 * (may be <code>null</code>)
	 * @param providedPackages the list of provided packages (may be <code>null</code>)
	 * @param singleton whether the bundle created should be a singleton
	 * @return the created bundle description
	 * @deprecated use {@link #createBundleDescription(long, String, Version, String, BundleSpecification[], HostSpecification, ImportPackageSpecification[], ExportPackageDescription[], boolean, boolean, boolean, String, String[], GenericSpecification[], GenericDescription[])}
	 */
	public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, ImportPackageSpecification[] imports, ExportPackageDescription[] exports, String[] providedPackages, boolean singleton);

	/**
	 * Creates a bundle description from the given parameters.
	 *
	 * @param id id for the bundle
	 * @param symbolicName symbolic name for the bundle (may be
	 * <code>null</code>)
	 * @param version version for the bundle (may be <code>null</code>)
	 * @param location location for the bundle (may be <code>null</code>)
	 * @param required version constraints for all required bundles (may be
	 * <code>null</code>)
	 * @param host version constraint specifying the host for the bundle to be
	 * created. Should be <code>null</code> if the bundle is not a fragment
	 * @param imports version constraints for all packages imported
	 * (may be <code>null</code>)
	 * @param exports package descriptions of all the exported packages
	 * (may be <code>null</code>)
	 * @param providedPackages the list of provided packages (may be <code>null</code>)
	 * @param singleton whether the bundle created should be a singleton
	 * @param attachFragments whether the bundle allows fragments to attach
	 * @param dynamicFragments whether the bundle allows fragments to dynamically attach
	 * @param platformFilter the platform filter (may be <code>null</code>)
	 * @param executionEnvironment the execution environment (may be <code>null</code>)
	 * @param genericRequires the version constraints for all required capabilities (may be <code>null</code>)
	 * @param genericCapabilities the specifications of all the capabilities of the bundle (may be <code>null</code>)
	 * @return the created bundle description
	 * @deprecated use {@link #createBundleDescription(long, String, Version, String, BundleSpecification[], HostSpecification, ImportPackageSpecification[], ExportPackageDescription[], boolean, boolean, boolean, String, String[], GenericSpecification[], GenericDescription[])}
	 */
	public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, ImportPackageSpecification[] imports, ExportPackageDescription[] exports, String[] providedPackages, boolean singleton, boolean attachFragments, boolean dynamicFragments, String platformFilter, String executionEnvironment, GenericSpecification[] genericRequires, GenericDescription[] genericCapabilities);

	/**
	 * Creates a bundle description from the given parameters.
	 *
	 * @param id id for the bundle
	 * @param symbolicName symbolic name for the bundle (may be  <code>null</code>)
	 * @param version version for the bundle (may be <code>null</code>)
	 * @param location location for the bundle (may be <code>null</code>)
	 * @param required version constraints for all required bundles (may be  <code>null</code>)
	 * @param host version constraint specifying the host for the bundle to be created. Should be <code>null</code> if the bundle is not a fragment
	 * @param imports version constraints for all packages imported  (may be <code>null</code>)
	 * @param exports package descriptions of all the exported packages (may be <code>null</code>)
	 * @param singleton whether the bundle created should be a singleton
	 * @param attachFragments whether the bundle allows fragments to attach
	 * @param dynamicFragments whether the bundle allows fragments to dynamically attach
	 * @param platformFilter the platform filter (may be <code>null</code>)
	 * @param executionEnvironments the execution environment (may be <code>null</code>)
	 * @param genericRequires the version constraints for all required capabilities (may be <code>null</code>)
	 * @param genericCapabilities the specifications of all the capabilities of the bundle (may be <code>null</code>)
	 * @return the created bundle description
	 */
	public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, ImportPackageSpecification[] imports, ExportPackageDescription[] exports, boolean singleton, boolean attachFragments, boolean dynamicFragments, String platformFilter, String[] executionEnvironments, GenericSpecification[] genericRequires, GenericDescription[] genericCapabilities);

	/**
	 * Creates a bundle description from the given parameters.
	 *
	 * @param id id for the bundle
	 * @param symbolicName symbolic name for the bundle (may be  <code>null</code>)
	 * @param version version for the bundle (may be <code>null</code>)
	 * @param location location for the bundle (may be <code>null</code>)
	 * @param required version constraints for all required bundles (may be  <code>null</code>)
	 * @param host version constraint specifying the host for the bundle to be created. Should be <code>null</code> if the bundle is not a fragment
	 * @param imports version constraints for all packages imported  (may be <code>null</code>)
	 * @param exports package descriptions of all the exported packages (may be <code>null</code>)
	 * @param singleton whether the bundle created should be a singleton
	 * @param attachFragments whether the bundle allows fragments to attach
	 * @param dynamicFragments whether the bundle allows fragments to dynamically attach
	 * @param platformFilter the platform filter (may be <code>null</code>)
	 * @param executionEnvironments the execution environment (may be <code>null</code>)
	 * @param genericRequires the version constraints for all required capabilities (may be <code>null</code>)
	 * @param genericCapabilities the specifications of all the capabilities of the bundle (may be <code>null</code>)
	 * @param nativeCode the native code specification of the bundle (may be <code>null</code>)
	 * @return the created bundle description
	 * @since 3.4
	 */
	public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, ImportPackageSpecification[] imports, ExportPackageDescription[] exports, boolean singleton, boolean attachFragments, boolean dynamicFragments, String platformFilter, String[] executionEnvironments, GenericSpecification[] genericRequires, GenericDescription[] genericCapabilities, NativeCodeSpecification nativeCode);

	/**
	 * Creates a bundle description from the given parameters.
	 *
	 * @param id id for the bundle
	 * @param symbolicName the symbolic name of the bundle.  This may include directives and/or attributes encoded using the Bundle-SymbolicName header.
	 * @param version version for the bundle (may be <code>null</code>)
	 * @param location location for the bundle (may be <code>null</code>)
	 * @param required version constraints for all required bundles (may be  <code>null</code>)
	 * @param host version constraint specifying the host for the bundle to be created. Should be <code>null</code> if the bundle is not a fragment
	 * @param imports version constraints for all packages imported  (may be <code>null</code>)
	 * @param exports package descriptions of all the exported packages (may be <code>null</code>)
	 * @param platformFilter the platform filter (may be <code>null</code>)
	 * @param executionEnvironments the execution environment (may be <code>null</code>)
	 * @param genericRequires the version constraints for all required capabilities (may be <code>null</code>)
	 * @param genericCapabilities the specifications of all the capabilities of the bundle (may be <code>null</code>)
	 * @param nativeCode the native code specification of the bundle (may be <code>null</code>)
	 * @return the created bundle description
	 * @since 3.8
	 */
	public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, ImportPackageSpecification[] imports, ExportPackageDescription[] exports, String platformFilter, String[] executionEnvironments, GenericSpecification[] genericRequires, GenericDescription[] genericCapabilities, NativeCodeSpecification nativeCode);

	/**
	 * Returns a bundle description based on the information in the supplied manifest dictionary.
	 * The manifest should contain String keys and String values which correspond to
	 * proper OSGi manifest headers and values.
	 *
	 * @param state the state for which the description is being created
	 * @param manifest a collection of OSGi manifest headers and values
	 * @param location the URL location of the bundle (may be <code>null</code>)
	 * @param id the id of the bundle
	 * @return a bundle description derived from the given information
	 * @throws BundleException if an error occurs while reading the manifest
	 */
	public BundleDescription createBundleDescription(State state, Dictionary<String, String> manifest, String location, long id) throws BundleException;

	/**
	 * Returns a bundle description based on the information in the supplied manifest dictionary.
	 * The manifest should contain String keys and String values which correspond to
	 * proper OSGi manifest headers and values.
	 *
	 * @param manifest a collection of OSGi manifest headers and values
	 * @param location the URL location of the bundle (may be <code>null</code>)
	 * @param id the id of the bundle
	 * @return a bundle description derived from the given information
	 * @throws BundleException if an error occurs while reading the manifest
	 * @deprecated use {@link #createBundleDescription(State, Dictionary, String, long)}
	 */
	public BundleDescription createBundleDescription(Dictionary<String, String> manifest, String location, long id) throws BundleException;

	/**
	 * Creates a bundle description that is a copy of the given description.
	 * The user object of the original bundle description is not copied.
	 *
	 * @param original the bundle description to be copied
	 * @return the created bundle description
	 */
	public BundleDescription createBundleDescription(BundleDescription original);

	/**
	 * Creates a bundle specification from the given parameters.
	 *
	 * @param requiredSymbolicName the symbolic name for the required bundle
	 * @param requiredVersionRange the required version range (may be <code>null</code>)
	 * @param export whether the required bundle should be re-exported
	 * @param optional whether the constraint should be optional
	 * @return the created bundle specification
	 * @see VersionConstraint for information on the available match rules
	 */
	public BundleSpecification createBundleSpecification(String requiredSymbolicName, VersionRange requiredVersionRange, boolean export, boolean optional);

	/**
	 * Creates a bundle specification that is a copy of the given constraint.
	 *
	 * @param original the constraint to be copied
	 * @return the created bundle specification
	 */
	public BundleSpecification createBundleSpecification(BundleSpecification original);

	/**
	 * Creates bundle specifications from the given declaration.  The declaration uses
	 * the bundle manifest syntax for the Require-Bundle header.
	 * @param declaration a string declaring bundle specifications
	 * @return the bundle specifications
	 * @since 3.8
	 */
	public List<BundleSpecification> createBundleSpecifications(String declaration);

	/**
	 * Creates a host specification from the given parameters.
	 *
	 * @param hostSymbolicName the symbolic name for the host bundle
	 * @param hostVersionRange the version range for the host bundle (may be <code>null</code>)
	 * @return the created host specification
	 * @see VersionConstraint for information on the available match rules
	 */
	public HostSpecification createHostSpecification(String hostSymbolicName, VersionRange hostVersionRange);

	/**
	 * Creates host specifications from the given declaration.  The declaration uses
	 * the bundle manifest syntax for the Fragment-Host header.
	 * @param declaration a string declaring host specifications
	 * @return the host specifications
	 * @since 3.8
	 */
	public List<HostSpecification> createHostSpecifications(String declaration);

	/**
	 * Creates a host specification that is a copy of the given constraint.
	 *
	 * @param original the constraint to be copied
	 * @return the created host specification
	 */
	public HostSpecification createHostSpecification(HostSpecification original);

	/**
	 * Creates an import package specification from the given parameters.
	 *
	 * @param packageName the package name
	 * @param versionRange the package versionRange (may be <code>null</code>).
	 * @param bundleSymbolicName the Bundle-SymbolicName of the bundle that must export the package (may be <code>null</code>)
	 * @param bundleVersionRange the bundle versionRange (may be <code>null</code>).
	 * @param directives the directives for this package (may be <code>null</code>)
	 * @param attributes the arbitrary attributes for the package import (may be <code>null</code>)
	 * @param importer the importing bundle (may be <code>null</code>)
	 * @return the created package specification
	 */
	public ImportPackageSpecification createImportPackageSpecification(String packageName, VersionRange versionRange, String bundleSymbolicName, VersionRange bundleVersionRange, Map<String, ?> directives, Map<String, ?> attributes, BundleDescription importer);

	/**
	 * Creates an import package specification that is a copy of the given import package
	 * @param original the import package to be copied
	 * @return the created package specification
	 */
	public ImportPackageSpecification createImportPackageSpecification(ImportPackageSpecification original);

	/**
	 * Creates an import package specifications from the given declaration.  The declaration uses
	 * the bundle manifest syntax for the Import-Package header.
	 * @param declaration a string declaring import package specifications
	 * @return the import package specifications
	 * @since 3.8
	 */
	public List<ImportPackageSpecification> createImportPackageSpecifications(String declaration);

	/**
	 * Used by the Resolver to dynamically create ExportPackageDescription objects
	 * during the resolution process. The Resolver needs to create
	 * ExportPackageDescriptions dynamically for a host when a fragment exports a
	 * package.
	 *
	 * @param packageName the package name
	 * @param version     the version of the package (may be <code>null</code>)
	 * @param directives  the directives for the package (may be <code>null</code>)
	 * @param attributes  the attributes for the package (may be <code>null</code>)
	 * @param root        whether the package is a root package
	 * @param exporter    the exporter of the package (may be <code>null</code>)
	 * @return the created package
	 */
	public ExportPackageDescription createExportPackageDescription(String packageName, Version version, Map<String, ?> directives, Map<String, ?> attributes, boolean root, BundleDescription exporter);

	/**
	 * Creates a generic description from the given parameters
	 * @param name the name of the generic description
	 * @param type the type of the generic description (may be <code>null</code>)
	 * @param version the version of the generic description (may be <code>null</code>)
	 * @param attributes the attributes for the generic description (may be <code>null</code>)
	 * @return the created generic description
	 * @deprecated use {@link #createGenericDescription(String, String, Version, Map)}
	 */
	public GenericDescription createGenericDescription(String name, String type, Version version, Map<String, ?> attributes);

	/**
	 * Creates a generic description from the given parameters
	 * @param type the type of the generic description (may be <code>null</code>)
	 * @param attributes the attributes for the generic description (may be <code>null</code>)
	 * @param directives the directives for the generic description (may be <code>null</code>)
	 * @param supplier the supplier of the generic description (may be <code>null</code>)
	 * @return the created generic description
	 * @since 3.7
	 */
	public GenericDescription createGenericDescription(String type, Map<String, ?> attributes, Map<String, String> directives, BundleDescription supplier);

	/**
	 * Creates generic descriptions from the given declaration.  The declaration uses
	 * the bundle manifest syntax for the Provide-Capability header.
	 * @param declaration a string declaring generic descriptions
	 * @return the generic descriptions
	 * @since 3.8
	 */
	public List<GenericDescription> createGenericDescriptions(String declaration);

	/**
	 * Creates a generic specification from the given parameters
	 * @param name the name of the generic specification
	 * @param type the type of the generic specification (may be <code>null</code>)
	 * @param matchingFilter the matching filter (may be <code>null</code>)
	 * @param optional whether the specification is optional
	 * @param multiple whether the specification allows for multiple suppliers
	 * @return the created generic specification
	 * @throws InvalidSyntaxException if the matching filter is invalid
	 */
	public GenericSpecification createGenericSpecification(String name, String type, String matchingFilter, boolean optional, boolean multiple) throws InvalidSyntaxException;

	/**
	 * Creates generic specifications from the given declaration.  The declaration uses
	 * the bundle manifest syntax for the Require-Capability header.
	 * @param declaration a string declaring generic specifications
	 * @return the generic specifications
	 * @since 3.8
	 */
	public List<GenericSpecification> createGenericSpecifications(String declaration);

	/**
	 * Creates a native code specification from the given parameters
	 * @param nativeCodeDescriptions the native code descriptors
	 * @param optional whether the specification is optional
	 * @return the created native code specification
	 * @since 3.4
	 */
	public NativeCodeSpecification createNativeCodeSpecification(NativeCodeDescription[] nativeCodeDescriptions, boolean optional);

	/**
	 * Creates a native code description from the given parameters
	 * @param nativePaths the native code paths (may be <code>null</code>)
	 * @param processors the supported processors (may be <code>null</code>)
	 * @param osNames the supported operating system names (may be <code>null</code>)
	 * @param osVersions the supported operating system version ranges (may be <code>null</code>)
	 * @param languages the supported languages (may be <code>null</code>)
	 * @param filter the selection filter (may be <code>null</code>)
	 * @return the created native code description
	 * @throws InvalidSyntaxException if the selection filter is invalid
	 * @since 3.4
	 */
	public NativeCodeDescription createNativeCodeDescription(String[] nativePaths, String[] processors, String[] osNames, VersionRange[] osVersions, String[] languages, String filter) throws InvalidSyntaxException;

	/**
	 * Creates an export package specification that is a copy of the given constraint
	 * @param original the export package to be copied
	 * @return the created package
	 */
	public ExportPackageDescription createExportPackageDescription(ExportPackageDescription original);

	/**
	 * Creates export package descriptions from the given declaration.  The declaration uses
	 * the bundle manifest syntax for the Export-Package header.
	 * @param declaration a string declaring export package descriptions
	 * @return the export package descriptions
	 * @since 3.8
	 */
	public List<ExportPackageDescription> createExportPackageDescriptions(String declaration);

	/**
	 * Persists the given state in the given output stream. Closes the stream.
	 *
	 * @param state the state to be written
	 * @param stream the stream where to write the state to
	 * @throws IOException if an IOException happens while writing the state to
	 * the stream
	 * @throws IllegalArgumentException if the state provided was not created by
	 * this factory
	 * @deprecated use {@link #writeState(State, File)} instead
	 * @since 3.1
	 */
	public void writeState(State state, OutputStream stream) throws IOException;

	/**
	 * Persists the given state in the given output stream. Closes the stream.
	 *
	 * @param state the state to be written
	 * @param stream the stream where to write the state to
	 * @throws IOException if an IOException happens while writing the state to
	 * the stream
	 * @throws IllegalArgumentException if the state provided was not created by
	 * this factory
	 * @deprecated use {@link #writeState(State, File)} instead
	 * @see #writeState(State, OutputStream)
	 */
	public void writeState(State state, DataOutputStream stream) throws IOException;

	/**
	 * Persists the given state in the given directory.
	 *
	 * @param state the state to be written
	 * @param stateDirectory the directory where to write the state to
	 * @throws IOException if an IOException happens while writing the state to
	 * the stream
	 * @throws IllegalArgumentException if the state provided was not created by
	 * this factory
	 */
	public void writeState(State state, File stateDirectory) throws IOException;

	/**
	 * Reads a persisted state from the given stream. Closes the stream.
	 *
	 * @param stream the stream where to read the state from
	 * @return the state read
	 * @throws IOException if an IOException happens while reading the state from
	 * the stream
	 * @deprecated use {@link #readState(File)} instead
	 * @since 3.1
	 */
	public State readState(InputStream stream) throws IOException;

	/**
	 * Reads a persisted state from the given stream. Closes the stream.
	 *
	 * @param stream the stream where to read the state from
	 * @return the state read
	 * @throws IOException if an IOException happens while reading the state from
	 * the stream
	 * @deprecated use {@link #readState(File)} instead
	 * @see #readState(InputStream)
	 */
	public State readState(DataInputStream stream) throws IOException;

	/**
	 * Reads a persisted state from the given directory.
	 *
	 * @param stateDirectory the directory where to read the state from
	 * @return the state read
	 * @throws IOException if an IOException happens while reading the state from
	 * the stream
	 */
	public State readState(File stateDirectory) throws IOException;

	static class StateObjectFactoryProxy implements StateObjectFactory {
		private static final String IMPL_NAME = "org.eclipse.osgi.internal.resolver.StateObjectFactoryImpl"; //$NON-NLS-1$
		private Object monitor = new Object();
		private StateObjectFactory implementation;

		private StateObjectFactory getImplementation() {
			synchronized (this.monitor) {
				if (implementation == null) {
					try {
						Class<?> implClass = Class.forName(IMPL_NAME);
						implementation = (StateObjectFactory) implClass.getConstructor().newInstance();
					} catch (Throwable t) {
						throw new UnsupportedOperationException("Not able to create StateObjectFactory implementation: " + IMPL_NAME, t); //$NON-NLS-1$
					}
				}
				return implementation;
			}
		}

		@Deprecated
		@Override
		public State createState() {
			return getImplementation().createState();
		}

		@Override
		public State createState(boolean resolver) {
			return getImplementation().createState(resolver);
		}

		@Override
		public State createState(State state) {
			return getImplementation().createState(state);
		}

		@Deprecated
		@Override
		public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, ImportPackageSpecification[] imports, ExportPackageDescription[] exports, String[] providedPackages, boolean singleton) {
			return getImplementation().createBundleDescription(id, symbolicName, version, location, required, host, imports, exports, providedPackages, singleton);
		}

		@Deprecated
		@Override
		public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, ImportPackageSpecification[] imports, ExportPackageDescription[] exports, String[] providedPackages, boolean singleton, boolean attachFragments, boolean dynamicFragments, String platformFilter, String executionEnvironment, GenericSpecification[] genericRequires, GenericDescription[] genericCapabilities) {
			return getImplementation().createBundleDescription(id, symbolicName, version, location, required, host, imports, exports, providedPackages, singleton, attachFragments, dynamicFragments, platformFilter, executionEnvironment, genericRequires, genericCapabilities);
		}

		@Override
		public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, ImportPackageSpecification[] imports, ExportPackageDescription[] exports, boolean singleton, boolean attachFragments, boolean dynamicFragments, String platformFilter, String[] executionEnvironments, GenericSpecification[] genericRequires, GenericDescription[] genericCapabilities) {
			return getImplementation().createBundleDescription(id, symbolicName, version, location, required, host, imports, exports, singleton, attachFragments, dynamicFragments, platformFilter, executionEnvironments, genericRequires, genericCapabilities);
		}

		@Override
		public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, ImportPackageSpecification[] imports, ExportPackageDescription[] exports, boolean singleton, boolean attachFragments, boolean dynamicFragments, String platformFilter, String[] executionEnvironments, GenericSpecification[] genericRequires, GenericDescription[] genericCapabilities, NativeCodeSpecification nativeCode) {
			return getImplementation().createBundleDescription(id, symbolicName, version, location, required, host, imports, exports, singleton, attachFragments, dynamicFragments, platformFilter, executionEnvironments, genericRequires, genericCapabilities, nativeCode);
		}

		@Override
		public BundleDescription createBundleDescription(long id, String symbolicName, Version version, String location, BundleSpecification[] required, HostSpecification host, ImportPackageSpecification[] imports, ExportPackageDescription[] exports, String platformFilter, String[] executionEnvironments, GenericSpecification[] genericRequires, GenericDescription[] genericCapabilities, NativeCodeSpecification nativeCode) {
			return getImplementation().createBundleDescription(id, symbolicName, version, location, required, host, imports, exports, platformFilter, executionEnvironments, genericRequires, genericCapabilities, nativeCode);
		}

		@Override
		public BundleDescription createBundleDescription(State state, Dictionary<String, String> manifest, String location, long id) throws BundleException {
			return getImplementation().createBundleDescription(state, manifest, location, id);
		}

		@Deprecated
		@Override
		public BundleDescription createBundleDescription(Dictionary<String, String> manifest, String location, long id) throws BundleException {
			return getImplementation().createBundleDescription(manifest, location, id);
		}

		@Override
		public BundleDescription createBundleDescription(BundleDescription original) {
			return getImplementation().createBundleDescription(original);
		}

		@Override
		public BundleSpecification createBundleSpecification(String requiredSymbolicName, VersionRange requiredVersionRange, boolean export, boolean optional) {
			return getImplementation().createBundleSpecification(requiredSymbolicName, requiredVersionRange, export, optional);
		}

		@Override
		public BundleSpecification createBundleSpecification(BundleSpecification original) {
			return getImplementation().createBundleSpecification(original);
		}

		@Override
		public List<BundleSpecification> createBundleSpecifications(String declaration) {
			return getImplementation().createBundleSpecifications(declaration);
		}

		@Override
		public HostSpecification createHostSpecification(String hostSymbolicName, VersionRange hostVersionRange) {
			return getImplementation().createHostSpecification(hostSymbolicName, hostVersionRange);
		}

		@Override
		public List<HostSpecification> createHostSpecifications(String declaration) {
			return getImplementation().createHostSpecifications(declaration);
		}

		@Override
		public HostSpecification createHostSpecification(HostSpecification original) {
			return getImplementation().createHostSpecification(original);
		}

		@Override
		public ImportPackageSpecification createImportPackageSpecification(String packageName, VersionRange versionRange, String bundleSymbolicName, VersionRange bundleVersionRange, Map<String, ?> directives, Map<String, ?> attributes, BundleDescription importer) {
			return getImplementation().createImportPackageSpecification(packageName, versionRange, bundleSymbolicName, bundleVersionRange, directives, attributes, importer);
		}

		@Override
		public ImportPackageSpecification createImportPackageSpecification(ImportPackageSpecification original) {
			return getImplementation().createImportPackageSpecification(original);
		}

		@Override
		public List<ImportPackageSpecification> createImportPackageSpecifications(String declaration) {
			return getImplementation().createImportPackageSpecifications(declaration);
		}

		@Override
		public ExportPackageDescription createExportPackageDescription(String packageName, Version version, Map<String, ?> directives, Map<String, ?> attributes, boolean root, BundleDescription exporter) {
			return getImplementation().createExportPackageDescription(packageName, version, directives, attributes, root, exporter);
		}

		@Deprecated
		@Override
		public GenericDescription createGenericDescription(String name, String type, Version version, Map<String, ?> attributes) {
			return getImplementation().createGenericDescription(name, type, version, attributes);
		}

		@Override
		public GenericDescription createGenericDescription(String type, Map<String, ?> attributes, Map<String, String> directives, BundleDescription supplier) {
			return getImplementation().createGenericDescription(type, attributes, directives, supplier);
		}

		@Override
		public List<GenericDescription> createGenericDescriptions(String declaration) {
			return getImplementation().createGenericDescriptions(declaration);
		}

		@Override
		public GenericSpecification createGenericSpecification(String name, String type, String matchingFilter, boolean optional, boolean multiple) throws InvalidSyntaxException {
			return getImplementation().createGenericSpecification(name, type, matchingFilter, optional, multiple);
		}

		@Override
		public List<GenericSpecification> createGenericSpecifications(String declaration) {
			return getImplementation().createGenericSpecifications(declaration);
		}

		@Override
		public NativeCodeSpecification createNativeCodeSpecification(NativeCodeDescription[] nativeCodeDescriptions, boolean optional) {
			return getImplementation().createNativeCodeSpecification(nativeCodeDescriptions, optional);
		}

		@Override
		public NativeCodeDescription createNativeCodeDescription(String[] nativePaths, String[] processors, String[] osNames, VersionRange[] osVersions, String[] languages, String filter) throws InvalidSyntaxException {
			return getImplementation().createNativeCodeDescription(nativePaths, processors, osNames, osVersions, languages, filter);
		}

		@Override
		public ExportPackageDescription createExportPackageDescription(ExportPackageDescription original) {
			return getImplementation().createExportPackageDescription(original);
		}

		@Override
		public List<ExportPackageDescription> createExportPackageDescriptions(String declaration) {
			return getImplementation().createExportPackageDescriptions(declaration);
		}

		@Deprecated
		@Override
		public void writeState(State state, OutputStream stream) throws IOException {
			getImplementation().writeState(state, stream);
		}

		@Deprecated
		@Override
		public void writeState(State state, DataOutputStream stream) throws IOException {
			getImplementation().writeState(state, stream);
		}

		@Override
		public void writeState(State state, File stateDirectory) throws IOException {
			getImplementation().writeState(state, stateDirectory);
		}

		@Deprecated
		@Override
		public State readState(InputStream stream) throws IOException {
			return getImplementation().readState(stream);
		}

		@Deprecated
		@Override
		public State readState(DataInputStream stream) throws IOException {
			return getImplementation().readState(stream);
		}

		@Override
		public State readState(File stateDirectory) throws IOException {
			return getImplementation().readState(stateDirectory);
		}
	}
}
