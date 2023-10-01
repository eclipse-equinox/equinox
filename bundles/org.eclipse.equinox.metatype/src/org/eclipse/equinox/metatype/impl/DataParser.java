/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.equinox.metatype.impl;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.SAXParser;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.AttributeDefinition;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Meta XML Data Parser
 */
public class DataParser {
	private static final String METADATA = "MetaData"; //$NON-NLS-1$
	private static final String LOCALIZATION = "localization"; //$NON-NLS-1$
	private static final String OCD = "OCD"; //$NON-NLS-1$
	private static final String ICON = "Icon"; //$NON-NLS-1$
	private static final String AD = "AD"; //$NON-NLS-1$
	private static final String CARDINALITY = "cardinality"; //$NON-NLS-1$
	private static final String OPTION = "Option"; //$NON-NLS-1$
	private static final String LABEL = "label"; //$NON-NLS-1$
	private static final String VALUE = "value"; //$NON-NLS-1$
	private static final String MIN = "min"; //$NON-NLS-1$
	private static final String MAX = "max"; //$NON-NLS-1$
	private static final String TYPE = "type"; //$NON-NLS-1$
	private static final String SIZE = "size"; //$NON-NLS-1$
	private static final String ID = "id"; //$NON-NLS-1$
	private static final String NAME = "name"; //$NON-NLS-1$
	private static final String DESCRIPTION = "description"; //$NON-NLS-1$
	private static final String RESOURCE = "resource"; //$NON-NLS-1$
	private static final String PID = "pid"; //$NON-NLS-1$
	private static final String DEFAULT = "default"; //$NON-NLS-1$
	private static final String ADREF = "adref"; //$NON-NLS-1$
	private static final String CONTENT = "content"; //$NON-NLS-1$
	private static final String FACTORY = "factoryPid"; //$NON-NLS-1$
	private static final String BUNDLE = "bundle"; //$NON-NLS-1$
	private static final String OPTIONAL = "optional"; //$NON-NLS-1$
	private static final String OBJECT = "Object"; //$NON-NLS-1$
	private static final String OCDREF = "ocdref"; //$NON-NLS-1$
	private static final String ATTRIBUTE = "Attribute"; //$NON-NLS-1$
	private static final String DESIGNATE = "Designate"; //$NON-NLS-1$
	private static final String MERGE = "merge"; //$NON-NLS-1$
	private static final String REQUIRED = "required"; //$NON-NLS-1$

	private static final String INTEGER = "Integer"; //$NON-NLS-1$
	private static final String STRING = "String"; //$NON-NLS-1$
	private static final String FLOAT = "Float"; //$NON-NLS-1$
	private static final String DOUBLE = "Double"; //$NON-NLS-1$
	private static final String BYTE = "Byte"; //$NON-NLS-1$
	private static final String LONG = "Long"; //$NON-NLS-1$
	private static final String CHAR = "Char"; //$NON-NLS-1$
	private static final String CHARACTER = "Character"; //$NON-NLS-1$
	private static final String BOOLEAN = "Boolean"; //$NON-NLS-1$
	private static final String SHORT = "Short"; //$NON-NLS-1$
	private static final String PASSWORD = "Password"; //$NON-NLS-1$
	private static final String BIGINTEGER = "BigInteger"; //$NON-NLS-1$
	private static final String BIGDECIMAL = "BigDecimal"; //$NON-NLS-1$

	protected Bundle _dp_bundle;
	protected URL _dp_url;
	protected SAXParser _dp_parser;
	protected XMLReader _dp_xmlReader;

	// DesignateHanders in DataParser class
	List<DesignateHandler> _dp_designateHandlers = new ArrayList<>(7);
	// ObjectClassDefinitions in DataParser class w/ corresponding reference keys
	Map<String, ObjectClassDefinitionImpl> _dp_OCDs = new HashMap<>(7);
	// Localization in DataParser class
	String _dp_localization;

	// Default visibility to avoid a plethora of synthetic accessor method warnings.
	final LogTracker logger;
	final Collection<Designate> designates = new ArrayList<>(7);

	/*
	 * Constructor of class DataParser.
	 */
	public DataParser(Bundle bundle, URL url, SAXParser parser, LogTracker logger) {

		this._dp_bundle = bundle;
		this._dp_url = url;
		this._dp_parser = parser;
		this.logger = logger;
	}

	/*
	 * Main method to parse specific MetaData file.
	 */
	public Collection<Designate> doParse() throws IOException, SAXException {
		_dp_xmlReader = _dp_parser.getXMLReader();
		_dp_xmlReader.setContentHandler(new RootHandler());
		_dp_xmlReader.setErrorHandler(new MyErrorHandler(System.err));
		InputStream is = _dp_url.openStream();
		InputSource isource = new InputSource(is);
		logger.log(LogTracker.LOG_DEBUG, "Starting to parse " + _dp_url); //$NON-NLS-1$
		_dp_xmlReader.parse(isource);
		return designates;
	}

	/*
	 * Convert String for expected data type.
	 */
	@SuppressWarnings("deprecation")
	static Object convert(String value, int type) {

		if (value == null) {
			return null;
		}

		switch (type) {
		// PASSWORD should be treated like STRING.
		case AttributeDefinition.PASSWORD:
		case AttributeDefinition.STRING:
			return value;
		case AttributeDefinition.LONG:
			return Long.valueOf(value);
		case AttributeDefinition.INTEGER:
			return Integer.valueOf(value);
		case AttributeDefinition.SHORT:
			return Short.valueOf(value);
		case AttributeDefinition.CHARACTER:
			return Character.valueOf(value.charAt(0));
		case AttributeDefinition.BYTE:
			return Byte.valueOf(value);
		case AttributeDefinition.DOUBLE:
			return Double.valueOf(value);
		case AttributeDefinition.FLOAT:
			return Float.valueOf(value);
		case AttributeDefinition.BIGINTEGER:
			return new BigInteger(value);
		case AttributeDefinition.BIGDECIMAL:
			return new BigDecimal(value);
		case AttributeDefinition.BOOLEAN:
			return Boolean.valueOf(value);
		default:
			// Unknown data type
			return null;
		}
	}

	/**
	 * Abstract of all Handlers.
	 */
	private class AbstractHandler extends DefaultHandler {
		protected ContentHandler _doc_handler;
		protected boolean _isParsedDataValid = true;
		protected Map<String, Map<String, String>> extensionAttributes = new HashMap<>();
		protected String elementId;
		protected String elementName;

		public AbstractHandler(ContentHandler parentHandler) {
			this._doc_handler = parentHandler;
			_dp_xmlReader.setContentHandler(this);
		}

		protected void init(String name, Attributes attributes) {
			elementId = attributes.getValue(ID);
			elementName = name;
		}

		@Override
		public void endElement(String namespaceURI, String localName, String qName) {
			finished();
			// Let parent resume handling SAX events
			_dp_xmlReader.setContentHandler(_doc_handler);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			throw new SAXException(NLS.bind(MetaTypeMsg.UNEXPECTED_ELEMENT, new Object[] { qName,
					attributes.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
		}

		@Override
		public void characters(char[] buf, int start, int end) throws SAXException {
			String s = new String(buf, start, end).trim();
			if (s.length() > 0) {
				throw new SAXException(NLS.bind(MetaTypeMsg.UNEXPECTED_TEXT, new Object[] { s, elementName, elementId,
						_dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
			}
		}

		protected void collectExtensionAttributes(Attributes attributes) {
			for (int i = 0; i < attributes.getLength(); i++) {
				String key = attributes.getURI(i);
				if (key.length() == 0 || key.startsWith("http://www.osgi.org/xmlns/metatype/v")) //$NON-NLS-1$
					continue;
				Map<String, String> value = extensionAttributes.get(key);
				if (value == null) {
					value = new HashMap<>();
					extensionAttributes.put(key, value);
				}
				value.put(getName(attributes.getLocalName(i), attributes.getQName(i)), attributes.getValue(i));
			}
		}

		/**
		 * Called when this element and all elements nested into it have been handled.
		 */
		protected void finished() {
			// do nothing by default
		}
	}

	/**
	 * Handler for the root element.
	 */
	private class RootHandler extends DefaultHandler {

		public RootHandler() {
			super();
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {

			logger.log(LogTracker.LOG_DEBUG, "Here is AbstractHandler:startElement():" //$NON-NLS-1$
					+ qName);
			String name = getName(localName, qName);
			if (name.equalsIgnoreCase(METADATA)) {
				new MetaDataHandler(this).init(name, attributes);
			} else {
				logger.log(LogTracker.LOG_WARNING, NLS.bind(MetaTypeMsg.UNEXPECTED_ELEMENT, new Object[] { name,
						attributes.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
			}
		}

		@Override
		public void setDocumentLocator(Locator locator) {
			// do nothing
		}
	}

	/**
	 * Handler for the MetaData element.
	 */
	private class MetaDataHandler extends AbstractHandler {

		public MetaDataHandler(ContentHandler handler) {
			super(handler);
		}

		@Override
		public void init(String name, Attributes attributes) {

			logger.log(LogTracker.LOG_DEBUG, "Here is MetaDataHandler():init()"); //$NON-NLS-1$
			super.init(name, attributes);
			_dp_localization = attributes.getValue(LOCALIZATION);
			if (_dp_localization == null) {
				// Not a problem, because LOCALIZATION is an optional attribute.
			}
			// The global variable "_dp_localization" will be used within
			// OcdHandler and AttributeDefinitionHandler later.
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) {

			logger.log(LogTracker.LOG_DEBUG, "Here is MetaDataHandler:startElement():" //$NON-NLS-1$
					+ qName);
			String name = getName(localName, qName);
			if (name.equalsIgnoreCase(DESIGNATE)) {
				DesignateHandler designateHandler = new DesignateHandler(this);
				designateHandler.init(name, atts);
				if (designateHandler._isParsedDataValid) {
					_dp_designateHandlers.add(designateHandler);
				}
			} else if (name.equalsIgnoreCase(OCD)) {
				OcdHandler ocdHandler = new OcdHandler(this);
				ocdHandler.init(name, atts, _dp_OCDs);
			} else {
				logger.log(LogTracker.LOG_WARNING, NLS.bind(MetaTypeMsg.UNEXPECTED_ELEMENT, new Object[] { name,
						atts.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
			}
		}

		@Override
		protected void finished() {

			logger.log(LogTracker.LOG_DEBUG, "Here is MetaDataHandler():finished()"); //$NON-NLS-1$
			if (_dp_designateHandlers.size() == 0) {
				// Schema defines at least one DESIGNATE is required.
				_isParsedDataValid = false;
				logger.log(LogTracker.LOG_WARNING, NLS.bind(MetaTypeMsg.MISSING_ELEMENT, new Object[] { DESIGNATE,
						METADATA, elementId, _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
				return;
			}

			for (DesignateHandler dh : _dp_designateHandlers) {
				ObjectClassDefinitionImpl ocd = _dp_OCDs.get(dh._ocdref);
				if (ocd != null) {
					designates.add(new Designate.Builder(ocd).bundle(dh._bundle_val).factoryPid(dh._factory_val)
							.merge(dh._merge_val).pid(dh._pid_val).optional(dh._optional_val).build());
				} else {
					logger.log(LogTracker.LOG_ERROR,
							NLS.bind(MetaTypeMsg.OCD_REF_NOT_FOUND, new Object[] { dh._pid_val, dh._factory_val,
									dh._ocdref, _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
				}
			}
		}
	}

	/**
	 * Handler for the ObjectClassDefinition element.
	 */
	private class OcdHandler extends AbstractHandler {

		Map<String, ObjectClassDefinitionImpl> _parent_OCDs;
		// This ID "_refID" is only used for reference by Designate element,
		// not the PID or FPID of this OCD.
		String _refID;
		ObjectClassDefinitionImpl _ocd;
		List<AttributeDefinitionImpl> _ads = new ArrayList<>(7);
		List<Icon> icons = new ArrayList<>(5);

		public OcdHandler(ContentHandler handler) {
			super(handler);
		}

		public void init(String name, Attributes atts, Map<String, ObjectClassDefinitionImpl> ocds_hashtable) {

			logger.log(LogTracker.LOG_DEBUG, "Here is OcdHandler():init()"); //$NON-NLS-1$
			super.init(name, atts);
			_parent_OCDs = ocds_hashtable;
			collectExtensionAttributes(atts);
			String ocd_name_val = atts.getValue(NAME);
			if (ocd_name_val == null) {
				_isParsedDataValid = false;
				logger.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.MISSING_ATTRIBUTE, new Object[] { NAME, name,
						atts.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
				return;
			}

			String ocd_description_val = atts.getValue(DESCRIPTION);
			if (ocd_description_val == null) {
				// Not a problem, because DESCRIPTION is an optional attribute.
			}

			_refID = atts.getValue(ID);
			if (_refID == null) {
				_isParsedDataValid = false;
				logger.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.MISSING_ATTRIBUTE, new Object[] { ID, name,
						atts.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
				return;
			}

			_ocd = new ObjectClassDefinitionImpl(ocd_name_val, ocd_description_val, _refID, _dp_localization,
					extensionAttributes);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) {

			logger.log(LogTracker.LOG_DEBUG, "Here is OcdHandler:startElement():" //$NON-NLS-1$
					+ qName);
			if (!_isParsedDataValid)
				return;

			String name = getName(localName, qName);
			if (name.equalsIgnoreCase(AD)) {
				AttributeDefinitionHandler attributeDefHandler = new AttributeDefinitionHandler(this);
				attributeDefHandler.init(name, atts, _ads);
			} else if (name.equalsIgnoreCase(ICON)) {
				IconHandler iconHandler = new IconHandler(this);
				iconHandler.init(name, atts);
				if (iconHandler._isParsedDataValid)
					icons.add(iconHandler._icon);
			} else {
				logger.log(LogTracker.LOG_WARNING, NLS.bind(MetaTypeMsg.UNEXPECTED_ELEMENT, new Object[] { name,
						atts.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
			}
		}

		@Override
		protected void finished() {
			logger.log(LogTracker.LOG_DEBUG, "Here is OcdHandler():finished()"); //$NON-NLS-1$
			if (!_isParsedDataValid)
				return;
			// OCD gets all parsed ADs.
			for (AttributeDefinitionImpl ad : _ads) {
				_ocd.addAttributeDefinition(ad, ad.isRequired());
			}
			_ocd.setIcons(icons);
			_parent_OCDs.put(_refID, _ocd);
		}
	}

	/**
	 * Handler for the Icon element.
	 */
	private class IconHandler extends AbstractHandler {

		Icon _icon;

		public IconHandler(ContentHandler handler) {
			super(handler);
		}

		@Override
		public void init(String name, Attributes atts) {

			logger.log(LogTracker.LOG_DEBUG, "Here is IconHandler:init()"); //$NON-NLS-1$
			super.init(name, atts);
			String icon_resource_val = atts.getValue(RESOURCE);
			if (icon_resource_val == null) {
				_isParsedDataValid = false;
				logger.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.MISSING_ATTRIBUTE, new Object[] { RESOURCE, name,
						atts.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
				return;
			}

			String icon_size_val = atts.getValue(SIZE);
			if (icon_size_val == null) {
				// Not a problem, because SIZE is an optional attribute.
				icon_size_val = "0"; //$NON-NLS-1$
			} else if (icon_size_val.equalsIgnoreCase("")) { //$NON-NLS-1$
				icon_size_val = "0"; //$NON-NLS-1$
			}

			_icon = new Icon(icon_resource_val, Integer.parseInt(icon_size_val), _dp_bundle);
		}
	}

	/**
	 * Handler for the Attribute element.
	 */
	private class AttributeDefinitionHandler extends AbstractHandler {

		AttributeDefinitionImpl _ad;
		int _dataType;

		List<AttributeDefinitionImpl> _parent_ADs;
		ArrayList<String> _optionLabels = new ArrayList<>(7);
		ArrayList<String> _optionValues = new ArrayList<>(7);

		private String ad_defaults_str;

		public AttributeDefinitionHandler(ContentHandler handler) {
			super(handler);
		}

		@SuppressWarnings("deprecation")
		public void init(String name, Attributes atts, List<AttributeDefinitionImpl> ads) {

			logger.log(LogTracker.LOG_DEBUG, "Here is AttributeDefinitionHandler():init()"); //$NON-NLS-1$
			super.init(name, atts);
			_parent_ADs = ads;
			collectExtensionAttributes(atts);
			String ad_name_val = atts.getValue(NAME);
			if (ad_name_val == null) {
				// Not a problem, because NAME is an optional attribute.
			}

			String ad_description_val = atts.getValue(DESCRIPTION);
			if (ad_description_val == null) {
				// Not a problem, because DESCRIPTION is an optional attribute.
			}

			String ad_id_val = atts.getValue(ID);
			if (ad_id_val == null) {
				_isParsedDataValid = false;
				logger.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.MISSING_ATTRIBUTE, new Object[] { ID, name,
						atts.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
				return;
			}

			String ad_type_val = atts.getValue(TYPE);
			if (ad_type_val == null) {
				_isParsedDataValid = false;
				logger.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.MISSING_ATTRIBUTE, new Object[] { TYPE, name,
						atts.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
				return;
			}
			if (ad_type_val.equalsIgnoreCase(STRING)) {
				_dataType = AttributeDefinition.STRING;
			} else if (ad_type_val.equalsIgnoreCase(LONG)) {
				_dataType = AttributeDefinition.LONG;
			} else if (ad_type_val.equalsIgnoreCase(DOUBLE)) {
				_dataType = AttributeDefinition.DOUBLE;
			} else if (ad_type_val.equalsIgnoreCase(FLOAT)) {
				_dataType = AttributeDefinition.FLOAT;
			} else if (ad_type_val.equalsIgnoreCase(INTEGER)) {
				_dataType = AttributeDefinition.INTEGER;
			} else if (ad_type_val.equalsIgnoreCase(BYTE)) {
				_dataType = AttributeDefinition.BYTE;
			} else if (ad_type_val.equalsIgnoreCase(CHAR) || ad_type_val.equalsIgnoreCase(CHARACTER)) {
				_dataType = AttributeDefinition.CHARACTER;
			} else if (ad_type_val.equalsIgnoreCase(BOOLEAN)) {
				_dataType = AttributeDefinition.BOOLEAN;
			} else if (ad_type_val.equalsIgnoreCase(SHORT)) {
				_dataType = AttributeDefinition.SHORT;
			} else if (ad_type_val.equalsIgnoreCase(PASSWORD)) {
				_dataType = AttributeDefinition.PASSWORD;
			} else if (ad_type_val.equalsIgnoreCase(BIGDECIMAL)) {
				_dataType = AttributeDefinition.BIGDECIMAL;
			} else if (ad_type_val.equalsIgnoreCase(BIGINTEGER)) {
				_dataType = AttributeDefinition.BIGINTEGER;
			} else {
				_isParsedDataValid = false;
				logger.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.INVALID_TYPE, new Object[] { ad_type_val,
						ad_id_val, _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
				return;
			}

			String ad_cardinality_str = atts.getValue(CARDINALITY);
			int ad_cardinality_val = 0;
			if (ad_cardinality_str == null) {
				// Not a problem, because CARDINALITY is an optional attribute.
				// And the default value is 0.
			} else {
				ad_cardinality_val = Integer.parseInt(ad_cardinality_str);
			}

			String ad_min_val = atts.getValue(MIN);
			if (ad_min_val == null) {
				// Not a problem, because MIN is an optional attribute.
			}

			String ad_max_val = atts.getValue(MAX);
			if (ad_max_val == null) {
				// Not a problem, because MAX is an optional attribute.
			}

			ad_defaults_str = atts.getValue(DEFAULT);
			if (ad_defaults_str == null) {
				// Not a problem, because DEFAULT is an optional attribute.
			}

			String ad_required_val = atts.getValue(REQUIRED);
			if (ad_required_val == null) {
				// Not a problem, because REQUIRED is an optional attribute.
				// And the default value is 'true'.
				ad_required_val = Boolean.TRUE.toString();
			}

			_ad = new AttributeDefinitionImpl(ad_id_val, ad_name_val, ad_description_val, _dataType, ad_cardinality_val,
					convert(ad_min_val, _dataType), convert(ad_max_val, _dataType),
					Boolean.valueOf(ad_required_val).booleanValue(), _dp_localization, logger, extensionAttributes);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) {

			logger.log(LogTracker.LOG_DEBUG, "Here is AttributeDefinitionHandler:startElement():" //$NON-NLS-1$
					+ qName);
			if (!_isParsedDataValid)
				return;

			String name = getName(localName, qName);
			if (name.equalsIgnoreCase(OPTION)) {
				OptionHandler optionHandler = new OptionHandler(this);
				optionHandler.init(name, atts);
				if (optionHandler._isParsedDataValid) {
					// Only add valid Option
					_optionLabels.add(optionHandler._label_val);
					_optionValues.add(optionHandler._value_val);
				}
			} else {
				logger.log(LogTracker.LOG_WARNING, NLS.bind(MetaTypeMsg.UNEXPECTED_ELEMENT, new Object[] { name,
						atts.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
			}
		}

		@Override
		protected void finished() {

			logger.log(LogTracker.LOG_DEBUG, "Here is AttributeDefinitionHandler():finished()"); //$NON-NLS-1$
			if (!_isParsedDataValid)
				return;
			int numOfValues = _optionValues.size();
			_ad.setOption(_optionLabels, _optionValues, true);
			String[] values = _ad.getOptionValues();
			if (values == null)
				values = new String[0];
			if (numOfValues != values.length)
				logger.log(LogTracker.LOG_WARNING, NLS.bind(MetaTypeMsg.INVALID_OPTIONS_XML,
						new Object[] { elementId, _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));

			if (ad_defaults_str != null) {
				_ad.setDefaultValue(ad_defaults_str, true);
				if (_ad.getDefaultValue() == null)
					logger.log(LogTracker.LOG_WARNING,
							NLS.bind(MetaTypeMsg.INVALID_DEFAULTS_XML, new Object[] { ad_defaults_str, elementId,
									_dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
			}

			_parent_ADs.add(_ad);
		}
	}

	/**
	 * Handler for the Option element.
	 */
	private class OptionHandler extends AbstractHandler {

		String _label_val;
		String _value_val;

		public OptionHandler(ContentHandler handler) {
			super(handler);
		}

		@Override
		public void init(String name, Attributes atts) {

			logger.log(LogTracker.LOG_DEBUG, "Here is OptionHandler:init()"); //$NON-NLS-1$
			super.init(name, atts);
			_label_val = atts.getValue(LABEL);
			if (_label_val == null) {
				_isParsedDataValid = false;
				logger.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.MISSING_ATTRIBUTE, new Object[] { LABEL, name,
						atts.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
				return;
			}

			_value_val = atts.getValue(VALUE);
			if (_value_val == null) {
				_isParsedDataValid = false;
				logger.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.MISSING_ATTRIBUTE, new Object[] { VALUE, name,
						atts.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
				return;
			}
		}
	}

	/**
	 * Handler for the Designate element.
	 */
	class DesignateHandler extends AbstractHandler {

		String _pid_val = null;
		String _factory_val = null;
		String _bundle_val = null; // Only used by RFC94
		boolean _optional_val = false; // Only used by RFC94
		boolean _merge_val = false; // Only used by RFC94

		// Referenced OCD ID
		String _ocdref;

		public DesignateHandler(ContentHandler handler) {
			super(handler);
		}

		@Override
		public void init(String name, Attributes atts) {
			logger.log(LogTracker.LOG_DEBUG, "Here is DesignateHandler():init()"); //$NON-NLS-1$
			super.init(name, atts);
			_pid_val = atts.getValue(PID);
			_factory_val = atts.getValue(FACTORY);
			if (_pid_val == null && _factory_val == null) {
				_isParsedDataValid = false;
				logger.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.MISSING_DESIGNATE_PID_AND_FACTORYPID,
						new Object[] { elementId, _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
				return;
			}

			_bundle_val = atts.getValue(BUNDLE);
			if (_bundle_val == null) {
				// Not a problem because BUNDLE is an optional attribute.
			}

			String optional_str = atts.getValue(OPTIONAL);
			if (optional_str == null) {
				// Not a problem, because OPTIONAL is an optional attribute.
				// The default value is "false".
				_optional_val = false;
			} else {
				_optional_val = Boolean.valueOf(optional_str).booleanValue();
			}

			String merge_str = atts.getValue(MERGE);
			if (merge_str == null) {
				// Not a problem, because MERGE is an optional attribute.
				// The default value is "false".
				_merge_val = false;
			} else {
				_merge_val = Boolean.valueOf(merge_str).booleanValue();
			}
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) {

			logger.log(LogTracker.LOG_DEBUG, "Here is DesignateHandler:startElement():" //$NON-NLS-1$
					+ qName);
			if (!_isParsedDataValid)
				return;

			String name = getName(localName, qName);
			if (name.equalsIgnoreCase(OBJECT)) {
				ObjectHandler objectHandler = new ObjectHandler(this);
				objectHandler.init(name, atts);
				if (objectHandler._isParsedDataValid) {
					_ocdref = objectHandler._ocdref;
				}
			} else {
				logger.log(LogTracker.LOG_WARNING, NLS.bind(MetaTypeMsg.UNEXPECTED_ELEMENT, new Object[] { name,
						atts.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
			}
		}

		@Override
		protected void finished() {

			logger.log(LogTracker.LOG_DEBUG, "Here is DesignateHandler():finished()"); //$NON-NLS-1$
			if (!_isParsedDataValid)
				return;

			if (_ocdref == null) {
				_isParsedDataValid = false;
				// Schema defines at least one OBJECT is required.
				logger.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.MISSING_ELEMENT, new Object[] { OBJECT, DESIGNATE,
						elementId, _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
				return;

			}
		}
	}

	/**
	 * Handler for the Object element.
	 */
	private class ObjectHandler extends AbstractHandler {

		String _ocdref;

		public ObjectHandler(ContentHandler handler) {
			super(handler);
		}

		@Override
		public void init(String name, Attributes atts) {

			logger.log(LogTracker.LOG_DEBUG, "Here is ObjectHandler():init()"); //$NON-NLS-1$
			super.init(name, atts);
			_ocdref = atts.getValue(OCDREF);
			if (_ocdref == null) {
				_isParsedDataValid = false;
				logger.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.MISSING_ATTRIBUTE, new Object[] { OCDREF, name,
						atts.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
				return;
			}
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes atts) {

			logger.log(LogTracker.LOG_DEBUG, "Here is ObjectHandler:startElement():" //$NON-NLS-1$
					+ qName);
			if (!_isParsedDataValid)
				return;

			String name = getName(localName, qName);
			if (name.equalsIgnoreCase(ATTRIBUTE)) {
				AttributeHandler attributeHandler = new AttributeHandler(this);
				attributeHandler.init(name, atts);
				// The ATTRIBUTE element is only used by RFC94, do nothing for it here.
			} else {
				logger.log(LogTracker.LOG_WARNING, NLS.bind(MetaTypeMsg.UNEXPECTED_ELEMENT, new Object[] { name,
						atts.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
			}
		}
	}

	/**
	 * Handler for the Attribute element.
	 * 
	 * This Handler is only used by RFC94.
	 */
	private class AttributeHandler extends AbstractHandler {

		String _adref_val;
		String _content_val;

		public AttributeHandler(ContentHandler handler) {
			super(handler);
		}

		@Override
		public void init(String name, Attributes atts) {

			logger.log(LogTracker.LOG_DEBUG, "Here is AttributeHandler():init()"); //$NON-NLS-1$
			super.init(name, atts);
			_adref_val = atts.getValue(ADREF);
			if (_adref_val == null) {
				_isParsedDataValid = false;
				logger.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.MISSING_ATTRIBUTE, new Object[] { ADREF, name,
						atts.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
				return;
			}

			_content_val = atts.getValue(CONTENT);
			if (_content_val == null) {
				_isParsedDataValid = false;
				logger.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.MISSING_ATTRIBUTE, new Object[] { CONTENT, name,
						atts.getValue(ID), _dp_url, _dp_bundle.getBundleId(), _dp_bundle.getSymbolicName() }));
				return;
			}
		}
	}

	/**
	 * Error Handler to report errors and warnings
	 */
	private static class MyErrorHandler implements ErrorHandler {

		/** Error handler output goes here */
		private PrintStream _out;

		MyErrorHandler(PrintStream out) {
			this._out = out;
		}

		/**
		 * Returns a string describing parse exception details
		 */
		private String getParseExceptionInfo(SAXParseException spe) {
			String systemId = spe.getSystemId();
			if (systemId == null) {
				systemId = "null"; //$NON-NLS-1$
			}
			String info = "URI=" + systemId + //$NON-NLS-1$
					" Line=" + spe.getLineNumber() + //$NON-NLS-1$
					": " + spe.getMessage(); //$NON-NLS-1$

			return info;
		}

		// The following methods are standard SAX ErrorHandler methods.
		// See SAX documentation for more info.

		@Override
		public void warning(SAXParseException spe) {
			_out.println("Warning: " + getParseExceptionInfo(spe)); //$NON-NLS-1$
		}

		@Override
		public void error(SAXParseException spe) throws SAXException {
			String message = "Error: " + getParseExceptionInfo(spe); //$NON-NLS-1$
			throw new SAXException(message);
		}

		@Override
		public void fatalError(SAXParseException spe) throws SAXException {
			String message = "Fatal Error: " + getParseExceptionInfo(spe); //$NON-NLS-1$
			throw new SAXException(message);
		}
	}

	public static String getName(String localName, String qName) {
		if (localName != null && localName.length() > 0) {
			return localName;
		}

		int nameSpaceIndex = qName.indexOf(":"); //$NON-NLS-1$
		return nameSpaceIndex == -1 ? qName : qName.substring(nameSpaceIndex + 1);
	}
}
