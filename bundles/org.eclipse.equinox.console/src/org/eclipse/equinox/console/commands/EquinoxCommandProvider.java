/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lazar Kirchev, SAP AG - derivative implementation to migrate the commands from FrameworkCommandProvider to Gogo shell commands
 *******************************************************************************/

package org.eclipse.equinox.console.commands;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Converter;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.eclipse.equinox.console.command.adapter.Activator;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.DisabledInfo;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.GenericSpecification;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.NativeCodeSpecification;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.osgi.service.resolver.VersionConstraint;
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
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.osgi.service.permissionadmin.PermissionAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * This class provides methods to execute commands from the command line.  It registers
 * itself as a service so it can be invoked by a CommandProcessor. 
 *
 * The commands provided by this class are:
 ---Controlling the OSGi framework---
 close - shutdown and exit
 exit - exit immediately (System.exit)
 gc - perform a garbage collection
 init - uninstall all bundles
 launch - start the Service Management Framework
 setprop <key>=<value> - set the OSGI property
 shutdown - shutdown the Service Management Framework
 ---Controlliing Bundles---
 install <url> {s[tart]} - install and optionally start bundle from the given URL
 refresh (<id>|<location>) - refresh the packages of the specified bundles
 start (<id>|<location>) - start the specified bundle(s)
 stop (<id>|<location>) - stop the specified bundle(s)
 uninstall (<id>|<location>) - uninstall the specified bundle(s)
 update (<id>|<location>|<*>) - update the specified bundle(s)
 ---Displaying Status---
 bundle (<id>|<location>) - display details for the specified bundle(s)
 bundles - display details for all installed bundles
 headers (<id>|<location>) - print bundle headers
 packages {<pkgname>|<id>|<location>} - display imported/exported package details
 props - display System properties
 services {filter} - display registered service details. Examples for [filter]: (objectClass=com.xyz.Person); (&(objectClass=com.xyz.Person)(|(sn=Jensen)(cn=Babs J*))); passing only com.xyz.Person is a shortcut for (objectClass=com.xyz.Person). The filter syntax specification is available at http://www.ietf.org/rfc/rfc1960.txt
 ss - display installed bundles (short status)
 status - display installed bundles and registered services
 threads - display threads and thread groups
 ---Extras---
 exec <command> - execute a command in a separate process and wait
 fork <command> - execute a command in a separate process
 getprop <name> -  Displays the system properties with the given name, or all of them.
 requiredBundles [<bsn>] - lists required bundles having the specified symbolic name or all if no bsn is specified
 classSpaces [<bsn>] - lists required bundles having the specified symbolic name or all if no bsn is specified
 ---Controlling StartLevel---
 sl {(<id>|<location>)} - display the start level for the specified bundle, or for the framework if no bundle specified
 setfwsl <start level> - set the framework start level
 setbsl <start level> (<id>|<location>) - set the start level for the bundle(s)
 setibsl <start level> - set the initial bundle start level
 ---Eclipse Runtime commands---
 diag - Displays unsatisfied constraints for the specified bundle(s)
 enableBundle - Enable the specified bundle(s)
 disableBundle - Disable the specified bundle(s)
 disabledBundles - List disabled bundles in the system
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
	private final List<Bundle> lazyActivation = new ArrayList<Bundle>();
	
	private Activator activator;
	
	/** commands provided by this command provider */ 
	private static final String[] functions = new String[] {"exit", "shutdown", "sta", "start", "sto", "stop", "i", 
		"install", "up", "up", "up", "update", "update", "update", "un", "uninstall", "s", "status", "se", "services",
		"p", "p", "packages", "packages", "bundles", "b", "bundle", "gc", "init", "close", "r", "refresh", "exec",
		"fork", "h", "headers", "pr", "props", "setp", "setprop", "ss", "t", "threads", "sl", "setfwsl", "setbsl",
		"setibsl", "requiredBundles", "classSpaces", "profilelog", "getPackages", "getprop", "diag", "enableBundle", 
		"disableBundle", "disabledBundles"};
	
	private static final String POLICY_CONSOLE = "org.eclipse.equinox.console"; //$NON-NLS-1$
	
	/**
	 *  Constructor.
	 *
	 *  start() must be called after creating this object.
	 *
	 *  @param framework The current instance of the framework
	 */
	public EquinoxCommandProvider(BundleContext context, Activator activator) {
		this.context = context;
		this.activator = activator;
	}

	/**
	 *  Starts this CommandProvider.
	 *
	 *  Registers this object as a service providing commands
	 *  Adds this object as a SynchronousBundleListener.
	 */
	public void startService() {
		EquinoxCommandsConverter converter = new EquinoxCommandsConverter(context);
		converterReg = context.registerService(Converter.class.getName(), converter, null);
		
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
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
	 *  Handle the exit command.  Exit immediately (System.exit)
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_EXIT_COMMAND_DESCRIPTION)
	public void exit() throws Exception {
		if (confirmStop()) {
			System.out.println();
			System.exit(0);
		}
	}

	/**
	 *  Handle the shutdown command.  Shutdown the OSGi framework.
	 *
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SHUTDOWN_COMMAND_DESCRIPTION)
	public void shutdown() throws Exception {
		context.getBundle(0).stop();
	}

	/**
	 *  Handle the start command's abbreviation.  Invoke start()
	 *
	 *  @param bundles bundle(s) to be started
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_START_COMMAND_DESCRIPTION)
	public void sta(@Descriptor(ConsoleMsg.CONSOLE_HELP_START_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles) throws Exception {
		start(bundles);
	}

	/**
	 *  Handle the start command.  Start the specified bundle(s).
	 *
	 *  @param bundles bundle(s) to be started
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_START_COMMAND_DESCRIPTION)
	public void start(@Descriptor(ConsoleMsg.CONSOLE_HELP_START_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles) throws Exception {
		if (bundles == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}
		
		if (bundles.length == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}
		
		for(Bundle bundle : bundles) {
				bundle.start();
		}
	}

	/**
	 *  Handle the stop command's abbreviation.  Invoke stop()
	 *
	 *  @param bundles bundle(s) to be stopped.
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_STOP_COMMAND_DESCRIPTION)
	public void sto(@Descriptor(ConsoleMsg.CONSOLE_HELP_STOP_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles) throws Exception {
		stop(bundles);
	}

	/**
	 *  Handle the stop command.  Stop the specified bundle(s).
	 *
	 *  @param bundles bundle(s) to be stopped.
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_STOP_COMMAND_DESCRIPTION)
	public void stop(@Descriptor(ConsoleMsg.CONSOLE_HELP_STOP_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles) throws Exception {
		if (bundles == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}
		
		if (bundles.length == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}
		
		for(Bundle bundle : bundles) {
				bundle.stop();
		}
	}

	/**
	 *  Handle the install command's abbreviation.  Invoke install()
	 *
	 *	@param shouldStart if the bundle should be start after installation
	 *  @param url location of the bundle to be installed
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_INSTALL_COMMAND_DESCRIPTION)
	public void i(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_INSTALL_START_OPTION_DESCRIPTION)
			@Parameter(absentValue = "false", presentValue = "true", names = { "-start" })
			boolean shouldStart,
			@Descriptor(ConsoleMsg.CONSOLE_HELP_INSTALL_START_ARGUMENT_DESCRIPTION) String url) throws Exception {
		install(shouldStart, url);
	}

	/**
	 *  Handle the install command.  Install and optionally start bundle from the given URL
	 *
	 *  @param shouldStart if the bundle should be start after installation
	 *  @param url location of the bundle to be installed
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_INSTALL_COMMAND_DESCRIPTION)
	public Bundle install(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_INSTALL_START_OPTION_DESCRIPTION)
			@Parameter(absentValue = "false", presentValue = "true", names = { "-start" })
			boolean shouldStart,
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
	 *  Handle the update command's abbreviation.  Invoke update()
	 *
	 *  @param bundles bundle(s) to be updated
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_COMMAND_DESCRIPTION)
	public void up(@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles) throws Exception {
		update(bundles);
	}
	
	/**
	 *  Handle the update command's abbreviation.  Invoke update()
	 *
	 *  @param bundle bundle to be updated
	 *  @param source location to get the new bundle's content
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_SOURCE_COMMAND_DESCRIPTION)
	public void up(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_SOURCE_COMMAND_BUNDLE_ARGUMENT_DESCRIPTION)
			Bundle bundle, 
			@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_SOURCE_COMMAND_URL_ARGUMENT_DESCRIPTION)
			URL source) throws Exception {
		update(bundle, source);
	}
	
	/**
	 *  Handle the update command.  Update the specified bundle(s).
	 *
	 *  @param bundles bundle(s) to be updated
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_COMMAND_DESCRIPTION)
	public void update(@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles) throws Exception {
		if (bundles == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}
		
		if(bundles.length == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}
		
		for(Bundle bundle : bundles) {
			bundle.update();
		}
	}

	/**
	 *  Handle the update command.  Update the specified bundle with the specified content.
	 *
	 *  @param bundle bundle to be updated
	 *  @param source location to get the new bundle's content
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_SOURCE_COMMAND_DESCRIPTION)
	public void update(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_SOURCE_COMMAND_BUNDLE_ARGUMENT_DESCRIPTION)
			Bundle bundle, 
			@Descriptor(ConsoleMsg.CONSOLE_HELP_UPDATE_SOURCE_COMMAND_URL_ARGUMENT_DESCRIPTION)
			URL source) throws Exception {
		bundle.update(source.openStream());
	}

	/**
	 *  Handle the uninstall command's abbreviation.  Invoke uninstall()
	 *
	 *  @param bundles bundle(s) to uninstall
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_UNINSTALL_COMMAND_DESCRIPTION)
	public void un(@Descriptor(ConsoleMsg.CONSOLE_HELP_UNINSTALL_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles) throws Exception {
		uninstall(bundles);
	}

	/**
	 *  Handle the uninstall command.  Uninstall the specified bundle(s).
	 *
	 *  @param bundles bundle(s) to uninstall
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_UNINSTALL_COMMAND_DESCRIPTION)
	public void uninstall(@Descriptor(ConsoleMsg.CONSOLE_HELP_UNINSTALL_COMMAND_ARGUMENT_DESCRIPTION) Bundle[] bundles) throws Exception {
		if(bundles == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}
		
		if(bundles.length == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}
		
		for (Bundle bundle : bundles) {
				bundle.uninstall();
		}
	}

	private int getStatesFromConstants(String states) throws IllegalArgumentException{
		int stateFilter = -1;
		if(!states.equals("")) {
			StringTokenizer tokens = new StringTokenizer(states, ","); //$NON-NLS-1
			while (tokens.hasMoreElements()) {
				String desiredState = (String) tokens.nextElement();
				Field match = null;
				try {
					match = Bundle.class.getField(desiredState.toUpperCase());
					if (stateFilter == -1)
						stateFilter = 0;
					stateFilter |= match.getInt(match);
				} catch (NoSuchFieldException e) {
					System.out.println(ConsoleMsg.CONSOLE_INVALID_INPUT + ": " + desiredState); //$NON-NLS-1$
					throw new IllegalArgumentException();
				} catch (IllegalAccessException e) {
					System.out.println(ConsoleMsg.CONSOLE_INVALID_INPUT + ": " + desiredState); //$NON-NLS-1$
					throw new IllegalArgumentException();
				}
			}
		}
		return stateFilter;
	}
	
	/**
	 *  Handle the status command's abbreviation.  Invoke status()
	 *
	 *  @param arguments 
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_STATUS_COMMAND_DESCRIPTION)
	public void s(@Descriptor(ConsoleMsg.CONSOLE_HELP_STATUS_ARGUMENT_DESCRIPTION) String... arguments) throws Exception {
		status(arguments);
	}

	/**
	 *  Handle the status command.  Display installed bundles and registered services.
	 *
	 *  @param arguments
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_STATUS_COMMAND_DESCRIPTION)
	public void status(@Descriptor(ConsoleMsg.CONSOLE_HELP_STATUS_ARGUMENT_DESCRIPTION) String... arguments) throws Exception {
		if (context.getBundle(0).getState() == Bundle.ACTIVE) {
			System.out.println(ConsoleMsg.CONSOLE_FRAMEWORK_IS_LAUNCHED_MESSAGE);
		} else {
			System.out.println(ConsoleMsg.CONSOLE_FRAMEWORK_IS_SHUTDOWN_MESSAGE);
		}
		System.out.println();
		
		String states = "";
		String[] bsnSegments = null;
		
		if(arguments != null && arguments.length > 0) {
			if(arguments[0].equals("-s")) {
				if (arguments.length > 1) {
					states = arguments[1];
					if(arguments.length > 2) {
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
			if (!match(bundle, bsnSegments, stateFilter))
				continue;
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
			for(ServiceReference<?> service : services) {
				System.out.println(service);
			}
		}
	}

	/**
	 *  Handle the services command's abbreviation.  Invoke services()
	 *
	 *  @param filters filters for services
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SERVICES_COMMAND_DESCRIPTION)
	public void se(@Descriptor(ConsoleMsg.CONSOLE_HELP_FILTER_ARGUMENT_DESCRIPTION)String... filters) throws Exception {
		services(filters);
	}

	/**
	 *  Handle the services command.  Display registered service details.
	 *
	 *  @param filters filters for services
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SERVICES_COMMAND_DESCRIPTION)
	public void services(@Descriptor(ConsoleMsg.CONSOLE_HELP_FILTER_ARGUMENT_DESCRIPTION)String... filters) throws Exception {
		String filter = null;
		if (filters != null && filters.length > 0) {
			StringBuffer buf = new StringBuffer();
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
		// If the filter is invalid and does not start with a bracket, probably the argument was the name of an interface.
		// Try to construct an object class filter with this argument, and if still invalid - throw the original InvalidSyntaxException
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
						for (int k = 0; k < users.length; k++) {
							System.out.print("    "); //$NON-NLS-1$
							System.out.println(users[k]);
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
	 *  Handle the packages command's abbreviation.  Invoke packages()
	 *
	 *  @param bundle bundle for which to display package details
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_COMMAND_DESCRIPTION)
	public void p(@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_BUNDLE_ARGUMENT_DESCRIPTION)Bundle... bundle) throws Exception {
		packages(bundle);
	}
	
	/**
	 *  Handle the packages command's abbreviation.  Invoke packages()
	 *
	 *  @param packageName package for which to display details
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_COMMAND_DESCRIPTION)
	public void p(@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_PACKAGE_ARGUMENT_DESCRIPTION)String packageName) throws Exception {
		packages(packageName);
	}
	
	/**
	 *  Handle the packages command.  Display imported/exported packages details.
	 *
	 *  @param bundle bundle for which to display package details
	 */
	@SuppressWarnings("deprecation")
	@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_COMMAND_DESCRIPTION)
	public void packages(@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_BUNDLE_ARGUMENT_DESCRIPTION)Bundle... bundle) throws Exception {
		if(activator.getPackageAdmin() != null) {
			ExportedPackage[] exportedPackages;
			if(bundle != null && bundle.length > 0) {
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
	 *  Handle the packages command.  Display imported/exported packages details.
	 *
	 *  @param packageName package for which to display details
	 **/
	@SuppressWarnings("deprecation")
	@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_COMMAND_DESCRIPTION)
	public void packages(@Descriptor(ConsoleMsg.CONSOLE_HELP_PACKAGES_PACKAGE_ARGUMENT_DESCRIPTION)String packageName) throws Exception {
		if(activator.getPackageAdmin() != null) {
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
		for (int i = 0; i < packages.length; i++) {
			org.osgi.service.packageadmin.ExportedPackage pkg = packages[i];
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
				for (int j = 0; j < importers.length; j++) {
					System.out.print("  "); //$NON-NLS-1$
					System.out.print(importers[j]);
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
	 *  Handle the bundles command.  Display details for all installed bundles.
	 *
	 *  @param arguments
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_BUNDLES_COMMAND_DESCRIPTION)
	public void bundles(@Descriptor(ConsoleMsg.CONSOLE_HELP_STATUS_ARGUMENT_DESCRIPTION) String... arguments) throws Exception {
		String states = "";
		String[] bsnSegments = null;
		
		if(arguments != null && arguments.length > 0) {
			if(arguments[0].equals("-s")) {
				if (arguments.length > 1) {
					states = arguments[1];
					if(arguments.length > 2) {
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
			if (!match(bundle, bsnSegments, stateFilter))
				continue;
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
				for (int j = 0; j < services.length; j++) {
					System.out.print("    "); //$NON-NLS-1$
					System.out.println(services[j]);
				}
			} else {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_NO_REGISTERED_SERVICES_MESSAGE);
			}

			services = bundle.getServicesInUse();
			if (services != null) {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_SERVICES_IN_USE_MESSAGE);
				for (int j = 0; j < services.length; j++) {
					System.out.print("    "); //$NON-NLS-1$
					System.out.println(services[j]);
				}
			} else {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_NO_SERVICES_IN_USE_MESSAGE);
			}
		}
	}

	/**
	 *  Handle the bundle command's abbreviation.  Invoke bundle()
	 *
	 *  @param bundles bundle(s) to display details for
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_BUNDLE_COMMAND_DESCRIPTION)
	public void b(@Descriptor(ConsoleMsg.CONSOLE_HELP_IDLOCATION_ARGUMENT_DESCRIPTION)Bundle[] bundles) throws Exception {
		bundle(bundles);
	}

	/**
	 *  Handle the bundle command.  Display details for the specified bundle(s).
	 *
	 *  @param bundles bundle(s) to display details for
	 */
	@SuppressWarnings({ "deprecation" })
	@Descriptor(ConsoleMsg.CONSOLE_HELP_BUNDLE_COMMAND_DESCRIPTION)
	public void bundle(@Descriptor(ConsoleMsg.CONSOLE_HELP_IDLOCATION_ARGUMENT_DESCRIPTION)Bundle[] bundles) throws Exception {
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
					for (int j = 0; j < services.length; j++) {
						System.out.print("    "); //$NON-NLS-1$
						System.out.println(services[j]);
					}
				} else {
					System.out.print("  "); //$NON-NLS-1$
					System.out.println(ConsoleMsg.CONSOLE_NO_REGISTERED_SERVICES_MESSAGE);
				}

				services = bundle.getServicesInUse();
				if (services != null) {
					System.out.print("  "); //$NON-NLS-1$
					System.out.println(ConsoleMsg.CONSOLE_SERVICES_IN_USE_MESSAGE);
					for (int j = 0; j < services.length; j++) {
						System.out.print("    "); //$NON-NLS-1$
						System.out.println(services[j]);
					}
				} else {
					System.out.print("  "); //$NON-NLS-1$
					System.out.println(ConsoleMsg.CONSOLE_NO_SERVICES_IN_USE_MESSAGE);
				}

				PackageAdmin packageAdmin = activator.getPackageAdmin();
				if (packageAdmin == null) {
					System.out.print("  "); //$NON-NLS-1$
					System.out.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_NO_PACKAGE_ADMIN_MESSAGE);
					continue;
				}
				
				PlatformAdmin platAdmin = activator.getPlatformAdmin();
				
				if (platAdmin != null) {
					BundleDescription desc = platAdmin.getState(false).getBundle(bundle.getBundleId());
					if (desc != null) {
						boolean title = true;
						
							ExportPackageDescription[] exports = desc.getExportPackages();
							if (exports == null || exports.length == 0) {
								System.out.print("  "); //$NON-NLS-1$
								System.out.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_MESSAGE);
							} else {
								title = true;

								for (int i = 0; i < exports.length; i++) {
									if (title) {
										System.out.print("  "); //$NON-NLS-1$
										System.out.println(ConsoleMsg.CONSOLE_EXPORTED_PACKAGES_MESSAGE);
										title = false;
									}
									System.out.print("    "); //$NON-NLS-1$
									System.out.print(exports[i].getName());
									System.out.print("; version=\""); //$NON-NLS-1$
									System.out.print(exports[i].getVersion());
									System.out.print("\""); //$NON-NLS-1$
									if (desc.isRemovalPending()) {
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
							if (desc != null) {
								List<ImportPackageSpecification> fragmentsImportPackages = new ArrayList<ImportPackageSpecification>();

								// Get bundle' fragments imports
								BundleDescription[] fragments = desc.getFragments();
								for (int i = 0; i < fragments.length; i++) {
									ImportPackageSpecification[] fragmentImports = fragments[i].getImportPackages();
									for (int j = 0; j < fragmentImports.length; j++) {
										fragmentsImportPackages.add(fragmentImports[j]);
									}
								}

								// Get all bundle imports
								ImportPackageSpecification[] importPackages;
								if (fragmentsImportPackages.size() > 0) {
									ImportPackageSpecification[] directImportPackages = desc.getImportPackages();
									importPackages = new ImportPackageSpecification[directImportPackages.length + fragmentsImportPackages.size()];

									for (int i = 0; i < directImportPackages.length; i++) {
										importPackages[i] = directImportPackages[i];
									}

									int offset = directImportPackages.length;
									for (int i = 0; i < fragmentsImportPackages.size(); i++) {
										importPackages[offset + i] = fragmentsImportPackages.get(i);
									}
								} else {
									importPackages = desc.getImportPackages();
								}

								// Get all resolved imports
								ExportPackageDescription[] imports = null;
								imports = desc.getContainingState().getStateHelper().getVisiblePackages(desc, StateHelper.VISIBLE_INCLUDE_EE_PACKAGES | StateHelper.VISIBLE_INCLUDE_ALL_HOST_WIRES);

								// Get the unresolved optional and dynamic imports
								List<ImportPackageSpecification> unresolvedImports = new ArrayList<ImportPackageSpecification>();

								for (int i = 0; i < importPackages.length; i++) {
									if (importPackages[i].getDirective(Constants.RESOLUTION_DIRECTIVE).equals(ImportPackageSpecification.RESOLUTION_OPTIONAL)) {
										if (importPackages[i].getSupplier() == null) {
											unresolvedImports.add(importPackages[i]);
										}
									} else if (importPackages[i].getDirective(org.osgi.framework.Constants.RESOLUTION_DIRECTIVE).equals(ImportPackageSpecification.RESOLUTION_DYNAMIC)) {
										boolean isResolvable = false;

										// Check if the dynamic import can be resolved by any of the wired imports, 
										// and if not - add it to the list of unresolved imports
										for (int j = 0; j < imports.length; j++) {
											if (importPackages[i].isSatisfiedBy(imports[j])) {
												isResolvable = true;
											}
										}

										if (isResolvable == false) {
											unresolvedImports.add(importPackages[i]);
										}
									}
								}

								title = printImportedPackages(imports, title);

								if (desc.isResolved() && (unresolvedImports.isEmpty() == false)) {
									printUnwiredDynamicImports(unresolvedImports);
									title = false;
								}
							}

							if (title) {
								System.out.print("  "); //$NON-NLS-1$
								System.out.println(ConsoleMsg.CONSOLE_NO_IMPORTED_PACKAGES_MESSAGE);
							}

							if (packageAdmin != null) {
								System.out.print("  "); //$NON-NLS-1$
								if ((packageAdmin.getBundleType(bundle) & PackageAdmin.BUNDLE_TYPE_FRAGMENT) > 0) {
									org.osgi.framework.Bundle[] hosts = packageAdmin.getHosts(bundle);
									if (hosts != null) {
										System.out.println(ConsoleMsg.CONSOLE_HOST_MESSAGE);
										for (int i = 0; i < hosts.length; i++) {
											System.out.print("    "); //$NON-NLS-1$
											System.out.println(hosts[i]);
										}
									} else {
										System.out.println(ConsoleMsg.CONSOLE_NO_HOST_MESSAGE);
									}
								} else {
									org.osgi.framework.Bundle[] fragments = packageAdmin.getFragments(bundle);
									if (fragments != null) {
										System.out.println(ConsoleMsg.CONSOLE_FRAGMENT_MESSAGE);
										for (int i = 0; i < fragments.length; i++) {
											System.out.print("    "); //$NON-NLS-1$
											System.out.println(fragments[i]);
										}
									} else {
										System.out.println(ConsoleMsg.CONSOLE_NO_FRAGMENT_MESSAGE);
									}
								}

								RequiredBundle[] requiredBundles = packageAdmin.getRequiredBundles(null);
								RequiredBundle requiredBundle = null;
								if (requiredBundles != null) {
									for (RequiredBundle rb : requiredBundles) {
										if (rb.getBundle() == bundle) {
											requiredBundle = rb;
											break;
										}
									}
								}

								if (requiredBundle == null) {
									System.out.print("  "); //$NON-NLS-1$
									System.out.println(ConsoleMsg.CONSOLE_NO_NAMED_CLASS_SPACES_MESSAGE);
								} else {
									System.out.print("  "); //$NON-NLS-1$
									System.out.println(ConsoleMsg.CONSOLE_NAMED_CLASS_SPACE_MESSAGE);
									System.out.print("    "); //$NON-NLS-1$
									System.out.print(requiredBundle);
									if (requiredBundle.isRemovalPending()) {
										System.out.println(ConsoleMsg.CONSOLE_REMOVAL_PENDING_MESSAGE);
									} else {
										System.out.println(ConsoleMsg.CONSOLE_PROVIDED_MESSAGE);
									}
								}
								title = true;
								if (requiredBundles != null) {
									for (RequiredBundle rb : requiredBundles) {
										if (rb == requiredBundle)
											continue;

										org.osgi.framework.Bundle[] depBundles = rb.getRequiringBundles();
										if (depBundles == null)
											continue;

										for (int j = 0; j < depBundles.length; j++) {
											if (depBundles[j] == bundle) {
												if (title) {
													System.out.print("  "); //$NON-NLS-1$
													System.out
															.println(ConsoleMsg.CONSOLE_REQUIRED_BUNDLES_MESSAGE);
													title = false;
												}
												System.out.print("    "); //$NON-NLS-1$
												System.out.print(rb);

												org.osgi.framework.Bundle provider = rb.getBundle();
												System.out.print("<"); //$NON-NLS-1$
												System.out.print(provider);
												System.out.println(">"); //$NON-NLS-1$
											}
										}
									}
								}
								if (title) {
									System.out.print("  "); //$NON-NLS-1$
									System.out.println(ConsoleMsg.CONSOLE_NO_REQUIRED_BUNDLES_MESSAGE);
								}

							}
						} 
					System.out.println();
					System.out.println();
				} else {
					System.out.print("  "); //$NON-NLS-1$
					System.out.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_NO_PLATFORM_ADMIN_MESSAGE);
				}		
		}
	}

	private boolean printImportedPackages(ExportPackageDescription[] importedPkgs, boolean title) {
		for (int i = 0; i < importedPkgs.length; i++) {
			if (title) {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_IMPORTED_PACKAGES_MESSAGE);
				title = false;
			}
			System.out.print("    "); //$NON-NLS-1$
			System.out.print(importedPkgs[i].getName());
			System.out.print("; version=\""); //$NON-NLS-1$
			System.out.print(importedPkgs[i].getVersion());
			System.out.print("\""); //$NON-NLS-1$
			Bundle exporter = context.getBundle(importedPkgs[i].getSupplier().getBundleId());
			if (exporter != null) {
				System.out.print("<"); //$NON-NLS-1$
				System.out.print(exporter);
				System.out.println(">"); //$NON-NLS-1$
			} else {
				System.out.print("<"); //$NON-NLS-1$
				System.out.print(ConsoleMsg.CONSOLE_STALE_MESSAGE);
				System.out.println(">"); //$NON-NLS-1$
			}
		}
		return title;
	}

	private void printUnwiredDynamicImports(List<ImportPackageSpecification> dynamicImports) {
		for (int i = 0; i < dynamicImports.size(); i++) {
			ImportPackageSpecification importPackage = dynamicImports.get(i);
			System.out.print("    "); //$NON-NLS-1$
			System.out.print(importPackage.getName());
			System.out.print("; version=\""); //$NON-NLS-1$
			System.out.print(importPackage.getVersionRange());
			System.out.print("\""); //$NON-NLS-1$
			System.out.print("<"); //$NON-NLS-1$
			System.out.print("unwired"); //$NON-NLS-1$
			System.out.print(">"); //$NON-NLS-1$
			System.out.print("<"); //$NON-NLS-1$
			System.out.print(importPackage.getDirective(org.osgi.framework.Constants.RESOLUTION_DIRECTIVE));
			System.out.println(">"); //$NON-NLS-1$
		}
	}

	/**
	 *  Handle the gc command.  Perform a garbage collection.
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
	 *  Handle the init command.  Uninstall all bundles.
	 *
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
			if (permLocations != null)
				for (int i = 0; i < permLocations.length; i++)
					securityAdmin.setPermissions(permLocations[i], null);
			ConditionalPermissionUpdate update = condPermAdmin.newConditionalPermissionUpdate();
			update.getConditionalPermissionInfos().clear();
			update.commit();
		}
		// clear the permissions from conditional permission admin
		if (securityAdmin != null)
			for (Enumeration<ConditionalPermissionInfo> infos = condPermAdmin.getConditionalPermissionInfos(); infos.hasMoreElements();)
				infos.nextElement().delete();
	}

	/**
	 *  Handle the close command.  Shutdown and exit.
	
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_CLOSE_COMMAND_DESCRIPTION)
	public void close() throws Exception {
		if (confirmStop()) {
			context.getBundle(0).stop();
			System.exit(0);
		}
	}

	/**
	 *  Handle the refresh command's abbreviation.  Invoke refresh()
	 *
	 *  @param bundles bundle(s) to be refreshed
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_REFRESH_COMMAND_DESCRIPTION)
	public void r(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_REFRESH_ALL_OPTION_DESCRIPTION)
			@Parameter(absentValue = "false", presentValue = "true", names = { "-all" })
			boolean shouldRefreshAll,
			@Descriptor(ConsoleMsg.CONSOLE_HELP_REFRESH_COMMAND_ARGUMENT_DESCRIPTION) Bundle... bundles) throws Exception {
		refresh(shouldRefreshAll, bundles);
	}

	/**
	 *  Handle the refresh command.  Refresh the packages of the specified bundles.
	 *
	 *  @param bundles bundle(s) to be refreshed
	 */
	@SuppressWarnings("deprecation")
	@Descriptor(ConsoleMsg.CONSOLE_HELP_REFRESH_COMMAND_DESCRIPTION)
	public void refresh(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_REFRESH_ALL_OPTION_DESCRIPTION)
			@Parameter(absentValue = "false", presentValue = "true", names = { "-all" })
			boolean shouldRefreshAll,
			@Descriptor(ConsoleMsg.CONSOLE_HELP_REFRESH_COMMAND_ARGUMENT_DESCRIPTION) Bundle... bundles) throws Exception {
		PackageAdmin packageAdmin = activator.getPackageAdmin();
		if (packageAdmin != null) {
			if(bundles != null && bundles.length > 0) {
				packageAdmin.refreshPackages(bundles);
			} else if (shouldRefreshAll == true) {
				packageAdmin.refreshPackages(context.getBundles());
			} else {
				packageAdmin.refreshPackages(null);
			}
		} else {
			System.out.println(ConsoleMsg.CONSOLE_CAN_NOT_REFRESH_NO_PACKAGE_ADMIN_ERROR);
		}
	}

	/**
	 * Executes the given system command in a separate system process
	 * and waits for it to finish.
	 *
	 * @param command command to be executed
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_EXEC_COMMAND_DESCRIPTION)
	public void exec(@Descriptor(ConsoleMsg.CONSOLE_HELP_EXEC_COMMAND_ARGUMENT_DESCRIPTION) String command) throws Exception {
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
	 * Executes the given system command in a separate system process.  It does
	 * not wait for a result.
	 *
	 * @param command command to be executed
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_FORK_COMMAND_DESCRIPTION)
	public void fork(@Descriptor(ConsoleMsg.CONSOLE_HELP_FORK_COMMAND_ARGUMENT_DESCRIPTION) String command) throws Exception {
		if (command == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_COMMAND_SPECIFIED_ERROR);
			return;
		}

		Process p = Runtime.getRuntime().exec(command);
		System.out.println(NLS.bind(ConsoleMsg.CONSOLE_STARTED_IN_MESSAGE, command, String.valueOf(p)));
	}

	/**
	 * Handle the headers command's abbreviation.  Invoke headers()
	 *
	 * @param bundles bundle(s) whose headers to display
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_HEADERS_COMMAND_DESCRIPTION)
	public List<Dictionary<String, String>> h(@Descriptor(ConsoleMsg.CONSOLE_HELP_HEADERS_COMMAND_ARGUMENT_DESCRIPTION) Bundle... bundles) throws Exception {
		return headers(bundles);
	}

	/**
	 * Handle the headers command.  Display headers for the specified bundle(s).
	 *
	 * @param bundles bundle(s) whose headers to display
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_HEADERS_COMMAND_DESCRIPTION)
	public List<Dictionary<String, String>> headers(@Descriptor(ConsoleMsg.CONSOLE_HELP_HEADERS_COMMAND_ARGUMENT_DESCRIPTION) Bundle... bundles) throws Exception {
		ArrayList<Dictionary<String, String>> headers = new ArrayList<Dictionary<String,String>>();
		
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
	 * Handles the props command's abbreviation.  Invokes props()
	 *
	 */
	@Descriptor(ConsoleMsg.CONSOLE_PROPS_COMMAND_DESCRIPTION)
	public Dictionary<?, ?> pr() throws Exception {
		 return props();
	}

	/**
	 * Handles the _props command.  Prints the system properties sorted.
	 *
	 */
	@Descriptor(ConsoleMsg.CONSOLE_PROPS_COMMAND_DESCRIPTION)
	public Dictionary<?, ?> props() throws Exception {
		System.out.println(ConsoleMsg.CONSOLE_SYSTEM_PROPERTIES_TITLE);
		return System.getProperties();
	}

	/**
	 * Handles the setprop command's abbreviation.  Invokes setprop()
	 *
	 * @param arguments key=value pairs for the new properties
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SETPROP_COMMAND_DESCRIPTION)
	public void setp(@Descriptor(ConsoleMsg.CONSOLE_HELP_SETPROP_COMMAND_ARGUMENTS_DESCRIPTION) String[] arguments) throws Exception {
		setprop(arguments);
	}

	/**
	 * Handles the setprop command.  Sets the CDS property in the given argument.
	 *
	 * @param arguments key=value pairs for the new properties
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SETPROP_COMMAND_DESCRIPTION)
	public void setprop(@Descriptor(ConsoleMsg.CONSOLE_HELP_SETPROP_COMMAND_ARGUMENTS_DESCRIPTION) String[] arguments) throws Exception {
		if (arguments == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_PARAMETERS_SPECIFIED_TITLE);
			props();
		} else {
			ServiceReference<EnvironmentInfo> envInfoRef = context.getServiceReference(EnvironmentInfo.class);
			if (envInfoRef != null) {
				// EnvironmentInfo is used because FrameworkProperties cannot be directly accessed outside of the system bundle
				EnvironmentInfo envInfo = context.getService(envInfoRef);
				if (envInfo != null) {
					System.out.println(ConsoleMsg.CONSOLE_SETTING_PROPERTIES_TITLE);
					for(String argument : arguments) {
						int index = argument.indexOf("=");
						if(index > -1) {
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
	 * Prints the short version of the status.
	 * For the long version use "status".
	 *
	 * @param arguments
	 */
	@SuppressWarnings("deprecation")
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SS_COMMAND_DESCRIPTION)
	public void ss(@Descriptor(ConsoleMsg.CONSOLE_HELP_STATUS_ARGUMENT_DESCRIPTION) String... arguments) throws Exception {
		if (context.getBundle(0).getState() == Bundle.ACTIVE) {
			System.out.println(ConsoleMsg.CONSOLE_FRAMEWORK_IS_LAUNCHED_MESSAGE);
		} else {
			System.out.println(ConsoleMsg.CONSOLE_FRAMEWORK_IS_SHUTDOWN_MESSAGE);
		}
		System.out.println();
		
		String states = "";
		String[] bsnSegments = null;
		
		if(arguments != null && arguments.length > 0) {
			if(arguments[0].equals("-s")) {
				if (arguments.length > 1) {
					states = arguments[1];
					if(arguments.length > 2) {
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
				
				if (!match(b, bsnSegments, stateFilter))
					continue;
				String label = b.getSymbolicName();
				if (label == null || label.length() == 0)
					label = b.toString();
				else
					label = label + "_" + b.getVersion(); //$NON-NLS-1$
				System.out.println(b.getBundleId() + "\t" + getStateName(b) + label); //$NON-NLS-1$ 
				PackageAdmin packageAdmin = activator.getPackageAdmin();
				if ((packageAdmin.getBundleType(b) & PackageAdmin.BUNDLE_TYPE_FRAGMENT) != 0) {
					Bundle[] hosts = packageAdmin.getHosts(b);
					if (hosts != null)
						for (int j = 0; j < hosts.length; j++)
							System.out.println("\t            Master=" + hosts[j].getBundleId()); //$NON-NLS-1$
				} else {
					Bundle[] fragments = packageAdmin.getFragments(b);
					if (fragments != null) {
						System.out.print("\t            Fragments="); //$NON-NLS-1$
						for (int f = 0; f < fragments.length; f++) {
							Bundle fragment = fragments[f];
							System.out.print((f > 0 ? ", " : "") + fragment.getBundleId()); //$NON-NLS-1$ //$NON-NLS-2$
						}
						System.out.println();
					}
				}
			}
		}
	}

	private boolean match(Bundle toFilter, String[] searchedName, int searchedState) {
		if ((toFilter.getState() & searchedState) == 0) {
			return false;
		}
		if (searchedName != null && searchedName.length > 0 && toFilter.getSymbolicName() != null && toFilter.getSymbolicName().indexOf(searchedName[0]) == -1) {
			return false;
		}
		return true;
	}

	/**
	 * Handles the threads command abbreviation.  Invokes threads().
	 *
	 */
	@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_DESCRIPTION)
	public void t() throws Exception {
		threads();
	}

	/**
	 * Prints the information about the currently running threads
	 * in the embedded system.
	 *
	 */
	@Descriptor(ConsoleMsg.CONSOLE_THREADS_COMMAND_DESCRIPTION)
	public void threads() throws Exception {

		ThreadGroup[] threadGroups = getThreadGroups();
		Util.sortByString(threadGroups);

		ThreadGroup tg = getTopThreadGroup();
		Thread[] threads = new Thread[tg.activeCount()];
		int count = tg.enumerate(threads, true);
		Util.sortByString(threads);

		StringBuffer sb = new StringBuffer(120);
		System.out.println();
		System.out.println(ConsoleMsg.CONSOLE_THREADGROUP_TITLE);
		for (int i = 0; i < threadGroups.length; i++) {
			tg = threadGroups[i];
			int all = tg.activeCount(); //tg.allThreadsCount();
			int local = tg.enumerate(new Thread[all], false); //tg.threadsCount();
			ThreadGroup p = tg.getParent();
			String parent = (p == null) ? "-none-" : p.getName(); //$NON-NLS-1$
			sb.setLength(0);
			sb.append(Util.toString(simpleClassName(tg), 18)).append(" ").append(Util.toString(tg.getName(), 21)).append(" ").append(Util.toString(parent, 16)).append(Util.toString(Integer.valueOf(tg.getMaxPriority()), 3)).append(Util.toString(Integer.valueOf(local), 4)).append("/").append(Util.toString(String.valueOf(all), 6)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			System.out.println(sb.toString());
		}
		System.out.print(newline);
		System.out.println(ConsoleMsg.CONSOLE_THREADTYPE_TITLE);
		for (int j = 0; j < count; j++) {
			Thread t = threads[j];
			if (t != null) {
				sb.setLength(0);
				sb.append(Util.toString(simpleClassName(t), 18)).append(" ").append(Util.toString(t.getName(), 21)).append(" ").append(Util.toString(t.getThreadGroup().getName(), 16)).append(Util.toString(Integer.valueOf(t.getPriority()), 3)); //$NON-NLS-1$ //$NON-NLS-2$
				if (t.isDaemon())
					sb.append(" [daemon]"); //$NON-NLS-1$
				System.out.println(sb.toString());
			}
		}
	}

	/**
	 * Handles the sl (startlevel) command. 
	 *
	 * @param bundle bundle to display startlevel for; if no bundle is specified, the framework startlevel is displayed
	 */
	@SuppressWarnings("deprecation")
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SL_COMMAND_DESCRIPTION)
	public void sl(@Descriptor(ConsoleMsg.CONSOLE_HELP_SL_COMMAND_ARGUMENT_DESCRIPTION) Bundle... bundle) throws Exception {
		StartLevel startLevel = activator.getStartLevel();
		if (startLevel != null) {
			int value = 0;
			if (bundle == null || bundle.length == 0) { // must want framework startlevel
				value = startLevel.getStartLevel();
				System.out.println(NLS.bind(ConsoleMsg.STARTLEVEL_FRAMEWORK_ACTIVE_STARTLEVEL, String.valueOf(value)));
			} else { // must want bundle startlevel
				value = startLevel.getBundleStartLevel(bundle[0]);
				System.out.println(NLS.bind(ConsoleMsg.STARTLEVEL_BUNDLE_STARTLEVEL, Long.valueOf(bundle[0].getBundleId()), Integer.valueOf(value)));
			}
		}
	}

	/**
	 * Handles the setfwsl (set framework startlevel) command. 
	 *
	 * @param newSL new value for the framewrok start level
	 */
	@SuppressWarnings("deprecation")
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SETFWSL_COMMAND_DESCRIPTION)
	public void setfwsl(@Descriptor(ConsoleMsg.CONSOLE_HELP_SETFWSL_COMMAND_ARGUMENT_DESCRIPTION) int newSL) throws Exception {
		StartLevel startLevel = activator.getStartLevel();
		if (startLevel != null) {
			try {
				startLevel.setStartLevel(newSL);
				System.out.println(NLS.bind(ConsoleMsg.STARTLEVEL_FRAMEWORK_ACTIVE_STARTLEVEL, String.valueOf(newSL)));
			} catch (IllegalArgumentException e) {
				System.out.println(e.getMessage());
			}
		}
	}

	/**
	 * Handles the setbsl (set bundle startlevel) command. 
	 *
	 * @param newSL new value for bundle start level
	 * @param bundles bundles whose start value will be changed
	 */
	@SuppressWarnings("deprecation")
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SETBSL_COMMAND_DESCRIPTION)
	public void setbsl(
			@Descriptor(ConsoleMsg.CONSOLE_HELP_SETFWSL_COMMAND_ARGUMENT_DESCRIPTION)int newSL, 
			@Descriptor(ConsoleMsg.CONSOLE_HELP_SETBSL_COMMAND_ARGUMENT_DESCRIPTION) Bundle... bundles) throws Exception {
		StartLevel startLevel = activator.getStartLevel();
		if (startLevel != null) {
			if (bundles == null) {
				System.out.println(ConsoleMsg.STARTLEVEL_NO_STARTLEVEL_OR_BUNDLE_GIVEN);
				return;
			}
			for (Bundle bundle : bundles) {	
				try {
					startLevel.setBundleStartLevel(bundle, newSL);
					System.out.println(NLS.bind(ConsoleMsg.STARTLEVEL_BUNDLE_STARTLEVEL, Long.valueOf(bundle.getBundleId()), Integer.valueOf(newSL)));
				} catch (IllegalArgumentException e) {
					System.out.println(e.getMessage());
				}
			}
		}
	}

	/**
	 * Handles the setibsl (set initial bundle startlevel) command. 
	 *
	 * @param newInitialSL new value for initial start level
	 */
	@SuppressWarnings("deprecation")
	@Descriptor(ConsoleMsg.CONSOLE_HELP_SETIBSL_COMMAND_DESCRIPTION)
	public void setibsl(@Descriptor(ConsoleMsg.CONSOLE_HELP_SETFWSL_COMMAND_ARGUMENT_DESCRIPTION) int newInitialSL) throws Exception {
		StartLevel startLevel = activator.getStartLevel();
		if (startLevel != null) {
			try {
				startLevel.setInitialBundleStartLevel(newInitialSL);
				System.out.println(NLS.bind(ConsoleMsg.STARTLEVEL_INITIAL_BUNDLE_STARTLEVEL, String.valueOf(newInitialSL)));
			} catch (IllegalArgumentException e) {
				System.out.println(e.getMessage());
			}
		}
	}

	/**
	 * Lists required bundles having the specified symbolic name or all if no bsn is specified
	 * 
	 * @param symbolicName
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_REQUIRED_BUNDLES_COMMAND_DESCRIPTION)
	public void requiredBundles(@Descriptor(ConsoleMsg.CONSOLE_HELP_REQUIRED_BUNDLES_COMMAND_ARGUMENT_DESCRIPTION) String... symbolicName) {
		classSpaces(symbolicName);
	}

	/**
	 * Lists required bundles having the specified symbolic name or all if no bsn is specified
	 * 
	 * @param symbolicName
	 */
	@SuppressWarnings("deprecation")
	@Descriptor(ConsoleMsg.CONSOLE_HELP_REQUIRED_BUNDLES_COMMAND_DESCRIPTION)
	public void classSpaces(@Descriptor(ConsoleMsg.CONSOLE_HELP_REQUIRED_BUNDLES_COMMAND_ARGUMENT_DESCRIPTION) String... symbolicName) {
		PackageAdmin packageAdmin = activator.getPackageAdmin();
		if (packageAdmin != null) {
			RequiredBundle[] symBundles = null;
			String name;
			if(symbolicName == null || symbolicName.length == 0) {
				name = null;
			} else {
				name = symbolicName[0];
			}
			symBundles = packageAdmin.getRequiredBundles(name);

			if (symBundles == null) {
				System.out.println(ConsoleMsg.CONSOLE_NO_NAMED_CLASS_SPACES_MESSAGE);
			} else {
				for (RequiredBundle symBundle : symBundles) {

					System.out.print(symBundle);

					boolean removalPending = symBundle.isRemovalPending();
					if (removalPending) {
						System.out.print("("); //$NON-NLS-1$
						System.out.print(ConsoleMsg.CONSOLE_REMOVAL_PENDING_MESSAGE);
						System.out.println(")"); //$NON-NLS-1$
					}

					Bundle provider = symBundle.getBundle();
					if (provider != null) {
						System.out.print("<"); //$NON-NLS-1$
						System.out.print(provider);
						System.out.println(">"); //$NON-NLS-1$

						Bundle[] requiring = symBundle.getRequiringBundles();
						if (requiring != null)
							for (int j = 0; j < requiring.length; j++) {
								System.out.print("  "); //$NON-NLS-1$
								System.out.print(requiring[j]);
								System.out.print(" "); //$NON-NLS-1$
								System.out.println(ConsoleMsg.CONSOLE_REQUIRES_MESSAGE);
							}
					} else {
						System.out.print("<"); //$NON-NLS-1$
						System.out.print(ConsoleMsg.CONSOLE_STALE_MESSAGE);
						System.out.println(">"); //$NON-NLS-1$
					}

				}
			}
		} else {
			System.out.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_NO_PACKAGE_ADMIN_MESSAGE);
		}
	}

	/**
	 * Handles the profilelog command. 
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_PROFILELOG_COMMAND_DESCRIPTION)
	public void profilelog() throws Exception {
		Class<?> profileClass = BundleContext.class.getClassLoader().loadClass("org.eclipse.osgi.internal.profile.Profile");
		Method getProfileLog = profileClass.getMethod("getProfileLog", (Class<?>[]) null);
		System.out.println(getProfileLog.invoke(null, (Object[]) null));
	}

	/**
	 * Lists all packages visible from the specified bundle
	 * @param bundle bundle to list visible packages
	 */
	@Descriptor(ConsoleMsg.CONSOLE_HELP_VISIBLE_PACKAGES_COMMAND_DESCRIPTION)
	public void getPackages(@Descriptor(ConsoleMsg.CONSOLE_HELP_VISIBLE_PACKAGES_COMMAND_ARGUMENTS_DESCRIPTION) Bundle bundle) {
		PlatformAdmin platformAdmin = activator.getPlatformAdmin();
		if (platformAdmin == null)
			return;
			BundleDescription bundleDescription = platformAdmin.getState(false).getBundle(bundle.getBundleId());
			ExportPackageDescription[] exports = platformAdmin.getStateHelper().getVisiblePackages(bundleDescription, StateHelper.VISIBLE_INCLUDE_EE_PACKAGES | StateHelper.VISIBLE_INCLUDE_ALL_HOST_WIRES);
			for (int i = 0; i < exports.length; i++) {
				System.out.println(exports[i] + ": " + platformAdmin.getStateHelper().getAccessCode(bundleDescription, exports[i])); //$NON-NLS-1$
			}
	}

	/**
	 *  Given a string containing a startlevel value, validate it and convert it to an int
	 * 
	 *  @param intp A CommandInterpreter object used for printing out error messages
	 *  @param value A string containing a potential startlevel
	 *  @return The start level or an int <0 if it was invalid
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
	 *  Given a bundle, return the string describing that bundle's state.
	 *
	 *  @param bundle A bundle to return the state of
	 *  @return A String describing the state
	 */
	protected String getStateName(Bundle bundle) {
		int state = bundle.getState();
		switch (state) {
			case Bundle.UNINSTALLED :
				return "UNINSTALLED "; //$NON-NLS-1$

			case Bundle.INSTALLED :
				if (isDisabled(bundle)) {
					return "<DISABLED>  "; //$NON-NLS-1$	
				}
				return "INSTALLED   "; //$NON-NLS-1$

			case Bundle.RESOLVED :
				return "RESOLVED    "; //$NON-NLS-1$

			case Bundle.STARTING :
				synchronized (lazyActivation) {
					if (lazyActivation.contains(bundle)) {
						return "<<LAZY>>    "; //$NON-NLS-1$
					}
					return "STARTING    "; //$NON-NLS-1$
				}

			case Bundle.STOPPING :
				return "STOPPING    "; //$NON-NLS-1$

			case Bundle.ACTIVE :
				return "ACTIVE      "; //$NON-NLS-1$

			default :
				return Integer.toHexString(state);
		}
	}

	private boolean isDisabled(Bundle bundle) {
		boolean disabled = false;
		ServiceReference<?> platformAdminRef = null;
		try {
			platformAdminRef = context.getServiceReference(PlatformAdmin.class.getName());
			if (platformAdminRef != null) {
				PlatformAdmin platAdmin = (PlatformAdmin) context.getService(platformAdminRef);
				if (platAdmin != null) {
					State state = platAdmin.getState(false);
					BundleDescription bundleDesc = state.getBundle(bundle.getBundleId());
					DisabledInfo[] disabledInfos = state.getDisabledInfos(bundleDesc);
					if ((disabledInfos != null) && (disabledInfos.length != 0)) {
						disabled = true;
					}
				}
			}
		} finally {
			if (platformAdminRef != null)
				context.ungetService(platformAdminRef);
		}
		return disabled;
	}

	/**
	 * Answers all thread groups in the system.
	 *
	 * @return	An array of all thread groups.
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
	 * It is the 'system' or 'main' thread group under
	 * which all 'user' thread groups are allocated.
	 *
	 * @return	The parent of all user thread groups.
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
	 * @return	The simple class name.
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
	public void getprop(@Descriptor(ConsoleMsg.CONSOLE_HELP_GETPROP_COMMAND_ARGUMENT_DESCRIPTION) String... propName) throws Exception {
		Properties allProperties = System.getProperties();
		Iterator<?> propertyNames = new TreeSet<Object>(allProperties.keySet()).iterator();
		while (propertyNames.hasNext()) {
			String prop = (String) propertyNames.next();
			if (propName == null || propName.length == 0 || prop.startsWith(propName[0])) {
				System.out.println(prop + '=' + allProperties.getProperty(prop));
			}
		}
	}
	
	@Descriptor(ConsoleMsg.CONSOLE_HELP_DIAG_COMMAND_DESCRIPTION)
	public void diag(@Descriptor(ConsoleMsg.CONSOLE_HELP_DIAG_COMMAND_ARGUMENT_DESCRIPTION) long[] bundleIds) throws Exception {
		if (bundleIds.length == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}
		
		PlatformAdmin platformAdmin = activator.getPlatformAdmin();
		if (platformAdmin == null) {
			System.out.println(ConsoleMsg.CONSOLE_NO_CONSTRAINTS_NO_PLATFORM_ADMIN_MESSAGE);
			return;
		}

		State systemState = platformAdmin.getState(false);
		for (long bundleId : bundleIds) {
			BundleDescription bundle = systemState.getBundle(bundleId);
			if (bundle == null) {
				System.out.println(NLS.bind(ConsoleMsg.CONSOLE_CANNOT_FIND_BUNDLE_ERROR, bundleId));
				continue;
			}
			System.out.println(bundle.getLocation() + " [" + bundle.getBundleId() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
			VersionConstraint[] unsatisfied = platformAdmin.getStateHelper().getUnsatisfiedConstraints(bundle);
			ResolverError[] resolverErrors = platformAdmin.getState(false).getResolverErrors(bundle);
			for (int i = 0; i < resolverErrors.length; i++) {
				if ((resolverErrors[i].getType() & (ResolverError.MISSING_FRAGMENT_HOST | ResolverError.MISSING_GENERIC_CAPABILITY | ResolverError.MISSING_IMPORT_PACKAGE | ResolverError.MISSING_REQUIRE_BUNDLE)) != 0)
					continue;
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(resolverErrors[i].toString());
			}

			if (unsatisfied.length == 0 && resolverErrors.length == 0) {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_NO_CONSTRAINTS);
			}
			if (unsatisfied.length > 0) {
				System.out.print("  "); //$NON-NLS-1$
				System.out.println(ConsoleMsg.CONSOLE_DIRECT_CONSTRAINTS);
			}
			for (int i = 0; i < unsatisfied.length; i++) {
				System.out.print("    "); //$NON-NLS-1$
				System.out.println(getResolutionFailureMessage(unsatisfied[i]));
			}
			VersionConstraint[] unsatisfiedLeaves = platformAdmin.getStateHelper().getUnsatisfiedLeaves(new BundleDescription[] {bundle});
			boolean foundLeaf = false;
			for (int i = 0; i < unsatisfiedLeaves.length; i++) {
				if (unsatisfiedLeaves[i].getBundle() == bundle)
					continue;
				if (!foundLeaf) {
					foundLeaf = true;
					System.out.print("  "); //$NON-NLS-1$
					System.out.println(ConsoleMsg.CONSOLE_LEAF_CONSTRAINTS);
				}
				System.out.print("    "); //$NON-NLS-1$
				System.out.println(unsatisfiedLeaves[i].getBundle().getLocation() + " [" + unsatisfiedLeaves[i].getBundle().getBundleId() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				System.out.print("      "); //$NON-NLS-1$
				System.out.println(getResolutionFailureMessage(unsatisfiedLeaves[i]));
			}
		}
	}
	
	@Descriptor(ConsoleMsg.CONSOLE_HELP_ENABLE_COMMAND_DESCRIPTION)
	public void enableBundle(@Descriptor(ConsoleMsg.CONSOLE_HELP_ENABLE_COMMAND_ARGUMENT_DESCRIPTION) long[] bundleIds) throws Exception {
		if (bundleIds.length == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}
		
		PlatformAdmin platformAdmin = activator.getPlatformAdmin();
		if (platformAdmin == null) {
			System.out.println(ConsoleMsg.CONSOLE_CANNOT_ENABLE_NO_PLATFORM_ADMIN_MESSAGE);
			return;
		}


		State systemState = platformAdmin.getState(false);
		for (long bundleId : bundleIds) {
			BundleDescription bundle = systemState.getBundle(bundleId);
			if (bundle == null) {
				System.out.println(NLS.bind(ConsoleMsg.CONSOLE_CANNOT_FIND_BUNDLE_ERROR, bundleId));
				continue;
			}

			DisabledInfo[] infos = systemState.getDisabledInfos(bundle);
			for (int i = 0; i < infos.length; i++) {
				platformAdmin.removeDisabledInfo(infos[i]);
			}
		}

	}
	
	@Descriptor(ConsoleMsg.CONSOLE_HELP_DISABLE_COMMAND_DESCRIPTION)
	public void disableBundle(@Descriptor(ConsoleMsg.CONSOLE_HELP_DISABLE_COMMAND_ARGUMENT_DESCRIPTION) long[] bundleIds) throws Exception {
		if (bundleIds.length == 0) {
			System.out.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
			return;
		}
		
		PlatformAdmin platformAdmin = activator.getPlatformAdmin();
		if (platformAdmin == null) {
			System.out.println(ConsoleMsg.CONSOLE_CANNOT_DISABLE_NO_PLATFORM_ADMIN_MESSAGE);
			return;
		}


		State systemState = platformAdmin.getState(false);
		for (long bundleId : bundleIds) {
			BundleDescription bundle = systemState.getBundle(bundleId);
			if (bundle == null) {
				System.out.println(NLS.bind(ConsoleMsg.CONSOLE_CANNOT_FIND_BUNDLE_ERROR, bundleId));
				continue;
			}
				DisabledInfo info = new DisabledInfo(POLICY_CONSOLE, ConsoleMsg.CONSOLE_CONSOLE_BUNDLE_DISABLED_MESSAGE, bundle);
				platformAdmin.addDisabledInfo(info);
			}
	}
	
	@Descriptor(ConsoleMsg.CONSOLE_HELP_LD_COMMAND_DESCRIPTION)
	public void disabledBundles() throws Exception {
		
		PlatformAdmin platformAdmin = activator.getPlatformAdmin();
		if (platformAdmin == null) {
			System.out.println(ConsoleMsg.CONSOLE_CANNOT_LIST_DISABLED_NO_PLATFORM_ADMIN_MESSAGE);
			return;
		}

		State systemState = platformAdmin.getState(false);
		BundleDescription[] disabledBundles = systemState.getDisabledBundles();

		System.out.println(NLS.bind(ConsoleMsg.CONSOLE_DISABLED_COUNT_MESSAGE, String.valueOf(disabledBundles.length)));

		if (disabledBundles.length > 0) {
			System.out.println();
		}
		for (int i = 0; i < disabledBundles.length; i++) {
			DisabledInfo[] disabledInfos = systemState.getDisabledInfos(disabledBundles[i]);

			System.out.println(NLS.bind(ConsoleMsg.CONSOLE_DISABLED_BUNDLE_HEADER, formatBundleName(disabledBundles[i]), String.valueOf(disabledBundles[i].getBundleId())));
			System.out.print(NLS.bind(ConsoleMsg.CONSOLE_DISABLED_BUNDLE_REASON, disabledInfos[0].getMessage(), disabledInfos[0].getPolicyName()));

			for (int j = 1; j < disabledInfos.length; j++) {
				System.out.print(NLS.bind(ConsoleMsg.CONSOLE_DISABLED_BUNDLE_REASON, disabledInfos[j].getMessage(), String.valueOf(disabledInfos[j].getPolicyName())));
			}

			System.out.println();
		}
	}
	
	private String formatBundleName(BundleDescription b) {
		String label = b.getSymbolicName();
		if (label == null || label.length() == 0)
			label = b.toString();
		else
			label = label + "_" + b.getVersion(); //$NON-NLS-1$

		return label;
	}
	
	private String getResolutionFailureMessage(VersionConstraint unsatisfied) {
		if (unsatisfied.isResolved())
			throw new IllegalArgumentException();
		if (unsatisfied instanceof ImportPackageSpecification) {
			if (ImportPackageSpecification.RESOLUTION_OPTIONAL.equals(((ImportPackageSpecification) unsatisfied).getDirective(Constants.RESOLUTION_DIRECTIVE)))
				return NLS.bind(ConsoleMsg.CONSOLE_MISSING_OPTIONAL_IMPORTED_PACKAGE, versionToString(unsatisfied));
			if (ImportPackageSpecification.RESOLUTION_DYNAMIC.equals(((ImportPackageSpecification) unsatisfied).getDirective(Constants.RESOLUTION_DIRECTIVE)))
				return NLS.bind(ConsoleMsg.CONSOLE_MISSING_DYNAMIC_IMPORTED_PACKAGE, versionToString(unsatisfied));
			return NLS.bind(ConsoleMsg.CONSOLE_MISSING_IMPORTED_PACKAGE, versionToString(unsatisfied));
		} else if (unsatisfied instanceof BundleSpecification) {
			if (((BundleSpecification) unsatisfied).isOptional())
				return NLS.bind(ConsoleMsg.CONSOLE_MISSING_OPTIONAL_REQUIRED_BUNDLE, versionToString(unsatisfied));
			return NLS.bind(ConsoleMsg.CONSOLE_MISSING_REQUIRED_BUNDLE, versionToString(unsatisfied));
		} else if (unsatisfied instanceof HostSpecification) {
			return NLS.bind(ConsoleMsg.CONSOLE_MISSING_HOST, versionToString(unsatisfied));
		} else if (unsatisfied instanceof NativeCodeSpecification) {
			return NLS.bind(ConsoleMsg.CONSOLE_MISSING_NATIVECODE, unsatisfied.toString());
		} else if (unsatisfied instanceof GenericSpecification) {
			return NLS.bind(ConsoleMsg.CONSOLE_MISSING_REQUIRED_CAPABILITY, unsatisfied.toString());
		}
		return NLS.bind(ConsoleMsg.CONSOLE_MISSING_REQUIREMENT, unsatisfied.toString());
	}
	
	private static String versionToString(VersionConstraint constraint) {
		org.eclipse.osgi.service.resolver.VersionRange versionRange = constraint.getVersionRange();
		if (versionRange == null)
			return constraint.getName();
		return constraint.getName() + '_' + versionRange;
	}

	/**
	 * This is used to track lazily activated bundles.
	 */
	public void bundleChanged(BundleEvent event) {
		int type = event.getType();
		Bundle bundle = event.getBundle();
		synchronized (lazyActivation) {
			switch (type) {
				case BundleEvent.LAZY_ACTIVATION :
					if (!lazyActivation.contains(bundle)) {
						lazyActivation.add(bundle);
					}
					break;

				default :
					lazyActivation.remove(bundle);
					break;
			}
		}

	}
	
	private boolean confirmStop() {
		System.out.print(ConsoleMsg.CONSOLE_STOP_MESSAGE);
		System.out.flush();
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		String reply = null;
		try {
			reply = reader.readLine();
		} catch (IOException e) {
			System.out.println(ConsoleMsg.CONSOLE_STOP_ERROR_READ_CONFIRMATION);
		}
		
		if (reply != null) {
			if (reply.toLowerCase().startsWith(ConsoleMsg.CONSOLE_STOP_CONFIRMATION_YES) || reply.length() == 0) {
				return true;
			}
		}
		
		return false;
	}
}

