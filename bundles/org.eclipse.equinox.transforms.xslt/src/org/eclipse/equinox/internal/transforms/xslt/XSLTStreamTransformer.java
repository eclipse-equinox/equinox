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

package org.eclipse.equinox.internal.transforms.xslt;

import java.io.*;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import org.eclipse.equinox.internal.transforms.Pipe;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.FrameworkEvent;
import org.osgi.util.tracker.ServiceTracker;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Implements the XSLT stream transformer. 
 * This class is capable of taking a source stream and an URL and applying the contents of the URL as a XSLT transform to the contents of the stream.
 */
public class XSLTStreamTransformer {

	/**
	 * Subclass of Pipe that is able to apply XSLT Transformers to the original input stream.  
	 * All handling of XML is done with validation and entity resolution disabled to improve performance and prevent undesired network access.
	 */
	class XSLTPipe extends Pipe {
		private Transformer transformer;

		public XSLTPipe(InputStream original, Transformer transformer) throws IOException {
			super(original);
			this.transformer = transformer;
		}

		protected void pipeInput(InputStream original, OutputStream result) throws IOException {
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

	/**
	 * The dummy entity resolver which returns empty content for all external entity requests.
	 */
	protected EntityResolver resolver = new EntityResolver() {
		public InputSource resolveEntity(String publicId, String systemId) {
			// don't validate external entities - too expensive
			return new InputSource(new StringReader("")); //$NON-NLS-1$
		}
	};

	/**
	 * Tracks the logging service.
	 */
	private ServiceTracker logTracker;

	/**
	 * A map containing compiled XSLT transformations.  
	 * These transforms are held by soft references so that we don't bloat memory for this purpose.  
	 * After startup these transforms are of little use.
	 */
	private static final Map templateMap = new HashMap();

	/**
	 * Create a new instance of this transformer.
	 * 
	 * @param logTracker the log service 
	 */
	public XSLTStreamTransformer(ServiceTracker logTracker) {
		this.logTracker = logTracker;
	}

	/**
	 * Implements the StreamTransformer.getInput(InputStream, URL) method.
	 * @param inputStream the original stream
	 * @param transformerUrl the transformer URL.  This should be an URL pointing to an XSLT transform.
	 * @return the transformed input stream
	 * @throws IOException thrown if there is an issue reading from the original stream or applying the transform.
	 */
	public InputStream getInputStream(InputStream inputStream, URL transformerUrl) throws IOException {
		Templates template = getTemplate(transformerUrl);
		if (template != null) {

			Transformer transformer = null;
			try {
				transformer = template.newTransformer();
				XSLTPipe pipe = new XSLTPipe(inputStream, transformer);
				return pipe.getPipedInputStream();
			} catch (TransformerConfigurationException e) {
				log(FrameworkEvent.ERROR, "Could not perform transform.", e); //$NON-NLS-1$
			}
		}

		return null;
	}

	/**
	 * Get a cached template for the provided XSLT template URL.  
	 * If the cached entry for this URL does not exist it will be created.
	 * @param transformerURL the XSLT template URL.
	 * @return the template
	 */
	private synchronized Templates getTemplate(URL transformerURL) {
		Templates templates = null;

		SoftReference templatesRef = (SoftReference) templateMap.get(transformerURL);
		if (templatesRef != null) {
			templates = (Templates) templatesRef.get();
		}

		if (templates != null)
			return templates;

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
					reader.setFeature("http://xml.org/sax/features/validation", //$NON-NLS-1$
							false);
				SAXSource xsltSource = new SAXSource(reader, inputSource);

				try {
					templatesRef = new SoftReference(templates = tFactory.newTemplates(xsltSource));
					templateMap.put(transformerURL, templatesRef);
				} catch (Exception e) {
					// can't create the template. May be an IO
					// exception from the source or perhaps a badly
					// formed XSLT. We shouldn't fail in this case.
					log(FrameworkEvent.WARNING, "Could not create transform template: " //$NON-NLS-1$
							+ transformerURL.toString(), e);
				}
			} catch (TransformerFactoryConfigurationError e) {
				// we can proceed without a factory
				log(FrameworkEvent.WARNING, "Could not create transformer factory.  No transforms will be invoked.", //$NON-NLS-1$
						e);
			} catch (SAXException e) {
				// we can proceed without a reader
				log(FrameworkEvent.WARNING, "Could not create XML reader.  No transforms will be invoked.", //$NON-NLS-1$
						e);
			}
		} catch (IOException e) {
			log(FrameworkEvent.WARNING, "General IO Exception creating templates.", e); //$NON-NLS-1$
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

		FrameworkLogEntry entry = new FrameworkLogEntry("org.eclipse.equinox.transforms.xslt", severity, 0, msg, 0, t, //$NON-NLS-1$
				null);
		log.log(entry);
	}
}
