#******************************************************************************
# Copyright (c) 2007, 2021 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at 
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
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
# EXE_OUTPUT_DIR  - the location into which the executable is installed (only used in 'install' target)
# LIB_OUTPUT_DIR  - the location into which the launcher library is installed (only used in 'install' target)
NODEBUG=1

APPVER=4.0
_WIN32_WINNT=0x0400
_WIN32_IE=0x0300

!include <..\make_version.mak>

PROGRAM_OUTPUT=eclipse.exe
# Separate filename from extention
PROGRAM_NAME=$(PROGRAM_OUTPUT:.exe=)

PROGRAM_LIBRARY = eclipse_$(LIB_VERSION).dll

EXE_OUTPUT_DIR=$(EXE_OUTPUT_DIR:/=\)
LIB_OUTPUT_DIR=$(LIB_OUTPUT_DIR:/=\)

# Define the object modules to be compiled and flags.
MAIN_OBJS = eclipseMain.obj
MAIN_CONSOLE_OBJS = eclipseMainConsole.obj
COMMON_OBJS = eclipseConfig.obj eclipseCommon.obj   eclipseWinCommon.obj
DLL_OBJS	= eclipse.obj  eclipseWin.obj  eclipseUtil.obj  eclipseJNI.obj eclipseShm.obj

cdebug	= -Ox -DNDEBUG
cvarsmt	= -D_MT -MT
cflags	= $(cdebug) -DUNICODE -D_UNICODE -DCOBJMACROS /c -DUSE_ASSEMBLER -GS -DWIN64 -D_WIN64 $(cvarsmt) $(CFLAGS)
LIBS   = kernel32.lib user32.lib comctl32.lib libcmt.lib \
	libvcruntime.lib libucrt.lib ole32.lib windowscodecs.lib
DLL_LIBS = kernel32.lib user32.lib comctl32.lib gdi32.lib Advapi32.lib libcmt.lib version.lib \
	libvcruntime.lib libucrt.lib ole32.lib windowscodecs.lib
LFLAGS = /DYNAMICBASE /NXCOMPAT /HIGHENTROPYVA /NODEFAULTLIB /INCREMENTAL:NO /RELEASE /NOLOGO -subsystem:windows -entry:wmainCRTStartup
CONSOLEFLAGS = /DYNAMICBASE /NXCOMPAT /HIGHENTROPYVA /NODEFAULTLIB /INCREMENTAL:NO /RELEASE /NOLOGO -subsystem:console -entry:wmainCRTStartup
#DLL_LFLAGS = /NODEFAULTLIB /INCREMENTAL:NO /PDB:NONE /RELEASE /NOLOGO -entry:_DllMainCRTStartup@12 -dll /BASE:0x72000000 /DLL
DLL_LFLAGS = /DYNAMICBASE /NXCOMPAT /HIGHENTROPYVA /NODEFAULTLIB /INCREMENTAL:NO /PDB:NONE /RELEASE /NOLOGO  -dll /BASE:0x140000000 /DLL
RES	= $(PROGRAM_NAME).res
EXEC	= $(PROGRAM_OUTPUT)
CONSOLE = $(PROGRAM_NAME)c.exe
DLL    = $(PROGRAM_LIBRARY)
DEBUG  = #$(cdebug)
wcflags = -DUNICODE -I.. -DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	-I"$(JAVA_HOME)\include" -I"$(JAVA_HOME)\include\win32" \
	$(cflags)
all: $(EXEC) $(DLL) $(CONSOLE)

eclipseMain.obj: ../eclipseUnicode.h ../eclipseCommon.h ../eclipseMain.c 
	cl $(DEBUG) $(wcflags) $(cvarsmt) /Fo$*.obj ../eclipseMain.c

eclipseMainConsole.obj: ../eclipseUnicode.h ../eclipseCommon.h ../eclipseMain.c 
	cl $(DEBUG) $(wcflags) $(cvarsmt) -D_WIN32_CONSOLE /Fo$*.obj ../eclipseMain.c

eclipseCommon.obj: ../eclipseCommon.h ../eclipseUnicode.h ../eclipseCommon.c
	cl $(DEBUG) $(wcflags) $(cvarsmt) /Fo$*.obj ../eclipseCommon.c

eclipse.obj: ../eclipseOS.h ../eclipseUnicode.h ../eclipse.c
    cl $(DEBUG) $(wcflags) $(cvarsmt) /Fo$*.obj ../eclipse.c

eclipseUtil.obj: ../eclipseUtil.h ../eclipseUnicode.h ../eclipseUtil.c
    cl $(DEBUG) $(wcflags) $(cvarsmt) /Fo$*.obj ../eclipseUtil.c

eclipseConfig.obj: ../eclipseConfig.h ../eclipseUnicode.h ../eclipseConfig.c
    cl $(DEBUG) $(wcflags) $(cvarsmt) /Fo$*.obj ../eclipseConfig.c

eclipseWin.obj: ../eclipseOS.h ../eclipseUnicode.h eclipseWin.c
    cl $(DEBUG) $(wcflags) $(cvarsmt) /Fo$*.obj eclipseWin.c

eclipseWinCommon.obj: ../eclipseCommon.h eclipseWinCommon.c
    cl $(DEBUG) $(wcflags) $(cvarsmt) /Fo$*.obj eclipseWinCommon.c

eclipseJNI.obj: ../eclipseCommon.h ../eclipseOS.h ../eclipseJNI.c
	cl $(DEBUG) $(wcflags) $(cvarsmt) /Fo$*.obj ../eclipseJNI.c

eclipseShm.obj: ../eclipseShm.h ../eclipseUnicode.h ../eclipseShm.c
	cl $(DEBUG) $(wcflags) $(cvarsmt) /Fo$*.obj ../eclipseShm.c

$(EXEC): $(MAIN_OBJS) $(COMMON_OBJS) $(RES)
    link $(LFLAGS) -out:$(PROGRAM_OUTPUT) $(MAIN_OBJS) $(COMMON_OBJS) $(RES) $(LIBS)

$(CONSOLE): $(MAIN_CONSOLE_OBJS) $(COMMON_OBJS)
    link $(CONSOLEFLAGS) -out:$(CONSOLE) $(MAIN_CONSOLE_OBJS) $(COMMON_OBJS) $(LIBS)

$(DLL): $(DLL_OBJS) $(COMMON_OBJS)
    link $(DLL_LFLAGS) -out:$(PROGRAM_LIBRARY) $(DLL_OBJS) $(COMMON_OBJS) $(DLL_LIBS)

$(RES): $(PROGRAM_NAME).rc
    rc -r -fo $(RES) eclipse.rc

install: all
	@echo Install into: EXE_OUTPUT_DIR:$(EXE_OUTPUT_DIR) LIB_OUTPUT_DIR:$(LIB_OUTPUT_DIR)
	-1cmd /c "mkdir $(EXE_OUTPUT_DIR)"
	move /y $(EXEC) $(EXE_OUTPUT_DIR)
	move /y $(CONSOLE) $(EXE_OUTPUT_DIR)
	-1cmd /c "mkdir $(LIB_OUTPUT_DIR)"
	del $(LIB_OUTPUT_DIR)\eclipse_*.dll
	move /y $(DLL) $(LIB_OUTPUT_DIR)
	del $(MAIN_OBJS) $(MAIN_CONSOLE_OBJS) $(DLL_OBJS) $(COMMON_OBJS) $(RES) eclipse_*.exp eclipse_*.lib

clean:
	del $(EXEC) $(CONSOLE) $(DLL) $(MAIN_OBJS) $(MAIN_CONSOLE_OBJS) $(DLL_OBJS) $(COMMON_OBJS) $(RES) eclipse_*.exp eclipse_*.lib
