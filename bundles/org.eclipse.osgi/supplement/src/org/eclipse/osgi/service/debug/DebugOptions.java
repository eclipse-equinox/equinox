/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
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
import java.util.Map;

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
	 * defaultValue is returned if no such option is found or if debug is not enabled.   
	 * 
	 * <p>
	 * Options are specified in the general form <i>&lt;Bundle-SymbolicName&gt;/&lt;option-path&gt;</i>.  
	 * For example, <code>org.eclipse.core.runtime/debug</code>
	 * </p>
	 * 
	 * @param option the name of the option to lookup
	 * @param defaultValue the value to return if no such option is found
	 * @return the value of the requested debug option or the
	 * defaultValue if no such option is found.
	 */
	public abstract boolean getBooleanOption(String option, boolean defaultValue);

	/**
	 * Returns the identified option.  A <code>null</code> value
	 * is returned if no such option is found or if debug is not enabled.
	 * 
	 * <p>
	 * Options are specified
	 * in the general form <i>&lt;Bundle-SymbolicName&gt;/&lt;option-path&gt;</i>.  
	 * For example, <code>org.eclipse.core.runtime/debug</code>
	 *</p>
	 * 
	 * @param option the name of the option to lookup
	 * @return the value of the requested debug option or <code>null</code>
	 */
	public abstract String getOption(String option);

	/**
	 * Returns the identified option.  The specified defaultValue is 
	 * returned if no such option is found or if debug is not enabled.
	 * 
	 * <p>
	 * Options are specified
	 * in the general form <i>&lt;Bundle-SymbolicName&gt;/&lt;option-path&gt;</i>.  
	 * For example, <code>org.eclipse.core.runtime/debug</code>
	 * </p>
	 * 
	 * @param option the name of the option to lookup
	 * @param defaultValue the value to return if no such option is found
	 * @return the value of the requested debug option or the
	 * defaultValue if no such option is found.
	 */
	public abstract String getOption(String option, String defaultValue);

	/**
	 * Returns the identified option as an int value.  The specified
	 * defaultValue is returned if no such option is found or if a 
	 * NumberFormatException is thrown while converting the option value 
	 * to an integer or if debug is not enabled.
	 * 
	 * <p>
	 * Options are specified
	 * in the general form <i>&lt;Bundle-SymbolicName&gt;/&lt;option-path&gt;</i>.  
	 * For example, <code>org.eclipse.core.runtime/debug</code>
	 * </p>
	 * 
	 * @param option the name of the option to lookup
	 * @param defaultValue the value to return if no such option is found
	 * @return the value of the requested debug option or the
	 * defaultValue if no such option is found.
	 */
	public abstract int getIntegerOption(String option, int defaultValue);

	/**
	 * Returns a snapshot of the current options.  All 
	 * keys and values are of type <code>String</code>.  If no
	 * options are set then an empty map is returned.
	 * <p>
	 * If debug is not enabled then the snapshot of the current disabled 
	 * values is returned. See {@link DebugOptions#setDebugEnabled(boolean)}.
	 * </p>
	 * @return a snapshot of the current options.
	 * @since 3.6
	 */
	public Map<String, String> getOptions();

	/**
	 * Sets the identified option to the identified value.  If debug is 
	 * not enabled then the specified option is not changed.
	 * @param option the name of the option to set
	 * @param value the value of the option to set
	 */
	public abstract void setOption(String option, String value);

	/**
	 * Sets the current option key/value pairs to the specified options.
	 * The specified map replaces all keys and values of the current debug options.
	 * An <code>IllegalArgumentException</code> is thrown if any key or value 
	 * in the specified map is not of type <code>String</code>.
	 * <p>
	 * If debug is not enabled then the specified options are saved as
	 * the disabled values and no notifications will be sent. 
	 * See {@link DebugOptions#setDebugEnabled(boolean)}.
	 * If debug is enabled then notifications will be sent to the 
	 * listeners which have options that have been changed, added or removed.
	 * </p>

	 * @param options the new set of options
	 * @since 3.6
	 */
	public abstract void setOptions(Map<String, String> options);

	/**
	 * Removes the identified option.  If debug is not enabled then
	 * the specified option is not removed.
	 * @param option the name of the option to remove
	 * @since 3.5
	 */
	public abstract void removeOption(String option);

	/**
	 * Returns true if debugging/tracing is currently enabled.
	 * @return true if debugging/tracing is currently enabled;  Otherwise false is returned.
	 * @since 3.5
	 */
	public abstract boolean isDebugEnabled();

	/**
	 * Enables or disables debugging/tracing.
	 * <p>
	 * When debug is disabled all debug options are unset.
	 * When disabling debug the current debug option values are
	 * stored in memory as disabled values.  If debug is re-enabled the
	 * disabled values will be set back and enabled.  The disabled values 
	 * are only stored in memory and if the framework is restarted then 
	 * the disabled option values will be lost.
	 * </p>
	 * @param value If <code>true</code>, debug is enabled, otherwise
	 * debug is disabled.
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
	public abstract DebugTrace newDebugTrace(String bundleSymbolicName, Class<?> traceEntryClass);
}