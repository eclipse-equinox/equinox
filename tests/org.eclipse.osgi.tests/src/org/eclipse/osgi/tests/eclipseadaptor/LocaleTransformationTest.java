package org.eclipse.osgi.tests.eclipseadaptor;

import static org.junit.Assert.assertEquals;

import java.util.Locale;
import org.eclipse.osgi.internal.framework.EquinoxConfiguration;
import org.junit.Test;

public class LocaleTransformationTest {

	@Test
	public void testValidLanguageCountryVariant() {
		String localeString = "de_DE_EURO";
		Locale locale = EquinoxConfiguration.toLocale(localeString, Locale.ENGLISH);
		assertEquals("de", locale.getLanguage());
		assertEquals("DE", locale.getCountry());
		assertEquals("EURO", locale.getVariant());
	}

	@Test
	public void testValidLanguageCountry() {
		String localeString = "de_DE";
		Locale locale = EquinoxConfiguration.toLocale(localeString, Locale.ENGLISH);
		assertEquals("de", locale.getLanguage());
		assertEquals("DE", locale.getCountry());
		assertEquals("", locale.getVariant());
	}

	@Test
	public void testValidLanguage() {
		String localeString = "de";
		Locale locale = EquinoxConfiguration.toLocale(localeString, Locale.ENGLISH);
		assertEquals("de", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("", locale.getVariant());
	}

	@Test
	public void testValidCountry() {
		String localeString = "_DE";
		Locale locale = EquinoxConfiguration.toLocale(localeString, Locale.ENGLISH);
		assertEquals("", locale.getLanguage());
		assertEquals("DE", locale.getCountry());
		assertEquals("", locale.getVariant());
	}

	@Test
	public void testValidLanguageVariant() {
		String localeString = "de__EURO";
		Locale locale = EquinoxConfiguration.toLocale(localeString, Locale.ENGLISH);
		assertEquals("de", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("EURO", locale.getVariant());
	}

	@Test
	public void testValidVariant() {
		String localeString = "__EURO";
		Locale locale = EquinoxConfiguration.toLocale(localeString, Locale.ENGLISH);
		assertEquals("", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("EURO", locale.getVariant());
	}

	@Test
	public void testValidCountryVariant() {
		String localeString = "_DE_EURO";
		Locale locale = EquinoxConfiguration.toLocale(localeString, Locale.ENGLISH);
		assertEquals("", locale.getLanguage());
		assertEquals("DE", locale.getCountry());
		assertEquals("EURO", locale.getVariant());
	}

	@Test
	public void testInvalidLanguage() {
		String localeString = "1234";
		Locale locale = EquinoxConfiguration.toLocale(localeString, Locale.ENGLISH);
		assertEquals("en", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("", locale.getVariant());
	}

	@Test
	public void testInvalidOneLetterLanguage() {
		String localeString = "a";
		Locale locale = EquinoxConfiguration.toLocale(localeString, Locale.ENGLISH);
		assertEquals("en", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("", locale.getVariant());
	}

	@Test
	public void testThreeLetterValidLanguage() {
		String localeString = "kok";
		Locale locale = EquinoxConfiguration.toLocale(localeString, Locale.ENGLISH);
		assertEquals("kok", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("", locale.getVariant());
	}

	@Test
	public void testInvalidOneLetterCountry() {
		String localeString = "_X";
		Locale locale = EquinoxConfiguration.toLocale(localeString, Locale.ENGLISH);
		assertEquals("en", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("", locale.getVariant());
	}

	@Test
	public void testInvalidThreeLetterCountry() {
		String localeString = "_XXX";
		Locale locale = EquinoxConfiguration.toLocale(localeString, Locale.ENGLISH);
		assertEquals("en", locale.getLanguage());
		assertEquals("", locale.getCountry());
		assertEquals("", locale.getVariant());
	}

	@Test
	public void testValidNumericAreaCode() {
		String localeString = "_029";
		Locale locale = EquinoxConfiguration.toLocale(localeString, Locale.ENGLISH);
		assertEquals("", locale.getLanguage());
		assertEquals("029", locale.getCountry());
		assertEquals("", locale.getVariant());
	}
}
