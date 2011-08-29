/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.resolver;

import java.util.*;
import org.osgi.framework.InvalidSyntaxException;

public class FilterParser {
	public static class FilterComponent {
		/* filter operators */
		public static final int EQUAL = 1;
		public static final int APPROX = 2;
		public static final int GREATER = 3;
		public static final int LESS = 4;
		public static final int AND = 7;
		public static final int OR = 8;
		public static final int NOT = 9;

		private final int op;
		private final String attr;
		private final String value;
		private final List<FilterComponent> nested;

		public FilterComponent(int op, List<FilterComponent> nested) {
			this.op = op;
			this.attr = null;
			this.value = null;
			this.nested = nested;
		}

		public FilterComponent(int op, String attr, String value) {
			this.op = op;
			this.attr = attr;
			this.value = value;
			this.nested = Collections.emptyList();
		}

		public int getOp() {
			return op;
		}

		public String getAttr() {
			return attr;
		}

		public String getValue() {
			return value;
		}

		public List<FilterComponent> getNested() {
			return nested;
		}

		public Map<String, String> getStandardOSGiAttributes(String... versions) {
			@SuppressWarnings("unchecked")
			Collection<String> versionAttrs = (Collection<String>) (versions == null ? Collections.emptyList() : Arrays.asList(versions));
			if (op != AND && op != EQUAL)
				throw new IllegalStateException("Invalid filter for Starndard OSGi Attributes: " + op); //$NON-NLS-1$
			Map<String, String> result = new HashMap<String, String>();
			addAttributes(result, versionAttrs, false);
			// fix >= ranges (e.g. Import-Package: foo; version=1.0 == (&(osgi.wiring.package=foo)(version>=0))
			for (String versionAttr : versionAttrs) {
				String range = result.get(versionAttr);
				if (range != null && range.startsWith("[") && !(range.endsWith(")") || range.endsWith("]"))) {
					range = range.substring(1);
					result.put(versionAttr, range);
				}
			}
			return result;
		}

		private void addAttributes(Map<String, String> attributes, Collection<String> versionAttrs, boolean not) {
			if (op == EQUAL) {
				if (!versionAttrs.contains(attr)) {
					attributes.put(attr, value);
				} else {
					// this is an exact range e.g. [value,value]
					String currentRange = attributes.get(attr);
					if (currentRange != null || not)
						throw new IllegalStateException("Invalid range for: " + attr); //$NON-NLS-1$
					attributes.put(attr, '[' + value + ',' + value + ']');
				}
			} else if (op == LESS) {
				if (!versionAttrs.contains(attr))
					throw new IllegalStateException("Invalid attribute: " + attr); //$NON-NLS-1$
				String currentRange = attributes.get(attr);
				boolean rangeStart = currentRange != null && (currentRange.startsWith("(") || currentRange.startsWith("[")); //$NON-NLS-1$ //$NON-NLS-2$
				boolean rangeEnd = currentRange != null && (currentRange.endsWith(")") || currentRange.endsWith("]")); //$NON-NLS-1$ //$NON-NLS-2$
				if (rangeStart && rangeEnd) {
					throw new IllegalStateException("range is already defined for attribute: " + attr); //$NON-NLS-1$
				}
				if (not) {
					// this must be a range start "(value"
					if (rangeStart)
						throw new IllegalStateException("range start is already processed for attribute: " + attr); //$NON-NLS-1$
					currentRange = '(' + value + (rangeEnd ? (',' + currentRange) : ""); //$NON-NLS-1$
				} else {
					// this must be a range end "value]"
					if (rangeEnd)
						throw new IllegalStateException("range end is already processed for attribute: " + attr); //$NON-NLS-1$
					currentRange = (rangeStart ? (currentRange + ',') : "") + value + ']'; //$NON-NLS-1$
				}
				if (currentRange != null)
					attributes.put(attr, currentRange);
			} else if (op == GREATER) {
				if (!versionAttrs.contains(attr))
					throw new IllegalStateException("Invalid attribute: " + attr); //$NON-NLS-1$
				String currentRange = attributes.get(attr);
				boolean rangeStart = currentRange != null && (currentRange.startsWith("(") || currentRange.startsWith("[")); //$NON-NLS-1$ //$NON-NLS-2$
				boolean rangeEnd = currentRange != null && (currentRange.endsWith(")") || currentRange.endsWith("]")); //$NON-NLS-1$ //$NON-NLS-2$
				if (rangeStart && rangeEnd) {
					throw new IllegalStateException("range is already defined for attribute: " + attr); //$NON-NLS-1$
				}
				if (not) {
					// this must be a range end "value)"
					if (rangeEnd)
						throw new IllegalStateException("range end is already processed for attribute: " + attr); //$NON-NLS-1$
					currentRange = (rangeStart ? (currentRange + ',') : "") + value + ')'; //$NON-NLS-1$
				} else {
					// this must be a range start "[value"
					if (rangeStart)
						throw new IllegalStateException("range start is already processed for attribute: " + attr); //$NON-NLS-1$
					currentRange = '[' + value + (rangeEnd ? (',' + currentRange) : ""); //$NON-NLS-1$
				}
				if (currentRange != null)
					attributes.put(attr, currentRange);
			} else if (op == AND) {
				for (FilterComponent component : nested) {
					component.addAttributes(attributes, versionAttrs, false);
				}
			} else if (op == NOT) {
				nested.get(0).addAttributes(attributes, versionAttrs, true);
			} else {
				throw new IllegalStateException("Invalid filter for standard OSGi requirements: " + op);
			}
		}
	}

	private final String filterstring;
	private final char[] filterChars;
	private int pos;

	public FilterParser(String filterstring) {
		this.filterstring = filterstring;
		filterChars = filterstring.toCharArray();
		pos = 0;
	}

	public FilterComponent parse() throws InvalidSyntaxException {
		FilterComponent filter;
		try {
			filter = parse_filter();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new InvalidSyntaxException("Filter ended abruptly", filterstring, e);
		}

		if (pos != filterChars.length) {
			throw new InvalidSyntaxException("Extraneous trailing characters: " + filterstring.substring(pos), filterstring);
		}
		return filter;
	}

	private FilterComponent parse_filter() throws InvalidSyntaxException {
		FilterComponent filter;
		skipWhiteSpace();

		if (filterChars[pos] != '(') {
			throw new InvalidSyntaxException("Missing '(': " + filterstring.substring(pos), filterstring);
		}

		pos++;

		filter = parse_filtercomp();

		skipWhiteSpace();

		if (filterChars[pos] != ')') {
			throw new InvalidSyntaxException("Missing ')': " + filterstring.substring(pos), filterstring);
		}

		pos++;

		skipWhiteSpace();

		return filter;
	}

	private FilterComponent parse_filtercomp() throws InvalidSyntaxException {
		skipWhiteSpace();

		char c = filterChars[pos];

		switch (c) {
			case '&' : {
				pos++;
				return parse_and();
			}
			case '|' : {
				pos++;
				return parse_or();
			}
			case '!' : {
				pos++;
				return parse_not();
			}
		}
		return parse_item();
	}

	private FilterComponent parse_and() throws InvalidSyntaxException {
		int lookahead = pos;
		skipWhiteSpace();

		if (filterChars[pos] != '(') {
			pos = lookahead - 1;
			return parse_item();
		}

		List<FilterComponent> operands = new ArrayList<FilterComponent>(10);

		while (filterChars[pos] == '(') {
			FilterComponent child = parse_filter();
			operands.add(child);
		}

		return new FilterComponent(FilterComponent.AND, operands);
	}

	private FilterComponent parse_or() throws InvalidSyntaxException {
		int lookahead = pos;
		skipWhiteSpace();

		if (filterChars[pos] != '(') {
			pos = lookahead - 1;
			return parse_item();
		}

		List<FilterComponent> operands = new ArrayList<FilterComponent>(10);

		while (filterChars[pos] == '(') {
			FilterComponent child = parse_filter();
			operands.add(child);
		}

		return new FilterComponent(FilterComponent.OR, operands);
	}

	private FilterComponent parse_not() throws InvalidSyntaxException {
		int lookahead = pos;
		skipWhiteSpace();

		if (filterChars[pos] != '(') {
			pos = lookahead - 1;
			return parse_item();
		}

		List<FilterComponent> operands = new ArrayList<FilterComponent>(1);
		FilterComponent child = parse_filter();
		operands.add(child);

		return new FilterComponent(FilterComponent.NOT, operands);
	}

	private FilterComponent parse_item() throws InvalidSyntaxException {
		String attr = parse_attr();

		skipWhiteSpace();

		switch (filterChars[pos]) {
			case '~' : {
				if (filterChars[pos + 1] == '=') {
					pos += 2;
					return new FilterComponent(FilterComponent.APPROX, attr, parse_value());
				}
				break;
			}
			case '>' : {
				if (filterChars[pos + 1] == '=') {
					pos += 2;
					return new FilterComponent(FilterComponent.GREATER, attr, parse_value());
				}
				break;
			}
			case '<' : {
				if (filterChars[pos + 1] == '=') {
					pos += 2;
					return new FilterComponent(FilterComponent.LESS, attr, parse_value());
				}
				break;
			}
			case '=' : {
				pos++;
				return new FilterComponent(FilterComponent.EQUAL, attr, parse_value());
			}
		}

		throw new InvalidSyntaxException("Invalid operator: " + filterstring.substring(pos), filterstring);
	}

	private String parse_attr() throws InvalidSyntaxException {
		skipWhiteSpace();

		int begin = pos;
		int end = pos;

		char c = filterChars[pos];

		while (c != '~' && c != '<' && c != '>' && c != '=' && c != '(' && c != ')') {
			pos++;

			if (!Character.isWhitespace(c)) {
				end = pos;
			}

			c = filterChars[pos];
		}

		int length = end - begin;

		if (length == 0) {
			throw new InvalidSyntaxException("Missing attr: " + filterstring.substring(pos), filterstring);
		}

		return new String(filterChars, begin, length);
	}

	private String parse_value() throws InvalidSyntaxException {
		StringBuffer sb = new StringBuffer(filterChars.length - pos);

		parseloop: while (true) {
			char c = filterChars[pos];

			switch (c) {
				case ')' : {
					break parseloop;
				}

				case '(' : {
					throw new InvalidSyntaxException("Invalid value: " + filterstring.substring(pos), filterstring);
				}

				case '\\' : {
					pos++;
					c = filterChars[pos];
					/* fall through into default */
				}

				default : {
					sb.append(c);
					pos++;
					break;
				}
			}
		}

		if (sb.length() == 0) {
			throw new InvalidSyntaxException("Missing value: " + filterstring.substring(pos), filterstring);
		}

		return sb.toString();
	}

	private void skipWhiteSpace() {
		for (int length = filterChars.length; (pos < length) && Character.isWhitespace(filterChars[pos]);) {
			pos++;
		}
	}
}
