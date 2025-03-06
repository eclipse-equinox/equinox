/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others.
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
 *     Lazar Kirchev, SAP AG - derivative implementation to migrate the commands from FrameworkCommandProvider to Gogo shell commands
 *******************************************************************************/

package org.eclipse.equinox.console.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.eclipse.equinox.console.command.adapter.Activator;
import org.eclipse.osgi.report.resolution.ResolutionReport;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleRevisions;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.resource.Namespace;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.permissionadmin.PermissionAdmin;

/**
 * This class provides methods to execute commands from the command line. It
 * registers itself as a service so it can be invoked by a CommandProcessor.
 *
 * The commands provided by this class are:
 * <p>
 * ---Controlling the OSGi framework---
 * </p>
 * <ul>
 * <li>close - shutdown and exit</li>
 * <li>exit - exit immediately (System.exit)</li>
 * <li>gc - perform a garbage collection</li>
 * <li>init - uninstall all bundles</li>
 * <li>launch - start the Service Management Framework</li>
 * <li>setprop {@code <key>=<value>} - set the OSGI property</li>
 * <li>shutdown - shutdown the Service Management Framework</li>
 * </ul>
 * <p>
 * ---Controlliing Bundles---
 * </p>
 * <ul>
 * <li>install {@code <url>} {s[tart]} - install and optionally start bundle from the
 * given URL</li>
 * <li>refresh ({@code <id>|<location>}) - refresh the packages of the specified
 * bundles</li>
 * <li>start ({@code <id>|<location>}) - start the specified bundle(s)</li>
 * <li>stop ({@code <id>|<location>}) - stop the specified bundle(s)</li>
 * <li>uninstall ({@code <id>|<location>}) - uninstall the specified bundle(s)</li>
 * <li>update ({@code <id>|<location>|<*>}) - update the specified bundle(s)</li>
 * </ul>
 * <p>
 * ---Displaying Status---
 * </p>
 * <ul>
 * <li>bundle ({@code <id>|<location>}) - display details for the specified
 * bundle(s)</li>
 * <li>bundles - display details for all installed bundles</li>
 * <li>headers ({@code <id>|<location>}) - print bundle headers</li>
 * <li>packages {{@code <pkgname>|<id>|<location>}} - display imported/exported package
 * details</li>
 * <li>props - display System properties</li>
 * <li>services {filter} - display registered service details. Examples for
 * [filter]: {@code (objectClass=com.xyz.Person);
 * (&(objectClass=com.xyz.Person)(|(sn=Jensen)(cn=Babs J*)));} passing only
 * com.xyz.Person is a shortcut for (objectClass=com.xyz.Person). The filter
 * syntax specification is available at http://www.ietf.org/rfc/rfc1960.txt</li>
 * <li>ss - display installed bundles (short status)</li>
 * <li>status - display installed bundles and registered services</li>
 * <li>threads - display threads and thread groups</li>
 * </ul>
 * <p>
 * ---Extras---
 * </p>
 * <ul>
 * <li>exec {@code <command>} - execute a command in a separate process and wait</li>
 * <li>fork {@code <command>} - execute a command in a separate process</li>
 * <li>getprop {@code <name>} - Displays the system properties with the given name, or
 * all of them.</li>
 * <li>requiredBundles [{@code <bsn>}] - lists required bundles having the specified
 * symbolic name or all if no bsn is specified</li>
 * <li>classSpaces [{@code <bsn>}] - lists required bundles having the specified
 * symbolic name or all if no bsn is specified</li>
 * </ul>
 * <p>
 * ---Controlling StartLevel---
 * </p>
 * <ul>
 * <li>sl {({@code <id>|<location>})} - display the start level for the specified
 * bundle, or for the framework if no bundle specified</li>
 * <li>setfwsl {@code <start level>} - set the framework start level</li>
 * <li>setbsl {@code <start level> (<id>|<location>)} - set the start level for the
 * bundle(s)</li>
 * <li>setibsl {@code <start level>} - set the initial bundle start level</li>
 * </ul>
 * <p>
 * ---Eclipse Runtime commands---
 * </p>
 * <ul>
 * <li>diag - Displays unsatisfied constraints for the specified bundle(s)</li>
 * <li>enableBundle - Enable the specified bundle(s)</li>
 * <li>disableBundle - Disable the specified bundle(s)</li>
 * <li>disabledBundles - List disabled bundles in the system</li>
 * </ul>
 */

public class EquinoxCommandProvider implements SynchronousBundleListener {

	/** The system bundle context */
	private final BundleContext context;
	private ServiceRegistration<?> providerReg;
	private ServiceRegistration<?> converterReg;

	/** Strings used to format other strings */
	private final static String tab = "\t"; //$NON-NLS-1$
	private final static String newline = "\r\n"; //$NON-NLS-1$

	/** this list contains the bundles known to be lazily awaiting activation */
	private final List<Bundle> lazyActivation = new ArrayList<>();

	private final Activator activator;

	/** commands provided by this command provider */
	private static final String[] functions = new String[] { "exit", "shutdown", "sta", "start", "sto", "stop", "i",
			"install", "up", "up", "up", "update", "update", "update", "un", "uninstall", "s", "status", "se",
			"services", "p", "p", "packages", "packages", "bundles", "b", "bundle", "gc", "init", "close", "r",
			"refresh", "exec", "fork", "h", "headers", "pr", "props", "setp", "setprop", "ss", "t", "threads", "sl",
			"setfwsl", "setbsl", "setibsl", "requiredBundles", "classSpaces", "profilelog", "getPackages", "getprop",
			"diag", "enableBundle", "disableBundle", "disabledBundles" };

	/**
	 * Constructor.
	 *
	 * start() must be called after creating this object.
	 */
	public EquinoxCommandProvider(BundleContext context, Activator activator) {
		this.context = context;
		this.activator = activator;
	}

	/**
	 * Starts this CommandProvider.
	 *
	 * Registers this object as a service providing commands Adds this object as a
	 * SynchronousBundleListener.
	 */
	public void startService() {
		EquinoxCommandsConverter converter = new EquinoxCommandsConverter(context);
		converterReg = context.registerService(Converter.class.getName(), converter, null);

		Dictionary<String, Object> props = new Hashtable<>();
		props.put(Constants.SERVICE_RANKING, Integer.valueOf(Integer.MAX_VALUE));
		props.put(CommandProcessor.COMMAND_SCOPE, "equinox");
		props.put(CommandProcessor.COMMAND_FUNCTION, functions);
		providerReg = context.registerService(EquinoxCommandProvider.class.getName(), this, props);
		context.addBundleListener(this);
	}

	public void stopService() {
		if (converterReg != null) {
			converterReg.unregister();
		}

		context.removeBundleListener(this);

		if (providerReg != null) {
			providerReg.unregister();
		}
	}

	/**
	 * Handle the exit command. Exit immediately (System.exit)
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_EXIT_COMMAND_DESCRIPTION)
	public void exit(CommandSession session) throws Exception {
		if (confirmStop(session)) {
			System.out.println();
			System.exit(0);
		}
	}

	/**
	 * Handle the shutdown command. Shutdown the OSGi framework.
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SHUTDOWN_COMMAND_DESCRIPTION)
	public void shutdown() throws Exception {
		context.getBundle(0).stop();
	}

	/**
	 * Handle the start command's abbreviation. Invoke start()
	 *
	 * @param bundles bundle(s) to be started
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_START_COMMAND_DESCRIPTION)
	public void sta(@Descriptor(ConsoleMsg.CONSOLE_HELP_START_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles)
			throws Exception {
		start(bundles);
	}

	/**
	 * Handle the start command. Start the specified bundle(s).
	 *
	 * @param bundles bundle(s) to be started
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_START_COMMAND_DESCRIPTION)
	public void start(@Descriptor(ConsoleMsg.CONSOLE_HELP_START_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles)
			throws Exception {
		if (bundles == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}

		if (bundles.length == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}

		for (Bundle bundle : bundles) {
			bundle.start();
		}
	}

	/**
	 * Handle the stop command's abbreviation. Invoke stop()
	 *
	 * @param bundles bundle(s) to be stopped.
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_STOP_COMMAND_DESCRIPTION)
	public void sto(@Descriptor(ConsoleMsg.CONSOLE_HELP_STOP_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles)
			throws Exception {
		stop(bundles);
	}

	/**
	 * Handle the stop command. Stop the specified bundle(s).
	 *
	 * @param bundles bundle(s) to be stopped.
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_STOP_COMMAND_DESCRIPTION)
	public void stop(@Descriptor(ConsoleMsg.CONSOLE_HELP_STOP_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles)
			throws Exception {
		if (bundles == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}

		if (bundles.length == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}

		for (Bundle bundle : bundles) {
			bundle.stop();
		}
	}

	/**
	 * Handle the install command's abbreviation. Invoke install()
	 *
	 * @param shouldStart if the bundle should be start after installation
	 * @param url         location of the bundle to be installed
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_INSTALL_COMMAND_DESCRIPTION)
	public void i(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_INSTALL_START_OPTION_DESCRIPTION) @Parameter(absentValue = "false", presentValue = "true", names = {
					"-start" }) boolean shouldStart,
			@Descriptor(ConsoleMsg.CONSOLE_HELP_INSTALL_START_ARGUMENT_DESCRIPTION) String url) throws Exception {
		install(shouldStart, url);
	}

	/**
	 * Handle the install command. Install and optionally start bundle from the
	 * given URL
	 *
	 * @param shouldStart if the bundle should be start after installation
	 * @param url         location of the bundle to be installed
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_INSTALL_COMMAND_DESCRIPTION)
	public Bundle install(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_INSTALL_START_OPTION_DESCRIPTION) @Parameter(absentValue = "false", presentValue = "true", names = {
					"-start" }) boolean shouldStart,
			@Descriptor(ConsoleMsg.CONSOLE_HELP_INSTALL_START_ARGUMENT_DESCRIPTION) String url) throws Exception {
		if (url == null) {
			System.out.println(ConsoleMsg.CONSOLE_NOTHING_TO_INSTALL_ERROR);
			return null;
		} else {
			Bundle bundle = context.installBundle(url);
			System.out.print(ConsoleMsg.CONSOLE_BUNDLE_ID_MESSAGE);
			System.out.println(bundle.getBundleId());
			if (shouldStart == true) {
				bundle.start();
			}
			return bundle;
		}
	}

	/**
	 * Handle the update command's abbreviation. Invoke update()
	 *
	 * @param bundles bundle(s) to be updated
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_COMMAND_DESCRIPTION)
	public void up(@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles)
			throws Exception {
		update(bundles);
	}

	/**
	 * Handle the update command's abbreviation. Invoke update()
	 *
	 * @param bundle bundle to be updated
	 * @param source location to get the new bundle's content
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_SOURCE_COMMAND_DESCRIPTION)
	public void up(@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_SOURCE_COMMAND_BUNDLE_ARGUMENT_DESCRIPTION) Bundle bundle,
			@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_SOURCE_COMMAND_URL_ARGUMENT_DESCRIPTION) URL source)
			throws Exception {
		update(bundle, source);
	}

	/**
	 * Handle the update command. Update the specified bundle(s).
	 *
	 * @param bundles bundle(s) to be updated
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_COMMAND_DESCRIPTION)
	public void update(@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles)
			throws Exception {
		if (bundles == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}

		if (bundles.length == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}

		for (Bundle bundle : bundles) {
			bundle.update();
		}
	}

	/**
	 * Handle the update command. Update the specified bundle with the specified
	 * content.
	 *
	 * @param bundle bundle to be updated
	 * @param source location to get the new bundle's content
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_SOURCE_COMMAND_DESCRIPTION)
	public void update(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_SOURCE_COMMAND_BUNDLE_ARGUMENT_DESCRIPTION) Bundle bundle,
			@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_SOURCE_COMMAND_URL_ARGUMENT_DESCRIPTION) URL source)
			throws Exception {
		bundle.update(source.openStream());
	}

	/**
	 * Handle the uninstall command's abbreviation. Invoke uninstall()
	 *
	 * @param bundles bundle(s) to uninstall
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_UNINSTALL_COMMAND_DESCRIPTION)
	public void un(@Descriptor(ConsoleMsg.CONSOLE_HELP_UNINSTALL_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles)
			throws Exception {
		uninstall(bundles);
	}

	/**
	 * Handle the uninstall command. Uninstall the specified bundle(s).
	 *
	 * @param bundles bundle(s) to uninstall
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_UNINSTALL_COMMAND_DESCRIPTION)
	public void uninstall(@Descriptor(ConsoleMsg.CONSOLE_HELP_UNINSTALL_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles)
			throws Exception {
		if (bundles == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}

		if (bundles.length == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}

		for (Bundle bundle : bundles) {
			bundle.uninstall();
		}
	}

	private int getStatesFromConstants(String states) throws IllegalArgumentException {
		int stateFilter = -1;
		if (!states.equals("")) {
			StringTokenizer tokens = new StringTokenizer(states, ","); // $NON-NLS-1
			while (tokens.hasMoreElements()) {
				String desiredState = (String) tokens.nextElement();
				Field match = null;
				try {
					match = Bundle.class.getField(desiredState.toUpperCase());
					if (stateFilter == -1) {
						stateFilter = 0;
					}
					stateFilter |= match.getInt(match);
				} catch (NoSuchFieldException | IllegalAccessException e) {
					System.out.println(ConsoleMsg.CONSOLE_INVALID_INPUT + ": " + desiredState); //$NON-NLS-1$
					throw new IllegalArgumentException();
				}
			}
		}
		return stateFilter;
	}

	/**
	 * Handle the status command's abbreviation. Invoke status()
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_STATUS_COMMAND_DESCRIPTION)
	public void s(@Descriptor(ConsoleMsg.CONSOLE_HELP_STATUS_ARGUMENT_DESCRIPTION) String... arguments)
			throws Exception {
		status(arguments);
	}

	/**
	 * Handle the status command. Display installed bundles and registered services.
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_STATUS_COMMAND_DESCRIPTION)
	public void status(@Descriptor(ConsoleMsg.CONSOLE_HELP_STATUS_ARGUMENT_DESCRIPTION) String... arguments)
			throws Exception {
		if (context.getBundle(0).getState() == Bundle.ACTIVE) {
			System.out.println(ConsoleMsg.CONSOLE_FRAMEWORK_IS_LAUNCHED_MESSAGE);
		} else {
			System.out.println(ConsoleMsg.CONSOLE_FRAMEWORK_IS_SHUTDOWN_MESSAGE);
		}
		System.out.println();

		String states = "";
		String[] bsnSegments = null;

		if (arguments != null && arguments.length > 0) {
			if (arguments[0].equals("-s")) {
				if (arguments.length > 1) {
					states = arguments[1];
					if (arguments.length > 2) {
						bsnSegments = new String[arguments.length - 2];
						System.arraycopy(arguments, 2, bsnSegments, 0, bsnSegments.length);
					}
				}
			} else {
				bsnSegments = arguments;
			}
		}

		int stateFilter;

		try {
			stateFilter = getStatesFromConstants(states);
		} catch (IllegalArgumentException e) {
			return;
		}

		Bundle[] bundles = context.getBundles();
		int size = bundles.length;

		if (size == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_INSTALLED_BUNDLES_ERROR);
			return;
		}

		System.out.print(ConsoleMsg.CONSOLE_ID);
		System.out.print(tab);
		System.out.println(ConsoleMsg.CONSOLE_BUNDLE_LOCATION_MESSAGE);
		System.out.println(ConsoleMsg.CONSOLE_STATE_BUNDLE_FILE_NAME_HEADER);
		for (int i = 0; i < size; i++) {
			Bundle bundle = bundles[i];
			if (!match(bundle, bsnSegments, stateFilter)) {
				continue;
			}
			System.out.print(bundle.getBundleId());
			System.out.print(tab);
			System.out.println(bundle.getLocation());
			System.out.print("  "); //$NON-NLS-1$
			System.out.print(getStateName(bundle));
			System.out.println(bundle.toString());
		}

		ServiceReference<?>[] services = context.getServiceReferences((String) null, (String) null);
		if (services != null) {
			System.out.println(ConsoleMsg.CONSOLE_REGISTERED_SERVICES_MESSAGE);
			for (ServiceReference<?> service : services) {
				System.out.println(service);
			}
		}
	}

	/**
	 * Handle the services command's abbreviation. Invoke services()
	 *
	 * @param filters filters for services
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SERVICES_COMMAND_DESCRIPTION)
	public void se(@Descriptor(ConsoleMsg.CONSOLE_HELP_FILTER_ARGUMENT_DESCRIPTION) String... filters)
			throws Exception {
		services(filters);
	}

	/**
	 * Handle the services command. Display registered service details.
	 *
	 * @param filters filters for services
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SERVICES_COMMAND_DESCRIPTION)
	public void services(@Descriptor(ConsoleMsg.CONSOLE_HELP_FILTER_ARGUMENT_DESCRIPTION) String... filters)
			throws Exception {
		String filter = null;
		if (filters != null && filters.length > 0) {
			StringBuilder buf = new StringBuilder();
			for (String singleFilter : filters) {
				buf.append(' ');
				buf.append(singleFilter);
			}
			filter = buf.toString();
		}

		InvalidSyntaxException originalException = null;
		ServiceReference<?>[] services = null;

		try {
			services = context.getServiceReferences((String) null, filter);
		} catch (InvalidSyntaxException e) {
			originalException = e;
		}

		if (filter != null) {
			filter = filter.trim();
		}
		// If the filter is invalid and does not start with a bracket, probably the
		// argument was the name of an interface.
		// Try to construct an object class filter with this argument, and if still
		// invalid - throw the original InvalidSyntaxException
		if (originalException != null && !filter.startsWith("(") && !filter.contains(" ")) { //$NON-NLS-1$ //$NON-NLS-2$
			try {
				filter = "(" + Constants.OBJECTCLASS + "=" + filter + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				services = context.getServiceReferences((String) null, filter);
			} catch (InvalidSyntaxException e) {
				throw originalException;
			}
		} else if (originalException != null) {
			throw originalException;
		}

		if (services != null) {
			int size = services.length;
			if (size > 0) {
				for (int j = 0; j < size; j++) {
					ServiceReference<?> service = services[j];
					System.out.println(service);
					System.out.print("  "); //$NON-NLS-1$
					System.out.print(ConsoleMsg.CONSOLE_REGISTERED_BY_BUNDLE_MESSAGE);
					System.out.print(" "); //$NON-NLS-1$
					System.out.println(service.getBundle());
					Bundle[] users = service.getUsingBundles();
					if (users != null) {
						System.out.print("  "); //$NON-NLS-1$
						System.out.println(ConsoleMsg.CONSOLE_BUNDLES_USING_SERVICE_MESSAGE);
						for (Bundle user : users) {
							System.out.print("    "); //$NON-NLS-1$
							System.out.println(user);
						}
					} else {
						System.out.print("  "); //$NON-NLS-1$
						System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLES_USING_SERVICE_MESSAGE);
					}
				}
				return;
			}
		}
		System.out.println(ConsoleMsg.CONSOLE_NO_REGISTERED_SERVICES_MESSAGE);
	}

	/**
	 * Handle the packages command's abbreviation. Invoke packages()
	 *
	 * @param bundle bundle for which to display package details
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_COMMAND_DESCRIPTION)
	public void p(@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_BUNDLE_ARGUMENT_DESCRIPTION) Bundle... bundle)
			throws Exception {
		packages(bundle);
	}

	/**
	 * Handle the packages command's abbreviation. Invoke packages()
	 *
	 * @param packageName package for which to display details
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_COMMAND_DESCRIPTION)
	public void p(@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_PACKAGE_ARGUMENT_DESCRIPTION) String packageName)
			throws Exception {
		packages(packageName);
	}

	/**
	 * Handle the packages command. Display imported/exported packages details.
	 *
	 * @param bundle bundle for which to display package details
	 */
	@SuppressWarnings("deprecation")
	@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_COMMAND_DESCRIPTION)
	public void packages(@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_BUNDLE_ARGUMENT_DESCRIPTION) Bundle... bundle)
			throws Exception {
		if (activator.getPackageAdmin() != null) {
			ExportedPackage[] exportedPackages;
			if (bundle != null && bundle.length > 0) {
				exportedPackages = activator.getPackageAdmin().getExportedPackages(bundle[0]);
			} else {
				exportedPackages = activator.getPackageAdmin().getExportedPackages((Bundle) null);
			}
			getPackages(exportedPackages);
		} else {
			System.out.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_NO_PACKAGE_ADMIN_MESSAGE);
		}
	}

	/**
	 * Handle the packages command. Display imported/exported packages details.
	 *
	 * @param packageName package for which to display details
	 **/
	@SuppressWarnings("deprecation")
	@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_COMMAND_DESCRIPTION)
	public void packages(@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_PACKAGE_ARGUMENT_DESCRIPTION) String packageName)
			throws Exception {
		if (activator.getPackageAdmin() != null) {
			ExportedPackage[] exportedPackages = activator.getPackageAdmin().getExportedPackages(packageName);
			getPackages(exportedPackages);
		} else {
			System.out.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_NO_PACKAGE_ADMIN_MESSAGE);
		}
	}

	@SuppressWarnings("deprecation")
	private void getPackages(ExportedPackage[] packages) throws Exception {
		if (packages == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_MESSAGE);
			return;
		}
		for (ExportedPackage pkg : packages) {
			System.out.print(pkg);
			boolean removalPending = pkg.isRemovalPending();
			if (removalPending) {
				System.out.print("("); //$NON-NLS-1$
				System.out.print(ConsoleMsg.CONSOLE_REMOVAL_PENDING_MESSAGE);
				System.out.println(")"); //$NON-NLS-1$
			}
			org.osgi.framework.Bundle exporter = pkg.getExportingBundle();
			if (exporter != null) {
				System.out.print("<"); //$NON-NLS-1$
				System.out.print(exporter);
				System.out.println(">"); //$NON-NLS-1$
				org.osgi.framework.Bundle[] importers = pkg.getImportingBundles();
				for (Bundle importer : importers) {
					System.out.print("  "); //$NON-NLS-1$
					System.out.print(importer);
					System.out.print(" "); //$NON-NLS-1$
					System.out.println(ConsoleMsg.CONSOLE_IMPORTS_MESSAGE);
				}
			} else {
				System.out.print("<"); //$NON-NLS-1$
				System.out.print(ConsoleMsg.CONSOLE_STALE_MESSAGE);
				System.out.println(">"); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Handle the bundles command. Display details for all installed bundles.
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_BUNDLES_COMMAND_DESCRIPTION)
	public void bundles(@Descriptor(ConsoleMsg.CONSOLE_HELP_STATUS_ARGUMENT_DESCRIPTION) String... arguments)
			throws Exception {
		String states = "";
		String[] bsnSegments = null;

		if (arguments != null && arguments.length > 0) {
			if (arguments[0].equals("-s")) {
				if (arguments.length > 1) {
					states = arguments[1];
					if (arguments.length > 2) {
						bsnSegments = new String[arguments.length - 2];
						System.arraycopy(arguments, 2, bsnSegments, 0, bsnSegments.length);
					}
				}
			} else {
				bsnSegments = arguments;
			}
		}
		int stateFilter;
		try {
			stateFilter = getStatesFromConstants(states);
		} catch (IllegalArgumentException e) {
			return;
		}

		Bundle[] bundles = context.getBundles();
		int size = bundles.length;

		if (size == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_INSTALLED_BUNDLES_ERROR);
			return;
		}

		for (int i = 0; i < size; i++) {
			Bundle bundle = bundles[i];
			if (!match(bundle, bsnSegments, stateFilter)) {
				continue;
			}
			long id = bundle.getBundleId();
			System.out.println(bundle);
			System.out.print("  "); //$NON-NLS-1$
			System.out.print(NLS.bind(ConsoleMsg.CONSOLE_ID_MESSAGE, String.valueOf(id)));
			System.out.print(", "); //$NON-NLS-1$
			System.out.print(NLS.bind(ConsoleMsg.CONSOLE_STATUS_MESSAGE, getStateName(bundle)));
			if (id != 0) {
				File dataRoot = bundle.getDataFile(""); //$NON-NLS-1$
				String root = (dataRoot == null) ? null : dataRoot.getAbsolutePath();
				System.out.print(NLS.bind(ConsoleMsg.CONSOLE_DATA_ROOT_MESSAGE, root));
			} else {
				System.out.println();
			}

			ServiceReference<?>[] services = bundle.getRegisteredServices();
			if (services != null) {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_REGISTERED_SERVICES_MESSAGE);
				for (ServiceReference<?> service : services) {
					System.out.print("    "); //$NON-NLS-1$
					System.out.println(service);
				}
			} else {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_NO_REGISTERED_SERVICES_MESSAGE);
			}

			services = bundle.getServicesInUse();
			if (services != null) {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_SERVICES_IN_USE_MESSAGE);
				for (ServiceReference<?> service : services) {
					System.out.print("    "); //$NON-NLS-1$
					System.out.println(service);
				}
			} else {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_NO_SERVICES_IN_USE_MESSAGE);
			}
		}
	}

	/**
	 * Handle the bundle command's abbreviation. Invoke bundle()
	 *
	 * @param bundles bundle(s) to display details for
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_BUNDLE_COMMAND_DESCRIPTION)
	public void b(@Descriptor(ConsoleMsg.CONSOLE_HELP_IDLOCATION_ARGUMENT_DESCRIPTION) Bundle[] bundles)
			throws Exception {
		bundle(bundles);
	}

	/**
	 * Handle the bundle command. Display details for the specified bundle(s).
	 *
	 * @param bundles bundle(s) to display details for
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_BUNDLE_COMMAND_DESCRIPTION)
	public void bundle(@Descriptor(ConsoleMsg.CONSOLE_HELP_IDLOCATION_ARGUMENT_DESCRIPTION) Bundle[] bundles)
			throws Exception {
		if (bundles.length == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}

		for (Bundle bundle : bundles) {
			long id = bundle.getBundleId();
			System.out.println(bundle);
			System.out.print("  "); //$NON-NLS-1$
			System.out.print(NLS.bind(ConsoleMsg.CONSOLE_ID_MESSAGE, String.valueOf(id)));
			System.out.print(", "); //$NON-NLS-1$
			System.out.print(NLS.bind(ConsoleMsg.CONSOLE_STATUS_MESSAGE, getStateName(bundle)));
			if (id != 0) {
				File dataRoot = bundle.getDataFile(""); //$NON-NLS-1$
				String root = (dataRoot == null) ? null : dataRoot.getAbsolutePath();
				System.out.print(NLS.bind(ConsoleMsg.CONSOLE_DATA_ROOT_MESSAGE, root));
				System.out.println();
			} else {
				System.out.println();
			}

			ServiceReference<?>[] services = bundle.getRegisteredServices();
			if (services != null) {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_REGISTERED_SERVICES_MESSAGE);
				for (ServiceReference<?> service : services) {
					System.out.print("    "); //$NON-NLS-1$
					System.out.println(service);
				}
			} else {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_NO_REGISTERED_SERVICES_MESSAGE);
			}

			services = bundle.getServicesInUse();
			if (services != null) {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_SERVICES_IN_USE_MESSAGE);
				for (ServiceReference<?> service : services) {
					System.out.print("    "); //$NON-NLS-1$
					System.out.println(service);
				}
			} else {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_NO_SERVICES_IN_USE_MESSAGE);
			}

			BundleRevision revision = bundle.adapt(BundleRevision.class);
			if (revision == null) {
				continue;
			}

			BundleWiring wiring = revision.getWiring();
			if (wiring == null) {
				continue;
			}
			boolean title = true;
			List<BundleCapability> exports = wiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
			if (exports.isEmpty()) {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_MESSAGE);
			} else {
				title = true;

				for (BundleCapability export : exports) {
					if (title) {
						System.out.print("  "); //$NON-NLS-1$
						System.out.println(ConsoleMsg.CONSOLE_EXPORTED_PACKAGES_MESSAGE);
						title = false;
					}
					Map<String, Object> exportAttrs = export.getAttributes();
					System.out.print("    "); //$NON-NLS-1$
					System.out.print(exportAttrs.get(PackageNamespace.PACKAGE_NAMESPACE));
					System.out.print("; version=\""); //$NON-NLS-1$
					System.out.print(exportAttrs.get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE));
					System.out.print("\""); //$NON-NLS-1$
					if (!wiring.isCurrent()) {
						System.out.println(ConsoleMsg.CONSOLE_EXPORTED_REMOVAL_PENDING_MESSAGE);
					} else {
						System.out.println(ConsoleMsg.CONSOLE_EXPORTED_MESSAGE);
					}
				}

				if (title) {
					System.out.print("  "); //$NON-NLS-1$
					System.out.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_MESSAGE);
				}
			}
			title = true;

			// Get all resolved imports
			Map<String, Set<PackageSource>> packages = getPackagesInternal(wiring);
			List<BundleRequirement> unresolvedImports = getUnresolvedImports(packages, wiring);

			title = printImportedPackages(packages, title);
			title = printUnwiredDynamicImports(unresolvedImports, title);

			if (title) {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_NO_IMPORTED_PACKAGES_MESSAGE);
			}

			System.out.print("  "); //$NON-NLS-1$
			if ((revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
				List<BundleWire> hostWires = wiring.getRequiredWires(HostNamespace.HOST_NAMESPACE);
				if (hostWires.isEmpty()) {
					System.out.println(ConsoleMsg.CONSOLE_NO_HOST_MESSAGE);
				} else {
					System.out.println(ConsoleMsg.CONSOLE_HOST_MESSAGE);
					for (BundleWire hostWire : hostWires) {
						System.out.print("    "); //$NON-NLS-1$
						System.out.println(hostWire.getProvider().getBundle());
					}
				}
			} else {
				List<BundleWire> fragmentWires = wiring.getProvidedWires(HostNamespace.HOST_NAMESPACE);
				if (fragmentWires.isEmpty()) {
					System.out.println(ConsoleMsg.CONSOLE_NO_FRAGMENT_MESSAGE);
				} else {
					System.out.println(ConsoleMsg.CONSOLE_FRAGMENT_MESSAGE);
					for (BundleWire fragmentWire : fragmentWires) {
						System.out.print("    "); //$NON-NLS-1$
						System.out.println(fragmentWire.getRequirer().getBundle());
					}
				}

				List<BundleWire> requiredBundles = wiring.getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE);
				title = true;
				for (BundleWire requiredBundle : requiredBundles) {
					if (title) {
						System.out.print("  "); //$NON-NLS-1$
						System.out.println(ConsoleMsg.CONSOLE_REQUIRED_BUNDLES_MESSAGE);
						title = false;
					}
					System.out.print("    "); //$NON-NLS-1$
					System.out.println(requiredBundle.getProvider());
				}
				if (title) {
					System.out.print("  "); //$NON-NLS-1$
					System.out.println(ConsoleMsg.CONSOLE_NO_REQUIRED_BUNDLES_MESSAGE);
				}

			}

			System.out.println();
			System.out.println();
		}
	}

	private List<BundleRequirement> getUnresolvedImports(Map<String, Set<PackageSource>> packages,
			BundleWiring wiring) {

		// TODO need to get this information
		return Collections.emptyList();
	}

	private boolean printImportedPackages(Map<String, Set<PackageSource>> packages, boolean title) {
		for (Set<PackageSource> packageList : packages.values()) {
			for (PackageSource packageSource : packageList) {
				if (title) {
					System.out.print("  "); //$NON-NLS-1$
					System.out.println(ConsoleMsg.CONSOLE_IMPORTED_PACKAGES_MESSAGE);
					title = false;
				}
				printCapability("    ", packageSource.getCapability(), packageSource.getWire(),
						PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			}
		}
		return title;
	}

	private void printCapability(String prepend, BundleCapability capability, BundleWire wire, String versionKey) {
		Map<String, Object> exportAttrs = capability.getAttributes();
		System.out.print(prepend);
		System.out.print(exportAttrs.get(capability.getNamespace()));
		if (versionKey != null) {
			System.out.print("; " + versionKey + "=\""); //$NON-NLS-1$
			System.out.print(exportAttrs.get(versionKey));
			System.out.print("\""); //$NON-NLS-1$
		}

		Bundle exporter = wire == null ? capability.getRevision().getBundle() : wire.getProvider().getBundle();
		if (exporter != null) {
			System.out.print(" <"); //$NON-NLS-1$
			System.out.print(exporter);
			System.out.println(">"); //$NON-NLS-1$
		} else {
			System.out.print(" <"); //$NON-NLS-1$
			System.out.print(ConsoleMsg.CONSOLE_STALE_MESSAGE);
			System.out.println(">"); //$NON-NLS-1$
		}
	}

	private boolean printUnwiredDynamicImports(List<BundleRequirement> dynamicImports, boolean title) {
		for (BundleRequirement importReq : dynamicImports) {
			if (title) {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_IMPORTED_PACKAGES_MESSAGE);
				title = false;
			}
			System.out.print("    "); //$NON-NLS-1$
			System.out.print(importReq);
			System.out.print(";<"); //$NON-NLS-1$
			System.out.print("unwired"); //$NON-NLS-1$
			System.out.print(">"); //$NON-NLS-1$
			System.out.print("<"); //$NON-NLS-1$
			System.out.print(importReq.getDirectives().get(Namespace.REQUIREMENT_RESOLUTION_DIRECTIVE));
			System.out.println(">"); //$NON-NLS-1$
		}
		return title;
	}

	/**
	 * Handle the gc command. Perform a garbage collection.
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_GC_COMMAND_DESCRIPTION)
	public void gc() throws Exception {
		long before = Runtime.getRuntime().freeMemory();

		/* Let the finilizer finish its work and remove objects from its queue */
		System.gc(); /* asyncronous garbage collector might already run */
		System.gc(); /* to make sure it does a full gc call it twice */
		System.runFinalization();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// do nothing
		}

		long after = Runtime.getRuntime().freeMemory();
		System.out.print(ConsoleMsg.CONSOLE_TOTAL_MEMORY_MESSAGE);
		System.out.println(String.valueOf(Runtime.getRuntime().totalMemory()));
		System.out.print(ConsoleMsg.CONSOLE_FREE_MEMORY_BEFORE_GARBAGE_COLLECTION_MESSAGE);
		System.out.println(String.valueOf(before));
		System.out.print(ConsoleMsg.CONSOLE_FREE_MEMORY_AFTER_GARBAGE_COLLECTION_MESSAGE);
		System.out.println(String.valueOf(after));
		System.out.print(ConsoleMsg.CONSOLE_MEMORY_GAINED_WITH_GARBAGE_COLLECTION_MESSAGE);
		System.out.println(String.valueOf(after - before));
	}

	/**
	 * Handle the init command. Uninstall all bundles.
	 */
	@SuppressWarnings("deprecation")
	@Descriptor(ConsoleMsg.CONSOLE_HELP_INIT_COMMAND_DESCRIPTION)
	public void init() throws Exception {
		if (context.getBundle(0).getState() == Bundle.ACTIVE) {
			System.out.print(newline);
			System.out.println(ConsoleMsg.CONSOLE_FRAMEWORK_LAUNCHED_PLEASE_SHUTDOWN_MESSAGE);
			return;
		}

		Bundle[] bundles = context.getBundles();

		int size = bundles.length;

		if (size > 0) {
			for (int i = 0; i < size; i++) {
				Bundle bundle = bundles[i];

				if (bundle.getBundleId() != 0) {
					try {
						bundle.uninstall();
					} catch (BundleException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			System.out.println(ConsoleMsg.CONSOLE_NO_INSTALLED_BUNDLES_ERROR);
		}
		PermissionAdmin securityAdmin = activator.getPermissionAdmin();
		ConditionalPermissionAdmin condPermAdmin = activator.getConditionalPermissionAdmin();
		if (securityAdmin != null) {
			// clear the permissions from permission admin
			securityAdmin.setDefaultPermissions(null);
			String[] permLocations = securityAdmin.getLocations();
			if (permLocations != null) {
				for (String permLocation : permLocations) {
					securityAdmin.setPermissions(permLocation, null);
				}
			}
			ConditionalPermissionUpdate update = condPermAdmin.newConditionalPermissionUpdate();
			update.getConditionalPermissionInfos().clear();
			update.commit();
		}
		// clear the permissions from conditional permission admin
		if (securityAdmin != null) {
			for (Enumeration<ConditionalPermissionInfo> infos = condPermAdmin.getConditionalPermissionInfos(); infos
					.hasMoreElements();) {
				infos.nextElement().delete();
			}
		}
	}

	/**
	 * Handle the close command. Shutdown and exit.
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_CLOSE_COMMAND_DESCRIPTION)
	public void close(CommandSession session) throws Exception {
		if (confirmStop(session)) {
			context.getBundle(0).stop();
		}
	}

	/**
	 * Handle the refresh command's abbreviation. Invoke refresh()
	 *
	 * @param bundles bundle(s) to be refreshed
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_REFRESH_COMMAND_DESCRIPTION)
	public void r(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_REFRESH_ALL_OPTION_DESCRIPTION) @Parameter(absentValue = "false", presentValue = "true", names = {
					"-all" }) boolean shouldRefreshAll,
			@Descriptor(ConsoleMsg.CONSOLE_HELP_REFRESH_COMMAND_ARGUMENT_DESCRIPTION) Bundle... bundles)
			throws Exception {
		refresh(shouldRefreshAll, bundles);
	}

	/**
	 * Handle the refresh command. Refresh the packages of the specified bundles.
	 *
	 * @param bundles bundle(s) to be refreshed
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_REFRESH_COMMAND_DESCRIPTION)
	public void refresh(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_REFRESH_ALL_OPTION_DESCRIPTION) @Parameter(absentValue = "false", presentValue = "true", names = {
					"-all" }) boolean shouldRefreshAll,
			@Descriptor(ConsoleMsg.CONSOLE_HELP_REFRESH_COMMAND_ARGUMENT_DESCRIPTION) Bundle... bundles)
			throws Exception {
		FrameworkWiring frameworkWiring = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION)
				.adapt(FrameworkWiring.class);
		if (bundles != null && bundles.length > 0) {
			frameworkWiring.refreshBundles(Arrays.asList(bundles));
		} else if (shouldRefreshAll == true) {
			frameworkWiring.refreshBundles(Arrays.asList(context.getBundles()));
		} else {
			frameworkWiring.refreshBundles(null);
		}
	}

	/**
	 * Executes the given system command in a separate system process and waits for
	 * it to finish.
	 *
	 * @param command command to be executed
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_EXEC_COMMAND_DESCRIPTION)
	public void exec(@Descriptor(ConsoleMsg.CONSOLE_HELP_EXEC_COMMAND_ARGUMENT_DESCRIPTION) String command)
			throws Exception {
		if (command == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_COMMAND_SPECIFIED_ERROR);
			return;
		}

		Process p = Runtime.getRuntime().exec(command);

		System.out.println(NLS.bind(ConsoleMsg.CONSOLE_STARTED_IN_MESSAGE, command, String.valueOf(p)));
		int result = p.waitFor();
		System.out.println(NLS.bind(ConsoleMsg.CONSOLE_EXECUTED_RESULT_CODE_MESSAGE, command, String.valueOf(result)));
	}

	/**
	 * Executes the given system command in a separate system process. It does not
	 * wait for a result.
	 *
	 * @param command command to be executed
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_FORK_COMMAND_DESCRIPTION)
	public void fork(@Descriptor(ConsoleMsg.CONSOLE_HELP_FORK_COMMAND_ARGUMENT_DESCRIPTION) String command)
			throws Exception {
		if (command == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_COMMAND_SPECIFIED_ERROR);
			return;
		}

		Process p = Runtime.getRuntime().exec(command);
		System.out.println(NLS.bind(ConsoleMsg.CONSOLE_STARTED_IN_MESSAGE, command, String.valueOf(p)));
	}

	/**
	 * Handle the headers command's abbreviation. Invoke headers()
	 *
	 * @param bundles bundle(s) whose headers to display
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_HEADERS_COMMAND_DESCRIPTION)
	public List<Dictionary<String, String>> h(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_HEADERS_COMMAND_ARGUMENT_DESCRIPTION) Bundle... bundles)
			throws Exception {
		return headers(bundles);
	}

	/**
	 * Handle the headers command. Display headers for the specified bundle(s).
	 *
	 * @param bundles bundle(s) whose headers to display
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_HEADERS_COMMAND_DESCRIPTION)
	public List<Dictionary<String, String>> headers(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_HEADERS_COMMAND_ARGUMENT_DESCRIPTION) Bundle... bundles)
			throws Exception {
		ArrayList<Dictionary<String, String>> headers = new ArrayList<>();

		if (bundles == null || bundles.length == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return headers;
		}

		for (Bundle bundle : bundles) {
			headers.add(bundle.getHeaders());
		}
		return headers;
	}

	/**
	 * Handles the props command's abbreviation. Invokes props()
	 */
	@Descriptor(ConsoleMsg.CONSOLE_PROPS_COMMAND_DESCRIPTION)
	public Dictionary<?, ?> pr() throws Exception {
		return props();
	}

	/**
	 * Handles the _props command. Prints the system properties sorted.
	 */
	@Descriptor(ConsoleMsg.CONSOLE_PROPS_COMMAND_DESCRIPTION)
	public Dictionary<?, ?> props() throws Exception {
		System.out.println(ConsoleMsg.CONSOLE_SYSTEM_PROPERTIES_TITLE);
		return System.getProperties();
	}

	/**
	 * Handles the setprop command's abbreviation. Invokes setprop()
	 *
	 * @param arguments key=value pairs for the new properties
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SETPROP_COMMAND_DESCRIPTION)
	public void setp(@Descriptor(ConsoleMsg.CONSOLE_HELP_SETPROP_COMMAND_ARGUMENTS_DESCRIPTION) String[] arguments)
			throws Exception {
		setprop(arguments);
	}

	/**
	 * Handles the setprop command. Sets the CDS property in the given argument.
	 *
	 * @param arguments key=value pairs for the new properties
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SETPROP_COMMAND_DESCRIPTION)
	public void setprop(@Descriptor(ConsoleMsg.CONSOLE_HELP_SETPROP_COMMAND_ARGUMENTS_DESCRIPTION) String[] arguments)
			throws Exception {
		if (arguments == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_PARAMETERS_SPECIFIED_TITLE);
			props();
		} else {
			ServiceReference<EnvironmentInfo> envInfoRef = context.getServiceReference(EnvironmentInfo.class);
			if (envInfoRef != null) {
				// EnvironmentInfo is used because FrameworkProperties cannot be directly
				// accessed outside of the system bundle
				EnvironmentInfo envInfo = context.getService(envInfoRef);
				if (envInfo != null) {
					System.out.println(ConsoleMsg.CONSOLE_SETTING_PROPERTIES_TITLE);
					for (String argument : arguments) {
						int index = argument.indexOf('=');
						if (index > -1) {
							String key = argument.substring(0, index);
							String value = argument.substring(index + 1, argument.length());
							envInfo.setProperty(key, value);
							System.out.println(tab + key + " = " + value); //$NON-NLS-1$
						}
					}
				} else {
					System.out.print("  "); //$NON-NLS-1$
					System.out.println(ConsoleMsg.CONSOLE_CANNOT_ACCESS_SYSTEM_PROPERTIES);
				}
			}
		}
	}

	/**
	 * Prints the short version of the status. For the long version use "status".
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SS_COMMAND_DESCRIPTION)
	public void ss(@Descriptor(ConsoleMsg.CONSOLE_HELP_STATUS_ARGUMENT_DESCRIPTION) String... arguments)
			throws Exception {
		if (context.getBundle(0).getState() == Bundle.ACTIVE) {
			System.out.println(ConsoleMsg.CONSOLE_FRAMEWORK_IS_LAUNCHED_MESSAGE);
		} else {
			System.out.println(ConsoleMsg.CONSOLE_FRAMEWORK_IS_SHUTDOWN_MESSAGE);
		}
		System.out.println();

		String states = "";
		String[] bsnSegments = null;

		if (arguments != null && arguments.length > 0) {
			if (arguments[0].equals("-s")) {
				if (arguments.length > 1) {
					states = arguments[1];
					if (arguments.length > 2) {
						bsnSegments = new String[arguments.length - 2];
						System.arraycopy(arguments, 2, bsnSegments, 0, bsnSegments.length);
					}
				}
			} else {
				bsnSegments = arguments;
			}
		}

		int stateFilter;

		try {
			stateFilter = getStatesFromConstants(states);
		} catch (IllegalArgumentException e) {
			return;
		}

		Bundle[] bundles = context.getBundles();
		int size = bundles.length;

		if (size == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_INSTALLED_BUNDLES_ERROR);
			return;
		} else {
			System.out.print(newline);
			System.out.print(ConsoleMsg.CONSOLE_ID);
			System.out.print(tab);
			System.out.println(ConsoleMsg.CONSOLE_STATE_BUNDLE_TITLE);
			for (Bundle b : bundles) {

				if (!match(b, bsnSegments, stateFilter)) {
					continue;
				}
				String label = b.getSymbolicName();
				if (label == null || label.length() == 0) {
					label = b.toString();
				} else {
					label = label + "_" + b.getVersion(); //$NON-NLS-1$
				}
				System.out.println(b.getBundleId() + "\t" + getStateName(b) + label); //$NON-NLS-1$
				BundleRevision revision = b.adapt(BundleRevision.class);
				BundleWiring wiring = b.adapt(BundleWiring.class);
				if (revision != null && wiring != null) {
					if ((revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
						for (BundleWire hostWire : wiring.getRequiredWires(HostNamespace.HOST_NAMESPACE)) {
							System.out.println(
									"\t            Master=" + hostWire.getProvider().getBundle().getBundleId()); //$NON-NLS-1$
						}
					} else {
						List<BundleWire> fragWires = wiring.getProvidedWires(HostNamespace.HOST_NAMESPACE);
						if (!fragWires.isEmpty()) {
							System.out.print("\t            Fragments="); //$NON-NLS-1$
							Iterator<BundleWire> itr = fragWires.iterator();
							System.out.print(itr.next().getRequirer().getBundle().getBundleId());
							itr.forEachRemaining(
									w -> System.out.print(", " + w.getRequirer().getBundle().getBundleId()));
							System.out.println();
						}
					}
				}
			}
		}
	}

	private boolean match(Bundle toFilter, String[] searchedName, int searchedState) {
		if ((toFilter.getState() & searchedState) == 0) {
			return false;
		}
		if (searchedName != null && searchedName.length > 0 && toFilter.getSymbolicName() != null
				&& !toFilter.getSymbolicName().contains(searchedName[0])) {
			return false;
		}
		return true;
	}

	/**
	 * Handles the threads command abbreviation. Invokes threads().
	 */
	@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_DESCRIPTION)
	public void t() throws Exception {
		t(null, null);
	}

	@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_ACTION_DESCRIPTION)
	public void t(@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_ARG_ACTION_DESCRIPTION) String action,
			@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_ARG_THREAD_DESCRIPTION) String thread) throws Exception {
		t(action, thread, null);
	}

	@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_ACTION_THROWABLE_DESCRIPTION)
	public void t(@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_ARG_ACTION_DESCRIPTION) String action,
			@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_ARG_THREAD_DESCRIPTION) String thread,
			@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_ARG_THROWABLE_DESCRIPTION) Class<? extends Throwable> throwable)
			throws Exception {
		threads(action, thread, throwable);
	}

	@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_DESCRIPTION)
	public void threads() throws Exception {
		threads(null, null);
	}

	@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_ACTION_DESCRIPTION)
	public void threads(@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_ARG_ACTION_DESCRIPTION) String action,
			@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_ARG_THREAD_DESCRIPTION) String thread) throws Exception {
		threads(action, thread, null);
	}

	/**
	 * Prints the information about the currently running threads in the embedded
	 * system.
	 */
	@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_ACTION_THROWABLE_DESCRIPTION)
	public void threads(@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_ARG_ACTION_DESCRIPTION) String action,
			@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_ARG_THREAD_DESCRIPTION) String thread,
			@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_ARG_THROWABLE_DESCRIPTION) Class<? extends Throwable> throwable)
			throws Exception {

		ThreadGroup[] threadGroups = getThreadGroups();
		Util.sortByString(threadGroups);

		ThreadGroup tg = getTopThreadGroup();
		Thread[] threads = new Thread[tg.activeCount()];
		int count = tg.enumerate(threads, true);

		// Was an action specified?
		if (action != null) {
			// An action was specified. Process it here and return without
			// listing all of the threads.
			if ("stop".equals(action)) {
				// A thread needs to be stopped.
				// Locate the thread with the specified name.
				Thread t = null;
				for (Thread namedthread : threads) {
					if (namedthread.getName().equals(thread)) {
						// Found the thread. Stop the loop.
						t = namedthread;
						break;
					}
				}
				if (t == null) {
					// Did not find a thread with the specified name.
					System.out.println(NLS.bind(ConsoleMsg.THREADS_THREAD_DOES_NOT_EXIST, thread));
					return;
				}
				String message = NLS.bind(ConsoleMsg.THREADS_THREAD_STOPPED_BY_CONSOLE, t.getName());
				// Instantiate the specified exception, if any.
				Throwable toThrow;
				// Was an exception specified?
				if (throwable == null) {
					// If not, use the default.
					toThrow = new IllegalStateException(message);
				} else {
					// Instantiate the throwable with the message, if possible.
					// Otherwise use the default constructor.
					try {
						toThrow = throwable.getConstructor(String.class).newInstance(message);
					} catch (Exception e) {
						toThrow = throwable.getDeclaredConstructor().newInstance();
					}
				}
				// Initialize the cause. Its stack trace will be that of the current thread.
				toThrow.initCause(new RuntimeException(message));
				// Set the stack trace to that of the target thread.
				toThrow.setStackTrace(t.getStackTrace());
				// Stop the thread using the specified throwable.
				// Thread#stop(Throwable) doesn't work any more in JDK 8 and is removed in Java
				// 11. Try stop0:
				Method stop0 = Thread.class.getDeclaredMethod("stop0", Object.class);
				stop0.setAccessible(true);
				stop0.invoke(t, toThrow);
				return;
			}
			// An unrecognized action was specified.
			System.out.println(ConsoleMsg.THREADS_UNRECOGNIZED_ACTION);
			return;
		}

		// No need to sort if an action was specified.
		Util.sortByString(threads);

		StringBuilder sb = new StringBuilder(120);
		System.out.println();
		System.out.println(ConsoleMsg.CONSOLE_THREADGROUP_TITLE);
		for (ThreadGroup threadGroup : threadGroups) {
			tg = threadGroup;
			int all = tg.activeCount(); // tg.allThreadsCount();
			int local = tg.enumerate(new Thread[all], false); // tg.threadsCount();
			ThreadGroup p = tg.getParent();
			String parent = (p == null) ? "-none-" : p.getName(); //$NON-NLS-1$
			sb.setLength(0);
			sb.append(Util.toString(simpleClassName(tg), 18)).append(" ").append(Util.toString(tg.getName(), 21)) //$NON-NLS-1$
					.append(" ").append(Util.toString(parent, 16)) //$NON-NLS-1$
					.append(Util.toString(Integer.valueOf(tg.getMaxPriority()), 3))
					.append(Util.toString(Integer.valueOf(local), 4)).append("/") //$NON-NLS-1$
					.append(Util.toString(String.valueOf(all), 6));
			System.out.println(sb.toString());
		}
		System.out.print(newline);
		System.out.println(ConsoleMsg.CONSOLE_THREADTYPE_TITLE);
		for (int j = 0; j < count; j++) {
			Thread t = threads[j];
			if (t != null) {
				sb.setLength(0);
				sb.append(Util.toString(simpleClassName(t), 18)).append(" ").append(Util.toString(t.getName(), 21)) //$NON-NLS-1$
						.append(" ").append(Util.toString(t.getThreadGroup().getName(), 16)) //$NON-NLS-1$
						.append(Util.toString(Integer.valueOf(t.getPriority()), 3));
				if (t.isDaemon()) {
					sb.append(" [daemon]"); //$NON-NLS-1$
				}
				System.out.println(sb.toString());
			}
		}
	}

	/**
	 * Handles the sl (startlevel) command.
	 *
	 * @param bundle bundle to display startlevel for; if no bundle is specified,
	 *               the framework startlevel is displayed
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SL_COMMAND_DESCRIPTION)
	public void sl(@Descriptor(ConsoleMsg.CONSOLE_HELP_SL_COMMAND_ARGUMENT_DESCRIPTION) Bundle... bundle)
			throws Exception {
		int value = 0;
		if (bundle == null || bundle.length == 0) { // must want framework startlevel
			value = activator.getStartLevel().getStartLevel();
			System.out.println(NLS.bind(ConsoleMsg.STARTLEVEL_FRAMEWORK_ACTIVE_STARTLEVEL, String.valueOf(value)));
		} else { // must want bundle startlevel
			value = bundle[0].adapt(BundleStartLevel.class).getStartLevel();
			System.out.println(NLS.bind(ConsoleMsg.STARTLEVEL_BUNDLE_STARTLEVEL, Long.valueOf(bundle[0].getBundleId()),
					Integer.valueOf(value)));
		}
	}

	/**
	 * Handles the setfwsl (set framework startlevel) command.
	 *
	 * @param newSL new value for the framewrok start level
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SETFWSL_COMMAND_DESCRIPTION)
	public void setfwsl(@Descriptor(ConsoleMsg.CONSOLE_HELP_SETFWSL_COMMAND_ARGUMENT_DESCRIPTION) int newSL)
			throws Exception {
		try {
			activator.getStartLevel().setStartLevel(newSL);
			System.out.println(NLS.bind(ConsoleMsg.STARTLEVEL_FRAMEWORK_ACTIVE_STARTLEVEL, String.valueOf(newSL)));
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Handles the setbsl (set bundle startlevel) command.
	 *
	 * @param newSL   new value for bundle start level
	 * @param bundles bundles whose start value will be changed
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SETBSL_COMMAND_DESCRIPTION)
	public void setbsl(@Descriptor(ConsoleMsg.CONSOLE_HELP_SETFWSL_COMMAND_ARGUMENT_DESCRIPTION) int newSL,
			@Descriptor(ConsoleMsg.CONSOLE_HELP_SETBSL_COMMAND_ARGUMENT_DESCRIPTION) Bundle... bundles)
			throws Exception {
		if (bundles == null) {
			System.out.println(ConsoleMsg.STARTLEVEL_NO_STARTLEVEL_OR_BUNDLE_GIVEN);
			return;
		}
		for (Bundle bundle : bundles) {
			try {
				bundle.adapt(BundleStartLevel.class).setStartLevel(newSL);
				System.out.println(NLS.bind(ConsoleMsg.STARTLEVEL_BUNDLE_STARTLEVEL, Long.valueOf(bundle.getBundleId()),
						Integer.valueOf(newSL)));
			} catch (IllegalArgumentException e) {
				System.out.println(e.getMessage());
			}
		}
	}

	/**
	 * Handles the setibsl (set initial bundle startlevel) command.
	 *
	 * @param newInitialSL new value for initial start level
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SETIBSL_COMMAND_DESCRIPTION)
	public void setibsl(@Descriptor(ConsoleMsg.CONSOLE_HELP_SETFWSL_COMMAND_ARGUMENT_DESCRIPTION) int newInitialSL)
			throws Exception {
		try {
			activator.getStartLevel().setInitialBundleStartLevel(newInitialSL);
			System.out.println(NLS.bind(ConsoleMsg.STARTLEVEL_INITIAL_BUNDLE_STARTLEVEL, String.valueOf(newInitialSL)));
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
		}
	}

	/**
	 * Lists required bundles having the specified symbolic name or all if no bsn is
	 * specified
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_REQUIRED_BUNDLES_COMMAND_DESCRIPTION)
	public void requiredBundles(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_REQUIRED_BUNDLES_COMMAND_ARGUMENT_DESCRIPTION) String... symbolicName) {
		classSpaces(symbolicName);
	}

	/**
	 * Lists required bundles having the specified symbolic name or all if no bsn is
	 * specified
	 */
	@SuppressWarnings("deprecation")
	@Descriptor(ConsoleMsg.CONSOLE_HELP_REQUIRED_BUNDLES_COMMAND_DESCRIPTION)
	public void classSpaces(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_REQUIRED_BUNDLES_COMMAND_ARGUMENT_DESCRIPTION) String... symbolicName) {
		PackageAdmin packageAdmin = activator.getPackageAdmin();
		if (packageAdmin == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_NO_PACKAGE_ADMIN_MESSAGE);
			return;
		}
		String[] names;
		if (symbolicName == null || symbolicName.length == 0) {
			names = null;
		} else {
			names = symbolicName;
		}
		List<Bundle> bundles = new ArrayList<>();
		if (names == null) {
			bundles.addAll(Arrays.asList(packageAdmin.getBundles(null, null)));
		} else {
			for (String name : names) {
				Bundle[] sameName = packageAdmin.getBundles(name, null);
				if (sameName != null) {
					bundles.addAll(Arrays.asList(sameName));
				}
			}
		}
		if (bundles.isEmpty()) {
			System.out.println(ConsoleMsg.CONSOLE_NO_NAMED_CLASS_SPACES_MESSAGE);
		} else {
			for (Bundle bundle : bundles) {
				BundleRevisions revisions = bundle.adapt(BundleRevisions.class);
				List<BundleRevision> revisionList = revisions.getRevisions();
				BundleRevision revision = revisionList.isEmpty() ? null : revisionList.get(0);
				BundleWiring wiring = revision == null ? null : revision.getWiring();
				System.out.print(revision);
				if (wiring == null) {
					System.out.print("<"); //$NON-NLS-1$
					System.out.print(ConsoleMsg.CONSOLE_STALE_MESSAGE);
					System.out.println(">"); //$NON-NLS-1$
				} else if (!wiring.isCurrent()) {
					System.out.print("<"); //$NON-NLS-1$
					System.out.print(ConsoleMsg.CONSOLE_REMOVAL_PENDING_MESSAGE);
					System.out.println(">"); //$NON-NLS-1$
				} else {
					System.out.println();
				}
				if (wiring != null) {
					List<BundleWire> requiring = wiring.getProvidedWires(BundleNamespace.BUNDLE_NAMESPACE);
					for (BundleWire requiringWire : requiring) {
						System.out.print("  "); //$NON-NLS-1$
						System.out.print(requiringWire.getRequirer().getBundle());
						System.out.print(" "); //$NON-NLS-1$
						System.out.println(ConsoleMsg.CONSOLE_REQUIRES_MESSAGE);
					}
				}
			}
		}
	}

	/**
	 * Handles the profilelog command.
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_PROFILELOG_COMMAND_DESCRIPTION)
	public void profilelog() throws Exception {
		Class<?> profileClass = BundleContext.class.getClassLoader()
				.loadClass("org.eclipse.osgi.internal.profile.Profile");
		Method getProfileLog = profileClass.getMethod("getProfileLog", (Class<?>[]) null);
		System.out.println(getProfileLog.invoke(null, (Object[]) null));
	}

	/**
	 * Lists all packages visible from the specified bundle
	 * 
	 * @param bundle bundle to list visible packages
	 */

	@Descriptor(ConsoleMsg.CONSOLE_HELP_VISIBLE_PACKAGES_COMMAND_DESCRIPTION)
	public void getPackages(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_VISIBLE_PACKAGES_COMMAND_ARGUMENTS_DESCRIPTION) Bundle bundle) {
		BundleRevision revision = bundle.adapt(BundleRevision.class);
		if (revision == null) {
			System.out.println("Bundle is uninstalled.");
			return;
		}

		if ((revision.getTypes() & BundleRevision.TYPE_FRAGMENT) != 0) {
			System.out.println("Bundle is a fragment.");
			return;
		}

		BundleWiring wiring = revision.getWiring();
		if (wiring == null) {
			System.out.println("Bundle is not resolved.");
			return;
		}

		Map<String, Set<PackageSource>> packages = getPackagesInternal(wiring);
		for (Set<PackageSource> packageSources : packages.values()) {
			for (PackageSource packageSource : packageSources) {
				printCapability("  ", packageSource.getCapability(), packageSource.getWire(),
						PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE);
			}
		}
	}

	class PackageSource {
		private final BundleCapability cap;
		private final BundleWire wire;

		PackageSource(BundleCapability cap, BundleWire wire) {
			this.cap = cap;
			this.wire = wire;
		}

		BundleCapability getCapability() {
			return cap;
		}

		BundleWire getWire() {
			return wire;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof PackageSource) {
				return Objects.equals(cap, ((PackageSource) o).cap)
						&& Objects.equals(wire.getProvider(), ((PackageSource) o).wire.getProvider());
			}
			return false;
		}

		@Override
		public int hashCode() {
			return cap.hashCode() ^ wire.getProvider().hashCode();
		}
	}

	private Map<String, Set<PackageSource>> getPackagesInternal(BundleWiring wiring) {
		return getPackagesInternal0(wiring, null);
	}

	private Map<String, Set<PackageSource>> getPackagesInternal0(BundleWiring wiring,
			Map<BundleWiring, Map<String, Set<PackageSource>>> allSources) {
		if (allSources == null) {
			allSources = new HashMap<>();
		}
		Map<String, Set<PackageSource>> packages = allSources.get(wiring);
		if (packages != null) {
			return packages;
		}
		packages = new TreeMap<>();
		allSources.put(wiring, packages);

		// first get the imported packages
		List<BundleWire> packageWires = wiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE);
		Set<String> importedPackageNames = new HashSet<>();
		for (BundleWire packageWire : packageWires) {
			String packageName = (String) packageWire.getCapability().getAttributes()
					.get(PackageNamespace.PACKAGE_NAMESPACE);
			importedPackageNames.add(packageName);
			addAggregatePackageSource(packageWire.getCapability(), packageName, packageWire, packages, allSources);
		}

		// now get packages from required bundles
		for (BundleWire requiredWire : wiring.getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE)) {
			getRequiredBundlePackages(requiredWire, importedPackageNames, packages, allSources);
		}

		return packages;
	}

	private void addAggregatePackageSource(BundleCapability packageCap, String packageName, BundleWire wire,
			Map<String, Set<PackageSource>> packages, Map<BundleWiring, Map<String, Set<PackageSource>>> allSources) {
		Set<PackageSource> packageSources = packages.get(packageName);
		if (packageSources == null) {
			packageSources = new LinkedHashSet<>();
			packages.put(packageName, packageSources);
		}
		packageSources.add(new PackageSource(packageCap, wire));
		// source may be a split package aggregate
		Set<PackageSource> providerSource = getPackagesInternal0(wire.getProviderWiring(), allSources).get(packageName);
		if (providerSource != null) {
			packageSources.addAll(providerSource);
		}
	}

	private void getRequiredBundlePackages(BundleWire requiredWire, Set<String> importedPackageNames,
			Map<String, Set<PackageSource>> packages, Map<BundleWiring, Map<String, Set<PackageSource>>> allSources) {
		BundleWiring providerWiring = requiredWire.getProviderWiring();
		for (BundleCapability packageCapability : providerWiring.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE)) {
			String packageName = (String) packageCapability.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
			// if imported then packages from required bundles do not get added
			if (!importedPackageNames.contains(packageName)) {
				addAggregatePackageSource(packageCapability, packageName, requiredWire, packages, allSources);
			}
		}

		// get the declared packages
		Set<String> declaredPackageNames = new HashSet<>();
		for (BundleCapability declaredPackage : providerWiring.getRevision()
				.getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE)) {
			declaredPackageNames.add((String) declaredPackage.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
		}
		// and from attached fragments
		for (BundleWire fragmentWire : providerWiring.getProvidedWires(HostNamespace.HOST_NAMESPACE)) {
			for (BundleCapability declaredPackage : fragmentWire.getRequirer()
					.getDeclaredCapabilities(PackageNamespace.PACKAGE_NAMESPACE)) {
				declaredPackageNames
						.add((String) declaredPackage.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
			}
		}

		for (BundleWire packageWire : providerWiring.getRequiredWires(PackageNamespace.PACKAGE_NAMESPACE)) {
			String packageName = (String) packageWire.getCapability().getAttributes()
					.get(PackageNamespace.PACKAGE_NAMESPACE);
			if (!importedPackageNames.contains(packageName) && declaredPackageNames.contains(packageName)) {
				// if the package is a declared capability AND the wiring imports the package
				// then it is substituted
				addAggregatePackageSource(packageWire.getCapability(), packageName, packageWire, packages, allSources);
			}
		}

		// now get packages from re-exported requires of the required bundle
		for (BundleWire providerBundleWire : providerWiring.getRequiredWires(BundleNamespace.BUNDLE_NAMESPACE)) {
			String visibilityDirective = providerBundleWire.getRequirement().getDirectives()
					.get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE);
			if (BundleNamespace.VISIBILITY_REEXPORT.equals(visibilityDirective)) {
				getRequiredBundlePackages(providerBundleWire, importedPackageNames, packages, allSources);
			}
		}
	}

	/**
	 * Given a string containing a startlevel value, validate it and convert it to
	 * an int
	 *
	 * @param value A string containing a potential startlevel
	 * @return The start level or an int &lt;0 if it was invalid
	 */
	protected int getStartLevelFromToken(String value) {
		int retval = -1;
		try {
			retval = Integer.parseInt(value);
			if (Integer.parseInt(value) <= 0) {
				System.out.println(ConsoleMsg.STARTLEVEL_POSITIVE_INTEGER);
			}
		} catch (NumberFormatException nfe) {
			System.out.println(ConsoleMsg.STARTLEVEL_POSITIVE_INTEGER);
		}
		return retval;
	}

	/**
	 * Given a bundle, return the string describing that bundle's state.
	 *
	 * @param bundle A bundle to return the state of
	 * @return A String describing the state
	 */
	protected String getStateName(Bundle bundle) {
		int state = bundle.getState();
		switch (state) {
		case Bundle.UNINSTALLED:
			return "UNINSTALLED "; //$NON-NLS-1$

		case Bundle.INSTALLED:
			return "INSTALLED   "; //$NON-NLS-1$

		case Bundle.RESOLVED:
			return "RESOLVED    "; //$NON-NLS-1$

		case Bundle.STARTING:
			synchronized (lazyActivation) {
				if (lazyActivation.contains(bundle)) {
					return "<<LAZY>>    "; //$NON-NLS-1$
				}
				return "STARTING    "; //$NON-NLS-1$
			}

		case Bundle.STOPPING:
			return "STOPPING    "; //$NON-NLS-1$

		case Bundle.ACTIVE:
			return "ACTIVE      "; //$NON-NLS-1$

		default:
			return Integer.toHexString(state);
		}
	}

	/**
	 * Answers all thread groups in the system.
	 *
	 * @return An array of all thread groups.
	 */
	protected ThreadGroup[] getThreadGroups() {
		ThreadGroup tg = getTopThreadGroup();
		ThreadGroup[] groups = new ThreadGroup[tg.activeGroupCount()];
		int count = tg.enumerate(groups, true);
		if (count == groups.length) {
			return groups;
		}
		// get rid of null entries
		ThreadGroup[] ngroups = new ThreadGroup[count];
		System.arraycopy(groups, 0, ngroups, 0, count);
		return ngroups;
	}

	/**
	 * Answers the top level group of the current thread.
	 * <p>
	 * It is the 'system' or 'main' thread group under which all 'user' thread
	 * groups are allocated.
	 *
	 * @return The parent of all user thread groups.
	 */
	protected ThreadGroup getTopThreadGroup() {
		ThreadGroup topGroup = Thread.currentThread().getThreadGroup();
		if (topGroup != null) {
			while (topGroup.getParent() != null) {
				topGroup = topGroup.getParent();
			}
		}
		return topGroup;
	}

	/**
	 * Returns the simple class name of an object.
	 *
	 * @param o The object for which a class name is requested
	 * @return The simple class name.
	 */
	public String simpleClassName(Object o) {
		java.util.StringTokenizer t = new java.util.StringTokenizer(o.getClass().getName(), "."); //$NON-NLS-1$
		int ct = t.countTokens();
		for (int i = 1; i < ct; i++) {
			t.nextToken();
		}
		return t.nextToken();
	}

	@Descriptor(ConsoleMsg.CONSOLE_HELP_GETPROP_COMMAND_DESCRIPTION)
	public void getprop(@Descriptor(ConsoleMsg.CONSOLE_HELP_GETPROP_COMMAND_ARGUMENT_DESCRIPTION) String... propName)
			throws Exception {
		Properties allProperties = System.getProperties();
		Iterator<?> propertyNames = new TreeSet<>(allProperties.keySet()).iterator();
		while (propertyNames.hasNext()) {
			String prop = (String) propertyNames.next();
			if (propName == null || propName.length == 0 || prop.startsWith(propName[0])) {
				System.out.println(prop + '=' + allProperties.getProperty(prop));
			}
		}
	}

	@Descriptor(ConsoleMsg.CONSOLE_HELP_DIAG_COMMAND_DESCRIPTION)
	public void diag(@Descriptor(ConsoleMsg.CONSOLE_HELP_DIAG_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles)
			throws Exception {
		if (bundles.length == 0) {
			List<Bundle> unresolved = new ArrayList<>();
			Bundle[] allBundles = context.getBundles();
			for (Bundle bundle : allBundles) {
				BundleRevision revision = bundle.adapt(BundleRevision.class);
				if (revision != null && revision.getWiring() == null) {
					unresolved.add(bundle);
				}
			}
			if (unresolved.isEmpty()) {
				System.out.println("No unresolved bundles.");
				return;
			}
			bundles = unresolved.toArray(new Bundle[unresolved.size()]);
		}
		ResolutionReport report = getResolutionReport(bundles);
		for (Bundle bundle : bundles) {
			BundleRevision revision = bundle.adapt(BundleRevision.class);
			if (revision != null) {
				System.out.println(report.getResolutionReportMessage(revision));
			}
		}
	}

	private ResolutionReport getResolutionReport(Bundle[] bundles) {
		DiagReportListener reportListener = new DiagReportListener(bundles);
		ServiceRegistration<ResolverHookFactory> hookReg = context.registerService(ResolverHookFactory.class,
				reportListener, null);
		try {
			Bundle systemBundle = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
			FrameworkWiring frameworkWiring = systemBundle.adapt(FrameworkWiring.class);
			frameworkWiring.resolveBundles(Arrays.asList(bundles));
			return reportListener.getReport();
		} finally {
			hookReg.unregister();
		}
	}

	private static class DiagReportListener implements ResolverHookFactory {
		private final Collection<BundleRevision> targetTriggers = new ArrayList<>();

		public DiagReportListener(Bundle[] bundles) {
			for (Bundle bundle : bundles) {
				BundleRevision revision = bundle.adapt(BundleRevision.class);
				if (revision != null && revision.getWiring() == null) {
					targetTriggers.add(revision);
				}
			}

		}

		volatile ResolutionReport report = null;

		class DiagResolverHook implements ResolverHook, ResolutionReport.Listener {

			@Override
			public void handleResolutionReport(ResolutionReport report) {
				DiagReportListener.this.report = report;
			}

			@Override
			public void filterResolvable(Collection<BundleRevision> candidates) {
				// nothing
			}

			@Override
			public void filterSingletonCollisions(BundleCapability singleton,
					Collection<BundleCapability> collisionCandidates) {
				// nothing
			}

			@Override
			public void filterMatches(BundleRequirement requirement, Collection<BundleCapability> candidates) {
				// nothing
			}

			@Override
			public void end() {
				// nothing
			}

		}

		@Override
		public ResolverHook begin(Collection<BundleRevision> triggers) {
			if (triggers.containsAll(targetTriggers)) {
				return new DiagResolverHook();
			}
			return null;
		}

		ResolutionReport getReport() {
			return report;
		}
	}

	/**
	 * This is used to track lazily activated bundles.
	 */
	@Override
	public void bundleChanged(BundleEvent event) {
		int type = event.getType();
		Bundle bundle = event.getBundle();
		synchronized (lazyActivation) {
			switch (type) {
			case BundleEvent.LAZY_ACTIVATION:
				if (!lazyActivation.contains(bundle)) {
					lazyActivation.add(bundle);
				}
				break;

			default:
				lazyActivation.remove(bundle);
				break;
			}
		}

	}

	private boolean confirmStop(CommandSession session) {
		PrintStream consoleStream = session.getConsole();
		consoleStream.print(ConsoleMsg.CONSOLE_STOP_MESSAGE);
		consoleStream.flush();

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String reply = null;
		try {
			reply = reader.readLine();
		} catch (IOException e) {
			consoleStream.println(ConsoleMsg.CONSOLE_STOP_ERROR_READ_CONFIRMATION);
		}

		if (reply != null) {
			if (reply.toLowerCase().startsWith(ConsoleMsg.CONSOLE_STOP_CONFIRMATION_YES) || reply.length() == 0) {
				return true;
			}
		}

		return false;
	}
}
