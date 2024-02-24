package org.apache.felix.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.osgi.container.ModuleContainer;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

/**
 * The idea of the {@link ProblemReduction} class is to strike out
 * {@link Capability}s that might satisfy {@link Requirement}s but violates some
 * contracts that would lead to a guaranteed unresolvable state, for example
 * <ul>
 * <li>When a package is substituted, the export of this bundle is discarded. If
 * there is a consumer for this package that has no other alternatives this will
 * not resolve and the substitution needs not to be considered.</li>
 * </ul>
 */
class ProblemReduction {

	private static final Capability[] EMPTY_CAPABILITIES = new Capability[0];

	private static final boolean DEBUG_SUBSTITUTE = false;

	private static final boolean DEBUG_VIOLATES = false;

	private static final boolean DEBUG_SINGLE = false;

	private static final boolean DEBUG_IMPLIED = true;

	public static void discardSubstitutionPackages(Candidates candidates, List<Resource> resources) {
		for (Resource resource : resources) {

			// Check if the resource has any IMPORT package requirements
			List<Requirement> requirements = resource.getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
			if (!requirements.isEmpty()) {
				for (Requirement requirement : requirements) {
					Optional<Capability> substitutionPackage = candidates.getSubstitutionPackage(requirement);
					substitutionPackage.ifPresent(self -> {
						Capability[] toRemove = candidates.getCandidates(requirement).stream().filter(c -> c != self)
								.toArray(Capability[]::new);
						for (Capability remove : toRemove) {
							System.out.println("remove " + remove);
							candidates.removeCandidate(requirement, remove);
						}
					});
				}
			}
		}
	}

	/**
	 * Reduces the candidates for any resource that requires a bundle that has a
	 * reexport what matches a required bundle of the resource. Assume we have
	 * <ul>
	 * <li>A that requires R in any version, and there is version 1, 2 and 3 of
	 * R</li>
	 * <li>A also requires B that requires R in version 1 and reexport this
	 * dependency</li>
	 * </ul>
	 * now it follows that A is actually restricted to only use R in version 1 as
	 * every other choice will lead to a package constraint violation as the
	 * packages of R are reachable via two dependency chains (e.g. trough A > R.v3
	 * and A > B > R.v1).
	 * 
	 * @param candidates
	 * @param resources
	 */
	public static void removeImpliedDependencies(Candidates candidates, List<Resource> resources) {
		boolean restart;
		int round = 0;
		do {
			restart = false;
			if (DEBUG_IMPLIED) {
				System.out.println("====== REMOVE IMPLIED DEPENDENCIES: Round " + (++round) + " =======");
			}
			for (Resource resource : resources) {
				List<Requirement> requiredBundles = resource.getRequirements(BundleNamespace.BUNDLE_NAMESPACE);
				if (requiredBundles.isEmpty()) {
					continue;
				}
				if (DEBUG_IMPLIED) {
					System.out.println("=== Filter implied bundles " + Util.getResourceName(resource));
					candidates.dumpResource(resource, null, false, System.out);
				}
				// First collect any implied bundles
				Map<Requirement, Set<Capability>> impliedBundleMap = new HashMap<>();
				for (Requirement bundle : requiredBundles) {
					Set<Capability> impliedBundles = candidates.getImpliedBundles(bundle);
					if (impliedBundles.isEmpty()) {
						continue;
					}
					impliedBundleMap.put(bundle, impliedBundles);
					if (DEBUG_IMPLIED) {
						System.out.println(ModuleContainer.toString(bundle) + " implies:");
						for (Capability capability : impliedBundles) {
							System.out.println("   " + Util.getResourceName(capability.getResource()) + ": "
									+ ModuleContainer.toString(capability));
						}
					}
				}
				// Now look for any candidates for a requirement in this bundles is conflicting
				// with any implied bundle
				next: for (Requirement bundle : requiredBundles) {
					List<Capability> bundleCandidates = candidates.getCandidatesList(bundle);
					if (bundleCandidates.size() > 1) {
						for (Capability candidate : bundleCandidates) {
							for (Entry<Requirement, Set<Capability>> entry : impliedBundleMap.entrySet()) {
								for (Capability implied : entry.getValue()) {
									if (implied == candidate) {
										// we have a match...
										if (DEBUG_IMPLIED) {
											Requirement implication = entry.getKey();
											System.out.println("The provider for " + ModuleContainer.toString(bundle)
													+ " is selected as \n\t"
													+ Util.getResourceName(implied.getResource())
													+ "\nbecause it is implied by a reexported dependency of \n\t"
													+ ModuleContainer.toString(implication));
										}
										for (Capability c : bundleCandidates.toArray(EMPTY_CAPABILITIES)) {
											if (c != implied) {
												candidates.removeCandidate(bundle, c);
											}
										}
										restart = true;
										continue next;
									}
								}
							}
						}

					}
				}
			}
		} while (restart);
	}

	public static void removeSubstitutions(Candidates candidates, List<Resource> resources) {
		for (Resource resource : resources) {

			// Check if the resource has any IMPORT package requirements
			List<Requirement> requirements = resource.getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
			if (!requirements.isEmpty()) {
				if (DEBUG_SUBSTITUTE && hasSubstitution(candidates, requirements, resource)) {
					System.out.println(
							"=== Filter possible substitution candidates for " + Util.getResourceName(resource));
				}
				// Now check if this is possibly a substitution package...
				Map<Resource, Set<String>> usesMap = new HashMap<>();
				for (Requirement requirement : requirements) {
					List<Capability> remainingCandidates = candidates.getCandidates(requirement);
					if (remainingCandidates == null || remainingCandidates.isEmpty()
							|| remainingCandidates.size() == 1) {
						// means either only one provider or no providers at all so nothing to check in
						// this stage
						continue;
					}
					// now find out if we have a self export here in the list of candidates ...
					Optional<Capability> selfExport = remainingCandidates.stream()
							.filter(cap -> Objects.equals(cap.getResource(), resource)).findFirst();
					if (selfExport.isPresent()) {
						// if yes we have a substitution package!
						Capability exportPackage = selfExport.get();
						Collection<Requirement> dependent = candidates.getDependent(exportPackage);
						if (dependent != null && !dependent.isEmpty()) {
							remainingCandidates = new ArrayList<>(remainingCandidates);
							// there is someone who depends potentially on our package export
							remainingCandidates.remove(exportPackage);
							for (Requirement depreq : dependent) {
								Capability moreMatching = hasMoreMatching(remainingCandidates, depreq);
								if (moreMatching == null) {
									// this means we are the only provider for the requirement, remove all other
									// providers so this package gets NOT substituted anymore
									for (Capability capability : remainingCandidates) {
										if (DEBUG_SUBSTITUTE) {
											System.out.println(Util.getResourceName(capability.getResource())
													+ " must be removed as a provider of "
													+ ModuleContainer.toString(requirement) + " because it would make "
													+ Util.getResourceName(depreq.getResource())
													+ " impossible to resolve as the requirement "
													+ ModuleContainer.toString(depreq) + " does not match "
													+ ModuleContainer.toString(capability));
										}
										removeSubstitutionCandidate(candidates, requirement, capability, usesMap);
									}
								}
							}
						}
					}
				}
				if (!usesMap.isEmpty()) {
					// next we need to iteratively remove all conflicting providers a conflict is if
					// we have removed a capability that uses as package it possibly can provide as
					// a substitution
					boolean removed;
					do {
						removed = false;
						for (Entry<Resource, Set<String>> entry : usesMap.entrySet()) {
							Resource removedResource = entry.getKey();
							Set<String> usedPackages = entry.getValue();
							if (DEBUG_SUBSTITUTE) {
								System.out.println(Util.getResourceName(removedResource) + " was removed and uses: ");
								for (String usedPackage : usedPackages) {
									System.out.println("\t" + usedPackage);
								}
							}
							for (Requirement requirement : requirements) {
								List<Capability> remainingCandidates = candidates.getCandidates(requirement);
								if (remainingCandidates == null || remainingCandidates.isEmpty()) {
									// this time we also need consider single provider candidates
									continue;
								}
								for (Capability candidate : remainingCandidates.toArray(EMPTY_CAPABILITIES)) {
									if (candidate.getResource() == removedResource) {
										String pkg = Util.getPackageName(candidate);
										if (usedPackages.contains(pkg)) {
											if (DEBUG_SUBSTITUTE) {
												System.out.println("Resource must be removed as a provider of "
														+ ModuleContainer.toString(requirement)
														+ " because is uses the package " + pkg
														+ " what is not provided anymore: "
														+ ModuleContainer.toString(candidate));
											}
											removed |= removeSubstitutionCandidate(candidates, requirement, candidate,
													usesMap);
										}
									}
								}
							}
						}
						System.out.println();
					} while (removed);
				}
			}
		}

	}

	private static boolean hasSubstitution(Candidates candidates, List<Requirement> requirements, Resource resource) {
		for (Requirement requirement : requirements) {
			List<Capability> remainingCandidates = candidates.getCandidates(requirement);
			if (remainingCandidates == null || remainingCandidates.isEmpty() || remainingCandidates.size() == 1) {
				continue;
			}
			Optional<Capability> selfExport = remainingCandidates.stream()
					.filter(cap -> Objects.equals(cap.getResource(), resource)).findFirst();
			if (selfExport.isPresent()) {
				return true;
			}
		}
		return false;
	}

	private static boolean removeSubstitutionCandidate(Candidates candidates, Requirement requirement,
			Capability capability, Map<Resource, Set<String>> usesMap) {
		candidates.removeCandidate(requirement, capability);
		// everything that this uses must also be removed in a second pass
		String uses = capability.getDirectives().get(PackageNamespace.CAPABILITY_USES_DIRECTIVE);
		if (uses == null) {
			// nothing to do...
			return false;
		}
		boolean added = false;
		if (uses != null) {
			String[] split = uses.split(",");
			for (String usedPackage : split) {
				added |= usesMap.computeIfAbsent(capability.getResource(), x -> new HashSet<>())
						.add(usedPackage.trim());
			}
		}
		return added;
	}

	private static Capability hasMoreMatching(List<Capability> remainingCandidates, Requirement depreq) {
		for (Capability candidate : remainingCandidates) {
			if (Util.matches(candidate, depreq)) {
				return candidate;
			}
		}
		return null;
	}

	public static void removeUsesViolations(Candidates candidates, Capability removedCapability,
			Requirement requirement) {
		// TODO more generally one can say that if a provider is chosen *and* there is
		// an alternative this alternative is only valid if all other it uses are also
		// from that domain
		// TODO actually one needs to consider:
		// - packages exported by required bundles
		// - reexported dependencies
		if (removedCapability != null) {
			Set<String> uses = new TreeSet<>(Util.getUses(removedCapability));
			if (uses.isEmpty()) {
				return;
			}
			Resource requiredResource = requirement.getResource();
			boolean removed = false;
			if (DEBUG_VIOLATES) {
				System.out.println("=== remove uses violations for " + ModuleContainer.toString(requirement));
				candidates.dumpResource(requiredResource, null, false, System.out);
			}
			Resource candidateResource = removedCapability.getResource();
			boolean repeat;
			int round = 0;
			do {
				repeat = false;
				round++;
				if (DEBUG_VIOLATES) {
					System.out.println("Round " + round + ":");
					for (String usedPackage : uses) {
						System.out.println(" uses: " + usedPackage);
					}
				}
				for (Requirement otherRequirement : requiredResource
						.getRequirements(PackageNamespace.PACKAGE_NAMESPACE)) {
					if (otherRequirement == requirement) {
						continue;
					}
					List<Capability> providers = candidates.getCandidates(otherRequirement);
					if (candidates != null) {
						for (Capability capability : providers.toArray(EMPTY_CAPABILITIES)) {
							if (capability.getResource() == candidateResource) {
								String packageName = Util.getPackageName(capability);
								if (uses.contains(packageName)) {
									if (DEBUG_VIOLATES) {
										// This means we have a package that is used by the just removed capability, if
										// it is provided by the removed resource, we must remove this as well as
										// otherwise there is a use constraint violation
										System.out.println(" Resource " + Util.getResourceName(candidateResource)
												+ " must be removed as a provider for "
												+ ModuleContainer.toString(otherRequirement) + " because the package "
												+ packageName + " is used by the removed capability "
												+ ModuleContainer.toString(removedCapability) + ": "
												+ ModuleContainer.toString(capability));
										removed = true;
										candidates.removeCandidate(otherRequirement, capability);
										repeat |= uses.addAll(Util.getUses(capability));
									}
								}
							}
						}
					}
				}
			} while (repeat);
			if (DEBUG_VIOLATES && removed) {
				System.out.println();
				System.out.println("After removal");
				candidates.dumpResource(requiredResource, null, false, System.out);
				System.out.println();
			}
		}

	}

	public static void removeUsesViolationsForSingletons(Candidates candidates, Resource requiredResource) {
		if (DEBUG_SINGLE) {
			System.out.println("=== remove singleton uses violations for ");
			candidates.dumpResource(requiredResource, null, false, System.out);
		}
		Map<String, Capability> singeltonPackageProvider = new HashMap<>();
		Map<Requirement, Capability[]> multiplePackageProvider = new HashMap<>();
		for (Requirement requirement : requiredResource.getRequirements(PackageNamespace.PACKAGE_NAMESPACE)) {
			List<Capability> list = candidates.getCandidates(requirement);
			if (list == null || list.isEmpty()) {
				continue;
			}
			if (list.size() == 1) {
				// a single provided capability for a package
				Capability candidate = list.get(0);
				singeltonPackageProvider.put(Util.getPackageName(candidate), candidate);
			} else {
				// multiple providers we need to check later
				multiplePackageProvider.put(requirement, list.toArray(EMPTY_CAPABILITIES));
			}
		}
		if (singeltonPackageProvider.isEmpty() || multiplePackageProvider.isEmpty()) {
			// nothing to check
			return;
		}
		int round = 0;
		do {
			round++;
			if (DEBUG_SINGLE) {
				System.out.println("-- Round " + round + ":");
			}

		} while (removeOneProvider(candidates, singeltonPackageProvider, multiplePackageProvider));
		if (DEBUG_SINGLE) {
			System.out.println("Results after reduction ");
			candidates.dumpResource(requiredResource, null, false, System.out);
		}
	}

	protected static boolean removeOneProvider(Candidates candidates, Map<String, Capability> singeltonPackageProvider,
			Map<Requirement, Capability[]> multiplePackageProvider) {
		if (DEBUG_SINGLE) {
			System.out.println("Following packages have single providers");
			for (String pkg : singeltonPackageProvider.keySet()) {
				System.out.println("   " + pkg);
			}
			System.out.println("Following requirements have multiple providers");
			for (Entry<Requirement, Capability[]> entry : multiplePackageProvider.entrySet()) {
				System.out.println("    " + ModuleContainer.toString(entry.getKey()));
				for (Capability capability : entry.getValue()) {
					System.out.println("        " + ModuleContainer.toString(capability));
				}
			}
		}
		for (Entry<Requirement, Capability[]> entry : multiplePackageProvider.entrySet()) {
			Requirement requirement = entry.getKey();
			Capability[] capabilities = entry.getValue();
			for (Capability capability : capabilities) {
				Set<String> uses = Util.getUses(capability);
				if (uses.isEmpty()) {
					continue;
				}
				for (String usedPackage : uses) {
					Capability singletonCapability = singeltonPackageProvider.get(usedPackage);
					if (singletonCapability != null) {
						// check if the singleton is provided by one resource that also provides this
						// package if yes ...
						if (isProvidedBy(singletonCapability, capabilities)) {
							// the resources must match or we have a use constraint violation
							Resource currentResource = capability.getResource();
							Resource providingResource = singletonCapability.getResource();
							if (providingResource != currentResource) {
								if (DEBUG_SINGLE) {
									System.out.println("Resource " + Util.getResourceName(currentResource)
											+ " must be removed as a provider for "
											+ ModuleContainer.toString(requirement) + " because "
											+ ModuleContainer.toString(capability) + " uses package " + usedPackage
											+ " that is already provided by "
											+ Util.getResourceName(providingResource));
								}
								candidates.removeCandidate(requirement, capability);
								List<Capability> remaining = candidates.getCandidates(requirement);
								if (remaining == null || remaining.isEmpty()) {
									// ???
									multiplePackageProvider.remove(requirement);
								} else if (remaining.size() == 1) {
									// this now has become a single provider...
									Capability newSingelton = remaining.get(0);
									singeltonPackageProvider.put(Util.getPackageName(newSingelton), newSingelton);
									multiplePackageProvider.remove(requirement);
								} else {
									// still something more left...
									multiplePackageProvider.put(requirement, remaining.toArray(EMPTY_CAPABILITIES));
								}
								// restart the round as we now have possible more to use...
								return true;
							}
						}
					}
				}
			}
		}
		// nothing changes... break out
		return false;
	}

	private static boolean isProvidedBy(Capability capability, Capability[] capabilities) {
		for (Capability other : capabilities) {
			if (other.getResource() == capability.getResource()) {
				return true;
			}
		}
		return false;
	}

}
