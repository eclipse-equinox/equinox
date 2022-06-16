#**********************************************************************
# Copyright (c) 2000, 2015 IBM Corporation and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Kevin Cornell (Rational Software Corporation)
#     Mikael Barbero
#**********************************************************************
include ../make_version.mak
# Makefile for creating the Cocoa eclipse launcher program.

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
MAIN_OBJS = eclipseMain.o eclipseCocoaMain.o
COMMON_OBJS = eclipseConfig.o eclipseCommon.o eclipseCocoaCommon.o
DLL_OBJS	= eclipse.o eclipseCocoa.o eclipseUtil.o eclipseJNI.o eclipseShm.o

EXEC = $(PROGRAM_OUTPUT)
DLL = $(PROGRAM_LIBRARY)
LIBS = -framework Cocoa

ifeq ($(ARCHS),-arch x86_64)
  LDFLAGS=-pagezero_size 0x1000
endif

CFLAGS = -O \
	-Wall \
	-DCOCOA -xobjective-c \
	$(ARCHS) \
	-DMACOSX \
	-DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	-I.. $(JAVA_HEADERS)

all: $(EXEC) $(DLL)

eclipse.o: ../eclipse.c ../eclipseOS.h ../eclipseCommon.h ../eclipseJNI.h
	$(CC) $(CFLAGS) -c ../eclipse.c -o $@

eclipseCocoaMain.o : eclipseCocoaMain.c
	$(CC) $(CFLAGS) -c eclipseCocoaMain.c -o $@

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

$(EXEC): $(MAIN_OBJS) $(COMMON_OBJS)
	$(CC) $(LDFLAGS) -o $(EXEC) $(ARCHS) $(MAIN_OBJS) $(COMMON_OBJS) $(LIBS)

$(DLL): $(DLL_OBJS) $(COMMON_OBJS)
	$(CC) -bundle -o $(DLL) $(ARCHS) $(DLL_OBJS) $(COMMON_OBJS) $(LIBS)

install: all
	cp -v $(EXEC) $(PROGRAM_OUTPUT_DIR)
	rm -f $(EXEC) $(OBJS)

clean:
	rm -f $(EXEC) $(DLL) $(MAIN_OBJS) $(COMMON_OBJS) $(DLL_OBJS)
