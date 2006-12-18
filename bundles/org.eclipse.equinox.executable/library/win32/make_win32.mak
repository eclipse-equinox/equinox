#******************************************************************************
# Copyright (c) 2000, 2006 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at 
# http://www.eclipse.org/legal/epl-v10.html
# 
# Contributors:
#     IBM Corporation - initial API and implementation
#     Kevin Cornell (Rational Software Corporation)
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
!include <ntwin32.mak>
!include <..\make_version.mak>

PROGRAM_LIBRARY = eclipse_$(LIB_VERSION).dll

# Define the object modules to be compiled and flags.
MAIN_OBJS = eclipseMain.obj  aeclipseMain.obj  
COMMON_OBJS = eclipseConfig.obj eclipseCommon.obj   eclipseWinCommon.obj\
			  aeclipseConfig.obj aeclipseCommon.obj  aeclipseWinCommon.obj
DLL_OBJS	= eclipse.obj  eclipseWin.obj  eclipseUtil.obj  eclipseJNI.obj\
	  		  aeclipse.obj aeclipseWin.obj aeclipseUtil.obj aeclipseJNI.obj

LIBS   = kernel32.lib user32.lib comctl32.lib msvcrt.lib
DLL_LIBS = kernel32.lib user32.lib comctl32.lib gdi32.lib Advapi32.lib msvcrt.lib
LFLAGS = /NODEFAULTLIB /INCREMENTAL:NO /RELEASE /NOLOGO -subsystem:windows,4.0 -entry:wmainCRTStartup
DLL_LFLAGS = /NODEFAULTLIB /INCREMENTAL:NO /PDB:NONE /RELEASE /NOLOGO -entry:_DllMainCRTStartup@12 -dll /BASE:0x10000000 /DLL
RES    = eclipse.res
EXEC   = eclipse.exe
DLL    = $(PROGRAM_LIBRARY)
DEBUG  = #$(cdebug)
acflags = -I.. -DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	-I$(JAVA_HOME)\include -I$(JAVA_HOME)\include\win32 \
	$(cflags)
wcflags = -DUNICODE $(acflags)
all: $(EXEC) $(DLL)

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

aeclipseJNI.obj: ../eclipseCommon.h ../eclipseOS.h ../eclipseJNI.c
	$(cc) $(DEBUG) $(acflags) $(cvarsdll) /FoaeclipseJNI.obj ../eclipseJNI.c
		
aeclipseMain.obj: ../eclipseUnicode.h ../eclipseCommon.h ../eclipseMain.c 
	$(cc) $(DEBUG) $(acflags) $(cvarsdll) /FoaeclipseMain.obj ../eclipseMain.c
	
aeclipseCommon.obj: ../eclipseCommon.h ../eclipseUnicode.h ../eclipseCommon.c
	$(cc) $(DEBUG) $(acflags) $(cvarsdll) /FoaeclipseCommon.obj ../eclipseCommon.c

aeclipse.obj: ../eclipseOS.h ../eclipseUnicode.h ../eclipse.c
    $(cc) $(DEBUG) $(acflags) $(cvarsdll) /Foaeclipse.obj ../eclipse.c

aeclipseUtil.obj: ../eclipseUtil.h ../eclipseUnicode.h ../eclipseUtil.c
    $(cc) $(DEBUG) $(acflags) $(cvarsdll) /FoaeclipseUtil.obj ../eclipseUtil.c

aeclipseConfig.obj: ../eclipseConfig.h ../eclipseConfig.h ../eclipseConfig.c
    $(cc) $(DEBUG) $(acflags) $(cvarsdll) /FoaeclipseConfig.obj ../eclipseConfig.c
    
aeclipseWin.obj: ../eclipseOS.h ../eclipseUnicode.h eclipseWin.c
    $(cc) $(DEBUG) $(acflags) $(cvarsdll) /FoaeclipseWin.obj eclipseWin.c

aeclipseWinCommon.obj: ../eclipseCommon.h eclipseWinCommon.c
    $(cc) $(DEBUG) $(acflags) $(cvarsdll) /FoaeclipseWinCommon.obj eclipseWinCommon.c

$(EXEC): $(MAIN_OBJS) $(COMMON_OBJS) $(RES)
    $(link) $(LFLAGS) -out:$(PROGRAM_OUTPUT) $(MAIN_OBJS) $(COMMON_OBJS) $(RES) $(LIBS)

$(DLL): $(DLL_OBJS) $(COMMON_OBJS)
    $(link) $(DLL_LFLAGS) -out:$(PROGRAM_LIBRARY) $(DLL_OBJS) $(COMMON_OBJS) $(RES) $(DLL_LIBS)

$(RES): eclipse.rc
    $(rc) -r -fo $(RES) eclipse.rc

install: all
	copy $(EXEC) $(OUTPUT_DIR)
	del -f $(EXEC) $(MAIN_OBJS) $(DLL_OBJS) $(COMMON_OBJS) $(RES)
   
clean:
	del $(EXEC) $(DLL)$(MAIN_OBJS) $(DLL_OBJS) $(COMMON_OBJS) $(RES)
