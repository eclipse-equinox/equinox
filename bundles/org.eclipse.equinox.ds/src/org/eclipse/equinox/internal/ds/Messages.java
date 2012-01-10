/*******************************************************************************
 * Copyright (c) 1997, 2010 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.ds;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.equinox.internal.ds.SCRmessages"; //$NON-NLS-1$
	public static String ALL_COMPONENTS;
	public static String ALL_REFERENCES_RESOLVED;
	public static String BIND_METHOD_NOT_FOUND_OR_NOT_ACCESSIBLE;
	public static String UPDATED_METHOD_NOT_FOUND_OR_NOT_ACCESSIBLE;
	public static String UPDATED_METHOD_NOT_CALLED;
	public static String BUNDLE_NOT_FOUND;
	public static String CANNOT_BUILD_COMPONENT;
	public static String CANNOT_CREATE_INSTANCE;
	public static String CANNOT_FIND_COMPONENT_BUNDLE;
	public static String CANNOT_GET_CONFIGURATION;
	public static String CANNOT_GET_REFERENCES;
	public static String CANNOT_MODIFY_INSTANCE__MODIFY_METHOD_NOT_FOUND;
	public static String CANT_ACTIVATE_INSTANCE;
	public static String CANT_GET_SERVICE;
	public static String CANT_GET_SERVICE_OBJECT;
	public static String CANT_LIST_CONFIGURATIONS;
	public static String CANT_OPEN_STREAM_TO_COMPONENT_XML;
	public static String CANT_RESOLVE_COMPONENT_INSTANCE;
	public static String CIRCULARITY_EXCEPTION_FOUND;
	public static String COMPONENT_DISPOSED;
	public static String COMPONENT_CONFIGURATIONS;
	public static String COMPONENT_DETAILS;
	public static String COMPONENT_HAS_ILLEGAL_REFERENCE;
	public static String COMPONENT_ID_DEFINIED_BY_LIST_COMMAND;
	public static String COMPONENT_LACKS_APPROPRIATE_PERMISSIONS;
	public static String COMPONENT_NAME;
	public static String COMPONENT_NAME_IS_NULL;
	public static String COMPONENT_NOT_FOUND;
	public static String COMPONENT_NOT_RESOLVED;
	public static String COMPONENT_REQURES_CONFIGURATION_ACTIVATION;
	public static String COMPONENT_RESOLVED;
	public static String COMPONENT_WAS_NOT_BUILT;
	public static String COMPONENT_XML_NOT_FOUND;
	public static String COMPONENTS_IN_BUNDLE;
	public static String CONFIG_ADMIN_SERVICE_NOT_AVAILABLE;
	public static String CONFIG_PROPERTIES;
	public static String COULD_NOT_CREATE_INSTANCE;
	public static String COULD_NOT_CREATE_NEW_INSTANCE;
	public static String DISABLE_ALL_COMPONENTS;
	public static String DISABLE_COMPONENT;
	public static String DISABLING_ALL_BUNDLE_COMPONENTS;
	public static String DISABLING_ALL_COMPONENTS;
	public static String DUPLICATED_REFERENCE_NAMES;
	public static String DUPLICATED_SERVICE_TAGS;
	public static String DYNAMIC_INFO;
	public static String ENABLE_ALL_COMPONENTS;
	public static String ENABLE_COMPONENT;
	public static String ENABLING_ALL_BUNDLE_COMPONENTS;
	public static String ENABLING_ALL_COMPONENTS;
	public static String ERROR_BINDING_REFERENCE;
	public static String ERROR_UPDATING_REFERENCE;
	public static String ERROR_BUILDING_COMPONENT_INSTANCE;
	public static String ERROR_CREATING_SCP;
	public static String ERROR_DEACTIVATING_INSTANCE;
	public static String ERROR_DISPATCHING_WORK;
	public static String ERROR_DISPOSING_INSTANCES;
	public static String ERROR_LISTING_CONFIGURATIONS;
	public static String ERROR_LOADING_COMPONENTS;
	public static String ERROR_LOADING_DATA_FILE;
	public static String ERROR_LOADING_PROPERTIES_FILE;
	public static String ERROR_OPENING_COMP_XML;
	public static String ERROR_PARSING_MANIFEST_HEADER;
	public static String ERROR_PROCESSING_CONFIGURATION;
	public static String ERROR_PROCESSING_END_TAG;
	public static String ERROR_PROCESSING_PROPERTY;
	public static String ERROR_PROCESSING_START_TAG;
	public static String ERROR_UNBINDING_REFERENCE;
	public static String ERROR_UNBINDING_REFERENCE2;
	public static String EXCEPTION_ACTIVATING_INSTANCE;
	public static String EXCEPTION_BUILDING_COMPONENT;
	public static String EXCEPTION_CREATING_COMPONENT_INSTANCE;
	public static String EXCEPTION_GETTING_METHOD;
	public static String EXCEPTION_LOCATING_SERVICE;
	public static String EXCEPTION_LOCATING_SERVICES;
	public static String EXCEPTION_MODIFYING_COMPONENT;
	public static String EXCEPTION_UNBINDING_REFERENCE;
	public static String EXPECTED_PARAMETER_COMPONENT_ID;
	public static String FACTORY_CONF_NOT_APPLICABLE_FOR_COMPONENT_FACTORY;
	public static String FACTORY_REGISTRATION_ALREADY_DISPOSED;
	public static String FOUND_COMPONENTS_WITH_DUPLICATED_NAMES;
	public static String FOUND_COMPONENTS_WITH_DUPLICATED_NAMES2;
	public static String ILLEGAL_DEFINITION_FILE;
	public static String ILLEGAL_ELEMENT_IN_SERVICE_TAG;
	public static String ILLEGAL_TAG_FOUND;
	public static String INCOMPATIBLE_COMBINATION;
	public static String INCORRECT_ACTIVATION_POLICY;
	public static String INCORRECT_VALUE_TYPE;
	public static String INSTANCE_ALREADY_CREATED;
	public static String INSTANCE_CREATION_TOOK_LONGER;
	public static String INSTANCE_NOT_BOUND;
	public static String INVALID_CARDINALITY_ATTR;
	public static String INVALID_COMPONENT_FACTORY_AND_SERVICE_FACTORY;
	public static String INVALID_COMPONENT_ID;
	public static String INVALID_COMPONENT_IMMEDIATE_AND_FACTORY;
	public static String INVALID_COMPONENT_IMMEDIATE_AND_SERVICE_FACTORY;
	public static String INVALID_COMPONENT_NO_SERVICES_NO_IMMEDIATE;
	public static String INVALID_COMPONENT_TAG__MULTIPLE_IMPL_ATTRIBS;
	public static String INVALID_COMPONENT_TAG__NO_CLASS_ATTR;
	public static String INVALID_OBJECT;
	public static String INVALID_POLICY_ATTR;
	public static String INVALID_POLICY_OPTION_ATTR;
	public static String INVALID_PROPERTIES_TAG__INVALID_ENTRY_VALUE;
	public static String INVALID_PROPERTIES_TAG__NO_ENTRY_ATTR;
	public static String INVALID_PROPERTY_TAG__NO_BODY_CONTENT;
	public static String INVALID_PROPERTY_TAG__NO_NAME_ATTR;
	public static String INVALID_PROPERTY_TYPE;
	public static String INVALID_PROVIDE_TAG__NO_INTERFACE_ATTR;
	public static String INVALID_REFERENCE_TAG__BIND_ATTR_EMPTY;
	public static String INVALID_REFERENCE_TAG__BIND_EQUALS_UNBIND;
	public static String INVALID_REFERENCE_TAG__UNBIND_ATTR_EMPTY;
	public static String INVALID_REFERENCE_TAG__UPDATED_ATTR_EMPTY;
	public static String INVALID_SERVICE_REFERENCE;
	public static String INVALID_SERVICE_TAG__NO_PROVIDE_TAG;
	public static String INVALID_TAG_ACCORDING_TO_NAMESPACE1_0;
	public static String INVALID_TAG_ACCORDING_TO_NAMESPACE1_2;
	public static String INVALID_TARGET_FILTER;
	public static String LIST_ALL_BUNDLE_COMPONENTS;
	public static String LIST_ALL_COMPONENTS;
	public static String LOCATED_IN_BUNDLE;
	public static String METHOD_UNACCESSABLE;
	public static String MISSING_CHARACTER;
	public static String NO_BUILT_COMPONENT_CONFIGURATIONS;
	public static String NO_COMPONENTS_FOUND;
	public static String NO_IMPLEMENTATION_ATTRIBUTE;
	public static String NO_INTERFACE_ATTR_IN_REFERENCE_TAG;
	public static String NO_NAME_ATTRIBUTE;
	public static String NOT_RESOLVED_REFERENCES;
	public static String PRINT_COMPONENT_INFO;
	public static String PROCESSING_BUNDLE_FAILED;
	public static String REGISTERED_AS_COMPONENT_AND_MANAGED_SERVICE_FACORY;
	public static String REGISTRATION_ALREADY_DISPOSED;
	public static String RETURNING_NOT_FULLY_ACTIVATED_INSTANCE;
	public static String SCR;
	public static String SENT_DISABLING_REQUEST;
	public static String SENT_ENABLING_REQUEST;
	public static String SERVICE_REFERENCE_ALREADY_BOUND;
	public static String SERVICE_USAGE_COUNT;
	public static String SPECIFIED_ACTIVATE_METHOD_NOT_FOUND;
	public static String SPECIFIED_DEACTIVATE_METHOD_NOT_FOUND;
	public static String STATE;
	public static String STATIC_OPTIONAL_REFERENCE_TO_BE_REMOVED;
	public static String TIMEOUT_GETTING_LOCK;
	public static String TIMEOUT_PROCESSING;
	public static String TIMEOUT_REACHED_ENABLING_COMPONENTS;
	public static String UNEXPECTED_ERROR;
	public static String UNEXPECTED_EXCEPTION;
	public static String UNSUPPORTED_TYPE;
	public static String WRONG_PARAMETER;
	public static String WRONG_PARAMETER2;
	public static String ERROR_DELETING_COMPONENT_DEFINITIONS;
	public static String ERROR_WRITING_OBJECT;
	public static String ERROR_READING_OBJECT;
	public static String DBMANAGER_SERVICE_TRACKER_OPENED;
	public static String ERROR_LOADING_COMPONENT_DEFINITIONS;
	public static String ERROR_MODIFYING_COMPONENT;
	public static String ERROR_SAVING_COMPONENT_DEFINITIONS;
	public static String FILE_DOESNT_EXIST_OR_DIRECTORY;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
		//
	}
}
