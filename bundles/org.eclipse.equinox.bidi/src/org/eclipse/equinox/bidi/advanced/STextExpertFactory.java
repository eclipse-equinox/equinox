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
import org.eclipse.equinox.bidi.STextTypeHandlerFactory;
import org.eclipse.equinox.bidi.custom.STextTypeHandler;
import org.eclipse.equinox.bidi.internal.STextImpl;

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
			STextTypeHandler handler = new STextTypeHandler(defaultSeparators);
			defaultExpert = new STextImpl(handler, STextEnvironment.DEFAULT, null);
		}
		return defaultExpert;
	}

	static public ISTextExpert getExpert(String type) {
		ISTextExpert expert;
		synchronized (sharedDefaultExperts) {
			expert = (ISTextExpert) sharedDefaultExperts.get(type);
			if (expert == null) {
				STextTypeHandler handler = STextTypeHandlerFactory.getHandler(type);
				if (handler == null)
					return null;
				expert = new STextImpl(handler, STextEnvironment.DEFAULT, null);
				sharedDefaultExperts.put(type, expert);
			}
		}
		return expert;
	}

	static public ISTextExpert getExpert(String type, STextEnvironment environment) {
		ISTextExpert expert;
		synchronized (sharedExperts) {
			Map experts = (Map) sharedExperts.get(type);
			if (experts == null) {
				experts = new HashMap(); // environment -> expert
				sharedExperts.put(type, experts);
			}
			expert = (ISTextExpert) experts.get(environment);
			if (expert == null) {
				STextTypeHandler handler = STextTypeHandlerFactory.getHandler(type);
				if (handler == null)
					return null;
				expert = new STextImpl(handler, environment, null);
				experts.put(type, expert);
			}
		}
		return expert;
	}

	static public ISTextExpert getExpert(STextTypeHandler handler, STextEnvironment environment) {
		return new STextImpl(handler, environment, STextState.createState());
	}

	static public ISTextExpertStateful getPrivateExpert(String type) {
		return getPrivateExpert(type, STextEnvironment.DEFAULT);
	}

	static public ISTextExpertStateful getPrivateExpert(String type, STextEnvironment environment) {
		STextTypeHandler handler = STextTypeHandlerFactory.getHandler(type);
		if (handler == null)
			return null;
		return new STextImpl(handler, environment, STextState.createState());
	}

}
