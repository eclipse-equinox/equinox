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
import java.net.URLConnection;
import java.util.*;
import org.eclipse.osgi.framework.adaptor.*;
import org.eclipse.osgi.framework.adaptor.core.AbstractFrameworkAdaptor;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.util.Headers;
import org.eclipse.osgi.internal.resolver.StateManager;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.*;

/**
 *  //TODO Add comment here
 */
public class DefaultAdaptor extends AbstractFrameworkAdaptor
{

	protected static final String METADATA_ADAPTOR_NEXTID = "METADATA_ADAPTOR_NEXTID";
	protected static final String METADATA_ADAPTOR_IBSL = "METADATA_ADAPTOR_IBSL";
	
	/** directory containing installed bundles */
	protected File bundleRootDir;

	/** String containing bundle dir */
	protected String bundledir = null;

	/** name of a bundles data directory */
	protected String dataDirName = "data";

	protected boolean reset = false;

	/** The MetaData for the default adaptor */
	protected MetaData metadata;

	/** Dictionary containing permission data */
	protected PermissionStorage permissionStore;

	/** next available bundle id */
	protected long nextId = 1;

	/** Development ClassPath entries */
	protected String[] devCP;
	
	/** Name of the Adaptor manifest file */
	protected final String ADAPTOR_MANIFEST = "/META-INF/ADAPTOR.MF";
	
	/** This adaptor's manifest file */
	protected Headers manifest=null;
		
	protected AdaptorElementFactory elementFactory = null;

	/** The State Manager */
	protected StateManager stateManager;
	
	/**
	 * Constructor for DefaultAdaptor.  This constructor parses the arguments passed
	 * and remembers them for later when initialize is called.
	 * <p>No blank spaces should be used in the arguments to the DefaultAdaptor.
	 * The options that DefaultAdaptor recognizes and handles are:
	 * <ul>
	 * <li><b>bundledir=<i>directory name</i></b>If a directory name is specified, the adaptor should initialize
	 * to store bundles in that directory.  This arg should be enclosed in "" if it contains the ":"
	 * character (example "bundledir=c:\mydir").
	 * <li><b>reset</b>Resets the bundle storage by deleting the bundledir
	 * </ul>
	 * Any other arguments are ignored.
	 *
	 * @param An array of strings containing arguments.
	 * This object cannot be used until initialize is called.
	 *
	 */
	public DefaultAdaptor(String[] args)
	{
		if (args != null)
		{
			for (int i = 0; i < args.length; i++)
			{
				String arg = args[i];
				if (arg.equalsIgnoreCase("reset"))
				{
					reset = true;
				}
				else if (arg.indexOf("=") != -1)
				{
					StringTokenizer tok = new StringTokenizer(args[i], "=");
					if (tok.countTokens() == 2)
					{
						String key = tok.nextToken();
						if (key.equalsIgnoreCase("bundledir"))
						{
							// save file name for initializeStorage to use
							bundledir = tok.nextToken();
						}
					}
				}
			}
		}
	}

	public void initialize(EventPublisher eventPublisher)
	{
		super.initialize(eventPublisher);
		getBundleDir();
		readAdaptorManifest();
		stateManager = createStateManager();
	}

	/**
	 * Creates the StateManager for the adaptor
	 * @return the StateManager.
	 */
	protected StateManager createStateManager(){
		stateManager = new StateManager(bundleRootDir);
		State systemState = stateManager.getSystemState();
		if (systemState != null)
			return stateManager;
		systemState = stateManager.createSystemState();				
		Vector installedBundles = getInstalledBundles();
		if (installedBundles == null)
			return stateManager;
		StateObjectFactory factory = stateManager.getFactory();
		for (Iterator iter = installedBundles.iterator(); iter.hasNext(); ) {
			BundleData toAdd = (BundleData) iter.next();
			try {
				Dictionary manifest = toAdd.getManifest();
				BundleDescription newDescription = factory.createBundleDescription(manifest, toAdd.getLocation(),toAdd.getBundleID());
				systemState.addBundle(newDescription);
			} catch (BundleException be) {
				// just ignore bundle datas with invalid manifests
			}
		}
		// we need the state resolved
		systemState.resolve();
		return stateManager;
	}

	/**
	 * Get the directory to store the bundles in.  Bundledir can be set in 3 different ways.
	 * Priority is:
	 * 1 - OSGI Launcher command line -adaptor argument
	 * 2 - System property org.eclipse.osgi.framework.defaultadaptor.bundledir - could be specified with -D when launching
	 * 3 - osgi.properties - org.eclipse.osgi.framework.defaultadaptor.bundledir property
	 *
	 * Bundledir will be stored back to adaptor properties which
	 * the framework will copy into the System properties.
	 */
	protected void getBundleDir()
	{
		/* if bundledir was not set by the constructor from the -adaptor cmd line arg */
		if (bundledir == null)
		{
			/* check the system properties */
			bundledir = System.getProperty("org.eclipse.osgi.framework.defaultadaptor.bundledir");

			if (bundledir == null)
			{
				/* check the osgi.properties file, but default to "bundles" */
				bundledir = properties.getProperty("org.eclipse.osgi.framework.defaultadaptor.bundledir", "bundles");
			}
		}

		/* store bundledir back into adaptor properties for others to see */
		properties.put("org.eclipse.osgi.framework.defaultadaptor.bundledir", bundledir);

		bundleRootDir = new File(bundledir);
	}

	/**
	 * Reads and initializes the adaptor BundleManifest object.  The
	 * BundleManifest is used by the getExportPackages() and getExportServices()
	 * methods of the adpator.
	 */
	protected void readAdaptorManifest() {	
		InputStream in = getClass().getResourceAsStream(ADAPTOR_MANIFEST);
		if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
			if (in == null) {
				Debug.println("Unable to find adaptor bundle manifest " + ADAPTOR_MANIFEST);
			}
		}
		try {
			manifest = Headers.parseManifest(in);		
		} catch (BundleException e) {
			Debug.println("Unable to read adaptor bundle manifest " + ADAPTOR_MANIFEST);
		}
	}

	/**
	 * Initialize the persistent storage.
	 *
	 * <p>This method initializes the bundle persistent storage
	 * area.
	 * If a dir was specified in the -adaptor command line option, then it
	 * is used.  If not,
	 * if the property
	 * <i>org.eclipse.osgi.framework.defaultadaptor.bundledir</i> is specifed, its value
	 * will be used as the name of the bundle directory
	 * instead of <tt>./bundles</tt>.
	 * If reset was specified on the -adaptor command line option,
	 * then the storage will be cleared.
	 *
	 * @throws IOException If an error occurs initializing the storage.
	 */
	public void initializeStorage() throws IOException
	{
		boolean makedir = false;
		if (bundleRootDir.exists())
		{
			if (reset)
			{
				makedir = true;
				if (!rm(bundleRootDir))
				{
					if (Debug.DEBUG && Debug.DEBUG_GENERAL)
					{
						Debug.println("Could not remove directory: " + bundleRootDir.getPath());
					}
				}
			}
			else
			{
				if (!bundleRootDir.isDirectory())
				{
					if (Debug.DEBUG && Debug.DEBUG_GENERAL)
					{
						Debug.println("Exists but not a directory: " + bundleRootDir.getPath());
					}

					throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"));
				}
			}
		}
		else
		{
			makedir = true;
		}
		if (makedir)
		{
			if (!bundleRootDir.mkdirs())
			{
				if (Debug.DEBUG && Debug.DEBUG_GENERAL)
				{
					Debug.println("Unable to create directory: " + bundleRootDir.getPath());
				}

				throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"));
			}
		}

		metadata = new MetaData(getMetaDataFile(), "Framework metadata");
		metadata.load();
		nextId = metadata.getLong(METADATA_ADAPTOR_NEXTID, 1);
		initialBundleStartLevel = metadata.getInt(METADATA_ADAPTOR_IBSL,1);
	}

	protected File getMetaDataFile(){
		return new File(bundleRootDir, ".framework");
	}

	/**
	 * This method cleans up storage in the specified directory and
	 * any subdirectories.
	 *
	 * @param directory The directory to clean.
	 * @param depth The remaining depth. When depth is zero, this
	 * method will not recurse any deeper
	 * @see #compactStorage
	 */
	private void compact(File directory)
	{
		if (Debug.DEBUG && Debug.DEBUG_GENERAL)
		{
			Debug.println("compact(" + directory.getPath() + ")");
		}

		String list[] = directory.list();

		if (list != null)
		{
			int len = list.length;

			for (int i = 0; i < len; i++)
			{
				if (dataDirName.equals(list[i]))
				{
					continue; /* do not examine the bundles data dir. */
				}

				File target = new File(directory, list[i]);

				/* if the file is a directory */
				if (target.isDirectory())
				{
					File delete = new File(target, ".delete");

					/* and the directory is marked for delete */
					if (delete.exists())
					{
						/* if rm fails to delete the directory *
						 * and .delete was removed
						 */
						if (!rm(target) && !delete.exists())
						{
							try
							{
								/* recreate .delete */
								FileOutputStream out = new FileOutputStream(delete);
								out.close();
							}
							catch (IOException e)
							{
								if (Debug.DEBUG && Debug.DEBUG_GENERAL)
								{
									Debug.println("Unable to write " + delete.getPath() + ": " + e.getMessage());
								}
							}
						}
					}
					else
					{
						compact(target); /* descend into directory */
					}
				}
			}
		}
	}

	/**
	 * Clean up the persistent storage.
	 *
	 * <p>Cleans up any deferred deletions in persistent storage.
	 *
	 */
	public void compactStorage()
	{
		compact(bundleRootDir);
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getInstalledBundles()
	 */
	public Vector getInstalledBundles()
	{
		String list[] = bundleRootDir.list();

		if (list == null) {
			return null;
		}
		int len = list.length;

		Vector bundleDatas = new Vector(len << 1, 10);

		/* create bundle objects for all installed bundles. */
		for (int i = 0; i < len; i++)
		{
			try
			{
				DefaultBundleData data;

				try
				{
					data = (DefaultBundleData)getElementFactory().getBundleData(this);
					data.initializeExistingBundle(list[i]);
				}
				catch (NumberFormatException e)
				{
					continue; /* the directory is not a bundle id */
				}

				if (Debug.DEBUG && Debug.DEBUG_GENERAL)
				{
					Debug.println("BundleData created: " + data);
				}

				bundleDatas.addElement(data);
			}
			catch (IOException e)
			{
				if (Debug.DEBUG && Debug.DEBUG_GENERAL)
				{
					Debug.println("Unable to open Bundle[" + list[i] + "]: " + e.getMessage());
					Debug.printStackTrace(e);
				}
			}
		}

		return (bundleDatas);
	}

	/**
	 * Prepare to install a bundle from a URLConnection.
	 * <p>To complete the install,
	 * begin and then commit
	 * must be called on the returned <code>BundleOperation</code> object.
	 * If either of these methods throw a BundleException
	 * or some other error occurs,
	 * then undo must be called on the <code>BundleOperation</code> object
	 * to undo the change to persistent storage.
	 *
	 * @param location Bundle location.
	 * @param source URLConnection from which the bundle may be read.
	 * Any InputStreams returned from the source
	 * (URLConnections.getInputStream) must be closed by the
	 * <code>BundleOperation</code> object.
	 * @return BundleOperation object to be used to complete the install.
	 */
	public BundleOperation installBundle(final String location, final URLConnection source)
	{
		return (new BundleOperation()
		{
			private DefaultBundleData data;

			/**
			 * Begin the operation on the bundle (install, update, uninstall).
			 *
			 * @return BundleData object for the target bundle.
			 * @throws BundleException If a failure occured modifiying peristent storage.
			 */
			public org.eclipse.osgi.framework.adaptor.BundleData begin() throws BundleException
			{
				long id;

				try
				{
					/*
					 * Open InputStream first to trigger prereq installs, if any,
					 * before allocating bundle id.
					 */
					InputStream in = source.getInputStream();

					try
					{
						try
						{
							id = getNextBundleId();
							metadata.save();
						}
						catch (IOException e)
						{
							throw new BundleException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"), e);
						}

						if (in instanceof ReferenceInputStream)
						{
							URL reference = ((ReferenceInputStream) in).getReference();

							if (!"file".equals(reference.getProtocol()))
							{
								throw new BundleException(
									AdaptorMsg.formatter.getString("ADAPTOR_URL_CREATE_EXCEPTION", reference));
							}

							data = (DefaultBundleData)getElementFactory().getBundleData(DefaultAdaptor.this);
							data.initializeReferencedBundle(id, location, reference.getPath());
						}
						else
						{
							data = (DefaultBundleData)getElementFactory().getBundleData(DefaultAdaptor.this);
							data.initializeNewBundle(id, location, in);
						}
					}
					finally
					{
						try
						{
							in.close();
						}
						catch (IOException e)
						{
						}
					}
				}
				catch (IOException ioe)
				{
					throw new BundleException(AdaptorMsg.formatter.getString("BUNDLE_READ_EXCEPTION"), ioe);
				}

				return (data);
			}

			public void undo()
			{
				if (data != null)
				{
					try
					{
						data.close();
					}
					catch (IOException e)
					{
						if (Debug.DEBUG && Debug.DEBUG_GENERAL)
						{
							Debug.println("Unable to close " + data + ": " + e.getMessage());
						}
					}
				}

				if (data != null)
				{
					File bundleDir = data.getBundleDir();

					if (!rm(bundleDir))
					{
						/* mark this bundle to be deleted to ensure it is fully cleaned up
						 * on next restart.
						 */
						File delete = new File(bundleDir, ".delete");

						if (!delete.exists())
						{
							try
							{
								/* create .delete */
								FileOutputStream out = new FileOutputStream(delete);
								out.close();
							}
							catch (IOException e)
							{
								if (Debug.DEBUG && Debug.DEBUG_GENERAL)
								{
									Debug.println("Unable to write " + delete.getPath() + ": " + e.getMessage());
								}
							}
						}
					}
				}
			}

			public void commit(boolean postpone) throws BundleException
			{
				try
				{
					data.save();
				}
				catch (IOException e)
				{
					throw new BundleException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"), e);
				}
				BundleDescription bundleDescription = stateManager.getFactory().createBundleDescription(data.getManifest(), data.getLocation(),data.getBundleID());
				stateManager.getSystemState().addBundle(bundleDescription);
			}

		});
	}

	/**
	 * Prepare to update a bundle from a URLConnection.
	 * <p>To complete the update,
	 * modify and then commit
	 * will be called on the returned BundleStorage object.
	 * If either of these methods throw a BundleException
	 * or some other error occurs,
	 * then undo will be called on the BundleStorage object
	 * to undo the change to persistent storage.
	 *
	 * @param bundle Bundle to update.
	 * @param source URLConnection from which the bundle may be read.
	 * @return BundleOperation object to be used to complete the update.
	 */

	public BundleOperation updateBundle(
		final org.eclipse.osgi.framework.adaptor.BundleData bundledata,
		final URLConnection source)
	{
		return (new BundleOperation()
		{
			private DefaultBundleData data;
			private DefaultBundleData newData;

			/**
			 * Perform the change to persistent storage.
			 *
			 * @return Bundle object for the target bundle.
			 */
			public org.eclipse.osgi.framework.adaptor.BundleData begin() throws BundleException
			{
				this.data = (DefaultBundleData) bundledata;
				try
				{
					InputStream in = source.getInputStream();
					try
					{
						if (in instanceof ReferenceInputStream)
						{
							URL reference = ((ReferenceInputStream) in).getReference();

							if (!"file".equals(reference.getProtocol()))
							{
								throw new BundleException(
									AdaptorMsg.formatter.getString("ADAPTOR_URL_CREATE_EXCEPTION", reference));
							}

							// check to make sure we are not just trying to update to the same
							// directory reference.  This would be a no-op.
							String path = reference.getPath();
							if (path.equals(data.getName())){
								throw new BundleException(
									AdaptorMsg.formatter.getString("ADAPTOR_SAME_REF_UPDATE",reference));
							}

							try
							{
								newData = data.nextGeneration(reference.getPath());
							}
							catch (IOException e)
							{
								throw new BundleException(
									AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"),
									e);
							}

							File bundleGenerationDir = newData.getGenerationDir();

							if (!bundleGenerationDir.exists())
							{
								throw new BundleException(
									AdaptorMsg.formatter.getString(
										"ADAPTOR_DIRECTORY_CREATE_EXCEPTION",
										bundleGenerationDir.getPath()));
							}

							newData.bundleFile = BundleFile.createBundleFile(newData.file, newData);
						}
						else
						{
							try
							{
								newData = data.nextGeneration();
							}
							catch (IOException e)
							{
								throw new BundleException(
									AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"),
									e);
							}

							File bundleGenerationDir = newData.getGenerationDir();

							if (!bundleGenerationDir.exists())
							{
								throw new BundleException(
									AdaptorMsg.formatter.getString(
										"ADAPTOR_DIRECTORY_CREATE_EXCEPTION",
										bundleGenerationDir.getPath()));
							}

							File file = newData.getBundleFile();

							readFile(in, file);
							newData.bundleFile = BundleFile.createBundleFile(file, newData);
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
					newData.loadFromManifest();
				}
				catch (IOException e)
				{
					throw new BundleException(AdaptorMsg.formatter.getString("BUNDLE_READ_EXCEPTION"), e);
				}

				return (newData);
			}
			/**
			 * Commit the change to persistent storage.
			 *
			 * @param postpone If true, the bundle's persistent
			 * storage cannot be immediately reclaimed.
			 * @throws BundleException If a failure occured modifiying peristent storage.
			 */

			public void commit(boolean postpone) throws BundleException
			{
				try
				{
					newData.save();
				}
				catch (IOException e)
				{
					throw new BundleException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"), e);
				}
				long bundleId = newData.getBundleID();
				State systemState = stateManager.getSystemState();
				systemState.removeBundle(bundleId);
				BundleDescription newDescription = stateManager.getFactory().createBundleDescription(newData.getManifest(), newData.getLocation(),bundleId);
				systemState.addBundle(newDescription);

				File originalGenerationDir = data.getGenerationDir();

				if (postpone || !rm(originalGenerationDir))
				{
					/* mark this bundle to be deleted to ensure it is fully cleaned up
					 * on next restart.
					 */

					File delete = new File(originalGenerationDir, ".delete");

					if (!delete.exists())
					{
						try
						{
							/* create .delete */
							FileOutputStream out = new FileOutputStream(delete);
							out.close();
						}
						catch (IOException e)
						{
							if (Debug.DEBUG && Debug.DEBUG_GENERAL)
							{
								Debug.println("Unable to write " + delete.getPath() + ": " + e.getMessage());
							}

							eventPublisher.publishFrameworkEvent(FrameworkEvent.ERROR, data.getBundle(), e);
						}
					}
				}
			}

			/**
			 * Undo the change to persistent storage.
			 *
			 * @throws BundleException If a failure occured modifiying peristent storage.
			 */
			public void undo() throws BundleException
			{
				/*if (bundleFile != null)
				{
					bundleFile.close();
				} */

				if (newData != null)
				{
					File nextGenerationDir = newData.getGenerationDir();

					if (!rm(nextGenerationDir)) /* delete downloaded bundle */
					{
						/* mark this bundle to be deleted to ensure it is fully cleaned up
						 * on next restart.
						 */

						File delete = new File(nextGenerationDir, ".delete");

						if (!delete.exists())
						{
							try
							{
								/* create .delete */
								FileOutputStream out = new FileOutputStream(delete);
								out.close();
							}
							catch (IOException e)
							{
								if (Debug.DEBUG && Debug.DEBUG_GENERAL)
								{
									Debug.println("Unable to write " + delete.getPath() + ": " + e.getMessage());
								}
							}
						}
					}
				}
			}
		});
	}

	/**
	 * Prepare to uninstall a bundle.
	 * <p>To complete the uninstall,
	 * modify and then commit
	 * will be called on the returned BundleStorage object.
	 * If either of these methods throw a BundleException
	 * or some other error occurs,
	 * then undo will be called on the BundleStorage object
	 * to undo the change to persistent storage.
	 *
	 * @param bundle BundleData to uninstall.
	 * @return BundleOperation object to be used to complete the uninstall.
	 */
	public BundleOperation uninstallBundle(final org.eclipse.osgi.framework.adaptor.BundleData bundledata)
	{
		return (new BundleOperation()
		{
			private DefaultBundleData data;

			/**
			 * Perform the change to persistent storage.
			 *
			 * @return Bundle object for the target bundle.
			 * @throws BundleException If a failure occured modifiying peristent storage.
			 */
			public org.eclipse.osgi.framework.adaptor.BundleData begin() throws BundleException
			{
				this.data = (DefaultBundleData) bundledata;
				return (bundledata);
			}
			/**
			 * Commit the change to persistent storage.
			 *
			 * @param postpone If true, the bundle's persistent
			 * storage cannot be immediately reclaimed.
			 * @throws BundleException If a failure occured modifiying peristent storage.
			 */
			public void commit(boolean postpone) throws BundleException
			{
				File bundleDir = data.getBundleDir();

				if (postpone || !rm(bundleDir))
				{
					/* mark this bundle to be deleted to ensure it is fully cleaned up
					 * on next restart.
					 */

					File delete = new File(bundleDir, ".delete");

					if (!delete.exists())
					{
						try
						{
							/* create .delete */
							FileOutputStream out = new FileOutputStream(delete);
							out.close();
						}
						catch (IOException e)
						{
							if (Debug.DEBUG && Debug.DEBUG_GENERAL)
							{
								Debug.println("Unable to write " + delete.getPath() + ": " + e.getMessage());
							}
						}
					}
				}

				stateManager.getSystemState().removeBundle(data.getBundleID());
			}
			/**
			 * Undo the change to persistent storage.
			 *
			 * @throws BundleException If a failure occured modifiying peristent storage.
			 */
			public void undo() throws BundleException
			{
			}
		});
	}

	/**
	 * Returns the PermissionStorage object which will be used to
	 * to manage the permission data.
	 *
	 * <p>The PermissionStorage object will store permission data
	 * in the "permdata" subdirectory of the bundle storage directory
	 * assigned by <tt>initializeStorage</tt>.
	 *
	 * @return The PermissionStorage object for the DefaultAdaptor.
	 */
	public org.eclipse.osgi.framework.adaptor.PermissionStorage getPermissionStorage() throws IOException
	{
		if (permissionStore == null)
		{
			synchronized (this)
			{
				if (permissionStore == null)
				{
					permissionStore = new PermissionStorage(this);
				}
			}
		}

		return permissionStore;
	}

	public void frameworkStart(BundleContext context) throws BundleException{
		super.frameworkStart(context);
				
		// Check the osgi.dev property to see if dev classpath entries have been defined.
		String osgiDev = context.getProperty("osgi.dev");
		if (osgiDev != null) {
			// Add each dev classpath entry
			Vector devClassPath = new Vector(6);
			StringTokenizer st = new StringTokenizer(osgiDev,",");
			while (st.hasMoreTokens()) {
				String tok = st.nextToken();
				if (!tok.equals("")) {
					devClassPath.addElement(tok);
				}
			}
			devCP = new String[devClassPath.size()];
			devClassPath.toArray(devCP);
		}

		State state = stateManager.getSystemState();
		BundleDescription systemBundle = state.getBundle(0);		
		if (systemBundle == null || !systemBundle.isResolved())
			// this would be a bug in the framework
			throw new IllegalStateException();
	}

	public void frameworkStop(BundleContext context) throws BundleException {
		try {
			stateManager.shutdown();
		} catch (IOException e) {
			throw new BundleException(null, e);  
		}
		super.frameworkStop(context);
	}
	/**
	 * Register a service object.
	 *
	 */
	protected ServiceRegistration register(String name, Object service, Bundle bundle)
	{
		Hashtable properties = new Hashtable(7);

		Dictionary headers = bundle.getHeaders();

		properties.put(Constants.SERVICE_VENDOR, headers.get(Constants.BUNDLE_VENDOR));

		properties.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));

		properties.put(Constants.SERVICE_PID, bundle.getBundleId() + "." + service.getClass().getName());

		return context.registerService(name, service, properties);
	}

	/**
	 * This function performs the equivalent of "rm -r" on a file or directory.
	 *
	 * @param   file file or directory to delete
	 * @return false is the specified files still exists, true otherwise.
	 */
	protected boolean rm(File file)
	{
		if (file.exists())
		{
			if (file.isDirectory())
			{
				String list[] = file.list();
				int len = list.length;
				for (int i = 0; i < len; i++)
				{
					// we are doing a lot of garbage collecting here
					rm(new File(file, list[i]));
				}

			}

			if (Debug.DEBUG && Debug.DEBUG_GENERAL)
			{
				if (file.isDirectory())
				{
					Debug.println("rmdir " + file.getPath());
				}
				else
				{
					Debug.println("rm " + file.getPath());
				}
			}

			boolean success = file.delete();

			if (Debug.DEBUG && Debug.DEBUG_GENERAL)
			{
				if (!success)
				{
					Debug.println("  rm failed!!");
				}
			}

			return (success);
		}
		else
		{
			return (true);
		}
	}

	public void setInitialBundleStartLevel(int value) {
		super.setInitialBundleStartLevel(value);
		metadata.setInt(METADATA_ADAPTOR_IBSL,value);
		try {
			metadata.save();
		} catch (IOException e) {
			eventPublisher.publishFrameworkEvent(FrameworkEvent.ERROR, context.getBundle(), e);
		}
	}

	/**
		* Map a location string into a bundle name.
		* This methods treats the location string as a URL.
		*
		* @param location bundle location string.
		* @return bundle name.
		*/
	public String mapLocationToName(String location)
	{
		int end = location.indexOf('?', 0); /* "?" query */

		if (end == -1)
		{
			end = location.indexOf('#', 0); /* "#" fragment */

			if (end == -1)
			{
				end = location.length();
			}
		}

		int begin = location.replace('\\', '/').lastIndexOf('/', end);
		int colon = location.lastIndexOf(':', end);

		if (colon > begin)
		{
			begin = colon;
		}

		return (location.substring(begin + 1, end));
	}

	/**
	 * Return the next valid, unused bundle id.
	 *
	 * @return Next valid, unused bundle id.
	 * @throws IOException If there are no more unused bundle ids.
	 */
	protected synchronized long getNextBundleId() throws IOException
	{
		while (nextId < Long.MAX_VALUE)
		{
			long id = nextId;

			nextId++;
			metadata.setLong(METADATA_ADAPTOR_NEXTID, nextId);
			
			File bundleDir = new File(bundleRootDir, String.valueOf(id));
			if (bundleDir.exists())
			{
				continue;
			}

			return (id);
		}

		throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"));
	}

	/**
	   * Read a file from an InputStream and write it to the file system.
	   *
	   * @param is InputStream from which to read.
	   * @param file output file to create.
	   * @exception IOException
	   */
	public static void readFile(InputStream in, File file) throws IOException
	{
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(file);

			byte buffer[] = new byte[1024];
			int count;
			while ((count = in.read(buffer, 0, buffer.length)) > 0)
			{
				fos.write(buffer, 0, count);
			}

			fos.close();
			fos = null;

			in.close();
			in = null;
		}
		catch (IOException e)
		{
			// close open streams
			if (fos != null)
			{
				try
				{
					fos.close();
				}
				catch (IOException ee)
				{
				}
			}

			if (in != null)
			{
				try
				{
					in.close();
				}
				catch (IOException ee)
				{
				}
			}

			if (Debug.DEBUG && Debug.DEBUG_GENERAL)
			{
				Debug.println("Unable to read file");
				Debug.printStackTrace(e);
			}

			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getExportPackages()
	 */
	public String getExportPackages() {
		if (manifest == null)
			return null;
		return (String)manifest.get(Constants.EXPORT_PACKAGE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getExportServices()
	 */
	public String getExportServices() {
		if (manifest == null)
			return null;
		return (String)manifest.get(Constants.EXPORT_SERVICE);
	}

	public AdaptorElementFactory getElementFactory() {
		if (elementFactory == null)
			elementFactory = new AdaptorElementFactory();
		return elementFactory;
	}
	
	public IBundleStats getBundleStats(){
		return null;
	}
	
	public State getState() {
		return stateManager.getSystemState();
	}
	public PlatformAdmin getPlatformAdmin() {
		return stateManager;
	}
}
