/*******************************************************************************
 * Copyright (c) 2003, 2017 IBM Corporation and others.
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
 *     Lazar Kirchev, SAP AG - derivative implementation from FrameworkCommandInterpreter
 *******************************************************************************/
package org.eclipse.equinox.console.command.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.service.command.CommandSession;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.osgi.framework.Bundle;

/**
 * A CommandInterpreter to be passed to the legacy Equinox commands,
 * executed by the CommandProviderAdapter.
 *
 */
public class CustomCommandInterpreter implements CommandInterpreter {
  private PrintStream out = System.out;
  /** Strings used to format other strings */
  private String tab = "\t"; //$NON-NLS-1$
  private String newline = "\r\n"; //$NON-NLS-1$
  private final Iterator<Object> arguments;
  private final CommandSession commandSession;
  /**
   * The maximum number of lines to print without user prompt.
   * 0 means no user prompt is required, the window is scrollable.
   */
  protected static int maxLineCount;

  /** The number of lines printed without user prompt.*/
  protected int currentLineCount;
  
  public CustomCommandInterpreter(CommandSession commandSession, List<Object> args) {
	  this.commandSession = commandSession;
	  arguments = args.iterator();
  }
  
  @Override
public Object execute(String cmd) {
	  try {
		  return commandSession.execute(cmd);
	  } catch (RuntimeException e) {
		  throw e;
	  } catch (Exception e) {
		  throw new RuntimeException(e);
	  } 
  }

  @Override
public String nextArgument() {
    if (arguments.hasNext()) {
    	Object next = arguments.next();
    	return next == null ? null : next.toString();
    }
    return null;
  }

  /**
   * Prints an object to the outputstream
   *
   * @param o the object to be printed
   */
  @Override
public void print(Object o) {
      check4More();
      out.print(o);
      out.flush();    
  }

  /**
   * Prints a empty line to the outputstream
   */
  @Override
public void println() {
    println(""); //$NON-NLS-1$
  }

  /**
   * Print a stack trace including nested exceptions.
   * @param t The offending exception
   */
  @Override
public void printStackTrace(Throwable t) {
    t.printStackTrace(out);

    Method[] methods = t.getClass().getMethods();

    int size = methods.length;
    Class<Throwable> throwable = Throwable.class;

    for (int i = 0; i < size; i++) {
      Method method = methods[i];

      if (Modifier.isPublic(method.getModifiers()) && method.getName().startsWith("get") && throwable.isAssignableFrom(method.getReturnType()) && (method.getParameterTypes().length == 0)) { //$NON-NLS-1$
        try {
          Throwable nested = (Throwable) method.invoke(t, (Object) null);

          if ((nested != null) && (nested != t)) {
            out.println("Nested Exception");
            printStackTrace(nested);
          }
        } catch (IllegalAccessException | InvocationTargetException e) {
        }
      }
    }
  }

  /**
   * Prints an object to the output medium (appended with newline character).
   * <p>
   * If running on the target environment, the user is prompted with '--more'
   * if more than the configured number of lines have been printed without user prompt.
   * This enables the user of the program to have control over scrolling.
   * <p>
   * For this to work properly you should not embed "\n" etc. into the string.
   *
   * @param o the object to be printed
   */
  @Override
public void println(Object o) {
    if (o == null) {
      return;
    }
    synchronized (out) {
      check4More();
      printline(o);
      currentLineCount++;
      currentLineCount += o.toString().length() / 80;
    }
  }
  
  /**
   * Prints a string to the output medium (appended with newline character).
   * <p>
   * This method does not increment the line counter for the 'more' prompt.
   *
   * @param o the string to be printed
   */
  private void printline(Object o) {
    print(o + newline);
  }

  /**
   * Prints the given dictionary sorted by keys.
   *
   * @param dic the dictionary to print
   * @param title the header to print above the key/value pairs
   */
  @Override
public void printDictionary(Dictionary<?,?> dic, String title) {
    if (dic == null)
      return;

    int count = dic.size();
    String[] keys = new String[count];
    Enumeration<?> keysEnum = dic.keys();
    int i = 0;
    while (keysEnum.hasMoreElements()) {
      keys[i++] = (String) keysEnum.nextElement();
    }
    Arrays.sort(keys);

    if (title != null) {
      println(title);
    }
    for (i = 0; i < count; i++) {
      println(" " + keys[i] + " = " + dic.get(keys[i])); //$NON-NLS-1$//$NON-NLS-2$
    }
    println();
  }

  /**
   * Prints the given bundle resource if it exists
   *
   * @param bundle  the bundle containing the resource
   * @param resource  the resource to print
   */
  @Override
public void printBundleResource(Bundle bundle, String resource) {
    URL entry = null;
    entry = bundle.getEntry(resource);
    if (entry != null) {
      try {
        println(resource);
        byte[] buffer = new byte[1024];
        int read = 0;
        try (InputStream in = entry.openStream()) {
          while ((read = in.read(buffer)) != -1)
            print(new String(buffer, 0, read));
        }
      } catch (Exception e) {
        System.err.println(e);
      }
    } else {
      println("CONSOLE_RESOURCE ["+resource+"] NOT_IN_BUNDLE " + bundle.toString());
    }
  }
  
  /**
   * Answers the number of lines output to the console
   * window should scroll without user interaction.
   *
   * @return  The number of lines to scroll.
   */
  private int getMaximumLinesToScroll() {
    return maxLineCount;
  }

  /**
   *  Displays the more... prompt if the max line count has been reached 
   *  and waits for the operator to hit enter.
   *
   */
  private void check4More() {
    int max = getMaximumLinesToScroll();
    if (max > 0) {
      if (currentLineCount >= max) {
        out.print("-- More...Press Enter to Continue...");
        out.flush();
        try {
          System.in.read();
        } catch (IOException e) {
          e.printStackTrace();
        } // wait for user entry
        resetLineCount(); //Reset the line counter for the 'more' prompt
      }
    }
  }

  /**
   * Resets the line counter for the 'more' prompt.
   */
  private void resetLineCount() {
    currentLineCount = 0;
  }
  
  /**
   Answer a string (may be as many lines as you like) with help
   texts that explain the command.
   */
  public String getHelp() {
    StringBuffer help = new StringBuffer(256);
    help.append("---Controlling the Console---");
    help.append(newline);
    help.append(tab);
    help.append("more - "); //$NON-NLS-1$
    help.append("More prompt for console output");
    help.append(newline);
    help.append(tab);
    help.append("disconnect - "); //$NON-NLS-1$
    help.append("isconnects from telnet session");
    help.append(newline);
    return help.toString();
  }

}
