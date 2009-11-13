/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.registry;

import java.io.*;
import java.util.HashMap;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.osgi.util.NLS;

public class TableReader {
	//Markers in the cache 
	static final int NULL = 0;
	static final int OBJECT = 1;

	//The version of the cache
	static final int CACHE_VERSION = 7;
	// Version 1 -> 2: the contributor Ids changed from "long" to "String"
	// Version 2 -> 3: added namespace index and the table of contributors
	// Version 3 -> 4: offset table saved in a binary form (performance)
	// Version 4 -> 5: remove support added in version 4 to save offset table in a binary form (performance)
	// Version 5 -> 6: replace HashtableOfInt with OffsetTable (memory usage optimization)
	// Version 6 -> 7: added option for multi-language support

	//Informations representing the MAIN file
	static final String MAIN = ".mainData"; //$NON-NLS-1$
	BufferedRandomInputStream mainDataFile = null;
	DataInputStream mainInput = null;

	//Informations representing the EXTRA file
	static final String EXTRA = ".extraData"; //$NON-NLS-1$
	BufferedRandomInputStream extraDataFile = null;
	DataInputStream extraInput = null;

	//The table file
	static final String TABLE = ".table"; //$NON-NLS-1$
	File tableFile;

	//The contributions file
	static final String CONTRIBUTIONS = ".contributions"; //$NON-NLS-1$
	File contributionsFile;

	//The contributor file
	static final String CONTRIBUTORS = ".contributors"; //$NON-NLS-1$
	File contributorsFile;

	//The namespace file
	static final String NAMESPACES = ".namespaces"; //$NON-NLS-1$
	File namespacesFile;

	//The orphan file
	static final String ORPHANS = ".orphans"; //$NON-NLS-1$
	File orphansFile;

	//Status code
	private static final byte fileError = 0;
	private static final boolean DEBUG = false; //TODO need to change

	private boolean holdObjects = false;

	private ExtensionRegistry registry;

	void setMainDataFile(File main) throws IOException {
		mainDataFile = new BufferedRandomInputStream(main);
		mainInput = new DataInputStream(mainDataFile);
	}

	void setExtraDataFile(File extra) throws IOException {
		extraDataFile = new BufferedRandomInputStream(extra);
		extraInput = new DataInputStream(extraDataFile);
	}

	void setTableFile(File table) {
		tableFile = table;
	}

	void setContributionsFile(File namespace) {
		contributionsFile = namespace;
	}

	void setContributorsFile(File file) {
		contributorsFile = file;
	}

	void setNamespacesFile(File file) {
		namespacesFile = file;
	}

	void setOrphansFile(File orphan) {
		orphansFile = orphan;
	}

	public TableReader(ExtensionRegistry registry) {
		this.registry = registry;
	}

	// Don't need to synchronize - called only from a synchronized method
	public Object[] loadTables(long expectedTimestamp) {
		HashtableOfStringAndInt extensionPoints;

		DataInputStream tableInput = null;
		try {
			tableInput = new DataInputStream(new BufferedInputStream(new FileInputStream(tableFile)));
			if (!checkCacheValidity(tableInput, expectedTimestamp))
				return null;

			Integer nextId = new Integer(tableInput.readInt());
			OffsetTable offsets = OffsetTable.load(tableInput);
			extensionPoints = new HashtableOfStringAndInt();
			extensionPoints.load(tableInput);
			return new Object[] {offsets, extensionPoints, nextId};
		} catch (IOException e) {
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, RegistryMessages.meta_registryCacheReadProblems, e));
			return null;
		} finally {
			if (tableInput != null)
				try {
					tableInput.close();
				} catch (IOException e1) {
					//Ignore
				}
		}

	}

	//	Check various aspect of the cache to see if it's valid 
	private boolean checkCacheValidity(DataInputStream in, long expectedTimestamp) {
		int version;
		try {
			version = in.readInt();
			if (version != CACHE_VERSION)
				return false;

			long installStamp = in.readLong();
			long registryStamp = in.readLong();
			long mainDataFileSize = in.readLong();
			long extraDataFileSize = in.readLong();
			long contributionsFileSize = in.readLong();
			long contributorsFileSize = in.readLong();
			long namespacesFileSize = in.readLong();
			long orphansFileSize = in.readLong();
			String osStamp = in.readUTF();
			String windowsStamp = in.readUTF();
			String localeStamp = in.readUTF();
			boolean multiLanguage = in.readBoolean();

			boolean validTime = (expectedTimestamp == 0 || expectedTimestamp == registryStamp);
			boolean validInstall = (installStamp == registry.computeState());
			boolean validOS = (osStamp.equals(RegistryProperties.getProperty(IRegistryConstants.PROP_OS, RegistryProperties.empty)));
			boolean validWS = (windowsStamp.equals(RegistryProperties.getProperty(IRegistryConstants.PROP_WS, RegistryProperties.empty)));
			boolean validNL = (localeStamp.equals(RegistryProperties.getProperty(IRegistryConstants.PROP_NL, RegistryProperties.empty)));
			boolean validMultiLang = (registry.isMultiLanguage() == multiLanguage);

			if (!validTime || !validInstall || !validOS || !validWS || !validNL || !validMultiLang)
				return false;

			boolean validMain = (mainDataFileSize == mainDataFile.length());
			boolean validExtra = (extraDataFileSize == extraDataFile.length());
			boolean validContrib = (contributionsFileSize == contributionsFile.length());
			boolean validContributors = (contributorsFileSize == contributorsFile.length());
			boolean validNamespace = (namespacesFileSize == namespacesFile.length());
			boolean validOrphan = (orphansFileSize == orphansFile.length());

			return (validMain && validExtra && validContrib && validContributors && validNamespace && validOrphan);
		} catch (IOException e) {
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, RegistryMessages.meta_registryCacheInconsistent, e));
			return false;
		}
	}

	public Object loadConfigurationElement(int offset) {
		try {
			synchronized (mainDataFile) {
				goToInputFile(offset);
				return basicLoadConfigurationElement(mainInput, null);
			}
		} catch (IOException e) {
			String message = NLS.bind(RegistryMessages.meta_regCacheIOExceptionReading, mainDataFile);
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, message, e));
			if (DEBUG)
				log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, "Error reading a configuration element (" + offset + ") from the registry cache", e)); //$NON-NLS-1$//$NON-NLS-2$
			return null;
		}
	}

	private ConfigurationElement basicLoadConfigurationElement(DataInputStream is, String actualContributorId) throws IOException {
		int self = is.readInt();
		String contributorId = readStringOrNull(is);
		String name = readStringOrNull(is);
		int parentId = is.readInt();
		byte parentType = is.readByte();
		int misc = is.readInt();//this is set in second level CEs, to indicate where in the extra data file the children CEs are
		String[] propertiesAndValue = readPropertiesAndValue(is);
		int[] children = readArray(is);
		if (actualContributorId == null)
			actualContributorId = contributorId;
		ConfigurationElement result = getObjectFactory().createConfigurationElement(self, actualContributorId, name, propertiesAndValue, children, misc, parentId, parentType, true);
		if (registry.isMultiLanguage()) { // cache is multi-language too or it would have failed validation
			int numberOfLocales = is.readInt();
			DirectMap translated = null;
			if (numberOfLocales != 0) {
				translated = new DirectMap(numberOfLocales, 0.5f);
				String[] NLs = readStringArray(is);
				for (int i = 0; i < numberOfLocales; i++) {
					String[] translatedProperties = readStringArray(is);
					translated.put(NLs[i], translatedProperties);
				}
			}
			ConfigurationElementMulti multiCE = (ConfigurationElementMulti) result;
			if (translated != null)
				multiCE.setTranslatedProperties(translated);
		}
		return result;
	}

	private String[] readStringArray(DataInputStream is) throws IOException {
		int size = is.readInt();
		if (size == 0)
			return null;
		String[] result = new String[size];
		for (int i = 0; i < size; i++) {
			result[i] = readStringOrNull(is);
		}
		return result;
	}

	public Object loadThirdLevelConfigurationElements(int offset, RegistryObjectManager objectManager) {
		try {
			synchronized (extraDataFile) {
				goToExtraFile(offset);
				return loadConfigurationElementAndChildren(null, extraInput, 3, Integer.MAX_VALUE, objectManager, null);
			}
		} catch (IOException e) {
			String message = NLS.bind(RegistryMessages.meta_regCacheIOExceptionReading, extraDataFile);
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, message, e));
			if (DEBUG)
				log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, "Error reading a third level configuration element (" + offset + ") from the registry cache", e)); //$NON-NLS-1$//$NON-NLS-2$
			return null;
		}
	}

	//Read a whole configuration element subtree
	private ConfigurationElement loadConfigurationElementAndChildren(DataInputStream is, DataInputStream extraIs, int depth, int maxDepth, RegistryObjectManager objectManager, String namespaceOwnerId) throws IOException {
		DataInputStream currentStream = is;
		if (depth > 2)
			currentStream = extraIs;

		ConfigurationElement ce = basicLoadConfigurationElement(currentStream, namespaceOwnerId);
		if (namespaceOwnerId == null)
			namespaceOwnerId = ce.getContributorId();
		int[] children = ce.getRawChildren();
		if (depth + 1 > maxDepth)
			return ce;

		for (int i = 0; i < children.length; i++) {
			ConfigurationElement tmp = loadConfigurationElementAndChildren(currentStream, extraIs, depth + 1, maxDepth, objectManager, namespaceOwnerId);
			objectManager.add(tmp, holdObjects);
		}
		return ce;
	}

	private String[] readPropertiesAndValue(DataInputStream inputStream) throws IOException {
		int numberOfProperties = inputStream.readInt();
		if (numberOfProperties == 0)
			return RegistryObjectManager.EMPTY_STRING_ARRAY;
		String[] properties = new String[numberOfProperties];
		for (int i = 0; i < numberOfProperties; i++) {
			properties[i] = readStringOrNull(inputStream);
		}
		return properties;
	}

	public Object loadExtension(int offset) {
		try {
			synchronized (mainDataFile) {
				goToInputFile(offset);
				return basicLoadExtension(mainInput);
			}
		} catch (IOException e) {
			String message = NLS.bind(RegistryMessages.meta_regCacheIOExceptionReading, mainDataFile);
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, message, e));
			if (DEBUG)
				log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, "Error reading an extension (" + offset + ") from the registry cache", e)); //$NON-NLS-1$//$NON-NLS-2$
		}
		return null;
	}

	private Extension basicLoadExtension(DataInputStream inputStream) throws IOException {
		int self = inputStream.readInt();
		String simpleId = readStringOrNull(mainInput);
		String namespace = readStringOrNull(mainInput);
		int[] children = readArray(mainInput);
		int extraData = mainInput.readInt();
		return getObjectFactory().createExtension(self, simpleId, namespace, children, extraData, true);
	}

	public ExtensionPoint loadExtensionPointTree(int offset, RegistryObjectManager objects) {
		try {
			synchronized (mainDataFile) {
				ExtensionPoint xpt = (ExtensionPoint) loadExtensionPoint(offset);
				int[] children = xpt.getRawChildren();
				int nbrOfExtension = children.length;
				for (int i = 0; i < nbrOfExtension; i++) {
					Extension loaded = basicLoadExtension(mainInput);
					objects.add(loaded, holdObjects);
				}

				for (int i = 0; i < nbrOfExtension; i++) {
					int nbrOfCe = mainInput.readInt();
					for (int j = 0; j < nbrOfCe; j++) {
						// note that max depth is set to 2 and extra input is never going to 
						// be used in this call to the loadConfigurationElementAndChildren().
						objects.add(loadConfigurationElementAndChildren(mainInput, null, 1, 2, objects, null), holdObjects);
					}
				}
				return xpt;
			}
		} catch (IOException e) {
			String message = NLS.bind(RegistryMessages.meta_regCacheIOExceptionReading, mainDataFile);
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, message, e));
			if (DEBUG)
				log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, "Error reading an extension point tree (" + offset + ") from the registry cache", e)); //$NON-NLS-1$//$NON-NLS-2$
			return null;
		}
	}

	private Object loadExtensionPoint(int offset) {
		try {
			goToInputFile(offset);
			return basicLoadExtensionPoint();
		} catch (IOException e) {
			String message = NLS.bind(RegistryMessages.meta_regCacheIOExceptionReading, mainDataFile);
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, message, e));
			if (DEBUG)
				log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, "Error reading an extension point (" + offset + ") from the registry cache", e)); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
	}

	private ExtensionPoint basicLoadExtensionPoint() throws IOException {
		int self = mainInput.readInt();
		int[] children = readArray(mainInput);
		int extraData = mainInput.readInt();
		return getObjectFactory().createExtensionPoint(self, children, extraData, true);
	}

	private int[] readArray(DataInputStream in) throws IOException {
		int arraySize = in.readInt();
		if (arraySize == 0)
			return RegistryObjectManager.EMPTY_INT_ARRAY;
		int[] result = new int[arraySize];
		for (int i = 0; i < arraySize; i++) {
			result[i] = in.readInt();
		}
		return result;
	}

	private void goToInputFile(int offset) throws IOException {
		mainDataFile.seek(offset);
	}

	private void goToExtraFile(int offset) throws IOException {
		extraDataFile.seek(offset);
	}

	private String readStringOrNull(DataInputStream in) throws IOException {
		byte type = in.readByte();
		if (type == NULL)
			return null;
		return in.readUTF();
	}

	public String[] loadExtensionExtraData(int dataPosition) {
		try {
			synchronized (extraDataFile) {
				goToExtraFile(dataPosition);
				return basicLoadExtensionExtraData();
			}
		} catch (IOException e) {
			String message = NLS.bind(RegistryMessages.meta_regCacheIOExceptionReading, extraDataFile);
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, message, e));
			if (DEBUG)
				log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, "Error reading extension label (" + dataPosition + ") from the registry cache", e)); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
	}

	private String[] basicLoadExtensionExtraData() throws IOException {
		return new String[] {readStringOrNull(extraInput), readStringOrNull(extraInput), readStringOrNull(extraInput)};
	}

	public String[] loadExtensionPointExtraData(int offset) {
		try {
			synchronized (extraDataFile) {
				goToExtraFile(offset);
				return basicLoadExtensionPointExtraData();
			}
		} catch (IOException e) {
			String message = NLS.bind(RegistryMessages.meta_regCacheIOExceptionReading, extraDataFile);
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, message, e));
			if (DEBUG)
				log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, "Error reading extension point data (" + offset + ") from the registry cache", e)); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
	}

	private String[] basicLoadExtensionPointExtraData() throws IOException {
		String[] result = new String[5];
		result[0] = readStringOrNull(extraInput); //the label
		result[1] = readStringOrNull(extraInput); //the schema
		result[2] = readStringOrNull(extraInput); //the fully qualified name
		result[3] = readStringOrNull(extraInput); //the namespace
		result[4] = readStringOrNull(extraInput); //the contributor Id 
		return result;
	}

	public KeyedHashSet loadContributions() {
		DataInputStream namespaceInput = null;
		try {
			synchronized (contributionsFile) {
				namespaceInput = new DataInputStream(new BufferedInputStream(new FileInputStream(contributionsFile)));
				int size = namespaceInput.readInt();
				KeyedHashSet result = new KeyedHashSet(size);
				for (int i = 0; i < size; i++) {
					String contributorId = readStringOrNull(namespaceInput);
					Contribution n = getObjectFactory().createContribution(contributorId, true);
					n.setRawChildren(readArray(namespaceInput));
					result.add(n);
				}
				return result;
			}
		} catch (IOException e) {
			String message = NLS.bind(RegistryMessages.meta_regCacheIOExceptionReading, contributionsFile);
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, message, e));
			return null;
		} finally {
			if (namespaceInput != null)
				try {
					namespaceInput.close();
				} catch (IOException e1) {
					//Ignore
				}
		}
	}

	final static float contributorsLoadFactor = 1.2f; // allocate more memory to avoid resizing

	public HashMap loadContributors() {
		HashMap result = null;
		DataInputStream contributorsInput = null;
		try {
			synchronized (contributorsFile) {
				contributorsInput = new DataInputStream(new BufferedInputStream(new FileInputStream(contributorsFile)));
				int size = contributorsInput.readInt();
				result = new HashMap((int) (size * contributorsLoadFactor));
				for (int i = 0; i < size; i++) {
					String id = readStringOrNull(contributorsInput);
					String name = readStringOrNull(contributorsInput);
					String hostId = readStringOrNull(contributorsInput);
					String hostName = readStringOrNull(contributorsInput);
					result.put(id, new RegistryContributor(id, name, hostId, hostName));
				}
			}
			return result;
		} catch (IOException e) {
			String message = NLS.bind(RegistryMessages.meta_regCacheIOExceptionReading, contributorsFile);
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, message, e));
			return null;
		} finally {
			if (contributorsInput != null)
				try {
					contributorsInput.close();
				} catch (IOException e1) {
					//Ignore
				}
		}
	}

	public KeyedHashSet loadNamespaces() {
		DataInputStream namespaceInput = null;
		try {
			synchronized (namespacesFile) {
				namespaceInput = new DataInputStream(new BufferedInputStream(new FileInputStream(namespacesFile)));
				int size = namespaceInput.readInt();
				KeyedHashSet result = new KeyedHashSet(size);
				for (int i = 0; i < size; i++) {
					String key = readStringOrNull(namespaceInput);
					RegistryIndexElement indexElement = new RegistryIndexElement(key);
					indexElement.updateExtensionPoints(readArray(namespaceInput), true);
					indexElement.updateExtensions(readArray(namespaceInput), true);
					result.add(indexElement);
				}
				return result;
			}
		} catch (IOException e) {
			String message = NLS.bind(RegistryMessages.meta_regCacheIOExceptionReading, namespacesFile);
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, message, e));
			return null;
		} finally {
			if (namespaceInput != null)
				try {
					namespaceInput.close();
				} catch (IOException e1) {
					//Ignore
				}
		}
	}

	private void loadAllOrphans(RegistryObjectManager objectManager) throws IOException {
		//Read the extensions and configuration elements of the orphans
		int orphans = objectManager.getOrphanExtensions().size();
		for (int k = 0; k < orphans; k++) {
			int numberOfOrphanExtensions = mainInput.readInt();
			for (int i = 0; i < numberOfOrphanExtensions; i++) {
				loadFullExtension(objectManager);
			}
			for (int i = 0; i < numberOfOrphanExtensions; i++) {
				int nbrOfCe = mainInput.readInt();
				for (int j = 0; j < nbrOfCe; j++) {
					objectManager.add(loadConfigurationElementAndChildren(mainInput, extraInput, 1, Integer.MAX_VALUE, objectManager, null), true);
				}
			}
		}
	}

	// Do not need to synchronize - called only from a synchronized method
	public boolean readAllCache(RegistryObjectManager objectManager) {
		try {
			int size = objectManager.getExtensionPoints().size();
			for (int i = 0; i < size; i++) {
				objectManager.add(readAllExtensionPointTree(objectManager), holdObjects);
			}
			loadAllOrphans(objectManager);
		} catch (IOException e) {
			String message = NLS.bind(RegistryMessages.meta_regCacheIOExceptionReading, mainDataFile);
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, message, e));
			return false;
		}
		return true;
	}

	private ExtensionPoint readAllExtensionPointTree(RegistryObjectManager objectManager) throws IOException {
		ExtensionPoint xpt = loadFullExtensionPoint();
		int[] children = xpt.getRawChildren();
		int nbrOfExtension = children.length;
		for (int i = 0; i < nbrOfExtension; i++) {
			loadFullExtension(objectManager);
		}

		for (int i = 0; i < nbrOfExtension; i++) {
			int nbrOfCe = mainInput.readInt();
			for (int j = 0; j < nbrOfCe; j++) {
				objectManager.add(loadConfigurationElementAndChildren(mainInput, extraInput, 1, Integer.MAX_VALUE, objectManager, null), true);
			}
		}
		return xpt;
	}

	private ExtensionPoint loadFullExtensionPoint() throws IOException { //TODO I don't like this. 
		ExtensionPoint xpt = basicLoadExtensionPoint();
		String[] tmp = basicLoadExtensionPointExtraData();
		xpt.setLabel(tmp[0]);
		xpt.setSchema(tmp[1]);
		xpt.setUniqueIdentifier(tmp[2]);
		xpt.setNamespace(tmp[3]);
		xpt.setContributorId(tmp[4]);
		return xpt;
	}

	private Extension loadFullExtension(RegistryObjectManager objectManager) throws IOException {
		String[] tmp;
		Extension loaded = basicLoadExtension(mainInput);
		tmp = basicLoadExtensionExtraData();
		loaded.setLabel(tmp[0]);
		loaded.setExtensionPointIdentifier(tmp[1]);
		loaded.setContributorId(tmp[2]);
		objectManager.add(loaded, holdObjects);
		return loaded;
	}

	public HashMap loadOrphans() {
		DataInputStream orphanInput = null;
		try {
			synchronized (orphansFile) {
				orphanInput = new DataInputStream(new BufferedInputStream(new FileInputStream(orphansFile)));
				int size = orphanInput.readInt();
				HashMap result = new HashMap(size);
				for (int i = 0; i < size; i++) {
					String key = orphanInput.readUTF();
					int[] value = readArray(orphanInput);
					result.put(key, value);
				}
				return result;
			}
		} catch (IOException e) {
			return null;
		} finally {
			if (orphanInput != null)
				try {
					orphanInput.close();
				} catch (IOException e1) {
					//ignore
				}
		}
	}

	// Don't need to synchronize - called only from a synchronized method
	public void setHoldObjects(boolean holdObjects) {
		this.holdObjects = holdObjects;
	}

	private void log(Status status) {
		registry.log(status);
	}

	private RegistryObjectFactory getObjectFactory() {
		return registry.getElementFactory();
	}

	// Returns a file name used to test if cache is actually present at a given location
	public static String getTestFileName() {
		return TABLE;
	}

	public void close() {
		try {
			if (mainInput != null)
				mainInput.close();
			if (extraInput != null)
				extraInput.close();
		} catch (IOException e) {
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, RegistryMessages.meta_registryCacheReadProblems, e));
		}
	}

}
