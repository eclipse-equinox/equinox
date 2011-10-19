/*******************************************************************************
 * Copyright (c) 2011 SAP AG
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/

package org.eclipse.equinox.console.ssh;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.sshd.common.Factory;
import org.apache.sshd.server.Command;
import org.osgi.framework.BundleContext;

/**
 *  Shell factory used by the SSH server to create a SSH shell
 *
 */
public class SshShellFactory implements Factory<Command> {
	
	private List<CommandProcessor> processors;
	private BundleContext context;
	private Set<SshShell> shells = new HashSet<SshShell>();
	
	public SshShellFactory(List<CommandProcessor> processors, BundleContext context) {
		this.processors = processors;
		this.context = context;
	}
	
	public synchronized Command create() {
		SshShell shell = new SshShell(processors, context);
		shells.add(shell);
		return shell;
	}
	
	public synchronized void addCommandProcessor (CommandProcessor processor) {
		processors.add(processor);
		for (SshShell shell : shells) {
			shell.addCommandProcessor(processor);
		}
	}
	
	public synchronized void removeCommandProcessor (CommandProcessor processor) {
		processors.remove(processor);
		for (SshShell shell : shells) {
			shell.removeCommandProcessor(processor);
		}
	}
	
	public void exit() {
		for(SshShell shell : shells) {
			shell.onExit();
		}
	}
}
