/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.commands;

import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import org.apache.felix.service.command.Converter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

/**
 * Converter for the arguments of the migrated equinox commands.
 */
public class EquinoxCommandsConverter implements Converter {
	BundleContext context;
	
	public EquinoxCommandsConverter(BundleContext context) {
		this.context = context;
	}
	
	public Object convert(Class<?> desiredType, Object in) throws Exception {
		if(desiredType == Bundle[].class) {
			if (in instanceof String) {
				if("*".equals((String) in)) {
					return context.getBundles();
				}
			} else if (in instanceof List<?>) {
				List<?> args = (List<?>) in;
				if (checkStringElements(args)) {
					try {
						ArrayList<Bundle> bundles = new ArrayList<Bundle>();
						for (Object arg : args) {
							long id = Long.parseLong((String)arg);
							bundles.add(context.getBundle(id));
						}
						return bundles.toArray(new Bundle[0]);
					} catch (Exception e) {
						return null;
					}
				}
			}
		}
		
		if(desiredType == Bundle.class) {
			Bundle bundle = null;
			try {
				long id = Long.parseLong((String) in);
				bundle = context.getBundle(id);
			} catch (NumberFormatException nfe) {

				// if not found, assume token is either symbolic name@version, or location
				String symbolicName = (String) in;
				Version version = null;

				// check for @ -- this may separate either the version string, or be part of the
				// location
				int ix = symbolicName.indexOf("@"); //$NON-NLS-1$
				if (ix != -1) {
					if ((ix + 1) != symbolicName.length()) {
						try {
							// if the version parses, then use the token prior to @ as a symbolic name
							version = Version.parseVersion(symbolicName.substring(ix + 1, symbolicName.length()));
							symbolicName = symbolicName.substring(0, ix);
						} catch (IllegalArgumentException e) {
							// version doesn't parse, assume token is symbolic name without version, or location
						}
					}
				}

				Bundle[] bundles = context.getBundles();
				for (Bundle b : bundles) {
					
					// if symbolicName matches, then matches if there is no version specific on command, or the version matches
					// if there is no version specified on command, pick first matching bundle
					if ((symbolicName.equals(b.getSymbolicName()) && (version == null || version.equals(b.getVersion()))) || ((String)in).equals(b.getLocation())) {
						bundle = b;
						break;
					}
				}
			}
			return bundle;
		}
		
		if (desiredType == URL.class) {
			URL url = null;
			try {
				url = new URL((String) in);
			} catch (Exception e) {
				//do nothing
			}
			return url;
		}
		
		return null;
	}
	
	private boolean checkStringElements(List<?> list) {
		for (Object element : list) {
			if (!(element instanceof String)) {
				return false;
			}
		}
		
		return true;
	}

	public CharSequence format(Object target, int level, Converter escape) throws Exception {
		if (target instanceof Dictionary<?, ?>) {
			Dictionary<?, ?> dic = (Dictionary<?, ?>) target;
			return printDictionary(dic);
		}
		
		if (target instanceof List<?>) {
			List<?> list = (List<?>) target;
			if (checkDictionaryElements(list)) {
				StringBuilder builder = new StringBuilder();
				for(Object dic : list) {
					builder.append("Bundle headers:\r\n");
					builder.append(printDictionary((Dictionary<?, ?>)dic));
					builder.append("\r\n");
					builder.append("\r\n");
				}
				return builder.toString();
			}
		}
		
		return null;
	}
	
	private boolean checkDictionaryElements(List<?> list) {
		for (Object element : list) {
			if (!(element instanceof Dictionary<?, ?>)) {
				return false;
			}
		}
		
		return true;
	}
	
	private String printDictionary(Dictionary<?, ?> dic) {
		int count = dic.size();
		String[] keys = new String[count];
		Enumeration<?> keysEnum = dic.keys();
		int i = 0;
		while (keysEnum.hasMoreElements()) {
			keys[i++] = (String) keysEnum.nextElement();
		}
		Util.sortByString(keys);
		
		StringBuilder builder = new StringBuilder();
		for (i = 0; i < count; i++) {
			builder.append(" " + keys[i] + " = " + dic.get(keys[i])); //$NON-NLS-1$//$NON-NLS-2$
			builder.append("\r\n");
		}
		builder.append("\r\n");
		return builder.toString();
	}

}
