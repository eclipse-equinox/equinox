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
# DEFAULT_OS      - the default value of the "-os" switch
# DEFAULT_OS_ARCH - the default value of the "-arch" switch
# DEFAULT_WS      - the default value of the "-ws" switch

!include <ntwin32.mak>

# Define the object modules to be compiled and flags.
MAIN_OBJS = eclipseMain.obj  aeclipseMain.obj  
COMMON_OBJS = eclipseConfig.obj eclipseCommon.obj   eclipseWinCommon.obj\
			  aeclipseConfig.obj aeclipseCommon.obj  aeclipseWinCommon.obj
DLL_OBJS	= eclipse.obj  eclipseWin.obj  eclipseUtil.obj  eclipseJNI.obj\
	  		  aeclipse.obj aeclipseWin.obj aeclipseUtil.obj aeclipseJNI.obj

OBJS   = eclipse.obj eclipseWin.obj eclipseShm.obj eclipseConfig.obj eclipseUtil.obj aeclipse.obj aeclipseWin.obj aeclipseShm.obj aeclipseConfig.obj aeclipseUtil.obj
LIBS   = kernel32.lib user32.lib gdi32.lib comctl32.lib
LFLAGS = /INCREMENTAL:NO /NOLOGO -subsystem:windows,4.0 -entry:wmainCRTStartup
DLL_LFLAGS = /INCREMENTAL:NO /PDB:NONE /RELEASE /NOLOGO -entry:_DllMainCRTStartup@12 -dll /BASE:0x10000000 /DLL
RES    = eclipse.res
EXEC   = eclipse.exe
DLL    = eclipse_1.exe
DEBUG  = #$(cdebug)
acflags = -I.. -DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	/I$(JAVA_HOME)\include /I$(JAVA_HOME)\include\win32 \
	$(cflags)
wcflags = -DUNICODE $(acflags)
	
all: $(EXEC) $(DLL)

eclipseMain.obj: ../eclipseUnicode.h ../eclipseCommon.h ../eclipseMain.c 
	$(cc) $(DEBUG) $(wcflags) $(cvars) /Fo$*.obj ../eclipseMain.c

eclipseCommon.obj: ../eclipseCommon.h ../eclipseUnicode.h ../eclipseCommon.c
	$(cc) $(DEBUG) $(wcflags) $(cvars) /Fo$*.obj ../eclipseCommon.c

eclipse.obj: ../eclipseOS.h ../eclipseUnicode.h ../eclipse.c
    $(cc) $(DEBUG) $(wcflags) $(cvars) /Fo$*.obj ../eclipse.c

eclipseUtil.obj: ../eclipseUtil.h ../eclipseUnicode.h ../eclipseUtil.c
    $(cc) $(DEBUG) $(wcflags) $(cvars) /Fo$*.obj ../eclipseUtil.c

eclipseConfig.obj: ../eclipseConfig.h ../eclipseUnicode.h ../eclipseConfig.c
    $(cc) $(DEBUG) $(wcflags) $(cvars) /Fo$*.obj ../eclipseConfig.c

eclipseWin.obj: ../eclipseOS.h ../eclipseUnicode.h eclipseWin.c
    $(cc) $(DEBUG) $(wcflags) $(cvars) /Fo$*.obj eclipseWin.c

eclipseWinCommon.obj: ../eclipseCommon.h eclipseWinCommon.c
    $(cc) $(DEBUG) $(wcflags) $(cvars) /Fo$*.obj eclipseWinCommon.c

aeclipseMain.obj: ../eclipseUnicode.h ../eclipseCommon.h ../eclipseMain.c 
	$(cc) $(DEBUG) $(acflags) $(cvars) /FoaeclipseMain.obj ../eclipseMain.c
	
aeclipseCommon.obj: ../eclipseCommon.h ../eclipseUnicode.h ../eclipseCommon.c
	$(cc) $(DEBUG) $(acflags) $(cvars) /FoaeclipseCommon.obj ../eclipseCommon.c

aeclipse.obj: ../eclipseOS.h ../eclipseUnicode.h ../eclipse.c
    $(cc) $(DEBUG) $(acflags) $(cvars) /Foaeclipse.obj ../eclipse.c

aeclipseUtil.obj: ../eclipseUtil.h ../eclipseUnicode.h ../eclipseUtil.c
    $(cc) $(DEBUG) $(acflags) $(cvars) /FoaeclipseUtil.obj ../eclipseUtil.c

aeclipseConfig.obj: ../eclipseConfig.h ../eclipseConfig.h ../eclipseConfig.c
    $(cc) $(DEBUG) $(acflags) $(cvars) /FoaeclipseConfig.obj ../eclipseConfig.c
    
aeclipseWin.obj: ../eclipseOS.h ../eclipseUnicode.h eclipseWin.c
    $(cc) $(DEBUG) $(acflags) $(cvars) /FoaeclipseWin.obj eclipseWin.c

aeclipseWinCommon.obj: ../eclipseCommon.h eclipseWinCommon.c
    $(cc) $(DEBUG) $(acflags) $(cvars) /FoaeclipseCommon.obj eclipseWinCommon.c

$(EXEC): $(MAIN_OBJS) $(COMMON_OBJS) $(RES)
    $(link) $(LFLAGS) -out:$(PROGRAM_OUTPUT) $(MAIN_OBJS) $(COMMON_OBJS) $(RES) $(LIBS)

$(DLL): $(DLL_OBJS) $(COMMON_OBJS)
    $(link) $(DLL_LFLAGS) -out:$(PROGRAM_OUTPUT) $(DLL_OBJS) $(COMMON_OBJS) $(RES) $(LIBS)

$(RES): eclipse.rc
    $(rc) -r -fo $(RES) eclipse.rc

install: all
	copy $(EXEC) $(OUTPUT_DIR)
	del -f $(EXEC) $(OBJS) $(RES)
   
clean:
	del $(EXEC) $(OBJS) $(RES)
