/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.adaptor.core;

import java.util.Hashtable;
import java.util.Vector;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * A default implementation of the ServiceRegistry.
 */
public class ServiceRegistry implements org.eclipse.osgi.framework.adaptor.ServiceRegistry {

	/** Published services by class name. Key is a String class name; Value is a Vector of ServiceRegistrations */
	protected Hashtable publishedServicesByClass;
	/** All published services. Value is ServiceRegistrations */
	protected Vector allPublishedServices;
	/** Published services by BundleContext.  Key is a BundleContext; Value is a Vector of ServiceRegistrations*/
	protected Hashtable publishedServicesByContext;

	/**
	 * Initializes the internal data structures of this ServiceRegistry.
	 *
	 */
	public void initialize(){
		publishedServicesByClass = new Hashtable(53);
		publishedServicesByContext = new Hashtable(53);
		allPublishedServices = new Vector(50, 20);
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.ServiceRegistry#publishService(BundleContext, ServiceRegistration)
	 */
	public void publishService(BundleContext context, ServiceRegistration serviceReg) {

		// Add the ServiceRegistration to the list of Services published by BundleContext.
		Vector contextServices = (Vector) publishedServicesByContext.get(context);
		if (contextServices == null) {
			contextServices = new Vector(10, 10);
			publishedServicesByContext.put(context, contextServices);
		}
		contextServices.addElement(serviceReg);

		// Add the ServiceRegistration to the list of Services published by Class Name.
		String[] clazzes = (String[]) serviceReg.getReference().getProperty(Constants.OBJECTCLASS);
		int size = clazzes.length;

		for (int i = 0; i < size; i++) {
			String clazz = clazzes[i];

			Vector services = (Vector) publishedServicesByClass.get(clazz);

			if (services == null) {
				services = new Vector(10, 10);
				publishedServicesByClass.put(clazz, services);
			}

			services.addElement(serviceReg);
		}

		// Add the ServiceRegistration to the list of all published Services.
		allPublishedServices.addElement(serviceReg);
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.ServiceRegistry#unpublishService(BundleContext, ServiceRegistration)
	 */
	public void unpublishService(BundleContext context, ServiceRegistration serviceReg) {

		// Remove the ServiceRegistration from the list of Services published by BundleContext.
		Vector contextServices = (Vector) publishedServicesByContext.get(context);
		if (contextServices != null) {
			contextServices.removeElement(serviceReg);
		}

		// Remove the ServiceRegistration from the list of Services published by Class Name.
		String[] clazzes = (String[]) serviceReg.getReference().getProperty(Constants.OBJECTCLASS);
		int size = clazzes.length;

		for (int i = 0; i < size; i++) {
			String clazz = clazzes[i];
			Vector services = (Vector) publishedServicesByClass.get(clazz);
			services.removeElement(serviceReg);
		}

		// Remove the ServiceRegistration from the list of all published Services.
		allPublishedServices.removeElement(serviceReg);

	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.ServiceRegistry#unpublishServices(BundleContext)
	 */
	public void unpublishServices(BundleContext context) {
		// Get all the Services published by the BundleContext.
		Vector serviceRegs = (Vector) publishedServicesByContext.get(context);
		if (serviceRegs != null) {
			// Remove this list for the BundleContext
			publishedServicesByContext.remove(context);
			int size = serviceRegs.size();
			for (int i = 0; i < size; i++) {
				ServiceRegistration serviceReg = (ServiceRegistration) serviceRegs.elementAt(i);
				// Remove each service from the list of all published Services
				allPublishedServices.removeElement(serviceReg);

				// Remove each service from the list of Services published by Class Name. 
				String[] clazzes = (String[]) serviceReg.getReference().getProperty(Constants.OBJECTCLASS);
				int numclazzes = clazzes.length;

				for (int j = 0; j < numclazzes; j++) {
					String clazz = clazzes[j];
					Vector services = (Vector) publishedServicesByClass.get(clazz);
					services.removeElement(serviceReg);
				}
			}
		}
	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.ServiceRegistry#lookupServiceReferences(String, Filter)
	 */
	public Vector lookupServiceReferences(String clazz, Filter filter) {
		int size;
		Vector references = new Vector();

		if (clazz == null) /* all services */ {
			Vector serviceRegs = allPublishedServices;

			if (serviceRegs == null) {
				return (null);
			}

			size = serviceRegs.size();

			if (size == 0) {
				return (null);
			}

			for (int i = 0; i < size; i++) {
				ServiceRegistration registration = (ServiceRegistration) serviceRegs.elementAt(i);

				ServiceReference reference = registration.getReference();
				if ((filter == null) || filter.match(reference)) {
					references.addElement(reference);
				}
			}
		} else /* services registered under the class name */ {
			Vector serviceRegs = (Vector) publishedServicesByClass.get(clazz);

			if (serviceRegs == null) {
				return (null);
			}

			size = serviceRegs.size();

			if (size == 0) {
				return (null);
			}

			for (int i = 0; i < size; i++) {
				ServiceRegistration registration = (ServiceRegistration) serviceRegs.elementAt(i);

				ServiceReference reference = registration.getReference();
				if ((filter == null) || filter.match(reference)) {
					references.addElement(reference);
				}
			}
		}

		if (references.size() == 0) {
			return null;
		}

		return (references);

	}

	/**
	 * @see org.eclipse.osgi.framework.adaptor.ServiceRegistry#lookupServiceReferences(BundleContext)
	 */
	public Vector lookupServiceReferences(BundleContext context){
		int size;
		Vector references = new Vector();
		Vector serviceRegs = (Vector) publishedServicesByContext.get(context);

		if (serviceRegs == null) {
			return (null);
		}

		size = serviceRegs.size();

		if (size == 0) {
			return (null);
		}

		for (int i = 0; i < size; i++) {
			ServiceRegistration registration = (ServiceRegistration) serviceRegs.elementAt(i);

			ServiceReference reference = registration.getReference();
			references.addElement(reference);
		}

		if (references.size() == 0) {
			return null;
		}

		return (references);
	}

}
