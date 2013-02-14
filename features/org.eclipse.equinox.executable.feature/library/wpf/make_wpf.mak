#******************************************************************************
# Copyright (c) 2000, 2007 IBM Corporation and others.
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
!include <..\make_version.mak>

PROGRAM_LIBRARY = eclipse_$(LIB_VERSION).dll
PROGRAM_OUTPUT=eclipse.exe
# Separate filename from extention
PROGRAM_NAME=$(PROGRAM_OUTPUT:.exe=)

# Define the object modules to be compiled and flags.
MAIN_OBJS = eclipseMain.obj  
COMMON_OBJS = eclipseConfig.obj eclipseCommon.obj  eclipseWpfCommon.obj
DLL_OBJS	= eclipse.obj  eclipseWpf.obj  eclipseUtil.obj  eclipseJNI.obj eclipseShm.obj

LIBS   = kernel32.lib msvcrt.lib mscoree.lib
DLL_LIBS = kernel32.lib Advapi32.lib msvcrt.lib version.lib
LFLAGS =  -CLRTHREADATTRIBUTE:STA /NODEFAULTLIB:LIBCMT /INCREMENTAL:NO /LARGEADDRESSAWARE /RELEASE /NOLOGO -subsystem:windows,4.0 -entry:wmainCRTStartup
CONSOLEFLAGS =  -CLRTHREADATTRIBUTE:STA /NODEFAULTLIB:LIBCMT /INCREMENTAL:NO /LARGEADDRESSAWARE /RELEASE /NOLOGO -subsystem:console,4.0 -entry:wmainCRTStartup
DLL_LFLAGS = -CLRTHREADATTRIBUTE:STA /NODEFAULTLIB:LIBCMT /INCREMENTAL:NO /LARGEADDRESSAWARE /PDB:NONE -dll /BASE:0x72000000 /DLL
RES    = $(PROGRAM_NAME).res
EXEC   = $(PROGRAM_OUTPUT)
CONSOLE = $(PROGRAM_NAME)c.exe
DLL    = $(PROGRAM_LIBRARY)
DEBUG  = #$(cdebug)

CFLAGS = -c -DUNICODE -DVISTA -D_CRT_SECURE_NO_DEPRECATE -D_CRT_NON_CONFORMING_SWPRINTFS -I.. -DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	-I$(JAVA_HOME)\include -I$(JAVA_HOME)\include\win32 \
	$(cflags)
	
WPF_HOME = C:\Program Files\Reference Assemblies\Microsoft\Framework\v3.0
DOTNET_HOME = C:\WINDOWS\Microsoft.NET\Framework\v2.0.50727
CPPFLAGS = -clr /FU"$(WPF_HOME)\PresentationCore.dll" /FU"$(WPF_HOME)\PresentationFramework.dll" /FU$(DOTNET_HOME)\System.Data.dll /FU$(DOTNET_HOME)\System.dll /FU$(DOTNET_HOME)\System.Xml.dll /FU"$(WPF_HOME)\UIAutomationProvider.dll" /FU"$(WPF_HOME)\UIAutomationTypes.dll" /FU"$(WPF_HOME)\WindowsBase.dll"

	
all: $(EXEC) $(DLL) com $(CONSOLE)

eclipseMain.obj: ../eclipseUnicode.h ../eclipseCommon.h ../eclipseMain.c 
	$(CC) $(DEBUG) $(CFLAGS) $(cvarsdll) /Fo$*.obj ../eclipseMain.c

eclipseCommon.obj: ../eclipseCommon.h ../eclipseUnicode.h ../eclipseCommon.c
	$(CC) $(DEBUG) $(CFLAGS) $(cvarsdll) /Fo$*.obj ../eclipseCommon.c

eclipse.obj: ../eclipseOS.h ../eclipseUnicode.h ../eclipse.c
    $(CC) $(DEBUG) $(CFLAGS) $(cvarsdll) /Fo$*.obj ../eclipse.c

eclipseUtil.obj: ../eclipseUtil.h ../eclipseUnicode.h ../eclipseUtil.c
    $(CC) $(DEBUG) $(CFLAGS) $(cvarsdll) /Fo$*.obj ../eclipseUtil.c

eclipseConfig.obj: ../eclipseConfig.h ../eclipseUnicode.h ../eclipseConfig.c
    $(CC) $(DEBUG) $(CFLAGS) $(cvarsdll) /Fo$*.obj ../eclipseConfig.c

eclipseWpf.obj: ../eclipseOS.h ../eclipseUnicode.h eclipseWpf.cpp
    $(CC) $(DEBUG) $(CFLAGS) $(CPPFLAGS) $(cvarsdll) /Fo$*.obj eclipseWpf.cpp

eclipseWpfCommon.obj: ../eclipseCommon.h eclipseWpfCommon.cpp
    $(CC) $(DEBUG) $(CFLAGS) $(CPPFLAGS) $(cvarsdll) /Fo$*.obj eclipseWpfCommon.cpp

eclipseJNI.obj: ../eclipseCommon.h ../eclipseOS.h ../eclipseJNI.c
	$(CC) $(DEBUG) $(CFLAGS) $(cvarsdll) /Fo$*.obj ../eclipseJNI.c

eclipseShm.obj: ../eclipseShm.h ../eclipseUnicode.h ../eclipseShm.c
	$(CC) $(DEBUG) $(CFLAGS) $(cvarsdll) /Fo$*.obj ../eclipseShm.c
	
$(EXEC): $(MAIN_OBJS) $(COMMON_OBJS)
    rc.exe -r -fo $(RES) $(PROGRAM_NAME).rc
    link $(LFLAGS) -out:$(PROGRAM_OUTPUT) $(MAIN_OBJS) $(COMMON_OBJS) $(RES) $(LIBS)
    mt.exe -manifest $(PROGRAM_OUTPUT).manifest -outputresource:$(PROGRAM_OUTPUT);2

$(CONSOLE): $(MAIN_OBJS) $(COMMON_OBJS)
	del -f eclipseConfig.obj
    $(CC) $(DEBUG) $(CFLAGS) $(cvarsdll) -D_WIN32_CONSOLE /FoeclipseConfig.obj ../eclipseConfig.c
    link $(CONSOLEFLAGS) -out:$(CONSOLE) $(MAIN_OBJS) $(COMMON_OBJS) $(LIBS)
    mt.exe -manifest $(PROGRAM_OUTPUT).manifest -outputresource:$(CONSOLE);2
    
$(DLL): $(DLL_OBJS) $(COMMON_OBJS)
    link $(DLL_LFLAGS) -out:$(PROGRAM_LIBRARY) $(DLL_OBJS) $(COMMON_OBJS)  $(DLL_LIBS)
    mt.exe -manifest $(PROGRAM_LIBRARY).manifest -outputresource:$(PROGRAM_LIBRARY);2

com.obj: com.c
	$(CC) $(DEBUG) $(CFLAGS) $(cvarsdll) /Fo$*.obj com.c

com: com.obj
 	link  /DLL -out:com_$(LIB_VERSION).dll com.obj ole32.lib
	
install: all
	copy $(EXEC) $(OUTPUT_DIR)
	del -f $(EXEC) $(MAIN_OBJS) $(DLL_OBJS) $(COMMON_OBJS) $(RES)
   
clean:
	del $(EXEC) $(DLL) $(MAIN_OBJS) $(DLL_OBJS) $(COMMON_OBJS) $(RES) *.manifest *.exp *.lib *.dll
