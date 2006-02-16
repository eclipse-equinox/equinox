/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.spi;

import java.io.File;
import java.util.Map;
import java.util.ResourceBundle;
import javax.xml.parsers.SAXParserFactory;
import org.eclipse.core.internal.registry.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

/**
 * This is the basic registry strategy. It describes how registry does logging,
 * message translation, namespace resolution, extra start/stop processing, event scheduling,
 * caching, and debugging.
 * <p>
 * In this strategy:
 * </p><p><ul>
 * <li>Clients can add information into the registry</li>
 * <li>Caching is enabled and doesn't use state or time stamp validation; no alternative cache location is supplied</li>
 * <li>Logging is done onto System.out</li>
 * <li>Registry contributors properties are determined by the toString() value of the defining objects</li>
 * <li>Standard Java class loading is used to create executable extensions</li>
 * </ul></p><p>
 * This class can be overridden and/or instantiated by clients. 
 * </p><p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under 
 * development and expected to change significantly before reaching stability. 
 * It is being made available at this early stage to solicit feedback from pioneering 
 * adopters on the understanding that any code that uses this API will almost certainly 
 * be broken (repeatedly) as the API evolves.
 * </p>
 * @since org.eclipse.equinox.registry 3.2
 */
public class RegistryStrategy {

	/**
	 * File system directory to store cache files; might be null
	 */
	private final File theStorageDir;

	/**
	 * Specifies if the registry file cache is read only
	 */
	private final boolean cacheReadOnly;

	/**
	 * Public constructor.
	 * 
	 * @param theStorageDir - file system directory to store cache files; might be null
	 * @param cacheReadOnly - true: cache is read only; false: cache is read/write
	 */
	public RegistryStrategy(File theStorageDir, boolean cacheReadOnly) {
		this.theStorageDir = theStorageDir;
		this.cacheReadOnly = cacheReadOnly;
	}

	public final File getStorage() {
		return theStorageDir;
	}

	public final boolean isCacheReadOnly() {
		return cacheReadOnly;
	}

	/**
	 * Specifies if registry clients can add information into the registry. 
	 * @return true: clients can add information; false: proper token should be supplied
	 * in order to add information into the registry. 
	 */
	public boolean isModifiable() {
		return true;
	}

	/**
	 * Adds a log entry based on the supplied status.
	 * This method writes a message to the System.out in the following format:
	 * 
	 * [Error|Warning|Log]: Main error message
	 * [Error|Warning|Log]: Child error message 1
	 * 	...
	 * [Error|Warning|Log]: Child error message N
	 * 
	 * @param status - the status to log
	 */
	public void log(IStatus status) {
		RegistrySupport.log(status, null);
	}

	/**
	 * Translates key using the supplied resource bundle. The resource bundle is 
	 * optional and might be null.
	 * The default translation routine assumes that keys are prefixed with '%'. If 
	 * no resource bundle is present, the key itself (without leading '%') is returned. 
	 * There is no decoding for the leading '%%'.
	 * 
	 * @param key - message key to be translated
	 * @param resources - resource bundle, might be null
	 * @return - translated string
	 */
	public String translate(String key, ResourceBundle resources) {
		return RegistrySupport.translate(key, resources);
	}

	/**
	 * Override this method to provide additional processing performed 
	 * at the end of the registry's constructor.
	 * 
	 * @param registry - the extension registry being created
	 */
	public void onStart(Object registry) {
		// The default implementation
	}

	/**
	 * Override this method to provide additional processing before 
	 * the extension registry's stop() method is executed.
	 * 
	 * @param registry - the extension registry being stopped
	 */
	public void onStop(Object registry) {
		// The default implementation
	}

	/**
	 * Creates an executable extension. Override this method to supply an alternative processing 
	 * for the creation of executable extensions. 
	 * <p>
	 * This method recieves the contributor of the executable extension and, possibly,
	 * an optional contributor name (if specified by the executable extension).  
	 * </p><p>
	 * In this implementation registry attempts to instantiate the class specified using 
	 * standard Java reflection mechanism; it assumes that constructor of such class has no arguments.
	 * </p>
	 * @see IConfigurationElement#createExecutableExtension(String)
	 * 
	 * @param contributor - contributor of this executable extension
	 * @param className - name of the class to be instantiated
	 * @param overridenContributorName - optionally executable extension can request a specific contributor
	 * to be used; might be null
	 * @return - the object created; might be null
	 * @throws CoreException
	 * @see IExecutableExtension
	 */
	public Object createExecutableExtension(RegistryContributor contributor, String className, String overridenContributorName) throws CoreException {
		Object result = null;
		Class classInstance = null;
		try {
			classInstance = Class.forName(className);
		} catch (ClassNotFoundException e1) {
			String message = NLS.bind(RegistryMessages.exExt_findClassError, contributor.getActualName(), className);
			throw new CoreException(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, IRegistryConstants.PLUGIN_ERROR, message, e1));
		}

		try {
			result = classInstance.newInstance();
		} catch (Exception e) {
			String message = NLS.bind(RegistryMessages.exExt_instantiateClassError, contributor.getActualName(), className);
			throw new CoreException(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, IRegistryConstants.PLUGIN_ERROR, message, e));
		}
		return result;
	}

	/**
	 * Override this method to customize scheduling of an extension registry event. Note that this method 
	 * must make the following call to actually process the event:
	 * 
	 * 	RegistryStrategy.processChangeEvent(listeners, deltas, registry);
	 * 
	 * In this implementation of the method registry events are executed in a queue 
	 * on a separate thread (i.e. asynchronously, sequentially).
	 * 
	 * @param listeners - list of active listeners (thread safe)
	 * @param deltas - registry deltas (thread safe)
	 * @param registry - the extension registry (NOT thread safe)
	 */
	public void scheduleChangeEvent(Object[] listeners, Map deltas, Object registry) {
		if (registry instanceof ExtensionRegistry)
			((ExtensionRegistry) registry).scheduleChangeEvent(listeners, deltas);
	}

	/**
	 * This method performs actual processing of the registry change event. It should 
	 * only be used by overrides of the RegistryStrategy.scheduleChangeEvent.
	 * 
	 * @param listeners - the list of active listeners
	 * @param deltas - the extension registry deltas
	 * @param registry - the extension registry
	 * @return - status of the operation; null if unexpected type of the registry 
	 * was encountered
	 */
	public final static IStatus processChangeEvent(Object[] listeners, Map deltas, Object registry) {
		if (registry instanceof ExtensionRegistry)
			return ((ExtensionRegistry) registry).processChangeEvent(listeners, deltas);
		return null;
	}

	/**
	 * Override this method to specify debug requirements to the registry. In this
	 * default implementation debug functionality is turned off.
	 * 
	 * Note that, in general case, the extension registry plugin doesn't depend on OSGI and,
	 * therefore, can't use Eclipse .options files to discover debug options.
	 * 
	 * @return true - perform debug logging and validation
	 */
	public boolean debug() {
		return false;
	}

	/**
	 * Override this method to specify debug requirements for the registry event processing. 
	 * In this default implementation debug functionality is turned off.
	 * 
	 * Note that, in general case, the extension registry plugin doesn't depend on OSGI and,
	 * therefore, can't use Eclipse .options files to discover debug options.
	 * 
	 * @return true - perform debug logging and validation of the registry events
	 */
	public boolean debugRegistryEvents() {
		return false;
	}

	/**
	 * Specifies if the extension registry should use cache to store
	 * registry data between invocations.
	 * 
	 * This basic implementation specifies that registry caching is going to tbe enabled.
	 *  
	 * @return - true - should use cache; false - don't
	 */
	public boolean cacheUse() {
		return true;
	}

	/**
	 * Specifies if lazy cache loading is used. If cache startegy is not supplied,
	 * lazy cache loading is used.
	 * 
	 * This basic implementation specifies that lazy cache loading is going to be used.
	 * 
	 * @return - true - use lazy extension registry cache loading
	 */
	public boolean cacheLazyLoading() {
		return true;
	}

	/**
	 * This method is called as a part of the registry cache validation. The cache is valid
	 * on the registry startup if the pair {state, time stamp} supplied by the application 
	 * is the same as the {state, time stamp} saved in the registry cache.
	 * 
	 * This method produces a number that corresponds to the current state of the data stored 
	 * in the registry. Increment the state if registry content changed and the registry cache 
	 * is no longer valid. 
	 * 
	 * Return 0 to indicate that state verification is not required.
	 * 
	 * @return number indicating state of the application data
	 */
	public long cacheComputeState() {
		return 0;
	}

	/**
	 * This method is called as a part of the registry cache validation. The cache is valid
	 * on the registry startup if the pair {state, time stamp} supplied by the application 
	 * is the same as the {state, time stamp} saved in the registry cache.
	 * 
	 * This method calculates current time stamp for the elements stored in the extension 
	 * registry. Treat this number as a hash code for the data stored in the registry. 
	 * It stays the same as long as the registry content is not changing. It becomes a different 
	 * number as the registry content gets modified.
	 * 
	 * Return 0 to indicate that no time stamp verification is required. 
	 * 
	 * @return the time stamp calculated with the application data
	 */
	public long cacheComputeTimeStamp() {
		return 0;
	}

	/**
	 * In case if the primary cache location has no data in it, the registry
	 * attemps to get cached information from this alternative location. The cache
	 * at alternative location is always considered read-only.
	 * 
	 * Return null if alternative cache location is not supported.
	 * 
	 * @return - directory containing the alternative cache location
	 */
	public File cacheAlternativeLocation() {
		return null;
	}

	private SAXParserFactory theXMLParserFactory = null;

	/**
	 * The parser used by the registry to parse descriptions of extension points
	 * and extensions from the XML input streams.
	 * 
	 * @see org.eclipse.core.runtime.IExtensionRegistry#addContribution(java.io.InputStream, String, boolean, String, ResourceBundle, Object)
	 * 
	 * @return XML parser
	 */
	public SAXParserFactory getXMLParser() {
		if (theXMLParserFactory == null)
			theXMLParserFactory = SAXParserFactory.newInstance();
		return theXMLParserFactory;
	}
}
