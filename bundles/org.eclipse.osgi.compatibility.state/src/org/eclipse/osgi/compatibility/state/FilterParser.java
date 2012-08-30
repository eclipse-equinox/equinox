/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.osgi.compatibility.state;

import java.util.*;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

public class FilterParser {
	static class Range {
		private char leftRule = 0;
		private Version leftVersion;
		private Version rightVersion;
		private char rightRule = 0;
		private Collection<Version> excludes = new ArrayList<Version>(0);

		public String toString() {
			if (rightVersion == null) {
				return leftVersion.toString();
			}
			return leftRule + leftVersion.toString() + ',' + rightVersion.toString() + rightRule;
		}

		void addExclude(Version exclude) {
			this.excludes.add(exclude);
			setLeft(leftRule, leftVersion);
			setRight(rightRule, rightVersion);
		}

		boolean setLeft(char leftRule, Version leftVersion) {
			if (this.leftVersion != null && this.leftVersion != leftVersion)
				return false;
			this.leftRule = excludes.contains(leftVersion) ? '(' : leftRule;
			this.leftVersion = leftVersion;
			return true;
		}

		boolean setRight(char rightRule, Version rightVersion) {
			if (this.rightVersion != null && this.rightVersion != rightVersion)
				return false;
			this.rightRule = excludes.contains(rightVersion) ? ')' : rightRule;
			this.rightVersion = rightVersion;
			return true;
		}
	}

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
			if (op != AND && op != EQUAL)
				throw new IllegalStateException("Invalid filter for Starndard OSGi Attributes: " + op); //$NON-NLS-1$
			Map<String, String> result = new HashMap<String, String>();
			Map<String, Range> versionAttrs = new HashMap<String, Range>();
			if (versions != null) {
				for (String versionAttr : versions) {
					versionAttrs.put(versionAttr, null);
				}
			}
			addAttributes(result, versionAttrs, false);
			for (Map.Entry<String, Range> entry : versionAttrs.entrySet()) {
				Range range = entry.getValue();
				if (range != null) {
					result.put(entry.getKey(), range.toString());
				}
			}

			return result;
		}

		private void addAttributes(Map<String, String> attributes, Map<String, Range> versionAttrs, boolean not) {
			if (op == EQUAL) {
				if (!versionAttrs.containsKey(attr)) {
					attributes.put(attr, value);
				} else {
					// this is an exact range e.g. [value,value]
					Range currentRange = versionAttrs.get(attr);
					if (currentRange != null) {
						if (not) {
							// this is an expanded for of the filter, e.g.:
							// [1.0,2.0) -> (&(version>=1.0)(version<=2.0)(!(version=2.0)))
							currentRange.addExclude(new Version(value));
						} else {
							throw new IllegalStateException("Invalid range for: " + attr); //$NON-NLS-1$
						}
					}
					currentRange = new Range();
					Version version = new Version(value);
					currentRange.setLeft('[', version);
					currentRange.setRight(']', version);
					versionAttrs.put(attr, currentRange);
				}
			} else if (op == LESS) {
				if (!versionAttrs.containsKey(attr))
					throw new IllegalStateException("Invalid attribute: " + attr); //$NON-NLS-1$
				Range currentRange = versionAttrs.get(attr);
				if (currentRange == null) {
					currentRange = new Range();
					versionAttrs.put(attr, currentRange);
				}
				if (not) {
					// this must be a range start "(value"
					if (!currentRange.setLeft('(', new Version(value)))
						throw new IllegalStateException("range start is already processed for attribute: " + attr); //$NON-NLS-1$
				} else {
					// this must be a range end "value]"
					if (!currentRange.setRight(']', new Version(value)))
						throw new IllegalStateException("range end is already processed for attribute: " + attr); //$NON-NLS-1$
				}
			} else if (op == GREATER) {
				if (!versionAttrs.containsKey(attr))
					throw new IllegalStateException("Invalid attribute: " + attr); //$NON-NLS-1$
				Range currentRange = versionAttrs.get(attr);
				if (currentRange == null) {
					currentRange = new Range();
					versionAttrs.put(attr, currentRange);
				}
				if (not) {
					// this must be a range end "value)"
					if (!currentRange.setRight(')', new Version(value)))
						throw new IllegalStateException("range end is already processed for attribute: " + attr); //$NON-NLS-1$
				} else {
					// this must be a range start "[value"
					if (!currentRange.setLeft('[', new Version(value)))
						throw new IllegalStateException("range start is already processed for attribute: " + attr); //$NON-NLS-1$
				}
			} else if (op == AND) {
				for (FilterComponent component : nested) {
					component.addAttributes(attributes, versionAttrs, false);
				}
			} else if (op == NOT) {
				nested.get(0).addAttributes(attributes, versionAttrs, true);
			} else {
				throw new IllegalStateException("Invalid filter for standard OSGi requirements: " + op); //$NON-NLS-1$
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
			throw new InvalidSyntaxException("Filter ended abruptly", filterstring, e); //$NON-NLS-1$
		}

		if (pos != filterChars.length) {
			throw new InvalidSyntaxException("Extraneous trailing characters: " + filterstring.substring(pos), filterstring); //$NON-NLS-1$
		}
		return filter;
	}

	private FilterComponent parse_filter() throws InvalidSyntaxException {
		FilterComponent filter;
		skipWhiteSpace();

		if (filterChars[pos] != '(') {
			throw new InvalidSyntaxException("Missing '(': " + filterstring.substring(pos), filterstring); //$NON-NLS-1$
		}

		pos++;

		filter = parse_filtercomp();

		skipWhiteSpace();

		if (filterChars[pos] != ')') {
			throw new InvalidSyntaxException("Missing ')': " + filterstring.substring(pos), filterstring); //$NON-NLS-1$
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

		throw new InvalidSyntaxException("Invalid operator: " + filterstring.substring(pos), filterstring); //$NON-NLS-1$
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
			throw new InvalidSyntaxException("Missing attr: " + filterstring.substring(pos), filterstring); //$NON-NLS-1$
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
					throw new InvalidSyntaxException("Invalid value: " + filterstring.substring(pos), filterstring); //$NON-NLS-1$
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
			throw new InvalidSyntaxException("Missing value: " + filterstring.substring(pos), filterstring); //$NON-NLS-1$
		}

		return sb.toString();
	}

	private void skipWhiteSpace() {
		for (int length = filterChars.length; (pos < length) && Character.isWhitespace(filterChars[pos]);) {
			pos++;
		}
	}
}
