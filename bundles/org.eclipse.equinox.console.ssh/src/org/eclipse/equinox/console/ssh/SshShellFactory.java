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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;
import org.osgi.framework.BundleContext;

/**
 * Shell factory used by the SSH server to create a SSH shell
 */
public class SshShellFactory implements ShellFactory {

	private final List<CommandProcessor> processors;
	private final BundleContext context;
	private final Set<SshShell> shells = new HashSet<>();

	public SshShellFactory(List<CommandProcessor> processors, BundleContext context) {
		this.processors = processors;
		this.context = context;
	}

	@Override
	public synchronized Command createShell(ChannelSession channel) {
		SshShell shell = new SshShell(processors, context);
		shells.add(shell);
		return shell;
	}

	public synchronized void addCommandProcessor(CommandProcessor processor) {
		processors.add(processor);
		for (SshShell shell : shells) {
			shell.addCommandProcessor(processor);
		}
	}

	public synchronized void removeCommandProcessor(CommandProcessor processor) {
		processors.remove(processor);
		for (SshShell shell : shells) {
			shell.removeCommandProcessor(processor);
		}
	}

	public void exit() {
		for (SshShell shell : shells) {
			shell.onExit();
		}
	}
}
