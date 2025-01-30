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

import java.io.PrintStream;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.eclipse.osgi.container.ModuleContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Resource;

/**
 * Provides a "wires" command to print information about the wiring of a bundle
 */
public class WireCommand {

	private static final Comparator<BundleRevision> BUNDLE_REVISIONS_BY_NAME = Comparator.comparing(
			BundleRevision::getSymbolicName, String.CASE_INSENSITIVE_ORDER);

	private final BundleContext context;

	public WireCommand(BundleContext context) {
		this.context = context;
	}

	@Descriptor("Prints information about the wiring of a particular bundle")
	public void wires(CommandSession session, long id) {
		PrintStream console = session.getConsole();
		Bundle bundle = context.getBundle(id);
		if (bundle == null) {
			console.println(String.format("Bundle with id %d not found!", id));
			return;
		}
		BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
		if (bundleWiring == null) {
			console.println(String.format("Bundle with id %d has no wiring!", id));
			return;
		}
		console.println("Bundle " + bundle.getSymbolicName() + " " + bundle.getVersion() + ":");
		printWiring(console, bundleWiring);
	}

	static void printWiring(PrintStream console, BundleWiring bundleWiring) {
		BundleRevision resource = bundleWiring.getResource();
		Map<BundleRevision, List<BundleWire>> usedWires =
				bundleWiring.getRequiredWires(null).stream()
						.filter(bw -> !resource.equals(bw.getProvider()))
						.collect(Collectors.groupingBy(BundleWire::getProvider));
		if (usedWires.isEmpty()) {
			console.println("is not wired to any other bundle");
		} else {
			console.println("is wired to:");
			usedWires.entrySet().stream().sorted(Comparator
					.comparing(java.util.Map.Entry::getKey, BUNDLE_REVISIONS_BY_NAME))
					.forEach(bre -> {
						console.println("\t - " + getResource(bre.getKey()));
						for (BundleWire bw : bre.getValue()) {
							console.println("\t   - because of "
									+ ModuleContainer.toString(bw.getRequirement()));
						}
					});

		}
		Map<BundleRevision, List<BundleWire>> consumersWires =
				bundleWiring.getProvidedWires(null).stream()
						.collect(Collectors.groupingBy(BundleWire::getRequirer));
		if (consumersWires.isEmpty()) {
			console.println("and is not consumed by any bundle");
		} else {
			console.println("and is consumed by:");
			consumersWires.entrySet().stream().sorted(Comparator
					.comparing(java.util.Map.Entry::getKey, BUNDLE_REVISIONS_BY_NAME))
					.forEach(bre -> {
						console.println("\t - " + getResource(bre.getKey()));
						for (BundleWire bw : bre.getValue()) {
							console.println("\t   - because it "
									+ ModuleContainer.toString(bw.getRequirement()));
						}
					});
		}
	}

	static String getResource(Resource resource) {
		if (resource instanceof BundleRevision) {
			BundleRevision bundleRevision = (BundleRevision) resource;
			return bundleRevision.getSymbolicName() + " " + bundleRevision.getVersion();
		}
		return String.valueOf(resource);
	}

	public void startService() {
		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put(CommandProcessor.COMMAND_SCOPE, "wiring");
		dict.put(CommandProcessor.COMMAND_FUNCTION, new String[] { "wires" });
		context.registerService(WireCommand.class, this, dict);
	}

}