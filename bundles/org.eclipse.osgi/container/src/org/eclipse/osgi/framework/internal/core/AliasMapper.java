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
package org.eclipse.osgi.framework.internal.core;

import java.io.*;
import java.util.*;
import org.eclipse.osgi.framework.debug.Debug;

/**
 * This class maps aliases.
 */
public class AliasMapper {
	private static Map<String, Object> processorAliasTable;
	private static Map<String, Object> osnameAliasTable;

	// Safe lazy initialization
	private static synchronized Map<String, Object> getProcessorAliasTable() {
		if (processorAliasTable == null) {
			InputStream in = AliasMapper.class.getResourceAsStream(Constants.OSGI_PROCESSOR_ALIASES);
			if (in != null) {
				try {
					processorAliasTable = initAliases(in);
				} finally {
					try {
						in.close();
					} catch (IOException ee) {
						// nothing
					}
				}
			}
		}
		return processorAliasTable;
	}

	// Safe lazy initialization
	private static synchronized Map<String, Object> getOSNameAliasTable() {
		if (osnameAliasTable == null) {
			InputStream in = AliasMapper.class.getResourceAsStream(Constants.OSGI_OSNAME_ALIASES);
			if (in != null) {
				try {
					osnameAliasTable = initAliases(in);
				} finally {
					try {
						in.close();
					} catch (IOException ee) {
						// nothing
					}
				}
			}
		}
		return osnameAliasTable;
	}

	/**
	 * Return the master alias for the processor.
	 *
	 * @param processor Input name
	 * @return aliased name (if any)
	 */
	public String aliasProcessor(String processor) {
		processor = processor.toLowerCase();
		Map<String, Object> aliases = getProcessorAliasTable();
		if (aliases != null) {
			String alias = (String) aliases.get(processor);
			if (alias != null) {
				processor = alias;
			}
		}
		return processor;
	}

	/**
	 * Return the master alias for the osname.
	 *
	 * @param osname Input name
	 * @return aliased name (if any)
	 */
	public Object aliasOSName(String osname) {
		osname = osname.toLowerCase();
		Map<String, Object> aliases = getOSNameAliasTable();
		if (aliases != null) {
			Object aliasObject = aliases.get(osname);
			//String alias = (String) osnameAliasTable.get(osname);
			if (aliasObject != null)
				if (aliasObject instanceof String) {
					osname = (String) aliasObject;
				} else {
					return aliasObject;
				}
		}
		return osname;
	}

	/**
	 * Read alias data and populate a Map.
	 *
	 * @param in InputStream from which to read alias data.
	 * @return Map of aliases.
	 */
	protected static Map<String, Object> initAliases(InputStream in) {
		Map<String, Object> aliases = new HashMap<String, Object>(37);
		try {
			BufferedReader br;
			try {
				br = new BufferedReader(new InputStreamReader(in, "UTF8")); //$NON-NLS-1$
			} catch (UnsupportedEncodingException e) {
				br = new BufferedReader(new InputStreamReader(in));
			}
			while (true) {
				String line = br.readLine();
				if (line == null) /* EOF */{
					break; /* done */
				}
				Tokenizer tokenizer = new Tokenizer(line);
				String master = tokenizer.getString("# \t"); //$NON-NLS-1$
				if (master != null) {
					aliases.put(master.toLowerCase(), master);
					parseloop: while (true) {
						String alias = tokenizer.getString("# \t"); //$NON-NLS-1$
						if (alias == null) {
							break parseloop;
						}
						String lowerCaseAlias = alias.toLowerCase();
						Object storedMaster = aliases.get(lowerCaseAlias);
						if (storedMaster == null) {
							aliases.put(lowerCaseAlias, master);
						} else if (storedMaster instanceof String) {
							List<String> newMaster = new ArrayList<String>();
							newMaster.add((String) storedMaster);
							newMaster.add(master);
							aliases.put(lowerCaseAlias, newMaster);
						} else {
							@SuppressWarnings("unchecked")
							List<String> newMaster = ((List<String>) storedMaster);
							newMaster.add(master);
						}
					}
				}
			}
		} catch (IOException e) {
			if (Debug.DEBUG_GENERAL) {
				Debug.printStackTrace(e);
			}
		}
		return aliases;
	}
}
