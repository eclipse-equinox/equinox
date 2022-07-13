/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others
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
package org.eclipse.equinox.metatype.tests;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import org.junit.*;
import org.osgi.framework.Bundle;
import org.osgi.service.metatype.*;

/*
 * The main idea of this defect was support for the Char attribute definition
 * type. The specification does not limit the Char type to non-whitespace
 * characters only. However, the whitespace stripping rules imposed on
 * implementations in terms of the 'default' XML attribute on the <AD> element
 * and the AttributeDefinition.getDefaultValue() method made supporting
 * whitespace characters impossible.
 * 
 * CPEG did not want to remove the whitespace stripping rules out of concern
 * for backwards compatibility. Consequently, it was decided that significant
 * whitespace should be escaped with '\', like the comma must be when it should 
 * not be used as a delimiter. Furthermore, the same whitespace stripping and
 * escape rules should apply to the AttributeDefinition.validate(String) method.
 * This decision, of course, had ramifications extending beyond the resolution 
 * of the original issue, which is the reason for the complexity of this test.
 * 
 * The Equinox Metatype implementation employs the following strategy.
 * 
 * (1) Significant whitespace at the beginning or end of the 'default' XML
 *     attribute within the <AD> element must be escaped; otherwise, it will be
 *     stripped.
 * (2) Significant whitespace at the beginning or end of each comma delimited 
 *     token within the 'default' attribute must be escaped; otherwise, it will
 *     be stripped.
 * (3) Significant whitespace at the beginning or end of the argument passed to
 *     the validate() method must be escaped; otherwise, it will be stripped.
 * (4) Significant whitespace at the beginning or end of the 'value' XML
 *     attribute within the <Option> element must be escaped; otherwise, it
 *     will be stripped.
 * (5) Escaping whitespace between two non-whitespace characters is permitted 
 *     but not required. In other words, whitespace between two non-whitespace
 *     characters will never be stripped.
 * (6) An escape character occurring as the last character in the sequence will
 *     be treated the same as insignificant whitespace.
 * (7) Escape characters will not be preserved in the results of 
 *     AttributeDefinition.getDefaultValue() or
 *     AttributeDefinition.getOptionValues(). This has the nonintuitive
 *     consequence that
 *     AttributeDefinition.validate(AttributeDefinition.getDefaultValue()[i])
 *     and
 *     AttributeDefinition.validate(AttributeDefinition.getOptionValues()[i])
 *     will not necessarily pass validation. However, preserving escape
 *     characters in the result would probably be even more nonintuitive.
 *     Moreover, this approach is not inconsistent with the requirement on 
 *     clients to escape certain characters (',', '\', and leading or trailing 
 *     significant whitespace) on other parameters to the validate() method.
 *     Finally, the two operations referenced above are completely superfluous
 *     since it must be the case that any declared default or option value is
 *     valid.
 * (8) Null parameters passed to AttributeDefinition.validate(String) are
 *     always invalid.
 * (9) Empty string parameters passed to AttributeDefinition.validate(String)
 *     are always valid for the String type, even for cardinality zero
 *     (required by the CT), unless restricted by options.
 *     Furthermore, a sequence of comma-delimited empty strings is valid for
 *     cardinality < -1 and cardinality > 1. For example, given a
 *     cardinality of 5, AttributeDefinition.validate(",,,,") would pass.
 *(10) In order to be valid, a value must pass all of the following tests.
 *          (a) The value must not be null.
 *          (b) The value must be convertible into the attribute definition's 
 *              type, unless it's an empty string and cardinality != 0.
 *          (c) The following relation must hold: min <= value <= max, if either 
 *              min or max is specified.
 *          (d) If options were specified, the value must be equal to one of 
 *              them.
 *     Note this approach means validation will always be present since the type
 *     compatibility check can always be performed (i.e. the Equinox
 *     implementation will never return null indicating no validation exists).
 *(11) An invalid option value will simply be ignored and not result in the
 *     entire metadata being rejected (this is based on the previous behavior).
 *(12) Similarly, an invalid default value will simply be ignored.
 *(13) When specifying 'min' or 'max' values for type Char, escapes must not be
 *     used. For example, <AD id="1" max="&#0020;" min="&#0009;" type="Char"/>
 *     not <AD id="1" max="\&#0020;" min="\&#0009;" type="Char"/>.
 */
public class Bug332161Test extends AbstractTest {
	private AttributeDefinition[] ads;
	private Bundle bundle;
	private MetaTypeInformation mti;
	private ObjectClassDefinition ocd;

	/*
	 * Tests an enumerated Char type consisting of a mixture of whitespace and
	 * non-whitespace characters. A whitespace default value is used.
	 */
	@Test
	public void test1() {
		doTest1();
		restartMetatype();
		doTest1();
	}

	private void doTest1() {
		AttributeDefinition ad = findAttributeDefinitionById("char1", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		String defaultValue = getFirstDefaultValue(ad.getDefaultValue());
		Assert.assertNotNull("Default value not found", defaultValue); //$NON-NLS-1$
		Assert.assertEquals("Wrong default value", "\r", defaultValue); //$NON-NLS-1$  //$NON-NLS-2$
		validateChar1Options(ad.getOptionLabels(), ad.getOptionValues());
		assertValidationPass("\\ ", ad); //$NON-NLS-1$
		assertValidationPass("\\\u0020", ad); //$NON-NLS-1$
		assertValidationPass("\\	", ad); //$NON-NLS-1$
		assertValidationPass("\\\u0009", ad); //$NON-NLS-1$
		assertValidationPass("\\\n", ad); //$NON-NLS-1$
		assertValidationPass("\\\r", ad); //$NON-NLS-1$
		assertValidationPass("A", ad); //$NON-NLS-1$
		assertValidationPass("z", ad); //$NON-NLS-1$
		assertValidationFail("\\\u0008", ad); //$NON-NLS-1$
		assertValidationFail("a", ad); //$NON-NLS-1$
		assertValidationFail("Z", ad); //$NON-NLS-1$
	}

	/*
	 * Tests a String type with a default value of CRLF.
	 */
	@Test
	public void test2() {
		doTest2();
		restartMetatype();
		doTest2();
	}

	private void doTest2() {
		AttributeDefinition ad = findAttributeDefinitionById("string1", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		String defaultValue = getFirstDefaultValue(ad.getDefaultValue());
		Assert.assertNotNull("Default value not found", defaultValue); //$NON-NLS-1$
		Assert.assertEquals("Wrong default value", "\r\n", defaultValue); //$NON-NLS-1$  //$NON-NLS-2$
		assertValidationPass("\\\r\\\n", ad); //$NON-NLS-1$
	}

	/*
	 * Tests a String type with a default value consisting of multiple tokens
	 * with a mixture of whitespace (significant and not significant), escapes,
	 * and non-whitespace characters.
	 */
	@Test
	public void test3() {
		doTest3();
		restartMetatype();
		doTest3();
	}

	private void doTest3() {
		AttributeDefinition ad = findAttributeDefinitionById("string2", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		String[] defaultValue = ad.getDefaultValue();
		Assert.assertNotNull("Default value not found", defaultValue); //$NON-NLS-1$
		String[] expectedValue = new String[] {"\\ Hello, world!", //$NON-NLS-1$
				"\"Goodbye, cruel world ...\" \r\n", //$NON-NLS-1$
				"To Be,\r\nOr not to be\u0009\u0009" //$NON-NLS-1$
		};
		assertTrue("Wrong default value", Arrays.equals(defaultValue, expectedValue)); //$NON-NLS-1$
		assertValidationPass(escape(defaultValue[0]), ad);
		assertValidationPass(escape(defaultValue[1]), ad);
		assertValidationPass(escape(defaultValue[2]), ad);
		assertValidationPass(escape(expectedValue[0]), ad);
		assertValidationPass(escape(expectedValue[1]), ad);
		assertValidationPass(escape(expectedValue[2]), ad);
		String token1 = " \\\\ Hello\\, wo\\rld! "; //$NON-NLS-1$
		String token2 = "\"Goodbye\\, cruel world ...\" \\\r\\\n     "; //$NON-NLS-1$
		String token3 = " To B\\e\\,\\\r\\\nOr not\\ to be	\\	"; //$NON-NLS-1$
		String tokens = token1 + ',' + token2 + ',' + token3;
		assertValidationPass(token1, ad);
		assertValidationPass(token2, ad);
		assertValidationPass(token3, ad);
		assertValidationPass(tokens, ad);
		assertValidationPass(" \\\\ Hello\\, wo\\rld! ", ad); //$NON-NLS-1$
		assertValidationFail("  Hello\\, wo\\rld! ", ad); //$NON-NLS-1$
		assertValidationPass("\"Goodbye\\, cruel world ...\" \\\r\\\n     ", ad); //$NON-NLS-1$
		assertValidationFail("\"Goodbye, cruel world ...\" \\\r\\\n     ", ad); //$NON-NLS-1$
		assertValidationPass(" To B\\e\\,\\\r\\\nOr not\\ to be	\\	 	", ad); //$NON-NLS-1$
		assertValidationFail(" To B\\e\\,\\\r\\\n Or not\\ to be	\\	 	", ad); //$NON-NLS-1$
		assertValidationFail("i,have,cardinality,4", ad); //$NON-NLS-1$
	}

	/*
	 * Make sure these changes still return null for the default value when
	 * unspecified.
	 */
	@Test
	public void test4() {
		doTest4();
		restartMetatype();
		doTest4();
	}

	private void doTest4() {
		AttributeDefinition ad = findAttributeDefinitionById("string3", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		assertNull("Default value was not null", ad.getDefaultValue()); //$NON-NLS-1$
	}

	/*
	 * Invalid default and option values should be logged and ignored.
	 */
	@Test
	public void test5() {
		doTest5();
		restartMetatype();
		doTest5();
	}

	private void doTest5() {
		AttributeDefinition ad = findAttributeDefinitionById("char2", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		assertNull("Default value was not null", ad.getDefaultValue()); //$NON-NLS-1$
		validateChar2Options(ad.getOptionLabels(), ad.getOptionValues());
	}

	/*
	 * Null validation parameters are always invalid. Empty string validation
	 * parameters are valid for type String.
	 */
	@Test
	public void test6() {
		doTest6();
		restartMetatype();
		doTest6();
	}

	private void doTest6() {
		AttributeDefinition ad = findAttributeDefinitionById("string3", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		assertValidationFail(null, ad);
		assertValidationPass("", ad); //$NON-NLS-1$
		ad = findAttributeDefinitionById("string2", ads); //$NON-NLS-1$
		assertValidationFail(null, ad);
		assertValidationFail("", ad); //$NON-NLS-1$
	}

	/*
	 * Test whitespace characters using min and max. No escapes on min or max.
	 */
	@Test
	public void test7() {
		doTest7();
		restartMetatype();
		doTest7();
	}

	private void doTest7() {
		AttributeDefinition ad = findAttributeDefinitionById("char3", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		assertValidationPass("\\\u0009", ad); //$NON-NLS-1$
		assertValidationPass("\\\u0020", ad); //$NON-NLS-1$
		assertValidationPass("\\ ", ad); //$NON-NLS-1$
		assertValidationPass("\\\u000b", ad); //$NON-NLS-1$
		assertValidationPass("\\	", ad); //$NON-NLS-1$
		assertValidationPass("\\\r", ad); //$NON-NLS-1$
		assertValidationPass("\\\n", ad); //$NON-NLS-1$
		assertValidationFail("\\\u0008", ad); //$NON-NLS-1$
		assertValidationFail("\u0021", ad); //$NON-NLS-1$
		assertValidationFail("!", ad); //$NON-NLS-1$
	}

	/*
	 * Test that empty string is a valid default and option value when
	 * cardinality is zero.
	 */
	@Test
	public void test8() {
		doTest8();
		restartMetatype();
		doTest8();
	}

	private void doTest8() {
		AttributeDefinition ad = findAttributeDefinitionById("string4", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		String[] defaultValue = ad.getDefaultValue();
		Assert.assertNotNull("Default value was null", defaultValue); //$NON-NLS-1$
		Assert.assertEquals("Wrong number of default values", 1, defaultValue.length); //$NON-NLS-1$
		Assert.assertEquals("Wrong default value", "", defaultValue[0]); //$NON-NLS-1$ //$NON-NLS-2$
		String[] optionValues = ad.getOptionValues();
		Assert.assertNotNull("Option values was null", optionValues); //$NON-NLS-1$
		Assert.assertEquals("Wrong number of option values", 1, optionValues.length); //$NON-NLS-1$
		Assert.assertEquals("Wrong option value", "", optionValues[0]); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * Test that empty string is an invalid default and option value when type
	 * is other than String. Also test that getOptionLabels() and
	 * getOptionValues() returns null if options were specified but all were
	 * invalid and removed.
	 */
	@Test
	public void test9() {
		doTest9();
		restartMetatype();
		doTest9();
	}

	private void doTest9() {
		AttributeDefinition ad = findAttributeDefinitionById("integer1", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		String[] defaultValue = ad.getDefaultValue();
		assertNull("Default value was not null", defaultValue); //$NON-NLS-1$
		assertNull("Option labels was not null", ad.getOptionLabels()); //$NON-NLS-1$
		assertNull("Option values was not null", ad.getOptionValues()); //$NON-NLS-1$
	}

	/*
	 * Test that empty string is a valid default and option value when
	 * cardinality is other than zero.
	 */
	@Test
	public void test10() {
		doTest10();
		restartMetatype();
		doTest10();
	}

	private void doTest10() {
		AttributeDefinition ad = findAttributeDefinitionById("string5", ads); //$NON-NLS-1$
		Assert.assertNotNull("Attribute definition not found", ad); //$NON-NLS-1$
		String[] defaultValue = ad.getDefaultValue();
		Assert.assertNotNull("Default value was null", defaultValue); //$NON-NLS-1$
		Assert.assertEquals("Wrong number of default values", 2, defaultValue.length); //$NON-NLS-1$
		Assert.assertEquals("Wrong default value", "", defaultValue[0]); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertEquals("Wrong default value", "", defaultValue[1]); //$NON-NLS-1$ //$NON-NLS-2$
		String[] optionValues = ad.getOptionValues();
		Assert.assertNotNull("Option values was null", optionValues); //$NON-NLS-1$
		Assert.assertEquals("Wrong number of option values", 5, optionValues.length); //$NON-NLS-1$
		Assert.assertEquals("Wrong option value", "", optionValues[0]); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertEquals("Wrong option value", ",", optionValues[1]); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertEquals("Wrong option value", ",,", optionValues[2]); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertEquals("Wrong option value", ",,,", optionValues[3]); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertEquals("Wrong option value", ",,,,", optionValues[4]); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		bundle = bundleInstaller.installBundle("tb4"); //$NON-NLS-1$
		bundle.start();
		getMetatypeObjects();
	}

	private void getMetatypeObjects() {
		mti = metatype.getMetaTypeInformation(bundle);
		Assert.assertNotNull("Metatype information not found", mti); //$NON-NLS-1$
		ocd = mti.getObjectClassDefinition("org.eclipse.equinox.metatype.tests.tb4", null); //$NON-NLS-1$
		Assert.assertNotNull("Object class definition not found", ocd); //$NON-NLS-1$
		ads = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		Assert.assertNotNull("Attribute definitions not found", ads); //$NON-NLS-1$
		Assert.assertEquals("Wrong number of attribute definitions", 9, ads.length); //$NON-NLS-1$
	}

	@Override
	public void restartMetatype() {
		super.restartMetatype();
		getMetatypeObjects();
	}

	private void validateChar1Options(String[] optionLabels, String[] optionValues) {
		Assert.assertNotNull("Option labels not found", optionLabels); //$NON-NLS-1$
		Assert.assertEquals("Wrong number of option labels", 6, optionLabels.length); //$NON-NLS-1$
		Assert.assertNotNull("Option values not found", optionValues); //$NON-NLS-1$
		Assert.assertEquals("Wrong number of option values", 6, optionValues.length); //$NON-NLS-1$
		for (int i = 0; i < optionLabels.length; i++) {
			if ("Space".equals(optionLabels[i])) { //$NON-NLS-1$
				Assert.assertEquals("\u0020", optionValues[i]); //$NON-NLS-1$
			} else if ("Tab".equals(optionLabels[i])) { //$NON-NLS-1$
				Assert.assertEquals("\u0009", optionValues[i]); //$NON-NLS-1$
			} else if ("Line Feed".equals(optionLabels[i])) { //$NON-NLS-1$
				Assert.assertEquals("\n", optionValues[i]); //$NON-NLS-1$
			} else if ("Carriage Return".equals(optionLabels[i])) { //$NON-NLS-1$
				Assert.assertEquals("\r", optionValues[i]); //$NON-NLS-1$
			} else if ("Capital A".equals(optionLabels[i])) { //$NON-NLS-1$
				Assert.assertEquals("A", optionValues[i]); //$NON-NLS-1$
			} else if ("Lowercase Z".equals(optionLabels[i])) { //$NON-NLS-1$
				Assert.assertEquals("z", optionValues[i]); //$NON-NLS-1$
			} else {
				fail("Wrong number of option labels"); //$NON-NLS-1$
			}
		}
	}

	private void validateChar2Options(String[] optionLabels, String[] optionValues) {
		Assert.assertNotNull("Option labels not found", optionLabels); //$NON-NLS-1$
		Assert.assertEquals("Wrong number of option labels", 3, optionLabels.length); //$NON-NLS-1$
		Assert.assertNotNull("Option values not found", optionValues); //$NON-NLS-1$
		Assert.assertEquals("Wrong number of option values", 3, optionValues.length); //$NON-NLS-1$
		for (int i = 0; i < optionLabels.length; i++) {
			if ("Capital A".equals(optionLabels[i])) { //$NON-NLS-1$
				Assert.assertEquals("A", optionValues[i]); //$NON-NLS-1$
			} else if ("Capital B".equals(optionLabels[i])) { //$NON-NLS-1$
				Assert.assertEquals("B", optionValues[i]); //$NON-NLS-1$
			} else if ("Capital E".equals(optionLabels[i])) { //$NON-NLS-1$
				Assert.assertEquals("E", optionValues[i]); //$NON-NLS-1$
			} else {
				fail("Wrong number of option labels"); //$NON-NLS-1$
			}
		}
	}
}
