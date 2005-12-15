/*******************************************************************************
 * Copyright (c) 1999, 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.log;

import java.util.*;
import org.eclipse.osgi.framework.eventmgr.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * An array of LogEntries which wraps when full.
 */
public class Activator implements BundleActivator, EventDispatcher, BundleListener, FrameworkListener, ServiceListener, ManagedService {
	protected BundleContext context;

	/** List of LogReaderServices for bundle's LogEntry listeners. */
	protected EventListeners logEvent;
	/** EventManager for event delivery. */
	protected EventManager eventManager;

	protected ServiceRegistration logservice;
	protected ServiceRegistration logreaderservice;
	protected ServiceRegistration logmanagedservice;

	/** default log size value */
	protected static final int DEFAULT_LOG_SIZE = 100;

	/** default log threshold value */
	protected static final int DEFAULT_LOG_THRESHOLD = LogService.LOG_DEBUG;

	/** current logsize value */
	protected int logSize = DEFAULT_LOG_SIZE;
	/** current logthreshold value */
	protected int logThreshold = DEFAULT_LOG_THRESHOLD;

	protected LogEntry[] logEntries;
	protected int head;
	protected int tail;

	/** The timestamp of the last log entry (to avoid duplicates). */
	protected long lastTimestamp = 0;

	/** key into properties for logsize value */
	protected static final String keyLogSize = "log.size"; //$NON-NLS-1$
	/** key into properties for log threshold value */
	protected static final String keyLogThreshold = "log.threshold"; //$NON-NLS-1$
	/** Pid that log service uses when it registers a CM Managed Service */
	protected static final String LOGSERVICEPID = "com.ibm.osg.service.log.Log"; //$NON-NLS-1$

	public Activator() {
	}

	/**
	 * BundleActivator.start method. We can now initialize the bundle
	 * and register the services.
	 *
	 */
	public void start(BundleContext context) {
		this.context = context;

		eventManager = new EventManager();
		logEvent = new EventListeners();

		logEntries = new LogEntry[logSize];
		head = 0;
		tail = 0;
		String initmessage = NLS.bind(LogMsg.Log_created_Log_Size, String.valueOf(logSize), String.valueOf(logThreshold));
		logEntries[0] = new LogEntry(LogService.LOG_INFO, initmessage, context.getBundle(), null, null);

		context.addBundleListener(this);
		context.addServiceListener(this);
		context.addFrameworkListener(this);

		registerLogService();
		registerLogReaderService();

		registerManagedService();
	}

	/**
	 * BundleActivator.stop method. We must now clean up and terminate
	 * execution.
	 *
	 */
	public synchronized void stop(BundleContext context) {
		if (logmanagedservice != null) {
			logmanagedservice.unregister();
			logmanagedservice = null;
		}

		/* remove my listeners before unregistering myself */
		this.context.removeBundleListener(this);
		this.context.removeServiceListener(this);
		this.context.removeFrameworkListener(this);

		if (logservice != null) {
			logservice.unregister();
			logservice = null;
		}
		if (logreaderservice != null) {
			logreaderservice.unregister();
			logreaderservice = null;
		}

		/* destroy my event manager */
		if (logEvent != null) {
			logEvent.removeAllListeners();
			logEvent = null;
		}

		if (eventManager != null) {
			eventManager.close();
			eventManager = null;
		}

		logEntries = null;

		this.context = null;
	}

	/**
	 *  Log a bundle message.  This is used internally by all of the public
	 *  log() methods as the common point through which all logging must
	 *  go through.
	 *  @param level The severity of the message.  (Should be one of the four
	 *               predefined severities.)
	 *  @param message Human readable string describing the condition.
	 *  @param context The BundleContext creating the log entry
	 *  @param sd The ServiceDescription of the service that this message
	 *  is associated with.
	 *  @param exception The exception that reflects the condition.
	 */
	protected void log(int level, String message, Bundle bundle, ServiceReference reference, Throwable exception)

	{
		LogEntry logentry = new LogEntry(level, message, bundle, reference, exception);

		synchronized (this) {
			if (context == null) {
				return;
			}

			// Make the timestamp unique, log listener's might use it as a key
			if (logentry.time <= lastTimestamp) {
				logentry.time = ++lastTimestamp;
			} else {
				lastTimestamp = logentry.time;
			}

			if (level <= logThreshold) { /* if level is within logging threshold */
				addLogEntry(logentry);
			}
		}

		/* queue to hold set of listeners */
		ListenerQueue listeners = new ListenerQueue(eventManager);

		/* queue to hold set of BundleContexts w/ listeners */
		ListenerQueue contexts = new ListenerQueue(eventManager);

		/* add set of BundleContexts w/ listeners to queue */
		contexts.queueListeners(logEvent, this);

		/* synchronously dispatch to populate listeners queue */
		contexts.dispatchEventSynchronous(0, listeners);

		/* dispatch event to set of listeners */
		listeners.dispatchEventAsynchronous(0, logentry);
	}

	public void dispatchEvent(Object l, Object lo, int action, Object object) {
		LogReaderService logreader = (LogReaderService) l;

		EventListeners listeners = logreader.logEvent;

		if (listeners != null) {
			ListenerQueue queue = (ListenerQueue) object;

			queue.queueListeners(listeners, logreader);
		}
	}

	/**
	 * This method must be called while synchronized.
	 *
	 */
	protected void addLogEntry(LogEntry logentry) {
		tail = (tail + 1) % logSize;
		logEntries[tail] = logentry;
		if (head == tail) {
			head = (head + 1) % logSize;
		}
	}

	protected synchronized Enumeration logEntries() {
		final int head = this.head;
		final int tail = this.tail;
		final int logSize = this.logSize;
		final LogEntry[] logEntries = this.logEntries;

		return (new Enumeration() {
			private int item;
			private LogEntry[] enumentries;

			{
				// The array is created in the log reader's memory space
				if (head <= tail) {
					item = tail - head + 1;
					enumentries = new LogEntry[item];
					System.arraycopy(logEntries, head, enumentries, 0, item);
				} else { // log is full
					int firstcopy = logSize - head;
					item = firstcopy + tail + 1;
					enumentries = new LogEntry[item];
					System.arraycopy(logEntries, head, enumentries, 0, firstcopy);
					System.arraycopy(logEntries, 0, enumentries, firstcopy, item - firstcopy);
				}
			}

			public boolean hasMoreElements() {
				if (item > 0) {
					return (true);
				}
				enumentries = null; /* release the storage */
				return (false);
			}

			/** Returns an Object of type LogEntry */
			public Object nextElement() {
				if (item > 0) {
					item--;
					LogEntry entry = (enumentries[item]).copy();
					enumentries[item] = null; /* release the storage as we go */
					return (entry);
				}
				enumentries = null; /* release the storage */
				throw new NoSuchElementException();
			}
		});
	}

	/**
	 * BundleListener.bundleChanged method.
	 *
	 */
	public void bundleChanged(BundleEvent event) {
		log(LogService.LOG_INFO, getBundleEventTypeName(event.getType()), event.getBundle(), null, null);
	}

	/**
	 * ServiceListener.serviceChanged method.
	 *
	 */
	public void serviceChanged(ServiceEvent event) {
		ServiceReference reference = event.getServiceReference();

		int eventType = event.getType();

		int logType = (eventType == ServiceEvent.MODIFIED) ? LogService.LOG_DEBUG : LogService.LOG_INFO;

		log(logType, getServiceEventTypeName(eventType), reference.getBundle(), reference, null);
	}

	/**
	 * FrameworkListener.frameworkEvent method.
	 *
	 */
	public void frameworkEvent(FrameworkEvent event) {
		int type = event.getType();

		if (type == FrameworkEvent.ERROR) {
			log(LogService.LOG_ERROR, getFrameworkEventTypeName(type), event.getBundle(), null, event.getThrowable());
		} else {
			log(LogService.LOG_INFO, getFrameworkEventTypeName(type), event.getBundle(), null, null);
		}
	}

	/**
	 * Convert BundleEvent type to a string.
	 *
	 */
	protected static String getBundleEventTypeName(int type) {
		switch (type) {
			case BundleEvent.INSTALLED :
				return ("BundleEvent INSTALLED"); //$NON-NLS-1$

			case BundleEvent.RESOLVED :
				return ("BundleEvent RESOLVED"); //$NON-NLS-1$

			case BundleEvent.STARTED :
				return ("BundleEvent STARTED"); //$NON-NLS-1$

			case BundleEvent.STARTING :
				return ("BundleEvent STARTING"); //$NON-NLS-1$
				
			case BundleEvent.STOPPED :
				return ("BundleEvent STOPPED"); //$NON-NLS-1$

			case BundleEvent.STOPPING :
				return ("BundleEvent STOPPING"); //$NON-NLS-1$
				
			case BundleEvent.UNINSTALLED :
				return ("BundleEvent UNINSTALLED"); //$NON-NLS-1$

			case BundleEvent.UNRESOLVED :
				return ("BundleEvent UNRESOLVED"); //$NON-NLS-1$
			
			case BundleEvent.UPDATED :
				return ("BundleEvent UPDATED"); //$NON-NLS-1$

			default :
				return (NLS.bind(LogMsg.BundleEvent, Integer.toHexString(type)));
		}
	}

	/**
	 * Convert ServiceEvent type to a string.
	 *
	 */
	protected static String getServiceEventTypeName(int type) {
		switch (type) {
			case ServiceEvent.REGISTERED :
				return ("ServiceEvent REGISTERED"); //$NON-NLS-1$

			case ServiceEvent.MODIFIED :
				return ("ServiceEvent MODIFIED"); //$NON-NLS-1$

			case ServiceEvent.UNREGISTERING :
				return ("ServiceEvent UNREGISTERING"); //$NON-NLS-1$

			default :
				return (NLS.bind(LogMsg.ServiceEvent, Integer.toHexString(type)));
		}
	}

	/**
	 * Convert FrameworkEvent type to a string.
	 *
	 */
	protected static String getFrameworkEventTypeName(int type) {
		switch (type) {
			case FrameworkEvent.ERROR :
				return ("FrameworkEvent ERROR"); //$NON-NLS-1$

			case FrameworkEvent.INFO :
				return ("FrameworkEvent INFO"); //$NON-NLS-1$
				
			case FrameworkEvent.PACKAGES_REFRESHED :
				return ("FrameworkEvent PACKAGES REFRESHED"); //$NON-NLS-1$

			case FrameworkEvent.STARTED :
				return ("FrameworkEvent STARTED"); //$NON-NLS-1$

			case FrameworkEvent.STARTLEVEL_CHANGED :
				return ("FrameworkEvent STARTLEVEL CHANGED"); //$NON-NLS-1$
				
			case FrameworkEvent.WARNING :
				return ("FrameworkEvent WARNING"); //$NON-NLS-1$

			default :
				return (NLS.bind(LogMsg.FrameworkEvent, Integer.toHexString(type)));
		}
	}

	/**
	 * Update the configuration for a ManagedService.
	 *
	 * <p> When the implementation of updated(Dictionary) detects any kind of
	 * error in the configuration properties, it should create a
	 * new ConfigurationException which describes the problem.  This
	 * can allow a management system to provide useful information to
	 * a human administrator.
	 * <p> If this method throws any other Exception, the
	 * ConfigurationAdmin must catch it and should log it.
	 * <p> The ConfigurationAdmin must call this method on a thread
	 * other than the thread which initiated the call-back. This
	 * implies that implementors of ManagedService can be assured
	 * that the call-back will not take place during registration
	 * when they execute the registration in a synchronized method.
	 *
	 * @param properties configuration properties, or null
	 * @throws ConfigurationException when the update fails
	 **/
	public synchronized void updated(Dictionary properties) throws ConfigurationException {
		/* Since updated is called asynchronously, we may have stopped
		 * after the decision was made to call.
		 */
		if (context != null) {
			if (properties == null) {
				/* We have no configuration; we will just use our defaults */
				return;
			}

			int size = logSize;
			int threshold = logThreshold;

			/* Get configuration values and validate */
			Object property = properties.get(keyLogSize);
			if (property != null) /* if null we will just use the default */
			{
				if (!(property instanceof Integer)) {
					throw new ConfigurationException(keyLogSize, "not an Integer"); //$NON-NLS-1$
				}

				size = ((Integer) property).intValue();

				if ((size < 10) || (size > 2000)) {
					throw new ConfigurationException(keyLogSize, "must be in the range 10-2000"); //$NON-NLS-1$
				}
			}

			property = properties.get(keyLogThreshold);
			if (property != null) /* if null we will just use the default */
			{
				if (!(property instanceof Integer)) {
					throw new ConfigurationException(keyLogThreshold, "not an Integer"); //$NON-NLS-1$
				}

				threshold = ((Integer) property).intValue();

				if ((threshold < LogService.LOG_ERROR) || (threshold > LogService.LOG_DEBUG)) {
					throw new ConfigurationException(keyLogThreshold, "must be one of the LogService defined Log levels"); //$NON-NLS-1$
				}
			}

			/* Configuration values have been validated */
			if (size != logSize) {
				updateLogSize(size);
			}

			if (threshold != logThreshold) {
				updateLogThreshold(threshold);
			}
		}
	}

	/**
	 * This method must be called while synchronized.
	 *
	 */
	private void updateLogSize(int size) {
		LogEntry[] newlog = new LogEntry[size];

		if (head <= tail) { /* log is not full */
			int count = tail - head + 1;
			if (size > count) { /* is new log bigger? */
				System.arraycopy(logEntries, head, newlog, 0, count);
				tail = count - 1;
			} else { /* new log smaller */
				System.arraycopy(logEntries, head + count - size, newlog, 0, size);
				tail = size - 1;
			}
		} else { /* log is full */
			int count = tail + 1 + logSize - head;
			if (size > count) { /* is new log bigger? */
				int boundary = logSize - head;
				System.arraycopy(logEntries, head, newlog, 0, boundary);
				System.arraycopy(logEntries, 0, newlog, boundary, count - boundary);
				tail = count - 1;
			} else { /* new log smaller */
				if ((tail + 1) < size) { /* is log big enough to hold first half? */
					int boundary = size - (tail + 1);
					System.arraycopy(logEntries, logSize - boundary, newlog, 0, boundary);
					System.arraycopy(logEntries, 0, newlog, boundary, tail + 1);
				} else {
					System.arraycopy(logEntries, tail + 1 - size, newlog, 0, size); //9626
				}
				tail = size - 1;
			}
		}

		logEntries = newlog;
		logSize = size;

		head = 0;

		String changemessage = NLS.bind(LogMsg.Log_modified_Log_Size, String.valueOf(logSize));

		log(LogService.LOG_INFO, changemessage, context.getBundle(), null, null);
	}

	/**
	 * This method must be called while synchronized.
	 *
	 */
	private void updateLogThreshold(int threshold) {
		logThreshold = threshold;

		String changemessage = NLS.bind(LogMsg.Log_modified_Log_Threshold, String.valueOf(logThreshold));

		log(LogService.LOG_INFO, changemessage, context.getBundle(), null, null);
	}

	protected void registerManagedService() {
		/* Register a Managed Service to handle updates to the Log configuration values */
		Hashtable properties = new Hashtable(7);

		properties.put(Constants.SERVICE_VENDOR, "IBM"); //$NON-NLS-1$
		properties.put(Constants.SERVICE_DESCRIPTION, LogMsg.OSGi_Log_Service_IBM_Implementation);
		properties.put(Constants.SERVICE_PID, LOGSERVICEPID);

		logmanagedservice = context.registerService(ManagedService.class.getName(), this, properties);
	}

	private void registerLogService() {
		Hashtable properties = new Hashtable(7);

		properties.put(Constants.SERVICE_VENDOR, "IBM"); //$NON-NLS-1$
		properties.put(Constants.SERVICE_DESCRIPTION, LogMsg.OSGi_Log_Service_IBM_Implementation);
		properties.put(Constants.SERVICE_PID, LogService.class.getName());

		logservice = context.registerService(org.osgi.service.log.LogService.class.getName(), new LogServiceFactory(this), properties);

	}

	private void registerLogReaderService() {
		Hashtable properties = new Hashtable(7);

		properties.put(Constants.SERVICE_VENDOR, "IBM"); //$NON-NLS-1$
		properties.put(Constants.SERVICE_DESCRIPTION, LogMsg.OSGi_Log_Service_IBM_Implementation);
		properties.put(Constants.SERVICE_PID, LogReaderService.class.getName());

		logreaderservice = context.registerService(org.osgi.service.log.LogReaderService.class.getName(), new LogReaderServiceFactory(this), properties);
	}
}
