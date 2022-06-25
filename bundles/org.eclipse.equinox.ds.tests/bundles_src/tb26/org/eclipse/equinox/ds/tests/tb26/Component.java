/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package org.eclipse.equinox.ds.tests.tb26;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.eclipse.osgi.service.urlconversion.URLConverter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;

public abstract class Component {
	protected ComponentContext context;
	protected ServiceTracker<URLConverter, URLConverter> tracker;
	
	public abstract String getName();
	
	public abstract void update() throws Exception;
	
	protected void activate(ComponentContext context) throws InvalidSyntaxException {
		this.context = context;
		BundleContext bc = context.getBundleContext();
		Filter f = bc.createFilter("(&(objectClass=" + URLConverter.class.getName() + ")(protocol=bundleentry))");
		tracker = new ServiceTracker<>(bc, f, null);
		tracker.open();
	}
	
	protected void deactivate(ComponentContext context) {
		tracker.close();
	}
	
	protected void replaceCurrentComponentXmlWith(String componentXmlFileName) throws Exception {
		writeResource("component.xml", readResource(componentXmlFileName));
	}
	
	private void closeSilently(Closeable closeable) {
		try {
			closeable.close();
		}
		catch (IOException e) {}
	}
	
	private URL getResource(String name) {
		Bundle b = context.getBundleContext().getBundle();
		URL result = b.getResource(name);
		if (result == null)
			result = b.findEntries("/", name, true).nextElement();
		return result;
	}
	
	private File getResourceAsFile(String name) throws Exception {
		return new File(getResourceAsUri(name));
	}
	
	private FileInputStream getResourceAsFileInputStream(String name) throws Exception {
		return new FileInputStream(getResourceAsFile(name));
	}
	
	private URI getResourceAsUri(String name) throws Exception {
		URL url = getResource(name);
		URLConverter converter = tracker.getService();
		url = converter.toFileURL(url);
		return url.toURI();
	}
	
	private byte[] readResource(String name) throws Exception {
		FileInputStream fis = getResourceAsFileInputStream(name);
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				byte[] bytes = new byte[1024];
				int read;
				while((read = fis.read(bytes)) != -1)
					baos.write(bytes, 0, read);
				return baos.toByteArray();
			}
			finally {
				closeSilently(baos);
			}
		}
		finally {
			closeSilently(fis);
		}
	}
	
	private void writeResource(String name, byte[] content) throws Exception {
		File file = getResourceAsFile(name);
		FileOutputStream fos = new FileOutputStream(file);
		try {
			fos.write(content);
		}
		finally {
			closeSilently(fos);
		}
		// Ensure the update will be detected.
		file.setLastModified(file.lastModified() + 1000);
	}
}
