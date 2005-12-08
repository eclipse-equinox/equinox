/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.registry;

import java.io.InputStream;
import java.util.EventListener;
import java.util.ResourceBundle;

/**
 * The extension registry holds the master list of all
 * discovered namespaces, extension points and extensions. 
 * <p>
 * The extension registry can be queried, by name, for 
 * extension points and extensions.  
 * </p>
 * <p>
 * The various objects that describe the contents of the extension registry
 * ({@link IExtensionPoint}, {@link IExtension}, and {@link IConfigurationElement})
 * are intended for relatively short-term use. Clients that deal with these objects
 * must be aware that they may become invalid if the declaring plug-in is updated
 * or uninstalled. If this happens, all methods on these object except
 * <code>isValid()</code> will throw {@link InvalidRegistryObjectException}.
 * Code in a plug-in that has declared that it is not dynamic aware (or not declared
 * anything) can safely ignore this issue, since the registry would not be
 * modified while it is active. However, code in a plug-in that declares that it
 * is dynamic aware must be careful if it accesses extension registry objects,
 * because it's at risk if plug-in are removed. Similarly, tools that analyze
 * or display the extension registry are vulnerable. Client code can pre-test for
 * invalid objects by calling <code>isValid()</code>, which never throws this exception.
 * However, pre-tests are usually not sufficient because of the possibility of the
 * extension registry object becoming invalid as a result of a concurrent activity.
 * At-risk clients must treat <code>InvalidRegistryObjectException</code> as if it
 * were a checked exception. Also, such clients should probably register a listener
 * with the extension registry so that they receive notification of any changes to
 * the registry.
 * </p>
 * <p>
 * Extensions and extension points are declared by generic entities called 
 * <cite>namespaces</cite>. The only fact known about namespaces is that they 
 * have unique string-based identifiers. One example of a namespace 
 * is a plug-in, for which the namespace id is the plug-in id.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @since 3.0
 */
public interface IExtensionRegistry {
	/**
	 * Adds the given listener for registry change events related to extension points 
	 * in the given namespace.
	 * Has no effect if an identical listener is already registered. After 
	 * completion of this method, the given listener will be registered for events 
	 * related to extension points in the specified namespace. If no namespace 
	 * is specified, the listener will receive notifications for changes to 
	 * extension points in any namespace.  
	 * <p>
	 * Once registered, a listener starts receiving notification of changes to
	 *  the registry. Registry change notifications are sent asynchronously.
	 * The listener continues to receive notifications until it is removed. 
	 * </p>
	 * @param listener the listener
	 * @param namespace the namespace in which to listen for changes
	 * @see IRegistryChangeListener
	 * @see IRegistryChangeEvent
	 * @see #removeRegistryChangeListener(IRegistryChangeListener) 
	 */
	public void addRegistryChangeListener(EventListener listener, String namespace);

	/**
	 * Adds the given listener for registry change events.
	 * Has no effect if an identical listener is already registered.
	 * 
	 * <p>
	 * This method is equivalent to:
	 * <pre>
	 *     addRegistryChangeListener(listener,null);
	 * </pre>
	 * </p>
	 * 
	 * @param listener the listener
	 * @see IRegistryChangeListener
	 * @see IRegistryChangeEvent
	 * @see #addRegistryChangeListener(IRegistryChangeListener, String)
	 * @see #removeRegistryChangeListener(IRegistryChangeListener)
	 */
	public void addRegistryChangeListener(EventListener listener);

	/**
	 * Returns all configuration elements from all extensions configured
	 * into the identified extension point. Returns an empty array if the extension 
	 * point does not exist, has no extensions configured, or none of the extensions 
	 * contain configuration elements.
	 *
	 * @param extensionPointId the unique identifier of the extension point
	 *		(e.g. <code>"org.eclipse.core.resources.builders"</code>)
	 * @return the configuration elements
	 */
	public IConfigurationElement[] getConfigurationElementsFor(String extensionPointId);

	/**
	 * Returns all configuration elements from all extensions configured
	 * into the identified extension point. Returns an empty array if the extension 
	 * point does not exist, has no extensions configured, or none of the extensions 
	 * contain configuration elements.
	 *
	 * @param namespace the namespace for the extension point 
	 *		(e.g. <code>"org.eclipse.core.resources"</code>)
	 * @param extensionPointName the simple identifier of the 
	 *		extension point (e.g. <code>"builders"</code>)
	 * @return the configuration elements
	 */
	public IConfigurationElement[] getConfigurationElementsFor(String namespace, String extensionPointName);

	/**
	 * Returns all configuration elements from the identified extension.
	 * Returns an empty array if the extension does not exist or 
	 * contains no configuration elements.
	 *
	 * @param namespace the namespace for the extension point 
	 *		(e.g. <code>"org.eclipse.core.resources"</code>)
	 * @param extensionPointName the simple identifier of the 
	 *		extension point (e.g. <code>"builders"</code>)
	 * @param extensionId the unique identifier of the extension 
	 *		(e.g. <code>"com.example.acme.coolbuilder</code>)
	 * @return the configuration elements
	 */
	public IConfigurationElement[] getConfigurationElementsFor(String namespace, String extensionPointName, String extensionId);

	/**
	 * Returns the specified extension in this extension registry, 
	 * or <code>null</code> if there is no such extension.
	 *
	 * @param extensionId the unique identifier of the extension 
	 *		(e.g. <code>"com.example.acme.coolbuilder"</code>)
	 * @return the extension, or <code>null</code>
	 */
	public IExtension getExtension(String extensionId);

	/**
	 * Returns the specified extension in this extension registry, 
	 * or <code>null</code> if there is no such extension.
	 * The first parameter identifies the extension point, and the second
	 * parameter identifies an extension plugged in to that extension point.
	 *
	 * @param extensionPointId the unique identifier of the extension point
	 *		(e.g. <code>"org.eclipse.core.resources.builders"</code>)
	 * @param extensionId the unique identifier of the extension 
	 *		(e.g. <code>"com.example.acme.coolbuilder"</code>)
	 * @return the extension, or <code>null</code>
	 */
	public IExtension getExtension(String extensionPointId, String extensionId);

	/**
	 * Returns the specified extension in this extension registry, 
	 * or <code>null</code> if there is no such extension.
	 * The first two parameters identify the extension point, and the third
	 * parameter identifies an extension plugged in to that extension point.
	 *
	 * @param namespace the namespace for the extension point 
	 *		(e.g. <code>"org.eclipse.core.resources"</code>)
	 * @param extensionPointName the simple identifier of the 
	 *		extension point (e.g. <code>"builders"</code>)
	 * @param extensionId the unique identifier of the extension 
	 *		(e.g. <code>"com.example.acme.coolbuilder"</code>)
	 * @return the extension, or <code>null</code>
	 */
	public IExtension getExtension(String namespace, String extensionPointName, String extensionId);

	/**
	 * Returns the extension point with the given extension point identifier
	 * in this extension registry, or <code>null</code> if there is no such
	 * extension point.
	 *
	 * @param extensionPointId the unique identifier of the extension point 
	 *    (e.g., <code>"org.eclipse.core.resources.builders"</code>)
	 * @return the extension point, or <code>null</code>
	 */
	public IExtensionPoint getExtensionPoint(String extensionPointId);

	/**
	 * Returns the extension point in this extension registry
	 * with the given namespace and extension point simple identifier,
	 * or <code>null</code> if there is no such extension point.
	 *
	 * @param namespace the namespace for the given extension point 
	 *		(e.g. <code>"org.eclipse.core.resources"</code>)
	 * @param extensionPointName the simple identifier of the 
	 *		extension point (e.g. <code>" builders"</code>)
	 * @return the extension point, or <code>null</code>
	 */
	public IExtensionPoint getExtensionPoint(String namespace, String extensionPointName);

	/**
	 * Returns all extension points known to this extension registry.
	 * Returns an empty array if there are no extension points.
	 *
	 * @return the extension points known to this extension registry
	 */
	public IExtensionPoint[] getExtensionPoints();

	/**
	 * Returns all extension points declared in the given namespace. Returns an empty array if 
	 * there are no extension points declared in the namespace.
	 * 
	 * @param namespace the namespace for the extension points 
	 *		(e.g. <code>"org.eclipse.core.resources"</code>) 
	 * @return the extension points in this registry declared in the given namespace 
	 */
	public IExtensionPoint[] getExtensionPoints(String namespace);

	/**
	 * Returns all extensions declared in the given namespace. Returns an empty array if 
	 * no extensions are declared in the namespace.
	 * 
	 * @param namespace the namespace for the extensions 
	 *		(e.g. <code>"org.eclipse.core.resources"</code>)
	 * @return the extensions in this registry declared in the given namespace 
	 */
	public IExtension[] getExtensions(String namespace);

	/**
	 * Returns all namespaces where extensions and/or extension points. Returns an 
	 * empty array if there are no known extensions/extension points in this registry.
	 * 
	 * @return all namespaces known to this registry
	 * @since 3.0 
	 */
	//TODO This needs to be clarified.
	public String[] getNamespaces();

	/** 
	 * Removes the given registry change listener from this registry.
	 * Has no effect if an identical listener is not registered.
	 *
	 * @param listener the listener
	 * @see IRegistryChangeListener
	 * @see #addRegistryChangeListener(IRegistryChangeListener)
	 * @see #addRegistryChangeListener(IRegistryChangeListener, String)
	 */
	public void removeRegistryChangeListener(EventListener listener);

	/**
	 * Adds to the extension registry an extension point(s), extension(s), or 
	 * a combination of those described by the XML file.
	 * 
	 * If registry is no modifiable, this method is an access controlled method. 
	 * Proper token is required for non-modifiable registries.
	 * 
	 * @see RegistryStrategy.isModifiable
	 * 
	 * @param is - stream open on the XML file. The XML file can contain an extension
	 * poin(s) or/and extension(s) described in the format similar to plugin.xml 
	 * @param contributorId - ID of the supplier of this contribution
	 * @param name - optional name of the contribution. Used for error reporting; might be null
	 * @param translationBundle - optional resource bundle used for translations; might be null 
	 * @param token - the key used to check permissions. The registry had two keys specified in its
	 * creation {@link RegistryFactory#createRegistry()}: master token and a user token. Use the
	 * user token to specify that contribution has dynamic nature. If registry is created with
	 * a registry strategy that specified isModifiable() as "true", null can be passed instead of
	 * a token.
	 * @return - true: the contribution was successfully processed; false - error in 
	 * the processing of the contribution
	 */
	public boolean addContribution(InputStream is, long contributorId, String name, ResourceBundle translationBundle, Object token);

	/**
	 * Call this method to properly stop the registry. It stops registry event processing
	 * and writes out cache information to be used in the next run.
	 * 
	 * This is an access controlled method; proper token is required.
	 * 
	 * @param registry - the registry to be stopped
	 * @param token - control key for the registry (should be the same key as used in 
	 * the RegistryManager#createExtensionRegistry() of this registry
	 */
	// XXX which token should be supplied here.  Master or other?  
	public void stop(Object token);
}
