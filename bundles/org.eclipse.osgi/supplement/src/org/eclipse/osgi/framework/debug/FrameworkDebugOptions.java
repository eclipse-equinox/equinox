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
package org.eclipse.osgi.framework.debug;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.service.debug.*;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The DebugOptions implementation class that allows accessing the list of debug options specified
 * for the application as well as creating {@link DebugTrace} objects for the purpose of having
 * dynamic enablement of debug tracing.
 * 
 * @since 3.1
 */
public class FrameworkDebugOptions implements DebugOptions, ServiceTrackerCustomizer {

	private static final String OSGI_DEBUG = "osgi.debug"; //$NON-NLS-1$
	public static final String PROP_TRACEFILE = "osgi.tracefile"; //$NON-NLS-1$
	/** monitor used to lock the options maps */
	private final Object lock = new Object();
	/** A current map of all the options with values set */
	private Properties options = null;
	/** A map of all the disabled options with values set at the time debug was disabled */
	private Properties disabledOptions = null;
	/** The singleton object of this class */
	private static FrameworkDebugOptions singleton = null;
	/** The default name of the .options file if loading when the -debug command-line argument is used */
	private static final String OPTIONS = ".options"; //$NON-NLS-1$
	/** A cache of all of the bundles <code>DebugTrace</code> in the format <key,value> --> <bundle name, DebugTrace> */
	protected final static Map debugTraceCache = new HashMap();
	/** The File object to store messages.  This value may be null. */
	protected File outFile = null;
	private volatile BundleContext context;
	private volatile ServiceTracker listenerTracker;

	/**
	 * Internal constructor to create a <code>FrameworkDebugOptions</code> singleton object. 
	 */
	private FrameworkDebugOptions() {
		// if no debug option was specified, don't even bother to try.
		// Must ensure that the options slot is null as this is the signal to the
		// platform that debugging is not enabled.
		String debugOptionsFilename = FrameworkProperties.getProperty(OSGI_DEBUG);
		if (debugOptionsFilename == null)
			return;
		options = new Properties();
		URL optionsFile;
		if (debugOptionsFilename.length() == 0) {
			// default options location is user.dir (install location may be r/o so
			// is not a good candidate for a trace options that need to be updatable by
			// by the user)
			String userDir = FrameworkProperties.getProperty("user.dir").replace(File.separatorChar, '/'); //$NON-NLS-1$
			if (!userDir.endsWith("/")) //$NON-NLS-1$
				userDir += "/"; //$NON-NLS-1$
			debugOptionsFilename = new File(userDir, OPTIONS).toString();
		}
		optionsFile = buildURL(debugOptionsFilename, false);
		if (optionsFile == null) {
			System.out.println("Unable to construct URL for options file: " + debugOptionsFilename); //$NON-NLS-1$
			return;
		}
		System.out.print("Debug options:\n    " + optionsFile.toExternalForm()); //$NON-NLS-1$
		try {
			InputStream input = optionsFile.openStream();
			try {
				options.load(input);
				System.out.println(" loaded"); //$NON-NLS-1$
			} finally {
				input.close();
			}
		} catch (FileNotFoundException e) {
			System.out.println(" not found"); //$NON-NLS-1$
		} catch (IOException e) {
			System.out.println(" did not parse"); //$NON-NLS-1$
			e.printStackTrace(System.out);
		}
		// trim off all the blanks since properties files don't do that.
		for (Iterator i = options.keySet().iterator(); i.hasNext();) {
			Object key = i.next();
			options.put(key, ((String) options.get(key)).trim());
		}
	}

	public void start(BundleContext bc) {
		this.context = bc;
		listenerTracker = new ServiceTracker(bc, DebugOptionsListener.class.getName(), this);
		listenerTracker.open();
	}

	public void stop(BundleContext bc) {
		listenerTracker.close();
		listenerTracker = null;
		this.context = null;
	}

	/**
	 * Returns the singleton instance of <code>FrameworkDebugOptions</code>.
	 * @return the instance of <code>FrameworkDebugOptions</code>
	 */
	public static FrameworkDebugOptions getDefault() {

		if (FrameworkDebugOptions.singleton == null) {
			FrameworkDebugOptions.singleton = new FrameworkDebugOptions();
		}
		return FrameworkDebugOptions.singleton;
	}

	private static URL buildURL(String spec, boolean trailingSlash) {
		if (spec == null)
			return null;
		boolean isFile = spec.startsWith("file:"); //$NON-NLS-1$
		try {
			if (isFile)
				return adjustTrailingSlash(new File(spec.substring(5)).toURL(), trailingSlash);
			return new URL(spec);
		} catch (MalformedURLException e) {
			// if we failed and it is a file spec, there is nothing more we can do
			// otherwise, try to make the spec into a file URL.
			if (isFile)
				return null;
			try {
				return adjustTrailingSlash(new File(spec).toURL(), trailingSlash);
			} catch (MalformedURLException e1) {
				return null;
			}
		}
	}

	private static URL adjustTrailingSlash(URL url, boolean trailingSlash) throws MalformedURLException {
		String file = url.getFile();
		if (trailingSlash == (file.endsWith("/"))) //$NON-NLS-1$
			return url;
		file = trailingSlash ? file + "/" : file.substring(0, file.length() - 1); //$NON-NLS-1$
		return new URL(url.getProtocol(), url.getHost(), file);
	}

	/**
	 * @see DebugOptions#getBooleanOption(String, boolean)
	 */
	public boolean getBooleanOption(String option, boolean defaultValue) {
		String optionValue = getOption(option);
		return optionValue != null ? optionValue.equalsIgnoreCase("true") : defaultValue; //$NON-NLS-1$
	}

	/**
	 * @see DebugOptions#getOption(String)
	 */
	public String getOption(String option) {
		return getOption(option, null);
	}

	/**
	 * @see DebugOptions#getOption(String, String)
	 */
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
	public int getIntegerOption(String option, int defaultValue) {
		String value = getOption(option);
		try {
			return value == null ? defaultValue : Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
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
				final Iterator entrySetIterator = options.entrySet().iterator();
				int i = 0;
				while (entrySetIterator.hasNext()) {
					Map.Entry entry = (Map.Entry) entrySetIterator.next();
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
	public void removeOption(String option) {
		if (option == null)
			return;
		boolean fireChangedEvent = false;
		synchronized (lock) {
			if (options != null) {
				fireChangedEvent = options.remove(option) != null;
			}
		}
		// Send the options change event outside the sync block
		if (fireChangedEvent) {
			int firstSlashIndex = option.indexOf("/"); //$NON-NLS-1$
			if (firstSlashIndex > 0) {
				String symbolicName = option.substring(0, firstSlashIndex);
				optionsChanged(symbolicName);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#setOption(java.lang.String, java.lang.String)
	 */
	public void setOption(String option, String value) {
		boolean fireChangedEvent = false;
		synchronized (lock) {
			if (options != null) {
				// get the current value
				String currentValue = options.getProperty(option);

				if (currentValue != null) {
					if (!currentValue.equals(value)) {
						fireChangedEvent = true;
					}
				} else {
					if (value != null) {
						fireChangedEvent = true;
					}
				}
				if (fireChangedEvent) {
					options.put(option, value.trim());
				}
			}
		}
		// Send the options change event outside the sync block
		if (fireChangedEvent) {
			int firstSlashIndex = option.indexOf("/"); //$NON-NLS-1$
			if (firstSlashIndex > 0) {
				String symbolicName = option.substring(0, firstSlashIndex);
				optionsChanged(symbolicName);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#isDebugEnabled()
	 */
	public boolean isDebugEnabled() {
		synchronized (lock) {
			return options != null;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#setDebugEnabled()
	 */
	public void setDebugEnabled(boolean enabled) {
		boolean fireChangedEvent = false;
		synchronized (lock) {
			if (enabled) {
				if (options != null)
					return;
				// notify the trace that a new session is started
				EclipseDebugTrace.newSession = true;

				// enable platform debugging - there is no .options file
				FrameworkProperties.setProperty(OSGI_DEBUG, ""); //$NON-NLS-1$
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
				FrameworkProperties.clearProperty(OSGI_DEBUG);
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
	public final DebugTrace newDebugTrace(String bundleSymbolicName) {

		return this.newDebugTrace(bundleSymbolicName, null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#createTrace(java.lang.String, java.lang.Class)
	 */
	public final DebugTrace newDebugTrace(String bundleSymbolicName, Class traceEntryClass) {

		DebugTrace debugTrace = null;
		synchronized (FrameworkDebugOptions.debugTraceCache) {
			debugTrace = (DebugTrace) FrameworkDebugOptions.debugTraceCache.get(bundleSymbolicName);
			if (debugTrace == null) {
				debugTrace = new EclipseDebugTrace(bundleSymbolicName, FrameworkDebugOptions.singleton, traceEntryClass);
				FrameworkDebugOptions.debugTraceCache.put(bundleSymbolicName, debugTrace);
			}
		}
		return debugTrace;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#getFile()
	 */
	public final File getFile() {

		return this.outFile;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.osgi.service.debug.DebugOptions#setFile(java.io.File)
	 */
	public synchronized void setFile(final File traceFile) {

		this.outFile = traceFile;
		FrameworkProperties.setProperty(PROP_TRACEFILE, this.outFile.getAbsolutePath());
		// the file changed so start a new session
		EclipseDebugTrace.newSession = true;
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
		ServiceReference[] listenerRefs = null;
		try {
			listenerRefs = bc.getServiceReferences(DebugOptionsListener.class.getName(), "(" + DebugOptions.LISTENER_SYMBOLICNAME + "=" + bundleSymbolicName + ")"); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		} catch (InvalidSyntaxException e) {
			// consider logging; should not happen
		}
		if (listenerRefs == null)
			return;
		for (int i = 0; i < listenerRefs.length; i++) {
			DebugOptionsListener service = (DebugOptionsListener) bc.getService(listenerRefs[i]);
			if (service == null)
				continue;
			try {
				service.optionsChanged(this);
			} catch (Throwable t) {
				// TODO consider logging
			} finally {
				bc.ungetService(listenerRefs[i]);
			}
		}
	}

	public Object addingService(ServiceReference reference) {
		DebugOptionsListener listener = (DebugOptionsListener) context.getService(reference);
		listener.optionsChanged(this);
		return listener;
	}

	public void modifiedService(ServiceReference reference, Object service) {
		// nothing
	}

	public void removedService(ServiceReference reference, Object service) {
		context.ungetService(reference);
	}
}