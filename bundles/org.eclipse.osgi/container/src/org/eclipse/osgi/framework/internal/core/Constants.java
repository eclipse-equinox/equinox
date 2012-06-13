/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

/**
 * This interface contains the constants used by the eclipse
 * OSGi implementation.
 */

public class Constants implements org.osgi.framework.Constants {
	/** Default framework version */
	public static final String OSGI_FRAMEWORK_VERSION = "1.3"; //$NON-NLS-1$

	/** Framework vendor */
	public static final String OSGI_FRAMEWORK_VENDOR = "Eclipse"; //$NON-NLS-1$

	/** Bundle manifest name */
	public static final String OSGI_BUNDLE_MANIFEST = "META-INF/MANIFEST.MF"; //$NON-NLS-1$

	/** OSGi framework package name. */
	public static final String OSGI_FRAMEWORK_PACKAGE = "org.osgi.framework"; //$NON-NLS-1$

	/** Bundle resource URL protocol */
	public static final String OSGI_RESOURCE_URL_PROTOCOL = "bundleresource"; //$NON-NLS-1$

	/** Bundle entry URL protocol */
	public static final String OSGI_ENTRY_URL_PROTOCOL = "bundleentry"; //$NON-NLS-1$

	/** Processor aliases resource */
	public static final String OSGI_PROCESSOR_ALIASES = "processor.aliases"; //$NON-NLS-1$

	/** OS name aliases resource */
	public static final String OSGI_OSNAME_ALIASES = "osname.aliases"; //$NON-NLS-1$

	/** Default permissions for bundles with no permission set
	 * and there are no default permissions set.
	 */
	public static final String OSGI_DEFAULT_DEFAULT_PERMISSIONS = "default.permissions"; //$NON-NLS-1$

	/** Base implied permissions for all bundles */
	public static final String OSGI_BASE_IMPLIED_PERMISSIONS = "implied.permissions"; //$NON-NLS-1$

	/** Name of OSGi LogService */
	public static final String OSGI_LOGSERVICE_NAME = "org.osgi.service.log.LogService"; //$NON-NLS-1$

	/** Name of OSGi PackageAdmin */
	public static final String OSGI_PACKAGEADMIN_NAME = "org.osgi.service.packageadmin.PackageAdmin"; //$NON-NLS-1$

	/** Name of OSGi PermissionAdmin */
	public static final String OSGI_PERMISSIONADMIN_NAME = "org.osgi.service.permissionadmin.PermissionAdmin"; //$NON-NLS-1$

	/** Name of OSGi StartLevel */
	public static final String OSGI_STARTLEVEL_NAME = "org.osgi.service.startlevel.StartLevel"; //$NON-NLS-1$

	/** JVM java.vm.name property name */
	public static final String JVM_VM_NAME = "java.vm.name"; //$NON-NLS-1$

	/** JVM os.arch property name */
	public static final String JVM_OS_ARCH = "os.arch"; //$NON-NLS-1$

	/** JVM os.name property name */
	public static final String JVM_OS_NAME = "os.name"; //$NON-NLS-1$

	/** JVM os.version property name */
	public static final String JVM_OS_VERSION = "os.version"; //$NON-NLS-1$

	/** JVM user.language property name */
	public static final String JVM_USER_LANGUAGE = "user.language"; //$NON-NLS-1$

	/** JVM user.region property name */
	public static final String JVM_USER_REGION = "user.region"; //$NON-NLS-1$

	/** J2ME configuration property name */
	public static final String J2ME_MICROEDITION_CONFIGURATION = "microedition.configuration"; //$NON-NLS-1$

	/** J2ME profile property name */
	public static final String J2ME_MICROEDITION_PROFILES = "microedition.profiles"; //$NON-NLS-1$

	/** Persistent start bundle status */
	public static final int BUNDLE_STARTED = 0x00000001;
	/** Lazy start flag bundle status */
	public static final int BUNDLE_LAZY_START = 0x00000002;
	public static final int BUNDLE_ACTIVATION_POLICY = 0x00000004;

	/** Property file locations and default names. */
	public static final String OSGI_PROPERTIES = "osgi.framework.properties"; //$NON-NLS-1$
	public static final String DEFAULT_OSGI_PROPERTIES = "osgi.properties"; //$NON-NLS-1$

	private static String INTERNAL_SYSTEM_BUNDLE = "org.eclipse.osgi"; //$NON-NLS-1$

	public static String getInternalSymbolicName() {
		return INTERNAL_SYSTEM_BUNDLE;
	}

	static void setInternalSymbolicName(String name) {
		INTERNAL_SYSTEM_BUNDLE = name;
	}

	/** OSGI implementation version properties key */
	public static final String OSGI_IMPL_VERSION_KEY = "osgi.framework.version"; //$NON-NLS-1$
	/** OSGi java profile; used to give a URL to a java profile */
	public static final String OSGI_JAVA_PROFILE = "osgi.java.profile"; //$NON-NLS-1$
	public static final String OSGI_JAVA_PROFILE_NAME = "osgi.java.profile.name"; //$NON-NLS-1$
	/** 
	 * OSGi java profile bootdelegation; used to indicate how the org.osgi.framework.bootdelegation
	 * property defined in the java profile should be processed, (ingnore, override, none). default is ignore
	 */
	public static final String OSGI_JAVA_PROFILE_BOOTDELEGATION = "osgi.java.profile.bootdelegation"; //$NON-NLS-1$
	/** indicates that the org.osgi.framework.bootdelegation in the java profile should be ingored */
	public static final String OSGI_BOOTDELEGATION_IGNORE = "ignore"; //$NON-NLS-1$
	/** indicates that the org.osgi.framework.bootdelegation in the java profile should override the system property */
	public static final String OSGI_BOOTDELEGATION_OVERRIDE = "override"; //$NON-NLS-1$
	/** indicates that the org.osgi.framework.bootdelegation in the java profile AND the system properties should be ignored */
	public static final String OSGI_BOOTDELEGATION_NONE = "none"; //$NON-NLS-1$
	/** OSGi strict delegation **/
	public static final String OSGI_RESOLVER_MODE = "osgi.resolverMode"; //$NON-NLS-1$
	public static final String STRICT_MODE = "strict"; //$NON-NLS-1$
	public static final String DEVELOPMENT_MODE = "development"; //$NON-NLS-1$

	public static final String STATE_SYSTEM_BUNDLE = "osgi.system.bundle"; //$NON-NLS-1$

	public static final String PROP_OSGI_RELAUNCH = "osgi.framework.relaunch"; //$NON-NLS-1$

	public static String OSGI_COMPATIBILITY_BOOTDELEGATION = "osgi.compatibility.bootdelegation"; //$NON-NLS-1$

	/** Eclipse-SystemBundle header */
	public static final String ECLIPSE_SYSTEMBUNDLE = "Eclipse-SystemBundle"; //$NON-NLS-1$
	public static final String ECLIPSE_PLATFORMFILTER = "Eclipse-PlatformFilter"; //$NON-NLS-1$
	public static final String Eclipse_JREBUNDLE = "Eclipse-JREBundle"; //$NON-NLS-1$
	/**
	 * Manifest Export-Package directive indicating that the exported package should only 
	 * be made available when the resolver is not in strict mode.
	 */
	public static final String INTERNAL_DIRECTIVE = "x-internal"; //$NON-NLS-1$

	/**
	 * Manifest Export-Package directive indicating that the exported package should only 
	 * be made available to friends of the exporting bundle.
	 */
	public static final String FRIENDS_DIRECTIVE = "x-friends"; //$NON-NLS-1$

	/**
	 * Manifest header (named &quot;Provide-Package&quot;)
	 * identifying the packages name
	 * provided to other bundles which require the bundle.
	 *
	 * <p>
	 * NOTE: this is only used for backwards compatibility, bundles manifest using
	 * syntax version 2 will not recognize this header.
	 *
	 * <p>The attribute value may be retrieved from the
	 * <tt>Dictionary</tt> object returned by the <tt>Bundle.getHeaders</tt> method.
	 * @deprecated
	 */
	public final static String PROVIDE_PACKAGE = "Provide-Package"; //$NON-NLS-1$

	/**
	 * Manifest header attribute (named &quot;reprovide&quot;)
	 * for Require-Bundle
	 * identifying that any packages that are provided
	 * by the required bundle must be reprovided by the requiring bundle.
	 * The default value is <tt>false</tt>.
	 * <p>
	 * The attribute value is encoded in the Require-Bundle manifest 
	 * header like:
	 * <pre>
	 * Require-Bundle: com.acme.module.test; reprovide="true"
	 * </pre>
	 * <p>
	 * NOTE: this is only used for backwards compatibility, bundles manifest using
	 * syntax version 2 will not recognize this attribute.
	 * @deprecated
	 */
	public final static String REPROVIDE_ATTRIBUTE = "reprovide"; //$NON-NLS-1$

	/**
	 * Manifest header attribute (named &quot;optional&quot;)
	 * for Require-Bundle
	 * identifying that a required bundle is optional and that
	 * the requiring bundle can be resolved if there is no 
	 * suitable required bundle.
	 * The default value is <tt>false</tt>.
	 *
	 * <p>The attribute value is encoded in the Require-Bundle manifest 
	 * header like:
	 * <pre>
	 * Require-Bundle: com.acme.module.test; optional="true"
	 * </pre>
	 * <p>
	 * NOTE: this is only used for backwards compatibility, bundles manifest using
	 * syntax version 2 will not recognize this attribute.
	 * @since 1.3 <b>EXPERIMENTAL</b>
	 * @deprecated
	 */
	public final static String OPTIONAL_ATTRIBUTE = "optional"; //$NON-NLS-1$

	/**
	* The key used to designate the buddy loader associated with a given bundle.
	*/
	public final static String BUDDY_LOADER = "Eclipse-BuddyPolicy"; //$NON-NLS-1$

	public final static String REGISTERED_POLICY = "Eclipse-RegisterBuddy"; //$NON-NLS-1$

	static public final String INTERNAL_HANDLER_PKGS = "equinox.interal.handler.pkgs"; //$NON-NLS-1$

	// TODO rename it to Eclipse-PluginClass
	public static final String PLUGIN_CLASS = "Plugin-Class"; //$NON-NLS-1$

	/** Manifest header used to specify the lazy start properties of a bundle */
	public static final String ECLIPSE_LAZYSTART = "Eclipse-LazyStart"; //$NON-NLS-1$

	/** An Eclipse-LazyStart attribute used to specify exception classes for auto start */
	public static final String ECLIPSE_LAZYSTART_EXCEPTIONS = "exceptions"; //$NON-NLS-1$

	/** 
	 * Manifest header used to specify the auto start properties of a bundle 
	 * @deprecated use {@link #ECLIPSE_LAZYSTART}
	 */
	public static final String ECLIPSE_AUTOSTART = "Eclipse-AutoStart"; //$NON-NLS-1$

	/**
	 * @deprecated use {@link #ECLIPSE_LAZYSTART_EXCEPTIONS}
	 */
	public static final String ECLIPSE_AUTOSTART_EXCEPTIONS = ECLIPSE_LAZYSTART_EXCEPTIONS;

	/**
	 * Framework launching property specifying whether Equinox's FrameworkWiring
	 * implementation should refresh bundles with equal symbolic names.
	 *
	 * <p>
	 * Default value is <b>TRUE</b> in this release of the Equinox.
	 * This default may change to <b>FALSE</b> in a future Equinox release.
	 * Therefore, code must not assume the default behavior is
	 * <b>TRUE</b> and should interrogate the value of this property to
	 * determine the behavior.
	 *
	 * <p>
	 * The value of this property may be retrieved by calling the
	 * {@code BundleContext.getProperty} method.
	 * @see  <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=351519">bug 351519</a>
	 * @since 3.7.1
	 */
	public static final String REFRESH_DUPLICATE_BSN = "equinox.refresh.duplicate.bsn"; //$NON-NLS-1$

}
