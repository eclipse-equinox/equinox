/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.resolver;

import java.util.Collection;
import java.util.Dictionary;
import org.eclipse.osgi.framework.internal.core.AliasMapper;
import org.eclipse.osgi.framework.internal.core.Constants;
import org.eclipse.osgi.service.resolver.*;
import org.osgi.framework.Filter;
import org.osgi.framework.Version;

public class NativeCodeSpecificationImpl extends VersionConstraintImpl implements NativeCodeSpecification {
	private static final NativeCodeDescription[] EMPTY_NATIVECODEDESCRIPTIONS = new NativeCodeDescription[0];
	private static AliasMapper aliasMapper = new AliasMapper();
	private NativeCodeDescription[] possibleSuppliers;
	private boolean optional;

	public NativeCodeDescription[] getPossibleSuppliers() {
		if (possibleSuppliers == null)
			return EMPTY_NATIVECODEDESCRIPTIONS;
		return possibleSuppliers;
	}

	void setPossibleSuppliers(NativeCodeDescription[] possibleSuppliers) {
		this.possibleSuppliers = possibleSuppliers;
	}

	public boolean isOptional() {
		return optional;
	}

	void setOptional(boolean optional) {
		this.optional = optional;
	}

	public boolean isSatisfiedBy(BaseDescription supplier) {
		if (!(supplier instanceof NativeCodeDescription))
			return false;
		State containingState = getBundle().getContainingState();
		if (containingState == null)
			return false;
		Dictionary[] platformProps = containingState.getPlatformProperties();
		NativeCodeDescription nativeSupplier = (NativeCodeDescription) supplier;
		Filter filter = nativeSupplier.getFilter();
		boolean match = false;
		for (int i = 0; i < platformProps.length && !match; i++) {
			if (filter != null && !filter.matchCase(platformProps[i]))
				continue;
			String[] osNames = nativeSupplier.getOSNames();
			if (osNames.length == 0)
				match = true;
			else {
				String platformOS = (String) platformProps[i].get(Constants.FRAMEWORK_OS_NAME);
				Object aliasedPlatformOS = platformOS == null ? null : aliasMapper.aliasOSName(platformOS);
				String[] platformOSes;
				if (aliasedPlatformOS instanceof Collection)
					platformOSes = (String[]) ((Collection) aliasedPlatformOS).toArray(new String[((Collection) aliasedPlatformOS).size()]);
				else
					platformOSes = aliasedPlatformOS == null ? new String[0] : new String[] {(String) aliasedPlatformOS};
				for (int j = 0; j < osNames.length && !match; j++) {
					Object aliasedName = aliasMapper.aliasOSName(osNames[j]);
					for (int k = 0; k < platformOSes.length; k++) {
						if (aliasedName instanceof String) {
							if (aliasedName.equals(platformOSes[k]))
								match = true;
						} else if (((Collection) aliasedName).contains(platformOSes[k])) {
							match = true;
						}
					}
				}
			}
			if (!match)
				continue;
			match = false;

			String[] processors = nativeSupplier.getProcessors();
			if (processors.length == 0)
				match = true;
			else {
				String platformProcessor = (String) platformProps[i].get(Constants.FRAMEWORK_PROCESSOR);
				for (int j = 0; j < processors.length && !match; j++) {
					String aliasedProcessor = aliasMapper.aliasProcessor(processors[j]);
					if (aliasedProcessor.equals(platformProcessor))
						match = true;
				}
			}
			if (!match)
				return false;
			match = false;

			String[] languages = nativeSupplier.getLanguages();
			if (languages.length == 0l)
				match = true;
			else {
				String platformLanguage = (String) platformProps[i].get(Constants.FRAMEWORK_LANGUAGE);
				for (int j = 0; j < languages.length && !match; j++) {
					if (languages[j].equalsIgnoreCase(platformLanguage))
						match = true;
				}
			}
			if (!match)
				return false;
			match = false;

			VersionRange[] osVersions = nativeSupplier.getOSVersions();
			if (osVersions.length == 0 || platformProps[i].get(Constants.FRAMEWORK_OS_VERSION) == null)
				match = true;
			else {
				Version osversion;
				try {
					osversion = Version.parseVersion((String) platformProps[i].get(Constants.FRAMEWORK_OS_VERSION));
				} catch (Exception e) {
					osversion = Version.emptyVersion;
				}
				for (int j = 0; j < osVersions.length && !match; j++) {
					if (osVersions[j].isIncluded(osversion))
						match = true;
				}
			}
		}
		return match;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		NativeCodeDescription[] suppliers = getPossibleSuppliers();
		for (int i = 0; i < suppliers.length; i++) {
			if (i > 0)
				sb.append(", "); //$NON-NLS-1$
			sb.append(suppliers[i].toString());
		}

		return sb.toString();
	}
}
