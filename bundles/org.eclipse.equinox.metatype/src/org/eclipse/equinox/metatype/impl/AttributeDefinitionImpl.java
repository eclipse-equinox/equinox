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

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.eclipse.equinox.metatype.EquinoxAttributeDefinition;
import org.eclipse.equinox.metatype.impl.Persistence.Reader;
import org.eclipse.equinox.metatype.impl.Persistence.Writer;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.metatype.AttributeDefinition;

/**
 * Implementation of AttributeDefintion
 */
public class AttributeDefinitionImpl extends LocalizationElement implements EquinoxAttributeDefinition, Cloneable {

	private final String _name;
	private final String _id;
	private final String _description;
	private final int _cardinality;
	private final int _dataType;
	private final Object _minValue;
	private final Object _maxValue;
	private final boolean _isRequired;

	private final LogTracker logger;
	private final ExtendableHelper helper;

	private volatile String[] _defaults = null;
	private volatile ArrayList<String> _values = new ArrayList<>(7);
	private volatile ArrayList<String> _labels = new ArrayList<>(7);

	/**
	 * Constructor of class AttributeDefinitionImpl.
	 */
	public AttributeDefinitionImpl(String id, String name, String description, int type, int cardinality, Object min, Object max, boolean isRequired, String localization, LogTracker logger, Map<String, Map<String, String>> extensionAttributes) {
		this(id, name, description, type, cardinality, min, max, isRequired, localization, logger, new ExtendableHelper(extensionAttributes));
	}

	private AttributeDefinitionImpl(String id, String name, String description, int type, int cardinality, Object min, Object max, boolean isRequired, String localization, LogTracker logger, ExtendableHelper helper) {
		super(localization);
		this._id = id;
		this._name = name;
		this._description = description;
		this._dataType = type;
		this._cardinality = cardinality;
		this._minValue = min;
		this._maxValue = max;
		this._isRequired = isRequired;
		this.logger = logger;
		this.helper = helper;
	}

	/*
	 * 
	 */
	@Override
	public Object clone() {

		AttributeDefinitionImpl ad = new AttributeDefinitionImpl(_id, _name, _description, _dataType, _cardinality, _minValue, _maxValue, _isRequired, getLocalization(), logger, helper);

		String[] curDefaults = _defaults;
		if (curDefaults != null) {
			ad.setDefaultValue(curDefaults.clone());
		}

		@SuppressWarnings("unchecked")
		ArrayList<String> labels = (ArrayList<String>) _labels.clone();
		@SuppressWarnings("unchecked")
		ArrayList<String> values = (ArrayList<String>) _values.clone();
		ad.setOption(labels, values, false);

		return ad;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.AttributeDefinition#getName()
	 */
	public String getName() {
		return getLocalized(_name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.AttributeDefinition#getID()
	 */
	public String getID() {
		return _id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.AttributeDefinition#getDescription()
	 */
	public String getDescription() {
		return getLocalized(_description);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.AttributeDefinition#getCardinality()
	 */
	public int getCardinality() {
		return _cardinality;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.AttributeDefinition#getType()
	 */
	public int getType() {
		return _dataType;
	}

	/**
	 * Method to get the required flag of AttributeDefinition.
	 */
	boolean isRequired() {
		return _isRequired;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.AttributeDefinition#getOptionLabels()
	 */
	public String[] getOptionLabels() {

		List<String> curLabels = _labels;
		if (curLabels.isEmpty()) {
			return null;
		}

		String[] returnedLabels = new String[curLabels.size()];
		int i = 0;
		for (String labelKey : curLabels) {
			returnedLabels[i] = getLocalized(labelKey);
			i++;
		}
		return returnedLabels;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.AttributeDefinition#getOptionValues()
	 */
	public String[] getOptionValues() {
		List<String> curValues = _values;

		if (curValues.isEmpty()) {
			return null;
		}

		return curValues.toArray(MetaTypeInformationImpl.emptyStringArray);
	}

	/**
	 * Method to set the Option values of AttributeDefinition.
	 */
	void setOption(ArrayList<String> labels, ArrayList<String> values, boolean needValidation) {
		if ((labels == null) || (values == null)) {
			logger.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.NULL_OPTIONS, getID()));
			return;
		}
		if (labels.size() != values.size()) {
			logger.log(LogTracker.LOG_ERROR, NLS.bind(MetaTypeMsg.INCONSISTENT_OPTIONS, getID()));
			return;
		}
		if (needValidation) {
			for (int index = 0; index < values.size(); index++) {
				ValueTokenizer vt = new ValueTokenizer(values.get(index), logger);
				values.set(index, vt.getValuesAsString());
				String reason = vt.validate(this);
				if ((reason != null) && reason.length() > 0) {
					logger.log(LogTracker.LOG_WARNING, NLS.bind(MetaTypeMsg.INVALID_OPTIONS, new Object[] {values.get(index), getID(), reason}));
					labels.remove(index);
					values.remove(index);
					index--; // Because this one has been removed.
				}
			}
		}
		_labels = labels;
		_values = values;
	}

	boolean containsInvalidValue(String value) {
		List<String> curValues = _values;
		return !curValues.isEmpty() && !curValues.contains(value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.metatype.AttributeDefinition#getDefaultValue()
	 */
	public String[] getDefaultValue() {
		return _defaults;
	}

	/**
	 * Method to set the default value of AttributeDefinition.
	 * The given parameter is a comma delimited list needed to be parsed.
	 */
	void setDefaultValue(String defaults_str, boolean needValidation) {
		ValueTokenizer vt = new ValueTokenizer(defaults_str, logger);
		String reason = vt.validate(this);
		if ((reason != null) && reason.length() > 0) {
			logger.log(LogTracker.LOG_WARNING, NLS.bind(MetaTypeMsg.INVALID_DEFAULTS, new Object[] {vt.getValuesAsString(), getID(), reason}));
			return;
		}
		String[] defaults = vt.getValuesAsArray();
		// If the default value is a single empty string and cardinality != 0, the default value must become String[0].
		// We know the cardinality has already been set in the constructor.
		if (_cardinality != 0 && defaults.length == 1 && defaults[0].length() == 0)
			setDefaultValue(new String[0]);
		else
			setDefaultValue(vt.getValuesAsArray());
	}

	/**
	 * Method to set the default value of AttributeDefinition.
	 * The given parameter is a String array of multi values.
	 */
	private void setDefaultValue(String[] defaults) {
		_defaults = defaults;
	}

	/*
	 * (non-Javadoc)
	 * In order to be valid, a value must pass all of the following tests.
	 * (1) The value must not be null.
	 * (2) The value must be convertible into the attribute definition's type.
	 * (3) The following relation must hold: min <= value <= max, if either min or max was specified.
	 * (4) If options were specified, the value must be equal to one of them.
	 * 
	 * Note this method will never return null to indicate there's no validation
	 * present. The type compatibility check can always be performed.
	 * 
	 * @see org.osgi.service.metatype.AttributeDefinition#validate(java.lang.String)
	 */
	public String validate(String value) {
		ValueTokenizer vt = new ValueTokenizer(value, logger);
		return vt.validate(this);
	}

	@Override
	public Map<String, String> getExtensionAttributes(String schema) {
		return helper.getExtensionAttributes(schema);
	}

	@Override
	public Set<String> getExtensionUris() {
		return helper.getExtensionUris();
	}

	@Override
	public String getMax() {
		return _maxValue == null ? null : String.valueOf(_maxValue);
	}

	@Override
	public String getMin() {
		return _minValue == null ? null : String.valueOf(_minValue);
	}

	Object getMaxValue() {
		return _maxValue;
	}

	Object getMinValue() {
		return _minValue;
	}

	void getStrings(Set<String> strings) {
		String[] curDefaults = _defaults;
		if (curDefaults != null) {
			for (String string : curDefaults) {
				strings.add(string);
			}
		}
		strings.add(_description);
		strings.add(_id);
		strings.add(_name);
		strings.add(getLocalization());
		for (String string : _values) {
			strings.add(string);
		}

		for (String string : _labels) {
			strings.add(string);
		}
		helper.getStrings(strings);
		if (getType() == AttributeDefinition.STRING || getType() == AttributeDefinition.PASSWORD) {
			if (_maxValue != null) {
				strings.add(getMax());
			}
			if (_minValue != null) {
				strings.add(getMin());
			}
		}
	}

	public static AttributeDefinitionImpl load(Reader reader, LogTracker logger) throws IOException {
		String id = reader.readString();
		String description = reader.readString();
		String name = reader.readString();
		int type = reader.readInt();
		int cardinality = reader.readInt();
		boolean isRequired = reader.readBoolean();
		String localization = reader.readString();

		String[] defaults = null;
		if (reader.readBoolean()) {
			int numDefaults = reader.readInt();
			defaults = new String[numDefaults];
			for (int i = 0; i < numDefaults; i++) {
				defaults[i] = reader.readString();
			}
		}
		int numLabels = reader.readInt();
		ArrayList<String> labels = new ArrayList<>(numLabels);
		for (int i = 0; i < numLabels; i++) {
			labels.add(reader.readString());
		}
		int numValues = reader.readInt();
		ArrayList<String> values = new ArrayList<>();
		for (int i = 0; i < numValues; i++) {
			values.add(reader.readString());
		}
		ExtendableHelper helper = ExtendableHelper.load(reader);
		Object min = readMinMax(type, reader);
		Object max = readMinMax(type, reader);

		AttributeDefinitionImpl result = new AttributeDefinitionImpl(id, name, description, type, cardinality, min, max, isRequired, localization, logger, helper);
		result.setDefaultValue(defaults);
		result.setOption(labels, values, false);
		return result;
	}

	public void write(Writer writer) throws IOException {
		writer.writeString(_id);
		writer.writeString(_description);
		writer.writeString(_name);
		writer.writeInt(_dataType);
		writer.writeInt(_cardinality);
		writer.writeBoolean(_isRequired);
		writer.writeString(getLocalization());
		String[] curDefaults = _defaults;
		if (curDefaults == null) {
			writer.writeBoolean(false);
		} else {
			writer.writeBoolean(true);
			writer.writeInt(curDefaults.length);
			for (String defaultValue : curDefaults) {
				writer.writeString(defaultValue);
			}
		}
		List<String> curLabels = _labels;
		writer.writeInt(curLabels.size());
		for (String label : curLabels) {
			writer.writeString(label);
		}
		List<String> curValues = _values;
		writer.writeInt(curValues.size());
		for (String value : curValues) {
			writer.writeString(value);
		}
		helper.write(writer);
		writeMinMax(_minValue, writer);
		writeMinMax(_maxValue, writer);
	}

	@SuppressWarnings("deprecation")
	private static Object readMinMax(int dataType, Reader reader) throws IOException {
		boolean isNull = reader.readBoolean();
		if (isNull) {
			return null;
		}
		switch (dataType) {
			// PASSWORD should be treated like STRING.
			case AttributeDefinition.PASSWORD :
			case AttributeDefinition.STRING :
				return reader.readString();
			case AttributeDefinition.LONG :
				return reader.readLong();
			case AttributeDefinition.INTEGER :
				return reader.readInt();
			case AttributeDefinition.SHORT :
				return reader.readShort();
			case AttributeDefinition.CHARACTER :
				return reader.readCharacter();
			case AttributeDefinition.BYTE :
				return reader.readByte();
			case AttributeDefinition.DOUBLE :
				return reader.readDouble();
			case AttributeDefinition.FLOAT :
				return reader.readFloat();
			case AttributeDefinition.BIGINTEGER :
				return new BigInteger(reader.readString());
			case AttributeDefinition.BIGDECIMAL :
				return new BigDecimal(reader.readString());
			case AttributeDefinition.BOOLEAN :
				return reader.readBoolean();
			default :
				return reader.readString();
		}
	}

	@SuppressWarnings("deprecation")
	private void writeMinMax(Object v, Writer writer) throws IOException {
		if (v == null) {
			writer.writeBoolean(true);
			return;
		}
		writer.writeBoolean(false);
		switch (_dataType) {
			// PASSWORD should be treated like STRING.
			case AttributeDefinition.PASSWORD :
			case AttributeDefinition.STRING :
				writer.writeString((String) v);
				return;
			case AttributeDefinition.LONG :
				writer.writeLong((Long) v);
				return;
			case AttributeDefinition.INTEGER :
				writer.writeInt((Integer) v);
				return;
			case AttributeDefinition.SHORT :
				writer.writeShort((Short) v);
				return;
			case AttributeDefinition.CHARACTER :
				writer.writeCharacter((Character) v);
				return;
			case AttributeDefinition.BYTE :
				writer.writeByte((Byte) v);
				return;
			case AttributeDefinition.DOUBLE :
				writer.writeDouble((Double) v);
				return;
			case AttributeDefinition.FLOAT :
				writer.writeFloat((Float) v);
				return;
			case AttributeDefinition.BIGINTEGER :
				writer.writeString(v.toString());
				return;
			case AttributeDefinition.BIGDECIMAL :
				writer.writeString(v.toString());
				return;
			case AttributeDefinition.BOOLEAN :
				writer.writeBoolean((Boolean) v);
				return;
			default :
				// Unknown data type
				writer.writeString(String.valueOf(v));
				return;
		}
	}

}
