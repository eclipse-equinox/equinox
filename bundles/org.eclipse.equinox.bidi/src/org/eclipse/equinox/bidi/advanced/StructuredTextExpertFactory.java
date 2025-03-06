/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
 ******************************************************************************/
package org.eclipse.equinox.bidi.advanced;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.equinox.bidi.StructuredTextProcessor;
import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.equinox.bidi.custom.StructuredTextTypeHandler;
import org.eclipse.equinox.bidi.internal.StructuredTextImpl;

/**
 * Obtains IStructuredTextExpert instances. An {@link IStructuredTextExpert}
 * instance (called in short an "expert") provides the advanced methods to
 * process a certain type of structured text, and is thus related to a specific
 * {@link StructuredTextTypeHandler structured text type handler}. There are two
 * kinds of experts:
 * <ul>
 * <li>stateful, obtained by calling {@link #getStatefulExpert}.</li>
 * <li>not stateful, obtained by calling {@link #getExpert}.</li>
 * </ul>
 * <p>
 * Only the stateful kind can remember the state established by a call to a text
 * processing method and transmit it as initial state in the next call to a text
 * processing method.
 * <p>
 * In other words, the methods {@link IStructuredTextExpert#getState()},
 * {@link IStructuredTextExpert#setState} and
 * {@link IStructuredTextExpert#clearState()} of {@link IStructuredTextExpert}
 * are inoperative for experts which are not stateful.
 * <p>
 * Using a stateful expert is more resource intensive, thus not stateful experts
 * should be used when feasible.
 */
final public class StructuredTextExpertFactory {

	/**
	 * The default set of separators used to segment a string: dot, colon, slash,
	 * backslash.
	 */
	private static final String defaultSeparators = StructuredTextProcessor.getDefaultSeparators();

	static private Map<String, IStructuredTextExpert> sharedDefaultExperts = new HashMap<>();

	static private Map<String, Map<StructuredTextEnvironment, IStructuredTextExpert>> sharedExperts = new HashMap<>();

	static private IStructuredTextExpert defaultExpert;

	private StructuredTextExpertFactory() {
		// prevents instantiation
	}

	/**
	 * Obtains a IStructuredTextExpert instance for processing structured text with
	 * a default type handler segmenting the text according to default separators.
	 * This expert instance does not handle states.
	 * 
	 * @return the IStructuredTextExpert instance.
	 * @see StructuredTextProcessor#getDefaultSeparators()
	 */
	static public IStructuredTextExpert getExpert() {
		if (defaultExpert == null) {
			StructuredTextTypeHandler handler = new StructuredTextTypeHandler(defaultSeparators);
			defaultExpert = new StructuredTextImpl(handler, StructuredTextEnvironment.DEFAULT, false);
		}
		return defaultExpert;
	}

	/**
	 * Obtains a IStructuredTextExpert instance for processing structured text with
	 * the specified type handler. This expert instance does not handle states.
	 * 
	 * @param type the identifier for the required type handler. This identifier may
	 *             be one of those listed in
	 *             {@link StructuredTextTypeHandlerFactory} or it may be have been
	 *             registered by a plug-in.
	 * @return the IStructuredTextExpert instance.
	 * @throws IllegalArgumentException if <code>type</code> is not a known type
	 *                                  identifier.
	 */
	static public IStructuredTextExpert getExpert(String type) {
		IStructuredTextExpert expert;
		synchronized (sharedDefaultExperts) {
			expert = sharedDefaultExperts.get(type);
			if (expert == null) {
				StructuredTextTypeHandler handler = StructuredTextTypeHandlerFactory.getHandler(type);
				if (handler == null) {
					throw new IllegalArgumentException("Invalid type argument"); //$NON-NLS-1$
				}
				expert = new StructuredTextImpl(handler, StructuredTextEnvironment.DEFAULT, false);
				sharedDefaultExperts.put(type, expert);
			}
		}
		return expert;
	}

	/**
	 * Obtains a IStructuredTextExpert instance for processing structured text with
	 * the specified type handler and the specified environment. This expert
	 * instance does not handle states.
	 * 
	 * @param type        the identifier for the required type handler. This
	 *                    identifier may be one of those listed in
	 *                    {@link StructuredTextTypeHandlerFactory} or it may be have
	 *                    been registered by a plug-in.
	 * @param environment the current environment, which may affect the behavior of
	 *                    the expert. This parameter may be specified as
	 *                    <code>null</code>, in which case the
	 *                    {@link StructuredTextEnvironment#DEFAULT} environment
	 *                    should be assumed.
	 * @return the IStructuredTextExpert instance.
	 * @throws IllegalArgumentException if <code>type</code> is not a known type
	 *                                  identifier.
	 */
	static public IStructuredTextExpert getExpert(String type, StructuredTextEnvironment environment) {
		IStructuredTextExpert expert;
		if (environment == null) {
			environment = StructuredTextEnvironment.DEFAULT;
		}
		synchronized (sharedExperts) {
			Map<StructuredTextEnvironment, IStructuredTextExpert> experts = sharedExperts.get(type);
			if (experts == null) {
				experts = new HashMap<>(); // environment -> expert
				sharedExperts.put(type, experts);
			}
			expert = experts.get(environment);
			if (expert == null) {
				StructuredTextTypeHandler handler = StructuredTextTypeHandlerFactory.getHandler(type);
				if (handler == null) {
					throw new IllegalArgumentException("Invalid type argument"); //$NON-NLS-1$
				}
				expert = new StructuredTextImpl(handler, environment, false);
				experts.put(environment, expert);
			}
		}
		return expert;
	}

	/**
	 * Obtains a IStructuredTextExpert instance for processing structured text with
	 * the specified type handler. This expert instance can handle states.
	 * 
	 * @param type the identifier for the required type handler. This identifier may
	 *             be one of those listed in
	 *             {@link StructuredTextTypeHandlerFactory} or it may be have been
	 *             registered by a plug-in.
	 * @return the IStructuredTextExpert instance.
	 * @throws IllegalArgumentException if <code>type</code> is not a known type
	 *                                  identifier.
	 */
	static public IStructuredTextExpert getStatefulExpert(String type) {
		return getStatefulExpert(type, StructuredTextEnvironment.DEFAULT);
	}

	/**
	 * Obtains a IStructuredTextExpert instance for processing structured text with
	 * the specified type handler and the specified environment. This expert
	 * instance can handle states.
	 * 
	 * @param type        the identifier for the required type handler. This
	 *                    identifier may be one of those listed in
	 *                    {@link StructuredTextTypeHandlerFactory} or it may be have
	 *                    been registered by a plug-in.
	 * @param environment the current environment, which may affect the behavior of
	 *                    the expert. This parameter may be specified as
	 *                    <code>null</code>, in which case the
	 *                    {@link StructuredTextEnvironment#DEFAULT} environment
	 *                    should be assumed.
	 * @return the IStructuredTextExpert instance.
	 * @throws IllegalArgumentException if <code>type</code> is not a known type
	 *                                  identifier.
	 */
	static public IStructuredTextExpert getStatefulExpert(String type, StructuredTextEnvironment environment) {
		StructuredTextTypeHandler handler = StructuredTextTypeHandlerFactory.getHandler(type);
		if (handler == null) {
			throw new IllegalArgumentException("Invalid type argument"); //$NON-NLS-1$
		}
		return getStatefulExpert(handler, environment);
	}

	/**
	 * Obtains a IStructuredTextExpert instance for processing structured text with
	 * the specified type handler and the specified environment. This expert
	 * instance can handle states.
	 * 
	 * @param handler     the type handler instance. It may have been obtained using
	 *                    {@link StructuredTextTypeHandlerFactory#getHandler(String)}
	 *                    or by instantiating a type handler.
	 * @param environment the current environment, which may affect the behavior of
	 *                    the expert. This parameter may be specified as
	 *                    <code>null</code>, in which case the
	 *                    {@link StructuredTextEnvironment#DEFAULT} environment
	 *                    should be assumed.
	 * @return the IStructuredTextExpert instance
	 * @throws IllegalArgumentException if the <code>handler</code> is
	 *                                  <code>null</code>
	 */
	static public IStructuredTextExpert getStatefulExpert(StructuredTextTypeHandler handler,
			StructuredTextEnvironment environment) {
		if (handler == null) {
			throw new IllegalArgumentException("handler must not be null"); //$NON-NLS-1$
		}
		if (environment == null) {
			environment = StructuredTextEnvironment.DEFAULT;
		}
		return new StructuredTextImpl(handler, environment, true);
	}

}
