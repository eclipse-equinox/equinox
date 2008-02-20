/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.transforms.xslt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.equinox.transforms.Pipe;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.FrameworkEvent;
import org.osgi.util.tracker.ServiceTracker;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class XSLTStreamTransformer {

	class XSLTPipe extends Pipe {
		private Transformer transformer;
		private EntityResolver resolver;

		public XSLTPipe(InputStream original, Transformer transformer,
				EntityResolver resolver) throws IOException {
			super(original);
			this.transformer = transformer;
			this.resolver = resolver;
		}

		protected void pipeInput(InputStream original, OutputStream result)
				throws IOException {
			try {
				InputSource streamSource = new InputSource(original);
				XMLReader reader = XMLReaderFactory.createXMLReader();
				if (resolver != null)
					reader.setEntityResolver(resolver);
				else
					reader.setFeature("http://xml.org/sax/features/validation", //$NON-NLS-1$
							false);

				SAXSource saxSource = new SAXSource(reader, streamSource);
				transformer.transform(saxSource, new StreamResult(result));
			} catch (TransformerException e) {
				log(FrameworkEvent.ERROR, "Could not perform transform.", e); //$NON-NLS-1$
				throw new IOException(e.getMessage());
			} catch (SAXException e) {
				log(FrameworkEvent.ERROR, "Problem parsing transform.", e); //$NON-NLS-1$
				throw new IOException(e.getMessage());
			}
		}
	}

	private EntityResolver resolver;

	private ServiceTracker logTracker;

	private static final Map templateMap = new HashMap();

	public XSLTStreamTransformer(final File dataFile, ServiceTracker logTracker) {
		this.logTracker = logTracker;
		if (dataFile != null) {
			if (!dataFile.exists())
				dataFile.mkdir();
			else if (!dataFile.isDirectory())
				return;
			resolver = new EntityResolver() {

				Map entities = new HashMap();

				public InputSource resolveEntity(String publicId,
						String systemId) throws SAXException, IOException {

					byte[] resolved = (byte[]) entities.get(systemId);
					if (resolved == null) {
						// check to see if we have it in our bundle
						String escapedSystemId = escape(systemId);
						File localCache = new File(dataFile, escapedSystemId);
						if (localCache != null && localCache.exists()
								&& localCache.isFile()) {
							// we have it on disk so just return our cache
							return new InputSource(new FileInputStream(
									localCache));
						}

						try {
							URL systemURL = new URL(systemId);
							InputStream resolvedStream = systemURL.openStream();

							ByteArrayOutputStream resolvedOutputStream = new ByteArrayOutputStream();
							byte[] buffer = new byte[1024];
							int count = 0;
							while ((count = resolvedStream.read(buffer)) != -1) {
								resolvedOutputStream.write(buffer, 0, count);
							}
							resolved = resolvedOutputStream.toByteArray();

						} catch (MalformedURLException e) {
							// un-resolvable entity - return an empty entity to
							// continue parsing
							return new InputSource(new StringReader("")); //$NON-NLS-1$
						} catch (IOException e) {
							// un-resolvable entity - return an empty entity to
							// continue parsing
							return new InputSource(new StringReader("")); //$NON-NLS-1$

						}
						if (resolved != null)
							entities.put(systemId, resolved);

					}
					return new InputSource(new ByteArrayInputStream(resolved));
				}

				private String escape(String systemId) {
					return systemId;
				}

			};
		}
	}

	public InputStream getInputStream(InputStream inputStream,
			URL transformerUrl) throws IOException {
		Templates template = getTemplate(transformerUrl);
		if (template != null) {

			Transformer transformer = null;
			try {
				transformer = template.newTransformer();
				XSLTPipe pipe = new XSLTPipe(inputStream, transformer, resolver);
				return pipe.getPipedInputStream();
			} catch (TransformerConfigurationException e) {
				log(FrameworkEvent.ERROR, "Could not perform transform.", e); //$NON-NLS-1$
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

					InputSource inputSource = new InputSource(xsltStream);
					XMLReader reader = XMLReaderFactory.createXMLReader();
					if (resolver != null)
						reader.setEntityResolver(resolver);
					else
						reader
								.setFeature(
										"http://xml.org/sax/features/validation", //$NON-NLS-1$
										false);
					SAXSource xsltSource = new SAXSource(reader, inputSource);

					try {
						templatesRef = new SoftReference(templates = tFactory
								.newTemplates(xsltSource));
						templateMap.put(transformerURL, templatesRef);
					} catch (Exception e) {
						// can't create the template. May be an IO
						// exception from the source or perhaps a badly
						// formed XSLT. We shouldn't fail in this case.
						log(FrameworkEvent.WARNING,
								"Could not create transform template: " //$NON-NLS-1$
										+ transformerURL.toString(), e);
					}
				} catch (TransformerFactoryConfigurationError e) {
					// we can proceed without a factory
					log(
							FrameworkEvent.WARNING,
							"Could not create transformer factory.  No transforms will be invoked.", //$NON-NLS-1$
							e);
				} catch (SAXException e) {
					// we can proceed without a reader
					log(
							FrameworkEvent.WARNING,
							"Could not create XML reader.  No transforms will be invoked.", //$NON-NLS-1$
							e);
				}
			} catch (IOException e) {
				log(FrameworkEvent.WARNING, "General IO Exception creating templates.", e); //$NON-NLS-1$
			}
		}
		return templates;
	}

	void log(int severity, String msg, Throwable t) {
		FrameworkLog log = (FrameworkLog) logTracker.getService();
		if (log == null) {
			if (msg != null)
				System.err.println(msg);
			if (t != null)
				t.printStackTrace();
			return;
		}

		FrameworkLogEntry entry = new FrameworkLogEntry(
				"org.eclipse.equinox.transforms.xslt", severity, 0, msg, 0, t, //$NON-NLS-1$
				null);
		log.log(entry);
	}
}
