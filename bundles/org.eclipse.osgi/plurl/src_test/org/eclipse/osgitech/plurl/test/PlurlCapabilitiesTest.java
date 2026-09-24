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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.eclipse.osgitech.plurl.Plurl;
import org.eclipse.osgitech.plurl.impl.PlurlImpl;
import org.junit.Test;

/**
 * A factory that can only be identified by the URL cannot be routed to correctly by
 * a plurl that does not consult {@link
 * org.eclipse.osgitech.plurl.PlurlStreamHandlerFactory#shouldHandleURL(String, String)}.
 * Since copies of different versions can be in use in the same JVM, a factory needs
 * to be able to ask what the installed implementation supports rather than assume.
 */
@SuppressWarnings("nls")
public class PlurlCapabilitiesTest {
	@Test
	public void capabilityIsReported() throws IOException {
		Plurl plurl = new PlurlImpl();
		plurl.install(Plurl.PLURL_FORBID_NOTHING);
		try {
			assertEquals("selection by spec is supported", Optional.of(Boolean.TRUE),
					Plurl.getCapability(Plurl.PLURL_CAPABILITY_SELECT_BY_SPEC));
		} finally {
			plurl.uninstall();
		}
	}

	/**
	 * A capability the implementation does not have must read as absent rather than
	 * as an error, which is what lets a factory treat an older plurl the same way.
	 */
	@Test
	public void unknownCapabilityIsAbsent() throws IOException {
		Plurl plurl = new PlurlImpl();
		plurl.install(Plurl.PLURL_FORBID_NOTHING);
		try {
			assertEquals("an unknown capability is not claimed", Optional.empty(),
					Plurl.getCapability("somethingElseEntirely"));
		} finally {
			plurl.uninstall();
		}
	}

	/**
	 * The operation must be reachable through the protocol form the javadoc
	 * documents, which parses with a leading slash in the path.
	 */
	@Test
	public void capabilityReachableThroughTheProtocol() throws IOException {
		Plurl plurl = new PlurlImpl();
		plurl.install(Plurl.PLURL_FORBID_NOTHING);
		try {
			Object content = new URL(
					"plurl://op/" + Plurl.PLURL_GET_CAPABILITY + "/" + Plurl.PLURL_CAPABILITY_SELECT_BY_SPEC)
							.openConnection().getContent();
			assertEquals(Boolean.TRUE, content);
		} finally {
			plurl.uninstall();
		}
	}

	/**
	 * An implementation that predates an operation rejects it, and that rejection is
	 * how a factory tells an older plurl apart from one that answers.
	 */
	@Test
	public void unknownOperationIsRejected() throws IOException {
		Plurl plurl = new PlurlImpl();
		plurl.install(Plurl.PLURL_FORBID_NOTHING);
		try {
			new URL("plurl://op/noSuchOperation").openConnection().getContent();
			fail("an unknown operation must be rejected");
		} catch (IOException e) {
			// expected: getCapability turns this into null
		} finally {
			plurl.uninstall();
		}
	}
}
