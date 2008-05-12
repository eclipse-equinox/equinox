/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.internal.security.ui;

import java.io.PrintWriter;
import java.security.Provider;
import java.security.Security;
import java.util.*;
import org.eclipse.ui.about.ISystemSummarySection;

public class SecurityConfigurationSection implements ISystemSummarySection {

	private static final String ALG_ALIAS = "Alg.Alias."; //$NON-NLS-1$
	private static final String PROVIDER = "Provider."; //$NON-NLS-1$

	public void write(PrintWriter writer) {

		Provider[] providers = Security.getProviders();
		writer.println("Providers (" + providers.length + "): ");//$NON-NLS-1$ //$NON-NLS-2$ 
		writer.println();

		for (int i = 0; i < providers.length; i++) {
			appendProvider(writer, providers[i], i);
		}
	}

	private void appendProvider(PrintWriter writer, Provider provider, int index) {
		writer.println(" Provider: " + provider.getName() + ", Version: " + provider.getVersion() + ", Class: " + provider.getClass().getName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		writer.println("  Description: " + provider.getInfo()); //$NON-NLS-1$
		ProviderService[] services = getServices(provider);
		writer.println("  Services (" + services.length + "):"); //$NON-NLS-1$  //$NON-NLS-2$
		for (int i = 0; i < services.length; i++) {
			appendService(writer, services[i], i);
		}
		writer.println();
	}

	private void appendService(PrintWriter writer, ProviderService service, int index) {
		writer.println("   Service: " + service.getType() + ", Algorithm: " + service.getAlgorithm() + ", Class: " + service.getClassName()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		List aliases = service.getAliases();
		if (null != aliases && (0 < aliases.size())) {
			writer.print("    Aliases: "); //$NON-NLS-1$
			for (Iterator it = aliases.iterator(); it.hasNext();) {
				writer.print((String) it.next());
				if (it.hasNext()) {
					writer.print(", "); //$NON-NLS-1$
				}
			}
			writer.println();
		}
		Map attributes = service.getAttributes();
		if ((null != attributes) && (0 < attributes.size())) {
			writer.println("    Attributes:"); //$NON-NLS-1$
			Set keys = attributes.keySet();
			for (Iterator it = keys.iterator(); it.hasNext();) {
				String key = (String) it.next();
				writer.print("      " + key + ": "); //$NON-NLS-1$//$NON-NLS-2$
				writer.println((String) attributes.get(key));
			}
		}
	}

	private static ProviderService[] getServices(Provider provider) {

		Set providerKeys = provider.keySet();
		Hashtable serviceList = new Hashtable();
		Hashtable attributeMap = new Hashtable(); // "type" => "Hashtable of (attribute,value) pairs"
		Hashtable aliasMap = new Hashtable(); // "type" => "Arraylist of aliases"
		for (Iterator it = providerKeys.iterator(); it.hasNext();) {
			String key = (String) it.next();

			// this is provider info, available off the Provider API
			if (key.startsWith(PROVIDER)) {
				continue;
			}

			// this is an alias
			if (key.startsWith(ALG_ALIAS)) {
				String value = key.substring(key.indexOf(ALG_ALIAS) + ALG_ALIAS.length(), key.length());
				String type = (String) provider.get(key);
				String algo = value.substring(0, value.indexOf('.'));
				String alias = value.substring(value.indexOf('.') + 1, value.length());
				ArrayList aliasList = (ArrayList) aliasMap.get(type + '.' + algo);
				if (aliasList == null) {
					aliasList = new ArrayList();
					aliasList.add(alias);
					aliasMap.put(type, aliasList);
				} else {
					aliasList.add(alias);
				}
			}

			// this is an attribute
			else if (key.indexOf(' ') > -1) {
				String type = key.substring(0, key.indexOf('.'));
				String algorithm = key.substring(key.indexOf('.') + 1, key.indexOf(' '));
				String attribute = key.substring(key.indexOf(' ') + 1, key.length());
				String value = (String) provider.get(key);
				Hashtable attributeTable = (Hashtable) attributeMap.get(type + '.' + algorithm);
				if (attributeTable == null) {
					attributeTable = new Hashtable();
					attributeTable.put(attribute, value);
					attributeMap.put(type + '.' + algorithm, attributeTable);
				} else {
					attributeTable.put(attribute, value);
				}
			}

			// else this is a service
			else {
				serviceList.put(key, provider.get(key));
			}
		}

		ProviderService[] serviceArray = new ProviderService[serviceList.size()];
		Set serviceKeys = serviceList.keySet();
		int serviceCount = 0;
		for (Iterator it = serviceKeys.iterator(); it.hasNext();) {
			String key = (String) it.next();
			String type = key.substring(0, key.indexOf('.'));
			String algo = key.substring(key.indexOf('.') + 1, key.length());
			String className = (String) serviceList.get(key);
			List aliases = (List) aliasMap.get(algo);
			Map attributes = (Map) attributeMap.get(key);

			serviceArray[serviceCount] = new ProviderService(type, algo, className, aliases, attributes);
			serviceCount++;
		}
		return serviceArray;
	}

	private static class ProviderService {

		private final String type;
		private final String algorithm;
		private final String className;
		private final List aliases;
		private final Map attributes;

		public ProviderService(String type, String algorithm, String className, List aliases, Map attributes) {
			this.type = type;
			this.algorithm = algorithm;
			this.className = className;
			this.aliases = aliases;
			this.attributes = attributes;
		}

		public String getType() {
			return type;
		}

		public String getAlgorithm() {
			return algorithm;
		}

		public String getClassName() {
			return className;
		}

		public List getAliases() {
			return aliases;
		}

		public Map getAttributes() {
			return attributes;
		}
	}
}
