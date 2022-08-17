/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.core.runtime;

/**
 * An extension declared in a plug-in. All information is obtained from the
 * declaring plug-in's manifest (<code>plugin.xml</code>) file.
 * <p>
 * These registry objects are intended for relatively short-term use. Clients
 * that deal with these objects must be aware that they may become invalid if
 * the declaring plug-in is updated or uninstalled. If this happens, all methods
 * except {@link #isValid()} will throw {@link InvalidRegistryObjectException}.
 * For extension objects, the most common case is code in a plug-in dealing with
 * extensions contributed to one of the extension points it declares. Code in a
 * plug-in that has declared that it is not dynamic aware (or not declared
 * anything) can safely ignore this issue, since the registry would not be
 * modified while it is active. However, code in a plug-in that declares that it
 * is dynamic aware must be careful when accessing the extension objects because
 * they become invalid if the contributing plug-in is removed. Similarly, tools
 * that analyze or display the extension registry are vulnerable. Client code
 * can pre-test for invalid objects by calling {@link #isValid()}, which never
 * throws this exception. However, pre-tests are usually not sufficient because
 * of the possibility of the extension object becoming invalid as a result of a
 * concurrent activity. At-risk clients must treat
 * <code>InvalidRegistryObjectException</code> as if it were a checked
 * exception. Also, such clients should probably register a listener with the
 * extension registry so that they receive notification of any changes to the
 * registry.
 * </p>
 * <p>
 * This interface can be used without OSGi running.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IExtension {
	/**
	 * Returns all configuration elements declared by this extension. These elements
	 * are a direct reflection of the configuration markup supplied in the manifest
	 * (<code>plugin.xml</code>) file for the plug-in that declares this extension.
	 * Returns an empty array if this extension does not declare any configuration
	 * elements.
	 *
	 * @return the configuration elements declared by this extension
	 * @throws InvalidRegistryObjectException if this extension is no longer valid
	 */
	public IConfigurationElement[] getConfigurationElements() throws InvalidRegistryObjectException;

	/**
	 * Returns the namespace for this extension. This value can be used in various
	 * global facilities to discover this extension's provider.
	 *
	 * @return the namespace for this extension
	 * @throws InvalidRegistryObjectException if this extension is no longer valid
	 * @see IExtensionRegistry
	 * @since 3.0
	 * @deprecated As namespace is no longer restricted to the contributor name, use
	 *             {@link #getNamespaceIdentifier()} to obtain namespace name or
	 *             {@link #getContributor()} to get the name of the contributor of
	 *             this registry element.
	 *             <p>
	 *             In the past namespace was dictated by the name of the bundle. If
	 *             bundle <code>org.abc</code> contributed registry element with Id
	 *             of <code>MyId</code>, the namespace of the element was always set
	 *             to <code>org.abc</code>, producing the qualified name of
	 *             <code>org.abc.MyId</code>.
	 *             </p>
	 *             <p>
	 *             The namespace used to be the same as the bundle name. As a
	 *             result, the {@link #getNamespace()} method was used both to
	 *             obtain the name of the bundle and to obtain the namespace of a
	 *             registry element.
	 *             </p>
	 *             <p>
	 *             Since 3.2, the extension registry allows elements to specify
	 *             qualified name. The extension point of the plug-in
	 *             <code>org.abc</code> could specify
	 *             <code>org.zzz.MyExtPoint</code> as an Id. In this case, namespace
	 *             name is <code>org.zzz</code>, but the contributor name is
	 *             <code>org.abc</code>.
	 *             </p>
	 *             <p>
	 *             (The use of a simple Id is still a preferred way. Whenever
	 *             possible, specify only the simple Id and let runtime take care of
	 *             the rest.)
	 *             </p>
	 *             <p>
	 *             If your code used the {@link #getNamespace()} to obtain the name
	 *             of the contributing bundle, use {@link #getContributor()}. The
	 *             typical usage pattern here is to find a bundle name to obtain
	 *             some information from the corresponding OSGi bundle. For example,
	 *             deducing the file location specified as a relative path to the
	 *             bundle install location would fall into this group.
	 *             </p>
	 *             <p>
	 *             If your code used the {@link #getNamespace()} to obtain the
	 *             namespace of the registry element, use
	 *             {@link #getNamespaceIdentifier()}. Typically, this is the case
	 *             when code is trying to process registry elements belonging to
	 *             some logical group. For example, processing notifications for all
	 *             elements belonging to the <code>org.abc</code> namespace would
	 *             fall into this category.
	 *             </p>
	 */
	@Deprecated
	public String getNamespace() throws InvalidRegistryObjectException;

	/**
	 * Returns the namespace name for this extension.
	 *
	 * @return the namespace name for this extension
	 * @throws InvalidRegistryObjectException if this extension is no longer valid
	 * @since org.eclipse.equinox.registry 3.2
	 */
	public String getNamespaceIdentifier() throws InvalidRegistryObjectException;

	/**
	 * Returns the contributor of this extension.
	 *
	 * @return the contributor for this extension
	 * @throws InvalidRegistryObjectException if this extension is no longer valid
	 * @since org.eclipse.equinox.registry 3.2
	 */
	public IContributor getContributor() throws InvalidRegistryObjectException;

	/**
	 * Returns the unique identifier of the extension point to which this extension
	 * should be contributed.
	 *
	 * @return the unique identifier of the relevant extension point
	 * @throws InvalidRegistryObjectException if this extension is no longer valid
	 */
	public String getExtensionPointUniqueIdentifier() throws InvalidRegistryObjectException;

	/**
	 * Returns a displayable label for this extension. Returns the empty string if
	 * no label for this extension is specified in the plug-in manifest file.
	 * <p>
	 * Note that any translation specified in the plug-in manifest file is
	 * automatically applied.
	 * </p>
	 *
	 * @return a displayable string label for this extension, possibly the empty
	 *         string
	 * @throws InvalidRegistryObjectException if this extension is no longer valid
	 */
	public String getLabel() throws InvalidRegistryObjectException;

	/**
	 * When multi-language support is enabled, this method returns a displayable
	 * label for this extension in the specified locale. Returns the empty string if
	 * no label for this extension is specified in the plug-in manifest file.
	 * <p>
	 * The locale matching tries to find the best match between available
	 * translations and the requested locale, falling back to a more generic locale
	 * ("en") when the specific locale ("en_US") is not available.
	 * </p>
	 * <p>
	 * If multi-language support is not enabled, this method is equivalent to the
	 * method {@link #getLabel()}.
	 * </p>
	 *
	 * @param locale the requested locale
	 * @return a displayable string label for this extension, possibly the empty
	 *         string
	 * @throws InvalidRegistryObjectException if this extension is no longer valid
	 * @see IExtensionRegistry#isMultiLanguage()
	 * @since 3.5
	 */
	public String getLabel(String locale) throws InvalidRegistryObjectException;

	/**
	 * Returns the simple identifier of this extension, or <code>null</code> if this
	 * extension does not have an identifier. This identifier is specified in the
	 * plug-in manifest (<code>plugin.xml</code>) file as a non-empty string
	 * containing no period characters (<code>'.'</code>) and must be unique within
	 * the namespace.
	 *
	 * @return the simple identifier of the extension (e.g. <code>"main"</code>) or
	 *         <code>null</code>
	 * @throws InvalidRegistryObjectException if this extension is no longer valid
	 */
	public String getSimpleIdentifier() throws InvalidRegistryObjectException;

	/**
	 * Returns the unique identifier of this extension, or <code>null</code> if this
	 * extension does not have an identifier. If available, this identifier is
	 * unique within the plug-in registry, and is composed of the namespace where
	 * this extension was declared and this extension's simple identifier.
	 *
	 * @return the unique identifier of the extension (e.g.
	 *         <code>"com.example.acme.main"</code>), or <code>null</code>
	 * @throws InvalidRegistryObjectException if this extension is no longer valid
	 */
	public String getUniqueIdentifier() throws InvalidRegistryObjectException;

	@Override
	public boolean equals(Object o);

	/**
	 * Returns whether this extension object is valid.
	 *
	 * @return <code>true</code> if the object is valid, and <code>false</code> if
	 *         it is no longer valid
	 * @since 3.1
	 */
	public boolean isValid();
}
