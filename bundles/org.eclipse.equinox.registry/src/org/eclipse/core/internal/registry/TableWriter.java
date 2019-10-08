/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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
package org.eclipse.core.internal.registry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.spi.RegistryContributor;

public class TableWriter {
	private static final byte fileError = 0;

	File mainDataFile;
	File extraDataFile;
	File tableFile;
	File contributionsFile;
	File contributorsFile;
	File namespacesFile;
	File orphansFile;

	void setMainDataFile(File main) {
		mainDataFile = main;
	}

	void setExtraDataFile(File extra) {
		extraDataFile = extra;
	}

	void setTableFile(File table) {
		tableFile = table;
	}

	void setContributionsFile(File fileName) {
		contributionsFile = fileName;
	}

	void setContributorsFile(File fileName) {
		contributorsFile = fileName;
	}

	void setNamespacesFile(File fileName) {
		namespacesFile = fileName;
	}

	void setOrphansFile(File orphan) {
		orphansFile = orphan;
	}

	DataOutputStream mainOutput;
	DataOutputStream extraOutput;
	FileOutputStream mainFileOutput = null;
	FileOutputStream extraFileOutput = null;

	private OffsetTable offsets;

	private final ExtensionRegistry registry;
	private RegistryObjectManager objectManager;

	public TableWriter(ExtensionRegistry registry) {
		this.registry = registry;
	}

	private int getExtraDataPosition() {
		return extraOutput.size();
	}

	public boolean saveCache(RegistryObjectManager objectManager, long timestamp) {
		this.objectManager = objectManager;
		try {
			if (!openFiles())
				return false;
			try {
				saveExtensionRegistry(timestamp);
			} catch (IOException io) {
				log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, RegistryMessages.meta_registryCacheWriteProblems, io));
				return false;
			}
		} finally {
			closeFiles();
		}
		return true;
	}

	private boolean openFiles() {
		try {
			mainFileOutput = new FileOutputStream(mainDataFile);
			mainOutput = new DataOutputStream(new BufferedOutputStream(mainFileOutput));
			extraFileOutput = new FileOutputStream(extraDataFile);
			extraOutput = new DataOutputStream(new BufferedOutputStream(extraFileOutput));
		} catch (FileNotFoundException e) {
			if (mainFileOutput != null)
				try {
					mainFileOutput.close();
				} catch (IOException e1) {
					//Ignore
				}

			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, RegistryMessages.meta_unableToCreateCache, e));
			return false;
		}
		return true;
	}

	private void closeFiles() {
		try {
			if (mainOutput != null) {
				mainOutput.flush();
				if (mainFileOutput.getFD().valid()) {
					mainFileOutput.getFD().sync();
				}
				mainOutput.close();
			}
		} catch (IOException e) {
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, RegistryMessages.meta_registryCacheWriteProblems, e));
			e.printStackTrace();
		}
		try {
			if (extraOutput != null) {
				extraOutput.flush();
				if (extraFileOutput.getFD().valid()) {
					extraFileOutput.getFD().sync();
				}
				extraOutput.close();
			}
		} catch (IOException e) {
			log(new Status(IStatus.ERROR, RegistryMessages.OWNER_NAME, fileError, RegistryMessages.meta_registryCacheWriteProblems, e));
			e.printStackTrace();
		}
	}

	private void saveExtensionRegistry(long timestamp) throws IOException {
		ExtensionPointHandle[] points = objectManager.getExtensionPointsHandles();
		offsets = new OffsetTable(objectManager.getNextId());
		for (ExtensionPointHandle point : points) {
			saveExtensionPoint(point);
		}
		saveOrphans();
		saveContributions(objectManager.getContributions());
		saveContributors(objectManager.getContributors());
		saveNamespaces(objectManager.getNamespacesIndex());
		closeFiles(); //Close the files here so we can write the appropriate size information in the table file.
		saveTables(timestamp); //Write the table last so if that is something went wrong we can know
	}

	private void saveContributions(KeyedHashSet[] contributions) throws IOException {
		FileOutputStream fosNamespace = new FileOutputStream(contributionsFile);
		DataOutputStream outputNamespace = new DataOutputStream(new BufferedOutputStream(fosNamespace));
		KeyedElement[] newElements = contributions[0].elements();
		KeyedElement[] formerElements = contributions[1].elements();

		// get count of contributions that will be cached
		int cacheSize = 0;
		for (KeyedElement newElement : newElements) {
			if (((Contribution) newElement).shouldPersist()) {
				cacheSize++;
			}
		}
		for (KeyedElement formerElement : formerElements) {
			if (((Contribution) formerElement).shouldPersist()) {
				cacheSize++;
			}
		}
		outputNamespace.writeInt(cacheSize);

		for (KeyedElement newElement : newElements) {
			Contribution element = (Contribution) newElement;
			if (element.shouldPersist()) {
				writeStringOrNull(element.getContributorId(), outputNamespace);
				saveArray(filterContributionChildren(element), outputNamespace);
			}
		}
		for (KeyedElement formerElement : formerElements) {
			Contribution element = (Contribution) formerElement;
			if (element.shouldPersist()) {
				writeStringOrNull(element.getContributorId(), outputNamespace);
				saveArray(filterContributionChildren(element), outputNamespace);
			}
		}
		outputNamespace.flush();
		fosNamespace.getFD().sync();
		outputNamespace.close();
	}

	// Contribution has raw children in a unique format that combines extensions and extension points.
	// To filter, need to dis-assmeble, filter, and then re-assemble its raw children
	private int[] filterContributionChildren(Contribution element) {
		int[] extensionPoints = filter(element.getExtensionPoints());
		int[] extensions = filter(element.getExtensions());
		int[] filteredRawChildren = new int[2 + extensionPoints.length + extensions.length];
		System.arraycopy(extensionPoints, 0, filteredRawChildren, 2, extensionPoints.length);
		System.arraycopy(extensions, 0, filteredRawChildren, 2 + extensionPoints.length, extensions.length);
		filteredRawChildren[Contribution.EXTENSION_POINT] = extensionPoints.length;
		filteredRawChildren[Contribution.EXTENSION] = extensions.length;
		return filteredRawChildren;
	}

	private void saveNamespaces(KeyedHashSet namespacesIndex) throws IOException {
		FileOutputStream fosNamespace = new FileOutputStream(namespacesFile);
		DataOutputStream outputNamespace = new DataOutputStream(new BufferedOutputStream(fosNamespace));
		KeyedElement[] elements = namespacesIndex.elements();

		KeyedElement[] cachedElements = new KeyedElement[elements.length];
		int cacheSize = 0;
		for (KeyedElement e : elements) {
			RegistryIndexElement element = (RegistryIndexElement) e;
			int[] extensionPoints = filter(element.getExtensionPoints());
			int[] extensions = filter(element.getExtensions());
			if (extensionPoints.length == 0 && extensions.length == 0)
				continue;
			RegistryIndexElement cachedElement = new RegistryIndexElement((String) element.getKey(), extensionPoints, extensions);
			cachedElements[cacheSize] = cachedElement;
			cacheSize++;
		}

		outputNamespace.writeInt(cacheSize);
		for (int i = 0; i < cacheSize; i++) {
			RegistryIndexElement element = (RegistryIndexElement) cachedElements[i];
			writeStringOrNull((String) element.getKey(), outputNamespace);
			saveArray(element.getExtensionPoints(), outputNamespace); // it was pre-filtered as we counted the number of elements
			saveArray(element.getExtensions(), outputNamespace); // it was pre-filtered as we counted the number of elements
		}
		outputNamespace.flush();
		fosNamespace.getFD().sync();
		outputNamespace.close();
	}

	private void saveContributors(HashMap<?, ?> contributors) throws IOException {
		FileOutputStream fosContributors = new FileOutputStream(contributorsFile);
		DataOutputStream outputContributors = new DataOutputStream(new BufferedOutputStream(fosContributors));

		Collection<?> entries = contributors.values();
		outputContributors.writeInt(entries.size());

		for (Iterator<?> i = entries.iterator(); i.hasNext();) {
			RegistryContributor contributor = (RegistryContributor) i.next();
			writeStringOrNull(contributor.getActualId(), outputContributors);
			writeStringOrNull(contributor.getActualName(), outputContributors);
			writeStringOrNull(contributor.getId(), outputContributors);
			writeStringOrNull(contributor.getName(), outputContributors);
		}

		outputContributors.flush();
		fosContributors.getFD().sync();
		outputContributors.close();
	}

	private void saveTables(long registryTimeStamp) throws IOException {
		FileOutputStream fosTable = new FileOutputStream(tableFile);
		DataOutputStream outputTable = new DataOutputStream(new BufferedOutputStream(fosTable));
		writeCacheHeader(outputTable, registryTimeStamp);
		outputTable.writeInt(objectManager.getNextId());
		offsets.save(outputTable);
		objectManager.getExtensionPoints().save(outputTable, objectManager); // uses writer to filter contents
		outputTable.flush();
		fosTable.getFD().sync();
		outputTable.close();
	}

	private void writeCacheHeader(DataOutputStream output, long registryTimeStamp) throws IOException {
		output.writeInt(TableReader.CACHE_VERSION);
		output.writeLong(registry.computeState());
		output.writeLong(registryTimeStamp);
		output.writeLong(mainDataFile.length());
		output.writeLong(extraDataFile.length());
		output.writeLong(contributionsFile.length());
		output.writeLong(contributorsFile.length());
		output.writeLong(namespacesFile.length());
		output.writeLong(orphansFile.length());
		output.writeUTF(RegistryProperties.getProperty(IRegistryConstants.PROP_OS, RegistryProperties.empty));
		output.writeUTF(RegistryProperties.getProperty(IRegistryConstants.PROP_WS, RegistryProperties.empty));
		output.writeUTF(RegistryProperties.getProperty(IRegistryConstants.PROP_NL, RegistryProperties.empty));
		output.writeBoolean(registry.isMultiLanguage());
	}

	private void saveArray(int[] array, DataOutputStream out) throws IOException {
		if (array == null) {
			out.writeInt(0);
			return;
		}
		out.writeInt(array.length);
		for (int i = 0; i < array.length; i++) {
			out.writeInt(array[i]);
		}
	}

	private void saveExtensionPoint(ExtensionPointHandle xpt) throws IOException {
		if (!xpt.shouldPersist())
			return;
		//save the file position
		offsets.put(xpt.getId(), mainOutput.size());
		//save the extensionPoint
		mainOutput.writeInt(xpt.getId());
		saveArray(filter(xpt.getObject().getRawChildren()), mainOutput);
		mainOutput.writeInt(getExtraDataPosition());
		saveExtensionPointData(xpt);

		saveExtensions(xpt.getExtensions(), mainOutput);
	}

	private void saveExtension(ExtensionHandle ext, DataOutputStream outputStream) throws IOException {
		if (!ext.shouldPersist())
			return;
		offsets.put(ext.getId(), outputStream.size());
		outputStream.writeInt(ext.getId());
		writeStringOrNull(ext.getSimpleIdentifier(), outputStream);
		writeStringOrNull(ext.getNamespaceIdentifier(), outputStream);
		saveArray(filter(ext.getObject().getRawChildren()), outputStream);
		outputStream.writeInt(getExtraDataPosition());
		saveExtensionData(ext);
	}

	private void writeStringArray(String[] array, DataOutputStream outputStream) throws IOException {
		outputStream.writeInt(array == null ? 0 : array.length);
		for (int i = 0; i < (array == null ? 0 : array.length); i++) {
			writeStringOrNull(array[i], outputStream);
		}
	}

	private void writeStringArray(String[] array, int size, DataOutputStream outputStream) throws IOException {
		outputStream.writeInt(array == null ? 0 : size);
		if (array == null)
			return;
		for (int i = 0; i < size; i++) {
			writeStringOrNull(array[i], outputStream);
		}
	}

	//Save Configuration elements depth first
	private void saveConfigurationElement(ConfigurationElementHandle element, DataOutputStream outputStream, DataOutputStream extraOutputStream, int depth) throws IOException {
		if (!element.shouldPersist())
			return;
		DataOutputStream currentOutput = outputStream;
		if (depth > 2)
			currentOutput = extraOutputStream;

		offsets.put(element.getId(), currentOutput.size());

		currentOutput.writeInt(element.getId());
		ConfigurationElement actualCe = (ConfigurationElement) element.getObject();

		writeStringOrNull(actualCe.getContributorId(), currentOutput);
		writeStringOrNull(actualCe.getName(), currentOutput);
		currentOutput.writeInt(actualCe.parentId);
		currentOutput.writeByte(actualCe.parentType);
		currentOutput.writeInt(depth > 1 ? extraOutputStream.size() : -1);
		writeStringArray(actualCe.getPropertiesAndValue(), currentOutput);
		//save the children
		saveArray(filter(actualCe.getRawChildren()), currentOutput);

		if (actualCe instanceof ConfigurationElementMulti) {
			ConfigurationElementMulti multiCE = (ConfigurationElementMulti) actualCe;
			int NLs = multiCE.getNumCachedLocales();
			currentOutput.writeInt(NLs);
			if (NLs != 0) {
				writeStringArray(multiCE.getCachedLocales(), NLs, currentOutput);
				String[][] translated = multiCE.getCachedTranslations();
				for (int i = 0; i < NLs; i++) {
					writeStringArray(translated[i], currentOutput);
				}
			}
		}

		ConfigurationElementHandle[] childrenCEs = (ConfigurationElementHandle[]) element.getChildren();
		for (ConfigurationElementHandle childrenCE : childrenCEs) {
			saveConfigurationElement(childrenCE, outputStream, extraOutputStream, depth + 1);
		}

	}

	private void saveExtensions(IExtension[] exts, DataOutputStream outputStream) throws IOException {
		for (IExtension ext : exts) {
			saveExtension((ExtensionHandle) ext, outputStream);
		}
		for (IExtension ext : exts) {
			if (!((ExtensionHandle) ext).shouldPersist()) {
				continue;
			}
			IConfigurationElement[] ces = ext.getConfigurationElements();
			int countCElements = 0;
			boolean[] save = new boolean[ces.length];
			for (int j = 0; j < ces.length; j++) {
				if (((ConfigurationElementHandle) ces[j]).shouldPersist()) {
					save[j] = true;
					countCElements++;
				} else
					save[j] = false;
			}
			outputStream.writeInt(countCElements);
			for (int j = 0; j < ces.length; j++) {
				if (save[j])
					saveConfigurationElement((ConfigurationElementHandle) ces[j], outputStream, extraOutput, 1);
			}
		}
	}

	private void saveExtensionPointData(ExtensionPointHandle xpt) throws IOException {
		writeStringOrNull(xpt.getLabelAsIs(), extraOutput);
		writeStringOrNull(xpt.getSchemaReference(), extraOutput);
		writeStringOrNull(xpt.getUniqueIdentifier(), extraOutput);
		writeStringOrNull(xpt.getNamespaceIdentifier(), extraOutput);
		writeStringOrNull(((ExtensionPoint) xpt.getObject()).getContributorId(), extraOutput);
	}

	private void saveExtensionData(ExtensionHandle extension) throws IOException {
		writeStringOrNull(extension.getLabelAsIs(), extraOutput);
		writeStringOrNull(extension.getExtensionPointUniqueIdentifier(), extraOutput);
		writeStringOrNull(extension.getContributorId(), extraOutput);
	}

	private void writeStringOrNull(String string, DataOutputStream out) throws IOException {
		if (string == null)
			out.writeByte(TableReader.NULL);
		else {
			byte[] data = string.getBytes(StandardCharsets.UTF_8);
			if (data.length > 65535) {
				out.writeByte(TableReader.LOBJECT);
				out.writeInt(data.length);
				out.write(data);
			} else {
				out.writeByte(TableReader.OBJECT);
				out.writeUTF(string);
			}
		}
	}

	private void saveOrphans() throws IOException {
		Map<String, int[]> orphans = objectManager.getOrphanExtensions();
		Map<String, int[]> filteredOrphans = new HashMap<>();
		for (Entry<String, int[]> entry : orphans.entrySet()) {
			int[] filteredValue = filter(entry.getValue());
			if (filteredValue.length != 0)
				filteredOrphans.put(entry.getKey(), filteredValue);
		}
		FileOutputStream fosOrphan = new FileOutputStream(orphansFile);
		DataOutputStream outputOrphan = new DataOutputStream(new BufferedOutputStream(fosOrphan));
		outputOrphan.writeInt(filteredOrphans.size());
		Set<Entry<String, int[]>> elements = filteredOrphans.entrySet();
		for (Entry<String, int[]> entry : elements) {
			outputOrphan.writeUTF(entry.getKey());
			saveArray(entry.getValue(), outputOrphan);
		}
		for (Entry<String, int[]> entry : elements) {
			mainOutput.writeInt(entry.getValue().length);
			saveExtensions((IExtension[]) objectManager.getHandles(entry.getValue(), RegistryObjectManager.EXTENSION), mainOutput);
		}
		outputOrphan.flush();
		fosOrphan.getFD().sync();
		outputOrphan.close();
	}

	private void log(Status status) {
		registry.log(status);
	}

	// Filters out registry objects that should not be cached
	private int[] filter(int[] input) {
		boolean[] save = new boolean[input.length];
		int resultSize = 0;
		for (int i = 0; i < input.length; i++) {
			if (objectManager.shouldPersist(input[i])) {
				save[i] = true;
				resultSize++;
			} else
				save[i] = false;
		}
		int[] result = new int[resultSize];
		int pos = 0;
		for (int i = 0; i < input.length; i++) {
			if (save[i]) {
				result[pos] = input[i];
				pos++;
			}
		}
		return result;
	}
}
