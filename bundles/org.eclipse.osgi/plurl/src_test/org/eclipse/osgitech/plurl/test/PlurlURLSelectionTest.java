/*
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.osgitech.plurl.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.function.Consumer;
import java.net.URLStreamHandlerFactory;

import org.eclipse.osgitech.plurl.Plurl;
import org.eclipse.osgitech.plurl.PlurlStreamHandlerBase;
import org.eclipse.osgitech.plurl.PlurlStreamHandlerFactory;
import org.eclipse.osgitech.plurl.impl.PlurlImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Several parties may share one protocol and be distinguishable only by the URL
 * itself, for example multiple instances of the same framework where the owner is
 * identified by an id in the URL host. Such a URL can also be used by a caller that
 * no factory recognizes from the call stack, which leaves nothing to select on.
 *
 * @see org.eclipse.osgitech.plurl.PlurlStreamHandlerFactory#shouldHandleURL(String, String)
 */
@SuppressWarnings("nls")
public class PlurlURLSelectionTest {
	static final String PROTOCOL = "plurlowner";
	/**
	 * Used by one test only. The JVM caches a handler per protocol for its lifetime,
	 * so a protocol another test has already had claimed cannot be used to assert
	 * anything about claiming it.
	 */
	static final String GATE_PROTOCOL = "plurlgateowner";

	/**
	 * Claims only the URLs whose host names it, and never claims by call stack, so
	 * selection can only succeed through the URL being parsed.
	 */
	static class OwnerFactory implements PlurlStreamHandlerFactory {
		final String owner;

		OwnerFactory(String owner) {
			this.owner = owner;
		}

		@Override
		public boolean shouldHandle(Class<?> clazz) {
			return false;
		}

		@Override
		public boolean shouldHandleURL(String protocol, String spec) {
			// A null spec is the protocol level question, asked before any URL exists:
			// yes, URLs of this protocol are ours to select on.
			return PROTOCOL.equals(protocol) && (spec == null || owner.equals(hostOf(spec)));
		}

		@Override
		public URLStreamHandler createURLStreamHandler(String protocol) {
			return PROTOCOL.equals(protocol) ? new OwnerHandler(owner) : null;
		}
	}

	/**
	 * A factory that predates {@code shouldHandleURL}, as one compiled against an
	 * older plurl would be: it has the required {@code shouldHandle(Class)} and no
	 * URL selection method at all. It must still register and take part in selection
	 * by call stack, and simply never claim by URL.
	 */
	static class PredatesURLSelectionFactory implements URLStreamHandlerFactory {
		public boolean shouldHandle(Class<?> clazz) {
			return PlurlURLSelectionTest.class.equals(clazz);
		}

		@Override
		public URLStreamHandler createURLStreamHandler(String protocol) {
			return PROTOCOL.equals(protocol) ? new OwnerHandler("predates") : null;
		}
	}

	/**
	 * Claims a protocol none of the other factories serve, by host, and answers the
	 * protocol level question so the protocol is claimed from the JVM at all.
	 */
	static class GateOwnerFactory implements PlurlStreamHandlerFactory {
		@Override
		public boolean shouldHandle(Class<?> clazz) {
			return false;
		}

		@Override
		public boolean shouldHandleURL(String protocol, String spec) {
			return GATE_PROTOCOL.equals(protocol) && (spec == null || "owner".equals(hostOf(spec)));
		}

		@Override
		public URLStreamHandler createURLStreamHandler(String protocol) {
			return GATE_PROTOCOL.equals(protocol) ? new OwnerHandler("gate") : null;
		}
	}

	/**
	 * The owner is in the host of the spec, which has to be read out of the string
	 * because no URL exists to ask yet.
	 */
	static String hostOf(String spec) {
		int start = spec.indexOf("//");
		if (start < 0) {
			return null;
		}
		start += 2;
		int end = start;
		while (end < spec.length() && "/?#".indexOf(spec.charAt(end)) < 0) {
			end++;
		}
		return spec.substring(start, end);
	}

	static class OwnerHandler extends PlurlStreamHandlerBase {
		final String owner;

		OwnerHandler(String owner) {
			this.owner = owner;
		}

		@Override
		public URLConnection openConnection(URL u) throws IOException {
			return new URLConnection(u) {
				@Override
				public void connect() throws IOException {
					// nothing to do
				}

				@Override
				public Object getContent() throws IOException {
					return owner;
				}
			};
		}
	}

	private Plurl plurl;
	private OwnerFactory first;
	private OwnerFactory second;

	@Before
	public void installPlurl() throws IOException {
		plurl = new PlurlImpl();
		plurl.install(Plurl.PLURL_FORBID_NOTHING);
		// Two factories, so plurl cannot take the single factory shortcut and has to
		// select between them.
		first = new OwnerFactory("first");
		second = new OwnerFactory("second");
		Plurl.add(first);
		Plurl.add(second);
	}

	@After
	public void uninstallPlurl() throws IOException {
		Plurl.remove(first);
		Plurl.remove(second);
		plurl.uninstall();
	}

	/**
	 * Each URL must be served by the factory that claims it, not by whichever factory
	 * was added first.
	 */
	@Test
	public void urlIsServedByItsOwner() throws IOException {
		assertEquals("second", new URL(PROTOCOL + "://second/resource").getContent());
		assertEquals("first", new URL(PROTOCOL + "://first/resource").getContent());
	}

	/**
	 * The same must hold for a URL rebuilt from its external form, which is the case
	 * that has nothing else to select on: nothing in the call stack belongs to either
	 * factory.
	 */
	@Test
	public void reparsedUrlIsServedByItsOwner() throws IOException {
		URL url = new URL(PROTOCOL + "://second/resource");
		assertEquals("second", new URL(url.toExternalForm()).getContent());
	}

	/**
	 * A factory with no URL selection method must not stop one that has it from
	 * claiming its URL. Registered first, so it would win any tie, and reached
	 * through the reflective path since it does not implement PlurlStreamHandlerFactory.
	 */
	@Test
	public void factoryPredatingUrlSelectionDoesNotClaim() throws IOException {
		PredatesURLSelectionFactory predates = new PredatesURLSelectionFactory();
		addRawFactory(predates);
		try {
			assertEquals("second", new URL(PROTOCOL + "://second/resource").getContent());
		} finally {
			removeRawFactory(predates);
		}
	}

	/**
	 * And it still takes part in selection by call stack, which is the only way it
	 * can be chosen. Asserted through a host no other factory owns.
	 */
	@Test
	public void factoryPredatingUrlSelectionStillSelectedByCallStack() throws IOException {
		PredatesURLSelectionFactory predates = new PredatesURLSelectionFactory();
		addRawFactory(predates);
		try {
			assertEquals("predates", new URL(PROTOCOL + "://unclaimed/resource").getContent());
		} finally {
			removeRawFactory(predates);
		}
	}

	@SuppressWarnings("unchecked")
	private static void addRawFactory(URLStreamHandlerFactory f) throws IOException {
		// A factory that is not a PlurlStreamHandlerFactory registers through the
		// protocol, which is how a copy from another plurl version would arrive.
		((Consumer<URLStreamHandlerFactory>) new URL(Plurl.PLURL_PROTOCOL, Plurl.PLURL_OP,
				Plurl.PLURL_ADD_URL_STREAM_HANDLER_FACTORY).openConnection().getContent()).accept(f);
	}

	@SuppressWarnings("unchecked")
	private static void removeRawFactory(URLStreamHandlerFactory f) throws IOException {
		((Consumer<URLStreamHandlerFactory>) new URL(Plurl.PLURL_PROTOCOL, Plurl.PLURL_OP,
				Plurl.PLURL_REMOVE_URL_STREAM_HANDLER_FACTORY).openConnection().getContent()).accept(f);
	}

	/**
	 * The protocol has to be claimed from the JVM even when the factory plurl falls
	 * back to does not serve it. plurl is asked for a handler once per protocol and
	 * given no URL, so if it declines there, no URL of that protocol is ever parsed
	 * and selection by URL never runs.
	 * <p>
	 * The factories registered before this one serve a different protocol, which is
	 * the shape of another framework's factory in a shared JVM. This is why a factory
	 * that selects on the URL has to answer the protocol level question: answering
	 * false leaves the protocol unclaimed and the URL fails with "unknown protocol".
	 */
	@Test
	public void protocolIsClaimedWhenTheFallbackFactoryDoesNotServeIt() throws IOException {
		GateOwnerFactory gateOwner = new GateOwnerFactory();
		Plurl.add(gateOwner);
		try {
			assertEquals("gate", new URL(GATE_PROTOCOL + "://owner/resource").getContent());
		} finally {
			Plurl.remove(gateOwner);
		}
	}
}
