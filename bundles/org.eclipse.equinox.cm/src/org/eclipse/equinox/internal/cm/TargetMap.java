/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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
package org.eclipse.equinox.internal.cm;

import java.util.*;
import org.osgi.framework.*;

public class TargetMap {

	private final Map<ServiceReference<?>, List<List<String>>> targetToQualifiedPids = new HashMap<>();
	private final Map<String, Collection<ServiceReference<?>>> qualifiedPidToTargets = new HashMap<>();

	public List<List<String>> add(ServiceReference<?> ref) {
		List<String> specifiedPids = getPids(ref.getProperty(Constants.SERVICE_PID));
		if (specifiedPids.isEmpty()) {
			return Collections.EMPTY_LIST;
		}

		Bundle b = ref.getBundle();
		String bsn = b.getSymbolicName();
		if (bsn == null) {
			bsn = ""; //$NON-NLS-1$
		}
		Version v = b.getVersion();
		String version = v == null ? "" : v.toString(); //$NON-NLS-1$
		String location = ConfigurationAdminImpl.getLocation(b);

		List<List<String>> result = new ArrayList<>(specifiedPids.size());
		for (String specifiedPid : specifiedPids) {
			getTargetsInternal(specifiedPid).add(ref);

			StringBuilder sb = new StringBuilder(specifiedPid);
			sb.append('|').append(bsn);
			String pidBsn = sb.toString();
			getTargetsInternal(pidBsn).add(ref);

			sb.append('|').append(version);
			String pidBsnVersion = sb.toString();
			getTargetsInternal(pidBsnVersion).add(ref);

			sb.append('|').append(location);
			String pidBsnVersionLocation = sb.toString();
			getTargetsInternal(pidBsnVersionLocation).add(ref);

			List<String> qualifiedPIDs = new ArrayList<>(4);
			qualifiedPIDs.add(pidBsnVersionLocation);
			qualifiedPIDs.add(pidBsnVersion);
			qualifiedPIDs.add(pidBsn);
			qualifiedPIDs.add(specifiedPid);
			result.add(qualifiedPIDs);
		}

		List<List<String>> unmodifiable = Collections.unmodifiableList(result);
		targetToQualifiedPids.put(ref, unmodifiable);
		return unmodifiable;
	}

	public static List<String> getPids(Object pid) {
		List<String> specifiedPids = Collections.EMPTY_LIST;
		if (pid == null) {
			return specifiedPids;
		}

		if (pid instanceof String) {
			specifiedPids = Collections.singletonList((String) pid);
		} else if (pid instanceof Collection) {
			@SuppressWarnings("unchecked")
			Collection<String> pidList = (Collection<String>) pid;
			specifiedPids = new ArrayList<>(pidList);
		} else if (pid.getClass().isArray()) {
			specifiedPids = new ArrayList<>(Arrays.asList((String[]) pid));
		}
		return specifiedPids;
	}

	public void remove(ServiceReference<?> ref) {
		List<List<String>> qualifiedPidLists = targetToQualifiedPids.remove(ref);
		if (qualifiedPidLists != null) {
			for (List<String> qualifiedPids : qualifiedPidLists) {
				for (String qualifiedPid : qualifiedPids) {
					Collection<ServiceReference<?>> targets = qualifiedPidToTargets.get(qualifiedPid);
					if (targets != null) {
						targets.remove(ref);
					}
				}
			}
		}
	}

	private Collection<ServiceReference<?>> getTargetsInternal(String pid) {
		Collection<ServiceReference<?>> targets = qualifiedPidToTargets.get(pid);
		if (targets == null) {
			targets = new ArrayList<>(1);
			qualifiedPidToTargets.put(pid, targets);
		}
		return targets;
	}

	List<ServiceReference<?>> getTargets(String qualifiedPid) {
		Collection<ServiceReference<?>> targets = qualifiedPidToTargets.get(qualifiedPid);
		return targets == null ? Collections.EMPTY_LIST : new ArrayList<>(targets);
	}

	List<List<String>> getQualifiedPids(ServiceReference<?> ref) {
		List<List<String>> pids = targetToQualifiedPids.get(ref);
		return pids == null ? Collections.EMPTY_LIST : pids;
	}
}
