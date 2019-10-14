#*******************************************************************************
# Copyright (c) 2000, 2018 IBM Corporation and others.
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
#     Kevin Cornell (Rational Software Corporation)
#     Tom Tromey (Red Hat, Inc.)
#     Leo Ufimtsev (Red Hat, Inc) 2018
#*******************************************************************************

include ../make_version.mak
# Makefile for creating the GTK eclipse launcher program.

# Can work on it's own or reached from build.sh.
# Common invocations:
# make -f make_linux.mak clean all
# make -f make_linux.mak clean all install # Install as part of eclipse build.
# make -f make_linux.mak clean all dev_build_install   # For development/testing of launcher, install into your development eclipse, see target below.

# This makefile expects the utility "pkg-config" to be in the PATH.
# This makefile expects the following environment variables be set. If they are not set, it will figure out reasonable defaults targeting linux build.
# PROGRAM_OUTPUT  - the filename of the output executable
# DEFAULT_OS      - the default value of the "-os" switch
# DEFAULT_OS_ARCH - the default value of the "-arch" switch
# DEFAULT_WS      - the default value of the "-ws" switch
# JAVA_HOME      - JAVA_HOME for jni headers
# M_ARCH & M_CFLAGS - architecture/cflags for gcc command (if relevant)

# Environment Variables:
DEFAULT_OS ?= $(shell uname -s | tr "[:upper:]" "[:lower:]")
DEFAULT_WS ?= gtk
DEFAULT_OS_ARCH ?= $(shell uname -m)
JAVA_HOME ?= $(shell readlink -f /usr/bin/java | sed "s:jre/::" | sed "s:bin/java::")
PROGRAM_OUTPUT ?= eclipse
PROGRAM_LIBRARY = $(PROGRAM_OUTPUT)_$(LIB_VERSION).so

EXEC_DIR ?= ../../../../../rt.equinox.binaries/org.eclipse.equinox.executable
OUTPUT_DIR ?= $(EXEC_DIR)/bin/$(DEFAULT_WS)/$(DEFAULT_OS)/$(DEFAULT_OS_ARCH)
LIBRARY_DIR ?= $(EXEC_DIR)/../org.eclipse.equinox.launcher.$(DEFAULT_WS).$(DEFAULT_OS).$(DEFAULT_OS_ARCH)

# 64 bit specific flag:
ifeq ($(M_CFLAGS),)
ifeq ($(DEFAULT_OS_ARCH),x86_64)
# Bug 517013: Avoid using memcpy() to remain compatible with older glibc (not use these flags on 32bit.
M_CFLAGS ?= -fno-builtin-memcpy -fno-builtin-memmove
endif
endif

# Determine launch mode.
ifeq ($(DEFAULT_OS_ARCH),x86_64)
DEFAULT_JAVA ?= DEFAULT_JAVA_EXEC
endif

CC ?= gcc

# Useful to figure out if there is any difference between running build.sh and make_linux directly.
INFO_PROG=CC:$(CC)  PROGRAM_OUTPUT:$(PROGRAM_OUTPUT)  PROGRAM_LIBRARY:$(PROGRAM_LIBRARY) #
INFO_ARCH=DEFAULT_OS:$(DEFAULT_OS)  DEFAULT_WS:$(DEFAULT_WS)  DEFAULT_OS_ARCH:$(DEFAULT_OS_ARCH)  M_ARCH:$(M_ARCH)  M_CFLAGS:$(M_CFLAGS) #
INFO_JAVA=JAVA_HOME:$(JAVA_HOME)  DEFAULT_JAVA:$(DEFAULT_JAVA) #
INFO_OUT_LIB_DIRS=OUTPUT_DIR:$(OUTPUT_DIR)  LIBRARY_DIR:$(LIBRARY_DIR) #
$(info Input info: $(INFO_PROG) $(INFO_ARCH) $(INFO_JAVA) $(INFO_OUT_LIB_DIRS))

# Define the object modules to be compiled and flags.
MAIN_OBJS = eclipseMain.o
COMMON_OBJS = eclipseConfig.o eclipseCommon.o eclipseGtkCommon.o eclipseGtkInit.o
DLL_OBJS	= eclipse.o eclipseGtk.o eclipseUtil.o eclipseJNI.o eclipseShm.o eclipseNix.o

EXEC = $(PROGRAM_OUTPUT)
# DLL == 'eclipse_XXXX.so'
DLL = $(PROGRAM_LIBRARY)

LIBS = -lpthread -ldl
GTK_LIBS = \
 -DGTK3_LIB="\"libgtk-3.so.0\"" -DGDK3_LIB="\"libgdk-3.so.0\"" \
 -DPIXBUF_LIB="\"libgdk_pixbuf-2.0.so.0\"" -DGOBJ_LIB="\"libgobject-2.0.so.0\"" \
 -DGIO_LIB="\"libgio-2.0.so.0\"" -DGLIB_LIB="\"libglib-2.0.so.0\""
LFLAGS = ${M_ARCH} -shared -fpic -Wl,--export-dynamic 
CFLAGS = ${M_CFLAGS} ${M_ARCH} -g -s -Wall\
	-fpic \
	-DLINUX \
	-DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	-D$(DEFAULT_JAVA) \
	$(GTK_LIBS) \
	-I. \
	-I.. \
	-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux \
	`pkg-config --cflags gtk+-3.0`

all: $(EXEC) $(DLL)

eclipse.o: ../eclipse.c ../eclipseOS.h ../eclipseCommon.h ../eclipseJNI.h
	$(CC) $(CFLAGS) -c ../eclipse.c -o eclipse.o

eclipseMain.o: ../eclipseUnicode.h ../eclipseCommon.h ../eclipseMain.c
	$(info Starting Build:)
	$(CC) $(CFLAGS) -c ../eclipseMain.c -o eclipseMain.o

eclipseCommon.o: ../eclipseCommon.h ../eclipseUnicode.h ../eclipseCommon.c
	$(CC) $(CFLAGS) -c ../eclipseCommon.c

eclipseGtkCommon.o: ../eclipseCommon.h ../eclipseOS.h eclipseGtk.h eclipseGtkCommon.c
	$(CC) $(CFLAGS) -c eclipseGtkCommon.c -o eclipseGtkCommon.o

eclipseGtkInit.o: ../eclipseCommon.h eclipseGtk.h eclipseGtkInit.c
	$(CC) $(CFLAGS) -c eclipseGtkInit.c -o eclipseGtkInit.o

eclipseUtil.o: ../eclipseUtil.c ../eclipseUtil.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseUtil.c -o eclipseUtil.o

eclipseJNI.o: ../eclipseJNI.c ../eclipseCommon.h ../eclipseOS.h ../eclipseJNI.h
	$(CC) $(CFLAGS) -c ../eclipseJNI.c -o eclipseJNI.o

eclipseConfig.o: ../eclipseConfig.c ../eclipseConfig.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c ../eclipseConfig.c -o eclipseConfig.o

eclipseShm.o: ../eclipseShm.h ../eclipseUnicode.h ../eclipseShm.c
	$(CC) $(CFLAGS) -c ../eclipseShm.c -o eclipseShm.o

eclipseNix.o: ../eclipseNix.c
	$(CC) $(CFLAGS) -c ../eclipseNix.c -o eclipseNix.o

$(EXEC): $(MAIN_OBJS) $(COMMON_OBJS)
	$(info Linking and generating: $(EXEC))
	$(CC) ${M_ARCH} -o $(EXEC) $(MAIN_OBJS) $(COMMON_OBJS) $(LIBS)

$(DLL): $(DLL_OBJS) $(COMMON_OBJS)
	$(info Linking and generating: $(DLL))
	$(CC) $(LFLAGS) -o $(DLL) $(DLL_OBJS) $(COMMON_OBJS) $(LIBS)

install: all
	cp $(EXEC) $(OUTPUT_DIR)
	cp $(DLL) $(LIBRARY_DIR)
	rm -f $(EXEC) $(DLL) $(MAIN_OBJS) $(COMMON_OBJS) $(DLL_OBJS)

clean:
	$(info Clean up:)
	rm -f $(EXEC) $(DLL) $(MAIN_OBJS) $(COMMON_OBJS) $(DLL_OBJS)

# Convienience method to install produced output into a developer's eclipse for testing/development.
dev_build_install: all
ifeq "$(origin DEV_ECLIPSE)" "environment"
	$(info Copying $(EXEC) and $(DLL) into your development eclipse folder:)
	cp $(EXEC) ${DEV_ECLIPSE}/
	cp $(DLL) ${DEV_ECLIPSE}/plugins/org.eclipse.equinox.launcher.gtk.linux.x86_64/
else
	$(error $(DEV_INSTALL_ERROR_MSG))
endif

define DEV_INSTALL_ERROR_MSG =
Note:
   DEV_ECLIPSE environmental variable is not defined.
   You can download an integration build eclipse for testing and set DEV_ECLIPSE to point to it's folder
   as per output of 'pwd'. Note, without trailing forwardslash. Integration build can be downloaded here:
   See: http://download.eclipse.org/eclipse/downloads/
   That way you can automatically build and copy eclipse and eclipse_XXXX.so into the relevant folders for testing. 
   E.g: you can put something like the following into your .bashrc
   export DEV_ECLIPSE="/home/YOUR_USER/Downloads/eclipse-SDK-I20YYMMDD-XXXX-linux-gtk-x86_64/eclipse"
endef
