/*******************************************************************************
 * Copyright (c) 2008 Martin Lippert and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Martin Lippert            supplementing mechanism reworked     
 *******************************************************************************/

package org.eclipse.equinox.weaving.hooks;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;

public class Supplementer {

	private ManifestElement[] supplementBundle;
	private ManifestElement[] supplementImporter;
	private ManifestElement[] supplementExporter;
	private Bundle supplementer;
	private List supplementedBundles; // elements of type Bundle

	public Supplementer(Bundle bundle,
			ManifestElement[] supplementBundle,
			ManifestElement[] supplementImporter,
			ManifestElement[] supplementExporter) {
		this.supplementer = bundle;
		this.supplementBundle = supplementBundle;
		this.supplementImporter = supplementImporter;
		this.supplementExporter = supplementExporter;
		this.supplementedBundles = new ArrayList();
	}

	public Bundle getSupplementerBundle() {
		return supplementer;
	}
	
	public String getSymbolicName() {
		return supplementer.getSymbolicName();
	}
	
	public void addSupplementedBundle(Bundle supplementedBundle) {
		this.supplementedBundles.add(supplementedBundle);
	}
	
	public void removeSupplementedBundle(Bundle supplementedBundle) {
		this.supplementedBundles.remove(supplementedBundle);
	}

	public Bundle[] getSupplementedBundles() {
		return (Bundle[]) supplementedBundles.toArray(new Bundle[supplementedBundles.size()]);
	}
	
	public boolean isSupplemented(Bundle bundle) {
		return supplementedBundles.contains(bundle);
	}

	public boolean matchSupplementer(String symbolicName) {
		boolean matches = false;
		
		if (supplementBundle != null) for (int i = 0; !matches && i < supplementBundle.length; i++) {
			ManifestElement bundle = supplementBundle[i];
			if (equals_wild(bundle.getValue(),symbolicName)) matches = true;
		}
		
		return matches;
	}
	
	public boolean matchesSupplementImporter(ManifestElement[] imports) {
		boolean matches = false;

		if (supplementImporter != null) for (int i = 0; !matches && i < supplementImporter.length; i++) {
			ManifestElement supplementImport = supplementImporter[i];
			for (int j = 0; !matches && j < imports.length; j++) {
				ManifestElement importPackage = imports[j];
				if (supplementImport.getValue().equals(importPackage.getValue())) matches = true;
			}
		}
		
		return matches;
	}
	
	public boolean matchesSupplementExporter(ManifestElement[] exports) {
		boolean matches = false;

		if (supplementExporter != null) for (int i = 0; !matches && i < supplementExporter.length; i++) {
			ManifestElement supplementExport = supplementExporter[i];
			for (int j = 0; !matches && j < exports.length; j++) {
				ManifestElement exportPackage = exports[j];
				if (supplementExport.getValue().equals(exportPackage.getValue())) matches = true;
			}
		}
		
		return matches;
	}
	
	//knibb
	//test if two Strings are equal
	//with wild card support - only supports strings ending in *
	private boolean equals_wild(String input, String match){
		if(input.equals(match)){
			//its a match so just return true
			return true;
		}
		if(input.endsWith("*")==false){
			//no wildcards are being used here
			return false;
		}
		String wild_in = input.substring(0, input.length()-1);
		if(match.startsWith(wild_in))
			return true;
		
		return false;
	}

}
