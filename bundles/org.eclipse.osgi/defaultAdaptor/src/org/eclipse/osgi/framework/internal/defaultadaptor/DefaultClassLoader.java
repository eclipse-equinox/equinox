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

package org.eclipse.osgi.framework.internal.defaultadaptor;

import java.io.*;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.Msg;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;

/**
 * A concrete implementation of BundleClassLoader.  This implementation 
 * consolidates all Bundle-ClassPath entries into a single ClassLoader.
 */
public class DefaultClassLoader
	extends org.eclipse.osgi.framework.adaptor.BundleClassLoader {

	/**
	 * The BundleData object for this BundleClassLoader
	 */
	protected DefaultBundleData hostdata;

	/**
	 * The BundleFiles for this BundleClassLoader.  Each BundleFile object
	 * represents on Bundle-ClassPath entry.
	 */
	protected BundleFile[] bundleFiles;

	protected Vector fragClasspaths;
	
	/**
	 * The buffer size to use when loading classes.  This value is used 
	 * only if we cannot determine the size of the class we are loading.
	 */
	protected int buffersize = 8*1024;

	/**
	 * BundleClassLoader constructor.
	 * @param delegate The ClassLoaderDelegate for this ClassLoader.
	 * @param domain The ProtectionDomain for this ClassLoader.
	 * @param bundleclasspath An array of Bundle-ClassPath entries to
	 * use for loading classes and resources.  This is specified by the 
	 * Bundle-ClassPath manifest entry.
	 * @param bundledata The BundleData for this ClassLoader
	 */
	public DefaultClassLoader(ClassLoaderDelegate delegate, ProtectionDomain domain, String[] classpath, DefaultBundleData bundledata)
	{
		this(delegate,domain,classpath,null,bundledata);
	}

	/**
	 * BundleClassLoader constructor.
	 * @param delegate The ClassLoaderDelegate for this ClassLoader.
	 * @param domain The ProtectionDomain for this ClassLoader.
	 * @param bundleclasspath An array of Bundle-ClassPath entries to
	 * use for loading classes and resources.  This is specified by the 
	 * Bundle-ClassPath manifest entry.
	 * @param parent The parent ClassLoader.
	 * @param bundledata The BundleData for this ClassLoader
	 */
	public DefaultClassLoader(ClassLoaderDelegate delegate, ProtectionDomain domain, String[] classpath, ClassLoader parent, DefaultBundleData bundledata)
	{
		super(delegate,domain,classpath,parent);
		this.hostdata = bundledata;

		try
		{
			hostdata.open(); /* make sure the BundleData is open */
		} catch (IOException e)
		{
			hostdata.adaptor.getEventPublisher().publishFrameworkEvent(
					FrameworkEvent.ERROR,hostdata.getBundle(),e);
		}

		bundleFiles = buildClasspath(classpath, hostdata);
	}

	/**
	 * Attaches the BundleData for a fragment to this BundleClassLoader.
	 * The Fragment BundleData resources must be appended to the end of
	 * this BundleClassLoader's classpath.  Fragment BundleData resources 
	 * must be searched ordered by Bundle ID's.  
	 * @param bundledata The BundleData of the fragment.
	 * @param domain The ProtectionDomain of the resources of the fragment.
	 * Any classes loaded from the fragment's BundleData must belong to this
	 * ProtectionDomain.
	 * @param classpath An array of Bundle-ClassPath entries to
	 * use for loading classes and resources.  This is specified by the 
	 * Bundle-ClassPath manifest entry of the fragment.
	 */
	public void attachFragment(org.eclipse.osgi.framework.adaptor.BundleData bundledata,ProtectionDomain domain,String[] classpath) {
		DefaultBundleData defaultBundledata = (DefaultBundleData) bundledata;
		try
		{
			bundledata.open(); /* make sure the BundleData is open */
		} catch (IOException e)
		{
			
			defaultBundledata.adaptor.getEventPublisher().publishFrameworkEvent(
					FrameworkEvent.ERROR,defaultBundledata.getBundle(),e);
		}
		BundleFile[] fragFiles = buildClasspath(classpath,defaultBundledata);
		FragmentClasspath fragClasspath = new FragmentClasspath(fragFiles,defaultBundledata,domain);
		insertFragment(fragClasspath);
	}

	/**
	 * Inserts a fragment classpath to into the list of fragments for this host.
	 * Fragments are inserted into the list according to the fragment's 
	 * Bundle ID.
	 * @param fragClasspath The FragmentClasspath to insert.
	 */
	protected synchronized void insertFragment(FragmentClasspath fragClasspath) {
		if (fragClasspaths == null) {
			// First fragment to attach.  Simply create the list and add the fragment.
			fragClasspaths = new Vector(10);
			fragClasspaths.addElement(fragClasspath);
			return;
		}

		// Find a place in the fragment list to insert this fragment.
		int size = fragClasspaths.size();
		long fragID = fragClasspath.bundledata.id;
		for (int i=0; i<size; i++) {
			long otherID = ((FragmentClasspath)fragClasspaths.elementAt(i)).bundledata.id;
			if (fragID < otherID) {
				fragClasspaths.insertElementAt(fragClasspath,i);
				return;
			}
		}
		// This fragment has the highest ID; put it at the end of the list.
		fragClasspaths.addElement(fragClasspath);
	}

	/**
	 * Gets a BundleFile object for the specified ClassPath entry.
	 * @param cp The ClassPath entry to get the BundleFile for.
	 * @return The BundleFile object for the ClassPath entry.
	 */
	protected BundleFile getClasspath(String cp, DefaultBundleData bundledata){
		BundleFile bundlefile = null;
		File file = bundledata.bundleFile.getFile(cp);
		if (file != null && file.exists()) {
			try
			{
				bundlefile = BundleFile.createBundleFile(file, bundledata);
			}
			catch (IOException e)
			{
				bundledata.adaptor.getEventPublisher().publishFrameworkEvent(
					FrameworkEvent.ERROR,bundledata.getBundle(),e);
			}
		}
		else {
			if (bundledata.bundleFile instanceof BundleFile.ZipBundleFile)
			{
				// the classpath entry may be a directory in the bundle jar file.
				Enumeration entries = bundledata.bundleFile.getEntryPaths(cp);
				if (entries.hasMoreElements())
				{
					bundlefile = BundleFile.createBundleFile(
							(BundleFile.ZipBundleFile)bundledata.bundleFile, cp);
				}
			}

			if (bundlefile == null)
			{
				BundleException be = new BundleException(Msg.formatter.getString("BUNDLE_CLASSPATH_ENTRY_NOT_FOUND_EXCEPTION", cp));
				bundledata.adaptor.getEventPublisher().publishFrameworkEvent(
						FrameworkEvent.ERROR,bundledata.getBundle(),be);
			}
			
		}
		return bundlefile;
	}
	
	protected synchronized Class findClass(String name) throws ClassNotFoundException {
		Class result = findLoadedClass(name);
		if (result != null)
			return result;
		for(int i=0; i<bundleFiles.length; i++) {
			if (bundleFiles[i] != null){
				result = findClassImpl(name,bundleFiles[i],domain);
				if (result != null) {
					return result;
				}
			}
		}
		// look in fragments.
		if (fragClasspaths != null){
			int size = fragClasspaths.size();
			for (int i=0; i<size; i++) {
				FragmentClasspath fragCP = (FragmentClasspath) fragClasspaths.elementAt(i);
				for (int j=0; j<fragCP.bundlefiles.length; j++) {
					result = findClassImpl(name,fragCP.bundlefiles[j],fragCP.domain);
					if (result != null) {
						return result;
					}
				}
			}
		}
		throw new ClassNotFoundException(name);
	}

	/**
	 * Finds a class in the BundleFile.  If a class is found then the class
	 * is defined using the ProtectionDomain bundledomain.
	 * @param name The name of the class to find.
	 * @param bundleFile The BundleFile to find the class in.
	 * @param bundledomain The ProtectionDomain to use to defind the class if
	 * it is found.
	 * @return The loaded class object or null if the class is not found.
	 */
	protected Class findClassImpl(String name,BundleFile bundleFile,ProtectionDomain bundledomain){
		if (Debug.DEBUG && Debug.DEBUG_LOADER)
		{
			Debug.println("BundleClassLoader["+hostdata+"].findClass("+name+")");
		}

		String filename = name.replace('.', '/').concat(".class");

		BundleEntry entry = bundleFile.getEntry(filename);

		if (entry == null)
		{
			return null;
		}

		InputStream in;
		try
		{
			in = entry.getInputStream();
		}
		catch (IOException e)
		{
			return null;
		}

		int length = (int)entry.getSize();
		byte[] classbytes;
		int bytesread = 0;
		int readcount;

		if (Debug.DEBUG && Debug.DEBUG_LOADER)
		{
			Debug.println("  about to read "+length+" bytes from "+filename);
		}

		try
		{
			try
			{
				if (length > 0)
				{
					classbytes = new byte[length];

					readloop:
					for (; bytesread < length; bytesread += readcount)
					{
						readcount = in.read(classbytes, bytesread, length-bytesread);

						if (readcount <= 0) /* if we didn't read anything */
						{
							break readloop;         /* leave the loop */
						}
					}
				}
				else    /* BundleEntry does not know its own length! */
				{
					length = buffersize;
					classbytes = new byte[length];

					readloop:
					while (true)
					{
						for (; bytesread < length; bytesread += readcount)
						{
							readcount = in.read(classbytes, bytesread, length-bytesread);

							if (readcount <= 0) /* if we didn't read anything */
							{
								break readloop;         /* leave the loop */
							}
						}

						byte[] oldbytes = classbytes;
						length += buffersize;
						classbytes = new byte[length];
						System.arraycopy(oldbytes, 0, classbytes, 0, bytesread);
					}
				}
			}
			catch (IOException e)
			{
				if (Debug.DEBUG && Debug.DEBUG_LOADER)
				{
					Debug.println("  IOException reading "+filename+" from "+hostdata);
				}

				return null;
			}
		}
		finally
		{
			try
			{
				in.close();
			}
			catch (IOException ee)
			{
			}
		}

		if (Debug.DEBUG && Debug.DEBUG_LOADER)
		{
			Debug.println("  read "+bytesread+" bytes from "+filename);
			Debug.println("  defining class "+name);
		}

		try
		{
			return(defineClass(name, classbytes, 0, bytesread, bundledomain));
		}
		catch (Error e)
		{
			if (Debug.DEBUG && Debug.DEBUG_LOADER)
			{
				Debug.println("  error defining class "+name);
			}

			throw e;
		}
	}

	/** 
	 * @see org.eclipse.osgi.framework.adaptor.BundleClassLoader#findResource(String)
	 */
	protected URL findResource(String name) {
		URL result = null;
		for(int i=0; i<bundleFiles.length; i++) {
			if (bundleFiles[i] != null){
				result = findResourceImpl(name,bundleFiles[i],i);
				if (result != null) {
					return result;
				}
			}
		}
		// look in fragments
		if (fragClasspaths != null){
			int size = fragClasspaths.size();
			for (int i=0; i<size; i++) {
				FragmentClasspath fragCP = (FragmentClasspath) fragClasspaths.elementAt(i);
				for (int j=0; j<fragCP.bundlefiles.length; j++) {
					result = findResourceImpl(name,fragCP.bundlefiles[j],j);
					if (result != null) {
						return result;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Looks in the specified BundleFile for the resource.
	 * @param name The name of the resource to find,.
	 * @param bundlefile The BundleFile to look in.
	 * @param cpEntry The ClassPath entry index of the BundleFile.
	 * @return A URL to the resource or null if the resource does not exist.
	 */
	protected URL findResourceImpl(String name,BundleFile bundlefile,int cpEntry) {
		return bundlefile.getURL(name,cpEntry);
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.BundleClassLoader#findLocalResources(String)
	 */
	public Enumeration findLocalResources(String resource) {
		Vector resources = new Vector(6);
		for(int i=0; i<bundleFiles.length; i++) {
			if (bundleFiles[i] != null){
				URL url = findResourceImpl(resource,bundleFiles[i],i);
				if (url != null) {
					resources.addElement(url);
				}
			}
		}
		// look in fragments
		if (fragClasspaths != null){
			int size = fragClasspaths.size();
			for (int i=0; i<size; i++) {
				FragmentClasspath fragCP = (FragmentClasspath) fragClasspaths.elementAt(i);
				for (int j=0; j<fragCP.bundlefiles.length; j++) {
					URL url = findResourceImpl(resource,fragCP.bundlefiles[j],j);
					if (url != null) {
						resources.addElement(url);
					}
				}
			}
		}
		if (resources.size()>0){
			return resources.elements();
		}
		return null;
	}

	/**
	 * Closes all the BundleFile objects for this BundleClassLoader.
	 */
	public void close(){
		if (!closed) {
			super.close();
			for (int i=0; i<bundleFiles.length; i++) {
				if (bundleFiles[i] != null) {
					try
					{
						if (bundleFiles[i]!=hostdata.bundleFile) {
							bundleFiles[i].close();
						}
					}
					catch (IOException e)
					{
						hostdata.adaptor.getEventPublisher().publishFrameworkEvent(
								FrameworkEvent.ERROR,hostdata.getBundle(),e);
					}
				}
			}
			if (fragClasspaths != null) {
				int size = fragClasspaths.size();
				for (int i=0; i<size; i++) {
					FragmentClasspath fragCP = (FragmentClasspath) fragClasspaths.elementAt(i);
					fragCP.close();
				}
			}
		}
	}

	protected BundleFile[] buildClasspath(String[] classpath, DefaultBundleData bundledata) {
		ArrayList result = new ArrayList(10);

		// If not in dev mode then just add the regular classpath entries and return
		if (System.getProperty("osgi.dev") == null) {
			for (int i = 0; i < classpath.length; i++) 
				addClassPathEntry(result, classpath[i],bundledata);
			return (BundleFile[])result.toArray(new BundleFile[result.size()]);
		}
		
		// Otherwise, add the legacy entries for backwards compatibility and
		// then for each classpath entry add the dev entries as spec'd in the 
		// corresponding properties file.  If none are spec'd, add the 
		// classpath entry itself
		addDefaultDevEntries(result, bundledata);
		for (int i = 0; i < classpath.length; i++) {
			String[] devEntries = getDevEntries(classpath[i], bundledata);
			if (devEntries != null && devEntries.length > 0) {
				for (int j = 0; j < devEntries.length; j++) 
					addClassPathEntry(result, devEntries[j], bundledata);
			} else 
				addClassPathEntry(result, classpath[i], bundledata);
		}
		return (BundleFile[])result.toArray(new BundleFile[result.size()]);
	}
	
	private void addDefaultDevEntries(ArrayList result, DefaultBundleData bundledata) {
		if (System.getProperty("osgi.dev") == null)
			return;
		String[] defaultDevEntries = bundledata.adaptor.devCP;
		if (defaultDevEntries != null) 
			for (int i = 0; i < defaultDevEntries.length; i++) 
				addClassPathEntry(result, defaultDevEntries[i], bundledata);
	}

	private void addClassPathEntry(ArrayList result, String entry, DefaultBundleData bundledata) {
		if (entry.equals(".")) 
			result.add(bundledata.bundleFile);
		else {
			Object element = getClasspath(entry, bundledata);
			if (element != null)
				result.add(element);
		}
	}
	
	private String[] getDevEntries(String classpathEntry, DefaultBundleData bundledata) {
		Properties devProps = null;
		File propLocation = bundledata.bundleFile.getFile(classpathEntry + ".properties");
		if (propLocation == null) 
			return null;
		try {
			InputStream in = new FileInputStream(propLocation);
			try {
				devProps = new Properties();
				devProps.load(in);
				return getArrayFromList(devProps.getProperty("bin"));
			} finally {
				in.close();
			}
		} catch (IOException e) {
			// TODO log the failures but ignore and try to keep going
		}
		return null;
	}

	/**
	 * Returns the result of converting a list of comma-separated tokens into an array
	 * 
	 * @return the array of string tokens
	 * @param prop the initial comma-separated string
	 */
	private String[] getArrayFromList(String prop) {
		if (prop == null || prop.trim().equals("")) //$NON-NLS-1$
			return new String[0];
		Vector list = new Vector();
		StringTokenizer tokens = new StringTokenizer(prop, ","); //$NON-NLS-1$
		while (tokens.hasMoreTokens()) {
			String token = tokens.nextToken().trim();
			if (!token.equals("")) //$NON-NLS-1$
				list.addElement(token);
		}
		return list.isEmpty() ? new String[0] : (String[]) list.toArray(new String[list.size()]);
	}

	/**
	 * A data structure to hold information about a fragment classpath.
	 */
	protected class FragmentClasspath {
		/** The BundleFiles of the fragments Bundle-Classpath */
		protected BundleFile[] bundlefiles;
		/** The BundleData of the fragment */
		protected DefaultBundleData bundledata;
		/** The ProtectionDomain of the fragment */
		protected ProtectionDomain domain;

		protected FragmentClasspath(BundleFile[] bundlefiles, DefaultBundleData bundledata, ProtectionDomain domain){
			this.bundlefiles = bundlefiles;
			this.bundledata = bundledata;
			this.domain = domain;
		}
		
		protected void close(){
			for (int i=0; i<bundlefiles.length; i++) {
				try {
					if (bundlefiles[i] != bundledata.bundleFile) {
						bundlefiles[i].close();
					}
				}
				catch (IOException e)
				{
					bundledata.adaptor.getEventPublisher().publishFrameworkEvent(
						FrameworkEvent.ERROR,bundledata.getBundle(),e);
				}
			}
		}
	}
}
