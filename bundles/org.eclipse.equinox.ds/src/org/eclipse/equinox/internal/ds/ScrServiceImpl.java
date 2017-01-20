/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds;

import java.util.*;
import java.util.Map.Entry;
import org.apache.felix.scr.*;
import org.osgi.framework.*;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.*;

@Deprecated
public class ScrServiceImpl implements ScrService {
	final ServiceComponentRuntime scr;
	final BundleContext context;

	ScrServiceImpl(ServiceComponentRuntime scr, BundleContext context) {
		this.scr = scr;
		this.context = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
	}

	@Override
	public Component[] getComponents() {
		return toComponents(scr.getComponentDescriptionDTOs());
	}

	private Component[] toComponents(Collection<ComponentDescriptionDTO> componentDescriptionDTOs) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Component getComponent(long componentId) {
		for (ComponentDescriptionDTO dto : scr.getComponentDescriptionDTOs()) {
			Long id = (Long) dto.properties.get(ComponentConstants.COMPONENT_ID);
			if (componentId == id) {
				return toComponent(dto);
			}
		}
		return null;
	}

	private Component toComponent(ComponentDescriptionDTO dto) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Component[] getComponents(String componentName) {
		Collection<ComponentDescriptionDTO> dtos = new ArrayList<ComponentDescriptionDTO>();
		for (ComponentDescriptionDTO dto : scr.getComponentDescriptionDTOs()) {
			if (componentName.equals(dto.name)) {
				dtos.add(dto);
			}
		}
		return toComponents(dtos);
	}

	@Override
	public Component[] getComponents(Bundle bundle) {
		return toComponents(scr.getComponentDescriptionDTOs(bundle));
	}

	ServiceReference<?> getServiceReference(long id) {
		try {
			ServiceReference<?>[] refs = context.getServiceReferences((String) null, "(" + Constants.SERVICE_ID + "=" + id + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (refs != null && refs.length > 0) {
				return refs[0];
			}
		} catch (InvalidSyntaxException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	class ComponentImpl implements Component {
		private final ComponentConfigurationDTO config;

		ComponentImpl(ComponentConfigurationDTO config) {
			this.config = config;
		}

		@Override
		public long getId() {
			return config.id;
		}

		@Override
		public String getName() {
			return config.description.name;
		}

		@Override
		public int getState() {
			switch (config.state) {
				case ComponentConfigurationDTO.ACTIVE :
					return Component.STATE_ACTIVE;
				case ComponentConfigurationDTO.SATISFIED :
					return Component.STATE_REGISTERED;
				case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION :
					return Component.STATE_UNSATISFIED;
				case ComponentConfigurationDTO.UNSATISFIED_REFERENCE :
					return Component.STATE_UNSATISFIED;
				default :
					break;
			}
			return Component.STATE_DISABLED;
		}

		@Override
		public Bundle getBundle() {
			return context.getBundle(config.description.bundle.id);
		}

		@Override
		public String getFactory() {
			return config.description.factory;
		}

		@Override
		public boolean isServiceFactory() {
			return Constants.SCOPE_BUNDLE.equals(config.description.scope);
		}

		@Override
		public String getClassName() {
			return config.description.implementationClass;
		}

		@Override
		public boolean isDefaultEnabled() {
			return config.description.defaultEnabled;
		}

		@Override
		public boolean isImmediate() {
			return config.description.immediate;
		}

		@Override
		public String[] getServices() {
			return config.description.serviceInterfaces;
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		@Override
		public Dictionary getProperties() {
			return new Hashtable(config.description.properties);
		}

		@Override
		public Reference[] getReferences() {
			if (config.description.references.length == 0) {
				return null;
			}
			Map<String, ReferenceDTO> referenceDTOs = new HashMap<String, ReferenceDTO>();
			for (ReferenceDTO reference : config.description.references) {
				referenceDTOs.put(reference.name, reference);
			}
			Map<String, SatisfiedReferenceDTO> satisfiedDTOs = new HashMap<String, SatisfiedReferenceDTO>();
			for (SatisfiedReferenceDTO satisfied : config.satisfiedReferences) {
				satisfiedDTOs.put(satisfied.name, satisfied);
			}
			Map<String, UnsatisfiedReferenceDTO> unsatisfiedDTOs = new HashMap<String, UnsatisfiedReferenceDTO>();
			for (UnsatisfiedReferenceDTO unsatisfied : config.unsatisfiedReferences) {
				unsatisfiedDTOs.put(unsatisfied.name, unsatisfied);
			}
			return toReferences(referenceDTOs, satisfiedDTOs, unsatisfiedDTOs);
		}

		private Reference[] toReferences(Map<String, ReferenceDTO> referenceDTOs, Map<String, SatisfiedReferenceDTO> satisfiedDTOs, Map<String, UnsatisfiedReferenceDTO> unsatisfiedDTOs) {
			Collection<Reference> references = new ArrayList<Reference>();
			for (Entry<String, SatisfiedReferenceDTO> satisfied : satisfiedDTOs.entrySet()) {
				references.add(new SatsifiedReference(satisfied.getValue(), referenceDTOs.get(satisfied.getValue().name)));
			}
			for (Entry<String, UnsatisfiedReferenceDTO> unsatisfied : unsatisfiedDTOs.entrySet()) {
				references.add(new UnsatsifiedReference(unsatisfied.getValue(), referenceDTOs.get(unsatisfied.getValue().name)));
			}
			return references.toArray(new Reference[0]);
		}

		@Override
		public ComponentInstance getComponentInstance() {
			throw new UnsupportedOperationException("Not supported."); //$NON-NLS-1$
		}

		@Override
		public String getActivate() {
			return config.description.activate;
		}

		@Override
		public boolean isActivateDeclared() {
			return config.description.activate != null;
		}

		@Override
		public String getDeactivate() {
			return config.description.deactivate;
		}

		@Override
		public boolean isDeactivateDeclared() {
			return config.description.deactivate != null;
		}

		@Override
		public String getModified() {
			return config.description.modified;
		}

		@Override
		public String getConfigurationPolicy() {
			return config.description.configurationPolicy;
		}

		@Override
		public void enable() {
			scr.disableComponent(config.description);
		}

		@Override
		public void disable() {
			scr.disableComponent(config.description);
		}
	}

	abstract class ReferenceBase implements Reference {
		protected final ReferenceDTO reference;

		ReferenceBase(ReferenceDTO reference) {
			this.reference = reference;
		}

		@Override
		public String getName() {
			return reference.name;
		}

		@Override
		public String getServiceName() {
			return reference.interfaceName;
		}

		@Override
		public boolean isOptional() {
			return reference.cardinality.startsWith("0"); //$NON-NLS-1$
		}

		@Override
		public boolean isMultiple() {
			return reference.cardinality.startsWith("1"); //$NON-NLS-1$
		}

		@Override
		public boolean isStatic() {
			return "static".equals(reference.policy); //$NON-NLS-1$
		}

		@Override
		public String getTarget() {
			return reference.target;
		}

		@Override
		public String getBindMethodName() {
			return reference.bind;
		}

		@Override
		public String getUnbindMethodName() {
			return reference.unbind;
		}

		@Override
		public String getUpdatedMethodName() {
			return reference.updated;
		}
	}

	class SatsifiedReference extends ReferenceBase {
		private final SatisfiedReferenceDTO satisfied;

		SatsifiedReference(SatisfiedReferenceDTO satisifed, ReferenceDTO reference) {
			super(reference);
			this.satisfied = satisifed;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public ServiceReference[] getServiceReferences() {
			Collection<ServiceReference<?>> serviceReferences = new ArrayList<ServiceReference<?>>();
			for (ServiceReferenceDTO serviceRefDTO : satisfied.boundServices) {
				ServiceReference<?> ref = getServiceReference(serviceRefDTO.id);
				if (ref != null) {
					serviceReferences.add(ref);
				}
			}
			return serviceReferences.isEmpty() ? null : serviceReferences.toArray(new ServiceReference[0]);
		}

		@Override
		public boolean isSatisfied() {
			return true;
		}
	}

	class UnsatsifiedReference extends ReferenceBase {
		private final UnsatisfiedReferenceDTO unsatisfied;

		UnsatsifiedReference(UnsatisfiedReferenceDTO unsatisifed, ReferenceDTO reference) {
			super(reference);
			this.unsatisfied = unsatisifed;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public ServiceReference[] getServiceReferences() {
			Collection<ServiceReference<?>> serviceReferences = new ArrayList<ServiceReference<?>>();
			for (ServiceReferenceDTO serviceRefDTO : unsatisfied.targetServices) {
				ServiceReference<?> ref = getServiceReference(serviceRefDTO.id);
				if (ref != null) {
					serviceReferences.add(ref);
				}
			}
			return serviceReferences.isEmpty() ? null : serviceReferences.toArray(new ServiceReference[0]);
		}

		@Override
		public boolean isSatisfied() {
			return false;
		}
	}

}
