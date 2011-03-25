/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lazar Kirchev, SAP AG - initial API and implementation
 *******************************************************************************/
package test;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

public class ConsoleTestCommandProvider implements CommandProvider {

	public void _echo(CommandInterpreter intp) {
		intp.println(intp.nextArgument());
	}

	public void _cust_exec(CommandInterpreter intp) {
		String nextArg = intp.nextArgument();
		String innerCommand = null;
		if (nextArg != null) {
			intp.println("Customized execution of command " + nextArg);
			StringBuffer builder = new StringBuffer();
			while (nextArg != null) {
				builder.append(' ');
				builder.append(nextArg);
				nextArg = intp.nextArgument();
			}
			innerCommand = builder.toString().trim();
			intp.execute(innerCommand);
		}
	}

	public String getHelp() {
		StringBuffer help = new StringBuffer();
		help.append("---");
		help.append("Custom commands");
		help.append("---");
		help.append("\r\n");
		help.append("\t");
		help.append("echo - echos input");
		help.append("\r\n");
		help.append("\t");
		help.append("cust_exec - executes the command, passed as an argument");
		help.append("\r\n");
		return help.toString();
	}

}
