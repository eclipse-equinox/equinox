#*******************************************************************************
# Copyright (c) 2000, 2010 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at 
# http://www.eclipse.org/legal/epl-v10.html
# 
# Contributors:
#     IBM Corporation - initial API and implementation
#     Kevin Cornell (Rational Software Corporation)
#     Silenio Quarti (IBM)
#     Sam Robb (TimeSys Corporation)
#*******************************************************************************
include ../make_version.mak
# Makefile for creating the eclipse launcher program.

# This makefile expects the following environment variables set:
#
# PROGRAM_OUTPUT  - the filename of the output executable
# PROGRAM_LIBRARY - the file of the output shared library
# DEFAULT_OS      - the default value of the "-os" switch
# DEFAULT_OS_ARCH - the default value of the "-arch" switch
# DEFAULT_WS      - the default value of the "-ws" switch

#if PROGRAM_OUTPUT is not set, assume eclipse.exe
ifeq ($(PROGRAM_OUTPUT),)
  PROGRAM_OUTPUT=eclipse.exe
endif

# Separate filename from extention
PROGRAM_NAME=$(PROGRAM_OUTPUT:.exe=)

PROGRAM_LIBRARY=eclipse_$(LIB_VERSION).dll

# Allow for cross-compiling under linux
OSTYPE	?= $(shell if uname -s | grep -iq cygwin ; then echo cygwin; else echo linux; fi)

ifeq ($(OSTYPE),cygwin)
CCVER   = i686
CC      = i686-pc-cygwin-gcc-3
RC      = windres
else
CCVER   = i586
CC      = $(shell which i586-pc-cygwin-gcc)
TDIR    = $(dir $(shell test -L $(CC) && readlink $(CC) || echo $(CC)))
RC      = $(TDIR)/i586-pc-cygwin-windres
SYSINC  = -isystem $(TDIR)/../include/mingw
endif

ifeq ($(CC),)
$(error Unable to find $(CCVER)-pc-cygwin-gcc)
endif

# Define the object modules to be compiled and flags.
MAIN_OBJS = eclipseMain.o 
COMMON_OBJS = eclipseConfig.o eclipseCommon.o   eclipseWinCommon.o
DLL_OBJS	= eclipse.o  eclipseWin.o  eclipseUtil.o  eclipseJNI.o eclipseShm.o
	  		  
LIBS	= -lkernel32 -luser32 -lgdi32 -lcomctl32 -lmsvcrt -lversion
LDFLAGS = -mwindows -mno-cygwin
CONSOLEFLAGS = -mconsole -mno-cygwin
DLL_LDFLAGS = -mno-cygwin -shared -Wl,--export-all-symbols -Wl,--kill-at,--image-base=0x72000000
RES	= $(PROGRAM_NAME).res
CONSOLE = $(PROGRAM_NAME)c.exe
EXEC	= $(PROGRAM_OUTPUT)
DLL     = $(PROGRAM_LIBRARY)
DEBUG	= $(CDEBUG)
CFLAGS	= -g -s -Wall \
	  -I. -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/win32 $(SYSINC) \
	  -D_WIN32 \
	  -DWIN32_LEAN_AND_MEAN \
	  -mno-cygwin -D__int64="long long"
WCFLAGS = -DUNICODE -I.. -DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	  -DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	  -DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	  $(DEBUG) $(CFLAGS)

all: $(EXEC) $(DLL) $(CONSOLE)

eclipseMain.o: ../eclipseUnicode.h ../eclipseCommon.h ../eclipseMain.c 
	$(CC) $(DEBUG) $(WCFLAGS) -c -o $@ ../eclipseMain.c
	
eclipseCommon.o: ../eclipseCommon.h ../eclipseUnicode.h ../eclipseCommon.c
	$(CC) $(DEBUG) $(WCFLAGS) -c -o $@ ../eclipseCommon.c
	
eclipseWinCommon.o: ../eclipseCommon.h eclipseWinCommon.c
	$(CC) $(DEBUG) $(WCFLAGS) -c -o $@ eclipseWinCommon.c

eclipse.o: ../eclipseOS.h ../eclipseUnicode.h ../eclipseJNI.h ../eclipseCommon.h ../eclipse.c
	$(CC) $(DEBUG) $(WCFLAGS) -c -o $@ ../eclipse.c

eclipseUtil.o: ../eclipseUtil.h ../eclipseUnicode.h ../eclipseUtil.c
	$(CC) $(DEBUG) $(WCFLAGS) -c -o $@ ../eclipseUtil.c

eclipseConfig.o: ../eclipseConfig.h ../eclipseUnicode.h ../eclipseConfig.c
	$(CC) $(DEBUG) $(WCFLAGS) -c -o $@ ../eclipseConfig.c
	
eclipseWin.o: ../eclipseOS.h ../eclipseUnicode.h eclipseWin.c
	$(CC) $(DEBUG) $(WCFLAGS) -c -o $@ eclipseWin.c

eclipseJNI.o: ../eclipseUnicode.h ../eclipseJNI.c
	$(CC) $(DEBUG) $(WCFLAGS) -c -o $@ ../eclipseJNI.c
	
eclipseShm.o: ../eclipseShm.h ../eclipseUnicode.h ../eclipseShm.c
	$(CC) $(DEBUG) $(WCFLAGS) -c -o $@ ../eclipseShm.c
	
$(RES): $(PROGRAM_NAME).rc
	$(RC) --output-format=coff --include-dir=.. -o $@ $<

$(EXEC): $(MAIN_OBJS) $(COMMON_OBJS) $(RES)
	$(CC) $(LDFLAGS) -o $(EXEC) $(MAIN_OBJS) $(COMMON_OBJS) $(RES) $(LIBS)

#the console version needs a flag set, should look for a better way to do this
$(CONSOLE): $(MAIN_OBJS) $(COMMON_OBJS) 
	rm -f eclipseConfig.o aeclipseConfig.o
	$(CC) $(DEBUG) $(WCFLAGS) -D_WIN32_CONSOLE -c -o eclipseConfig.o ../eclipseConfig.c
	$(CC) $(CONSOLEFLAGS) -o $(CONSOLE) $(MAIN_OBJS) $(COMMON_OBJS) $(LIBS)
	
$(DLL): $(DLL_OBJS) $(COMMON_OBJS)
	$(CC) $(DLL_LDFLAGS) -o $(DLL) $(DLL_OBJS) $(COMMON_OBJS) $(LIBS)
	
install: all
	cp $(EXEC) $(DLL) $(CONSOLE) $(OUTPUT_DIR) 
	rm -f $(EXEC) $(DLL_OBJS) $(COMMON_OBJS) $(MAIN_OBJS) $(RES) $(CONSOLE)

clean:
	$(RM) $(EXEC) $(DLL) $(DLL_OBJS) $(COMMON_OBJS) $(MAIN_OBJS) $(RES) $(CONSOLE)
