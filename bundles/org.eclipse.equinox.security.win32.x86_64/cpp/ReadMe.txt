/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

This is a JNI bridge to access native Windows encryption methods from Java. This version
works in a 64-bit Windows environment.

The methods perform user-specific encryption of the data. The same user can later decrypt 
data using methods provided by this DLL. A different user won't be able to decrypt the data. 

If the user has a roaming profile, he can decrypt data on a different computer in the domain.

In the event if stand-alone computer needs to have OS re-installed (or the domain controller
and the computer in the domain), be sure to create Windows password recovery disk BEFORE 
re-installing the operating system.

Note that this mechanism is intended to be used with small size data (i.e., passwords). For 
large amount of data consider encrypting your password using this mechanism and using 
symmetric encryption to encrypt the data.

To compile this DLL:
=> JAVA_HOME environment variable needs to be setup so that jni.h can be found

Note C++ projects settings:
=> Additional include directories - "$(JAVA_HOME)/include";"$(JAVA_HOME)/include/win32"
=> Additional linker dependency - Crypt32.lib
