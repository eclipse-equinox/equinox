/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
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
import org.eclipse.osgi.framework.adaptor.core.*;
import org.eclipse.osgi.framework.debug.Debug;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.internal.resolver.StateManager;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.*;

/**
 *  //TODO Add comment here
 */
public class DefaultAdaptor extends AbstractFrameworkAdaptor {

	public static final String METADATA_ADAPTOR_NEXTID = "METADATA_ADAPTOR_NEXTID"; //$NON-NLS-1$
	public static final String METADATA_ADAPTOR_IBSL = "METADATA_ADAPTOR_IBSL"; //$NON-NLS-1$
	public static final String DATA_DIR_NAME = "data"; //$NON-NLS-1$
	public static final String BUNDLE_STORE = "osgi.bundlestore"; //$NON-NLS-1$

	protected AdaptorElementFactory elementFactory;

	/** directory containing installed bundles */
	protected File bundleStoreRootDir;

	/** directory containing data directories for installed bundles */
	protected File dataRootDir;

	/** String containing bundle store root dir */
	protected String bundleStore = null;

	protected boolean reset = false;

	/** The MetaData for the default adaptor */
	protected MetaData fwMetadata;

	/** Dictionary containing permission data */
	protected DefaultPermissionStorage permissionStore;

	/** next available bundle id */
	protected long nextId = 1;

	/** The State Manager */
	protected StateManager stateManager;

	/** The FrameworkLog for the adaptor */
	protected FrameworkLog frameworkLog;

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
	public DefaultAdaptor(String[] args) {
		if (args != null) {
			for (int i = 0; i < args.length; i++) {
				String arg = args[i];
				if (arg.equalsIgnoreCase("reset")) {
					reset = true;
				} else if (arg.indexOf("=") != -1) {
					StringTokenizer tok = new StringTokenizer(args[i], "=");
					if (tok.countTokens() == 2) {
						String key = tok.nextToken();
						if (key.equalsIgnoreCase("bundledir")) {
							// save file name for initializeStorage to use
							bundleStore = tok.nextToken();
						}
					}
				}
			}
		}
	}

	public void initialize(EventPublisher eventPublisher) {
		super.initialize(eventPublisher);
		initBundleStoreRootDir();

		// need to create the FrameworkLog very early
		frameworkLog = createFrameworkLog();
		stateManager = createStateManager();
	}

	/**
	 * Creates the StateManager for the adaptor
	 * @return the StateManager.
	 */
	protected StateManager createStateManager() {
		File stateLocation = new File(getBundleStoreRootDir(), ".state");
		stateManager = new StateManager(stateLocation);
		State systemState = stateManager.getSystemState();
		if (systemState != null)
			return stateManager;
		systemState = stateManager.createSystemState();
		Vector installedBundles = getInstalledBundles();
		if (installedBundles == null)
			return stateManager;
		StateObjectFactory factory = stateManager.getFactory();
		for (Iterator iter = installedBundles.iterator(); iter.hasNext();) {
			BundleData toAdd = (BundleData) iter.next();
			try {
				Dictionary manifest = toAdd.getManifest();
				BundleDescription newDescription = factory.createBundleDescription(manifest, toAdd.getLocation(), toAdd.getBundleID());
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
	 * 
	 *
	 */
	protected FrameworkLog createFrameworkLog() {
		return new DefaultLog();
	}

	/**
	 * Init the directory to store the bundles in.  Bundledir can be set in 3 different ways.
	 * Priority is:
	 * 1 - OSGI Launcher command line -adaptor argument
	 * 2 - System property org.eclipse.osgi.framework.defaultadaptor.bundledir - could be specified with -D when launching
	 * 3 - osgi.properties - org.eclipse.osgi.framework.defaultadaptor.bundledir property
	 *
	 * Bundledir will be stored back to adaptor properties which
	 * the framework will copy into the System properties.
	 */
	protected void initBundleStoreRootDir() {
		/* if bundleStore was not set by the constructor from the -adaptor cmd line arg */
		if (bundleStore == null) {
			/* check the system properties */
			bundleStore = System.getProperty(BUNDLE_STORE);

			if (bundleStore == null) {
				/* check the osgi.properties file, but default to "bundles" */
				bundleStore = properties.getProperty(BUNDLE_STORE, "bundles");
			}
		}

		bundleStoreRootDir = new File(bundleStore);
		/* store bundleStore back into adaptor properties for others to see */
		properties.put(BUNDLE_STORE, bundleStoreRootDir.getAbsolutePath());

	}

	protected void initDataRootDir() {
		dataRootDir = getBundleStoreRootDir();
	}

	public File getBundleStoreRootDir() {
		return bundleStoreRootDir;
	}

	public File getDataRootDir() {
		if (dataRootDir == null)
			initDataRootDir();
		return dataRootDir;
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
	public void initializeStorage() throws IOException {
		boolean makedir = false;
		File bundleStore = getBundleStoreRootDir();
		if (bundleStore.exists()) {
			if (reset) {
				makedir = true;
				if (!rm(bundleStore)) {
					if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
						Debug.println("Could not remove directory: " + bundleStore.getPath());
					}
				}
			} else {
				if (!bundleStore.isDirectory()) {
					if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
						Debug.println("Exists but not a directory: " + bundleStore.getPath());
					}

					throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"));
				}
			}
		} else {
			makedir = true;
		}
		if (makedir) {
			if (!bundleStore.mkdirs()) {
				if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
					Debug.println("Unable to create directory: " + bundleStore.getPath());
				}

				throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION")); //$NON-NLS-1$
			}
		}

		fwMetadata = new MetaData(getMetaDataFile(), "Framework metadata");
		fwMetadata.load();
		nextId = fwMetadata.getLong(METADATA_ADAPTOR_NEXTID, 1);
		initialBundleStartLevel = fwMetadata.getInt(METADATA_ADAPTOR_IBSL, 1);
	}

	protected File getMetaDataFile() {
		return new File(getBundleStoreRootDir(), ".framework");
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
	private void compact(File directory) {
		if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
			Debug.println("compact(" + directory.getPath() + ")");
		}

		String list[] = directory.list();

		if (list != null) {
			int len = list.length;

			for (int i = 0; i < len; i++) {
				if (DATA_DIR_NAME.equals(list[i])) {
					continue; /* do not examine the bundles data dir. */
				}

				File target = new File(directory, list[i]);

				/* if the file is a directory */
				if (target.isDirectory()) { //TODO Simplify the nesting.
					File delete = new File(target, ".delete");

					/* and the directory is marked for delete */
					if (delete.exists()) {
						/* if rm fails to delete the directory *
						 * and .delete was removed
						 */
						if (!rm(target) && !delete.exists()) {
							try {
								/* recreate .delete */
								FileOutputStream out = new FileOutputStream(delete);
								out.close();
							} catch (IOException e) {
								if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
									Debug.println("Unable to write " + delete.getPath() + ": " + e.getMessage());
								}
							}
						}
					} else {
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
	public void compactStorage() {
		compact(getBundleStoreRootDir());
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.FrameworkAdaptor#getInstalledBundles()
	 */
	public Vector getInstalledBundles() {
		String list[] = getBundleStoreRootDir().list();

		if (list == null) {
			return null;
		}
		int len = list.length;

		Vector bundleDatas = new Vector(len << 1, 10); //TODO ArrayList? array?

		/* create bundle objects for all installed bundles. */
		for (int i = 0; i < len; i++) {
			try {
				DefaultBundleData data;

				long id = -1;
				try {
					id = Long.parseLong(list[i]);
				} catch (NumberFormatException nfe) {
					continue;
				}
				data = (DefaultBundleData) getElementFactory().createBundleData(this, id);
				loadMetaDataFor(data);
				data.initializeExistingBundle();

				if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
					Debug.println("BundleData created: " + data);
				}

				bundleDatas.addElement(data);
			} catch (BundleException e) {
				if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
					Debug.println("Unable to open Bundle[" + list[i] + "]: " + e.getMessage());
					Debug.printStackTrace(e);
				}				
			} catch (IOException e) {
				if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
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
	public BundleOperation installBundle(final String location, final URLConnection source) {
		return (new BundleOperation() {
			private DefaultBundleData data;

			/**
			 * Begin the operation on the bundle (install, update, uninstall).
			 *
			 * @return BundleData object for the target bundle.
			 * @throws BundleException If a failure occured modifiying peristent storage.
			 */
			public org.eclipse.osgi.framework.adaptor.BundleData begin() throws BundleException {
				long id;

				try {
					/*
					 * Open InputStream first to trigger prereq installs, if any,
					 * before allocating bundle id.
					 */
					InputStream in = source.getInputStream();
					URL sourceURL = source.getURL();
					String protocol = sourceURL == null ? null : sourceURL.getProtocol();
					try {
						try {
							id = getNextBundleId();
							fwMetadata.save();
						} catch (IOException e) {
							throw new BundleException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"), e); //$NON-NLS-1$
						}
						data = (DefaultBundleData) getElementFactory().createBundleData(DefaultAdaptor.this, id);
						data.setLocation(location);
						data.setStartLevel(getInitialBundleStartLevel());

						if (in instanceof ReferenceInputStream) {
							URL reference = ((ReferenceInputStream) in).getReference();

							if (!"file".equals(reference.getProtocol())) {
								throw new BundleException(AdaptorMsg.formatter.getString("ADAPTOR_URL_CREATE_EXCEPTION", reference)); //$NON-NLS-1$
							}

							data.setReference(true);
							data.setFileName(reference.getPath());
							data.initializeNewBundle();
						} else {
							File genDir = data.createGenerationDir();
							if (!genDir.exists()) {
								throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_DIRECTORY_CREATE_EXCEPTION", genDir.getPath())); //$NON-NLS-1$
							}

							String fileName = mapLocationToName(location);
							File outFile = new File(genDir, fileName);
							if ("file".equals(protocol)) {
								File inFile = new File(source.getURL().getPath());
								if (inFile.isDirectory()) {
									copyDir(inFile, outFile);
								} else {
									readFile(in, outFile);
								}
							} else {
								readFile(in, outFile);
							}
							data.setReference(false);
							data.setFileName(fileName);
							data.initializeNewBundle();
						}
					} finally {
						try {
							in.close();
						} catch (IOException e) {
						}
					}
				} catch (IOException ioe) {
					throw new BundleException(AdaptorMsg.formatter.getString("BUNDLE_READ_EXCEPTION"), ioe);
				}

				return (data);
			}

			public void undo() {
				if (data != null) {
					try {
						data.close();
					} catch (IOException e) {
						if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
							Debug.println("Unable to close " + data + ": " + e.getMessage());
						}
					}
				}

				if (data != null) {
					File bundleDir = data.getBundleStoreDir();

					if (!rm(bundleDir)) {
						/* mark this bundle to be deleted to ensure it is fully cleaned up
						 * on next restart.
						 */
						File delete = new File(bundleDir, ".delete");

						if (!delete.exists()) {
							try {
								/* create .delete */
								FileOutputStream out = new FileOutputStream(delete);
								out.close();
							} catch (IOException e) {
								if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
									Debug.println("Unable to write " + delete.getPath() + ": " + e.getMessage());
								}
							}
						}
					}
				}
			}

			public void commit(boolean postpone) throws BundleException {
				try {
					data.save();
				} catch (IOException e) {
					throw new BundleException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"), e);
				}
				BundleDescription bundleDescription = stateManager.getFactory().createBundleDescription(data.getManifest(), data.getLocation(), data.getBundleID());
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

	public BundleOperation updateBundle(final org.eclipse.osgi.framework.adaptor.BundleData bundledata, final URLConnection source) {
		return (new BundleOperation() {
			private DefaultBundleData data;
			private DefaultBundleData newData;

			/**
			 * Perform the change to persistent storage.
			 *
			 * @return Bundle object for the target bundle.
			 */
			public org.eclipse.osgi.framework.adaptor.BundleData begin() throws BundleException {
				this.data = (DefaultBundleData) bundledata;
				try {
					InputStream in = source.getInputStream();
					URL sourceURL = source.getURL();
					String protocol = sourceURL == null ? null : sourceURL.getProtocol();
					try {
						if (in instanceof ReferenceInputStream) {
							ReferenceInputStream refIn = (ReferenceInputStream) in;
							URL reference = (refIn).getReference();
							if (!"file".equals(reference.getProtocol())) {
								throw new BundleException(AdaptorMsg.formatter.getString("ADAPTOR_URL_CREATE_EXCEPTION", reference)); //$NON-NLS-1$
							}
							// check to make sure we are not just trying to update to the same
							// directory reference.  This would be a no-op.
							//TODO Unless the jars and manifest have been updated on disk
							String path = reference.getPath();
							if (path.equals(data.getFileName())) {
								throw new BundleException(AdaptorMsg.formatter.getString("ADAPTOR_SAME_REF_UPDATE", reference)); //$NON-NLS-1$
							}
							try {
								newData = data.nextGeneration(reference.getPath());
							} catch (IOException e) {
								throw new BundleException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"), e); //$NON-NLS-1$
							}
							File bundleGenerationDir = newData.createGenerationDir();
							if (!bundleGenerationDir.exists()) {
								throw new BundleException(AdaptorMsg.formatter.getString("ADAPTOR_DIRECTORY_CREATE_EXCEPTION", bundleGenerationDir.getPath())); //$NON-NLS-1$
							}
							newData.createBaseBundleFile();
						} else {
							try {
								newData = data.nextGeneration(null);
							} catch (IOException e) {
								throw new BundleException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"), e); //$NON-NLS-1$
							}
							File bundleGenerationDir = newData.createGenerationDir();
							if (!bundleGenerationDir.exists()) {
								throw new BundleException(AdaptorMsg.formatter.getString("ADAPTOR_DIRECTORY_CREATE_EXCEPTION", bundleGenerationDir.getPath())); //$NON-NLS-1$
							}
							File outFile = newData.getBaseFile();
							if ("file".equals(protocol)) {
								File inFile = new File(source.getURL().getPath());
								if (inFile.isDirectory()) {
									copyDir(inFile, outFile);
								} else {
									readFile(in, outFile);
								}
							} else {
								readFile(in, outFile);
							}
							newData.createBaseBundleFile();
						}
					} finally {
						try {
							in.close();
						} catch (IOException ee) {
						}
					}
					newData.loadFromManifest();
				} catch (IOException e) {
					throw new BundleException(AdaptorMsg.formatter.getString("BUNDLE_READ_EXCEPTION"), e); //$NON-NLS-1$
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

			public void commit(boolean postpone) throws BundleException {
				try {
					newData.save();
				} catch (IOException e) {
					throw new BundleException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"), e); //$NON-NLS-1$
				}
				long bundleId = newData.getBundleID();
				State systemState = stateManager.getSystemState();
				systemState.removeBundle(bundleId);
				BundleDescription newDescription = stateManager.getFactory().createBundleDescription(newData.getManifest(), newData.getLocation(), bundleId);
				systemState.addBundle(newDescription);

				File originalGenerationDir = data.createGenerationDir();

				if (postpone || !rm(originalGenerationDir)) {
					/* mark this bundle to be deleted to ensure it is fully cleaned up
					 * on next restart.
					 */

					File delete = new File(originalGenerationDir, ".delete");

					if (!delete.exists()) {
						try {
							/* create .delete */
							FileOutputStream out = new FileOutputStream(delete);
							out.close();
						} catch (IOException e) {
							if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
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
			public void undo() throws BundleException {
				/*if (bundleFile != null)
				 {
				 bundleFile.close();
				 } */

				if (newData != null) {
					File nextGenerationDir = newData.createGenerationDir();

					if (!rm(nextGenerationDir)) /* delete downloaded bundle */{
						/* mark this bundle to be deleted to ensure it is fully cleaned up
						 * on next restart.
						 */

						File delete = new File(nextGenerationDir, ".delete");

						if (!delete.exists()) {
							try {
								/* create .delete */
								FileOutputStream out = new FileOutputStream(delete);
								out.close();
							} catch (IOException e) {
								if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
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
	public BundleOperation uninstallBundle(final org.eclipse.osgi.framework.adaptor.BundleData bundledata) {
		return (new BundleOperation() {
			private DefaultBundleData data;

			/**
			 * Perform the change to persistent storage.
			 *
			 * @return Bundle object for the target bundle.
			 * @throws BundleException If a failure occured modifiying peristent storage.
			 */
			public org.eclipse.osgi.framework.adaptor.BundleData begin() throws BundleException {
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
			public void commit(boolean postpone) throws BundleException {
				File bundleDir = data.getBundleStoreDir();

				if (postpone || !rm(bundleDir)) {
					/* mark this bundle to be deleted to ensure it is fully cleaned up
					 * on next restart.
					 */

					File delete = new File(bundleDir, ".delete");

					if (!delete.exists()) {
						try {
							/* create .delete */
							FileOutputStream out = new FileOutputStream(delete);
							out.close();
						} catch (IOException e) {
							if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
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
			public void undo() throws BundleException {
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
	public org.eclipse.osgi.framework.adaptor.PermissionStorage getPermissionStorage() throws IOException {
		if (permissionStore == null) {
			synchronized (this) {
				if (permissionStore == null) {
					permissionStore = new DefaultPermissionStorage(this);
				}
			}
		}

		return permissionStore;
	}

	public void frameworkStart(BundleContext context) throws BundleException {
		super.frameworkStart(context);

		if (frameworkLog == null) {
			frameworkLog = createFrameworkLog();
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

		frameworkLog.close();
		frameworkLog = null;
	}

	/**
	 * Register a service object.
	 *
	 */
	protected ServiceRegistration register(String name, Object service, Bundle bundle) {
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
	protected boolean rm(File file) {
		if (file.exists()) {
			if (file.isDirectory()) {
				String list[] = file.list();
				int len = list.length;
				for (int i = 0; i < len; i++) {
					// we are doing a lot of garbage collecting here
					rm(new File(file, list[i]));
				}

			}

			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				if (file.isDirectory()) {
					Debug.println("rmdir " + file.getPath());
				} else {
					Debug.println("rm " + file.getPath());
				}
			}

			boolean success = file.delete();

			if (Debug.DEBUG && Debug.DEBUG_GENERAL) {
				if (!success) {
					Debug.println("  rm failed!!");
				}
			}

			return (success);
		} else {
			return (true);
		}
	}

	public void setInitialBundleStartLevel(int value) {
		super.setInitialBundleStartLevel(value);
		fwMetadata.setInt(METADATA_ADAPTOR_IBSL, value);
		try {
			fwMetadata.save();
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
	public String mapLocationToName(String location) {
		int end = location.indexOf('?', 0); /* "?" query */

		if (end == -1) {
			end = location.indexOf('#', 0); /* "#" fragment */

			if (end == -1) {
				end = location.length();
			}
		}

		int begin = location.replace('\\', '/').lastIndexOf('/', end);
		int colon = location.lastIndexOf(':', end);

		if (colon > begin) {
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
	protected synchronized long getNextBundleId() throws IOException {
		while (nextId < Long.MAX_VALUE) {
			long id = nextId;

			nextId++;
			fwMetadata.setLong(METADATA_ADAPTOR_NEXTID, nextId);

			File bundleDir = new File(getBundleStoreRootDir(), String.valueOf(id));
			if (bundleDir.exists()) {
				continue;
			}

			return (id);
		}

		throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION")); //$NON-NLS-1$
	}

	public AdaptorElementFactory getElementFactory() {
		if (elementFactory == null)
			elementFactory = new DefaultElementFactory();
		return elementFactory;
	}

	public FrameworkLog getFrameworkLog() {
		return frameworkLog;
	}

	public State getState() {
		return stateManager.getSystemState();
	}

	public PlatformAdmin getPlatformAdmin() {
		return stateManager;
	}

	public static final String METADATA_BUNDLE_GEN = "METADATA_BUNDLE_GEN";
	public static final String METADATA_BUNDLE_LOC = "METADATA_BUNDLE_LOC";
	public static final String METADATA_BUNDLE_REF = "METADATA_BUNDLE_REF";
	public static final String METADATA_BUNDLE_NAME = "METADATA_BUNDLE_NAME";
	public static final String METADATA_BUNDLE_NCP = "METADATA_BUNDLE_NCP";
	public static final String METADATA_BUNDLE_ABSL = "METADATA_BUNDLE_ABSL";
	public static final String METADATA_BUNDLE_STATUS = "METADATA_BUNDLE_STATUS";
	public static final String METADATA_BUNDLE_METADATA = "METADATA_BUNDLE_METADATA";

	protected void loadMetaDataFor(DefaultBundleData data) throws IOException {
		MetaData bundleMetaData = (new MetaData(new File(data.getBundleStoreDir(), ".bundle"), "Bundle metadata"));
		bundleMetaData.load();

		data.setLocation(bundleMetaData.get(METADATA_BUNDLE_LOC, null));
		data.setFileName(bundleMetaData.get(METADATA_BUNDLE_NAME, null));
		data.setGeneration(bundleMetaData.getInt(METADATA_BUNDLE_GEN, -1));
		data.setNativePaths(bundleMetaData.get(METADATA_BUNDLE_NCP, null));
		data.setStartLevel(bundleMetaData.getInt(METADATA_BUNDLE_ABSL, 1));
		data.setStatus(bundleMetaData.getInt(METADATA_BUNDLE_STATUS, 0));
		data.setReference(bundleMetaData.getBoolean(METADATA_BUNDLE_REF, false));

		if (data.getGeneration() == -1 || data.getFileName() == null || data.getLocation() == null) {
			throw new IOException(AdaptorMsg.formatter.getString("ADAPTOR_STORAGE_EXCEPTION"));
		}
	}

	public void saveMetaDataFor(DefaultBundleData data) throws IOException {
		MetaData bundleMetadata = (new MetaData(new File(data.createBundleStoreDir(), ".bundle"), "Bundle metadata"));
		bundleMetadata.load();

		bundleMetadata.set(METADATA_BUNDLE_LOC, data.getLocation());
		bundleMetadata.set(METADATA_BUNDLE_NAME, data.getFileName());
		bundleMetadata.setInt(METADATA_BUNDLE_GEN, data.getGeneration());
		String nativePaths = data.getNativePathsString();
		if (nativePaths != null) {
			bundleMetadata.set(METADATA_BUNDLE_NCP, nativePaths);
		}
		bundleMetadata.setInt(METADATA_BUNDLE_ABSL, data.getStartLevel());
		bundleMetadata.setInt(METADATA_BUNDLE_STATUS, data.getStatus());
		bundleMetadata.setBoolean(METADATA_BUNDLE_REF, data.isReference());

		bundleMetadata.save();
	}
}