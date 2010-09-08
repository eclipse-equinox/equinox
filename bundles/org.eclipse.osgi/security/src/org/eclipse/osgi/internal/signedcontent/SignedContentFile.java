/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.signedcontent;

import java.io.IOException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.*;
import org.eclipse.osgi.signedcontent.*;

/*
 * This class is used by the SignedContentFactory to create SignedContent objects from File objects.  This is needed 
 * to avoid leaving the underlying ZipFiles open for the SignedContent objects returned from the 
 * SignedContentFactory (bug 225090) 
 */
public class SignedContentFile implements SignedContent {

	private final SignedContentImpl signedContent;
	// a cache of verification exceptions
	private Map<String, Throwable> entryExceptions = null;

	public SignedContentFile(SignedContentImpl signedContent) {
		try {
			signedContent.content.close();
		} catch (IOException e) {
			// do nothing
		}
		this.signedContent = signedContent;
	}

	public void checkValidity(SignerInfo signerInfo) throws CertificateExpiredException, CertificateNotYetValidException {
		signedContent.checkValidity(signerInfo);
	}

	public synchronized SignedContentEntry[] getSignedEntries() {
		SignedContentEntry[] entries = signedContent.getSignedEntries();
		if (signedContent == null)
			return null;
		SignedContentEntry[] results = new SignedContentEntry[entries.length];
		Map<String, Throwable> exceptions = getEntryExceptions(true);
		for (int i = 0; i < entries.length; i++) {
			try {
				entries[i].verify();
			} catch (Throwable t) {
				exceptions.put(entries[i].getName(), t);
			}
			results[i] = new SignedContentFileEntry(entries[i]);
		}
		try {
			// ensure the content is closed after caching the exceptions
			signedContent.content.close();
		} catch (IOException e) {
			// do nothing
		}
		return results;
	}

	public synchronized SignedContentEntry getSignedEntry(String name) {
		if (getEntryExceptions(false) == null)
			getSignedEntries(); // populate the entry exceptions
		SignedContentEntry entry = signedContent.getSignedEntry(name);
		return entry == null ? null : new SignedContentFileEntry(entry);
	}

	public SignerInfo[] getSignerInfos() {
		return signedContent.getSignerInfos();
	}

	public Date getSigningTime(SignerInfo signerInfo) {
		return signedContent.getSigningTime(signerInfo);
	}

	public SignerInfo getTSASignerInfo(SignerInfo signerInfo) {
		return signedContent.getTSASignerInfo(signerInfo);
	}

	public boolean isSigned() {
		return signedContent.isSigned();
	}

	synchronized Map<String, Throwable> getEntryExceptions(boolean create) {
		if (create && entryExceptions == null)
			entryExceptions = new HashMap<String, Throwable>(5);
		return entryExceptions;
	}

	public class SignedContentFileEntry implements SignedContentEntry {
		private final SignedContentEntry entry;

		public SignedContentFileEntry(SignedContentEntry entry) {
			this.entry = entry;
		}

		public String getName() {
			return entry.getName();
		}

		public SignerInfo[] getSignerInfos() {
			return entry.getSignerInfos();
		}

		public boolean isSigned() {
			return entry.isSigned();
		}

		public void verify() throws IOException, InvalidContentException {
			// check the entry exceptions map for the entry name
			Map<String, Throwable> exceptions = getEntryExceptions(false);
			Throwable t = exceptions == null ? null : (Throwable) exceptions.get(entry.getName());
			if (t == null)
				return;
			if (t instanceof IOException)
				throw (IOException) t;
			if (t instanceof InvalidContentException)
				throw (InvalidContentException) t;
			if (t instanceof Error)
				throw (Error) t;
			if (t instanceof RuntimeException)
				throw (RuntimeException) t;
		}

	}
}
