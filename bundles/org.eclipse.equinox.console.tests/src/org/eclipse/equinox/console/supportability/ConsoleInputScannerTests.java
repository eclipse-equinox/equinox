/*******************************************************************************
 * Copyright (c) 2011, 2017 SAP AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Lazar Kirchev, SAP AG - initial contribution
 ******************************************************************************/

package org.eclipse.equinox.console.supportability;

import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.apache.felix.service.command.CommandSession;
import org.eclipse.equinox.console.common.ConsoleInputScanner;
import org.eclipse.equinox.console.common.ConsoleInputStream;
import org.eclipse.equinox.console.common.ConsoleOutputStream;
import org.eclipse.equinox.console.common.KEYS;
import org.eclipse.equinox.console.common.terminal.ANSITerminalTypeMappings;
import org.eclipse.equinox.console.common.terminal.SCOTerminalTypeMappings;
import org.eclipse.equinox.console.common.terminal.TerminalTypeMappings;
import org.eclipse.equinox.console.common.terminal.VT100TerminalTypeMappings;
import org.eclipse.equinox.console.common.terminal.VT220TerminalTypeMappings;
import org.eclipse.equinox.console.common.terminal.VT320TerminalTypeMappings;
import org.eclipse.equinox.console.completion.common.Completer;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ConsoleInputScannerTests {

	private static int BS;

	private static final int LF = 10;

	private static final int CR = 13;

	private static final int ESC = 27;

	private static int DELL;
	
	private static int TAB = 9;
	
	private static final String COMMANDS = ".commands";

	@Test
	public void test() throws Exception {
		Set<TerminalTypeMappings> supportedEscapeSequences = new HashSet<>();
		supportedEscapeSequences.add(new ANSITerminalTypeMappings());
		supportedEscapeSequences.add(new VT100TerminalTypeMappings());
		supportedEscapeSequences.add(new VT220TerminalTypeMappings());
		supportedEscapeSequences.add(new VT320TerminalTypeMappings());
		supportedEscapeSequences.add(new SCOTerminalTypeMappings());

		for (TerminalTypeMappings ttMappings : supportedEscapeSequences) {
			Map<String, KEYS> escapesToKey = ttMappings.getEscapesToKey();
			Map<KEYS, byte[]> keysToEscapes = new HashMap<>();
			for (Entry<String, KEYS> entry : escapesToKey.entrySet()) {
				keysToEscapes.put(entry.getValue(), entry.getKey().getBytes());
			}

			BS = ttMappings.getBackspace();
			DELL = ttMappings.getDel();

			testScan(ttMappings, keysToEscapes);
		}
	}

	private void testScan(TerminalTypeMappings mappings, Map<KEYS, byte[]> keysToEscapes) throws Exception {
		ConsoleInputStream in = new ConsoleInputStream();
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		ConsoleOutputStream out = new ConsoleOutputStream(byteOut);
		ConsoleInputScanner scanner = new ConsoleInputScanner(in, out);
		scanner.setBackspace(mappings.getBackspace());
		scanner.setCurrentEscapesToKey(mappings.getEscapesToKey());
		scanner.setDel(mappings.getDel());
		scanner.setEscapes(mappings.getEscapes());

		byte[] line1 = new byte[] { 'a', 'b', 'c', 'd', 'e' };
		byte[] line2 = new byte[] { 't', 'e', 's', 't' };
		byte[] line3 = new byte[] { 'l', 'a', 's', 't' };

		addLine(scanner, line1);
		checkInpusStream(in, line1);

		addLine(scanner, line2);
		checkInpusStream(in, line2);

		addLine(scanner, line3);
		checkInpusStream(in, line3);

		add(scanner, keysToEscapes.get(KEYS.UP));
		add(scanner, keysToEscapes.get(KEYS.UP));
		String res = byteOut.toString();
		Assert.assertTrue("Error processing up arrow; expected test, actual " + res.substring(res.length() - 4), res.endsWith("test"));

		add(scanner, keysToEscapes.get(KEYS.DOWN));
		res = byteOut.toString();
		Assert.assertTrue("Error processing down arrow; expected last, actual " + res.substring(res.length() - 4), res.endsWith("last"));

		add(scanner, keysToEscapes.get(KEYS.PGUP));
		res = byteOut.toString();
		Assert.assertTrue("Error processing PageUp; expected abcde, actual " + res.substring(res.length() - 4), res.endsWith("abcde"));

		add(scanner, keysToEscapes.get(KEYS.PGDN));
		res = byteOut.toString();
		Assert.assertTrue("Error processing PageDown; expected last, actual " + res.substring(res.length() - 4), res.endsWith("last"));

		if (BS > 0) {
			scanner.scan(BS);
			res = byteOut.toString();
			Assert.assertTrue("Error processing backspace; expected las, actual " + res.substring(res.length() - 3), res.endsWith("las"));
			scanner.scan('t');
		}

		if (DELL > 0) {
			add(scanner, keysToEscapes.get(KEYS.LEFT));
			scanner.scan(DELL);
			res = byteOut.toString();
			Assert.assertTrue("Error processing del; expected las, actual " + res.substring(res.length() - 3), res.endsWith("las"));
			scanner.scan('t');
		}

		add(scanner, keysToEscapes.get(KEYS.LEFT));
		add(scanner, keysToEscapes.get(KEYS.LEFT));
		add(scanner, keysToEscapes.get(KEYS.RIGHT));
		if (DELL > 0) {
			scanner.scan(DELL);
		} else {
			add(scanner, keysToEscapes.get(KEYS.DEL));
		}
		res = byteOut.toString();
		Assert.assertTrue("Error processing arrows; expected las, actual " + res.substring(res.length() - 3), res.endsWith("las"));
		scanner.scan('t');

		if (keysToEscapes.get(KEYS.DEL) != null) {
			add(scanner, keysToEscapes.get(KEYS.LEFT));
			add(scanner, keysToEscapes.get(KEYS.DEL));
			res = byteOut.toString();
			Assert.assertTrue("Error processing delete; expected las, actual " + res.substring(res.length() - 3), res.endsWith("las"));
			scanner.scan('t');
		}

		add(scanner, keysToEscapes.get(KEYS.HOME));
		if (DELL > 0) {
			scanner.scan(DELL);
		} else {
			add(scanner, keysToEscapes.get(KEYS.DEL));
		}
		res = byteOut.toString();
		res = res.substring(res.length() - 6, res.length() - 3);
		Assert.assertTrue("Error processing Home; expected ast, actual " + res, res.equals("ast"));
		scanner.scan('l');

		add(scanner, keysToEscapes.get(KEYS.END));
		add(scanner, keysToEscapes.get(KEYS.LEFT));
		if (DELL > 0) {
			scanner.scan(DELL);
		} else {
			add(scanner, keysToEscapes.get(KEYS.DEL));
		}
		res = byteOut.toString();
		Assert.assertTrue("Error processing End; expected las, actual " + res.substring(res.length() - 3), res.endsWith("las"));
		scanner.scan('t');

		add(scanner, keysToEscapes.get(KEYS.LEFT));
		add(scanner, keysToEscapes.get(KEYS.INS));
		scanner.scan('a');
		res = byteOut.toString();
		Assert.assertTrue("Error processing Ins; expected las, actual " + res.substring(res.length() - 4), res.endsWith("lasa"));

		Filter filter = createMock(Filter.class);
		replay(filter);
		
		BundleContext context = createMock(BundleContext.class);
		expect(context.getServiceReferences(Completer.class.getName(), null)).andReturn(null).anyTimes();
		expect(context.createFilter("(objectClass=org.eclipse.equinox.console.commands.CommandsTracker)")).andReturn(filter);
		context.addServiceListener(isA(ServiceListener.class), isA(String.class));
		expect(context.getServiceReferences("org.eclipse.equinox.console.commands.CommandsTracker", null)).andReturn(new ServiceReference[]{});
		replay(context);
		
		Set<String> commands = new HashSet<>();
		commands.add("equinox:bundles");
		commands.add("equinox:bundle");
		commands.add("gogo:bundlebylocation");
		commands.add("gogo:bundlelevel");
		commands.add("equinox:headers");
		
		CommandSession session = createMock(CommandSession.class);
		expect(session.get(COMMANDS)).andReturn(commands).anyTimes();
		replay(session);
		
		scanner.setContext(context);
		scanner.setSession(session);
		
		scanner.scan(CR);
		scanner.scan(LF);
		scanner.scan('b');
		scanner.scan('u');
		scanner.scan('n');
		scanner.scan(TAB);
		res = byteOut.toString();
		Assert.assertTrue("Expected completion suggestion is not contained in the output", res.contains("bundles\r\n"));
		Assert.assertTrue("Expected completion suggestion is not contained in the output", res.contains("bundle\r\n"));
		Assert.assertTrue("bun should be completed to bundle", res.endsWith("bundle"));
		Assert.assertTrue("Expected completion suggestion is not contained in the output", res.contains("bundlebylocation\r\n"));
		Assert.assertTrue("Expected completion suggestion is not contained in the output", res.contains("bundlelevel\r\n"));
		Assert.assertFalse("Not expected completion suggestion", res.contains("headers\r\n"));
	}

	private static void addLine(ConsoleInputScanner scanner, byte[] line) throws Exception {
		for (byte b : line) {
			try {
				scanner.scan(b);
			} catch (Exception e) {
				System.out.println("Error scanning symbol " + b);
				throw new Exception("Error scanning symbol" + b);
			}
		}

		try {
			scanner.scan(CR);
		} catch (Exception e) {
			System.out.println("Error scanning symbol " + CR);
			throw new Exception("Error scanning symbol " + CR);
		}

		try {
			scanner.scan(LF);
		} catch (Exception e) {
			System.out.println("Error scanning symbol " + LF);
			throw new Exception("Error scanning symbol " + LF);
		}
	}

	private void add(ConsoleInputScanner scanner, byte[] sequence) throws Exception {
		scanner.scan(ESC);
		for (byte b : sequence) {
			scanner.scan(b);
		}
	}

	private void checkInpusStream(ConsoleInputStream in, byte[] expected) throws Exception {
		// the actual number of bytes in the stream is two more than the bytes in the array, because of the CR and LF
		// symbols, added after the array
		byte[] read = new byte[expected.length + 1];
		in.read(read, 0, read.length);
		for (int i = 0; i < expected.length; i++) {
			Assert.assertEquals("Incorrect char read. Position " + i + ", expected " + expected[i] + ", read " + read[i], expected[i], read[i]);
		}
	}

}
