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
import org.eclipse.equinox.bidi.STextProcessor;
import org.eclipse.equinox.bidi.STextTypeHandlerFactory;
import org.eclipse.equinox.bidi.custom.STextTypeHandler;
import org.eclipse.equinox.bidi.internal.STextImpl;

/**
 * Obtains ISTextExpert instances.
 * An {@link ISTextExpert} instance (called in short an "expert") provides
 * the advanced methods to process a certain type of structured text, and 
 * is thus related to a specific 
 * {@link STextTypeHandler structured text type handler}.
 * There are two kinds of experts:
 * <ul>
 *   <li>stateful, obtained by calling {@link #getStatefulExpert}.</li>
 *   <li>not stateful, obtained by calling {@link #getExpert}.</li>
 * </ul>  
 * <p>Only the stateful kind can remember the state established by a call to
 * a text processing method and transmit it as initial state in the next call
 * to a text processing method.
 * <p>In other words, the methods 
 * {@link ISTextExpert#getState()},
 * {@link ISTextExpert#setState} and
 * {@link ISTextExpert#clearState()} of 
 * {@link ISTextExpert} are inoperative for experts which are not stateful.
 * <p>
 * Using a stateful expert is more resource intensive, thus not stateful
 * experts should be used when feasible. 
 * 
 * @author Matitiahu Allouche
 *
 */
final public class STextExpertFactory {

	/**
	 * The default set of separators used to segment a string: dot, colon, slash, backslash.
	 */
	private static final String defaultSeparators = STextProcessor.getDefaultSeparators();

	static private Map sharedDefaultExperts = new HashMap(); // String type -> expert

	static private Map sharedExperts = new HashMap(); // String type -> map of { environment -> expert }

	static private ISTextExpert defaultExpert;

	private STextExpertFactory() {
		// prevents instantiation
	}

	/**
	 * Obtains a ISTextExpert instance for processing structured text with
	 *  a default type handler segmenting the text according to default separators.
	 *  This expert instance does not handle states.
	 * @return the ISTextExpert instance.
	 * @see STextProcessor#getDefaultSeparators()
	 */
	static public ISTextExpert getExpert() {
		if (defaultExpert == null) {
			STextTypeHandler handler = new STextTypeHandler(defaultSeparators);
			defaultExpert = new STextImpl(handler, STextEnvironment.DEFAULT, false);
		}
		return defaultExpert;
	}

	/**
	 * Obtains a ISTextExpert instance for processing structured text with
	 *  the specified type handler. 
	 *  This expert instance does not handle states.
	 * 
	 * @param type the identifier for the required type handler. This identifier 
	 *             may be one of those listed in {@link STextTypeHandlerFactory}
	 *             or it may be have been registered by a plug-in.
	 * @return the ISTextExpert instance.
	 * @throws IllegalArgumentException if <code>type</code> is not a known type
	 *         identifier.
	 */
	static public ISTextExpert getExpert(String type) {
		ISTextExpert expert;
		synchronized (sharedDefaultExperts) {
			expert = (ISTextExpert) sharedDefaultExperts.get(type);
			if (expert == null) {
				STextTypeHandler handler = STextTypeHandlerFactory.getHandler(type);
				if (handler == null)
					throw new IllegalArgumentException("Invalid type argument"); //$NON-NLS-1$
				expert = new STextImpl(handler, STextEnvironment.DEFAULT, false);
				sharedDefaultExperts.put(type, expert);
			}
		}
		return expert;
	}

	/**
	 * Obtains a ISTextExpert instance for processing structured text with
	 *  the specified type handler and the specified environment.
	 *  This expert instance does not handle states.
	 * 
	 * @param type the identifier for the required type handler. This identifier 
	 *             may be one of those listed in {@link STextTypeHandlerFactory}
	 *             or it may be have been registered by a plug-in.
	 * @param  environment the current environment, which may affect the behavior of
	 *         the expert. This parameter may be specified as
	 *         <code>null</code>, in which case the
	 *         {@link STextEnvironment#DEFAULT}
	 *         environment should be assumed.
	 * @return the ISTextExpert instance.
	 * @throws IllegalArgumentException if <code>type</code> is not a known type
	 *         identifier.
	 */
	static public ISTextExpert getExpert(String type, STextEnvironment environment) {
		ISTextExpert expert;
		if (environment == null)
			environment = STextEnvironment.DEFAULT;
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
					throw new IllegalArgumentException("Invalid type argument"); //$NON-NLS-1$
				expert = new STextImpl(handler, environment, false);
				experts.put(type, expert);
			}
		}
		return expert;
	}

	/**
	 * Obtains a ISTextExpert instance for processing structured text with
	 *  the specified type handler.
	 *  This expert instance can handle states.
	 * 
	 * @param type the identifier for the required type handler. This identifier 
	 *             may be one of those listed in {@link STextTypeHandlerFactory}
	 *             or it may be have been registered by a plug-in.
	 * @return the ISTextExpert instance.
	 * @throws IllegalArgumentException if <code>type</code> is not a known type
	 *         identifier.
	 */
	static public ISTextExpert getStatefulExpert(String type) {
		return getStatefulExpert(type, STextEnvironment.DEFAULT);
	}

	/**
	 * Obtains a ISTextExpert instance for processing structured text with
	 *  the specified type handler and the specified environment.
	 *  This expert instance can handle states.
	 * 
	 * @param type the identifier for the required type handler. This identifier 
	 *             may be one of those listed in {@link STextTypeHandlerFactory}
	 *             or it may be have been registered by a plug-in.
	 * @param  environment the current environment, which may affect the behavior of
	 *         the expert. This parameter may be specified as
	 *         <code>null</code>, in which case the
	 *         {@link STextEnvironment#DEFAULT}
	 *         environment should be assumed.
	 * @return the ISTextExpert instance.
	 * @throws IllegalArgumentException if <code>type</code> is not a known type
	 *         identifier.
	 */
	static public ISTextExpert getStatefulExpert(String type, STextEnvironment environment) {
		STextTypeHandler handler = STextTypeHandlerFactory.getHandler(type);
		if (handler == null)
			throw new IllegalArgumentException("Invalid type argument"); //$NON-NLS-1$
		return getStatefulExpert(handler, environment);
	}

	/**
	 * Obtains a ISTextExpert instance for processing structured text with
	 *  the specified type handler and the specified environment.
	 *  This expert instance can handle states.
	 * 
	 * @param handler the type handler instance. It may have been obtained using 
	 *             {@link STextTypeHandlerFactory#getHandler(String)} or
	 *             by instantiating a type handler.
	 * @param  environment the current environment, which may affect the behavior of
	 *         the expert. This parameter may be specified as
	 *         <code>null</code>, in which case the
	 *         {@link STextEnvironment#DEFAULT}
	 *         environment should be assumed.
	 * @return the ISTextExpert instance.
	 * @throws IllegalArgumentException if <code>type</code> is not a known type
	 *         identifier.
	 */
	static public ISTextExpert getStatefulExpert(STextTypeHandler handler, STextEnvironment environment) {
		if (environment == null)
			environment = STextEnvironment.DEFAULT;
		return new STextImpl(handler, environment, true);
	}

}
