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
		synchronized (this.monitor) {
			if (possibleSuppliers == null)
				return EMPTY_NATIVECODEDESCRIPTIONS;
			return possibleSuppliers;
		}
	}

	void setPossibleSuppliers(NativeCodeDescription[] possibleSuppliers) {
		synchronized (this.monitor) {
			this.possibleSuppliers = possibleSuppliers;
		}
	}

	public boolean isOptional() {
		synchronized (this.monitor) {
			return optional;
		}
	}

	void setOptional(boolean optional) {
		synchronized (this.monitor) {
			this.optional = optional;
		}
	}

	public boolean isSatisfiedBy(BaseDescription supplier) {
		if (!(supplier instanceof NativeCodeDescription))
			return false;
		State containingState = getBundle().getContainingState();
		if (containingState == null)
			return false;
		Dictionary<Object, Object>[] platformProps = containingState.getPlatformProperties();
		NativeCodeDescription nativeSupplier = (NativeCodeDescription) supplier;
		Filter filter = nativeSupplier.getFilter();
		boolean match = false;
		for (int i = 0; i < platformProps.length && !match; i++) {
			@SuppressWarnings("rawtypes")
			Dictionary props = platformProps[i];
			if (filter != null && !filter.matchCase(props))
				continue;
			String[] osNames = nativeSupplier.getOSNames();
			if (osNames.length == 0)
				match = true;
			else {
				Object platformOS = platformProps[i].get(Constants.FRAMEWORK_OS_NAME);
				Object aliasedPlatformOS = platformOS == null || !(platformOS instanceof String) ? platformOS : aliasMapper.aliasOSName((String) platformOS);
				Object[] platformOSes;
				if (aliasedPlatformOS instanceof Collection)
					platformOSes = ((Collection<?>) aliasedPlatformOS).toArray();
				else
					platformOSes = aliasedPlatformOS == null ? new Object[0] : new Object[] {aliasedPlatformOS};
				for (int j = 0; j < osNames.length && !match; j++) {
					Object aliasedName = aliasMapper.aliasOSName(osNames[j]);
					for (int k = 0; k < platformOSes.length; k++) {
						if (aliasedName instanceof String) {
							if (platformOSes[k].equals(aliasedName))
								match = true;
						} else {
							for (Iterator<?> iAliases = ((Collection<?>) aliasedName).iterator(); iAliases.hasNext() && !match;)
								if (platformOSes[k].equals(iAliases.next()))
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
				Object platformProcessor = platformProps[i].get(Constants.FRAMEWORK_PROCESSOR);
				Object aliasedPlatformProcessor = platformProcessor == null || !(platformProcessor instanceof String) ? platformProcessor : aliasMapper.aliasProcessor((String) platformProcessor);
				if (aliasedPlatformProcessor != null)
					for (int j = 0; j < processors.length && !match; j++) {
						String aliasedProcessor = aliasMapper.aliasProcessor(processors[j]);
						if (aliasedPlatformProcessor.equals(aliasedProcessor))
							match = true;
					}
			}
			if (!match)
				return false;
			match = false;

			String[] languages = nativeSupplier.getLanguages();
			if (languages.length == 0)
				match = true;
			else {
				Object platformLanguage = platformProps[i].get(Constants.FRAMEWORK_LANGUAGE);
				if (platformLanguage != null)
					for (int j = 0; j < languages.length && !match; j++) {
						if ((platformLanguage instanceof String) ? ((String) platformLanguage).equalsIgnoreCase(languages[j]) : platformLanguage.equals(languages[j]))
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

	@Override
	protected boolean hasMandatoryAttributes(String[] mandatory) {
		return true;
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

	@SuppressWarnings("unchecked")
	protected Map<String, String> getInternalDirectives() {
		return Collections.EMPTY_MAP;
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> getInteralAttributes() {
		return Collections.EMPTY_MAP;
	}

	@Override
	protected String getInternalNameSpace() {
		return null;
	}
}
