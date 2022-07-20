/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 577497 - Support version-specific entries in dev-classPath file
 *******************************************************************************/
package org.eclipse.osgi.internal.framework;

import static org.osgi.framework.Constants.FRAMEWORK_LANGUAGE;
import static org.osgi.framework.Constants.FRAMEWORK_OS_NAME;
import static org.osgi.framework.Constants.FRAMEWORK_OS_VERSION;
import static org.osgi.framework.Constants.FRAMEWORK_PROCESSOR;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT;
import static org.osgi.framework.Constants.FRAMEWORK_UUID;
import static org.osgi.framework.Constants.FRAMEWORK_VENDOR;
import static org.osgi.framework.Constants.SUPPORTS_FRAMEWORK_EXTENSION;
import static org.osgi.framework.Constants.SUPPORTS_FRAMEWORK_FRAGMENT;
import static org.osgi.framework.Constants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.core.runtime.internal.adaptor.ConsoleManager;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.container.InternalUtils;
import org.eclipse.osgi.internal.debug.Debug;
import org.eclipse.osgi.internal.debug.FrameworkDebugOptions;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.eclipse.osgi.internal.location.LocationHelper;
import org.eclipse.osgi.internal.log.EquinoxLogServices;
import org.eclipse.osgi.internal.messages.Msg;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleRevision;

/**
 * Internal class.
 */
public class EquinoxConfiguration implements EnvironmentInfo {
	private static final String CONFIG_FILE = "config.ini"; //$NON-NLS-1$
	// While we recognize the SunOS operating system, we change
	// this internally to be Solaris.
	private static final String INTERNAL_OS_SUNOS = "SunOS"; //$NON-NLS-1$
	private static final String INTERNAL_OS_LINUX = "Linux"; //$NON-NLS-1$
	private static final String INTERNAL_OS_MACOSX = "Mac OS"; //$NON-NLS-1$
	private static final String INTERNAL_OS_AIX = "AIX"; //$NON-NLS-1$
	private static final String INTERNAL_OS_HPUX = "HP-UX"; //$NON-NLS-1$
	private static final String INTERNAL_OS_QNX = "QNX"; //$NON-NLS-1$
	private static final String INTERNAL_OS_OS400 = "OS/400"; //$NON-NLS-1$
	private static final String INTERNAL_OS_OS390 = "OS/390"; //$NON-NLS-1$
	private static final String INTERNAL_OS_ZOS = "z/OS"; //$NON-NLS-1$
	private static final String INTERNAL_OS_FREEBSD = "FreeBSD"; //$NON-NLS-1$
	// While we recognize the i386 architecture, we change
	// this internally to be x86.
	private static final String INTERNAL_ARCH_I386 = "i386"; //$NON-NLS-1$
	// While we recognize the amd64 architecture, we change
	// this internally to be x86_64.
	private static final String INTERNAL_AMD64 = "amd64"; //$NON-NLS-1$

	public static final String VARIABLE_DELIM_STRING = "$"; //$NON-NLS-1$
	public static final char VARIABLE_DELIM_CHAR = '$';

	private static final String DEV_FILE_ENTRY_VERSION_SEPARATOR = ";"; //$NON-NLS-1$

	private final ConfigValues configValues;

	private final Debug debug;
	private final DebugOptions debugOptions;
	private final HookRegistry hookRegistry;
	private final AliasMapper aliasMapper = new AliasMapper();
	private final EquinoxLocations equinoxLocations;

	private volatile String[] allArgs;
	private volatile String[] frameworkArgs;
	private volatile String[] appArgs;

	// dev mode fields
	private final boolean inDevelopmentMode;
	private final File devLocation;
	private final Object devMonitor = new Object();
	private String[] devDefaultClasspath;
	private Map<String, String> devProperties = null;
	// timestamp for the dev.properties file
	private long devLastModified = 0;

	public final boolean contextBootDelegation;
	public final boolean compatibilityBootDelegation;
	public final boolean compatibilityLazyTriggerOnFailLoad;

	public final List<String> LIB_EXTENSIONS;
	public final List<String> ECLIPSE_LIB_VARIANTS;
	public final boolean COPY_NATIVES;
	public final List<String> ECLIPSE_NL_JAR_VARIANTS;
	public final boolean DEFINE_PACKAGE_ATTRIBUTES;
	public final boolean BUNDLE_SET_TCCL;

	public final int BSN_VERSION;
	public static final int BSN_VERSION_SINGLE = 1;
	public static final int BSN_VERSION_MULTIPLE = 2;
	public static final int BSN_VERSION_MANAGED = 3;

	public final boolean throwErrorOnFailedStart;

	public final boolean CLASS_CERTIFICATE;
	public final boolean PARALLEL_CAPABLE;

	private final Map<Throwable, Integer> exceptions = new LinkedHashMap<>(0);

	// JVM os.arch property name
	public static final String PROP_JVM_OS_ARCH = "os.arch"; //$NON-NLS-1$
	// JVM os.name property name
	public static final String PROP_JVM_OS_NAME = "os.name"; //$NON-NLS-1$
	// JVM os.version property name
	public static final String PROP_JVM_OS_VERSION = "os.version"; //$NON-NLS-1$
	public static final String PROP_JVM_SPEC_VERSION = "java.specification.version"; //$NON-NLS-1$
	public static final String PROP_JVM_SPEC_NAME = "java.specification.name"; //$NON-NLS-1$

	public static final String PROP_SETPERMS_CMD = "osgi.filepermissions.command"; //$NON-NLS-1$
	public static final String PROP_DEBUG = "osgi.debug"; //$NON-NLS-1$
	public static final String PROP_DEBUG_VERBOSE = "osgi.debug.verbose"; //$NON-NLS-1$
	public static final String PROP_DEV = "osgi.dev"; //$NON-NLS-1$
	public static final String PROP_CLEAN = "osgi.clean"; //$NON-NLS-1$
	public static final String PROP_USE_SYSTEM_PROPERTIES = "osgi.framework.useSystemProperties"; //$NON-NLS-1$
	public static final String PROP_FRAMEWORK = "osgi.framework"; //$NON-NLS-1$

	public static final String ECLIPSE_FRAMEWORK_VENDOR = "Eclipse"; //$NON-NLS-1$

	public static final String PROP_OSGI_JAVA_PROFILE = "osgi.java.profile"; //$NON-NLS-1$
	public static final String PROP_OSGI_JAVA_PROFILE_NAME = "osgi.java.profile.name"; //$NON-NLS-1$
	// OSGi java profile bootdelegation; used to indicate how the org.osgi.framework.bootdelegation
	// property defined in the java profile should be processed, (ingnore, override, none). default is ignore
	public static final String PROP_OSGI_JAVA_PROFILE_BOOTDELEGATION = "osgi.java.profile.bootdelegation"; //$NON-NLS-1$
	// indicates that the org.osgi.framework.bootdelegation in the java profile should be ingored
	public static final String PROP_OSGI_BOOTDELEGATION_IGNORE = "ignore"; //$NON-NLS-1$
	// indicates that the org.osgi.framework.bootdelegation in the java profile should override the system property
	public static final String PROP_OSGI_BOOTDELEGATION_OVERRIDE = "override"; //$NON-NLS-1$
	// indicates that the org.osgi.framework.bootdelegation in the java profile AND the system properties should be ignored
	public static final String PROP_OSGI_BOOTDELEGATION_NONE = "none"; //$NON-NLS-1$

	public static final String PROP_CONTEXT_BOOTDELEGATION = "osgi.context.bootdelegation"; //$NON-NLS-1$
	public static final String PROP_COMPATIBILITY_BOOTDELEGATION = "osgi.compatibility.bootdelegation"; //$NON-NLS-1$
	public static final String PROP_DS_DELAYED_KEEPINSTANCES = "ds.delayed.keepInstances"; //$NON-NLS-1$
	public static final String PROP_COMPATIBILITY_ERROR_FAILED_START = "osgi.compatibility.errorOnFailedStart"; //$NON-NLS-1$
	public static final String PROP_COMPATIBILITY_START_LAZY = "osgi.compatibility.eagerStart.LazyActivation"; //$NON-NLS-1$
	public static final String PROP_COMPATIBILITY_START_LAZY_ON_FAIL_CLASSLOAD = "osgi.compatibility.trigger.lazyActivation.onFailedClassLoad"; //$NON-NLS-1$

	public static final String PROP_OSGI_OS = "osgi.os"; //$NON-NLS-1$
	public static final String PROP_OSGI_WS = "osgi.ws"; //$NON-NLS-1$
	public static final String PROP_OSGI_ARCH = "osgi.arch"; //$NON-NLS-1$
	public static final String PROP_OSGI_NL = "osgi.nl"; //$NON-NLS-1$
	public static final String PROP_OSGI_NL_USER = "osgi.nl.user"; //$NON-NLS-1$

	public static final String PROP_ROOT_LOCALE = "equinox.root.locale"; //$NON-NLS-1$

	public static final String PROP_PARENT_CLASSLOADER = "osgi.parentClassloader"; //$NON-NLS-1$
	// A parent classloader type that specifies the framework classlaoder
	public static final String PARENT_CLASSLOADER_FWK = "fwk"; //$NON-NLS-1$
	// System property used to set the context classloader parent classloader type (ccl is the default)
	public static final String PROP_CONTEXTCLASSLOADER_PARENT = "osgi.contextClassLoaderParent"; //$NON-NLS-1$
	public static final String CONTEXTCLASSLOADER_PARENT_APP = "app"; //$NON-NLS-1$
	public static final String CONTEXTCLASSLOADER_PARENT_EXT = "ext"; //$NON-NLS-1$
	public static final String CONTEXTCLASSLOADER_PARENT_BOOT = "boot"; //$NON-NLS-1$
	public static final String CONTEXTCLASSLOADER_PARENT_FWK = "fwk"; //$NON-NLS-1$

	public static final String PROP_FRAMEWORK_LIBRARY_EXTENSIONS = "osgi.framework.library.extensions"; //$NON-NLS-1$
	public static final String PROP_COPY_NATIVES = "osgi.classloader.copy.natives"; //$NON-NLS-1$
	public static final String PROP_DEFINE_PACKAGES = "osgi.classloader.define.packages"; //$NON-NLS-1$
	public static final String PROP_BUNDLE_SETTCCL = "eclipse.bundle.setTCCL"; //$NON-NLS-1$

	public static final String PROP_EQUINOX_SECURITY = "eclipse.security"; //$NON-NLS-1$
	public static final String PROP_FILE_LIMIT = "osgi.bundlefile.limit"; //$NON-NLS-1$

	public final static String PROP_CLASS_CERTIFICATE_SUPPORT = "osgi.support.class.certificate"; //$NON-NLS-1$
	public final static String PROP_CLASS_LOADER_TYPE = "osgi.classloader.type"; //$NON-NLS-1$
	public final static String CLASS_LOADER_TYPE_PARALLEL = "parallel"; //$NON-NLS-1$

	public static final String PROP_FORCED_RESTART = "osgi.forcedRestart"; //$NON-NLS-1$
	public static final String PROP_IGNORE_USER_CONFIGURATION = "eclipse.ignoreUserConfiguration"; //$NON-NLS-1$

	public static final String PROPERTY_STRICT_BUNDLE_ENTRY_PATH = "osgi.strictBundleEntryPath";//$NON-NLS-1$

	public static final String PROP_CHECK_CONFIGURATION = "osgi.checkConfiguration"; //$NON-NLS-1$
	private final boolean inCheckConfigurationMode;

	public static final String DEFAULT_STATE_SAVE_DELAY_INTERVAL = "30000"; //$NON-NLS-1$
	public static final String PROP_STATE_SAVE_DELAY_INTERVAL = "eclipse.stateSaveDelayInterval"; //$NON-NLS-1$

	public static final String PROP_MODULE_LOCK_TIMEOUT = "osgi.module.lock.timeout"; //$NON-NLS-1$
	public static final String PROP_MODULE_AUTO_START_ON_RESOLVE = "osgi.module.auto.start.on.resolve"; //$NON-NLS-1$
	public static final String PROP_ALLOW_RESTRICTED_PROVIDES = "osgi.equinox.allow.restricted.provides"; //$NON-NLS-1$
	public static final String PROP_LOG_HISTORY_MAX = "equinox.log.history.max"; //$NON-NLS-1$
	public static final String PROP_LOG_CAPTURE_ENTRY_LOCATION = "equinox.log.capture.entry.location"; //$NON-NLS-1$

	@Deprecated
	public static final String PROP_RESOLVER_THREAD_COUNT = "equinox.resolver.thead.count"; //$NON-NLS-1$
	public static final String PROP_EQUINOX_RESOLVER_THREAD_COUNT = "equinox.resolver.thread.count"; //$NON-NLS-1$
	public static final String PROP_EQUINOX_START_LEVEL_THREAD_COUNT = "equinox.start.level.thread.count"; //$NON-NLS-1$
	public static final String PROP_EQUINOX_START_LEVEL_RESTRICT_PARALLEL = "equinox.start.level.restrict.parallel"; //$NON-NLS-1$
	public static final String PROP_RESOLVER_REVISION_BATCH_SIZE = "equinox.resolver.revision.batch.size"; //$NON-NLS-1$
	public static final String PROP_RESOLVER_BATCH_TIMEOUT = "equinox.resolver.batch.timeout"; //$NON-NLS-1$

	public static final String PROP_SYSTEM_PROVIDE_HEADER = "equinox.system.provide.header"; //$NON-NLS-1$
	public static final String SYSTEM_PROVIDE_HEADER_ORIGINAL = "original"; //$NON-NLS-1$
	public static final String SYSTEM_PROVIDE_HEADER_SYSTEM = "system"; //$NON-NLS-1$
	public static final String SYSTEM_PROVIDE_HEADER_SYSTEM_EXTRA = "system.extra"; //$NON-NLS-1$

	public static final String PROP_DEFAULT_SUFFIX = ".default"; //$NON-NLS-1$
	public static final Collection<String> PROP_WITH_ECLIPSE_STARTER_DEFAULTS = Collections.unmodifiableList(Arrays.asList(PROP_COMPATIBILITY_BOOTDELEGATION, PROP_DS_DELAYED_KEEPINSTANCES));
	public static final String PROP_INIT_UUID = "equinox.init.uuid"; //$NON-NLS-1$

	public static final String PROP_ACTIVE_THREAD_TYPE = "osgi.framework.activeThreadType"; //$NON-NLS-1$
	public static final String ACTIVE_THREAD_TYPE_NORMAL = "normal"; //$NON-NLS-1$

	public static final String PROP_GOSH_ARGS = "gosh.args"; //$NON-NLS-1$

	public static final String PROP_SECURE_UUID = "equinox.uuid.secure"; //$NON-NLS-1$

	public final static String SIGNED_BUNDLE_SUPPORT = "osgi.support.signature.verify"; //$NON-NLS-1$
	public final static String SIGNED_CONTENT_SUPPORT = "osgi.signedcontent.support"; //$NON-NLS-1$
	public static final int SIGNED_CONTENT_VERIFY_CERTIFICATE = 0x01;
	public static final int SIGNED_CONTENT_VERIFY_TRUST = 0x02;
	public static final int SIGNED_CONTENT_VERIFY_RUNTIME = 0x04;
	public static final int SIGNED_CONTENT_VERIFY_ALL = SIGNED_CONTENT_VERIFY_CERTIFICATE | SIGNED_CONTENT_VERIFY_TRUST
			| SIGNED_CONTENT_VERIFY_RUNTIME;
	private final static String SIGNED_CONTENT_SUPPORT_CERTIFICATE = "certificate"; //$NON-NLS-1$
	private final static String SIGNED_CONTENT_SUPPORT_TRUST = "trust"; //$NON-NLS-1$
	private final static String SIGNED_CONTENT_SUPPORT_RUNTIME = "runtime"; //$NON-NLS-1$
	private final static String SIGNED_CONTENT_SUPPORT_ALL = "all"; //$NON-NLS-1$
	private final static String SIGNED_CONTENT_SUPPORT_TRUE = "true"; //$NON-NLS-1$
	public final int supportSignedBundles;
	public final boolean runtimeVerifySignedBundles;

	public static final class ConfigValues {
		/**
		 * Value of {@link #localConfig} properties that should be considered
		 * <code>null</code> and for which access should not fall back to
		 * {@link System#getProperty(String)}.
		 * The instance must be compared by identity (==, not equals) and must not
		 * be leaked outside this class.
		 */
		private final static String NULL_CONFIG = new String("org.eclipse.equinox.configuration.null.value"); //$NON-NLS-1$
		private final static Collection<String> populateInitConfig = Arrays.asList(PROP_OSGI_ARCH, PROP_OSGI_OS, PROP_OSGI_WS, PROP_OSGI_NL, FRAMEWORK_OS_NAME, FRAMEWORK_OS_VERSION, FRAMEWORK_PROCESSOR, FRAMEWORK_LANGUAGE);

		private final boolean useSystemProperties;
		private final Map<String, Object> initialConfig;
		private final Properties localConfig;

		private final Map<Throwable, Integer> exceptions;

		public ConfigValues(Map<String, ?> initialConfiguration, Map<Throwable, Integer> exceptions) {
			this.exceptions = exceptions;
			this.initialConfig = initialConfiguration == null ? new HashMap<>(0) : new HashMap<>(initialConfiguration);
			Object useSystemPropsValue = initialConfig.get(PROP_USE_SYSTEM_PROPERTIES);
			this.useSystemProperties = useSystemPropsValue == null ? false : Boolean.parseBoolean(useSystemPropsValue.toString());
			Properties tempConfiguration = useSystemProperties ? EquinoxContainer.secureAction.getProperties() : new Properties();
			// do this the hard way to handle null values
			for (Map.Entry<String, ?> initialEntry : this.initialConfig.entrySet()) {
				if (initialEntry.getValue() == null) {
					if (useSystemProperties) {
						tempConfiguration.remove(initialEntry.getKey());
					} else {
						tempConfiguration.put(initialEntry.getKey(), NULL_CONFIG);
					}
				} else {
					tempConfiguration.put(initialEntry.getKey(), initialEntry.getValue());
				}
			}
			localConfig = useSystemProperties ? null : tempConfiguration;
		}

		void loadConfigIni(URL configIni) {
			if (configIni != null) {
				mergeConfiguration(loadProperties(configIni));
			}
		}

		void finalizeValues() {
			// populate initConfig after loading the configIni
			for (String initialKey : populateInitConfig) {
				String value = getConfiguration(initialKey);
				if (value != null) {
					this.initialConfig.put(initialKey, value);
				}
			}

			// look for special case EclipseStarter defaults
			for (String keyWithEclipseStarterDefault : PROP_WITH_ECLIPSE_STARTER_DEFAULTS) {
				String currentValue = getConfiguration(keyWithEclipseStarterDefault);
				if (currentValue == null) {
					String defaultValue = getConfiguration(keyWithEclipseStarterDefault + PROP_DEFAULT_SUFFIX);
					if (defaultValue != null) {
						setConfiguration(keyWithEclipseStarterDefault, defaultValue);
					}
				}
			}

			// set other defaults
			if (FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT.equals(getConfiguration(FRAMEWORK_STORAGE_CLEAN))) {
				setConfiguration(PROP_CLEAN, "true"); //$NON-NLS-1$
			}

			if (getConfiguration(PROP_STATE_SAVE_DELAY_INTERVAL) == null)
				// Property not specified. Use the default.
				setConfiguration(PROP_STATE_SAVE_DELAY_INTERVAL, DEFAULT_STATE_SAVE_DELAY_INTERVAL);
			try {
				// Verify type compatibility.
				Long.parseLong(getConfiguration(PROP_STATE_SAVE_DELAY_INTERVAL));
			} catch (NumberFormatException e) {
				exceptions.put(e, FrameworkLogEntry.ERROR);
				// The specified value is not type compatible. Use the default.
				setConfiguration(PROP_STATE_SAVE_DELAY_INTERVAL, DEFAULT_STATE_SAVE_DELAY_INTERVAL);
			}

			if (getConfiguration(PROP_GOSH_ARGS) == null) {
				String consoleProp = getConfiguration(ConsoleManager.PROP_CONSOLE);
				consoleProp = consoleProp == null ? null : consoleProp.trim();
				if (consoleProp == null || consoleProp.length() > 0) {
					// no -console was specified or it has specified none or a port for telnet;
					// need to make sure the gogo shell does not create an interactive console on standard in/out
					setConfiguration(PROP_GOSH_ARGS, "--nointeractive"); //$NON-NLS-1$
				} else {
					// Need to make sure we don't shutdown the framework if no console is around (bug 362412)
					setConfiguration(PROP_GOSH_ARGS, "--noshutdown"); //$NON-NLS-1$
				}
			}
		}

		private void mergeConfiguration(Properties source) {
			for (Enumeration<?> e = source.keys(); e.hasMoreElements();) {
				String key = (String) e.nextElement();
				String value = source.getProperty(key);
				if (getConfiguration(key) == null) {
					setProperty(key, value);
					initialConfig.put(key, value);
				} else {
					initialConfig.put(key, getConfiguration(key));
				}
			}
		}

		private Properties loadProperties(URL location) {
			Properties result = new Properties();
			if (location == null)
				return result;
			try {
				try (InputStream in = LocationHelper.getStream(location)) {
					result.load(in);
				}
			} catch (FileNotFoundException e) {
				// TODO probably should log, but the common case for non-eclipse
				// environments would be to not have a config.ini ...
			} catch (IOException e) {
				// We'll just use the defaults for everything but log the exception on reading
				exceptions.put(e, FrameworkLogEntry.WARNING);
			}
			return substituteVars(result);
		}

		private Properties substituteVars(Properties result) {
			if (result == null) {
				//nothing todo.
				return null;
			}
			for (Enumeration<Object> eKeys = result.keys(); eKeys.hasMoreElements();) {
				Object key = eKeys.nextElement();
				if (key instanceof String) {
					String value = result.getProperty((String) key);
					if (value != null)
						result.put(key, substituteVars(value, true));
				}
			}
			return result;
		}

		public String substituteVars(String path, boolean preserveDelimiters) {
			StringBuilder buf = new StringBuilder(path.length());
			StringTokenizer st = new StringTokenizer(path, VARIABLE_DELIM_STRING, true);
			boolean varStarted = false; // indicates we are processing a var subtitute
			String var = null; // the current var key
			while (st.hasMoreElements()) {
				String tok = st.nextToken();
				if (VARIABLE_DELIM_STRING.equals(tok)) {
					if (!varStarted) {
						varStarted = true; // we found the start of a var
						var = ""; //$NON-NLS-1$
					} else {
						// we have found the end of a var
						String prop = null;
						// get the value of the var from system properties
						if (var != null && var.length() > 0)
							prop = getProperty(var);
						if (prop == null) {
							try {
								// try using the System.getenv method if it exists (bug 126921)
								Method getenv = System.class.getMethod("getenv", new Class[] {String.class}); //$NON-NLS-1$
								prop = (String) getenv.invoke(null, new Object[] {var});
							} catch (Throwable t) {
								// do nothing;
								// on 1.4 VMs this throws an error
								// on J2ME this method does not exist
							}
						}
						if (prop != null) {
							// found a value; use it
							buf.append(prop);
						} else {
							// could not find a value append the var
							if (preserveDelimiters) {
								buf.append(VARIABLE_DELIM_CHAR);
							}
							buf.append(var == null ? "" : var); //$NON-NLS-1$
							if (preserveDelimiters) {
								buf.append(VARIABLE_DELIM_CHAR);
							}
						}
						varStarted = false;
						var = null;
					}
				} else {
					if (!varStarted)
						buf.append(tok); // the token is not part of a var
					else
						var = tok; // the token is the var key; save the key to process when we find the end token
				}
			}
			if (var != null)
				// found a case of $var at the end of the path with no trailing $; just append it as is.
				buf.append(VARIABLE_DELIM_CHAR).append(var);
			return buf.toString();
		}

		public String getProperty(String key) {
			// have to access configuration directly instead of using getConfiguration to get access to NULL_CONFIG values
			String result = internalGet(key);
			if (result == NULL_CONFIG) {
				return null;
			}
			return result == null ? EquinoxContainer.secureAction.getProperty(key) : result;
		}

		public String setProperty(String key, String value) {
			if (value == null) {
				return clearConfiguration(key);
			}
			return setConfiguration(key, value);
		}

		public String getConfiguration(String key) {
			String result = internalGet(key);
			return result == NULL_CONFIG ? null : result;
		}

		private String internalGet(String key) {
			if (useSystemProperties) {
				return EquinoxContainer.secureAction.getProperty(key);
			}
			return localConfig.getProperty(key);
		}

		public String getConfiguration(String key, String defaultValue) {
			String result = getConfiguration(key);
			return result == null ? defaultValue : result;
		}

		public String setConfiguration(String key, String value) {
			Object result = internalPut(key, value);
			return result instanceof String && result != NULL_CONFIG ? (String) result : null;
		}

		public String clearConfiguration(String key) {
			Object result = internalRemove(key);
			if (!useSystemProperties) {
				internalPut(key, NULL_CONFIG);
			}
			return result instanceof String && result != NULL_CONFIG ? (String) result : null;
		}

		private Object internalPut(String key, String value) {
			if (useSystemProperties) {
				return EquinoxContainer.secureAction.getProperties().put(key, value);
			}
			return localConfig.put(key, value);
		}

		private Object internalRemove(String key) {
			if (useSystemProperties) {
				return EquinoxContainer.secureAction.getProperties().remove(key);
			}
			return localConfig.remove(key);
		}

		public Map<String, String> getConfiguration() {
			Properties props = useSystemProperties ? EquinoxContainer.secureAction.getProperties() : localConfig;
			// must sync on props to avoid concurrent modification exception
			synchronized (props) {
				Map<String, String> result = new HashMap<>(props.size());
				for (Object key : props.keySet()) {
					if (key instanceof String) {
						String skey = (String) key;
						String sValue = props.getProperty(skey);
						if (sValue != NULL_CONFIG) {
							result.put(skey, sValue);
						}
					}
				}
				return result;
			}
		}

		public Map<String, Object> getInitialConfig() {
			return Collections.unmodifiableMap(initialConfig);
		}
	}

	EquinoxConfiguration(Map<String, ?> initialConfiguration, HookRegistry hookRegistry) {
		this.hookRegistry = hookRegistry;
		// Care must be taken to bootstrap of the config values properly
		// A separate class is used to hold the configuration maps so that we can pass them
		// to the EquionxLocations before the EquinoxConfiguration has been fully constructed
		this.configValues = new ConfigValues(initialConfiguration, exceptions);

		// We need to initialize some properties always before constructing the EquinoxLocations
		initializeProperties();

		// At this point we do not know if we want to debug locations because we have not determined if osgi.debug is set yet
		// We use an AttomicBoolean to hold the setting so we can set it after the config.ini has been loaded
		AtomicBoolean debugLocations = new AtomicBoolean();
		this.equinoxLocations = new EquinoxLocations(this.configValues, this.hookRegistry.getContainer(), debugLocations, exceptions);
		this.configValues.loadConfigIni(getConfigIni(equinoxLocations, false));
		this.configValues.loadConfigIni(getConfigIni(equinoxLocations, true));
		this.configValues.finalizeValues();

		this.debugOptions = new FrameworkDebugOptions(this);
		this.debug = new Debug(this.debugOptions);
		// finally we know if locations should be debugged.
		debugLocations.set(debug.DEBUG_LOCATION);

		// Finally we can set all our variables according to the configuration values

		String osgiDev = getConfiguration(PROP_DEV);
		File f = null;
		boolean devMode = false;
		if (osgiDev != null) {
			try {
				devMode = true;
				URL location = new URL(osgiDev);

				if ("file".equals(location.getProtocol())) { //$NON-NLS-1$
					f = LocationHelper.decodePath(new File(location.getPath()));
					devLastModified = f.lastModified();
				}

				// Check the osgi.dev property to see if dev classpath entries have been defined.
				try {
					loadDevProperties(LocationHelper.getStream(location));
					devMode = true;
				} catch (IOException e) {
					exceptions.put(e, FrameworkLogEntry.ERROR);
				}

			} catch (MalformedURLException e) {
				devDefaultClasspath = getArrayFromList(osgiDev);
			}
		}
		inDevelopmentMode = devMode;
		devLocation = f;

		contextBootDelegation = "true".equals(getConfiguration(PROP_CONTEXT_BOOTDELEGATION, "true")); //$NON-NLS-1$ //$NON-NLS-2$
		compatibilityBootDelegation = "true".equals(getConfiguration(PROP_COMPATIBILITY_BOOTDELEGATION)); //$NON-NLS-1$
		compatibilityLazyTriggerOnFailLoad = "true".equals(getConfiguration(PROP_COMPATIBILITY_START_LAZY_ON_FAIL_CLASSLOAD)); //$NON-NLS-1$

		COPY_NATIVES = Boolean.valueOf(getConfiguration(PROP_COPY_NATIVES)).booleanValue();
		String[] libExtensions = ManifestElement.getArrayFromList(getConfiguration(EquinoxConfiguration.PROP_FRAMEWORK_LIBRARY_EXTENSIONS, getConfiguration(org.osgi.framework.Constants.FRAMEWORK_LIBRARY_EXTENSIONS, getOSLibraryExtDefaults())), ","); //$NON-NLS-1$
		for (int i = 0; i < libExtensions.length; i++)
			if (libExtensions[i].length() > 0 && libExtensions[i].charAt(0) != '.')
				libExtensions[i] = '.' + libExtensions[i];
		LIB_EXTENSIONS = Collections.unmodifiableList(Arrays.asList(libExtensions));
		ECLIPSE_LIB_VARIANTS = buildEclipseLibraryVariants(getWS(), getOS(), getOSArch(), getNL());
		ECLIPSE_NL_JAR_VARIANTS = buildNLJarVariants(getNL());
		DEFINE_PACKAGE_ATTRIBUTES = !"noattributes".equals(getConfiguration(PROP_DEFINE_PACKAGES)); //$NON-NLS-1$

		String bsnVersion = getConfiguration(org.osgi.framework.Constants.FRAMEWORK_BSNVERSION);
		if (org.osgi.framework.Constants.FRAMEWORK_BSNVERSION_SINGLE.equals(bsnVersion)) {
			BSN_VERSION = BSN_VERSION_SINGLE;
		} else if (org.osgi.framework.Constants.FRAMEWORK_BSNVERSION_MULTIPLE.equals(bsnVersion)) {
			BSN_VERSION = BSN_VERSION_MULTIPLE;
		} else {
			BSN_VERSION = BSN_VERSION_MANAGED;
		}

		BUNDLE_SET_TCCL = "true".equals(getConfiguration(PROP_BUNDLE_SETTCCL, "true")); //$NON-NLS-1$ //$NON-NLS-2$

		throwErrorOnFailedStart = "true".equals(getConfiguration(PROP_COMPATIBILITY_ERROR_FAILED_START, "true")); //$NON-NLS-1$//$NON-NLS-2$

		PARALLEL_CAPABLE = CLASS_LOADER_TYPE_PARALLEL.equals(getConfiguration(PROP_CLASS_LOADER_TYPE));

		// A specified osgi.dev property but unspecified osgi.checkConfiguration
		// property implies osgi.checkConfiguration = true.
		inCheckConfigurationMode = Boolean.valueOf(getConfiguration(PROP_CHECK_CONFIGURATION, Boolean.toString(devMode)));
		// Must ensure the check configuration property is set if in osgi.dev mode (bug 443340)
		if (inCheckConfigurationMode && getConfiguration(PROP_CHECK_CONFIGURATION) == null) {
			setConfiguration(PROP_CHECK_CONFIGURATION, "true"); //$NON-NLS-1$
		}
		supportSignedBundles = getSupportSignedBundles(this);
		CLASS_CERTIFICATE = (supportSignedBundles & SIGNED_CONTENT_VERIFY_CERTIFICATE) != 0 && //
				Boolean.valueOf(getConfiguration(PROP_CLASS_CERTIFICATE_SUPPORT, "true")).booleanValue(); //$NON-NLS-1$
		runtimeVerifySignedBundles = (supportSignedBundles & SIGNED_CONTENT_VERIFY_RUNTIME) != 0;
	}

	private static int getSupportSignedBundles(EquinoxConfiguration config) {
		int supportSignedBundles = 0;
		String[] supportOptions = ManifestElement.getArrayFromList(
				config.getConfiguration(SIGNED_CONTENT_SUPPORT, config.getConfiguration(SIGNED_BUNDLE_SUPPORT)), ","); //$NON-NLS-1$
		for (String supportOption : supportOptions) {
			if (SIGNED_CONTENT_SUPPORT_CERTIFICATE.equals(supportOption)) {
				supportSignedBundles |= SIGNED_CONTENT_VERIFY_CERTIFICATE;
			} else if (SIGNED_CONTENT_SUPPORT_TRUST.equals(supportOption)) {
				supportSignedBundles |= SIGNED_CONTENT_VERIFY_CERTIFICATE | SIGNED_CONTENT_VERIFY_TRUST;
			} else if (SIGNED_CONTENT_SUPPORT_RUNTIME.equals(supportOption)) {
				supportSignedBundles |= SIGNED_CONTENT_VERIFY_CERTIFICATE | SIGNED_CONTENT_VERIFY_RUNTIME;
			} else if (SIGNED_CONTENT_SUPPORT_TRUE.equals(supportOption)
					|| SIGNED_CONTENT_SUPPORT_ALL.equals(supportOption)) {
				supportSignedBundles |= SIGNED_CONTENT_VERIFY_ALL;
			}
		}
		return supportSignedBundles;
	}

	private URL getConfigIni(EquinoxLocations locations, boolean parent) {
		if (Boolean.TRUE.toString().equals(getConfiguration(EquinoxConfiguration.PROP_IGNORE_USER_CONFIGURATION)))
			return null;
		Location configArea = locations.getConfigurationLocation();
		if (configArea != null && parent) {
			configArea = configArea.getParentLocation();
		}
		if (configArea == null) {
			return null;
		}
		try {
			return new URL(configArea.getURL().toExternalForm() + CONFIG_FILE);
		} catch (MalformedURLException e) {
			// its ok.  This should never happen
		}
		return null;
	}

	public Map<String, Object> getInitialConfig() {
		return this.configValues.getInitialConfig();
	}

	private static List<String> buildEclipseLibraryVariants(String ws, String os, String arch, String nl) {
		List<String> result = new ArrayList<>();
		result.add("ws/" + ws + "/"); //$NON-NLS-1$ //$NON-NLS-2$
		result.add("os/" + os + "/" + arch + "/"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		result.add("os/" + os + "/"); //$NON-NLS-1$ //$NON-NLS-2$
		nl = nl.replace('_', '/');
		while (nl.length() > 0) {
			result.add("nl/" + nl + "/"); //$NON-NLS-1$ //$NON-NLS-2$
			int i = nl.lastIndexOf('/');
			nl = (i < 0) ? "" : nl.substring(0, i); //$NON-NLS-1$
		}
		result.add(""); //$NON-NLS-1$
		return Collections.unmodifiableList(result);
	}

	private static List<String> buildNLJarVariants(String nl) {
		List<String> result = new ArrayList<>();
		nl = nl.replace('_', '/');
		while (nl.length() > 0) {
			result.add("nl/" + nl + "/"); //$NON-NLS-1$ //$NON-NLS-2$
			int i = nl.lastIndexOf('/');
			nl = (i < 0) ? "" : nl.substring(0, i); //$NON-NLS-1$
		}
		result.add(""); //$NON-NLS-1$
		return result;
	}

	private static String getOSLibraryExtDefaults() {
		// Some OSes have multiple library extensions
		// We should provide defaults to the known ones
		// For example Mac OS X uses dylib and jnilib (bug 380350)
		String os = EquinoxContainer.secureAction.getProperty(EquinoxConfiguration.PROP_JVM_OS_NAME);
		return os == null || !os.startsWith("Mac OS") ? null : "dylib,jnilib"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public boolean inCheckConfigurationMode() {
		return inCheckConfigurationMode;
	}

	@Override
	public boolean inDevelopmentMode() {
		return inDevelopmentMode;
	}

	@Override
	public boolean inDebugMode() {
		return debugOptions.isDebugEnabled();
	}

	@Override
	public String[] getCommandLineArgs() {
		return allArgs;
	}

	@Override
	public String[] getFrameworkArgs() {
		return frameworkArgs;
	}

	@Override
	public String[] getNonFrameworkArgs() {
		return appArgs;
	}

	@Override
	public String getOSArch() {
		return getConfiguration(PROP_OSGI_ARCH);
	}

	@Override
	public String getNL() {
		return getConfiguration(PROP_OSGI_NL);
	}

	@Override
	public String getOS() {
		return getConfiguration(PROP_OSGI_OS);
	}

	@Override
	public String getWS() {
		return getConfiguration(PROP_OSGI_WS);
	}

	public void setAllArgs(String[] allArgs) {
		// do not check if this is set already to allow arguments to change when multiple applications are launched
		this.allArgs = allArgs;
	}

	public void setAppArgs(String[] appArgs) {
		// do not check if this is set already to allow arguments to change when multiple applications are launched
		this.appArgs = appArgs;
	}

	public void setFrameworkArgs(String[] frameworkArgs) {
		// do not check if this is set already to allow arguments to change when multiple applications are launched
		this.frameworkArgs = frameworkArgs;
	}

	public static String guessWS(String osName) {
		// setup default values for known OSes if nothing was specified
		if (osName.equals(Constants.OS_WIN32))
			return Constants.WS_WIN32;
		if (osName.equals(Constants.OS_LINUX))
			return Constants.WS_GTK;
		if (osName.equals(Constants.OS_FREEBSD))
			return Constants.WS_GTK;
		if (osName.equals(Constants.OS_MACOSX))
			return Constants.WS_COCOA;
		if (osName.equals(Constants.OS_HPUX))
			return Constants.WS_MOTIF;
		if (osName.equals(Constants.OS_AIX))
			return Constants.WS_MOTIF;
		if (osName.equals(Constants.OS_SOLARIS))
			return Constants.WS_GTK;
		if (osName.equals(Constants.OS_QNX))
			return Constants.WS_PHOTON;
		return Constants.WS_UNKNOWN;
	}

	public static String guessOS(String osName) {
		// check to see if the OS name is "Windows 98" or some other
		// flavour which should be converted to win32.
		if (osName.regionMatches(true, 0, Constants.OS_WIN32, 0, 3))
			return Constants.OS_WIN32;
		// EXCEPTION: All mappings of SunOS convert to Solaris
		if (osName.equalsIgnoreCase(INTERNAL_OS_SUNOS))
			return Constants.OS_SOLARIS;
		if (osName.equalsIgnoreCase(INTERNAL_OS_LINUX))
			return Constants.OS_LINUX;
		if (osName.equalsIgnoreCase(INTERNAL_OS_QNX))
			return Constants.OS_QNX;
		if (osName.equalsIgnoreCase(INTERNAL_OS_AIX))
			return Constants.OS_AIX;
		if (osName.equalsIgnoreCase(INTERNAL_OS_HPUX))
			return Constants.OS_HPUX;
		if (osName.equalsIgnoreCase(INTERNAL_OS_OS400))
			return Constants.OS_OS400;
		if (osName.equalsIgnoreCase(INTERNAL_OS_OS390))
			return Constants.OS_OS390;
		if (osName.equalsIgnoreCase(INTERNAL_OS_ZOS))
			return Constants.OS_ZOS;
		if (osName.equalsIgnoreCase(INTERNAL_OS_FREEBSD))
			return Constants.OS_FREEBSD;
		// os.name on Mac OS can be either Mac OS or Mac OS X
		if (osName.regionMatches(true, 0, INTERNAL_OS_MACOSX, 0, INTERNAL_OS_MACOSX.length()))
			return Constants.OS_MACOSX;
		return Constants.OS_UNKNOWN;
	}

	public String getConfiguration(String key) {
		return this.configValues.getConfiguration(key);
	}

	public String getConfiguration(String key, String defaultValue) {
		return this.configValues.getConfiguration(key, defaultValue);
	}

	public String setConfiguration(String key, String value) {
		return this.configValues.setConfiguration(key, value);
	}

	public String clearConfiguration(String key) {
		return this.configValues.clearConfiguration(key);
	}

	public Map<String, String> getConfiguration() {
		return this.configValues.getConfiguration();
	}

	public Debug getDebug() {
		return this.debug;
	}

	public DebugOptions getDebugOptions() {
		return this.debugOptions;
	}

	public HookRegistry getHookRegistry() {
		return hookRegistry;
	}

	@Override
	public String getProperty(String key) {
		return this.configValues.getProperty(key);
	}

	@Override
	public String setProperty(String key, String value) {
		return this.configValues.setProperty(key, value);
	}

	public AliasMapper getAliasMapper() {
		return aliasMapper;
	}

	/*
	 * Updates the dev classpath if the file containing the entries have changed
	 */
	private void updateDevProperties() {
		if (devLocation != null && devLocation.lastModified() != devLastModified) {
			try {
				loadDevProperties(new FileInputStream(devLocation));
				devLastModified = devLocation.lastModified();
			} catch (FileNotFoundException e) { // ignore and try again next time
			}
		}
	}

	private static String[] getDevClassPath(BundleRevision bundle, Map<String, String> properties, String[] defaultClasspath) {
		String[] result = null;
		String id = bundle.getSymbolicName();
		if (id != null && properties != null) {
			String idVersion = id + DEV_FILE_ENTRY_VERSION_SEPARATOR + bundle.getVersion();
			String entry = properties.getOrDefault(idVersion, properties.get(id)); // prefer idVersion value
			if (entry != null)
				result = getArrayFromList(entry);
		}
		if (result == null)
			result = defaultClasspath;
		return result;
	}

	/**
	 * Returns a list of classpath elements for the specified bundle symbolic name.
	 * @param bundle a bundle revision to get the development classpath for
	 * @return a list of development classpath elements
	 */
	public String[] getDevClassPath(BundleRevision bundle) {
		synchronized (devMonitor) {
			updateDevProperties();
			return getDevClassPath(bundle, devProperties, devDefaultClasspath);
		}
	}

	private static String[] getArrayFromList(String prop) {
		return ManifestElement.getArrayFromList(prop, ","); //$NON-NLS-1$
	}

	/*
	 * Load the given input stream into a dictionary
	 */
	private void loadDevProperties(InputStream input) {
		Properties props = new Properties();
		try (InputStream inputStream = input) {
			props.load(inputStream);
		} catch (IOException e) {
			exceptions.put(e, FrameworkLogEntry.ERROR);
		}
		@SuppressWarnings({ "unchecked", "rawtypes" })
		Map<String, String> result = (Map) props;
		synchronized (devMonitor) {
			devProperties = result;
			devDefaultClasspath = getArrayFromList(devProperties.get("*")); //$NON-NLS-1$
		}
	}

	private void initializeProperties() {
		// initialize some framework properties that must always be set AND cannot be set with config.ini
		if (getConfiguration(PROP_FRAMEWORK) == null || getConfiguration(EquinoxLocations.PROP_INSTALL_AREA) == null) {
			ProtectionDomain pd = EquinoxConfiguration.class.getProtectionDomain();
			CodeSource cs = pd == null ? null : pd.getCodeSource();
			URL url = cs == null ? null : cs.getLocation();
			if (url == null) {
				IOException cause = null;
				// try to determine by loading a resource we know we have
				URL java6Profile = EquinoxConfiguration.class.getResource("/JavaSE-1.6.profile"); //$NON-NLS-1$
				if (java6Profile != null && "jar".equals(java6Profile.getProtocol())) { //$NON-NLS-1$
					try {
						url = ((JarURLConnection) java6Profile.openConnection()).getJarFileURL();
					} catch (IOException e) {
						cause = e;
					}
				}
				if (url == null) {
					throw new IllegalArgumentException(NLS.bind(Msg.ECLIPSE_STARTUP_PROPS_NOT_SET, PROP_FRAMEWORK + ", " + EquinoxLocations.PROP_INSTALL_AREA), cause); //$NON-NLS-1$
				}
			}

			// allow props to be preset
			if (getConfiguration(PROP_FRAMEWORK) == null) {
				String externalForm = getFrameworkPath(url.toExternalForm(), false);
				setConfiguration(PROP_FRAMEWORK, externalForm);
			}
			if (getConfiguration(EquinoxLocations.PROP_INSTALL_AREA) == null) {
				String installArea;
				if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
					installArea = getFrameworkPath(url.getPath(), true);
				} else {
					// likely not a full eclipse install, and probably not loaded out of a typical
					// URL class loader. Just default to the user dir.
					installArea = System.getProperty("user.dir"); //$NON-NLS-1$
				}
				setConfiguration(EquinoxLocations.PROP_INSTALL_AREA, installArea);
			}
		}
		// always decode these properties
		setConfiguration(PROP_FRAMEWORK, LocationHelper.decode(getConfiguration(PROP_FRAMEWORK), true));
		setConfiguration(EquinoxLocations.PROP_INSTALL_AREA, LocationHelper.decode(getConfiguration(EquinoxLocations.PROP_INSTALL_AREA), true));

		setConfiguration(FRAMEWORK_VENDOR, ECLIPSE_FRAMEWORK_VENDOR);
		String value = getConfiguration(FRAMEWORK_PROCESSOR);
		if (value == null) {
			value = EquinoxContainer.secureAction.getProperty(PROP_JVM_OS_ARCH);
			if (value != null) {
				setConfiguration(FRAMEWORK_PROCESSOR, aliasMapper.getCanonicalProcessor(value));
			}
		}

		value = getConfiguration(FRAMEWORK_OS_NAME);
		if (value == null) {
			value = EquinoxContainer.secureAction.getProperty(PROP_JVM_OS_NAME);
			if (value != null) {
				setConfiguration(FRAMEWORK_OS_NAME, aliasMapper.getCanonicalOSName(value));
			}
		}

		value = getConfiguration(FRAMEWORK_OS_VERSION);
		if (value == null) {
			value = EquinoxContainer.secureAction.getProperty(PROP_JVM_OS_VERSION);
			if (value != null) {
				// only use the value upto the first space
				int space = value.indexOf(' ');
				if (space > 0) {
					value = value.substring(0, space);
				}
				// fix up cases where the os version does not make a valid Version string.
				int major = 0, minor = 0, micro = 0;
				String qualifier = ""; //$NON-NLS-1$
				try {
					StringTokenizer st = new StringTokenizer(value, ".", true); //$NON-NLS-1$
					major = parseVersionInt(st.nextToken());

					if (st.hasMoreTokens()) {
						st.nextToken(); // consume delimiter
						minor = parseVersionInt(st.nextToken());

						if (st.hasMoreTokens()) {
							st.nextToken(); // consume delimiter
							micro = parseVersionInt(st.nextToken());

							if (st.hasMoreTokens()) {
								st.nextToken(); // consume delimiter
								qualifier = st.nextToken();
							}
						}
					}
				} catch (NoSuchElementException e) {
					// ignore, use the values parsed so far
				}
				try {
					value = new Version(major, minor, micro, qualifier).toString();
				} catch (IllegalArgumentException e) {
					// must be an invalid qualifier; just ignore it
					value = new Version(major, minor, micro).toString();
				}
				setConfiguration(FRAMEWORK_OS_VERSION, value);
			}
		}
		value = getConfiguration(FRAMEWORK_LANGUAGE);
		if (value == null)
			// set the value of the framework language property
			setConfiguration(FRAMEWORK_LANGUAGE, Locale.getDefault().getLanguage());
		// set the support properties for fragments and require-bundle (bug 173090)
		setConfiguration(SUPPORTS_FRAMEWORK_FRAGMENT, "true"); //$NON-NLS-1$
		setConfiguration(SUPPORTS_FRAMEWORK_REQUIREBUNDLE, "true"); //$NON-NLS-1$
		setConfiguration(SUPPORTS_FRAMEWORK_EXTENSION, "true"); //$NON-NLS-1$

		/*
		 * Initializes the execution context for this run of the platform.  The context
		 * includes information about the locale, operating system and window system.
		 *
		 * NOTE: The OS, WS, and ARCH values should never be null. The executable should
		 * be setting these values and therefore this code path is obsolete for Eclipse
		 * when run from the executable.
		 */

		// if the user didn't set the locale with a command line argument then use the default.
		String nlValue = getConfiguration(PROP_OSGI_NL);
		if (nlValue != null) {
			Locale userLocale = toLocale(nlValue, Locale.getDefault());
			Locale.setDefault(userLocale);
			// TODO what the heck is this for?? why not just use osgi.nl
			setConfiguration(PROP_OSGI_NL_USER, nlValue);
		}
		nlValue = Locale.getDefault().toString();
		setConfiguration(PROP_OSGI_NL, nlValue);

		// if the user didn't set the operating system with a command line
		// argument then use the default.
		String osValue = getConfiguration(PROP_OSGI_OS);
		if (osValue == null) {
			osValue = guessOS(EquinoxContainer.secureAction.getProperty(PROP_JVM_OS_NAME));
			setConfiguration(PROP_OSGI_OS, osValue);
		}

		// if the user didn't set the window system with a command line
		// argument then use the default.
		String wsValue = getConfiguration(PROP_OSGI_WS);
		if (wsValue == null) {
			wsValue = guessWS(osValue);
			setConfiguration(PROP_OSGI_WS, wsValue);
		}

		// if the user didn't set the system architecture with a command line
		// argument then use the default.
		String archValue = getConfiguration(PROP_OSGI_ARCH);
		if (archValue == null) {
			String name = EquinoxContainer.secureAction.getProperty(PROP_JVM_OS_ARCH);
			// Map i386 architecture to x86
			if (name.equalsIgnoreCase(INTERNAL_ARCH_I386))
				archValue = Constants.ARCH_X86;
			// Map amd64 architecture to x86_64
			else if (name.equalsIgnoreCase(INTERNAL_AMD64))
				archValue = Constants.ARCH_X86_64;
			else
				archValue = name;
			setConfiguration(PROP_OSGI_ARCH, archValue);
		}
		// set the initial UUID so that it is set as soon as possible
		String uuid = InternalUtils.newUUID(this);
		setConfiguration(FRAMEWORK_UUID, uuid);
	}

	private static String getFrameworkPath(String path, boolean parent) {
		if (File.separatorChar == '\\') {
			// in case on windows the \ is used
			path = path.replace('\\', '/');
		}
		// TODO is there a better way?
		// this is a hack to get the framework to launch from a self-hosted eclipse instance
		// we assume the code source will end in the path org.eclipse.osgi/bin/
		if (path.endsWith("org.eclipse.osgi/bin/")) { //$NON-NLS-1$
			path = path.substring(0, path.length() - "bin/".length()); //$NON-NLS-1$
		}
		if (parent) {
			int lastSlash = path.lastIndexOf('/');
			return lastSlash == -1 ? "/" : path.substring(0, lastSlash); //$NON-NLS-1$
		}
		return path;
	}

	private static int parseVersionInt(String value) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			// try up to the first non-number char
			StringBuilder sb = new StringBuilder(value.length());
			char[] chars = value.toCharArray();
			for (char element : chars) {
				if (!Character.isDigit(element))
					break;
				sb.append(element);
			}
			if (sb.length() > 0)
				return Integer.parseInt(sb.toString());
			return 0;
		}
	}

	public String substituteVars(String path) {
		return substituteVars(path, false);
	}

	public String substituteVars(String path, boolean preserveDelimiters) {
		return this.configValues.substituteVars(path, preserveDelimiters);
	}

	/**
	 * <p>
	 * Converts a String to a Locale.
	 * </p>
	 *
	 * <p>
	 * This method takes the string format of a locale and creates the locale object from it.
	 * </p>
	 *
	 * <p>
	 * This method validates the input strictly. The language code must be lowercase. The country
	 * code must be uppercase. The separator must be an underscore. The length must be correct.
	 * </p>
	 *
	 * <p>
	 * This method is inspired by <code>org.apache.commons.lang.LocaleUtils.toLocale(String)</code>
	 * by fixing the parsing error for uncommon Locales like having a language and a variant code
	 * but no country code, or a Locale that only consists of a country code.
	 * </p>
	 *
	 * @param str
	 *            the locale String to convert
	 * @param defaultLocale
	 *            the Locale that should be returned in case of an invalid Locale String
	 * @return a Locale that matches the specified locale String. If the given input String is
	 *         <code>null</code> or can not be parsed because of an invalid format, the given
	 *         default {@link Locale} will be returned.
	 */
	public static Locale toLocale(String str, Locale defaultLocale) {
		if (str == null) {
			System.err.println("Given locale String is null - Default Locale will be used instead."); //$NON-NLS-1$
			return defaultLocale;
		}

		String language = ""; //$NON-NLS-1$
		String country = ""; //$NON-NLS-1$
		String variant = ""; //$NON-NLS-1$

		String[] localeParts = str.split("_"); //$NON-NLS-1$
		if (localeParts.length == 0 || localeParts.length > 3 || (localeParts.length == 1 && localeParts[0].length() == 0)) {
			System.err.println(NLS.bind(Msg.error_badNL, str));
			return defaultLocale;
		}

		if (localeParts[0].length() > 0 && !localeParts[0].matches("[a-zA-Z]{2,8}")) { //$NON-NLS-1$
			System.err.println(NLS.bind(Msg.error_badNL, str));
			return defaultLocale;
		}

		language = localeParts[0];

		if (localeParts.length > 1) {
			if (localeParts[1].length() > 0 && !localeParts[1].matches("[a-zA-Z]{2}|[0-9]{3}")) { //$NON-NLS-1$
				if (language.length() > 0) {
					System.err.println(NLS.bind(Msg.error_badNL_language, str));
					return new Locale(language);
				}
				System.err.println(NLS.bind(Msg.error_badNL, str));
				return defaultLocale;
			}

			country = localeParts[1];
		}

		if (localeParts.length == 3) {
			if (localeParts[2].length() == 0) {
				System.err.println(NLS.bind(Msg.error_badNL_language_country, str));
				return new Locale(language, country);
			}
			variant = localeParts[2];
		}

		return new Locale(language, country, variant);
	}

	public EquinoxLocations getEquinoxLocations() {
		return equinoxLocations;
	}

	void logMessages(EquinoxLogServices logServices) {
		for (Map.Entry<Throwable, Integer> exception : exceptions.entrySet()) {
			logServices.log(EquinoxContainer.NAME, exception.getValue(), exception.getKey().getMessage(), exception.getKey());
		}
		exceptions.clear();
	}
}
