#******************************************************************************
# Copyright (c) 2007, 2009 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at 
# http://www.eclipse.org/legal/epl-v10.html
# 
# Contributors:
#     IBM Corporation - initial API and implementation
#*******************************************************************************

# Makefile for creating the eclipse launcher program.

# This makefile expects the following environment variables set:
#
# PROGRAM_OUTPUT  - the filename of the output executable
# PROGRAM_LIBRARY - the filename of the output dll library
# DEFAULT_OS      - the default value of the "-os" switch
# DEFAULT_OS_ARCH - the default value of the "-arch" switch
# DEFAULT_WS      - the default value of the "-ws" switch
# JAVA_HOME       - the location of the Java for JNI includes
NODEBUG=1

APPVER=4.0
_WIN32_WINNT=0x0400
_WIN32_IE=0x0300

!include <ntwin32.mak>
!include <..\make_version.mak>

PROGRAM_OUTPUT=eclipse.exe
# Separate filename from extention
PROGRAM_NAME=$(PROGRAM_OUTPUT:.exe=)

PROGRAM_LIBRARY = eclipse_$(LIB_VERSION).dll

# Define the object modules to be compiled and flags.
MAIN_OBJS = eclipseMain.obj
COMMON_OBJS = eclipseConfig.obj eclipseCommon.obj   eclipseWinCommon.obj
DLL_OBJS	= eclipse.obj  eclipseWin.obj  eclipseUtil.obj  eclipseJNI.obj eclipseShm.obj

LIBS   = kernel32.lib user32.lib comctl32.lib msvcrt.lib bufferoverflowU.lib
DLL_LIBS = kernel32.lib user32.lib comctl32.lib gdi32.lib Advapi32.lib msvcrt.lib version.lib bufferoverflowU.lib
LFLAGS = /NODEFAULTLIB /INCREMENTAL:NO /RELEASE /NOLOGO -subsystem:windows -entry:wmainCRTStartup
CONSOLEFLAGS = /NODEFAULTLIB /INCREMENTAL:NO /RELEASE /NOLOGO -subsystem:console -entry:wmainCRTStartup
#DLL_LFLAGS = /NODEFAULTLIB /INCREMENTAL:NO /PDB:NONE /RELEASE /NOLOGO -entry:_DllMainCRTStartup@12 -dll /BASE:0x72000000 /DLL
DLL_LFLAGS = /NODEFAULTLIB /INCREMENTAL:NO /PDB:NONE /RELEASE /NOLOGO  -dll /BASE:0x72000000 /DLL
RES	= $(PROGRAM_NAME).res
EXEC	= $(PROGRAM_OUTPUT)
CONSOLE = $(PROGRAM_NAME)c.exe
DLL    = $(PROGRAM_LIBRARY)
DEBUG  = #$(cdebug)
wcflags = -DUNICODE -I.. -DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	-I$(JAVA_HOME)\include -I$(JAVA_HOME)\include\win32 \
	$(cflags)
all: $(EXEC) $(DLL) $(CONSOLE)

eclipseMain.obj: ../eclipseUnicode.h ../eclipseCommon.h ../eclipseMain.c 
	$(cc) $(DEBUG) $(wcflags) $(cvarsdll) /Fo$*.obj ../eclipseMain.c

eclipseCommon.obj: ../eclipseCommon.h ../eclipseUnicode.h ../eclipseCommon.c
	$(cc) $(DEBUG) $(wcflags) $(cvarsdll) /Fo$*.obj ../eclipseCommon.c

eclipse.obj: ../eclipseOS.h ../eclipseUnicode.h ../eclipse.c
    $(cc) $(DEBUG) $(wcflags) $(cvarsdll) /Fo$*.obj ../eclipse.c

eclipseUtil.obj: ../eclipseUtil.h ../eclipseUnicode.h ../eclipseUtil.c
    $(cc) $(DEBUG) $(wcflags) $(cvarsdll) /Fo$*.obj ../eclipseUtil.c

eclipseConfig.obj: ../eclipseConfig.h ../eclipseUnicode.h ../eclipseConfig.c
    $(cc) $(DEBUG) $(wcflags) $(cvarsdll) /Fo$*.obj ../eclipseConfig.c

eclipseWin.obj: ../eclipseOS.h ../eclipseUnicode.h eclipseWin.c
    $(cc) $(DEBUG) $(wcflags) $(cvarsdll) /Fo$*.obj eclipseWin.c

eclipseWinCommon.obj: ../eclipseCommon.h eclipseWinCommon.c
    $(cc) $(DEBUG) $(wcflags) $(cvarsdll) /Fo$*.obj eclipseWinCommon.c

eclipseJNI.obj: ../eclipseCommon.h ../eclipseOS.h ../eclipseJNI.c
	$(CC) $(DEBUG) $(wcflags) $(cvarsdll) /Fo$*.obj ../eclipseJNI.c

eclipseShm.obj: ../eclipseShm.h ../eclipseUnicode.h ../eclipseShm.c
	$(CC) $(DEBUG) $(wcflags) $(cvarsdll) /Fo$*.obj ../eclipseShm.c
	
$(EXEC): $(MAIN_OBJS) $(COMMON_OBJS) $(RES)
    $(link) $(LFLAGS) -out:$(PROGRAM_OUTPUT) $(MAIN_OBJS) $(COMMON_OBJS) $(RES) $(LIBS)

#the console version needs a flag set, should look for a better way to do this
$(CONSOLE): $(MAIN_OBJS) $(COMMON_OBJS)
	del -f eclipseConfig.obj aeclipseConfig.obj
	$(cc) $(DEBUG) $(wcflags) $(cvarsdll) -D_WIN32_CONSOLE /FoeclipseConfig.obj ../eclipseConfig.c
    $(link) $(CONSOLEFLAGS) -out:$(CONSOLE) $(MAIN_OBJS) $(COMMON_OBJS) $(LIBS)

$(DLL): $(DLL_OBJS) $(COMMON_OBJS)
    $(link) $(DLL_LFLAGS) -out:$(PROGRAM_LIBRARY) $(DLL_OBJS) $(COMMON_OBJS) $(DLL_LIBS)

$(RES): $(PROGRAM_NAME).rc
    $(rc) -r -fo $(RES) eclipse.rc

install: all
	copy $(EXEC) $(OUTPUT_DIR)
	del -f $(EXEC) $(MAIN_OBJS) $(DLL_OBJS) $(COMMON_OBJS) $(RES)
   
clean:
	del $(EXEC) $(DLL) $(MAIN_OBJS) $(DLL_OBJS) $(COMMON_OBJS) $(RES)
