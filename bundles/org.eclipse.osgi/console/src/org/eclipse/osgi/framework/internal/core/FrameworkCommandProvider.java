/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.internal.core;

import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.internal.permadmin.SecurityAdmin;
import org.eclipse.osgi.internal.profile.Profile;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.*;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;
import org.osgi.service.condpermadmin.ConditionalPermissionUpdate;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.packageadmin.RequiredBundle;
import org.osgi.service.startlevel.StartLevel;

/**
 * This class provides methods to execute commands from the command line.  It registers
 * itself as a CommandProvider so it can be invoked by a CommandInterpreter.  The
 * FrameworkCommandProvider registers itself with the highest ranking (Integer.MAXVALUE) so it will always be
 * called first.  Other CommandProviders should register with lower rankings.
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
 services {filter} - display registered service details. Examples for [filter]: (objectClass=com.xyz.Person); (&(objectClass=com.xyz.Person)(sn=Jensen)); passing only com.xyz.Person is a shortcut for (objectClass=com.xyz.Person). The filter syntax specification is available at http://www.ietf.org/rfc/rfc1960.txt
 ss - display installed bundles (short status)
 status - display installed bundles and registered services
 threads - display threads and thread groups
 ---Extras---
 exec <command> - execute a command in a separate process and wait
 fork <command> - execute a command in a separate process
 getprop <name> -  Displays the system properties with the given name, or all of them.
 ---Controlling StartLevel---
 sl {(<id>|<location>)} - display the start level for the specified bundle, or for the framework if no bundle specified
 setfwsl <start level> - set the framework start level
 setbsl <start level> (<id>|<location>) - set the start level for the bundle(s)
 setibsl <start level> - set the initial bundle start level
 ---Getting Help---
 help <command> - Display help for the specified command
 
 *
 *  There is a method for each command which is named '_'+method.  The methods are
 *  invoked by a CommandInterpreter's execute method.
 */
public class FrameworkCommandProvider implements CommandProvider, SynchronousBundleListener {

	/** An instance of the OSGi framework */
	private final Framework framework;
	/** The system bundle context */
	private final BundleContext context;
	/** The start level implementation */
	private final StartLevelManager slImpl;
	private final SecurityAdmin securityAdmin;
	private ServiceRegistration<?> providerReg;

	/** Strings used to format other strings */
	private final static String tab = "\t"; //$NON-NLS-1$
	private final static String newline = "\r\n"; //$NON-NLS-1$

	/** this list contains the bundles known to be lazily awaiting activation */
	private final List<Bundle> lazyActivation = new ArrayList<Bundle>();

	/** this map contains the mapping between the command name and its description and eventually arguments.*/
	private Map<String, String[]> commandsHelp = null;
	/** this map contains the mapping between the command groups and the names of the commands in each group*/
	private Map<String, String[]> commandGroups = null;

	/**
	 *  Constructor.
	 *
	 *  initialize must be called after creating this object.
	 *
	 *  @param framework The current instance of the framework
	 */
	public FrameworkCommandProvider(Framework framework) {
		this.framework = framework;
		context = framework.systemBundle.getContext();
		slImpl = framework.startLevelManager;
		securityAdmin = framework.securityAdmin;
	}

	/**
	 *  Starts this CommandProvider.
	 *
	 *  Registers this object as a CommandProvider with the highest ranking possible.
	 *  Adds this object as a SynchronousBundleListener.
	 */
	void start() {
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put(Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
		providerReg = context.registerService(CommandProvider.class.getName(), this, props);
		context.addBundleListener(this);
	}

	void stop() {
		context.removeBundleListener(this);
		if (providerReg != null)
			providerReg.unregister();
	}

	/**
	 Answer a string (may be as many lines as you like) with help
	 texts that explain the command.  This getHelp() method uses the 
	 ConsoleMsg class to obtain the correct NLS data to display to the user.
	 
	 @return The help string
	 */
	public String getHelp() {
		return getHelp(null);
	}

	/* This method either returns the help message for a particular command, 
	 * or returns the help messages for all commands (if commandName is not specified)*/
	private String getHelp(String commandName) {
		StringBuffer help = new StringBuffer(1024);

		if (commandsHelp == null) {
			initializeCommandsHelp();
		}

		if (commandGroups == null) {
			initializeCommandGroups();
		}

		if (commandName != null) {
			if (commandsHelp.containsKey(commandName)) {
				addCommand(commandName, commandsHelp.get(commandName), help);
			}
			return help.toString();
		}

		for (Entry<String, String[]> groupEntry : commandGroups.entrySet()) {
			addHeader(groupEntry.getKey(), help);
			for (String command : groupEntry.getValue()) {
				addCommand(command, commandsHelp.get(command), help);
			}
		}

		return help.toString();
	}

	private void initializeCommandsHelp() {
		commandsHelp = new HashMap<String, String[]>();
		// add help for commands for controlling the framework
		commandsHelp.put("launch", new String[] {ConsoleMsg.CONSOLE_HELP_LAUNCH_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("shutdown", new String[] {ConsoleMsg.CONSOLE_HELP_SHUTDOWN_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("close", new String[] {ConsoleMsg.CONSOLE_HELP_CLOSE_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("exit", new String[] {ConsoleMsg.CONSOLE_HELP_EXIT_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("init", new String[] {ConsoleMsg.CONSOLE_HELP_INIT_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("setprop", new String[] {ConsoleMsg.CONSOLE_HELP_KEYVALUE_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_SETPROP_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("setp", new String[] {ConsoleMsg.CONSOLE_HELP_KEYVALUE_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_SETPROP_COMMAND_DESCRIPTION}); //$NON-NLS-1$

		// add help for commands for controlling bundles
		commandsHelp.put("install", new String[] {ConsoleMsg.CONSOLE_HELP_INSTALL_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("i", new String[] {ConsoleMsg.CONSOLE_HELP_INSTALL_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("uninstall", new String[] {ConsoleMsg.CONSOLE_HELP_UNINSTALL_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("un", new String[] {ConsoleMsg.CONSOLE_HELP_UNINSTALL_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("start", new String[] {ConsoleMsg.CONSOLE_HELP_START_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("sta", new String[] {ConsoleMsg.CONSOLE_HELP_START_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("stop", new String[] {ConsoleMsg.CONSOLE_HELP_STOP_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("sto", new String[] {ConsoleMsg.CONSOLE_HELP_STOP_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("refresh", new String[] {ConsoleMsg.CONSOLE_HELP_REFRESH_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("r", new String[] {ConsoleMsg.CONSOLE_HELP_REFRESH_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("update", new String[] {ConsoleMsg.CONSOLE_HELP_UPDATE_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("up", new String[] {ConsoleMsg.CONSOLE_HELP_UPDATE_COMMAND_DESCRIPTION}); //$NON-NLS-1$

		// add help for commands for displaying status
		commandsHelp.put("status", new String[] {ConsoleMsg.CONSOLE_HELP_STATE_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_STATUS_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("s", new String[] {ConsoleMsg.CONSOLE_HELP_STATE_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_STATUS_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("ss", new String[] {ConsoleMsg.CONSOLE_HELP_STATE_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_SS_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("services", new String[] {ConsoleMsg.CONSOLE_HELP_FILTER_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_SERVICES_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("packages", new String[] {ConsoleMsg.CONSOLE_HELP_PACKAGES_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_PACKAGES_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("p", new String[] {ConsoleMsg.CONSOLE_HELP_PACKAGES_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_PACKAGES_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("bundles", new String[] {ConsoleMsg.CONSOLE_HELP_STATE_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_BUNDLES_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("bundle", new String[] {ConsoleMsg.CONSOLE_HELP_IDLOCATION_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_BUNDLE_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("b", new String[] {ConsoleMsg.CONSOLE_HELP_IDLOCATION_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_BUNDLE_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("headers", new String[] {ConsoleMsg.CONSOLE_HELP_IDLOCATION_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_HEADERS_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("h", new String[] {ConsoleMsg.CONSOLE_HELP_IDLOCATION_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_HEADERS_COMMAND_DESCRIPTION}); //$NON-NLS-1$

		// add help for extra commands
		commandsHelp.put("exec", new String[] {ConsoleMsg.CONSOLE_HELP_COMMAND_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_EXEC_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("fork", new String[] {ConsoleMsg.CONSOLE_HELP_COMMAND_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_FORK_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("gc", new String[] {ConsoleMsg.CONSOLE_HELP_GC_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("getprop", new String[] {ConsoleMsg.CONSOLE_HELP_GETPROP_ARGUMENT_DESCRIPTION, ConsoleMsg.CONSOLE_HELP_GETPROP_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("props", new String[] {ConsoleMsg.CONSOLE_PROPS_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("pr", new String[] {ConsoleMsg.CONSOLE_PROPS_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("threads", new String[] {ConsoleMsg.CONSOLE_THREADS_COMMAND_DESCRIPTION}); //$NON-NLS-1$
		commandsHelp.put("t", new String[] {ConsoleMsg.CONSOLE_THREADS_COMMAND_DESCRIPTION}); //$NON-NLS-1$

		// add help for startlevel commands
		commandsHelp.put("sl", new String[] {ConsoleMsg.CONSOLE_HELP_OPTIONAL_IDLOCATION_ARGUMENT_DESCRIPTION, ConsoleMsg.STARTLEVEL_HELP_SL}); //$NON-NLS-1$
		commandsHelp.put("setfwsl", new String[] {ConsoleMsg.STARTLEVEL_ARGUMENT_DESCRIPTION, ConsoleMsg.STARTLEVEL_HELP_SETFWSL}); //$NON-NLS-1$
		commandsHelp.put("setbsl", new String[] {ConsoleMsg.STARTLEVEL_IDLOCATION_ARGUMENT_DESCRIPTION, ConsoleMsg.STARTLEVEL_HELP_SETBSL}); //$NON-NLS-1$
		commandsHelp.put("setibsl", new String[] {ConsoleMsg.STARTLEVEL_ARGUMENT_DESCRIPTION, ConsoleMsg.STARTLEVEL_HELP_SETIBSL}); //$NON-NLS-1$

		// add help for profilelog command
		commandsHelp.put("profilelog", new String[] {ConsoleMsg.CONSOLE_HELP_PROFILELOG_DESCRIPTION}); //$NON-NLS-1$
	}

	private void initializeCommandGroups() {
		commandGroups = new LinkedHashMap<String, String[]>();
		commandGroups.put(ConsoleMsg.CONSOLE_HELP_CONTROLLING_FRAMEWORK_HEADER, new String[] {"launch", "shutdown", "close", "exit", "init", "setprop"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		commandGroups.put(ConsoleMsg.CONSOLE_HELP_CONTROLLING_BUNDLES_HEADER, new String[] {"install", "uninstall", "start", "stop", "refresh", "update"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		commandGroups.put(ConsoleMsg.CONSOLE_HELP_DISPLAYING_STATUS_HEADER, new String[] {"status", "ss", "services", "packages", "bundles", "bundle", "headers"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		commandGroups.put(ConsoleMsg.CONSOLE_HELP_EXTRAS_HEADER, new String[] {"exec", "fork", "gc", "getprop", "props", "threads"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		commandGroups.put(ConsoleMsg.STARTLEVEL_HELP_HEADING, new String[] {"sl", "setfwsl", "setbsl", "setibsl"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		commandGroups.put(ConsoleMsg.CONSOLE_HELP_PROFILE_HEADING, new String[] {"profilelog"}); //$NON-NLS-1$
	}

	/** Private helper method for getHelp.  Formats the help headers. */
	private void addHeader(String header, StringBuffer help) {
		help.append("---"); //$NON-NLS-1$
		help.append(header);
		help.append("---"); //$NON-NLS-1$
		help.append(newline);
	}

	/** Private helper method for getHelp.  Formats the command descriptions. */
	private void addCommand(String command, String description, StringBuffer help) {
		help.append(tab);
		help.append(command);
		help.append(" - "); //$NON-NLS-1$
		help.append(description);
		help.append(newline);
	}

	/** Private helper method for getHelp.  Formats the command descriptions with command arguments. */
	private void addCommand(String command, String parameters, String description, StringBuffer help) {
		help.append(tab);
		help.append(command);
		help.append(" "); //$NON-NLS-1$
		help.append(parameters);
		help.append(" - "); //$NON-NLS-1$
		help.append(description);
		help.append(newline);
	}

	/** Private helper method for getHelp. According to its arguments chooses which one of the above addCommand methods to use. */
	private void addCommand(String command, String[] attributes, StringBuffer help) {
		if (attributes.length == 1) {
			addCommand(command, attributes[0], help);
		} else if (attributes.length == 2) {
			addCommand(command, attributes[0], attributes[1], help);
		}
	}

	/**
	 *  Handle the exit command.  Exit immediately (System.exit)
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _exit(CommandInterpreter intp) throws Exception {
		intp.println();
		System.exit(0);
	}

	/**
	 *  Handle the launch command.  Start the OSGi framework.
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _launch(CommandInterpreter intp) throws Exception {
		framework.launch();
	}

	/**
	 *  Handle the shutdown command.  Shutdown the OSGi framework.
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _shutdown(CommandInterpreter intp) throws Exception {
		framework.shutdown(FrameworkEvent.STOPPED);
	}

	/**
	 *  Handle the start command's abbreviation.  Invoke _start()
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _sta(CommandInterpreter intp) throws Exception {
		_start(intp);
	}

	/**
	 *  Handle the start command.  Start the specified bundle(s).
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _start(CommandInterpreter intp) throws Exception {
		String nextArg = intp.nextArgument();
		if (nextArg == null) {
			intp.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
		}
		while (nextArg != null) {
			AbstractBundle bundle = getBundleFromToken(intp, nextArg, true);
			if (bundle != null) {
				bundle.start();
			}
			nextArg = intp.nextArgument();
		}
	}

	/**
	 *  Handle the stop command's abbreviation.  Invoke _stop()
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _sto(CommandInterpreter intp) throws Exception {
		_stop(intp);
	}

	/**
	 *  Handle the stop command.  Stop the specified bundle(s).
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _stop(CommandInterpreter intp) throws Exception {
		String nextArg = intp.nextArgument();
		if (nextArg == null) {
			intp.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
		}
		while (nextArg != null) {
			AbstractBundle bundle = getBundleFromToken(intp, nextArg, true);
			if (bundle != null) {
				bundle.stop();
			}
			nextArg = intp.nextArgument();
		}
	}

	/**
	 *  Handle the install command's abbreviation.  Invoke _install()
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _i(CommandInterpreter intp) throws Exception {
		_install(intp);
	}

	/**
	 *  Handle the install command.  Install and optionally start bundle from the given URL\r\n"
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _install(CommandInterpreter intp) throws Exception {
		String url = intp.nextArgument();
		if (url == null) {
			intp.println(ConsoleMsg.CONSOLE_NOTHING_TO_INSTALL_ERROR);
		} else {
			AbstractBundle bundle = (AbstractBundle) context.installBundle(url);
			intp.print(ConsoleMsg.CONSOLE_BUNDLE_ID_MESSAGE);
			intp.println(new Long(bundle.getBundleId()));

			String nextArg = intp.nextArgument();
			if (nextArg != null) {
				String start = nextArg.toLowerCase();

				if (matchCommand("start", start, 1)) { //$NON-NLS-1$
					bundle.start();
				}
			}
		}

	}

	private static boolean matchCommand(String command, String input, int minLength) {
		if (minLength <= 0)
			minLength = command.length();
		int length = input.length();
		if (minLength > length)
			length = minLength;
		return (command.regionMatches(0, input, 0, length));
	}

	/**
	 *  Handle the update command's abbreviation.  Invoke _update()
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _up(CommandInterpreter intp) throws Exception {
		_update(intp);
	}

	/**
	 *  Handle the update command.  Update the specified bundle(s).
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _update(CommandInterpreter intp) throws Exception {
		String token = intp.nextArgument();
		if (token == null) {
			intp.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
		}
		while (token != null) {

			if ("*".equals(token)) { //$NON-NLS-1$
				AbstractBundle[] bundles = (AbstractBundle[]) context.getBundles();

				int size = bundles.length;

				if (size > 0) {
					for (int i = 0; i < size; i++) {
						AbstractBundle bundle = bundles[i];

						if (bundle.getBundleId() != 0) {
							try {
								bundle.update();
							} catch (BundleException e) {
								intp.printStackTrace(e);
							}
						}
					}
				} else {
					intp.println(ConsoleMsg.CONSOLE_NO_INSTALLED_BUNDLES_ERROR);
				}
			} else {
				AbstractBundle bundle = getBundleFromToken(intp, token, true);
				if (bundle != null) {
					String source = intp.nextArgument();
					try {
						if (source != null) {
							bundle.update(new URL(source).openStream());
						} else {
							bundle.update();
						}
					} catch (BundleException e) {
						intp.printStackTrace(e);
					}
				}
			}
			token = intp.nextArgument();
		}
	}

	/**
	 *  Handle the uninstall command's abbreviation.  Invoke _uninstall()
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _un(CommandInterpreter intp) throws Exception {
		_uninstall(intp);
	}

	/**
	 *  Handle the uninstall command.  Uninstall the specified bundle(s).
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _uninstall(CommandInterpreter intp) throws Exception {
		String nextArg = intp.nextArgument();
		if (nextArg == null) {
			intp.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
		}
		while (nextArg != null) {
			AbstractBundle bundle = getBundleFromToken(intp, nextArg, true);
			if (bundle != null) {
				bundle.uninstall();
			}
			nextArg = intp.nextArgument();
		}
	}

	/**
	 *  Handle the status command's abbreviation.  Invoke _status()
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _s(CommandInterpreter intp) throws Exception {
		_status(intp);
	}

	private Object[] processOption(CommandInterpreter intp) {
		String option = intp.nextArgument();
		String filteredName = null;
		int stateFilter = -1;
		if (option != null && option.equals("-s")) { //$NON-NLS-1$
			String searchedState = intp.nextArgument();
			if (searchedState == null)
				searchedState = ""; //$NON-NLS-1$
			StringTokenizer tokens = new StringTokenizer(searchedState, ","); //$NON-NLS-1$
			while (tokens.hasMoreElements()) {
				String desiredState = (String) tokens.nextElement();
				Field match = null;
				try {
					match = Bundle.class.getField(desiredState.toUpperCase());
					if (stateFilter == -1)
						stateFilter = 0;
					stateFilter |= match.getInt(match);
				} catch (NoSuchFieldException e) {
					intp.println(ConsoleMsg.CONSOLE_INVALID_INPUT + ": " + desiredState); //$NON-NLS-1$
					return null;
				} catch (IllegalAccessException e) {
					intp.println(ConsoleMsg.CONSOLE_INVALID_INPUT + ": " + desiredState); //$NON-NLS-1$
					return null;
				}
			}
		} else {
			filteredName = option;
		}
		String tmp = intp.nextArgument();
		if (tmp != null)
			filteredName = tmp;
		return new Object[] {filteredName, new Integer(stateFilter)};
	}

	/**
	 *  Handle the status command.  Display installed bundles and registered services.
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _status(CommandInterpreter intp) throws Exception {
		if (framework.isActive()) {
			intp.println(ConsoleMsg.CONSOLE_FRAMEWORK_IS_LAUNCHED_MESSAGE);
		} else {
			intp.println(ConsoleMsg.CONSOLE_FRAMEWORK_IS_SHUTDOWN_MESSAGE);
		}
		intp.println();

		Object[] options = processOption(intp);
		if (options == null)
			return;

		AbstractBundle[] bundles = (AbstractBundle[]) context.getBundles();
		int size = bundles.length;

		if (size == 0) {
			intp.println(ConsoleMsg.CONSOLE_NO_INSTALLED_BUNDLES_ERROR);
			return;
		}
		intp.print(ConsoleMsg.CONSOLE_ID);
		intp.print(tab);
		intp.println(ConsoleMsg.CONSOLE_BUNDLE_LOCATION_MESSAGE);
		intp.println(ConsoleMsg.CONSOLE_STATE_BUNDLE_FILE_NAME_HEADER);
		for (int i = 0; i < size; i++) {
			AbstractBundle bundle = bundles[i];
			if (!match(bundle, (String) options[0], ((Integer) options[1]).intValue()))
				continue;
			intp.print(new Long(bundle.getBundleId()));
			intp.print(tab);
			intp.println(bundle.getLocation());
			intp.print("  "); //$NON-NLS-1$
			intp.print(getStateName(bundle));
			intp.println(bundle.bundledata);
		}

		ServiceReference<?>[] services = context.getServiceReferences((String) null, (String) null);
		if (services != null) {
			intp.println(ConsoleMsg.CONSOLE_REGISTERED_SERVICES_MESSAGE);
			size = services.length;
			for (int i = 0; i < size; i++) {
				intp.println(services[i]);
			}
		}
	}

	/**
	 *  Handle the services command's abbreviation.  Invoke _services()
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _se(CommandInterpreter intp) throws Exception {
		_services(intp);
	}

	/**
	 *  Handle the services command.  Display registered service details.
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _services(CommandInterpreter intp) throws Exception {
		String filter = null;

		String nextArg = intp.nextArgument();
		if (nextArg != null) {
			StringBuffer buf = new StringBuffer();
			while (nextArg != null) {
				buf.append(' ');
				buf.append(nextArg);
				nextArg = intp.nextArgument();
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
		if (originalException != null && !filter.startsWith("(") && filter.indexOf(' ') < 0) { //$NON-NLS-1$
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
					intp.println(service);
					intp.print("  "); //$NON-NLS-1$
					intp.print(ConsoleMsg.CONSOLE_REGISTERED_BY_BUNDLE_MESSAGE);
					intp.print(" "); //$NON-NLS-1$
					intp.println(service.getBundle());
					Bundle[] users = service.getUsingBundles();
					if (users != null) {
						intp.print("  "); //$NON-NLS-1$
						intp.println(ConsoleMsg.CONSOLE_BUNDLES_USING_SERVICE_MESSAGE);
						for (int k = 0; k < users.length; k++) {
							intp.print("    "); //$NON-NLS-1$
							intp.println(users[k]);
						}
					} else {
						intp.print("  "); //$NON-NLS-1$
						intp.println(ConsoleMsg.CONSOLE_NO_BUNDLES_USING_SERVICE_MESSAGE);
					}
				}
				return;
			}
		}
		intp.println(ConsoleMsg.CONSOLE_NO_REGISTERED_SERVICES_MESSAGE);
	}

	/**
	 *  Handle the packages command's abbreviation.  Invoke _packages()
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _p(CommandInterpreter intp) throws Exception {
		_packages(intp);
	}

	/**
	 *  Handle the packages command.  Display imported/exported package details.
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _packages(CommandInterpreter intp) throws Exception {
		org.osgi.framework.Bundle bundle = null;

		String token = intp.nextArgument();
		if (token != null) {
			bundle = getBundleFromToken(intp, token, false);
		}

		ServiceReference<?> packageAdminRef = context.getServiceReference("org.osgi.service.packageadmin.PackageAdmin"); //$NON-NLS-1$
		if (packageAdminRef != null) {
			PackageAdmin packageAdmin = (PackageAdmin) context.getService(packageAdminRef);
			if (packageAdmin != null) {
				try {
					org.osgi.service.packageadmin.ExportedPackage[] packages = null;

					if (token != null)
						packages = packageAdmin.getExportedPackages(token);
					if (packages == null)
						packages = packageAdmin.getExportedPackages(bundle);

					if (packages == null) {
						intp.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_MESSAGE);
					} else {
						for (int i = 0; i < packages.length; i++) {
							org.osgi.service.packageadmin.ExportedPackage pkg = packages[i];
							intp.print(pkg);

							boolean removalPending = pkg.isRemovalPending();
							if (removalPending) {
								intp.print("("); //$NON-NLS-1$
								intp.print(ConsoleMsg.CONSOLE_REMOVAL_PENDING_MESSAGE);
								intp.println(")"); //$NON-NLS-1$
							}

							org.osgi.framework.Bundle exporter = pkg.getExportingBundle();
							if (exporter != null) {
								intp.print("<"); //$NON-NLS-1$
								intp.print(exporter);
								intp.println(">"); //$NON-NLS-1$

								org.osgi.framework.Bundle[] importers = pkg.getImportingBundles();
								for (int j = 0; j < importers.length; j++) {
									intp.print("  "); //$NON-NLS-1$
									intp.print(importers[j]);
									intp.print(" "); //$NON-NLS-1$
									intp.println(ConsoleMsg.CONSOLE_IMPORTS_MESSAGE);
								}
							} else {
								intp.print("<"); //$NON-NLS-1$
								intp.print(ConsoleMsg.CONSOLE_STALE_MESSAGE);
								intp.println(">"); //$NON-NLS-1$
							}

						}
					}
				} finally {
					context.ungetService(packageAdminRef);
				}
			}
		} else {
			intp.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_NO_PACKAGE_ADMIN_MESSAGE);
		}
	}

	/**
	 *  Handle the bundles command.  Display details for all installed bundles.
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _bundles(CommandInterpreter intp) throws Exception {
		Object[] options = processOption(intp);
		if (options == null)
			return;

		AbstractBundle[] bundles = (AbstractBundle[]) context.getBundles();
		int size = bundles.length;

		if (size == 0) {
			intp.println(ConsoleMsg.CONSOLE_NO_INSTALLED_BUNDLES_ERROR);
			return;
		}

		for (int i = 0; i < size; i++) {
			AbstractBundle bundle = bundles[i];
			if (!match(bundle, (String) options[0], ((Integer) options[1]).intValue()))
				continue;
			long id = bundle.getBundleId();
			intp.println(bundle);
			intp.print("  "); //$NON-NLS-1$
			intp.print(NLS.bind(ConsoleMsg.CONSOLE_ID_MESSAGE, String.valueOf(id)));
			intp.print(", "); //$NON-NLS-1$
			intp.print(NLS.bind(ConsoleMsg.CONSOLE_STATUS_MESSAGE, getStateName(bundle)));
			if (id != 0) {
				File dataRoot = framework.getDataFile(bundle, ""); //$NON-NLS-1$

				String root = (dataRoot == null) ? null : dataRoot.getAbsolutePath();

				intp.print(NLS.bind(ConsoleMsg.CONSOLE_DATA_ROOT_MESSAGE, root));
			} else {
				intp.println();
			}

			ServiceReference<?>[] services = bundle.getRegisteredServices();
			if (services != null) {
				intp.print("  "); //$NON-NLS-1$
				intp.println(ConsoleMsg.CONSOLE_REGISTERED_SERVICES_MESSAGE);
				for (int j = 0; j < services.length; j++) {
					intp.print("    "); //$NON-NLS-1$
					intp.println(services[j]);
				}
			} else {
				intp.print("  "); //$NON-NLS-1$
				intp.println(ConsoleMsg.CONSOLE_NO_REGISTERED_SERVICES_MESSAGE);
			}

			services = bundle.getServicesInUse();
			if (services != null) {
				intp.print("  "); //$NON-NLS-1$
				intp.println(ConsoleMsg.CONSOLE_SERVICES_IN_USE_MESSAGE);
				for (int j = 0; j < services.length; j++) {
					intp.print("    "); //$NON-NLS-1$
					intp.println(services[j]);
				}
			} else {
				intp.print("  "); //$NON-NLS-1$
				intp.println(ConsoleMsg.CONSOLE_NO_SERVICES_IN_USE_MESSAGE);
			}
		}
	}

	/**
	 *  Handle the bundle command's abbreviation.  Invoke _bundle()
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _b(CommandInterpreter intp) throws Exception {
		_bundle(intp);
	}

	/**
	 *  Handle the bundle command.  Display details for the specified bundle(s).
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _bundle(CommandInterpreter intp) throws Exception {
		String nextArg = intp.nextArgument();
		if (nextArg == null) {
			intp.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
		}
		while (nextArg != null) {
			AbstractBundle bundle = getBundleFromToken(intp, nextArg, true);
			if (bundle != null) {
				long id = bundle.getBundleId();
				intp.println(bundle);
				intp.print("  "); //$NON-NLS-1$
				intp.print(NLS.bind(ConsoleMsg.CONSOLE_ID_MESSAGE, String.valueOf(id)));
				intp.print(", "); //$NON-NLS-1$
				intp.print(NLS.bind(ConsoleMsg.CONSOLE_STATUS_MESSAGE, getStateName(bundle)));
				if (id != 0) {
					File dataRoot = framework.getDataFile(bundle, ""); //$NON-NLS-1$

					String root = (dataRoot == null) ? null : dataRoot.getAbsolutePath();

					intp.print(NLS.bind(ConsoleMsg.CONSOLE_DATA_ROOT_MESSAGE, root));
					intp.println();
				} else {
					intp.println();
				}

				ServiceReference<?>[] services = bundle.getRegisteredServices();
				if (services != null) {
					intp.print("  "); //$NON-NLS-1$
					intp.println(ConsoleMsg.CONSOLE_REGISTERED_SERVICES_MESSAGE);
					for (int j = 0; j < services.length; j++) {
						intp.print("    "); //$NON-NLS-1$
						intp.println(services[j]);
					}
				} else {
					intp.print("  "); //$NON-NLS-1$
					intp.println(ConsoleMsg.CONSOLE_NO_REGISTERED_SERVICES_MESSAGE);
				}

				services = bundle.getServicesInUse();
				if (services != null) {
					intp.print("  "); //$NON-NLS-1$
					intp.println(ConsoleMsg.CONSOLE_SERVICES_IN_USE_MESSAGE);
					for (int j = 0; j < services.length; j++) {
						intp.print("    "); //$NON-NLS-1$
						intp.println(services[j]);
					}
				} else {
					intp.print("  "); //$NON-NLS-1$
					intp.println(ConsoleMsg.CONSOLE_NO_SERVICES_IN_USE_MESSAGE);
				}

				ServiceReference<?> packageAdminRef = context.getServiceReference("org.osgi.service.packageadmin.PackageAdmin"); //$NON-NLS-1$
				if (packageAdminRef != null) {
					BundleDescription desc = bundle.getBundleDescription();
					if (desc != null) {
						boolean title = true;
						try {
							ExportPackageDescription[] exports = desc.getSelectedExports();
							if (exports == null || exports.length == 0) {
								intp.print("  "); //$NON-NLS-1$
								intp.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_MESSAGE);
							} else {
								title = true;

								for (int i = 0; i < exports.length; i++) {
									if (title) {
										intp.print("  "); //$NON-NLS-1$
										intp.println(ConsoleMsg.CONSOLE_EXPORTED_PACKAGES_MESSAGE);
										title = false;
									}
									intp.print("    "); //$NON-NLS-1$
									intp.print(exports[i].getName());
									intp.print("; version=\""); //$NON-NLS-1$
									intp.print(exports[i].getVersion());
									intp.print("\""); //$NON-NLS-1$
									if (desc.isRemovalPending()) {
										intp.println(ConsoleMsg.CONSOLE_EXPORTED_REMOVAL_PENDING_MESSAGE);
									} else {
										intp.println(ConsoleMsg.CONSOLE_EXPORTED_MESSAGE);
									}
								}

								if (title) {
									intp.print("  "); //$NON-NLS-1$
									intp.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_MESSAGE);
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

								title = printImportedPackages(imports, intp, title);

								if (desc.isResolved() && (unresolvedImports.isEmpty() == false)) {
									printUnwiredDynamicImports(unresolvedImports, intp);
									title = false;
								}
							}

							if (title) {
								intp.print("  "); //$NON-NLS-1$
								intp.println(ConsoleMsg.CONSOLE_NO_IMPORTED_PACKAGES_MESSAGE);
							}

							PackageAdmin packageAdmin = (PackageAdmin) context.getService(packageAdminRef);
							if (packageAdmin != null) {
								intp.print("  "); //$NON-NLS-1$
								if ((packageAdmin.getBundleType(bundle) & PackageAdmin.BUNDLE_TYPE_FRAGMENT) > 0) {
									org.osgi.framework.Bundle[] hosts = packageAdmin.getHosts(bundle);
									if (hosts != null) {
										intp.println(ConsoleMsg.CONSOLE_HOST_MESSAGE);
										for (int i = 0; i < hosts.length; i++) {
											intp.print("    "); //$NON-NLS-1$
											intp.println(hosts[i]);
										}
									} else {
										intp.println(ConsoleMsg.CONSOLE_NO_HOST_MESSAGE);
									}
								} else {
									org.osgi.framework.Bundle[] fragments = packageAdmin.getFragments(bundle);
									if (fragments != null) {
										intp.println(ConsoleMsg.CONSOLE_FRAGMENT_MESSAGE);
										for (int i = 0; i < fragments.length; i++) {
											intp.print("    "); //$NON-NLS-1$
											intp.println(fragments[i]);
										}
									} else {
										intp.println(ConsoleMsg.CONSOLE_NO_FRAGMENT_MESSAGE);
									}
								}

								RequiredBundle[] requiredBundles = packageAdmin.getRequiredBundles(null);
								RequiredBundle requiredBundle = null;
								if (requiredBundles != null) {
									for (int i = 0; i < requiredBundles.length; i++) {
										if (requiredBundles[i].getBundle() == bundle) {
											requiredBundle = requiredBundles[i];
											break;
										}
									}
								}

								if (requiredBundle == null) {
									intp.print("  "); //$NON-NLS-1$
									intp.println(ConsoleMsg.CONSOLE_NO_NAMED_CLASS_SPACES_MESSAGE);
								} else {
									intp.print("  "); //$NON-NLS-1$
									intp.println(ConsoleMsg.CONSOLE_NAMED_CLASS_SPACE_MESSAGE);
									intp.print("    "); //$NON-NLS-1$
									intp.print(requiredBundle);
									if (requiredBundle.isRemovalPending()) {
										intp.println(ConsoleMsg.CONSOLE_REMOVAL_PENDING_MESSAGE);
									} else {
										intp.println(ConsoleMsg.CONSOLE_PROVIDED_MESSAGE);
									}
								}
								title = true;
								for (int i = 0; i < requiredBundles.length; i++) {
									if (requiredBundles[i] == requiredBundle)
										continue;

									org.osgi.framework.Bundle[] depBundles = requiredBundles[i].getRequiringBundles();
									if (depBundles == null)
										continue;

									for (int j = 0; j < depBundles.length; j++) {
										if (depBundles[j] == bundle) {
											if (title) {
												intp.print("  "); //$NON-NLS-1$
												intp.println(ConsoleMsg.CONSOLE_REQUIRED_BUNDLES_MESSAGE);
												title = false;
											}
											intp.print("    "); //$NON-NLS-1$
											intp.print(requiredBundles[i]);

											org.osgi.framework.Bundle provider = requiredBundles[i].getBundle();
											intp.print("<"); //$NON-NLS-1$
											intp.print(provider);
											intp.println(">"); //$NON-NLS-1$
										}
									}
								}
								if (title) {
									intp.print("  "); //$NON-NLS-1$
									intp.println(ConsoleMsg.CONSOLE_NO_REQUIRED_BUNDLES_MESSAGE);
								}

							}
						} finally {
							context.ungetService(packageAdminRef);
						}
					}
				} else {
					intp.print("  "); //$NON-NLS-1$
					intp.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_NO_PACKAGE_ADMIN_MESSAGE);
				}

				SecurityManager sm = System.getSecurityManager();
				if (sm != null) {
					ProtectionDomain domain = bundle.getProtectionDomain();

					intp.println(domain);
				}
			}
			nextArg = intp.nextArgument();
		}
	}

	private boolean printImportedPackages(ExportPackageDescription[] importedPkgs, CommandInterpreter intp, boolean title) {
		for (int i = 0; i < importedPkgs.length; i++) {
			if (title) {
				intp.print("  "); //$NON-NLS-1$
				intp.println(ConsoleMsg.CONSOLE_IMPORTED_PACKAGES_MESSAGE);
				title = false;
			}
			intp.print("    "); //$NON-NLS-1$
			intp.print(importedPkgs[i].getName());
			intp.print("; version=\""); //$NON-NLS-1$
			intp.print(importedPkgs[i].getVersion());
			intp.print("\""); //$NON-NLS-1$
			Bundle exporter = context.getBundle(importedPkgs[i].getSupplier().getBundleId());
			if (exporter != null) {
				intp.print("<"); //$NON-NLS-1$
				intp.print(exporter);
				intp.println(">"); //$NON-NLS-1$
			} else {
				intp.print("<"); //$NON-NLS-1$
				intp.print(ConsoleMsg.CONSOLE_STALE_MESSAGE);
				intp.println(">"); //$NON-NLS-1$
			}
		}
		return title;
	}

	private void printUnwiredDynamicImports(List<ImportPackageSpecification> dynamicImports, CommandInterpreter intp) {
		for (int i = 0; i < dynamicImports.size(); i++) {
			ImportPackageSpecification importPackage = dynamicImports.get(i);
			intp.print("    "); //$NON-NLS-1$
			intp.print(importPackage.getName());
			intp.print("; version=\""); //$NON-NLS-1$
			intp.print(importPackage.getVersionRange());
			intp.print("\""); //$NON-NLS-1$
			intp.print("<"); //$NON-NLS-1$
			intp.print("unwired"); //$NON-NLS-1$
			intp.print(">"); //$NON-NLS-1$
			intp.print("<"); //$NON-NLS-1$
			intp.print(importPackage.getDirective(org.osgi.framework.Constants.RESOLUTION_DIRECTIVE));
			intp.println(">"); //$NON-NLS-1$
		}
	}

	/**
	 *  Handle the gc command.  Perform a garbage collection.
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _gc(CommandInterpreter intp) throws Exception {
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
		intp.print(ConsoleMsg.CONSOLE_TOTAL_MEMORY_MESSAGE);
		intp.println(String.valueOf(Runtime.getRuntime().totalMemory()));
		intp.print(ConsoleMsg.CONSOLE_FREE_MEMORY_BEFORE_GARBAGE_COLLECTION_MESSAGE);
		intp.println(String.valueOf(before));
		intp.print(ConsoleMsg.CONSOLE_FREE_MEMORY_AFTER_GARBAGE_COLLECTION_MESSAGE);
		intp.println(String.valueOf(after));
		intp.print(ConsoleMsg.CONSOLE_MEMORY_GAINED_WITH_GARBAGE_COLLECTION_MESSAGE);
		intp.println(String.valueOf(after - before));
	}

	/**
	 *  Handle the init command.  Uninstall all bundles.
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	@SuppressWarnings("deprecation")
	public void _init(CommandInterpreter intp) throws Exception {
		if (framework.isActive()) {
			intp.print(newline);
			intp.println(ConsoleMsg.CONSOLE_FRAMEWORK_LAUNCHED_PLEASE_SHUTDOWN_MESSAGE);
			return;
		}

		AbstractBundle[] bundles = (AbstractBundle[]) context.getBundles();

		int size = bundles.length;

		if (size > 0) {
			for (int i = 0; i < size; i++) {
				AbstractBundle bundle = bundles[i];

				if (bundle.getBundleId() != 0) {
					try {
						bundle.uninstall();
					} catch (BundleException e) {
						intp.printStackTrace(e);
					}
				}
			}
		} else {
			intp.println(ConsoleMsg.CONSOLE_NO_INSTALLED_BUNDLES_ERROR);
		}
		if (securityAdmin != null) {
			// clear the permissions from permission admin
			securityAdmin.setDefaultPermissions(null);
			String[] permLocations = securityAdmin.getLocations();
			if (permLocations != null)
				for (int i = 0; i < permLocations.length; i++)
					securityAdmin.setPermissions(permLocations[i], null);
			ConditionalPermissionUpdate update = securityAdmin.newConditionalPermissionUpdate();
			update.getConditionalPermissionInfos().clear();
			update.commit();
		}
		// clear the permissions from conditional permission admin
		if (securityAdmin != null)
			for (Enumeration<ConditionalPermissionInfo> infos = securityAdmin.getConditionalPermissionInfos(); infos.hasMoreElements();)
				infos.nextElement().delete();
	}

	/**
	 *  Handle the close command.  Shutdown and exit.
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _close(CommandInterpreter intp) throws Exception {
		intp.println();
		framework.close();
		System.exit(0);
	}

	/**
	 *  Handle the refresh command's abbreviation.  Invoke _refresh()
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _r(CommandInterpreter intp) throws Exception {
		_refresh(intp);
	}

	/**
	 *  Handle the refresh command.  Refresh the packages of the specified bundles.
	 *
	 *  @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _refresh(CommandInterpreter intp) throws Exception {
		ServiceReference<?> packageAdminRef = context.getServiceReference("org.osgi.service.packageadmin.PackageAdmin"); //$NON-NLS-1$
		if (packageAdminRef != null) {
			org.osgi.service.packageadmin.PackageAdmin packageAdmin = (org.osgi.service.packageadmin.PackageAdmin) context.getService(packageAdminRef);
			if (packageAdmin != null) {
				try {
					Bundle[] refresh = null;

					String token = intp.nextArgument();
					if (token != null) {
						List<Bundle> bundles = new ArrayList<Bundle>();

						while (token != null) {
							AbstractBundle bundle = getBundleFromToken(intp, token, true);

							if (bundle != null) {
								bundles.add(bundle);
							}
							token = intp.nextArgument();
						}

						int size = bundles.size();

						if (size == 0) {
							intp.println(ConsoleMsg.CONSOLE_INVALID_BUNDLE_SPECIFICATION_ERROR);
							return;
						}

						refresh = new Bundle[size];
						bundles.toArray(refresh);
					}

					packageAdmin.refreshPackages(refresh);
				} finally {
					context.ungetService(packageAdminRef);
				}
			}
		} else {
			intp.println(ConsoleMsg.CONSOLE_CAN_NOT_REFRESH_NO_PACKAGE_ADMIN_ERROR);
		}
	}

	/**
	 * Executes the given system command in a separate system process
	 * and waits for it to finish.
	 *
	 * @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _exec(CommandInterpreter intp) throws Exception {
		String command = intp.nextArgument();
		if (command == null) {
			intp.println(ConsoleMsg.CONSOLE_NO_COMMAND_SPECIFIED_ERROR);
			return;
		}

		Process p = Runtime.getRuntime().exec(command);

		intp.println(NLS.bind(ConsoleMsg.CONSOLE_STARTED_IN_MESSAGE, command, String.valueOf(p)));
		int result = p.waitFor();
		intp.println(NLS.bind(ConsoleMsg.CONSOLE_EXECUTED_RESULT_CODE_MESSAGE, command, String.valueOf(result)));
	}

	/**
	 * Executes the given system command in a separate system process.  It does
	 * not wait for a result.
	 *
	 * @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _fork(CommandInterpreter intp) throws Exception {
		String command = intp.nextArgument();
		if (command == null) {
			intp.println(ConsoleMsg.CONSOLE_NO_COMMAND_SPECIFIED_ERROR);
			return;
		}

		Process p = Runtime.getRuntime().exec(command);
		intp.println(NLS.bind(ConsoleMsg.CONSOLE_STARTED_IN_MESSAGE, command, String.valueOf(p)));
	}

	/**
	 * Handle the headers command's abbreviation.  Invoke _headers()
	 *
	 * @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _h(CommandInterpreter intp) throws Exception {
		_headers(intp);
	}

	/**
	 * Handle the headers command.  Display headers for the specified bundle(s).
	 *
	 * @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _headers(CommandInterpreter intp) throws Exception {

		String nextArg = intp.nextArgument();
		if (nextArg == null) {
			intp.println(ConsoleMsg.CONSOLE_NO_BUNDLE_SPECIFIED_ERROR);
		}
		while (nextArg != null) {
			AbstractBundle bundle = getBundleFromToken(intp, nextArg, true);
			if (bundle != null) {
				intp.printDictionary(bundle.getHeaders(), ConsoleMsg.CONSOLE_BUNDLE_HEADERS_TITLE);
			}
			nextArg = intp.nextArgument();
		}
	}

	/**
	 * Handles the props command's abbreviation.  Invokes _props()
	 *
	 * @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _pr(CommandInterpreter intp) throws Exception {
		_props(intp);
	}

	/**
	 * Handles the _props command.  Prints the system properties sorted.
	 *
	 * @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _props(CommandInterpreter intp) throws Exception {
		intp.printDictionary(FrameworkProperties.getProperties(), ConsoleMsg.CONSOLE_SYSTEM_PROPERTIES_TITLE);
	}

	/**
	 * Handles the setprop command's abbreviation.  Invokes _setprop()
	 *
	 * @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _setp(CommandInterpreter intp) throws Exception {
		_setprop(intp);
	}

	/**
	 * Handles the setprop command.  Sets the CDS property in the given argument.
	 *
	 * @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _setprop(CommandInterpreter intp) throws Exception {
		String argument = intp.nextArgument();
		if (argument == null) {
			intp.println(ConsoleMsg.CONSOLE_NO_PARAMETERS_SPECIFIED_TITLE);
			_props(intp);
		} else {
			InputStream in = new ByteArrayInputStream(argument.getBytes());
			try {
				Properties sysprops = FrameworkProperties.getProperties();
				Properties newprops = new Properties();
				newprops.load(in);
				intp.println(ConsoleMsg.CONSOLE_SETTING_PROPERTIES_TITLE);
				Enumeration<?> keys = newprops.propertyNames();
				while (keys.hasMoreElements()) {
					String key = (String) keys.nextElement();
					String value = (String) newprops.get(key);
					sysprops.put(key, value);
					intp.println(tab + key + " = " + value); //$NON-NLS-1$
				}
			} catch (IOException e) {
				// ignore
			} finally {
				try {
					in.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
	}

	/**
	 * Prints the short version of the status.
	 * For the long version use "status".
	 *
	 * @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _ss(CommandInterpreter intp) throws Exception {
		if (framework.isActive()) {
			intp.println();
			intp.println(ConsoleMsg.CONSOLE_FRAMEWORK_IS_LAUNCHED_MESSAGE);
		} else {
			intp.println();
			intp.println(ConsoleMsg.CONSOLE_FRAMEWORK_IS_SHUTDOWN_MESSAGE);
		}

		Object[] options = processOption(intp);
		if (options == null)
			return;

		AbstractBundle[] bundles = (AbstractBundle[]) context.getBundles();
		if (bundles.length == 0) {
			intp.println(ConsoleMsg.CONSOLE_NO_INSTALLED_BUNDLES_ERROR);
		} else {
			intp.print(newline);
			intp.print(ConsoleMsg.CONSOLE_ID);
			intp.print(tab);
			intp.println(ConsoleMsg.CONSOLE_STATE_BUNDLE_TITLE);
			for (int i = 0; i < bundles.length; i++) {
				AbstractBundle b = bundles[i];
				if (!match(b, (String) options[0], ((Integer) options[1]).intValue()))
					continue;
				String label = b.getSymbolicName();
				if (label == null || label.length() == 0)
					label = b.toString();
				else
					label = label + "_" + b.getVersion(); //$NON-NLS-1$
				intp.println(b.getBundleId() + "\t" + getStateName(b) + label); //$NON-NLS-1$ 
				if (b.isFragment()) {
					Bundle[] hosts = b.getHosts();
					if (hosts != null)
						for (int j = 0; j < hosts.length; j++)
							intp.println("\t            Master=" + hosts[j].getBundleId()); //$NON-NLS-1$
				} else {
					Bundle[] fragments = b.getFragments();
					if (fragments != null) {
						intp.print("\t            Fragments="); //$NON-NLS-1$
						for (int f = 0; f < fragments.length; f++) {
							AbstractBundle fragment = (AbstractBundle) fragments[f];
							intp.print((f > 0 ? ", " : "") + fragment.getBundleId()); //$NON-NLS-1$ //$NON-NLS-2$
						}
						intp.println();
					}
				}
			}
		}
	}

	private boolean match(Bundle toFilter, String searchedName, int searchedState) {
		if ((toFilter.getState() & searchedState) == 0) {
			return false;
		}
		if (searchedName != null && toFilter.getSymbolicName() != null && toFilter.getSymbolicName().indexOf(searchedName) == -1) {
			return false;
		}
		return true;
	}

	/**
	 * Handles the threads command abbreviation.  Invokes _threads().
	 *
	 * @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _t(CommandInterpreter intp) throws Exception {
		_threads(intp);
	}

	/**
	 * Prints the information about the currently running threads
	 * in the embedded system.
	 *
	 * @param intp A CommandInterpreter object containing the command
	 * and it's arguments.
	 */
	public void _threads(CommandInterpreter intp) throws Exception {

		ThreadGroup[] threadGroups = getThreadGroups();
		Util.sortByString(threadGroups);

		ThreadGroup tg = getTopThreadGroup();
		Thread[] threads = new Thread[tg.activeCount()];
		int count = tg.enumerate(threads, true);
		Util.sortByString(threads);

		StringBuffer sb = new StringBuffer(120);
		intp.println();
		intp.println(ConsoleMsg.CONSOLE_THREADGROUP_TITLE);
		for (int i = 0; i < threadGroups.length; i++) {
			tg = threadGroups[i];
			int all = tg.activeCount(); //tg.allThreadsCount();
			int local = tg.enumerate(new Thread[all], false); //tg.threadsCount();
			ThreadGroup p = tg.getParent();
			String parent = (p == null) ? "-none-" : p.getName(); //$NON-NLS-1$
			sb.setLength(0);
			sb.append(Util.toString(simpleClassName(tg), 18)).append(" ").append(Util.toString(tg.getName(), 21)).append(" ").append(Util.toString(parent, 16)).append(Util.toString(new Integer(tg.getMaxPriority()), 3)).append(Util.toString(new Integer(local), 4)).append("/").append(Util.toString(String.valueOf(all), 6)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			intp.println(sb.toString());
		}
		intp.print(newline);
		intp.println(ConsoleMsg.CONSOLE_THREADTYPE_TITLE);
		for (int j = 0; j < count; j++) {
			Thread t = threads[j];
			if (t != null) {
				sb.setLength(0);
				sb.append(Util.toString(simpleClassName(t), 18)).append(" ").append(Util.toString(t.getName(), 21)).append(" ").append(Util.toString(t.getThreadGroup().getName(), 16)).append(Util.toString(new Integer(t.getPriority()), 3)); //$NON-NLS-1$ //$NON-NLS-2$
				if (t.isDaemon())
					sb.append(" [daemon]"); //$NON-NLS-1$
				intp.println(sb.toString());
			}
		}
	}

	/**
	 * Handles the sl (startlevel) command. 
	 *
	 * @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _sl(CommandInterpreter intp) throws Exception {
		if (isStartLevelSvcPresent(intp)) {
			org.osgi.framework.Bundle bundle = null;
			String token = intp.nextArgument();
			int value = 0;
			if (token != null) {
				bundle = getBundleFromToken(intp, token, true);
				if (bundle == null) {
					return;
				}
			}
			if (bundle == null) { // must want framework startlevel
				value = slImpl.getStartLevel();
				intp.println(NLS.bind(ConsoleMsg.STARTLEVEL_FRAMEWORK_ACTIVE_STARTLEVEL, String.valueOf(value)));
			} else { // must want bundle startlevel
				value = slImpl.getBundleStartLevel(bundle);
				intp.println(NLS.bind(ConsoleMsg.STARTLEVEL_BUNDLE_STARTLEVEL, new Long(bundle.getBundleId()), new Integer(value)));
			}
		}
	}

	/**
	 * Handles the setfwsl (set framework startlevel) command. 
	 *
	 * @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _setfwsl(CommandInterpreter intp) throws Exception {
		if (isStartLevelSvcPresent(intp)) {
			int value = 0;
			String token = intp.nextArgument();
			if (token == null) {
				intp.println(ConsoleMsg.STARTLEVEL_NO_STARTLEVEL_GIVEN);
				value = slImpl.getStartLevel();
				intp.println(NLS.bind(ConsoleMsg.STARTLEVEL_FRAMEWORK_ACTIVE_STARTLEVEL, String.valueOf(value)));
			} else {
				value = this.getStartLevelFromToken(intp, token);
				if (value > 0) {
					try {
						slImpl.setStartLevel(value);
						intp.println(NLS.bind(ConsoleMsg.STARTLEVEL_FRAMEWORK_ACTIVE_STARTLEVEL, String.valueOf(value)));
					} catch (IllegalArgumentException e) {
						intp.println(e.getMessage());
					}
				}
			}
		}
	}

	/**
	 * Handles the setbsl (set bundle startlevel) command. 
	 *
	 * @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _setbsl(CommandInterpreter intp) throws Exception {
		if (isStartLevelSvcPresent(intp)) {
			String token;
			AbstractBundle bundle = null;
			token = intp.nextArgument();
			if (token == null) {
				intp.println(ConsoleMsg.STARTLEVEL_NO_STARTLEVEL_OR_BUNDLE_GIVEN);
				return;
			}

			int newSL = this.getStartLevelFromToken(intp, token);

			token = intp.nextArgument();
			if (token == null) {
				intp.println(ConsoleMsg.STARTLEVEL_NO_STARTLEVEL_OR_BUNDLE_GIVEN);
				return;
			}
			while (token != null) {
				bundle = getBundleFromToken(intp, token, true);
				if (bundle != null) {
					try {
						slImpl.setBundleStartLevel(bundle, newSL);
						intp.println(NLS.bind(ConsoleMsg.STARTLEVEL_BUNDLE_STARTLEVEL, new Long(bundle.getBundleId()), new Integer(newSL)));
					} catch (IllegalArgumentException e) {
						intp.println(e.getMessage());
					}
				}
				token = intp.nextArgument();
			}
		}
	}

	/**
	 * Handles the setibsl (set initial bundle startlevel) command. 
	 *
	 * @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _setibsl(CommandInterpreter intp) throws Exception {
		if (isStartLevelSvcPresent(intp)) {
			int value = 0;
			String token = intp.nextArgument();
			if (token == null) {
				intp.println(ConsoleMsg.STARTLEVEL_NO_STARTLEVEL_GIVEN);
				value = slImpl.getInitialBundleStartLevel();
				intp.println(NLS.bind(ConsoleMsg.STARTLEVEL_INITIAL_BUNDLE_STARTLEVEL, String.valueOf(value)));
			} else {
				value = this.getStartLevelFromToken(intp, token);
				if (value > 0) {
					try {
						slImpl.setInitialBundleStartLevel(value);
						intp.println(NLS.bind(ConsoleMsg.STARTLEVEL_INITIAL_BUNDLE_STARTLEVEL, String.valueOf(value)));
					} catch (IllegalArgumentException e) {
						intp.println(e.getMessage());
					}
				}
			}
		}
	}

	public void _requiredBundles(CommandInterpreter intp) {
		_classSpaces(intp);
	}

	public void _classSpaces(CommandInterpreter intp) {

		String token = intp.nextArgument();

		ServiceReference<?> packageAdminRef = context.getServiceReference("org.osgi.service.packageadmin.PackageAdmin"); //$NON-NLS-1$
		if (packageAdminRef != null) {
			PackageAdmin packageAdmin = (PackageAdmin) context.getService(packageAdminRef);
			if (packageAdmin != null) {
				try {
					org.osgi.service.packageadmin.RequiredBundle[] symBundles = null;

					symBundles = packageAdmin.getRequiredBundles(token);

					if (symBundles == null) {
						intp.println(ConsoleMsg.CONSOLE_NO_NAMED_CLASS_SPACES_MESSAGE);
					} else {
						for (int i = 0; i < symBundles.length; i++) {
							org.osgi.service.packageadmin.RequiredBundle symBundle = symBundles[i];
							intp.print(symBundle);

							boolean removalPending = symBundle.isRemovalPending();
							if (removalPending) {
								intp.print("("); //$NON-NLS-1$
								intp.print(ConsoleMsg.CONSOLE_REMOVAL_PENDING_MESSAGE);
								intp.println(")"); //$NON-NLS-1$
							}

							org.osgi.framework.Bundle provider = symBundle.getBundle();
							if (provider != null) {
								intp.print("<"); //$NON-NLS-1$
								intp.print(provider);
								intp.println(">"); //$NON-NLS-1$

								org.osgi.framework.Bundle[] requiring = symBundle.getRequiringBundles();
								if (requiring != null)
									for (int j = 0; j < requiring.length; j++) {
										intp.print("  "); //$NON-NLS-1$
										intp.print(requiring[j]);
										intp.print(" "); //$NON-NLS-1$
										intp.println(ConsoleMsg.CONSOLE_REQUIRES_MESSAGE);
									}
							} else {
								intp.print("<"); //$NON-NLS-1$
								intp.print(ConsoleMsg.CONSOLE_STALE_MESSAGE);
								intp.println(">"); //$NON-NLS-1$
							}

						}
					}
				} finally {
					context.ungetService(packageAdminRef);
				}
			}
		} else {
			intp.println(ConsoleMsg.CONSOLE_NO_EXPORTED_PACKAGES_NO_PACKAGE_ADMIN_MESSAGE);
		}
	}

	/**
	 * Handles the profilelog command. 
	 *
	 * @param intp A CommandInterpreter object containing the command and it's arguments.
	 */
	public void _profilelog(CommandInterpreter intp) throws Exception {
		intp.println(Profile.getProfileLog());
	}

	public void _getPackages(CommandInterpreter intp) {

		String nextArg = intp.nextArgument();
		if (nextArg == null)
			return;
		AbstractBundle bundle = getBundleFromToken(intp, nextArg, true);
		ServiceReference<?> ref = context.getServiceReference("org.eclipse.osgi.service.resolver.PlatformAdmin"); //$NON-NLS-1$
		if (ref == null)
			return;
		PlatformAdmin platformAdmin = (PlatformAdmin) context.getService(ref);
		try {
			ExportPackageDescription[] exports = platformAdmin.getStateHelper().getVisiblePackages(bundle.getBundleDescription(), StateHelper.VISIBLE_INCLUDE_EE_PACKAGES | StateHelper.VISIBLE_INCLUDE_ALL_HOST_WIRES);
			for (int i = 0; i < exports.length; i++) {
				intp.println(exports[i] + ": " + platformAdmin.getStateHelper().getAccessCode(bundle.getBundleDescription(), exports[i])); //$NON-NLS-1$
			}
		} finally {
			context.ungetService(ref);
		}
	}

	/**
	 * Handles the help command
	 * 
	 * @param intp
	 * @return description for a particular command or false if there is no command with the specified name
	 */
	public Object _help(CommandInterpreter intp) {
		String commandName = intp.nextArgument();
		if (commandName == null) {
			return false;
		}
		String help = getHelp(commandName);
		return help.length() > 0 ? help : false;
	}

	/**
	 * Checks for the presence of the StartLevel Service.  Outputs a message if it is not present.
	 * @param intp The CommandInterpreter object to be used to write to the console
	 * @return true or false if service is present or not
	 */
	protected boolean isStartLevelSvcPresent(CommandInterpreter intp) {
		boolean retval = false;
		ServiceReference<?> slSvcRef = context.getServiceReference("org.osgi.service.startlevel.StartLevel"); //$NON-NLS-1$
		if (slSvcRef != null) {
			StartLevel slSvc = (StartLevel) context.getService(slSvcRef);
			if (slSvc != null) {
				retval = true;
			}
		} else {
			intp.println(ConsoleMsg.CONSOLE_CAN_NOT_USE_STARTLEVEL_NO_STARTLEVEL_SVC_ERROR);
		}
		return retval;
	}

	/**
	 *  Given a number or a token representing a bundle symbolic name or bundle location,
	 *  retrieve the Bundle object with that id.  The bundle symbolic name token is parsed as
	 *  symbolicname[@version]
	 *  
	 *	@param intp The CommandInterpreter
	 *  @param token A string containing a potential bundle it
	 *  @param error A boolean indicating whether or not to output a message
	 *  @return The requested Bundle object
	 */
	protected AbstractBundle getBundleFromToken(CommandInterpreter intp, String token, boolean error) {
		AbstractBundle bundle = null;
		try {
			long id = Long.parseLong(token);
			bundle = (AbstractBundle) context.getBundle(id);
		} catch (NumberFormatException nfe) {

			// if not found, assume token is either symbolic name@version, or location
			String symbolicName = token;
			Version version = null;

			// check for @ -- this may separate either the version string, or be part of the
			// location
			int ix = token.indexOf("@"); //$NON-NLS-1$
			if (ix != -1) {
				if ((ix + 1) != token.length()) {
					try {
						// if the version parses, then use the token prior to @ as a symbolic name
						version = Version.parseVersion(token.substring(ix + 1, token.length()));
						symbolicName = token.substring(0, ix);
					} catch (IllegalArgumentException e) {
						// version doesn't parse, assume token is symbolic name without version, or location
					}
				}
			}

			Bundle[] bundles = context.getBundles();
			for (int i = 0, n = bundles.length; i < n; i++) {
				AbstractBundle b = (AbstractBundle) bundles[i];
				// if symbolicName matches, then matches if there is no version specific on command, or the version matches
				// if there is no version specified on command, pick first matching bundle
				if ((symbolicName.equals(b.getSymbolicName()) && (version == null || version.equals(b.getVersion()))) || token.equals(b.getLocation())) {
					bundle = b;
					break;
				}
			}
		}

		if ((bundle == null) && error) {
			intp.println(NLS.bind(ConsoleMsg.CONSOLE_CANNOT_FIND_BUNDLE_ERROR, token));
		}

		return (bundle);
	}

	/**
	 *  Given a string containing a startlevel value, validate it and convert it to an int
	 * 
	 *  @param intp A CommandInterpreter object used for printing out error messages
	 *  @param value A string containing a potential startlevel
	 *  @return The start level or an int <0 if it was invalid
	 */
	protected int getStartLevelFromToken(CommandInterpreter intp, String value) {
		int retval = -1;
		try {
			retval = Integer.parseInt(value);
			if (Integer.parseInt(value) <= 0) {
				intp.println(ConsoleMsg.STARTLEVEL_POSITIVE_INTEGER);
			}
		} catch (NumberFormatException nfe) {
			intp.println(ConsoleMsg.STARTLEVEL_POSITIVE_INTEGER);
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

	public void _getprop(CommandInterpreter ci) throws Exception {
		Properties allProperties = FrameworkProperties.getProperties();
		String filter = ci.nextArgument();
		Iterator<?> propertyNames = new TreeSet<Object>(allProperties.keySet()).iterator();
		while (propertyNames.hasNext()) {
			String prop = (String) propertyNames.next();
			if (filter == null || prop.startsWith(filter)) {
				ci.println(prop + '=' + allProperties.getProperty(prop));
			}
		}
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
}
