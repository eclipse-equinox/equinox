package org.eclipse.osgi.tests.hooks.framework;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.tests.OSGiTestsActivator;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;

/*
 * The framework must discard bundles that are not valid according to storage 
 * hooks. See bug 407416.
 */
public class StorageHookTests extends AbstractFrameworkHookTests {
	private static final String HOOK_CONFIGURATOR_BUNDLE = "storage.hooks.a";
	private static final String HOOK_CONFIGURATOR_CLASS = "org.eclipse.osgi.tests.hooks.framework.storage.a.TestHookConfigurator";
	private static final String HOOK_CONFIGURATOR_FIELD_INVALID = "invalid";
	private static final String HOOK_CONFIGURATOR_FIELD_VALIDATE_CALLED = "validateCalled";

	private Map<String, String> configuration;
	private Framework framework;
	private String location;

	/*
	 * Bundles must be discarded if a storage hook throws an 
	 * IllegalStateException during validation.
	 */
	public void testBundleDiscardedWhenClasspathStorageHookInvalidates() throws Exception {
		initAndStartFramework();
		installBundle();
		setStorageHookInvalid(true);
		restartFramework();
		assertStorageHookValidateCalled();
		assertBundleDiscarded();
	}

	/*
	 * Bundles must not be discarded when a storage hook says they are valid.
	 */
	public void testBundleNotDiscardedWhenClasspathStorageHookValidates() throws Exception {
		initAndStartFramework();
		installBundle();
		setStorageHookInvalid(false);
		restartFramework();
		assertStorageHookValidateCalled();
		assertBundleNotDiscarded();
	}

	protected void setUp() throws Exception {
		super.setUp();
		String loc = bundleInstaller.getBundleLocation(HOOK_CONFIGURATOR_BUNDLE);
		location = loc.substring(loc.indexOf("file:"));
		classLoader.addURL(new URL(location));
		File file = OSGiTestsActivator.getContext().getDataFile(getName());
		configuration = new HashMap<String, String>();
		configuration.put(Constants.FRAMEWORK_STORAGE, file.getAbsolutePath());
		configuration.put(HookRegistry.PROP_HOOK_CONFIGURATORS_INCLUDE, HOOK_CONFIGURATOR_CLASS);
		framework = createFramework(configuration);
		resetStorageHook();
	}

	protected void tearDown() throws Exception {
		stopQuietly(framework);
		super.tearDown();
	}

	private void assertBundleDiscarded() {
		assertBundleDiscarded(location, framework);
	}

	private void assertBundleNotDiscarded() {
		assertBundleNotDiscarded(location, framework);
	}

	private void assertStorageHookValidateCalled() throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		assertTrue("Storage hook not called by framework", clazz.getField(HOOK_CONFIGURATOR_FIELD_VALIDATE_CALLED).getBoolean(null));
	}

	private void initAndStartFramework() throws Exception {
		initAndStart(framework);
	}

	private void installBundle() throws Exception {
		framework.getBundleContext().installBundle(location);
	}

	private void resetStorageHook() throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_INVALID).set(null, false);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_VALIDATE_CALLED).set(null, false);
	}

	private void restartFramework() throws Exception {
		framework = restart(framework, configuration);
	}

	private void setStorageHookInvalid(boolean value) throws Exception {
		Class<?> clazz = classLoader.loadClass(HOOK_CONFIGURATOR_CLASS);
		clazz.getField(HOOK_CONFIGURATOR_FIELD_INVALID).set(null, value);
	}
}
