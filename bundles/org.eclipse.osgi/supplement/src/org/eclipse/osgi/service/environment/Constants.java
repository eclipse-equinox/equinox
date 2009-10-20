/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.service.environment;

/**
 * Constants used with the {@link EnvironmentInfo} service.
 * @since 3.0
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface Constants {
	//TODO These constants need to be aligned with the OSGi ones. See page 64-588 of the spec

	/**
	 * Constant string (value "win32") indicating the platform is running on a
	 * Window 32-bit operating system (e.g., Windows 98, NT, 2000).
	 */
	public static final String OS_WIN32 = "win32";//$NON-NLS-1$

	/**
	 * Constant string (value "linux") indicating the platform is running on a
	 * Linux-based operating system.
	 */
	public static final String OS_LINUX = "linux";//$NON-NLS-1$

	/**
	 * Constant string (value "aix") indicating the platform is running on an
	 * AIX-based operating system.
	 */
	public static final String OS_AIX = "aix";//$NON-NLS-1$

	/**
	 * Constant string (value "solaris") indicating the platform is running on a
	 * Solaris-based operating system.
	 */
	public static final String OS_SOLARIS = "solaris";//$NON-NLS-1$

	/**
	 * Constant string (value "hpux") indicating the platform is running on an
	 * HP/UX-based operating system.
	 */
	public static final String OS_HPUX = "hpux";//$NON-NLS-1$

	/**
	 * Constant string (value "qnx") indicating the platform is running on a
	 * QNX-based operating system.
	 */
	public static final String OS_QNX = "qnx";//$NON-NLS-1$

	/**
	 * Constant string (value "macosx") indicating the platform is running on a
	 * Mac OS X operating system.
	 */
	public static final String OS_MACOSX = "macosx";//$NON-NLS-1$

	/**
	 * Constant string (value "epoc32") indicating the platform is running on a
	 * Epoc 32-bit Symbian operating system.
	 * @since 3.4
	 */
	public static final String OS_EPOC32 = "epoc32";//$NON-NLS-1$

	/**
	 * Constant string (value "os/400") indicating the platform is running on a
	 * OS/400 operating system.
	 * @since 3.5
	 */
	public static final String OS_OS400 = "os/400"; //$NON-NLS-1$

	/**
	 * Constant string (value "os/390") indicating the platform is running on a
	 * OS/390 operating system.
	 * @since 3.5
	 */
	public static final String OS_OS390 = "os/390"; //$NON-NLS-1$

	/**
	 * Constant string (value "z/os") indicating the platform is running on a
	 * z/OS operating system.
	 * @since 3.5
	 */
	public static final String OS_ZOS = "z/os"; //$NON-NLS-1$

	/**
	 * Constant string (value "unknown") indicating the platform is running on a
	 * machine running an unknown operating system.
	 */
	public static final String OS_UNKNOWN = "unknown";//$NON-NLS-1$

	/**
	 * Constant string (value "x86") indicating the platform is running on an
	 * x86-based architecture.
	 */
	public static final String ARCH_X86 = "x86";//$NON-NLS-1$

	/**
	 * Constant string (value "PA_RISC") indicating the platform is running on an
	 * PA_RISC-based architecture.
	 */
	public static final String ARCH_PA_RISC = "PA_RISC";//$NON-NLS-1$

	/**
	 * Constant string (value "ppc") indicating the platform is running on an
	 * PowerPC-based architecture.
	 */
	public static final String ARCH_PPC = "ppc";//$NON-NLS-1$

	/**
	 * Constant string (value "ppc64") indicating the platform is running on an
	 * PowerPC-based 64-bit architecture.
	 * @since 3.6
	 */
	public static final String ARCH_PPC64 = "ppc64";//$NON-NLS-1$

	/**
	 * Constant string (value "sparc") indicating the platform is running on an
	 * Sparc-based architecture.
	 */
	public static final String ARCH_SPARC = "sparc";//$NON-NLS-1$

	/**
	 * Constant string (value "x86_64") indicating the platform is running on an
	 * x86 64bit-based architecture.
	 * 
	 * @since 3.1
	 */
	public static final String ARCH_X86_64 = "x86_64";//$NON-NLS-1$

	/**
	 * Constant string (value "amd64") indicating the platform is running on an
	 * AMD64-based architecture.
	 * 
	 * @deprecated use <code>ARCH_X86_64</code> instead. Note the values
	 * has been changed to be the value of the <code>ARCH_X86_64</code> constant.
	 */
	public static final String ARCH_AMD64 = ARCH_X86_64;

	/**
	 * Constant string (value "ia64") indicating the platform is running on an
	 * IA64-based architecture.
	 */
	public static final String ARCH_IA64 = "ia64"; //$NON-NLS-1$

	/**
	 * Constant string (value "ia64_32") indicating the platform is running on an
	 * IA64 32bit-based architecture.
	 * 
	 * @since 3.1
	 */
	public static final String ARCH_IA64_32 = "ia64_32";//$NON-NLS-1$

	/**
	 * Constant string (value "win32") indicating the platform is running on a
	 * machine using the Windows windowing system.
	 */
	public static final String WS_WIN32 = "win32";//$NON-NLS-1$

	/**
	 * Constant string (value "wpf") indicating the platform is running on a
	 * machine using the Windows Presentation Foundation sytstem.
	 */
	public static final String WS_WPF = "wpf"; //$NON-NLS-1$

	/**
	 * Constant string (value "motif") indicating the platform is running on a
	 * machine using the Motif windowing system.
	 */
	public static final String WS_MOTIF = "motif";//$NON-NLS-1$

	/**
	 * Constant string (value "gtk") indicating the platform is running on a
	 * machine using the GTK windowing system.
	 */
	public static final String WS_GTK = "gtk";//$NON-NLS-1$

	/**
	 * Constant string (value "photon") indicating the platform is running on a
	 * machine using the Photon windowing system.
	 */
	public static final String WS_PHOTON = "photon";//$NON-NLS-1$

	/**
	 * Constant string (value "carbon") indicating the platform is running on a
	 * machine using the Carbon windowing system (Mac OS X).
	 */
	public static final String WS_CARBON = "carbon";//$NON-NLS-1$

	/**
	 * Constant string (value "cocoa") indicating the platform is running on a
	 * machine using the Cocoa windowing system.
	 * @since 3.5
	 */
	public static final String WS_COCOA = "cocoa";//$NON-NLS-1$

	/**
	 * Constant string (value "s60") indicating the platform is running on a
	 * machine using the S60 windowing system.
	 * @since 3.4
	 */
	public static final String WS_S60 = "s60";//$NON-NLS-1$

	/**
	 * Constant string (value "unknown") indicating the platform is running on a
	 * machine running an unknown windowing system.
	 */
	public static final String WS_UNKNOWN = "unknown";//$NON-NLS-1$
}
