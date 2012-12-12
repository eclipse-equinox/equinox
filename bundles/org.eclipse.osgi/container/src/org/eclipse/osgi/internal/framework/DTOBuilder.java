/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.internal.framework;

import java.lang.reflect.Array;
import java.util.*;
import org.osgi.dto.DTO;
import org.osgi.dto.framework.*;
import org.osgi.dto.framework.startlevel.BundleStartLevelDTO;
import org.osgi.dto.framework.startlevel.FrameworkStartLevelDTO;
import org.osgi.dto.framework.wiring.*;
import org.osgi.dto.resource.*;
import org.osgi.framework.*;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.*;

public class DTOBuilder {
	private final Map<Object, Object> objects;

	private DTOBuilder() {
		objects = new IdentityHashMap<Object, Object>();
	}

	public static BundleDTO newBundleDTO(Bundle bundle) {
		return new DTOBuilder().getBundleDTO(bundle);
	}

	private BundleDTO getBundleDTO(Bundle bundle) {
		if (bundle == null) {
			return null;
		}
		BundleDTO dto = (BundleDTO) objects.get(bundle);
		if (dto != null) {
			return dto;
		}
		dto = new BundleDTO();
		objects.put(bundle, dto);
		dto.id = bundle.getBundleId();
		dto.lastModified = bundle.getLastModified();
		dto.state = bundle.getState();
		dto.symbolicName = bundle.getSymbolicName();
		dto.version = bundle.getVersion().toString();
		return dto;
	}

	public static BundleStartLevelDTO newBundleStartLevelDTO(BundleStartLevel bsl) {
		if (bsl == null) {
			return null;
		}
		BundleStartLevelDTO dto = new BundleStartLevelDTO();
		dto.activationPolicyUsed = bsl.isActivationPolicyUsed();
		dto.persistentlyStarted = bsl.isPersistentlyStarted();
		dto.startLevel = bsl.getStartLevel();
		return dto;
	}

	public static BundleRevisionDTO newBundleRevisionDTO(BundleRevision revision) {
		return new DTOBuilder().getBundleRevisionDTO(revision);
	}

	private BundleRevisionDTO getBundleRevisionDTO(BundleRevision revision) {
		if (revision == null) {
			return null;
		}
		BundleRevisionDTO dto = (BundleRevisionDTO) objects.get(revision);
		if (dto != null) {
			return dto;
		}
		dto = new BundleRevisionDTO();
		objects.put(revision, dto);
		dto.bundle = getBundleDTO(revision.getBundle());
		dto.symbolicName = revision.getSymbolicName();
		dto.type = revision.getTypes();
		dto.version = revision.getVersion().toString();
		dto.capabilities = getListCapabilityDTO(revision.getDeclaredCapabilities(null));
		dto.requirements = getListRequirementDTO(revision.getDeclaredRequirements(null));
		return dto;
	}

	private List<CapabilityDTO> getListCapabilityDTO(List<BundleCapability> caps) {
		if (caps == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		List<CapabilityDTO> dtos = (List<CapabilityDTO>) objects.get(caps);
		if (dtos != null) {
			return dtos;
		}
		dtos = newList(caps.size());
		objects.put(caps, dtos);
		for (BundleCapability cap : caps) {
			dtos.add(getCapabilityDTO(cap));
		}
		return dtos;
	}

	private CapabilityDTO getCapabilityDTO(BundleCapability cap) {
		if (cap == null) {
			return null;
		}
		CapabilityDTO dto = (CapabilityDTO) objects.get(cap);
		if (dto != null) {
			return dto;
		}
		dto = new CapabilityDTO();
		objects.put(cap, dto);
		dto.namespace = cap.getNamespace();
		dto.resource = getBundleRevisionDTO(cap.getResource());
		dto.attributes = newMap(cap.getAttributes());
		dto.directives = newMap(cap.getDirectives());
		return dto;
	}

	private List<RequirementDTO> getListRequirementDTO(List<BundleRequirement> reqs) {
		if (reqs == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		List<RequirementDTO> dtos = (List<RequirementDTO>) objects.get(reqs);
		if (dtos != null) {
			return dtos;
		}
		dtos = newList(reqs.size());
		objects.put(reqs, dtos);
		for (BundleRequirement req : reqs) {
			dtos.add(getRequirementDTO(req));
		}
		return dtos;
	}

	private RequirementDTO getRequirementDTO(BundleRequirement req) {
		if (req == null) {
			return null;
		}
		RequirementDTO dto = (RequirementDTO) objects.get(req);
		if (dto != null) {
			return dto;
		}
		dto = new RequirementDTO();
		objects.put(req, dto);
		dto.namespace = req.getNamespace();
		dto.resource = getBundleRevisionDTO(req.getResource());
		dto.attributes = newMap(req.getAttributes());
		dto.directives = newMap(req.getDirectives());
		return dto;
	}

	public static BundleRevisionsDTO newBundleRevisionsDTO(BundleRevisions revisions) {
		return new DTOBuilder().getBundleRevisionsDTO(revisions);
	}

	private BundleRevisionsDTO getBundleRevisionsDTO(BundleRevisions revisions) {
		if (revisions == null) {
			return null;
		}
		BundleRevisionsDTO dto = new BundleRevisionsDTO();
		List<BundleRevision> revs = revisions.getRevisions();
		List<BundleRevisionDTO> dtos = newList(revs.size());
		for (BundleRevision rev : revs) {
			dtos.add(getBundleRevisionDTO(rev));
		}
		dto.revisions = dtos;
		return dto;
	}

	public static BundleWiringDTO newBundleWiringDTO(BundleRevision revision) {
		if (revision == null) {
			return null;
		}
		return new DTOBuilder().getBundleWiringDTO(revision.getWiring());
	}

	private BundleWiringDTO getBundleWiringDTO(BundleWiring wiring) {
		if (wiring == null) {
			return null;
		}
		BundleWiringDTO dto = (BundleWiringDTO) objects.get(wiring);
		if (dto != null) {
			return dto;
		}
		dto = new BundleWiringDTO();
		objects.put(wiring, dto);
		dto.current = wiring.isCurrent();
		dto.inUse = wiring.isInUse();
		dto.resource = getBundleRevisionDTO(wiring.getRevision());
		dto.capabilities = getListCapabilityDTO(wiring.getCapabilities(null));
		dto.requirements = getListRequirementDTO(wiring.getRequirements(null));
		dto.providedWires = getListBundleWireDTO(wiring.getProvidedWires(null));
		dto.requiredWires = getListBundleWireDTO(wiring.getRequiredWires(null));
		return dto;
	}

	private List<WireDTO> getListBundleWireDTO(List<BundleWire> wires) {
		if (wires == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		List<WireDTO> dtos = (List<WireDTO>) objects.get(wires);
		if (dtos != null) {
			return dtos;
		}
		dtos = newList(wires.size());
		objects.put(wires, dtos);
		for (BundleWire wire : wires) {
			dtos.add(getBundleWireDTO(wire));
		}
		return dtos;
	}

	private BundleWireDTO getBundleWireDTO(BundleWire wire) {
		if (wire == null) {
			return null;
		}
		BundleWireDTO dto = (BundleWireDTO) objects.get(wire);
		if (dto != null) {
			return dto;
		}
		dto = new BundleWireDTO();
		objects.put(wire, dto);
		dto.capability = getCapabilityDTO(wire.getCapability());
		dto.requirement = getRequirementDTO(wire.getRequirement());
		dto.provider = getBundleRevisionDTO(wire.getProvider());
		dto.requirer = getBundleRevisionDTO(wire.getRequirer());
		dto.providerWiring = getBundleWiringDTO(wire.getProviderWiring());
		dto.requirerWiring = getBundleWiringDTO(wire.getRequirerWiring());
		return dto;
	}

	public static BundleWiringsDTO newBundleWiringsDTO(BundleRevisions revisions) {
		return new DTOBuilder().getBundleWiringsDTO(revisions);
	}

	private BundleWiringsDTO getBundleWiringsDTO(BundleRevisions revisions) {
		if (revisions == null) {
			return null;
		}
		BundleWiringsDTO dto = new BundleWiringsDTO();
		List<BundleRevision> revs = revisions.getRevisions();
		List<BundleWiringDTO> dtos = newList(revs.size());
		for (BundleRevision rev : revs) {
			BundleWiring wiring = rev.getWiring();
			if (wiring != null) {
				dtos.add(getBundleWiringDTO(wiring));
			}
		}
		dto.wirings = dtos;
		return dto;
	}

	public static FrameworkDTO newFrameworkDTO(BundleContext systemBundleContext, Map<String, String> configuration) {
		return new DTOBuilder().getFrameworkDTO(systemBundleContext, configuration);
	}

	private FrameworkDTO getFrameworkDTO(BundleContext systemBundleContext, Map<String, String> configuration) {
		FrameworkDTO dto = new FrameworkDTO();
		dto.properties = asProperties(configuration);
		if (systemBundleContext == null) {
			dto.bundles = newList(0);
			dto.services = newList(0);
			return dto;
		}
		Bundle[] bundles = systemBundleContext.getBundles();
		int size = bundles == null ? 0 : bundles.length;
		List<BundleDTO> bundleDTOs = newList(size);
		for (int i = 0; i < size; i++) {
			bundleDTOs.add(getBundleDTO(bundles[i]));
		}
		dto.bundles = bundleDTOs;
		try {
			ServiceReference<?>[] references = systemBundleContext.getAllServiceReferences(null, null);
			size = references == null ? 0 : references.length;
			List<ServiceReferenceDTO> refDTOs = newList(size);
			for (int i = 0; i < size; i++) {
				refDTOs.add(getServiceReferenceDTO(references[i]));
			}
			dto.services = refDTOs;
		} catch (InvalidSyntaxException e) {
			dto.services = newList(0);
		}
		return dto;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asProperties(Map<String, ?> m) {
		return (Map<String, Object>) m;
	}

	private ServiceReferenceDTO getServiceReferenceDTO(ServiceReference<?> ref) {
		if (ref == null) {
			return null;
		}
		ServiceReferenceDTO dto = (ServiceReferenceDTO) objects.get(ref);
		if (dto != null) {
			return dto;
		}
		dto = new ServiceReferenceDTO();
		objects.put(ref, dto);
		dto.bundle = ref.getBundle().getBundleId();
		String[] keys = ref.getPropertyKeys();
		Map<String, Object> properties = newMap(keys.length);
		for (String k : keys) {
			properties.put(k, mapValue(ref.getProperty(k)));
		}
		dto.properties = properties;
		Bundle[] using = ref.getUsingBundles();
		final int length = (using == null) ? 0 : using.length;
		long[] usingBundles = new long[length];
		for (int i = 0; i < length; i++) {
			usingBundles[i] = using[i].getBundleId();
		}
		dto.usingBundles = usingBundles;
		return dto;
	}

	public static FrameworkStartLevelDTO newFrameworkStartLevelDTO(FrameworkStartLevel fsl) {
		if (fsl == null) {
			return null;
		}
		FrameworkStartLevelDTO dto = new FrameworkStartLevelDTO();
		dto.initialBundleStartLevel = fsl.getInitialBundleStartLevel();
		dto.startLevel = fsl.getStartLevel();
		return dto;
	}

	public static ServiceReferenceDTO[] newArrayServiceReferenceDTO(ServiceReference<?>[] references) {
		return new DTOBuilder().getArrayServiceReferenceDTO(references);
	}

	private ServiceReferenceDTO[] getArrayServiceReferenceDTO(ServiceReference<?>[] references) {
		final int length = (references == null) ? 0 : references.length;
		ServiceReferenceDTO[] refDTOs = new ServiceReferenceDTO[length];
		for (int i = 0; i < length; i++) {
			refDTOs[i] = getServiceReferenceDTO(references[i]);
		}
		return refDTOs;
	}

	private static Object mapValue(Object v) {
		if ((v == null)
				|| v instanceof Number
				|| v instanceof Boolean
				|| v instanceof Character
				|| v instanceof String
				|| v instanceof DTO) {
			return v;
		}
		if (v instanceof Map) {
			Map<?, ?> m = (Map<?, ?>) v;
			Map<Object, Object> map = newMap(m.size());
			for (Map.Entry<?, ?> e : m.entrySet()) {
				map.put(mapValue(e.getKey()), mapValue(e.getValue()));
			}
			return map;
		}
		if (v instanceof List) {
			List<?> c = (List<?>) v;
			List<Object> list = newList(c.size());
			for (Object o : c) {
				list.add(mapValue(o));
			}
			return list;
		}
		if (v instanceof Set) {
			Set<?> c = (Set<?>) v;
			Set<Object> set = newSet(c.size());
			for (Object o : c) {
				set.add(mapValue(o));
			}
			return set;
		}
		if (v.getClass().isArray()) {
			final int length = Array.getLength(v);
			final Class<?> componentType = mapComponentType(v.getClass().getComponentType());
			Object array = Array.newInstance(componentType, length);
			for (int i = 0; i < length; i++) {
				Array.set(array, i, mapValue(Array.get(v, i)));
			}
			return array;
		}
		return String.valueOf(v);
	}

	private static Class<?> mapComponentType(Class<?> componentType) {
		if (componentType.isPrimitive()
				|| componentType.isArray()
				|| Object.class.equals(componentType)
				|| Number.class.isAssignableFrom(componentType)
				|| Boolean.class.isAssignableFrom(componentType)
				|| Character.class.isAssignableFrom(componentType)
				|| String.class.isAssignableFrom(componentType)
				|| DTO.class.isAssignableFrom(componentType)) {
			return componentType;
		}
		if (Map.class.isAssignableFrom(componentType)) {
			return Map.class;
		}
		if (List.class.isAssignableFrom(componentType)) {
			return List.class;
		}
		if (Set.class.isAssignableFrom(componentType)) {
			return Set.class;
		}
		return String.class;
	}

	private static <E> List<E> newList(int size) {
		return new ArrayList<E>(size);
	}

	private static <E> Set<E> newSet(int size) {
		return new HashSet<E>(size);
	}

	private static <K, V> Map<K, V> newMap(int size) {
		return new HashMap<K, V>(size);
	}

	private static <K, V> Map<K, V> newMap(Map<? extends K, ? extends V> map) {
		return new HashMap<K, V>(map);
	}
}
