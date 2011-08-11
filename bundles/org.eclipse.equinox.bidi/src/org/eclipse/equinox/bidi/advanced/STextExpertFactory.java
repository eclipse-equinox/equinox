/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.bidi.advanced;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.bidi.STextProcessorFactory;
import org.eclipse.equinox.bidi.custom.STextProcessor;
import org.eclipse.equinox.bidi.internal.STextExpertImpl;
import org.eclipse.equinox.bidi.internal.STextExpertMultipassImpl;

final public class STextExpertFactory {

	/**
	 * The default set of separators used to segment a string: dot, colon, slash, backslash.
	 */
	private static final String defaultSeparators = ".:/\\"; //$NON-NLS-1$

	static private Map sharedDefaultExperts = new HashMap(); // String type -> expert

	static private Map sharedExperts = new HashMap(); // String type -> map of { environment -> expert }

	static private ISTextExpert defaultExpert;

	private STextExpertFactory() {
		// prevents instantiation
	}

	static public ISTextExpert getExpert() {
		if (defaultExpert == null) {
			STextProcessor descriptor = new STextProcessor(defaultSeparators);
			defaultExpert = new STextExpertImpl(descriptor, STextEnvironment.DEFAULT);
		}
		return defaultExpert;
	}

	static public ISTextExpert getExpert(String type) {
		ISTextExpert processor;
		synchronized (sharedDefaultExperts) {
			processor = (ISTextExpert) sharedDefaultExperts.get(type);
			if (processor == null) {
				STextProcessor descriptor = STextProcessorFactory.getProcessor(type);
				if (descriptor == null)
					return null;
				processor = new STextExpertImpl(descriptor, STextEnvironment.DEFAULT);
				sharedDefaultExperts.put(type, processor);
			}
		}
		return processor;
	}

	static public ISTextExpert getExpert(String type, STextEnvironment environment) {
		ISTextExpert processor;
		synchronized (sharedExperts) {
			Map processors = (Map) sharedExperts.get(type);
			if (processors == null) {
				processors = new HashMap(); // environment -> processor
				sharedExperts.put(type, processors);
			}
			processor = (ISTextExpert) processors.get(environment);
			if (processor == null) {
				STextProcessor descriptor = STextProcessorFactory.getProcessor(type);
				if (descriptor == null)
					return null;
				processor = new STextExpertImpl(descriptor, environment);
				processors.put(type, processor);
			}
		}
		return processor;
	}

	static public ISTextExpert getExpert(STextProcessor descriptor, STextEnvironment environment) {
		return new STextExpertImpl(descriptor, environment);
	}

	static public ISTextExpertStateful getPrivateExpert(String type) {
		return getPrivateExpert(type, STextEnvironment.DEFAULT);
	}

	static public ISTextExpertStateful getPrivateExpert(String type, STextEnvironment environment) {
		STextProcessor descriptor = STextProcessorFactory.getProcessor(type);
		if (descriptor == null)
			return null;
		return new STextExpertMultipassImpl(descriptor, environment);
	}

}
