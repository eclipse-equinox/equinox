/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
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
package org.eclipse.osgi.internal.debug;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.eclipse.osgi.internal.location.LocationHelper;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The DebugOptions implementation class that allows accessing the list of debug options specified
 * for the application as well as creating {@link DebugTrace} objects for the purpose of having
 * dynamic enablement of debug tracing.
 *
 * @since 3.1
 */
public class FrameworkDebugOptions implements DebugOptions, ServiceTrackerCustomizer<DebugOptionsListener, DebugOptionsListener> {

	private static final String OSGI_DEBUG = "osgi.debug"; //$NON-NLS-1$
	private static final String OSGI_DEBUG_VERBOSE = "osgi.debug.verbose"; //$NON-NLS-1$
	public static final String PROP_TRACEFILE = "osgi.tracefile"; //$NON-NLS-1$
	/** The default name of the .options file if loading when the -debug command-line argument is used */
	private static final String OPTIONS = ".options"; //$NON-NLS-1$

	/** A lock object used to synchronize access to the trace file */
	private final static Object writeLock = new Object();
	/** monitor used to lock the options maps */
	private final Object lock = new Object();
	/** A current map of all the options with values set */
	private Properties options = null;
	/** A map of all the disabled options with values set at the time debug was disabled */
	private Properties disabledOptions = null;
	/** A cache of all of the bundles <code>DebugTrace</code> in the format <key,value> --> <bundle name, DebugTrace> */
	protected final Map<String, DebugTrace> debugTraceCache = new HashMap<>();
	/** The File object to store messages.  This value may be null. */
	protected File outFile = null;
	/** Is verbose debugging enabled?  Changing this value causes a new tracing session to start. */
	protected boolean verboseDebug = true;
	/** A flag to determine if the message being written is done to a new file (i.e. should the header information be written) */
	private boolean newSession = true;
	private final EquinoxConfiguration environmentInfo;
	private volatile BundleContext context;
	private volatile ServiceTracker<DebugOptionsListener, DebugOptionsListener> listenerTracker;

	public FrameworkDebugOptions(EquinoxConfiguration environmentInfo) {
		this.environmentInfo = environmentInfo;
		// check if verbose debugging was set during initialization.  This needs to be set even if debugging is disabled
		this.verboseDebug = Boolean.valueOf(environmentInfo.getConfiguration(OSGI_DEBUG_VERBOSE, Boolean.TRUE.toString())).booleanValue();
		// if no debug option was specified, don't even bother to try.
		// Must ensure that the options slot is null as this is the signal to the
		// platform that debugging is not enabled.
		String debugOptionsFilename = environmentInfo.getConfiguration(OSGI_DEBUG);
		if (debugOptionsFilename == null)
			return;
		options = new Properties();
		URL optionsFile;
		if (debugOptionsFilename.length() == 0) {
			// default options location is user.dir (install location may be r/o so
			// is not a good candidate for a trace options that need to be updatable by
			// by the user)
			String userDir = System.getProperty("user.dir").replace(File.separatorChar, '/'); //$NON-NLS-1$
			if (!userDir.endsWith("/")) //$NON-NLS-1$
				userDir += "/"; //$NON-NLS-1$
			debugOptionsFilename = new File(userDir, OPTIONS).toString();
		}
		optionsFile = LocationHelper.buildURL(debugOptionsFilename, false);
		if (optionsFile == null) {
			System.out.println("Unable to construct URL for options file: " + debugOptionsFilename); //$NON-NLS-1$
			return;
		}
		System.out.print("Debug options:\n    " + optionsFile.toExternalForm()); //$NON-NLS-1$
		try {
			try (InputStream input = LocationHelper.getStream(optionsFile)) {
				options.load(input);
				System.out.println(" loaded"); //$NON-NLS-1$
			}
		} catch (FileNotFoundException e) {
			System.out.println(" not found"); //$NON-NLS-1$
		} catch (IOException e) {
			System.out.println(" did not parse"); //$NON-NLS-1$
			e.printStackTrace(System.out);
		}
		// trim off all the blanks since properties files don't do that.
		for (Object key : options.keySet()) {
			options.put(key, ((String) options.get(key)).trim());
		}
	}

	public void start(BundleContext bc) {
		this.context = bc;
		listenerTracker = new ServiceTracker<>(bc, DebugOptionsListener.class.getName(), this);
		listenerTracker.open();
	}

	public void stop(BundleContext bc) {
		listenerTracker.close();
		listenerTracker = null;
		this.context = null;
	}

	/**
	 * @see DebugOptions#getBooleanOption(String, boolean)
	 */
	@Override
	public boolean getBooleanOption(String option, boolean defaultValue) {
		String optionValue = getOption(option);
		return optionValue != null ? optionValue.equalsIgnoreCase("true") : defaultValue; //$NON-NLS-1$
	}

	/**
	 * @see DebugOptions#getOption(String)
	 */
	@Override
	public String getOption(String option) {
		return getOption(option, null);
	}

	/**
	 * @see DebugOptions#getOption(String, String)
	 */
	@Override
	public String getOption(String option, String defaultValue) {
		synchronized (lock) {
			if (options != null) {
				return options.getProperty(option, defaultValue);
			}
		}
		return defaultValue;
	}

	/**
	 * @see DebugOptions#getIntegerOption(String, int)
	 */
	@Override
	public int getIntegerOption(String option, int defaultValue) {
		String value = getOption(option);
		try {
			return value == null ? defaultValue : Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	@Override
	public Map<String, String> getOptions() {
		Map<String, String> snapShot = new HashMap<>();
		synchronized (lock) {
			if (options != null)
				snapShot.putAll((Map) options);
			else if (disabledOptions != null)
				snapShot.putAll((Map) disabledOptions);
		}
		return snapShot;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#getAllOptions()
	 */
	String[] getAllOptions() {

		String[] optionsArray = null;
		synchronized (lock) {
			if (options != null) {
				optionsArray = new String[options.size()];
				int i = 0;
				for (Entry<Object, Object> entry : options.entrySet()) {
					optionsArray[i] = ((String) entry.getKey()) + "=" + ((String) entry.getValue()); //$NON-NLS-1$
					i++;
				}
			}
		}
		if (optionsArray == null) {
			optionsArray = new String[1]; // TODO this is strange; null is the only element so we can print null in writeSession
		}
		return optionsArray;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#removeOption(java.lang.String)
	 */
	@Override
	public void removeOption(String option) {
		if (option == null)
			return;
		String fireChangedEvent = null;
		synchronized (lock) {
			if (options != null && options.remove(option) != null) {
				fireChangedEvent = getSymbolicName(option);
			}
		}
		// Send the options change event outside the sync block
		if (fireChangedEvent != null) {
			optionsChanged(fireChangedEvent);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#setOption(java.lang.String, java.lang.String)
	 */
	@Override
	public void setOption(String option, String value) {

		if (option == null || value == null) {
			throw new IllegalArgumentException("The option and value must not be null."); //$NON-NLS-1$
		}
		String fireChangedEvent = null;
		value = value != null ? value.trim() : null;
		synchronized (lock) {
			if (options != null) {
				// get the current value
				String currentValue = options.getProperty(option);

				if (currentValue != null) {
					if (!currentValue.equals(value)) {
						fireChangedEvent = getSymbolicName(option);
					}
				} else {
					if (value != null) {
						fireChangedEvent = getSymbolicName(option);
					}
				}
				if (fireChangedEvent != null) {
					options.put(option, value);
				}
			}
		}
		// Send the options change event outside the sync block
		if (fireChangedEvent != null) {
			optionsChanged(fireChangedEvent);
		}
	}

	private String getSymbolicName(String option) {
		int firstSlashIndex = option.indexOf('/');
		if (firstSlashIndex > 0)
			return option.substring(0, firstSlashIndex);
		return null;
	}

	@SuppressWarnings("cast")
	@Override
	public void setOptions(Map<String, String> ops) {
		if (ops == null)
			throw new IllegalArgumentException("The options must not be null."); //$NON-NLS-1$
		Properties newOptions = new Properties();
		for (Map.Entry<String, String> entry : ops.entrySet()) {
			if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String))
				throw new IllegalArgumentException("Option keys and values must be of type String: " + entry.getKey() + "=" + entry.getValue()); //$NON-NLS-1$ //$NON-NLS-2$
			newOptions.put(entry.getKey(), entry.getValue().trim());
		}
		Set<String> fireChangesTo = null;

		synchronized (lock) {
			if (options == null) {
				disabledOptions = newOptions;
				// no events to fire
				return;
			}
			fireChangesTo = new HashSet<>();
			// first check for removals
			for (Object object : options.keySet()) {
				String key = (String) object;
				if (!newOptions.containsKey(key)) {
					String symbolicName = getSymbolicName(key);
					if (symbolicName != null)
						fireChangesTo.add(symbolicName);
				}
			}
			// now check for changes to existing values
			for (Map.Entry<Object, Object> entry : newOptions.entrySet()) {
				String existingValue = (String) options.get(entry.getKey());
				if (!entry.getValue().equals(existingValue)) {
					String symbolicName = getSymbolicName((String) entry.getKey());
					if (symbolicName != null)
						fireChangesTo.add(symbolicName);
				}
			}
			// finally set the actual options
			options = newOptions;
		}
		if (fireChangesTo != null)
			for (String string : fireChangesTo)
				optionsChanged(string);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#isDebugEnabled()
	 */
	@Override
	public boolean isDebugEnabled() {
		synchronized (lock) {
			return options != null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#setDebugEnabled()
	 */
	@Override
	public void setDebugEnabled(boolean enabled) {
		boolean fireChangedEvent = false;
		synchronized (lock) {
			if (enabled) {
				if (options != null)
					return;
				// notify the trace that a new session is started
				this.newSession = true;

				// enable platform debugging - there is no .options file
				environmentInfo.setConfiguration(OSGI_DEBUG, ""); //$NON-NLS-1$
				if (disabledOptions != null) {
					options = disabledOptions;
					disabledOptions = null;
					// fire changed event to indicate some options were re-enabled
					fireChangedEvent = true;
				} else {
					options = new Properties();
				}
			} else {
				if (options == null)
					return;
				// disable platform debugging.
				environmentInfo.clearConfiguration(OSGI_DEBUG);
				if (options.size() > 0) {
					// Save the current options off in case debug is re-enabled
					disabledOptions = options;
					// fire changed event to indicate some options were disabled
					fireChangedEvent = true;
				}
				options = null;
			}
		}
		if (fireChangedEvent) {
			// (Bug 300911) need to fire event to listeners that options have been disabled
			optionsChanged("*"); //$NON-NLS-1$
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#createTrace(java.lang.String)
	 */
	@Override
	public final DebugTrace newDebugTrace(String bundleSymbolicName) {

		return this.newDebugTrace(bundleSymbolicName, null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#createTrace(java.lang.String, java.lang.Class)
	 */
	@Override
	public final DebugTrace newDebugTrace(String bundleSymbolicName, Class<?> traceEntryClass) {

		DebugTrace debugTrace = null;
		synchronized (debugTraceCache) {
			debugTrace = debugTraceCache.get(bundleSymbolicName);
			if (debugTrace == null) {
				debugTrace = new EclipseDebugTrace(bundleSymbolicName, this, traceEntryClass);
				debugTraceCache.put(bundleSymbolicName, debugTrace);
			}
		}
		return debugTrace;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#getFile()
	 */
	@Override
	public final File getFile() {

		return this.outFile;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#setFile(java.io.File)
	 */
	@Override
	public void setFile(final File traceFile) {
		synchronized (lock) {
			this.outFile = traceFile;
			if (this.outFile != null)
				environmentInfo.setConfiguration(PROP_TRACEFILE, this.outFile.getAbsolutePath());
			else
				environmentInfo.clearConfiguration(PROP_TRACEFILE);
			// the file changed so start a new session
			this.newSession = true;
		}
	}

	boolean newSession() {
		synchronized (lock) {
			if (newSession) {
				this.newSession = false;
				return true;
			}
			return false;
		}
	}

	Object getWriteLock() {
		return writeLock;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#getVerbose()
	 */
	boolean isVerbose() {

		return this.verboseDebug;
	}

	EquinoxConfiguration getConfiguration() {
		return this.environmentInfo;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#setVerbose(boolean)
	 */
	public void setVerbose(final boolean verbose) {
		synchronized (lock) {
			this.verboseDebug = verbose;
			// the verbose flag changed so start a new session
			this.newSession = true;
		}
	}

	/**
	 * Notifies the trace listener for the specified bundle that its option-path has changed.
	 * @param bundleSymbolicName The bundle of the owning trace listener to notify.
	 */
	private void optionsChanged(String bundleSymbolicName) {
		// use osgi services to get the listeners
		BundleContext bc = context;
		if (bc == null)
			return;
		// do not use the service tracker because that is only used to call all listeners initially when they are registered
		// here we only want the services with the specified name.
		ServiceReference<?>[] listenerRefs = null;
		try {
			listenerRefs = bc.getServiceReferences(DebugOptionsListener.class.getName(), "(" + DebugOptions.LISTENER_SYMBOLICNAME + "=" + bundleSymbolicName + ")"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		} catch (InvalidSyntaxException e) {
			// consider logging; should not happen
		}
		if (listenerRefs == null)
			return;
		for (ServiceReference<?> listenerRef : listenerRefs) {
			DebugOptionsListener service = (DebugOptionsListener) bc.getService(listenerRef);
			if (service == null)
				continue;
			try {
				service.optionsChanged(this);
			}catch (Throwable t) {
				// TODO consider logging
			} finally {
				bc.ungetService(listenerRef);
			}
		}
	}

	@Override
	public DebugOptionsListener addingService(ServiceReference<DebugOptionsListener> reference) {
		DebugOptionsListener listener = context.getService(reference);
		listener.optionsChanged(this);
		return listener;
	}

	@Override
	public void modifiedService(ServiceReference<DebugOptionsListener> reference, DebugOptionsListener service) {
		// nothing
	}

	@Override
	public void removedService(ServiceReference<DebugOptionsListener> reference, DebugOptionsListener service) {
		context.ungetService(reference);
	}
}
