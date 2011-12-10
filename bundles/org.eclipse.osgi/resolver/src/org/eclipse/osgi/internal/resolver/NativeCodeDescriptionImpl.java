/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rob Harrop - SpringSource Inc. (bug 247522)
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.util.*;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.framework.internal.core.FilterImpl;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.*;

public class NativeCodeDescriptionImpl extends BaseDescriptionImpl implements NativeCodeDescription {
	private static final VersionRange[] EMPTY_VERSIONRANGES = new VersionRange[0];

	private volatile Filter filter;
	private String[] languages;
	private String[] nativePaths;
	private String[] osNames;
	private VersionRange[] osVersions;
	private String[] processors;
	private BundleDescription supplier;
	private volatile boolean invalidNativePaths = false;

	public Filter getFilter() {
		return filter;
	}

	public String[] getLanguages() {
		synchronized (this.monitor) {
			if (languages == null)
				return BundleDescriptionImpl.EMPTY_STRING;
			return languages;
		}
	}

	public String[] getNativePaths() {
		synchronized (this.monitor) {
			if (nativePaths == null)
				return BundleDescriptionImpl.EMPTY_STRING;
			return nativePaths;
		}
	}

	public String[] getOSNames() {
		synchronized (this.monitor) {
			if (osNames == null)
				return BundleDescriptionImpl.EMPTY_STRING;
			return osNames;
		}
	}

	public VersionRange[] getOSVersions() {
		synchronized (this.monitor) {
			if (osVersions == null)
				return EMPTY_VERSIONRANGES;
			return osVersions;
		}
	}

	public String[] getProcessors() {
		synchronized (this.monitor) {
			if (processors == null)
				return BundleDescriptionImpl.EMPTY_STRING;
			return processors;
		}
	}

	public BundleDescription getSupplier() {
		return supplier;
	}

	public int compareTo(NativeCodeDescription otherDesc) {
		State containingState = getSupplier().getContainingState();
		if (containingState == null)
			return 0;
		Dictionary<Object, Object>[] platformProps = containingState.getPlatformProperties();
		Version osversion;
		try {
			osversion = Version.parseVersion((String) platformProps[0].get(Constants.FRAMEWORK_OS_VERSION));
		} catch (Exception e) {
			osversion = Version.emptyVersion;
		}
		VersionRange[] thisRanges = getOSVersions();
		VersionRange[] otherRanges = otherDesc.getOSVersions();
		Version thisHighest = getHighestVersionMatch(osversion, thisRanges);
		Version otherHighest = getHighestVersionMatch(osversion, otherRanges);
		if (thisHighest.compareTo(otherHighest) < 0)
			return -1;
		return (getLanguages().length == 0 ? 0 : 1) - (otherDesc.getLanguages().length == 0 ? 0 : 1);
	}

	public boolean hasInvalidNativePaths() {
		return invalidNativePaths;
	}

	private Version getHighestVersionMatch(Version version, VersionRange[] ranges) {
		Version highest = Version.emptyVersion;
		for (int i = 0; i < ranges.length; i++) {
			if (ranges[i].isIncluded(version) && highest.compareTo(ranges[i].getMinimum()) < 0)
				highest = ranges[i].getMinimum();
		}
		return highest;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		String[] paths = getNativePaths();
		for (int i = 0; i < paths.length; i++) {
			if (i > 0) {
				sb.append("; "); //$NON-NLS-1$
			}
			sb.append(paths[i]);
		}

		String[] procs = getProcessors();
		for (int i = 0; i < procs.length; i++) {
			sb.append("; "); //$NON-NLS-1$
			sb.append(Constants.BUNDLE_NATIVECODE_PROCESSOR);
			sb.append('=');
			sb.append(procs[i]);
		}

		String[] oses = getOSNames();
		for (int i = 0; i < oses.length; i++) {
			sb.append("; "); //$NON-NLS-1$
			sb.append(Constants.BUNDLE_NATIVECODE_OSNAME);
			sb.append('=');
			sb.append(oses[i]);
		}

		VersionRange[] osRanges = getOSVersions();
		for (int i = 0; i < osRanges.length; i++) {
			sb.append("; "); //$NON-NLS-1$
			sb.append(Constants.BUNDLE_NATIVECODE_OSVERSION);
			sb.append("=\""); //$NON-NLS-1$
			sb.append(osRanges[i].toString());
			sb.append('"');
		}

		String[] langs = getLanguages();
		for (int i = 0; i < langs.length; i++) {
			sb.append("; "); //$NON-NLS-1$
			sb.append(Constants.BUNDLE_NATIVECODE_LANGUAGE);
			sb.append('=');
			sb.append(langs[i]);
		}

		Filter f = getFilter();
		if (f != null) {
			sb.append("; "); //$NON-NLS-1$
			sb.append(Constants.SELECTION_FILTER_ATTRIBUTE);
			sb.append("=\""); //$NON-NLS-1$
			sb.append(f.toString());
			sb.append('"');
		}
		return (sb.toString());
	}

	void setInvalidNativePaths(boolean invalidNativePaths) {
		this.invalidNativePaths = invalidNativePaths;
	}

	void setOSNames(String[] osNames) {
		synchronized (this.monitor) {
			this.osNames = osNames;
		}
	}

	void setOSVersions(VersionRange[] osVersions) {
		synchronized (this.monitor) {
			this.osVersions = osVersions;
		}
	}

	void setFilter(String filter) throws InvalidSyntaxException {
		this.filter = filter == null ? null : FilterImpl.newInstance(filter);
	}

	void setLanguages(String[] languages) {
		synchronized (this.monitor) {
			this.languages = languages;
		}
	}

	void setNativePaths(String[] nativePaths) {
		synchronized (this.monitor) {
			this.nativePaths = nativePaths;
		}
	}

	void setProcessors(String[] processors) {
		synchronized (this.monitor) {
			this.processors = processors;
		}
	}

	void setSupplier(BundleDescription supplier) {
		this.supplier = supplier;
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getDeclaredDirectives() {
		return Collections.EMPTY_MAP;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getDeclaredAttributes() {
		return Collections.EMPTY_MAP;
	}
}
