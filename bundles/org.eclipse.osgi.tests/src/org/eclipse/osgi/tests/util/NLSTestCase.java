/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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

package org.eclipse.osgi.tests.util;

import static org.junit.Assert.assertEquals;

import org.eclipse.osgi.util.NLS;
import org.junit.Test;

public class NLSTestCase {

	@Test
	public void testEmptyMessageBug200296() {
		NLS.bind("", Integer.valueOf(0));
	}

	@Test
	public void testPropertiesLatin1() {
		assertEquals("Lorem ipsum dolor sit amet", MessagesLatin1.key_ascii);
		assertEquals("ÀÅÆÇÈÊËÌÍÏÐÑÒÓÖ×ØÙÚÜÝÞßàâäåæçèéëìíïðñòõö÷øùúüýþ", MessagesLatin1.key_latin1);
	}

	@Test
	public void testPropertiesUtf8() {
		assertEquals("Ḽơᶉëᶆ ȋṕšᶙṁ", MessagesUtf8.key_en);
		assertEquals("顾客很高兴", MessagesUtf8.key_ch);
		assertEquals("고객은 매우 행복합니다", MessagesUtf8.key_ko);
		assertEquals("Клиент очень доволен", MessagesUtf8.key_ru);
	}

	public static class MessagesLatin1 extends NLS {
		public static String key_ascii;
		public static String key_latin1;

		static {
			NLS.initializeMessages("org.eclipse.osgi.tests.util.nls.messages_latin1", MessagesLatin1.class);
		}
	}

	public static class MessagesUtf8 extends NLS {
		public static String key_en;
		public static String key_ch;
		public static String key_ko;
		public static String key_ru;

		static {
			NLS.initializeMessages("org.eclipse.osgi.tests.util.nls.messages_utf8", MessagesUtf8.class);
		}
	}
}
