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
import org.eclipse.equinox.bidi.internal.STextProcessorImplNew;
import org.eclipse.equinox.bidi.internal.STextProcessorMultipassImplNew;

final public class STextProcessorFactoryNew {

	/**
	 * The default set of separators used to segment a string: dot, colon, slash, backslash.
	 */
	private static final String defaultSeparators = ".:/\\"; //$NON-NLS-1$

	static private Map sharedDefaultProcessors = new HashMap(); // String id -> processor

	static private Map sharedProcessors = new HashMap(); // String id -> map { environment -> processor }

	static private STextProcessorNew defaultProcessor;

	private STextProcessorFactoryNew() {
		// prevents instantiation
	}

	static public STextProcessorNew getProcessor() {
		if (defaultProcessor == null) {
			STextProcessor descriptor = new STextProcessor(defaultSeparators);
			defaultProcessor = new STextProcessorImplNew(descriptor, STextEnvironment.DEFAULT);
		}
		return defaultProcessor;
	}

	static public STextProcessorNew getProcessor(String type) {
		STextProcessorNew processor;
		synchronized (sharedDefaultProcessors) {
			processor = (STextProcessorNew) sharedDefaultProcessors.get(type);
			if (processor == null) {
				STextProcessor descriptor = STextProcessorFactory.getProcessor(type);
				processor = new STextProcessorImplNew(descriptor, STextEnvironment.DEFAULT);
				sharedDefaultProcessors.put(type, processor);
			}
		}
		return processor;
	}

	static public STextProcessorNew getProcessor(String type, STextEnvironment environment) {
		STextProcessorNew processor;
		synchronized (sharedProcessors) {
			Map processors = (Map) sharedProcessors.get(type);
			if (processors == null) {
				processors = new HashMap(); // environment -> processor
				sharedProcessors.put(type, processors);
			}
			processor = (STextProcessorNew) processors.get(environment);
			if (processor == null) {
				STextProcessor descriptor = STextProcessorFactory.getProcessor(type);
				processor = new STextProcessorImplNew(descriptor, environment);
				processors.put(type, processor);
			}
		}
		return processor;
	}

	// TBD could this be removed?
	static public STextProcessorNew getProcessor(STextProcessor descriptor, STextEnvironment environment) {
		return new STextProcessorImplNew(descriptor, environment);
	}

	static public STextProcessorMultipassNew getMultipassProcessor(String type) {
		return getMultipassProcessor(type, STextEnvironment.DEFAULT);
	}

	static public STextProcessorMultipassNew getMultipassProcessor(String type, STextEnvironment environment) {
		STextProcessor descriptor = STextProcessorFactory.getProcessor(type);
		return new STextProcessorMultipassImplNew(descriptor, environment);
	}

}
