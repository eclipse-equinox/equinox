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
package org.eclipse.osgi.service.debug;

import java.io.File;

/**
 * Used to get debug options settings and creating a new {@link DebugTrace} instance for
 * a bundle to use for dynamic tracing.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 3.1
 */
public interface DebugOptions {
	/**
	 * The service property (named &quot;listener.symbolic.name&quot;) which specifies 
	 * the bundle symbolic name of a {@link DebugOptionsListener} service.
	 * 
	 * @since 3.5
	 */
	public static String LISTENER_SYMBOLICNAME = "listener.symbolic.name"; //$NON-NLS-1$

	/**
	 * Returns the identified option as a boolean value.  The specified
	 * defaultValue is returned if no such option is found.   Options are specified
	 * in the general form <i>&lt;Bundle-SymbolicName&gt;/&lt;option-path&gt;</i>.  
	 * For example, <code>org.eclipse.core.runtime/debug</code>
	 *
	 * @param option the name of the option to lookup
	 * @param defaultValue the value to return if no such option is found
	 * @return the value of the requested debug option or the
	 * defaultValue if no such option is found.
	 */
	public abstract boolean getBooleanOption(String option, boolean defaultValue);

	/**
	 * Returns the identified option.  <code>null</code>
	 * is returned if no such option is found.   Options are specified
	 * in the general form <i>&lt;Bundle-SymbolicName&gt;/&lt;option-path&gt;</i>.  
	 * For example, <code>org.eclipse.core.runtime/debug</code>
	 *
	 * @param option the name of the option to lookup
	 * @return the value of the requested debug option or <code>null</code>
	 */
	public abstract String getOption(String option);

	/**
	 * Returns the identified option.  The specified defaultValue is 
	 * returned if no such option is found or if a NumberFormatException is thrown 
	 * while converting the option value to an integer.   Options are specified
	 * in the general form <i>&lt;Bundle-SymbolicName&gt;/&lt;option-path&gt;</i>.  
	 * For example, <code>org.eclipse.core.runtime/debug</code>
	 *
	 * @param option the name of the option to lookup
	 * @param defaultValue the value to return if no such option is found
	 * @return the value of the requested debug option or the
	 * defaultValue if no such option is found.
	 */
	public abstract String getOption(String option, String defaultValue);

	/**
	 * Returns the identified option as an int value.  The specified
	 * defaultValue is returned if no such option is found.   Options are specified
	 * in the general form <i>&lt;Bundle-SymbolicName&gt;/&lt;option-path&gt;</i>.  
	 * For example, <code>org.eclipse.core.runtime/debug</code>
	 *
	 * @param option the name of the option to lookup
	 * @param defaultValue the value to return if no such option is found
	 * @return the value of the requested debug option or the
	 * defaultValue if no such option is found.
	 */
	public abstract int getIntegerOption(String option, int defaultValue);

	/**
	 * Sets the identified option to the identified value.
	 * @param option the name of the option to set
	 * @param value the value of the option to set
	 */
	public abstract void setOption(String option, String value);

	/**
	 * Removes the identified option
	 * @param option the name of the option to remove
	 * @since 3.5
	 */
	public abstract void removeOption(String option);

	/**
	 * Accessor to determine if debugging/tracing is enabled for the product.
	 * @return true if debugging/tracing is enabled;  Otherwise false is returned.
	 * @since 3.5
	 */
	public abstract boolean isDebugEnabled();

	/**
	 * Enables or disables debug tracing for the entire application.
	 * @param value If <code>true</code>, debugging is enabled, otherwise
	 * debugging is disabled
	 * @since 3.5
	 */
	public abstract void setDebugEnabled(boolean value);

	/** 
	 * Sets the current file used to trace messages to.
	 * 
	 * @param newFile The file to be used for tracing messages.
	 * @since 3.5
	 */
	public abstract void setFile(File newFile);

	/**
	 * Returns the trace file if it is set, otherwise <code>null</code> is returned.
	 * 
	 * @return the trace file if it is set, otherwise <code>null</code> is returned.
	 * @since 3.5
	 */
	public abstract File getFile();

	/**
	 * Creates a new <code>DebugTrace</code> instance for the specified bundle symbolic name.
	 * If a <code>DebugTrace</code> object has already been created for the specified symbolic
	 * name then the existing <code>DebugTrace</code> object will be returned.
	 * 
	 * The class name, method name, and line number of any callers to the <code>DebugTrace</code>
	 * API will automatically be determined by parsing the stack trace of the executing thread.
	 * These attributes will be set based on the first caller of this API. 
	 * 
	 * @param bundleSymbolicName The symbolic name of the bundle that is requesting a
	 * new instance of a <code>DebugTrace</code>.   
	 * @return A new or existing <code>DebugTrace</code> object for the specified plug-in ID
	 * @since 3.5
	 */
	public abstract DebugTrace newDebugTrace(String bundleSymbolicName);

	/**
	 * Create a new <code>DebugTrace</code> instance for the specified bundle symbolic name.
	 * If a <code>DebugTrace</code> object has already been created for the specified symbolic
	 * name then the existing <code>DebugTrace</code> object will be returned.
	 *
	 * The class name, method name, and line number of any callers to the <code>DebugTrace</code>
	 * API will automatically be determined by parsing the stack trace of the executing thread.
	 * The values of these attributes will be based on the last invocation to the specified traceEntryClass
	 * found in the parsed stack trace.
	 *
	 * @param bundleSymbolicName The symbolic name of the bundle that is requesting a
	 * new instance of a <code>DebugTrace</code>.
	 * @param traceEntryClass The class that is being used to abstract tracing calls for a bundle. 
	 * @return A new or existing <code>DebugTrace</code> object for the specified plug-in ID
	 * @since 3.5
	 */
	public abstract DebugTrace newDebugTrace(String bundleSymbolicName, Class traceEntryClass);
}