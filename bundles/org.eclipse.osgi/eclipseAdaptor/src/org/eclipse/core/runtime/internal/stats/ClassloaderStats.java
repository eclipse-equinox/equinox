/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime.internal.stats;

import java.io.*;
import java.util.*;

/**
 * Contains information about the classes and the bundles loaded by a given classloader. Typically there is one classloader per plugin so at levels above boot, this equates to information about
 * classes and bundles in a plugin.
 */
public class ClassloaderStats {
	private String id;
	private long loadingTime; // time spent loading classes
	/**
	 * classes loaded by the plugin (key: class name, value: ClassStats) 
	 */
	private Map<String, ClassStats> classes = Collections.synchronizedMap(new HashMap<String, ClassStats>(20));
	private List<ResourceBundleStats> bundles = new ArrayList<ResourceBundleStats>(2); // bundles loaded

	private boolean keepTraces = false; // indicate whether or not the traces of classes loaded are kept

	// filters to indicate which classes we want to keep the traces
	private static List<String> packageFilters = new ArrayList<String>(4); // filters on a package basis 
	private static Set<String> pluginFilters = new HashSet<String>(5); // filters on a plugin basis

	private static Hashtable<Thread, Stack<ClassStats>> classStacks = new Hashtable<Thread, Stack<ClassStats>>(); // represents the classes that are currently being loaded
	/**
	 * a dictionary of the classloaderStats (key: pluginId, value: ClassloaderStats) 
	 */
	private static Map<String, ClassloaderStats> loaders = Collections.synchronizedMap(new HashMap<String, ClassloaderStats>(20));
	public static File traceFile;

	static {
		if (StatsManager.TRACE_CLASSES || StatsManager.TRACE_BUNDLES)
			initializeTraceOptions();
	}

	private static void initializeTraceOptions() {
		// create the trace file
		String filename = StatsManager.TRACE_FILENAME;
		traceFile = new File(filename);
		traceFile.delete();

		//load the filters
		if (!StatsManager.TRACE_CLASSES)
			return;
		filename = StatsManager.TRACE_FILTERS;
		if (filename == null || filename.length() == 0)
			return;
		try {
			File filterFile = new File(filename);
			System.out.print("Runtime tracing elements defined in: " + filterFile.getAbsolutePath() + "..."); //$NON-NLS-1$ //$NON-NLS-2$
			InputStream input = new FileInputStream(filterFile);
			System.out.println("  Loaded."); //$NON-NLS-1$
			Properties filters = new Properties() {
				private static final long serialVersionUID = 3546359543853365296L;

				public synchronized Object put(Object key, Object value) {
					addFilters((String) key, (String) value);
					return null;
				}
			};
			try {
				filters.load(input);
			} finally {
				input.close();
			}
		} catch (IOException e) {
			System.out.println("  No trace filters loaded."); //$NON-NLS-1$
		}
	}

	protected static void addFilters(String key, String value) {
		String[] filters = StatsManager.getArrayFromList(value);
		if ("plugins".equals(key)) //$NON-NLS-1$
			pluginFilters.addAll(Arrays.asList(filters));
		if ("packages".equals(key)) //$NON-NLS-1$
			packageFilters.addAll(Arrays.asList(filters));
	}

	public static void startLoadingClass(String id, String className) {
		findLoader(id).startLoadClass(className);
	}

	// get and create if does not exist
	private static ClassloaderStats findLoader(String id) {
		synchronized (loaders) {
			ClassloaderStats result = loaders.get(id);
			if (result == null) {
				result = new ClassloaderStats(id);
				loaders.put(id, result);
			}
			return result;
		}
	}

	public static synchronized Stack<ClassStats> getClassStack() {
		Stack<ClassStats> result = classStacks.get(Thread.currentThread());
		if (result == null) {
			result = new Stack<ClassStats>();
			classStacks.put(Thread.currentThread(), result);
		}
		return result;
	}

	public static ClassloaderStats[] getLoaders() {
		//the parameter to toArray is of size zero for thread safety, otherwise this
		//could return an array with null entries if the map shrinks concurrently
		return loaders.values().toArray(new ClassloaderStats[0]);
	}

	public static void endLoadingClass(String id, String className, boolean success) {
		findLoader(id).endLoadClass(className, success);
	}

	public static void loadedBundle(String id, ResourceBundleStats info) {
		findLoader(id).loadedBundle(info);
	}

	public static ClassloaderStats getLoader(String id) {
		return loaders.get(id);
	}

	public ClassloaderStats(String id) {
		this.id = id;
		keepTraces = pluginFilters.contains(id);
	}

	public void addBaseClasses(String[] baseClasses) {
		for (int i = 0; i < baseClasses.length; i++) {
			String name = baseClasses[i];
			if (classes.get(name) == null) {
				ClassStats value = new ClassStats(name, this);
				value.toBaseClass();
				classes.put(name, value);
			}
		}
	}

	private void loadedBundle(ResourceBundleStats bundle) {
		bundles.add(bundle);
	}

	public List<ResourceBundleStats> getBundles() {
		return bundles;
	}

	private synchronized void startLoadClass(String name) {
		getClassStack().push(findClass(name));
	}

	// internal method that return the existing classStats or creates one
	private ClassStats findClass(String name) {
		ClassStats result = classes.get(name);
		return result == null ? new ClassStats(name, this) : result;
	}

	private synchronized void endLoadClass(String name, boolean success) {
		ClassStats current = getClassStack().pop();
		if (!success)
			return;
		if (current.getLoadOrder() >= 0)
			return;

		classes.put(name, current);
		current.setLoadOrder(classes.size());
		current.loadingDone();
		traceLoad(name, current);

		// is there something on the load stack. if so, link them together...
		Stack<ClassStats> classStack = getClassStack();
		if (classStack.size() != 0) {
			// get the time spent loading cli and subtract its load time from the class that requires loading
			ClassStats previous = classStack.peek();
			previous.addTimeLoadingOthers(current.getTimeLoading());
			current.setLoadedBy(previous);
			previous.loaded(current);
		} else {
			loadingTime = loadingTime + current.getTimeLoading();
		}
	}

	private void traceLoad(String name, ClassStats target) {
		// Stack trace code
		if (!keepTraces) {
			boolean found = false;
			for (int i = 0; !found && i < packageFilters.size(); i++)
				if (name.startsWith(packageFilters.get(i)))
					found = true;
			if (!found)
				return;
		}

		// Write the stack trace. The position in the file are set to the corresponding classStat object
		try {
			target.setTraceStart(traceFile.length());
			PrintWriter output = new PrintWriter(new FileOutputStream(traceFile.getAbsolutePath(), true));
			try {
				output.println("Loading class: " + name); //$NON-NLS-1$
				output.println("Class loading stack:"); //$NON-NLS-1$
				output.println("\t" + name); //$NON-NLS-1$
				Stack<ClassStats> classStack = getClassStack();
				for (int i = classStack.size() - 1; i >= 0; i--)
					output.println("\t" + classStack.get(i).getClassName()); //$NON-NLS-1$
				output.println("Stack trace:"); //$NON-NLS-1$
				new Throwable().printStackTrace(output);
			} finally {
				output.close();
			}
			target.setTraceEnd(traceFile.length());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public int getClassLoadCount() {
		return classes.size();
	}

	public long getClassLoadTime() {
		return loadingTime;
	}

	public ClassStats[] getClasses() {
		//the parameter to toArray is of size zero for thread safety, otherwise this
		//could return an array with null entries if the map shrinks concurrently
		return classes.values().toArray(new ClassStats[0]);
	}

	public String getId() {
		return id;
	}
}
