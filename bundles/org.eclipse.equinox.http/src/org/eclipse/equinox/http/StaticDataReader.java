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
package org.eclipse.equinox.http;

import java.io.*;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class StaticDataReader {
	protected Http http;
	protected static final String defaultMimeType = "application/octet-stream"; //$NON-NLS-1$
	protected static final String defaultMimeTable = "mime.types"; //$NON-NLS-1$
	protected static final String defaultStatusCodes = "status.codes"; //$NON-NLS-1$

	/** file extentsions (String) => MIME type (String) */
	protected Hashtable mimeTypes;

	/** status code (Integer) => status phrase (String) */
	protected Hashtable statusCodes;

	/**
	 * Construct mime table from a standard mime.types file.
	 */
	public StaticDataReader(Http http) {
		this.http = http;

		InputStream in = getClass().getResourceAsStream(defaultMimeTable);
		mimeTypes = parseMimeTypes(in);

		in = getClass().getResourceAsStream(defaultStatusCodes);
		statusCodes = parseStatusCodes(in);

	}

	/**
	 * Determine the mime type of a file based on the file extension.
	 * This method is more convenient to use than getMIMEType because
	 * takes a filename as an argument.  It is also able to discern that
	 * files which are directories, such as
	 * http://www.ibm.com/  are assumed to be HTML, rather than appOctet.
	 *
	 * @param String filename - the name of the file, which must
	 * not be null.
	 * @returns String - the mime type of the file.
	 */
	public String computeMimeType(String filename) {
		int i = filename.lastIndexOf('.');

		if (i >= 0) {
			return (getMimeType(filename.substring(i + 1)));
		}

		return (getMimeType(filename));
	}

	/**
	 * This method was created in VisualAge.
	 * @return java.lang.String
	 * @param statusCode int
	 */
	public String computeStatusPhrase(int statusCode) {
		String statusPhrase = (String) statusCodes.get(new Integer(statusCode));
		if (statusPhrase != null) {
			return (statusPhrase);
		}

		return (HttpMsg.HTTP_STATUS_CODE_NOT_FOUND);
	}

	private String getMimeType(String extension) {
		String type = (String) mimeTypes.get(extension.toLowerCase());

		if (type != null) {
			return (type);
		}

		return (defaultMimeType);
	}

	/**
	 * Parses the default MIME type table.
	 *
	 * @return Default MIME type Hashtable
	 */
	private Hashtable parseMimeTypes(InputStream in) {
		Hashtable mimeTypes = new Hashtable();

		if (in != null) {
			try {
				BufferedReader rdr = new BufferedReader(new InputStreamReader(in, "8859_1")); //$NON-NLS-1$
				while (true) {
					String line = rdr.readLine();
					if (line == null) /* EOF */
					{
						break;
					}

					if ((line.length() != 0) && (line.charAt(0) != '#')) { // skip comments and blank lines
						StringTokenizer tokens = new StringTokenizer(line);
						String type = tokens.nextToken();
						while (tokens.hasMoreTokens()) {
							String ext = tokens.nextToken();
							mimeTypes.put(ext.toLowerCase(), type);
						}
					}
				}
			} catch (Exception e) {
				http.logError(HttpMsg.HTTP_DEFAULT_MIME_TABLE_ERROR, e);
			} finally {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}

		return (mimeTypes);
	}

	/**
	 * This method was created in VisualAge.
	 */
	private Hashtable parseStatusCodes(InputStream in) {
		Hashtable statusCodes = new Hashtable();

		if (in != null) {
			try {
				BufferedReader rdr = new BufferedReader(new InputStreamReader(in, "8859_1")); //$NON-NLS-1$
				while (true) {
					String line = rdr.readLine();
					if (line == null) /* EOF */
					{
						break;
					}

					if ((line.length() != 0) && (line.charAt(0) != '#')) { // skip comments and blank lines
						int space = line.indexOf(' ');
						Integer status = new Integer(line.substring(0, space));
						String statusPhrase = line.substring(space + 1);
						statusCodes.put(status, statusPhrase);
					}
				}
			} catch (Exception e) {
				http.logError(HttpMsg.HTTP_STATUS_CODES_TABLE_ERROR, e);
			} finally {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
		}

		return (statusCodes);
	}

	/**
	 * Read alias data and populate a Hashtable.
	 * The inputstream is always closed.
	 *
	 * @param in InputStream from which to read alias data.
	 * @return Hashtable of aliases.
	 */
	private static Hashtable parseAliases(InputStream in) {
		Hashtable aliases = new Hashtable(37);

		if (in != null) {
			try {
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(in, "8859_1")); //$NON-NLS-1$

					while (true) {
						String line = br.readLine();

						if (line == null) /* EOF */
						{
							break; /* done */
						}

						Tokenizer tokenizer = new Tokenizer(line);

						String master = tokenizer.getString("#"); //$NON-NLS-1$

						if (master != null) {
							master = master.toUpperCase();

							aliases.put(master, master);

							parseloop: while (true) {
								String alias = tokenizer.getString("#"); //$NON-NLS-1$

								if (alias == null) {
									break parseloop;
								}

								aliases.put(alias.toUpperCase(), master);
							}
						}
					}
				} catch (IOException e) {
					if (Http.DEBUG) {
						e.printStackTrace();
					}
				}
			} finally {
				try {
					in.close();
				} catch (IOException ee) {
				}
			}
		}

		return (aliases);
	}

}
