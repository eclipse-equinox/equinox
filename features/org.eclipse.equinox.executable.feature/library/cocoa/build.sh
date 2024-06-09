#!/bin/sh
#*******************************************************************************
# Copyright (c) 2000, 2009 IBM Corporation and others.
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
#*******************************************************************************
#
# Usage: sh build.sh [<optional switches>] [clean]
#
#   where the optional switches are:
#       -output <PROGRAM_OUTPUT>  - executable filename ("eclipse")
#       -os     <DEFAULT_OS>      - default Eclipse "-os" value
#       -arch   <DEFAULT_OS_ARCH> - default Eclipse "-arch" value
#       -ws     <DEFAULT_WS>      - default Eclipse "-ws" value
#
#
#    This script can also be invoked with the "clean" argument.

cd `dirname $0`

# Define default values for environment variables used in the makefiles.
programOutput="eclipse"
defaultOS="macosx"
defaultOSArch="x86_64"
defaultWS="cocoa"
makefile="make_cocoa.mak"

# Parse the command line arguments and override the default values.
extraArgs=""
while [ "$1" != "" ]; do
    if [ "$1" = "-os" ] && [ "$2" != "" ]; then
        defaultOS="$2"
        shift
    elif [ "$1" = "-arch" ] && [ "$2" != "" ]; then
        defaultOSArch="$2"
        shift
    elif [ "$1" = "-ws" ] && [ "$2" != "" ]; then
        defaultWS="$2"
        shift
    elif [ "$1" = "-output" ] && [ "$2" != "" ]; then
        programOutput="$2"
        shift
    else
        extraArgs="$extraArgs $1"
    fi
    shift
done

echo "build $defaultOSArch"

# Set up environment variables needed by the makefiles.
PROGRAM_OUTPUT="$programOutput"
DEFAULT_OS="$defaultOS"
DEFAULT_WS="$defaultWS"
DEPLOYMENT_TARGET=11.0
if [ "$BINARIES_DIR" = "" ]; then BINARIES_DIR="../../../../../equinox.binaries"; fi

if [ "$defaultOSArch" == "arm64" ] || [ "$defaultOSArch" == "aarch64" ]
then
  DEFAULT_OS_ARCH="aarch64"
  defaultOSArch="arm64"
else
  DEFAULT_OS_ARCH="$defaultOSArch"
fi

if [ "$EXE_OUTPUT_DIR" = "" ]; then EXE_OUTPUT_DIR="$BINARIES_DIR/org.eclipse.equinox.executable/bin/$defaultWS/$defaultOS/$DEFAULT_OS_ARCH/Eclipse.app/Contents/MacOS"; fi
if [ "$LIB_OUTPUT_DIR" = "" ]; then LIB_OUTPUT_DIR="$BINARIES_DIR/org.eclipse.equinox.launcher.$defaultWS.$defaultOS.$DEFAULT_OS_ARCH"; fi

if [ "$JAVA_HOME" != "" ]; then
  JAVA_HEADERS="-I$JAVA_HOME/include -I$JAVA_HOME/include/darwin"
else
  JAVA_HEADERS="-I$(/usr/libexec/java_home)/include -I$(/usr/libexec/java_home)/include/darwin"
fi

ARCHS="-arch $defaultOSArch"

export PROGRAM_OUTPUT DEFAULT_OS DEFAULT_OS_ARCH DEFAULT_WS ARCHS JAVA_HEADERS EXE_OUTPUT_DIR LIB_OUTPUT_DIR 
export MACOSX_DEPLOYMENT_TARGET=$DEPLOYMENT_TARGET

if [ "$extraArgs" != "" ]; then
	make -f $makefile $extraArgs
else
	echo "Building $defaultOS launcher. Defaults: -os $DEFAULT_OS -arch $DEFAULT_OS_ARCH -ws $DEFAULT_WS"
	make -f $makefile clean
	make -f $makefile all
fi
