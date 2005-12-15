/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.ds.model;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import org.eclipse.equinox.ds.Log;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentException;
import org.osgi.service.log.LogService;

/**
 * 
 * Memory model of the Service Component xml
 * 
 * @version $Revision: 1.2 $
 */
public class ComponentDescription implements Serializable {
	/**
	 * Eclipse-generated <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -6131574862715499271L;

	transient private BundleContext bundleContext;
	private String name;
	private boolean autoenable;
	private boolean immediate;
	private boolean enabled;
	private boolean valid;
	private String factory;

	private ImplementationDescription implementation;
	private List propertyDescriptions;
	private ServiceDescription service;
	private List servicesProvided;

	private List referenceDescriptions;

	transient private List componentDescriptionProps;

	transient private boolean activateMethodInitialized;
	transient private Method activateMethod;
	transient private boolean deactivateMethodInitialized;
	transient private Method deactivateMethod;

	//does this really need to be transient?
	transient private Map properties;

	/**
	 * Map of Component Configurations created for this Service Component keyed
	 * by ConfigurationAdmin PID
	 */
	transient private Map cdpsByPID;

	/**
	 * Constructor
	 * 
	 * @param bundleContext The bundle to set.
	 */
	public ComponentDescription(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
		autoenable = true;
		immediate = false;
		propertyDescriptions = new ArrayList();
		referenceDescriptions = new ArrayList();
		componentDescriptionProps = new ArrayList();
		cdpsByPID = new Hashtable();
		servicesProvided = Collections.EMPTY_LIST;
	}

	/**
	 * Return true if autoenable is set
	 * 
	 * @return Returns autoenable
	 */
	public boolean isAutoenable() {
		return autoenable;
	}

	/**
	 * @param autoenable The autoenable value to set.
	 */
	public void setAutoenable(boolean autoenable) {
		this.autoenable = autoenable;
	}

	/**
	 * @return Returns true if immediate is set
	 */
	public boolean isImmediate() {
		return immediate;
	}

	/**
	 * Set the specified value for immediate
	 * 
	 * @param immediate
	 */
	public void setImmediate(boolean immediate) {
		this.immediate = immediate;
	}

	/**
	 * @return Returns the bundleContext
	 */
	public BundleContext getBundleContext() {
		return bundleContext;
	}

	/**
	 * Set the bundle context for this service component (note package private 
	 * visibility)
	 */
	void setBundleContext(BundleContext context) {
		this.bundleContext = context;
	}

	/**
	 * @return Returns the factory.
	 */
	public String getFactory() {
		return factory;
	}

	/**
	 * @param factory The factory to set.
	 */
	public void setFactory(String factory) {
		this.factory = factory;
	}

	/**
	 * @return Returns the implementation.
	 */
	public ImplementationDescription getImplementation() {
		return implementation;
	}

	/**
	 * @param implementation The implementation to set.
	 */
	public void setImplementation(ImplementationDescription implementation) {
		this.implementation = implementation;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the XML properties.
	 */
	public Map getProperties() {
		if (properties == null) {
			//the first time this method is called load the properties 
			//(possibly from files) - for subsequent calls we do not 
			//load again
			if (propertyDescriptions.isEmpty()) {
				properties = Collections.EMPTY_MAP;
			} else {
				properties = new Hashtable(propertyDescriptions.size());
			}

			//get properties from Service Component XML, in parse order
			Iterator it = propertyDescriptions.iterator();
			while (it.hasNext()) {
				PropertyDescription propertyDescription = (PropertyDescription) it.next();
				if (propertyDescription instanceof PropertyValueDescription) {
					PropertyValueDescription propvalue = (PropertyValueDescription) propertyDescription;
					properties.put(propvalue.getName(), propvalue.getValue());
				} else {
					// read from seperate properties file
					properties.putAll(loadPropertyFile(((PropertyResourceDescription) propertyDescription).getEntry()));
				}
			}
		}

		return properties;
	}

	/**
	 * @param propertyDescription The properties to set.
	 */
	public void addPropertyDescription(PropertyDescription propertyDescription) {
		propertyDescriptions.add(propertyDescription);
	}

	/**
	 * Get the Service Component properties from a properties entry file
	 * 
	 * @param propertyEntryName - the name of the properties file
	 */
	private Hashtable loadPropertyFile(String propertyEntryName) {

		URL url = null;
		Properties props = new Properties();

		url = this.getBundleContext().getBundle().getEntry(propertyEntryName);
		if (url == null) {
			throw new ComponentException("Properties entry file " + propertyEntryName + " cannot be found");
		}

		try {
			InputStream in = null;
			File file = new File(propertyEntryName);

			if (file.exists()) {
				// throws FileNotFoundException if it's not there or no read
				// access
				in = new FileInputStream(file);
			} else {
				in = url.openStream();
			}
			if (in != null) {
				props.load(new BufferedInputStream(in));
				in.close();
			} else {
				Log.log(LogService.LOG_WARNING, "Unable to find properties file " + propertyEntryName);
			}
		} catch (IOException e) {
			throw new ComponentException("Properties entry file " + propertyEntryName + " cannot be read");
		}

		return props;

	}

	/**
	 * @return Returns the service.
	 */
	public ServiceDescription getService() {
		return service;
	}

	/**
	 * return a handly list of the serviceInterfaces provided
	 */
	public List getServicesProvided() {
		return servicesProvided;
	}

	/**
	 * @param service The service to set.
	 */
	public void setService(ServiceDescription service) {
		this.service = service;

		if (service != null) {
			servicesProvided = new ArrayList();
			ProvideDescription[] provideDescription = service.getProvides();
			for (int i = 0; i < provideDescription.length; i++) {
				servicesProvided.add(provideDescription[i].getInterfacename());
			}
		} else {
			servicesProvided = Collections.EMPTY_LIST;
		}

	}

	/**
	 * @return Returns the reference Descriptions.
	 */
	public List getReferenceDescriptions() {
		return referenceDescriptions;
	}

	/**
	 * @param reference The references to set.
	 */
	public void addReferenceDescription(ReferenceDescription reference) {
		referenceDescriptions.add(reference);
	}

	/**
	 * @return Returns true if enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled set the value
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @return Returns true if valid
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * @param valid - set the value
	 */
	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public void addComponentDescriptionProp(ComponentDescriptionProp cdp) {
		componentDescriptionProps.add(cdp);
		String pid = (String) cdp.getProperties().get(Constants.SERVICE_PID);
		if (pid != null) {
			cdpsByPID.put(pid, cdp);
		}
	}

	public List getComponentDescriptionProps() {
		return componentDescriptionProps;
	}

	public ComponentDescriptionProp getComponentDescriptionPropByPID(String pid) {
		return (ComponentDescriptionProp) cdpsByPID.get(pid);
	}

	public void clearComponentDescriptionProps() {
		componentDescriptionProps.clear();
		cdpsByPID.clear();
	}

	public void removeComponentDescriptionProp(ComponentDescriptionProp cdp) {
		componentDescriptionProps.remove(cdp);
	}

	/**
	 * Called by the (de)serialization logic to create an object of this class
	 * from a serialized file record
	 * 
	 * @see Serializable
	 */
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		//do standard deserialization
		in.defaultReadObject();

		//initialize transient collections
		this.componentDescriptionProps = new ArrayList();
		this.cdpsByPID = new Hashtable();
	}

	public Method getActivateMethod() {
		return activateMethod;
	}

	public void setActivateMethod(Method activateMethod) {
		activateMethodInitialized = true;
		this.activateMethod = activateMethod;
	}

	public boolean isActivateMethodInitialized() {
		return activateMethodInitialized;
	}

	public Method getDeactivateMethod() {
		return deactivateMethod;
	}

	public void setDeactivateMethod(Method deactivateMethod) {
		deactivateMethodInitialized = true;
		this.deactivateMethod = deactivateMethod;
	}

	public boolean isDeactivateMethodInitialized() {
		return deactivateMethodInitialized;
	}
}
