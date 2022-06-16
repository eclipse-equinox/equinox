package org.eclipse.osgi.tests.util;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.osgi.framework.util.CaseInsensitiveDictionaryMap;
import org.osgi.framework.Constants;

public class TestCaseinsensitiveMap extends CoreTest {
	@SuppressWarnings("deprecation")
	static String[] COMMON_KEY_NAMES = new String[] { //

			// common core service property keys
			Constants.OBJECTCLASS, //
			Constants.SERVICE_BUNDLEID, //
			Constants.SERVICE_CHANGECOUNT, //
			Constants.SERVICE_DESCRIPTION, //
			Constants.SERVICE_ID, //
			Constants.SERVICE_PID, //
			Constants.SERVICE_RANKING, //
			Constants.SERVICE_SCOPE, //
			Constants.SERVICE_VENDOR, //

			// common SCR service property keys
			"component.name", //$NON-NLS-1$
			"component.id", //$NON-NLS-1$

			// common meta-type property keys
			"metatype.pid", //$NON-NLS-1$
			"metatype.factory.pid", //$NON-NLS-1$

			// common event admin keys
			"event.topics", //$NON-NLS-1$
			"event.filter", //$NON-NLS-1$

			// jmx keys
			"jmx.objectname", //$NON-NLS-1$

			// common bundle manifest headers
			"Manifest-Version", // $NON-NLS-1$
			Constants.BUNDLE_ACTIVATIONPOLICY, //
			Constants.BUNDLE_ACTIVATOR, //
			Constants.BUNDLE_CLASSPATH, //
			Constants.BUNDLE_DESCRIPTION, //
			Constants.BUNDLE_LICENSE, //
			Constants.BUNDLE_LOCALIZATION, //
			Constants.BUNDLE_MANIFESTVERSION, //
			Constants.BUNDLE_NAME, //
			Constants.BUNDLE_NATIVECODE, //
			Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, //
			Constants.BUNDLE_SCM, //
			Constants.BUNDLE_SYMBOLICNAME, //
			Constants.BUNDLE_VENDOR, //
			Constants.BUNDLE_VERSION, //
			Constants.DYNAMICIMPORT_PACKAGE, //
			Constants.EXPORT_PACKAGE, //
			Constants.FRAGMENT_HOST, //
			Constants.IMPORT_PACKAGE, //
			Constants.REQUIRE_BUNDLE, //
			Constants.REQUIRE_CAPABILITY, //
			Constants.PROVIDE_CAPABILITY //
	};

	String[] OTHER_KEY_NAMES = new String[] {"test.key0", //
			"test.key1", //
			"test.key2", //
			"test.key3", //
			"test.key4", //
			"test.key5", //
			"test.key6", //
			"test.key7", //
			"test.key8", //
			"test.key9" //
	};

	private static final String VALUE1 = "-VALUE1";
	private static final String VALUE2 = "-VALUE2";

	public void testCommonKeys() {
		testKeys(COMMON_KEY_NAMES);
	}

	public void testOtherKeys() {
		testKeys(OTHER_KEY_NAMES);
	}

	private void testKeys(String[] keys) {
		Map<String, Object> testMap = new CaseInsensitiveDictionaryMap<>();
		// first put a value in for all common keys
		for (String key : keys) {
			testMap.put(key, key + VALUE1);
			assertEquals("Wrong value found.", key + VALUE1, testMap.get(key));
		}

		Set<String> upperKeys = new HashSet<>();
		for (String key : keys) {
			// now upper case all keys
			String upperKey = key.toUpperCase();
			upperKeys.add(upperKey);
			assertEquals("Wrong value found.", key + VALUE1, testMap.get(upperKey));
			// replace with value2 using upper case
			assertEquals("Wrong value found.", key + VALUE1, testMap.put(upperKey, key + VALUE2));
			// both original key and upper case key should give same value
			assertEquals("Wrong value found.", key + VALUE2, testMap.get(key));
			assertEquals("Wrong value found.", key + VALUE2, testMap.get(upperKey));
		}

		Set<String> currentKeys = testMap.keySet();
		assertEquals("Wrong number of keys.", upperKeys.size(), currentKeys.size());
		assertTrue("Wrong keys found: " + currentKeys, upperKeys.containsAll(currentKeys));
	}
}
