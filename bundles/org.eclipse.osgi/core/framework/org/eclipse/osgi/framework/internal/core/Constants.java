/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

/**
 * This interface contains the constants used by the eclipse
 * OSGi implementation.
 */

public interface Constants
extends org.osgi.framework.Constants    /* since Framework 1.1 */
{
	/** OSGI implementation version - make sure it is 3 digits for ServerConnection.java */
    public static final String OSGI_IMPL_VERSION = "3.6.0";

    /** OSGI implementation version properties key */
    public static final String OSGI_IMPL_VERSION_KEY = "org.eclipse.osgi.framework.internal.core.version";

    /** Default framework version */
    public static final String OSGI_FRAMEWORK_VERSION = "1.2";

    /** Framework vendor */
    public static final String OSGI_FRAMEWORK_VENDOR = "Eclipse";

    /** SystemBundle manifest name */
    public static final String OSGI_SYSTEMBUNDLE_MANIFEST = "/META-INF/SYSTEMBUNDLE.MF";

    /** Bundle manifest name */
    public static final String OSGI_BUNDLE_MANIFEST = "META-INF/MANIFEST.MF";

    /** OSGi framework package name. */
    public static final String OSGI_FRAMEWORK_PACKAGE = "org.osgi.framework";

    /** Bundle resource URL protocol */
    public static final String OSGI_URL_PROTOCOL = "bundle";

    /** Processor aliases resource */
    public static final String OSGI_PROCESSOR_ALIASES = "processor.aliases";

    /** OS name aliases resource */
    public static final String OSGI_OSNAME_ALIASES = "osname.aliases";

    /** Default permissions for bundles with no permission set
     * and there are no default permissions set.
     */
    public static final String OSGI_DEFAULT_DEFAULT_PERMISSIONS = "default.permissions";

    /** Base implied permissions for all bundles */
    public static final String OSGI_BASE_IMPLIED_PERMISSIONS = "implied.permissions";

    /** Name of OSGi LogService */
    public static final String OSGI_LOGSERVICE_NAME = "org.osgi.service.log.LogService";

    /** Name of OSGi PackageAdmin */
    public static final String OSGI_PACKAGEADMIN_NAME = "org.osgi.service.packageadmin.PackageAdmin";

    /** Name of OSGi PermissionAdmin */
    public static final String OSGI_PERMISSIONADMIN_NAME = "org.osgi.service.permissionadmin.PermissionAdmin";

    /** Name of OSGi StartLevel */
    public static final String OSGI_STARTLEVEL_NAME = "org.osgi.service.startlevel.StartLevel";

    /** OSGI system package property */
    public static final String OSGI_SYSTEMPACKAGES_PROPERTY = "org.eclipse.osgi.framework.internal.core.systempackages";

    /** Bundle loader buffer size property */
    public static final String OSGI_LOADERBUFFERSIZE_PROPERTY = "org.eclipse.osgi.framework.internal.core.loader.buffersize";

    /** JVM java.vm.name property name */
    public static final String JVM_VM_NAME = "java.vm.name";

    /** J9 configuration (for example: core, gw, gwp, rm, foun, max) */
    public static final String JVM_CONFIGURATION = "com.ibm.oti.configuration";

    /** JVM os.arch property name */
    public static final String JVM_OS_ARCH = "os.arch";

    /** JVM os.name property name */
    public static final String JVM_OS_NAME = "os.name";

    /** JVM os.version property name */
    public static final String JVM_OS_VERSION = "os.version";

    /** JVM user.language property name */
    public static final String JVM_USER_LANGUAGE = "user.language";

    /** JVM user.region property name */
    public static final String JVM_USER_REGION = "user.region";

	/** J2ME configuration property name */
	public static final String J2ME_MICROEDITION_CONFIGURATION = "microedition.configuration";

	/** J2ME profile property name */
	public static final String J2ME_MICROEDITION_PROFILES = "microedition.profiles";

    /** Persistent bundle status */
    public static final int BUNDLE_STARTED = 0x00000001;

	/** Property file locations and default names. */
	public static final String KEY_OSGI_PROPERTIES = "org.eclipse.osgi.framework.internal.core.properties";
	public static final String DEFAULT_OSGI_PROPERTIES = "osgi.properties";

	/** Properties set by the framework */
	public static final String KEY_ENDIAN = "org.eclipse.osgi.framework.internal.core.endian";
	public static final String KEY_ADDRESSLENGTH = "org.eclipse.osgi.framework.internal.core.addresslength";
	public static final String KEY_VM = "org.eclipse.osgi.framework.internal.core.vm";
	public static final String KEY_IMPLTYPE = "org.eclipse.osgi.framework.internal.core.impltype";
	public static final String KEY_COUNTRY = "org.eclipse.osgi.framework.internal.core.country";
	public static final String KEY_FRAMEWORKBEGINNINGSTARTLEVEL = "org.eclipse.osgi.framework.internal.core.FrameworkBeginningStartLevel";

    /** Properties defaults */
    public static final String DEFAULT_ADDRESSLENGTH = "32";
    public static final String DEFAULT_ENDIAN = "le";
    public static final String DEFAULT_STARTLEVEL = "1";
    
    /** ImplTypes (Execution environments) properties */
    public static final String IMPLTYPE_FOUNDATION = "JCL_Foundation_1_3";
    public static final String IMPLTYPE_GWP = "JCL_GatewayPlus_1_3";
    public static final String IMPLTYPE_MAX = "JCL_Max_1_3";
    public static final String IMPLTYPE_UNDEFINED = "";

}
