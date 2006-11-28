/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.transforms.xslt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.eclipse.equinox.transforms.CSVTransformingBundleFile;
import org.eclipse.equinox.transforms.Pipe;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleFile;
import org.osgi.framework.BundleContext;

public class XSLTBundleFile extends CSVTransformingBundleFile {

	private static final Map templateMap = new HashMap();

	public XSLTBundleFile(BundleContext context, BaseData data,
			BundleFile delegate, TransformList transformList)
			throws IOException {
		super(context, data, delegate, transformList);
	}

	protected InputStream getInputStream(InputStream inputStream,
			URL transformerUrl) throws IOException {
		Templates template = getTemplate(transformerUrl);
		if (template != null) {

			Transformer transformer = null;
			try {
				transformer = template.newTransformer();
				XSLTPipe pipe = new XSLTPipe(inputStream, transformer);
				return pipe.getPipedInputStream();
			} catch (TransformerConfigurationException e) {
				log(e);
			}
		}

		return null;
	}

	private synchronized Templates getTemplate(URL transformerURL) {
		Templates templates = null;

		SoftReference templatesRef = (SoftReference) templateMap
				.get(transformerURL);
		if (templatesRef != null) {
			templates = (Templates) templatesRef.get();
		}

		if (templates == null) {
			try {
				InputStream xsltStream = transformerURL.openStream();
				TransformerFactory tFactory = null;
				try {
					tFactory = TransformerFactory.newInstance();
					StreamSource xsltSource = new StreamSource(xsltStream);
					try {
						templatesRef = new SoftReference(templates = tFactory
								.newTemplates(xsltSource));
						templateMap.put(transformerURL, templatesRef);
					} catch (Exception e) {
						// cant create the template. May be an IO
						// exception from the source or perhaps a badly
						// formed XSLT. We shouldn't fail in this case.
						log(e);
					}
				} catch (TransformerFactoryConfigurationError e) {
					// we can proceed without a factory
					log(e);
				}
			} catch (IOException e) {
				log(e);
			}
		}
		return templates;
	}
}

class XSLTPipe extends Pipe {
	private Transformer transformer;

	public XSLTPipe(InputStream original, Transformer transformer)
			throws IOException {
		super(original);
		this.transformer = transformer;
	}

	protected void pipeInput(InputStream original, OutputStream result)
			throws IOException {
		try {
			transformer.transform(new StreamSource(original), new StreamResult(
					result));
		} catch (TransformerException e) {
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}
	}
}
