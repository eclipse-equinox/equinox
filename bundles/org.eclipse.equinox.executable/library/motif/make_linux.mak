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
#*******************************************************************************
include ../make_version.mak
# Makefile for creating the Linux/Motif eclipse launcher program.

# This makefile expects the following environment variables set:
#
# PROGRAM_OUTPUT  - the filename of the output executable
# PROGRAM_LIBRARY - the filename of the output library
# DEFAULT_OS      - the default value of the "-os" switch
# DEFAULT_OS_ARCH - the default value of the "-arch" switch
# DEFAULT_WS      - the default value of the "-ws" switch
# X11_HOME	     - the full path to X11 header files
# MOTIF_HOME	 - the full path to Motif header files
# JAVA_HOME      - JAVA_HOME for the java jni header files

ifeq ($(PROGRAM_OUTPUT),)
  PROGRAM_OUTPUT=eclipse
endif

PROGRAM_LIBRARY=eclipse_$(LIB_VERSION).so

ifeq ($(DEFAULT_JAVA),)
  DEFAULT_JAVA=DEFAULT_JAVA_JNI
endif

# Define the object modules to be compiled and flags.
CC?=gcc
MAIN_OBJS = eclipseMain.o
COMMON_OBJS = eclipseConfig.o eclipseCommon.o eclipseMotifCommon.o eclipseMotifInit.o
DLL_OBJS	= eclipse.o eclipseMotif.o eclipseUtil.o eclipseJNI.o eclipseMozilla.o eclipseShm.o eclipseNix.o \
			  NgCommon.o NgImage.o NgImageData.o NgWinBMPFileFormat.o 

EXEC = $(PROGRAM_OUTPUT)
DLL = $(PROGRAM_LIBRARY)
LIBS = -Xlinker -rpath -Xlinker . -L$(MOTIF_HOME)/lib -L$(X11_HOME)/lib  -lpthread -ldl
MOTIF_LIBS = -DXM_LIB="\"libXm.so.2\"" -DXT_LIB="\"libXt.so.6\"" -DX11_LIB="\"libX11.so.6\"" -DXIN_LIB="\"libXinerama.so.1\""
LFLAGS = -shared -fpic -Wl,--export-dynamic 
CFLAGS = -g -s -Wall \
	-DLINUX \
	-DMOTIF \
	-DMOZILLA_FIX \
	-DDEFAULT_OS="\"$(DEFAULT_OS)\"" \
	-DDEFAULT_OS_ARCH="\"$(DEFAULT_OS_ARCH)\"" \
	-DDEFAULT_WS="\"$(DEFAULT_WS)\"" \
	$(MOTIF_LIBS) \
	-D$(DEFAULT_JAVA)\
	-fPIC \
	-I./ \
	-I../ \
	-I$(MOTIF_HOME)/include \
	-I$(X11_HOME)/include \
	-I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux

all: $(EXEC) $(DLL)

.c.o:
	$(CC) $(CFLAGS) -c $< -o $@

eclipseMain.o: ../eclipseMain.c ../eclipseUnicode.h ../eclipseCommon.h  
	$(CC) $(CFLAGS) -c $< -o $@
	
eclipse.o: ../eclipse.c ../eclipseOS.h ../eclipseCommon.h ../eclipseJNI.h
	$(CC) $(CFLAGS) -c $< -o $@

eclipseCommon.o: ../eclipseCommon.c ../eclipseCommon.h ../eclipseUnicode.h 
	$(CC) $(CFLAGS) -c $< -o $@
	
eclipseUtil.o: ../eclipseUtil.c ../eclipseUtil.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c $< -o $@

eclipseJNI.o: ../eclipseJNI.c ../eclipseCommon.h ../eclipseOS.h ../eclipseJNI.h
	$(CC) $(CFLAGS) -c $< -o $@
	
eclipseConfig.o: ../eclipseConfig.c ../eclipseConfig.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c $< -o $@
	
eclipseMozilla.o: ../eclipseMozilla.c ../eclipseMozilla.h ../eclipseOS.h
	$(CC) $(CFLAGS) -c $< -o $@
	
eclipseShm.o: ../eclipseShm.c ../eclipseShm.h ../eclipseUnicode.h 
	$(CC) $(CFLAGS) -c $< -o $@
	
eclipseNix.o: ../eclipseNix.c
	$(CC) $(CFLAGS) -c $< -o $@
	
$(EXEC): $(MAIN_OBJS) $(COMMON_OBJS)
	$(CC) -o $(EXEC) $(MAIN_OBJS) $(COMMON_OBJS) $(LIBS)

$(DLL): $(DLL_OBJS) $(COMMON_OBJS)
	$(CC) $(LFLAGS) -o $(DLL) $(DLL_OBJS) $(COMMON_OBJS) $(LIBS)
	
install: all
	cp $(EXEC) $(OUTPUT_DIR)
	cp  $(DLL) $(LIBRARY_DIR)
	rm -f $(EXEC) $(MAIN_OBJS) $(COMMON_OBJS) $(DLL_OBJS)

clean:
	rm -f $(EXEC) $(MAIN_OBJS) $(COMMON_OBJS) $(DLL_OBJS)
