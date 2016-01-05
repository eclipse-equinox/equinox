#!/bin/sh
#*******************************************************************************
# Copyright (c) 2000, 2009 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
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
DEFAULT_OS_ARCH="$defaultOSArch"
DEFAULT_WS="$defaultWS"
EXEC_DIR=../../../../../rt.equinox.binaries/org.eclipse.equinox.executable
PROGRAM_OUTPUT_DIR="$EXEC_DIR/bin/$defaultWS/$defaultOS/$defaultOSArch/Eclipse.app/Contents/MacOS"

# /System/Library/Frameworks/JavaVM.framework/Headers does not exist anymore on Yosemite
if [ -e /System/Library/Frameworks/JavaVM.framework/Headers ]; then
  JAVA_HEADERS="-I/System/Library/Frameworks/JavaVM.framework/Headers"
else
  JAVA_HEADERS="-I$(/usr/libexec/java_home)/include -I$(/usr/libexec/java_home)/include/darwin"
fi

ARCHS="-arch x86_64"
export PROGRAM_OUTPUT DEFAULT_OS DEFAULT_OS_ARCH DEFAULT_WS ARCHS PROGRAM_OUTPUT_DIR JAVA_HEADERS

if [ "$extraArgs" != "" ]; then
	make -f $makefile $extraArgs
else
	echo "Building $defaultOS launcher. Defaults: -os $DEFAULT_OS -arch $DEFAULT_OS_ARCH -ws $DEFAULT_WS"
	make -f $makefile clean
	make -f $makefile all
	make -f $makefile install
fi
