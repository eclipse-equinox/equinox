/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.resolver;

import java.util.List;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

/**
 * Performs a selection of substitution packages (see <a href=
 * "https://docs.osgi.org/specification/osgi.core/8.0.0/framework.module.html#framework.module-import.export.same.package">the
 * spec</a> for reference).
 * 
 * The rules are as follows for a package that is substitutable:
 * <ol>
 * <li><code>External</code> - If this resolves to an export statement in
 * <b>another bundle</b>, then the overlapping export definition <b>in this
 * bundle</b> is discarded.</li>
 * <li><code>Internal</code> - If it is resolved to an export statement in
 * <b>this bundle</b>, then the overlapping import definition <b>in this
 * bundle</b> is discarded.</li>
 * <li><code>Unresolved</code> - There is no matching export definition. In this
 * case the framework is free to discard either the overlapping export
 * definition or overlapping import definition in this bundle. If the export
 * definition is discarded and the import definition is not optional then the
 * bundle will fail to resolve.</li>
 * </ol>
 */
public class SubstitutionPackages {

//	 Emergency Switch to disable the usage of the new {@link SubstitutionPackages}
//	 computation JVM wide. Will be removed when we are confident that no
//	 regressions occurs.
	public static final boolean USE_LEGACY_SUBSTITUTION_PACKAGES = Boolean
			.getBoolean("felix.resolver.substitution.legacy");

	/**
	 * Resolves all substitution packages for the given candidate set and returns a
	 * list of new permutations that has to be considered.
	 * 
	 * @return the {@link Candidates} permutation for an alternative substitution
	 *         choice or <code>null</code> otherwise
	 */
	public static Candidates resolve(Candidates candidates, Requirement requirement, Logger logger) {
		if (USE_LEGACY_SUBSTITUTION_PACKAGES || !candidates.isSubstitutionPackage(requirement)) {
			return null;
		}
		Candidates permutation = candidates.permutate(requirement);
		Capability firstCandidate = candidates.getFirstCandidate(requirement);
		if (firstCandidate.getResource().equals(requirement.getResource())) {
			// Internal case, resolved to an export statement in this bundle.
			// now drop all other alternatives
			candidates.dropOtherCandidates(requirement);
		} else {
			// External case we must drop the export package capability of our bundle when
			// there is still an export package, this might not the case if previously this
			// was already dropped!
			// FIXME handle the case where the requirement has dropped before due to use
			// constraint violations!
			// This must be handled in removeFirstCandidate already!?
			if (permutation!=null) {
				List<Capability> others = permutation.getCandidates(requirement);
				for (Capability capability : others) {
					if (capability.getResource().equals(requirement.getResource())) {
						candidates.discardExportPackageCapability(firstCandidate);
					}
				}
			}
		}
		return permutation;
	}
}
