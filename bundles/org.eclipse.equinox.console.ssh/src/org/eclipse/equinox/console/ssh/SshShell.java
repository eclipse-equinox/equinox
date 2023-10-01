/*******************************************************************************
 * Copyright (c) 2011, 2020 SAP AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.ssh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.eclipse.equinox.console.common.KEYS;
import org.eclipse.equinox.console.common.terminal.ANSITerminalTypeMappings;
import org.eclipse.equinox.console.common.terminal.SCOTerminalTypeMappings;
import org.eclipse.equinox.console.common.terminal.TerminalTypeMappings;
import org.eclipse.equinox.console.common.terminal.VT100TerminalTypeMappings;
import org.eclipse.equinox.console.common.terminal.VT220TerminalTypeMappings;
import org.eclipse.equinox.console.common.terminal.VT320TerminalTypeMappings;
import org.osgi.framework.BundleContext;

/**
 * This class manages a ssh connection. It is responsible for starting a sessions to execute commands
 * from the ssh. If there are multiple CommandProcessors, a session is started for each of them.
 *
 */
public class SshShell implements Command {

	private List<CommandProcessor> processors;
	private BundleContext context;
	private InputStream in;
	private OutputStream out;
	private ExitCallback callback;
	private Map<CommandProcessor, SshSession> commandProcessorToConsoleThreadMap = new HashMap<>();

	private final Map<String, TerminalTypeMappings> supportedEscapeSequences;
	private static final String DEFAULT_TTYPE = File.separatorChar == '/' ? "XTERM" : "ANSI";
	private TerminalTypeMappings currentMappings;
	private Map<String, KEYS> currentEscapesToKey;
	private static final String TERMINAL_PROPERTY = "TERM";

	public SshShell(List<CommandProcessor> processors, BundleContext context) {
		this.processors = processors;
		this.context = context;
		supportedEscapeSequences = new HashMap<> ();
		supportedEscapeSequences.put("ANSI", new ANSITerminalTypeMappings());
		supportedEscapeSequences.put("WINDOWS", new ANSITerminalTypeMappings());
		supportedEscapeSequences.put("VT100", new VT100TerminalTypeMappings());
		VT220TerminalTypeMappings vtMappings = new VT220TerminalTypeMappings();
		supportedEscapeSequences.put("VT220", vtMappings);
		supportedEscapeSequences.put("XTERM", vtMappings);
		supportedEscapeSequences.put("VT320", new VT320TerminalTypeMappings());
		supportedEscapeSequences.put("SCO", new SCOTerminalTypeMappings());

		currentMappings = supportedEscapeSequences.get(DEFAULT_TTYPE);
		currentEscapesToKey = currentMappings.getEscapesToKey();
	}

	@Override
	public void setInputStream(InputStream in) {
		this.in = in;
	}

	@Override
	public void setOutputStream(OutputStream out) {
		this.out = out;
	}

	@Override
	public void setErrorStream(OutputStream err) {
		// do nothing
	}

	@Override
	public void setExitCallback(ExitCallback callback) {
		this.callback = callback;
	}

	@Override
	public synchronized void start(ChannelSession channel, Environment env) throws IOException {
		String term = env.getEnv().get(TERMINAL_PROPERTY);
		TerminalTypeMappings mapping = supportedEscapeSequences.get(term.toUpperCase());
		if(mapping != null) {
			currentMappings = mapping;
			currentEscapesToKey = mapping.getEscapesToKey();
		}

		for (CommandProcessor processor : processors) {
			createNewSession(processor);
		}
	}

	public synchronized void addCommandProcessor(CommandProcessor processor) {
		createNewSession(processor);
	}

	public synchronized void removeCommandProcessor(CommandProcessor processor) {
		Thread consoleSession = commandProcessorToConsoleThreadMap.get(processor);
		if (consoleSession != null) {
			consoleSession.interrupt();
		}
	}

	private void createNewSession(CommandProcessor processor) {
		SshSession consoleSession = startNewConsoleSession(processor);
		commandProcessorToConsoleThreadMap.put(processor, consoleSession);
	}

	@Override
	public void destroy(ChannelSession channel) {
		return;
	}

	public void onExit() {
		if (commandProcessorToConsoleThreadMap.values() != null) {
			for (Thread consoleSession : commandProcessorToConsoleThreadMap.values()) {
				consoleSession.interrupt();
			}
		}
		callback.onExit(0);
	}

	public void removeSession(SshSession session) {
		CommandProcessor processorToRemove = null;
		for (java.util.Map.Entry<CommandProcessor, SshSession> entry : commandProcessorToConsoleThreadMap.entrySet()) {
			CommandProcessor processor = entry.getKey();
			if (session.equals(entry.getValue())) {
				processorToRemove = processor;
				break;
			}
		}

		if (processorToRemove != null) {
			commandProcessorToConsoleThreadMap.remove(processorToRemove);
		}

		if (commandProcessorToConsoleThreadMap.size() == 0) {
			onExit();
		}
	}

	private SshSession startNewConsoleSession(CommandProcessor processor) {
		SshSession consoleSession = new SshSession(processor, context, this, in, out, currentMappings, currentEscapesToKey);
		consoleSession.start();
		return consoleSession;
	}

}
