/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
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

package org.eclipse.osgi.internal.framework;

import java.lang.reflect.Array;
import java.util.*;
import org.osgi.dto.DTO;
import org.osgi.framework.*;
import org.osgi.framework.dto.*;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.startlevel.dto.BundleStartLevelDTO;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;
import org.osgi.framework.wiring.*;
import org.osgi.framework.wiring.dto.*;
import org.osgi.resource.dto.*;

public class DTOBuilder {
	private final Map<BundleRevision, BundleRevisionDTO> resources;
	private final Map<BundleWiring, BundleWiringDTO.NodeDTO> wiringnodes;

	private DTOBuilder() {
		resources = new IdentityHashMap<>();
		wiringnodes = new IdentityHashMap<>();
	}

	public static BundleDTO newBundleDTO(Bundle bundle) {
		if (bundle == null) {
			return null;
		}
		BundleDTO dto = new BundleDTO();
		dto.id = bundle.getBundleId();
		dto.lastModified = bundle.getLastModified();
		dto.state = bundle.getState();
		dto.symbolicName = bundle.getSymbolicName();
		dto.version = bundle.getVersion().toString();
		return dto;
	}

	public static BundleStartLevelDTO newBundleStartLevelDTO(Bundle b, BundleStartLevel bsl) {
		if (bsl == null) {
			return null;
		}
		BundleStartLevelDTO dto = new BundleStartLevelDTO();
		dto.bundle = b.getBundleId();
		dto.activationPolicyUsed = bsl.isActivationPolicyUsed();
		dto.persistentlyStarted = bsl.isPersistentlyStarted();
		dto.startLevel = bsl.getStartLevel();
		return dto;
	}

	public static BundleRevisionDTO newBundleRevisionDTO(BundleRevision revision) {
		BundleRevisionDTO dto = new DTOBuilder().getBundleRevisionDTO(revision);
		return dto;
	}

	private int getResourceId(BundleRevision revision) {
		BundleRevisionDTO dto = getBundleRevisionDTO(revision);
		if (dto == null) {
			return 0;
		}
		return dto.id;
	}

	private BundleRevisionDTO getBundleRevisionDTO(BundleRevision revision) {
		if (revision == null) {
			return null;
		}
		BundleRevisionDTO dto = resources.get(revision);
		if (dto != null) {
			return dto;
		}
		dto = new BundleRevisionDTO();
		dto.id = identifier(revision);
		resources.put(revision, dto);
		dto.bundle = revision.getBundle().getBundleId();
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
		List<CapabilityDTO> dtos = newList(caps.size());
		for (BundleCapability cap : caps) {
			dtos.add(getCapabilityDTO(cap));
		}
		return dtos;
	}

	private CapabilityDTO getCapabilityDTO(BundleCapability cap) {
		if (cap == null) {
			return null;
		}
		CapabilityDTO dto = new CapabilityDTO();
		dto.id = identifier(cap);
		dto.namespace = cap.getNamespace();
		dto.resource = getResourceId(cap.getRevision());
		dto.attributes = newAttributesMapDTO(cap.getAttributes());
		dto.directives = newDirectivesMapDTO(cap.getDirectives());
		return dto;
	}

	private List<CapabilityRefDTO> getListCapabilityRefDTO(List<BundleCapability> caps) {
		if (caps == null) {
			return null;
		}
		List<CapabilityRefDTO> dtos = newList(caps.size());
		for (BundleCapability cap : caps) {
			dtos.add(getCapabilityRefDTO(cap));
		}
		return dtos;
	}

	private CapabilityRefDTO getCapabilityRefDTO(BundleCapability cap) {
		if (cap == null) {
			return null;
		}
		CapabilityRefDTO dto = new CapabilityRefDTO();
		dto.capability = identifier(cap);
		dto.resource = getResourceId(cap.getRevision());
		return dto;
	}

	private List<RequirementDTO> getListRequirementDTO(List<BundleRequirement> reqs) {
		if (reqs == null) {
			return null;
		}
		List<RequirementDTO> dtos = newList(reqs.size());
		for (BundleRequirement req : reqs) {
			dtos.add(getRequirementDTO(req));
		}
		return dtos;
	}

	private RequirementDTO getRequirementDTO(BundleRequirement req) {
		if (req == null) {
			return null;
		}
		RequirementDTO dto = new RequirementDTO();
		dto.id = identifier(req);
		dto.namespace = req.getNamespace();
		dto.resource = getResourceId(req.getRevision());
		dto.attributes = newAttributesMapDTO(req.getAttributes());
		dto.directives = newDirectivesMapDTO(req.getDirectives());
		return dto;
	}

	private List<RequirementRefDTO> getListRequirementRefDTO(List<BundleRequirement> reqs) {
		if (reqs == null) {
			return null;
		}
		List<RequirementRefDTO> dtos = newList(reqs.size());
		for (BundleRequirement req : reqs) {
			dtos.add(getRequirementRefDTO(req));
		}
		return dtos;
	}

	private RequirementRefDTO getRequirementRefDTO(BundleRequirement req) {
		if (req == null) {
			return null;
		}
		RequirementRefDTO dto = new RequirementRefDTO();
		dto.requirement = identifier(req);
		dto.resource = getResourceId(req.getRevision());
		return dto;
	}

	public static BundleRevisionDTO[] newArrayBundleRevisionDTO(BundleRevisions revisions) {
		if (revisions == null) {
			return null;
		}
		List<BundleRevision> revs = revisions.getRevisions();
		final int size = revs.size();
		BundleRevisionDTO[] dtos = new BundleRevisionDTO[size];
		for (int i = 0; i < size; i++) {
			dtos[i] = new DTOBuilder().getBundleRevisionDTO(revs.get(i));
		}
		return dtos;
	}

	public static BundleWiringDTO newBundleWiringDTO(BundleRevision revision) {
		if (revision == null) {
			return null;
		}
		BundleWiringDTO dto = new DTOBuilder().getBundleWiringDTO(revision.getWiring());
		return dto;
	}

	public static FrameworkWiringDTO newFrameworkWiringDTO(Collection<BundleWiring> allWirings) {
		DTOBuilder builder = new DTOBuilder();
		for (BundleWiring wiring : allWirings) {
			builder.getBundleWiringNodeDTO(wiring);
		}
		FrameworkWiringDTO dto = new FrameworkWiringDTO();
		dto.wirings = new HashSet<>(builder.wiringnodes.values());
		dto.resources = new HashSet<>(builder.resources.values());
		return dto;
	}

	private BundleWiringDTO getBundleWiringDTO(BundleWiring wiring) {
		if (wiring == null) {
			return null;
		}
		BundleWiringDTO dto = new BundleWiringDTO();
		dto.bundle = wiring.getBundle().getBundleId();
		dto.root = getWiringId(wiring);
		dto.nodes = new HashSet<>(wiringnodes.values());
		dto.resources = new HashSet<>(resources.values());
		return dto;
	}

	private int getWiringId(BundleWiring wiring) {
		BundleWiringDTO.NodeDTO dto = getBundleWiringNodeDTO(wiring);
		if (dto == null) {
			return 0;
		}
		return dto.id;
	}

	private BundleWiringDTO.NodeDTO getBundleWiringNodeDTO(BundleWiring wiring) {
		if (wiring == null) {
			return null;
		}
		BundleWiringDTO.NodeDTO dto = wiringnodes.get(wiring);
		if (dto != null) {
			return dto;
		}
		dto = new BundleWiringDTO.NodeDTO();
		dto.id = identifier(wiring);
		wiringnodes.put(wiring, dto);
		dto.current = wiring.isCurrent();
		dto.inUse = wiring.isInUse();
		dto.resource = getResourceId(wiring.getRevision());
		dto.capabilities = getListCapabilityRefDTO(wiring.getCapabilities(null));
		dto.requirements = getListRequirementRefDTO(wiring.getRequirements(null));
		dto.providedWires = getListBundleWireDTO(wiring.getProvidedWires(null));
		dto.requiredWires = getListBundleWireDTO(wiring.getRequiredWires(null));
		return dto;
	}

	private List<WireDTO> getListBundleWireDTO(List<BundleWire> wires) {
		if (wires == null) {
			return null;
		}
		List<WireDTO> dtos = newList(wires.size());
		for (BundleWire wire : wires) {
			dtos.add(getBundleWireDTO(wire));
		}
		return dtos;
	}

	private BundleWireDTO getBundleWireDTO(BundleWire wire) {
		if (wire == null) {
			return null;
		}
		BundleWireDTO dto = new BundleWireDTO();
		dto.capability = getCapabilityRefDTO(wire.getCapability());
		dto.requirement = getRequirementRefDTO(wire.getRequirement());
		dto.provider = getResourceId(wire.getProvider());
		dto.requirer = getResourceId(wire.getRequirer());
		dto.providerWiring = getWiringId(wire.getProviderWiring());
		dto.requirerWiring = getWiringId(wire.getRequirerWiring());
		return dto;
	}

	public static BundleWiringDTO[] newArrayBundleWiringDTO(BundleRevisions revisions) {
		if (revisions == null) {
			return null;
		}
		List<BundleRevision> revs = revisions.getRevisions();
		final int size = revs.size();
		List<BundleWiringDTO> dtos = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			BundleWiring wiring = revs.get(i).getWiring();
			if (wiring != null) {
				dtos.add(new DTOBuilder().getBundleWiringDTO(wiring)); // use new DTOBuilder for each wiring dto
			}
		}
		return dtos.toArray(new BundleWiringDTO[dtos.size()]);
	}

	public static FrameworkDTO newFrameworkDTO(BundleContext systemBundleContext, Map<String, String> configuration) {
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
			bundleDTOs.add(newBundleDTO(bundles[i]));
		}
		dto.bundles = bundleDTOs;
		try {
			ServiceReference<?>[] references = systemBundleContext.getAllServiceReferences(null, null);
			size = references == null ? 0 : references.length;
			List<ServiceReferenceDTO> refDTOs = newList(size);
			for (int i = 0; i < size; i++) {
				ServiceReferenceDTO serviceRefDTO = getServiceReferenceDTO(references[i]);
				if (serviceRefDTO != null) {
					refDTOs.add(serviceRefDTO);
				}
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

	private static ServiceReferenceDTO getServiceReferenceDTO(ServiceReference<?> ref) {
		if (ref == null) {
			return null;
		}
		Bundle b = ref.getBundle();
		if (b == null) {
			// service has been unregistered
			return null;
		}
		ServiceReferenceDTO dto = new ServiceReferenceDTO();
		dto.bundle = b.getBundleId();
		String[] keys = ref.getPropertyKeys();
		Map<String, Object> properties = newMap(keys.length);
		for (String k : keys) {
			Object v = ref.getProperty(k);
			if (Constants.SERVICE_ID.equals(k)) {
				dto.id = ((Long) v).longValue();
			}
			properties.put(k, mapValue(v));
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
		if (references == null) {
			return null;
		}
		final int length = references.length;
		List<ServiceReferenceDTO> refDTOs = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			ServiceReferenceDTO dto = getServiceReferenceDTO(references[i]);
			if (dto != null) {
				refDTOs.add(dto);
			}
		}
		return refDTOs.toArray(new ServiceReferenceDTO[refDTOs.size()]);
	}

	private static Object mapValue(Object v) {
		if ((v == null) || v instanceof Number || v instanceof Boolean || v instanceof Character || v instanceof String || v instanceof DTO) {
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
		if (componentType.isPrimitive() || componentType.isArray() || Object.class.equals(componentType) || Number.class.isAssignableFrom(componentType) || Boolean.class.isAssignableFrom(componentType) || Character.class.isAssignableFrom(componentType) || String.class.isAssignableFrom(componentType) || DTO.class.isAssignableFrom(componentType)) {
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
		return new ArrayList<>(size);
	}

	private static <E> Set<E> newSet(int size) {
		return new HashSet<>(size);
	}

	private static <K, V> Map<K, V> newMap(int size) {
		return new HashMap<>(size);
	}

	/**
	 * Assumes the input map is always <String,String>.
	 */
	private static Map<String, String> newDirectivesMapDTO(Map<String, String> map) {
		Map<String, String> dto = new HashMap<>(map);
		return dto;
	}

	/**
	 * Assumes the input map always has String keys and the values are of types:
	 * String, Version, Long, Double or List of the previous types. Lists are copied
	 * and Version objects are converted to String objects.
	 */
	private static Map<String, Object> newAttributesMapDTO(Map<String, Object> map) {
		Map<String, Object> dto = new HashMap<>(map);
		/* Lists are copied and Version objects are converted to String objects. */
		for (Map.Entry<String, Object> entry : dto.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof Version) {
				entry.setValue(String.valueOf(value));
				continue;
			}
			if (value instanceof List) {
				List<Object> newList = new ArrayList<>((List<?>) value);
				for (ListIterator<Object> iter = newList.listIterator(); iter.hasNext();) {
					Object element = iter.next();
					if (element instanceof Version) {
						iter.set(String.valueOf(element));
					}
				}
				entry.setValue(newList);
				continue;
			}
		}
		return dto;
	}

	private static int identifier(Object o) {
		return System.identityHashCode(o);
	}
}
