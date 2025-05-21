/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package org.eclipse.equinox.plurl;

import java.io.IOException;
import java.net.ContentHandlerFactory;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandlerFactory;
import java.util.function.Consumer;

/**
 * Plurl is used to multiplex the URL factory singletons for
 * {@link URL#setURLStreamHandlerFactory(URLStreamHandlerFactory)} and
 * {@link URLConnection#setContentHandlerFactory(ContentHandlerFactory)}. Plurl
 * factories may be added and removed using the add and remove methods or using
 * the {@link #PLURL_PROTOCOL plurl} protocol.
 * 
 * <p>
 * The {@link #PLURL_PROTOCOL plurl} protocol allows factories to be added even
 * if the installed plurl implementation is not using the same
 * org.eclipse.equinox.plurl package as the factories being registered. A plurl
 * implementation must handle this case by reflecting on the plurl factories
 * that are added. A plurl factory can be added and removed with the plurl
 * protocol like this:
 * 
 * <pre>
 * PlurlStreamHandlerFactory myStreamFactory = getStreamFactory();
 * PlurlContentHandlerFactory myContentFactory = getContentFactory();
 * 
 * ((Consumer&lt;URLStreamHandlerFactory&gt;) ("plurl://op/addURLStreamHandlerFactory").getContent()).accept(myStreamFactory);
 * ((Consumer&lt;ContentHandlerFactory&gt;) ("plurl://op/addContentHandlerFactory").getContent()).accept(myContentFactory);
 *
 * ((Consumer&lt;URLStreamHandlerFactory&gt;) ("plurl://op/removeURLStreamHandlerFactory").getContent())
 * 		.accept(myStreamFactory);
 * ((Consumer&lt;ContentHandlerFactory&gt;) ("plurl://op/removeContentHandlerFactory").getContent()).accept(myContentFactory);
 * </pre>
 * 
 * The content provided by the plurl protocol is of type {@link Consumer} which
 * can take either an {@link URLStreamHandlerFactory} or a
 * {@link ContentHandlerFactory} depending on the operation.
 * 
 * <p>
 * A plurl implementation delegates to the added {@link PlurlFactory} objects.
 * To select which {@code PlurlFactory} to delegate the
 * {@link PlurlFactory#shouldHandle(Class)} method is used.
 * <p>
 * If only one factory has been added to plurl then that {@code PlurlFactory} is
 * used to create the handler. Otherwise each
 * {@link PlurlFactory#shouldHandle(Class)} is called for a class in the call
 * stack until a factory returns true. If no factory returns true then the next
 * class in the call stack is used. If no factory is found after using all
 * classes in the call stack then the first factory added is selected. Once a
 * factory is selected, it is used to create the requested handler. If the
 * selected factory returns a {@code null} handler then no other factory is
 * asked to create the handler.
 * 
 * @see #PLURL_ADD_URL_STREAM_HANDLER_FACTORY
 * @see #PLURL_ADD_CONTENT_HANDLER_FACTORY
 * @see #PLURL_REMOVE_URL_STREAM_HANDLER_FACTORY
 * @see #PLURL_REMOVE_CONTENT_HANDLER_FACTORY
 */
public interface Plurl {
	/**
	 * The "plurl" protocol to add and remove plurl factories.
	 */
	public static final String PLURL_PROTOCOL = "plurl"; //$NON-NLS-1$
	/**
	 * The host to use for the "plurl" protocol to indicate an operation for adding
	 * or removing factories.
	 */
	public static final String PLURL_OP = "op"; //$NON-NLS-1$
	/**
	 * The plurl protocol operation to add a URLStreamHandlerFactory
	 */
	public static final String PLURL_ADD_URL_STREAM_HANDLER_FACTORY = "addURLStreamHandlerFactory"; //$NON-NLS-1$
	/**
	 * The plurl protocol operation to remove a URLStreamHandlerFactory
	 */
	public static final String PLURL_REMOVE_URL_STREAM_HANDLER_FACTORY = "removeURLStreamHandlerFactory"; //$NON-NLS-1$

	/**
	 * The plurl protocol operation to add a ContentStreamHandlerFactory
	 */
	public static final String PLURL_ADD_CONTENT_HANDLER_FACTORY = "addContentHandlerFactory"; //$NON-NLS-1$

	/**
	 * The plurl protocol operation to remove a ContentStreamHandlerFactory
	 */
	public static final String PLURL_REMOVE_CONTENT_HANDLER_FACTORY = "removeContentHandlerFactory"; //$NON-NLS-1$

	/**
	 * The value to use for the {@link #install(String...)} method to indicate that
	 * no protocols are forbidden. for overriding by plurl handlers.
	 */
	public static final String PLURL_FORBID_NOTHING = "plurlForbidNothing"; //$NON-NLS-1$

	/**
	 * Installs plurl factories into the JVM singletons. If plurl factories are
	 * already installed then this is a no-op. If the plurl factories cannot be
	 * installed then an {@code IllegalStateException} is thrown.
	 * <p>
	 * When this method returns without throwing an exception then the following
	 * will be true:
	 * <ol>
	 * <li>The singleton
	 * {@link URL#setURLStreamHandlerFactory(URLStreamHandlerFactory)} is set with a
	 * plurl implementation which delegates to the {@link PlurlStreamHandlerFactory}
	 * objects that have been {@link #add(PlurlStreamHandlerFactory) added}.
	 * <li>The singleton
	 * {@link URLConnection#setContentHandlerFactory(ContentHandlerFactory)} is set
	 * with a plurl implementation which delegates to the
	 * {@link PlurlContentHandlerFactory} objects that have been
	 * {@link #add(PlurlContentHandlerFactory) added}.</li>
	 * <li>The plurl protocol is available for creating {@code URL} objects.</li>
	 * </ol>
	 * 
	 * @param forbidden builtin JVM protocols that cannot be overridden by plurl. If
	 *                  no forbidden protocols are specified then the default
	 *                  forbidden protocols are 'jar', 'jmod', 'file', and 'jrt'. To
	 *                  forbid no protocols then use the value
	 *                  {@link #PLURL_FORBID_NOTHING}
	 * @throws IllegalStateException if the Plurl factories cannot be installed
	 */
	public void install(String... forbidden);

	/**
	 * Adds a {@link PlurlStreamHandlerFactory} to an {@link #install installed}
	 * plurl implementation. If there is no plurl implementation installed then an
	 * {@link IOException} is thrown. The plurl implementation must not hold any
	 * strong references to the factory. If the factory is garbage collected then
	 * the plurl implementation must behave as if the factory got
	 * {@link #remove(PlurlStreamHandlerFactory) removed}.
	 * <p>
	 * This is a convenience method for using the plurl protocol like this:
	 * 
	 * <pre>
	 * ((Consumer&lt;URLStreamHandlerFactory&gt;) ("plurl://op/addURLStreamHandlerFactory").getContent()).accept(factory);
	 * </pre>
	 * 
	 * @param factory the PlurlStreamHandlerFactory to add
	 * @throws IOException if there is no plurl implementation installed or there
	 *                     was an error adding the factory
	 */
	public static void add(PlurlStreamHandlerFactory factory) throws IOException {
		URL plurl = new URL(Plurl.PLURL_PROTOCOL, Plurl.PLURL_OP, Plurl.PLURL_ADD_URL_STREAM_HANDLER_FACTORY);
		@SuppressWarnings("unchecked")
		Consumer<URLStreamHandlerFactory> addFactory = (Consumer<URLStreamHandlerFactory>) plurl.openConnection()
				.getContent();
		addFactory.accept(factory);
	}

	/**
	 * Removes a {@link PlurlStreamHandlerFactory} to an {@link #install installed}
	 * plurl implementation. If there is no plurl implementation installed then an
	 * {@link IOException} is thrown.
	 * <p>
	 * This is a convenience method for using the plurl protocol like this:
	 * 
	 * <pre>
	 * ((Consumer&lt;URLStreamHandlerFactory&gt;) ("plurl://op/removeURLStreamHandlerFactory").getContent()).accept(factory);
	 * </pre>
	 * 
	 * @param factory the PlurlStreamHandlerFactory to remove
	 * @throws IOException if there is no plurl implementation installed or there
	 *                     was an error removing the factory
	 */
	public static void remove(PlurlStreamHandlerFactory factory) throws IOException {
		URL plurl = new URL(Plurl.PLURL_PROTOCOL, Plurl.PLURL_OP, Plurl.PLURL_REMOVE_URL_STREAM_HANDLER_FACTORY);
		@SuppressWarnings("unchecked")
		Consumer<URLStreamHandlerFactory> removeFactory = (Consumer<URLStreamHandlerFactory>) plurl.openConnection()
				.getContent();
		removeFactory.accept(factory);
	}

	/**
	 * Adds a {@link PlurlContentHandlerFactory} from an {@link #install installed}
	 * plurl implementation. If there is no plurl implementation installed then an
	 * {@link IOException} is thrown. The plurl implementation must not hold any
	 * strong references to the factory. If the factory is garbage collected then
	 * the plurl implementation must behave as if the factory got
	 * {@link #remove(PlurlContentHandlerFactory) removed}.
	 * <p>
	 * This is a convenience method for using the plurl protocol like this:
	 * 
	 * <pre>
	 * ((Consumer&lt;ContentHandlerFactory&gt;) ("plurl://op/addContentHandlerFactory").getContent()).accept(factory);
	 * </pre>
	 * 
	 * @param factory the PlurlContentHandlerFactory to add
	 * @throws IOException if there is no plurl implementation installed or there
	 *                     was an error adding the factory
	 */
	public static void add(PlurlContentHandlerFactory factory) throws IOException {
		URL plurl = new URL(Plurl.PLURL_PROTOCOL, Plurl.PLURL_OP, Plurl.PLURL_ADD_CONTENT_HANDLER_FACTORY);
		@SuppressWarnings("unchecked")
		Consumer<ContentHandlerFactory> addFactory = (Consumer<ContentHandlerFactory>) plurl.openConnection()
				.getContent();
		addFactory.accept(factory);
	}

	/**
	 * Removes a {@link PlurlContentHandlerFactory} from an {@link #install
	 * installed} plurl implementation. If there is no plurl implementation
	 * installed then an {@link IOException} is thrown.
	 * <p>
	 * This is a convenience method for using the plurl protocol like this:
	 * 
	 * <pre>
	 * ((Consumer&lt;ContentHandlerFactory&gt;) ("plurl://op/removeContentHandlerFactory").getContent()).accept(factory);
	 * </pre>
	 * 
	 * @param factory the PlurlContentHandlerFactory to remove
	 * @throws IOException if there is no plurl implementation installed or there
	 *                     was an error removing the factory
	 */
	public static void remove(PlurlContentHandlerFactory factory) throws IOException {
		URL plurl = new URL(Plurl.PLURL_PROTOCOL, Plurl.PLURL_OP, Plurl.PLURL_REMOVE_CONTENT_HANDLER_FACTORY);
		@SuppressWarnings("unchecked")
		Consumer<ContentHandlerFactory> removeFactory = (Consumer<ContentHandlerFactory>) plurl.openConnection()
				.getContent();
		removeFactory.accept(factory);
	}
}
