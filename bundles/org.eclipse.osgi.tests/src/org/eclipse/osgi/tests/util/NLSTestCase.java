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
	public void testPropertiesWithoutBom() {
		assertEquals("Lorem ipsum dolor sit amet", MessagesNoBom.key_ascii);
		assertEquals("ÀÅÆÇÈÊËÌÍÏÐÑÒÓÖ×ØÙÚÜÝÞßàâäåæçèéëìíïðñòõö÷øùúüýþ", MessagesNoBom.key_latin1);
	}

	@Test
	public void testPropertiesBomUtf8() {
		assertEquals("Ḽơᶉëᶆ ȋṕšᶙṁ", MessagesBomUtf8.key_en);
		assertEquals("顾客很高兴", MessagesBomUtf8.key_ch);
		assertEquals("고객은 매우 행복합니다", MessagesBomUtf8.key_ko);
		assertEquals("Клиент очень доволен", MessagesBomUtf8.key_ru);
	}

	@Test
	public void testPropertiesBomUtf16Be() {
		assertEquals("Ḽơᶉëᶆ ȋṕšᶙṁ", MessagesBomUtf16Be.key_en);
		assertEquals("顾客很高兴", MessagesBomUtf16Be.key_ch);
		assertEquals("고객은 매우 행복합니다", MessagesBomUtf16Be.key_ko);
		assertEquals("Клиент очень доволен", MessagesBomUtf16Be.key_ru);
	}

	@Test
	public void testPropertiesBomUtf16Le() {
		assertEquals("Ḽơᶉëᶆ ȋṕšᶙṁ", MessagesBomUtf16Le.key_en);
		assertEquals("顾客很高兴", MessagesBomUtf16Le.key_ch);
		assertEquals("고객은 매우 행복합니다", MessagesBomUtf16Le.key_ko);
		assertEquals("Клиент очень доволен", MessagesBomUtf16Le.key_ru);
	}

	public static class MessagesNoBom extends NLS {
		public static String key_ascii;
		public static String key_latin1;

		static {
			NLS.initializeMessages("org.eclipse.osgi.tests.util.nls.messages_no_bom", MessagesNoBom.class);
		}
	}

	public static class MessagesBomUtf8 extends NLS {
		public static String key_en;
		public static String key_ch;
		public static String key_ko;
		public static String key_ru;

		static {
			NLS.initializeMessages("org.eclipse.osgi.tests.util.nls.messages_bom_utf8", MessagesBomUtf8.class);
		}
	}

	public static class MessagesBomUtf16Be extends NLS {
		public static String key_en;
		public static String key_ch;
		public static String key_ko;
		public static String key_ru;

		static {
			NLS.initializeMessages("org.eclipse.osgi.tests.util.nls.messages_bom_utf16be", MessagesBomUtf16Be.class);
		}
	}

	public static class MessagesBomUtf16Le extends NLS {
		public static String key_en;
		public static String key_ch;
		public static String key_ko;
		public static String key_ru;

		static {
			NLS.initializeMessages("org.eclipse.osgi.tests.util.nls.messages_bom_utf16le", MessagesBomUtf16Le.class);
		}
	}
}
