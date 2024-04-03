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
# EXE_OUTPUT_DIR  - the location into which the executable is installed (only used in 'install' target)
# LIB_OUTPUT_DIR  - the location into which the launcher library is installed (only used in 'install' target)

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

LIBRARY_FRAGMENT_NAME ?= org.eclipse.equinox.launcher.$(DEFAULT_WS).$(DEFAULT_OS).$(DEFAULT_OS_ARCH)

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
	$(info Install into: EXE_OUTPUT_DIR:$(EXE_OUTPUT_DIR) LIB_OUTPUT_DIR:$(LIB_OUTPUT_DIR))
	mkdir -p $(EXE_OUTPUT_DIR)
	mv $(EXEC) $(EXE_OUTPUT_DIR)
	mkdir -p $(LIB_OUTPUT_DIR)
	rm -f $(LIB_OUTPUT_DIR)/eclipse_*.so
	mv $(DLL) $(LIB_OUTPUT_DIR)
	rm -f $(EXEC) $(OBJS)

clean:
	rm -f $(EXEC) $(DLL) $(MAIN_OBJS) $(COMMON_OBJS) $(DLL_OBJS)

dev_build_install: all
ifneq ($(filter "$(origin DEV_ECLIPSE)", "environment" "command line"),)
	$(info Copying $(EXEC) and $(DLL) into your development eclipse folder:)
	mkdir -p ${DEV_ECLIPSE}/
	cp $(EXEC) ${DEV_ECLIPSE}/
	mkdir -p ${DEV_ECLIPSE}/plugins/$(LIBRARY_FRAGMENT_NAME)/
	cp $(DLL) ${DEV_ECLIPSE}/plugins/$(LIBRARY_FRAGMENT_NAME)/
else
	$(error $(DEV_INSTALL_ERROR_MSG))
endif

test:
	$(eval export DEV_ECLIPSE=../org.eclipse.launcher.tests/target/test-run)
	mvn -f ../org.eclipse.launcher.tests/pom.xml clean verify -Dmaven.test.skip=true
	make -f $(firstword $(MAKEFILE_LIST)) dev_build_install LIBRARY_FRAGMENT_NAME=org.eclipse.equinox.launcher
	mkdir $(DEV_ECLIPSE)/../Eclipse
	mvn -f ../org.eclipse.launcher.tests/pom.xml -DargLine="-DECLIPSE_INI_PATH=../Eclipse/eclipse.ini" test

define DEV_INSTALL_ERROR_MSG =
Note:
   DEV_ECLIPSE environmental variable is not defined.
   You can download an integration build eclipse for testing and set DEV_ECLIPSE to point to it's folder
   as per output of 'pwd'. Note, without trailing forwardslash. Integration build can be downloaded here:
   See: https://download.eclipse.org/eclipse/downloads/
   That way you can automatically build and copy eclipse and eclipse_XXXX.so into the relevant folders for testing.
   E.g: you can put something like the following into your .bashrc
   export DEV_ECLIPSE="/home/YOUR_USER/Downloads/eclipse-SDK-I20YYMMDD-XXXX-linux-gtk-x86_64/eclipse"
endef
