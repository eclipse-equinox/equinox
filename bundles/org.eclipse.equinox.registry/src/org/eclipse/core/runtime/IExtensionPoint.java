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
 * An extension point declared in a plug-in. Except for the list of extensions
 * plugged in to it, the information available for an extension point is
 * obtained from the declaring plug-in's manifest (<code>plugin.xml</code>)
 * file.
 * <p>
 * These registry objects are intended for relatively short-term use. Clients
 * that deal with these objects must be aware that they may become invalid if
 * the declaring plug-in is updated or uninstalled. If this happens, all methods
 * except {@link #isValid()} will throw {@link InvalidRegistryObjectException}.
 * For extension point objects, the most common case is code in a plug-in
 * dealing with one of the extension points it declares. These extension point
 * objects are guaranteed to be valid while the plug-in is active. Code in a
 * plug-in that has declared that it is not dynamic aware (or not declared
 * anything) can also safely ignore this issue, since the registry would not be
 * modified while it is active. However, code in a plug-in that declares that it
 * is dynamic aware must be careful if it access the extension point object of a
 * different plug-in, because it's at risk if that other plug-in is removed.
 * Similarly, tools that analyze or display the extension registry are
 * vulnerable. Client code can pre-test for invalid objects by calling
 * {@link #isValid()}, which never throws this exception. However, pre-tests are
 * usually not sufficient because of the possibility of the extension point
 * object becoming invalid as a result of a concurrent activity. At-risk clients
 * must treat <code>InvalidRegistryObjectException</code> as if it were a
 * checked exception. Also, such clients should probably register a listener
 * with the extension registry so that they receive notification of any changes
 * to the registry.
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
public interface IExtensionPoint {
	/**
	 * Returns all configuration elements from all extensions configured into this
	 * extension point. Returns an empty array if this extension point has no
	 * extensions configured, or none of the extensions contain configuration
	 * elements.
	 *
	 * @return the configuration elements for all extension configured into this
	 *         extension point
	 * @throws InvalidRegistryObjectException if this extension point is no longer
	 *                                        valid
	 */
	public IConfigurationElement[] getConfigurationElements() throws InvalidRegistryObjectException;

	/**
	 * Returns the namespace for this extension point. This value can be used in
	 * various global facilities to discover this extension point's provider.
	 *
	 * @return the namespace for this extension point
	 * @throws InvalidRegistryObjectException if this extension point is no longer
	 *                                        valid
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
	public String getNamespace() throws InvalidRegistryObjectException;

	/**
	 * Returns the namespace name for this extension point.
	 *
	 * @return the namespace name for this extension point
	 * @throws InvalidRegistryObjectException if this extension point is no longer
	 *                                        valid
	 * @since org.eclipse.equinox.registry 3.2
	 */
	public String getNamespaceIdentifier() throws InvalidRegistryObjectException;

	/**
	 * Returns the contributor of this extension point.
	 *
	 * @return the contributor for this extension point
	 * @throws InvalidRegistryObjectException if this extension point is no longer
	 *                                        valid
	 * @since org.eclipse.equinox.registry 3.2
	 */
	public IContributor getContributor() throws InvalidRegistryObjectException;

	/**
	 * Returns the extension with the given unique identifier configured into this
	 * extension point, or <code>null</code> if there is no such extension. Since an
	 * extension might not have an identifier, some extensions can only be found via
	 * the <code>getExtensions</code> method.
	 *
	 * @param extensionId the unique identifier of an extension (e.g.
	 *                    <code>"com.example.acme.main"</code>).
	 * @return an extension, or <code>null</code>
	 * @throws InvalidRegistryObjectException if this extension point is no longer
	 *                                        valid
	 */
	public IExtension getExtension(String extensionId) throws InvalidRegistryObjectException;

	/**
	 * Returns all extensions configured into this extension point. Returns an empty
	 * array if this extension point has no extensions.
	 *
	 * @return the extensions configured into this extension point
	 * @throws InvalidRegistryObjectException if this extension point is no longer
	 *                                        valid
	 */
	public IExtension[] getExtensions() throws InvalidRegistryObjectException;

	/**
	 * Returns a displayable label for this extension point. Returns the empty
	 * string if no label for this extension point is specified in the plug-in
	 * manifest file.
	 * <p>
	 * Note that any translation specified in the plug-in manifest file is
	 * automatically applied.
	 * </p>
	 *
	 * @return a displayable string label for this extension point, possibly the
	 *         empty string
	 * @throws InvalidRegistryObjectException if this extension point is no longer
	 *                                        valid
	 */
	public String getLabel() throws InvalidRegistryObjectException;

	/**
	 * When multi-language support is enabled, this method returns a displayable
	 * label for this extension point in the specified locale. Returns the empty
	 * string if no label for this extension point is specified in the plug-in
	 * manifest file.
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
	 * @return a displayable string label for this extension point, possibly the
	 *         empty string
	 * @throws InvalidRegistryObjectException if this extension point is no longer
	 *                                        valid
	 * @see IExtensionRegistry#isMultiLanguage()
	 * @since 3.5
	 */
	public String getLabel(String locale) throws InvalidRegistryObjectException;

	/**
	 * Returns reference to the extension point schema. The schema reference is
	 * returned as a URL path relative to the plug-in installation URL. Returns the
	 * empty string if no schema for this extension point is specified in the
	 * plug-in manifest file.
	 *
	 * @return a relative URL path, or an empty string
	 * @throws InvalidRegistryObjectException if this extension point is no longer
	 *                                        valid
	 */
	public String getSchemaReference() throws InvalidRegistryObjectException;

	/**
	 * Returns the simple identifier of this extension point. This identifier is a
	 * non-empty string containing no period characters (<code>'.'</code>) and is
	 * guaranteed to be unique within the namespace.
	 *
	 * @return the simple identifier of the extension point (e.g.
	 *         <code>"builders"</code>)
	 * @throws InvalidRegistryObjectException if this extension point is no longer
	 *                                        valid
	 */
	public String getSimpleIdentifier() throws InvalidRegistryObjectException;

	/**
	 * Returns the unique identifier of this extension point. This identifier is
	 * unique within the plug-in registry, and is composed of the namespace for this
	 * extension point and this extension point's simple identifier.
	 *
	 *
	 * @return the unique identifier of the extension point (e.g.
	 *         <code>"org.eclipse.core.resources.builders"</code>)
	 * @throws InvalidRegistryObjectException if this extension point is no longer
	 *                                        valid
	 */
	public String getUniqueIdentifier() throws InvalidRegistryObjectException;

	/*
	 * (non-javadoc)
	 * 
	 * @see Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o);

	/**
	 * Returns whether this extension point object is valid.
	 *
	 * @return <code>true</code> if the object is valid, and <code>false</code> if
	 *         it is no longer valid
	 * @since 3.1
	 */
	public boolean isValid();
}
