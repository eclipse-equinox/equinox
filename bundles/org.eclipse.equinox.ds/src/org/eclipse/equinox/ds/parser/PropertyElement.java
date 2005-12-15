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
package org.eclipse.equinox.ds.parser;

import java.util.*;
import org.eclipse.equinox.ds.model.ComponentDescription;
import org.eclipse.equinox.ds.model.PropertyValueDescription;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

class PropertyElement extends DefaultHandler {
	private ParserHandler root;
	private ComponentElement parent;
	private PropertyValueDescription property;
	private List values;

	PropertyElement(ParserHandler root, ComponentElement parent, Attributes attributes) {
		this.root = root;
		this.parent = parent;
		property = new PropertyValueDescription();
		values = new ArrayList();

		int size = attributes.getLength();
		for (int i = 0; i < size; i++) {
			String key = attributes.getQName(i);
			String value = attributes.getValue(i);

			if (key.equals(ParserConstants.NAME_ATTRIBUTE)) {
				property.setName(value);
				continue;
			}

			if (key.equals(ParserConstants.VALUE_ATTRIBUTE)) {
				property.setValue(value);
				continue;
			}

			if (key.equals(ParserConstants.TYPE_ATTRIBUTE)) {
				property.setType(value);
				continue;
			}
			root.logError("unrecognized properties element attribute: " + key);
		}

		if (property.getName() == null) {
			root.logError("property name not specified");
		}
	}

	public void startElement(String uri, String localName, String qName, Attributes attributes) {
		root.logError("property does not support nested elements");
	}

	public void characters(char[] ch, int start, int length) {
		int end = start + length;
		int cursor = start;
		while (cursor < end) {
			if (ch[cursor] == '\n') {
				charLine(ch, start, cursor - start);
				start = cursor;
			}
			cursor++;
		}
		charLine(ch, start, cursor - start);
	}

	private void charLine(char[] ch, int start, int length) {
		if (length > 0) {
			String line = new String(ch, start, length).trim();
			if (line.length() > 0) {
				values.add(line);
			}
		}
	}

	public void endElement(String uri, String localName, String qName) {

		int size = values.size();

		// If the value attribute is specified, then body of the property
		// element is ignored.
		if (property.getValue() != null) {
			if (size > 0) {

				root.logError("If the value attribute is specified, the body of the property element is ignored. key = " + property.getName() + " value = " + property.getValue());
			}

			//parse the value according to the type
			String type = property.getType();
			if (type == null || type.equals("String")) {
				//value is already a string
			} else if (type.equals("Long")) {
				property.setValue(Long.valueOf((String) property.getValue(), 10));
			} else if (type.equals("Double")) {
				property.setValue(Double.valueOf((String) property.getValue()));
			} else if (type.equals("Float")) {
				property.setValue(Float.valueOf((String) property.getValue()));
			} else if (type.equals("Integer")) {
				property.setValue(Integer.valueOf((String) property.getValue()));
			} else if (type.equals("Byte")) {
				property.setValue(Byte.valueOf((String) property.getValue(), 10));
			} else if (type.equals("Char")) {
				property.setValue(new Character(((String) property.getValue()).charAt(0)));
			} else if (type.equals("Boolean")) {
				property.setValue(Boolean.valueOf((String) property.getValue()));
			} else if (type.equals("Short")) {
				property.setValue(Short.valueOf((String) property.getValue(), 10));
			}

			// if characters were specified ( values are specifed in the body of
			// the property element )
		} else if (size > 0) {
			// if String then store as String[]
			if (property.getType().equals("String")) {
				String[] result = new String[size];
				values.toArray(result);
				property.setValue(result);
			} else if (property.getType().equals("Integer")) {
				int[] result = new int[size];
				if (values != null) {
					Iterator it = values.iterator();
					int i = 0;
					while (it.hasNext()) {
						Integer value = new Integer((String) it.next());
						result[i++] = value.intValue();
					}
					property.setValue(result);
				}
			} else if (property.getType().equals("Long")) {
				long[] result = new long[size];
				if (values != null) {
					Iterator it = values.iterator();
					int i = 0;
					while (it.hasNext()) {
						Long value = new Long((String) it.next());
						result[i++] = value.longValue();
					}
					property.setValue(result);
				}
			} else if (property.getType().equals("Double")) {
				double[] result = new double[size];
				if (values != null) {
					Iterator it = values.iterator();
					int i = 0;
					while (it.hasNext()) {
						Double value = new Double((String) it.next());
						result[i++] = value.doubleValue();
					}
					property.setValue(result);
				}
			} else if (property.getType().equals("Float")) {
				float[] result = new float[size];
				if (values != null) {
					Iterator it = values.iterator();
					int i = 0;
					while (it.hasNext()) {
						Float value = new Float((String) it.next());
						result[i++] = value.floatValue();
					}
					property.setValue(result);
				}
			} else if (property.getType().equals("Byte")) {
				byte[] result = new byte[size];
				if (values != null) {
					Iterator it = values.iterator();
					int i = 0;
					while (it.hasNext()) {
						Byte value = new Byte((String) it.next());
						result[i++] = value.byteValue();
					}
					property.setValue(result);
				}
			} else if (property.getType().equals("Char")) {
				char[] result = new char[size];
				if (values != null) {
					Iterator it = values.iterator();
					int i = 0;
					while (it.hasNext()) {
						char[] value = ((String) it.next()).toCharArray();
						result[i++] = value[0];
					}
					property.setValue(result);
				}
			} else if (property.getType().equals("Boolean")) {
				boolean[] result = new boolean[size];
				if (values != null) {
					Iterator it = values.iterator();
					int i = 0;
					while (it.hasNext()) {
						Boolean value = new Boolean((String) it.next());
						result[i++] = value.booleanValue();
					}
					property.setValue(result);
				}
			} else if (property.getType().equals("Short")) {
				short[] result = new short[size];
				if (values != null) {
					Iterator it = values.iterator();
					int i = 0;
					while (it.hasNext()) {
						Short value = new Short((String) it.next());
						result[i++] = value.shortValue();
					}
					property.setValue(result);
				}
			}

		}

		ComponentDescription component = parent.getComponentDescription();
		component.addPropertyDescription(property);
		root.setHandler(parent);
	}
}
