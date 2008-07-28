/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   David Knibb               initial implementation      
 *   Matthew Webster           Eclipse 3.2 changes     
 *******************************************************************************/

package org.eclipse.equinox.weaving.aspectj.loadtime;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.aspectj.weaver.loadtime.DefaultWeavingContext;
import org.aspectj.weaver.tools.WeavingAdaptor;
import org.eclipse.equinox.service.weaving.SupplementerRegistry;
import org.eclipse.equinox.weaving.aspectj.WeavingServicePlugin;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.State;
import org.osgi.framework.Bundle;

public class OSGiWeavingContext extends DefaultWeavingContext {
	
	private final State resolverState;
	private final Bundle bundle;
	private final BundleDescription bundleDescription;
	private final SupplementerRegistry supplementerRegistry;

	public OSGiWeavingContext(ClassLoader loader, Bundle bundle, State state, BundleDescription bundleDescription, SupplementerRegistry supplementerRegistry) {
		super(loader);
		this.bundle = bundle;
		this.bundleDescription = bundleDescription;
		this.resolverState = state;
		this.supplementerRegistry = supplementerRegistry;
		if (WeavingServicePlugin.DEBUG) System.out.println("- WeavingContext.WeavingContext() locader=" + loader + ", bundle=" + bundle.getSymbolicName());
	}

	public String getBundleIdFromURL(URL url) {
		return resolverState.getBundle(Integer.parseInt(url.getHost())).getSymbolicName();
	}
	
    public String getBundleVersionFromURL(URL url) {
        return resolverState.getBundle(Integer.parseInt(url.getHost())).getVersion().toString();
    }

    public String toString(){
		return getClass().getName() + "[" + bundleDescription.getSymbolicName() + "]";
	}

	public String getClassLoaderName() {
		return bundleDescription.getSymbolicName();
	}

	public String getFile(URL url) {
		return getBundleIdFromURL(url) + url.getFile();
	}

	public String getId () {
		return bundleDescription.getSymbolicName();
	}

	public Enumeration getResources(String name) throws IOException {
		Enumeration result = super.getResources(name);
		
		if (name.endsWith("aop.xml")){
			Vector modified =new Vector();
			BundleSpecification[] requires = bundleDescription.getRequiredBundles();
			BundleDescription[] fragments = bundleDescription.getFragments();
			
			while(result.hasMoreElements()){
				URL xml = (URL) result.nextElement();
				String resourceBundleName = getBundleIdFromURL(xml);

				if (bundleDescription.getSymbolicName().equals(resourceBundleName)){
				        modified.add(xml);
				        continue;
				}

				for (int i=0; i<requires.length; i++){
					BundleSpecification r = requires[i];
					if (r.getName().equals(resourceBundleName)){
						modified.add(xml);
						continue;
					}
				}
				
				for (int i = 0; i < fragments.length; i++) {
					BundleSpecification[] fragmentRequires = fragments[i].getRequiredBundles();
					for (int j=0; j<fragmentRequires.length; j++){
						BundleSpecification r = fragmentRequires[j];
						if (r.getName().equals(resourceBundleName)){
							modified.add(xml);
							continue;
						}
					}
				}
			}
			
			result=modified.elements();
		}
		return result;
	}
	
	public List getDefinitions(ClassLoader loader, WeavingAdaptor adaptor) {
        List definitions = ((OSGiWeavingAdaptor)adaptor).parseDefinitionsForBundle();
		return definitions;
	}

	public Bundle[] getBundles() {
		Set bundles = new HashSet();

		// the bundle this context belongs to should be used
		bundles.add(this.bundle);
		
		// add required bundles
		if (this.bundle.getBundleContext() != null) {
			BundleDescription[] resolvedRequires = this.bundleDescription.getResolvedRequires();
			for (int i = 0; i < resolvedRequires.length; i++) {
				Bundle requiredBundle = this.bundle.getBundleContext().getBundle(resolvedRequires[i].getBundleId());
				if (requiredBundle != null) {
					bundles.add(requiredBundle);
				}
			}
		}
		
		// add supplementers
		Bundle[] supplementers = this.supplementerRegistry.getSupplementers(this.bundle);
		bundles.addAll(Arrays.asList(supplementers));
		
		return (Bundle[]) bundles.toArray(new Bundle[bundles.size()]);
	}

}
