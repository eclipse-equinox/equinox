/*******************************************************************************
 * Copyright (c) 2005, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.eclipse.osgi.internal.util.SupplementDebug;

/**
 * Common superclass for all message bundle classes.  Provides convenience
 * methods for manipulating messages.
 * <p>
 * The <code>#bind</code> methods perform string substitution and should be considered a
 * convenience and <em>not</em> a full substitute replacement for <code>MessageFormat#format</code>
 * method calls.
 * </p>
 * <p>
 * Text appearing within curly braces in the given message, will be interpreted
 * as a numeric index to the corresponding substitution object in the given array. Calling
 * the <code>#bind</code> methods with text that does not map to an integer will result in an
 * {@link IllegalArgumentException}.
 * </p>
 * <p>
 * Text appearing within single quotes is treated as a literal. A single quote is escaped by
 * a preceeding single quote.
 * </p>
 * <p>
 * Clients who wish to use the full substitution power of the <code>MessageFormat</code> class should
 * call that class directly and not use these <code>#bind</code> methods.
 * </p>
 * <p>
 * Clients may subclass this type.
 * </p>
 *
 * @since 3.1
 */
public abstract class NLS {

	private static final Object[] EMPTY_ARGS = new Object[0];
	private static final String EXTENSION = ".properties"; //$NON-NLS-1$
	private static String[] nlSuffixes;
	private static final String PROP_WARNINGS = "osgi.nls.warnings"; //$NON-NLS-1$
	private static final String IGNORE = "ignore"; //$NON-NLS-1$
	private static final boolean ignoreWarnings = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> IGNORE.equals(System.getProperty(PROP_WARNINGS)));

	/*
	 * NOTE do not change the name of this field; it is set by the Framework using reflection
	 */
	private static FrameworkLog frameworkLog;

	static final int SEVERITY_ERROR = 0x04;
	static final int SEVERITY_WARNING = 0x02;
	/*
	 * This object is assigned to the value of a field map to indicate
	 * that a translated message has already been assigned to that field.
	 */
	static final Object ASSIGNED = new Object();

	/**
	 * Creates a new NLS instance.
	 */
	protected NLS() {
		super();
	}

	/**
	 * Bind the given message's substitution locations with the given string value.
	 *
	 * @param message the message to be manipulated
	 * @param binding the object to be inserted into the message
	 * @return the manipulated String
	 * @throws IllegalArgumentException if the text appearing within curly braces in the given message does not map to an integer
	 */
	public static String bind(String message, Object binding) {
		return internalBind(message, null, String.valueOf(binding), null);
	}

	/**
	 * Bind the given message's substitution locations with the given string values.
	 *
	 * @param message the message to be manipulated
	 * @param binding1 An object to be inserted into the message
	 * @param binding2 A second object to be inserted into the message
	 * @return the manipulated String
	 * @throws IllegalArgumentException if the text appearing within curly braces in the given message does not map to an integer
	 */
	public static String bind(String message, Object binding1, Object binding2) {
		return internalBind(message, null, String.valueOf(binding1), String.valueOf(binding2));
	}

	/**
	 * Bind the given message's substitution locations with the given string values.
	 *
	 * @param message the message to be manipulated
	 * @param bindings An array of objects to be inserted into the message
	 * @return the manipulated String
	 * @throws IllegalArgumentException if the text appearing within curly braces in the given message does not map to an integer
	 */
	public static String bind(String message, Object[] bindings) {
		return internalBind(message, bindings, null, null);
	}

	/**
	 * Initialize the given class with the values from the message properties specified by the
	 * base name.  The base name specifies a fully qualified base name to a message properties file,
	 * including the package where the message properties file is located.  The class loader of the
	 * specified class will be used to load the message properties resources.
	 * <p>
	 * For example, if the locale is set to en_US and <code>org.eclipse.example.nls.messages</code>
	 * is used as the base name then the following resources will be searched using the class
	 * loader of the specified class:
	 * </p>
	 * <pre>
	 *   org/eclipse/example/nls/messages_en_US.properties
	 *   org/eclipse/example/nls/messages_en.properties
	 *   org/eclipse/example/nls/messages.properties
	 * </pre>
	 *
	 * @param baseName the base name of a fully qualified message properties file.
	 * @param clazz the class where the constants will exist
	 */
	public static void initializeMessages(final String baseName, final Class<?> clazz) {
		if (System.getSecurityManager() == null) {
			load(baseName, clazz);
			return;
		}
		AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
			load(baseName, clazz);
			return null;
		});
	}

	/*
	 * Perform the string substitution on the given message with the specified args.
	 * See the class comment for exact details.
	 */
	private static String internalBind(String message, Object[] args, String argZero, String argOne) {
		if (message == null)
			return "No message available."; //$NON-NLS-1$
		if (args == null || args.length == 0)
			args = EMPTY_ARGS;

		int length = message.length();
		//estimate correct size of string buffer to avoid growth
		int bufLen = length + (args.length * 5);
		if (argZero != null)
			bufLen += argZero.length() - 3;
		if (argOne != null)
			bufLen += argOne.length() - 3;
		StringBuilder buffer = new StringBuilder(bufLen < 0 ? 0 : bufLen);
		for (int i = 0; i < length; i++) {
			char c = message.charAt(i);
			switch (c) {
				case '{' :
					int index = message.indexOf('}', i);
					// if we don't have a matching closing brace then...
					if (index == -1) {
						buffer.append(c);
						break;
					}
					i++;
					if (i >= length) {
						buffer.append(c);
						break;
					}
					// look for a substitution
					int number = -1;
					try {
						number = Integer.parseInt(message.substring(i, index));
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException(e);
					}
					if (number == 0 && argZero != null)
						buffer.append(argZero);
					else if (number == 1 && argOne != null)
						buffer.append(argOne);
					else {
						if (number >= args.length || number < 0) {
							buffer.append("<missing argument>"); //$NON-NLS-1$
							i = index;
							break;
						}
						buffer.append(args[number]);
					}
					i = index;
					break;
				case '\'' :
					// if a single quote is the last char on the line then skip it
					int nextIndex = i + 1;
					if (nextIndex >= length) {
						buffer.append(c);
						break;
					}
					char next = message.charAt(nextIndex);
					// if the next char is another single quote then write out one
					if (next == '\'') {
						i++;
						buffer.append(c);
						break;
					}
					// otherwise we want to read until we get to the next single quote
					index = message.indexOf('\'', nextIndex);
					// if there are no more in the string, then skip it
					if (index == -1) {
						buffer.append(c);
						break;
					}
					// otherwise write out the chars inside the quotes
					buffer.append(message, nextIndex, index);
					i = index;
					break;
				default :
					buffer.append(c);
			}
		}
		return buffer.toString();
	}

	/*
	 * Build an array of property files to search.  The returned array contains
	 * the property fields in order from most specific to most generic.
	 * So, in the FR_fr locale, it will return file_fr_FR.properties, then
	 * file_fr.properties, and finally file.properties.
	 */
	private static String[] buildVariants(String root) {
		if (nlSuffixes == null) {
			//build list of suffixes for loading resource bundles
			String nl = Locale.getDefault().toString();
			List<String> result = new ArrayList<>(4);
			int lastSeparator;
			while (true) {
				result.add('_' + nl + EXTENSION);
				String additional = getAdditionalSuffix(nl);
				if (additional != null) {
					result.add('_' + additional + EXTENSION);
				}
				lastSeparator = nl.lastIndexOf('_');
				if (lastSeparator == -1)
					break;
				nl = nl.substring(0, lastSeparator);
			}
			//add the empty suffix last (most general)
			result.add(EXTENSION);
			nlSuffixes = result.toArray(new String[result.size()]);
		}
		root = root.replace('.', '/');
		String[] variants = new String[nlSuffixes.length];
		for (int i = 0; i < variants.length; i++)
			variants[i] = root + nlSuffixes[i];
		return variants;
	}

	/*
	 * This is a fix due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=579215
	 * Ideally, this needs to be removed once the Eclipse minimum support moves to Java 17
	 */
	private static String getAdditionalSuffix(String nl) {
		String additional = null;
		if (nl != null) {
			if ("he".equals(nl)) { //$NON-NLS-1$
				additional = "iw"; //$NON-NLS-1$
			} else if (nl.startsWith("he_")) { //$NON-NLS-1$
				additional = "iw_" + nl.substring(3); //$NON-NLS-1$
			}
		}

		return additional;
	}

	private static void computeMissingMessages(String bundleName, Class<?> clazz, Map<Object, Object> fieldMap, Field[] fieldArray, boolean isAccessible) {
		// iterate over the fields in the class to make sure that there aren't any empty ones
		final int MOD_EXPECTED = Modifier.PUBLIC | Modifier.STATIC;
		final int MOD_MASK = MOD_EXPECTED | Modifier.FINAL;
		final int numFields = fieldArray.length;
		for (int i = 0; i < numFields; i++) {
			Field field = fieldArray[i];
			if ((field.getModifiers() & MOD_MASK) != MOD_EXPECTED)
				continue;
			//if the field has a a value assigned, there is nothing to do
			if (fieldMap.get(field.getName()) == ASSIGNED)
				continue;
			try {
				// Set a value for this empty field. We should never get an exception here because
				// we know we have a public static non-final field. If we do get an exception, silently
				// log it and continue. This means that the field will (most likely) be un-initialized and
				// will fail later in the code and if so then we will see both the NPE and this error.
				String value = "NLS missing message: " + field.getName() + " in: " + bundleName; //$NON-NLS-1$ //$NON-NLS-2$
				if (SupplementDebug.STATIC_DEBUG_MESSAGE_BUNDLES)
					System.out.println(value);
				log(SEVERITY_WARNING, value, null);
				if (!isAccessible)
					field.setAccessible(true);
				field.set(null, value);
			} catch (Exception e) {
				log(SEVERITY_ERROR, "Error setting the missing message value for: " + field.getName(), e); //$NON-NLS-1$
			}
		}
	}

	/*
	 * Load the given resource bundle using the specified class loader.
	 */
	static void load(final String bundleName, Class<?> clazz) {
		long start = System.currentTimeMillis();
		final Field[] fieldArray = clazz.getDeclaredFields();
		ClassLoader loader = clazz.getClassLoader();

		boolean isAccessible = (clazz.getModifiers() & Modifier.PUBLIC) != 0;

		//build a map of field names to Field objects
		final int len = fieldArray.length;
		Map<Object, Object> fields = new HashMap<>(len * 2);
		for (int i = 0; i < len; i++)
			fields.put(fieldArray[i].getName(), fieldArray[i]);

		// search the variants from most specific to most general, since
		// the MessagesProperties.put method will mark assigned fields
		// to prevent them from being assigned twice
		final String[] variants = buildVariants(bundleName);
		for (String variant : variants) {
			// loader==null if we're launched off the Java boot classpath
			final InputStream input = loader == null ? ClassLoader.getSystemResourceAsStream(variant) : loader.getResourceAsStream(variant);
			if (input == null)
				continue;
			try {
				final MessagesProperties properties = new MessagesProperties(fields, bundleName, isAccessible);
				properties.load(input);
			} catch (IOException e) {
				log(SEVERITY_ERROR, "Error loading " + variant, e); //$NON-NLS-1$
			} finally {
				if (input != null)
					try {
						input.close();
					} catch (IOException e) {
						// ignore
					}
			}
		}
		computeMissingMessages(bundleName, clazz, fields, fieldArray, isAccessible);
		if (SupplementDebug.STATIC_DEBUG_MESSAGE_BUNDLES)
			System.out.println("Time to load message bundle: " + bundleName + " was " + (System.currentTimeMillis() - start) + "ms."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/*
	 * The method adds a log entry based on the error message and exception.
	 * The output is written to the System.err.
	 *
	 * This method is only expected to be called if there is a problem in
	 * the NLS mechanism. As a result, translation facility is not available
	 * here and messages coming out of this log are generally not translated.
	 *
	 * @param severity - severity of the message (SEVERITY_ERROR or SEVERITY_WARNING)
	 * @param message - message to log
	 * @param e - exception to log
	 */
	static void log(int severity, String message, Exception e) {
		if (severity == SEVERITY_WARNING && ignoreWarnings)
			return; // ignoring warnings; bug 292980
		if (frameworkLog != null) {
			frameworkLog.log(new FrameworkLogEntry("org.eclipse.osgi", severity, 1, message, 0, e, null)); //$NON-NLS-1$
			return;
		}
		String statusMsg;
		switch (severity) {
			case SEVERITY_ERROR :
				statusMsg = "Error: "; //$NON-NLS-1$
				break;
			case SEVERITY_WARNING :
				// intentionally fall through:
			default :
				statusMsg = "Warning: "; //$NON-NLS-1$
		}
		if (message != null)
			statusMsg += message;
		if (e != null)
			statusMsg += ": " + e.getMessage(); //$NON-NLS-1$
		System.err.println(statusMsg);
		if (e != null)
			e.printStackTrace();
	}

	/*
	 * Class which sub-classes java.util.Properties and uses the #put method
	 * to set field values rather than storing the values in the table.
	 */
	private static class MessagesProperties extends Properties {

		private static final int MOD_EXPECTED = Modifier.PUBLIC | Modifier.STATIC;
		private static final int MOD_MASK = MOD_EXPECTED | Modifier.FINAL;
		private static final long serialVersionUID = 1L;

		private final String bundleName;
		private final Map<Object, Object> fields;
		private final boolean isAccessible;

		public MessagesProperties(Map<Object, Object> fieldMap, String bundleName, boolean isAccessible) {
			super();
			this.fields = fieldMap;
			this.bundleName = bundleName;
			this.isAccessible = isAccessible;
		}

		/* (non-Javadoc)
		 * @see java.util.Hashtable#put(java.lang.Object, java.lang.Object)
		 */
		@Override
		public synchronized Object put(Object key, Object value) {
			Object fieldObject = fields.put(key, ASSIGNED);
			// if already assigned, there is nothing to do
			if (fieldObject == ASSIGNED)
				return null;
			if (fieldObject == null) {
				final String msg = "NLS unused message: " + key + " in: " + bundleName;//$NON-NLS-1$ //$NON-NLS-2$
				if (SupplementDebug.STATIC_DEBUG_MESSAGE_BUNDLES)
					System.out.println(msg);
				// keys with '.' are ignored by design (bug 433424)
				if (key instanceof String && ((String) key).indexOf('.') < 0) {
					log(SEVERITY_WARNING, msg, null);
				}
				return null;
			}
			final Field field = (Field) fieldObject;
			//can only set value of public static non-final fields
			if ((field.getModifiers() & MOD_MASK) != MOD_EXPECTED)
				return null;
			try {
				// Check to see if we are allowed to modify the field. If we aren't (for instance
				// if the class is not public) then change the accessible attribute of the field
				// before trying to set the value.
				if (!isAccessible)
					field.setAccessible(true);
				// Set the value into the field. We should never get an exception here because
				// we know we have a public static non-final field. If we do get an exception, silently
				// log it and continue. This means that the field will (most likely) be un-initialized and
				// will fail later in the code and if so then we will see both the NPE and this error.

				// Extra care is taken to be sure we create a String with its own backing char[] (bug 287183)
				// This is to ensure we do not keep the key chars in memory.
				field.set(null, new String(((String) value).toCharArray()));
			} catch (Exception e) {
				log(SEVERITY_ERROR, "Exception setting field value.", e); //$NON-NLS-1$
			}
			return null;
		}
	}
}
