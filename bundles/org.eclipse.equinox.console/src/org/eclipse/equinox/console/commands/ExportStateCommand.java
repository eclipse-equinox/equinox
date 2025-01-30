/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.console.commands;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.eclipse.osgi.container.Module;
import org.osgi.framework.BundleContext;

public class ExportStateCommand {

	private final BundleContext context;

	public ExportStateCommand(BundleContext context) {
		this.context = context;
	}

	public void startService() {
		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put(CommandProcessor.COMMAND_SCOPE, "export");
		dict.put(CommandProcessor.COMMAND_FUNCTION, new String[] { "exportFrameworkState" });
		context.registerService(ExportStateCommand.class, this, dict);
	}

	@Descriptor("Exports the current framework state including the wiring information")
	public void exportFrameworkState(CommandSession session, String path) throws IOException {
		exportFrameworkState(session, path, true);
	}

	@Descriptor("Exports the current framework state, with or without the wiring information")
	public void exportFrameworkState(CommandSession session, String path, boolean persistWirings) throws IOException {
		Module module = context.getBundle(0).adapt(Module.class);
		PrintStream console = session.getConsole();
		if (module != null) {
			File file = new File(path);
			console.println("Exporting ModuleDatabase to " + file.getAbsolutePath() + "...");
			try (DataOutputStream stream = new DataOutputStream(new FileOutputStream(file))) {
				module.getContainer().store(stream, persistWirings);
			}
		} else {
			console.println("Can't determine ModuleDatabase!");
		}
	}

}
