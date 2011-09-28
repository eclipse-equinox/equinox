#**********************************************************************
# Copyright (c) 2000, 2008 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at 
# http://www.eclipse.org/legal/epl-v10.html
#   
# Contributors: 
#     Kevin Cornell (Rational Software Corporation)
#********************************************************************** 
include ../make_version.mak
# Makefile for creating the Carbon eclipse launcher program.

# This makefile expects the following environment variables set:
#
# PROGRAM_OUTPUT  - the filename of the output executable
# DEFAULT_OS      - the default value of the "-os" switch
# DEFAULT_OS_ARCH - the default value of the "-arch" switch
# DEFAULT_WS      - the default value of the "-ws" switch

#default value for PROGRAM_OUTPUT
ifeq ($(PROGRAM_OUTPUT),)
  PROGRAM_OUTPUT=eclipse
endif
PROGRAM_LIBRARY=eclipse_$(LIB_VERSION).so

# Define the object modules to be compiled and flags.
MAIN_OBJS = eclipseMain.o eclipseCarbonMain.o
COMMON_OBJS = eclipseConfig.o eclipseCommon.o eclipseCarbonCommon.o
DLL_OBJS	= eclipse.o eclipseCarbon.o eclipseUtil.o eclipseJNI.o eclipseShm.o NgImageData.o NgWinBMPFileFormat.o NgCommon.o

EXEC = $(PROGRAM_OUTPUT)
DLL = $(PROGRAM_LIBRARY)
LIBS = -framework Carbon
ARCHS = -arch i386 -arch ppc
CFLAGS = -O -s \
	-mmacosx-version-min=10.3 \
	-Wall \
	$(ARCHS) \
	-DMACOSX \
	-DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	-I.. -I../motif -I/System/Library/Frameworks/JavaVM.framework/Headers

all: $(EXEC) $(DLL)

eclipse.o: ../eclipse.c ../eclipseOS.h ../eclipseCommon.h ../eclipseJNI.h
	$(CC) $(CFLAGS) -c ../eclipse.c -o $@

eclipseCarbonMain.o : eclipseCarbonMain.c
	$(CC) $(CFLAGS) -c eclipseCarbonMain.c -o $@

eclipseMain.o: ../eclipseUnicode.h ../eclipseCommon.h eclipseMain.c ../eclipseMain.c 
	$(CC) $(CFLAGS) -c eclipseMain.c -o $@
	
eclipseJNI.o: ../eclipseJNI.c ../eclipseCommon.h ../eclipseOS.h ../eclipseJNI.h
	$(CC) $(CFLAGS) -c ../eclipseJNI.c -o $@

eclipseUtil.o: ../eclipseUtil.c ../eclipseUtil.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseUtil.c -o $@
	
eclipseConfig.o: ../eclipseConfig.c ../eclipseConfig.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseConfig.c -o $@

eclipseCommon.o: ../eclipseCommon.h ../eclipseUnicode.h ../eclipseCommon.c
	$(CC) $(CFLAGS) -c ../eclipseCommon.c -o $@

eclipseShm.o: ../eclipseShm.h ../eclipseUnicode.h ../eclipseShm.c
	$(CC) $(CFLAGS) -c ../eclipseShm.c -o $@

NgCommon.o: ../motif/NgCommon.c
	$(CC) $(CFLAGS) -c ../motif/NgCommon.c -o $@

NgWinBMPFileFormat.o: ../motif/NgWinBMPFileFormat.c
	$(CC) $(CFLAGS) -c ../motif/NgWinBMPFileFormat.c -o $@

NgImageData.o: ../motif/NgImageData.c
	$(CC) $(CFLAGS) -c ../motif/NgImageData.c -o $@

$(EXEC): $(MAIN_OBJS) $(COMMON_OBJS)
	$(CC) -o $(EXEC) $(ARCHS) $(MAIN_OBJS) $(COMMON_OBJS) $(LIBS)

$(DLL): $(DLL_OBJS) $(COMMON_OBJS)
	$(CC) -bundle -o $(DLL) $(ARCHS) $(DLL_OBJS) $(COMMON_OBJS) $(LIBS)

install: all
	cp $(EXEC) $(PPC_OUTPUT_DIR)
	cp $(EXEC) $(X86_OUTPUT_DIR)
	rm -f $(EXEC) $(OBJS)

clean:
	rm -f $(EXEC) $(DLL) $(MAIN_OBJS) $(COMMON_OBJS) $(DLL_OBJS)
