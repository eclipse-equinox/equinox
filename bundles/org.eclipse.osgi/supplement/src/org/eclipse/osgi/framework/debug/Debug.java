/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.osgi.framework.debug;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Debug
{
    public static final boolean DEBUG = true;
    public static boolean DEBUG_GENERAL = false;		// "debug"
    public static boolean DEBUG_BUNDLE_TIME = false;  //"debug.bundleTime"
	public static boolean DEBUG_LOADER = false;			// "debug.loader"
	public static boolean DEBUG_EVENTS = false;			// "debug.events"
	public static boolean DEBUG_SERVICES = false;		// "debug.services"
	public static boolean DEBUG_PACKAGES = false;		// "debug.packages"
	public static boolean DEBUG_MANIFEST = false;		// "debug.manifest"
	public static boolean DEBUG_FILTER = false;			// "debug.filter"
	public static boolean DEBUG_SECURITY = false;		// "debug.security"
	public static boolean DEBUG_STARTLEVEL = false;		// "debug.startlevel"
	public static boolean DEBUG_PACKAGEADMIN = false;	// "debug.packageadmin"
	public static boolean DEBUG_PACKAGEADMIN_TIMING = false; //"debug.packageadmin/timing"
	static {
		DebugOptions dbgOptions = DebugOptions.getDefault();
		if (dbgOptions != null) {
			DEBUG_GENERAL = dbgOptions.getBooleanOption("debug", false);
			DEBUG_BUNDLE_TIME = dbgOptions.getBooleanOption("debug.bundleTime", false) || dbgOptions.getBooleanOption("org.eclipse.core.runtime/timing/startup", false);
			DEBUG_LOADER = dbgOptions.getBooleanOption("debug.loader",false);
			DEBUG_EVENTS = dbgOptions.getBooleanOption("debug.events",false);
			DEBUG_SERVICES = dbgOptions.getBooleanOption("debug.services",false);
			DEBUG_PACKAGES = dbgOptions.getBooleanOption("debug.packages",false);
			DEBUG_MANIFEST = dbgOptions.getBooleanOption("debug.manifest",false);
			DEBUG_FILTER = dbgOptions.getBooleanOption("debug.filter",false);
			DEBUG_SECURITY = dbgOptions.getBooleanOption("debug.security",false);
			DEBUG_STARTLEVEL = dbgOptions.getBooleanOption("debug.startlevel",false);
			DEBUG_PACKAGEADMIN = dbgOptions.getBooleanOption("debug.packageadmin",false) ;
			DEBUG_PACKAGEADMIN_TIMING = dbgOptions.getBooleanOption("debug.packageadmin/timing", false) || dbgOptions.getBooleanOption("org.eclipse.core.runtime/debug", false);
		}
	}
	public static PrintStream out = System.out;

    public static void print(boolean x)
    {
        out.print(x);
    }
    public static void print(char x)
    {
        out.print(x);
    }
    public static void print(int x)
    {
        out.print(x);
    }
    public static void print(long x)
    {
        out.print(x);
    }
    public static void print(float x)
    {
        out.print(x);
    }
    public static void print(double x)
    {
        out.print(x);
    }
    public static void print(char x[])
    {
        out.print(x);
    }
    public static void print(String x)
    {
        out.print(x);
    }
    public static void print(Object x)
    {
        out.print(x);
    }
    public static void println(boolean x)
    {
        out.println(x);
    }
    public static void println(char x)
    {
        out.println(x);
    }
    public static void println(int x)
    {
        out.println(x);
    }
    public static void println(long x)
    {
        out.println(x);
    }
    public static void println(float x)
    {
        out.println(x);
    }
    public static void println(double x)
    {
        out.println(x);
    }
    public static void println(char x[])
    {
        out.println(x);
    }
    public static void println(String x)
    {
        out.println(x);
    }
    public static void println(Object x)
    {
        out.println(x);
    }
    public static void printStackTrace(Throwable x)
    {
        printStackTrace(x,out);
    }
    private static void printStackTrace(Throwable t, PrintStream out)
    {
        t.printStackTrace(out);

        Method[] methods = t.getClass().getMethods();

        int size = methods.length;
        Class throwable = Throwable.class;

        for (int i = 0; i < size; i++)
        {
            Method method = methods[i];

            if (Modifier.isPublic(method.getModifiers()) &&
                method.getName().startsWith("get") &&
                throwable.isAssignableFrom(method.getReturnType()) &&
                (method.getParameterTypes().length == 0))
            {
                try
                {
                    Throwable nested = (Throwable) method.invoke(t, null);

                    if ((nested != null) && (nested != t))
                    {
                        out.println("Nested Exception:");
                        printStackTrace(nested, out);
                    }
                }
                catch (IllegalAccessException e)
                {
                }
                catch (InvocationTargetException e)
                {
                }
            }
        }
    }
}
