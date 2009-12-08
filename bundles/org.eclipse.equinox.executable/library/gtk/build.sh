#!/bin/sh
#*******************************************************************************
# Copyright (c) 2000, 2005 IBM Corporation and others.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at 
# http://www.eclipse.org/legal/epl-v10.html
# 
# Contributors:
#     IBM Corporation - initial API and implementation
#     Kevin Cornell (Rational Software Corporation)
# Martin Oberhuber (Wind River) - [176805] Support building with gcc and debug
#*******************************************************************************
#
# Usage: sh build.sh [<optional switches>] [clean]
#
#   where the optional switches are:
#       -output <PROGRAM_OUTPUT>  - executable filename ("eclipse")
#       -os     <DEFAULT_OS>      - default Eclipse "-os" value
#       -arch   <DEFAULT_OS_ARCH> - default Eclipse "-arch" value
#       -ws     <DEFAULT_WS>      - default Eclipse "-ws" value
#       -java <JAVA_HOME>  - java install for jni headers
#
#   All other arguments are directly passed to the "make" program.
#   This script can also be invoked with the "clean" argument.
#
#   Examples:
#   sh build.sh clean
#   sh build.sh -java /usr/j2se OPTFLAG=-g PICFLAG=-fpic

cd `dirname $0`

# Define default values for environment variables used in the makefiles.
programOutput="eclipse"
defaultOS=""
defaultOSArch=""
defaultWS="gtk"
defaultJava=DEFAULT_JAVA_JNI
javaHome=""
makefile=""
if [ "$OS" = "" ];  then
    OS=`uname -s`
fi
if [ "$MODEL" = "" ];  then
    MODEL=`uname -m`
fi
if [ "${CC}" = "" ]; then
	CC=cc
	export CC
fi

case $OS in
	"Linux")
		makefile="make_linux.mak"
		defaultOS="linux"
		case $MODEL in
			"x86_64")
				defaultOSArch="x86_64"
				defaultJava=DEFAULT_JAVA_EXEC
				[ -d /usr/java64/1.5 ] && javaHome="/usr/java64/1.5"
				OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
				;;
			i?86)
				defaultOSArch="x86"
				OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
				;;
			"ppc")
				defaultOSArch="ppc"
				defaultJava=DEFAULT_JAVA_EXEC
				OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
				;;
			"ppc64")
				defaultOSArch="ppc64"
				defaultJava=DEFAULT_JAVA_EXEC
				OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
				M_ARCH=-m64
				export M_ARCH
				;;
			"s390")
				defaultOSArch="s390"
				defaultJava=DEFAULT_JAVA_EXEC
				OUTPUT_DIR="../../contributed/$defaultWS/$defaultOS/$defaultOSArch"
				;;
			"s390x")
				defaultOSArch="s390x"
				defaultJava=DEFAULT_JAVA_EXEC
				OUTPUT_DIR="../../contributed/$defaultWS/$defaultOS/$defaultOSArch"
				;;
			"ia64")
				defaultOSArch="ia64"
				defaultJava=DEFAULT_JAVA_EXEC
				OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
				;;
			*)
				echo "*** Unknown MODEL <${MODEL}>"
				;;
		esac
		;;
	"SunOS")
		makefile="make_solaris.mak"
		defaultOS="solaris"
		[ -d /usr/jdk/jdk1.5.0_01 ] && javaHome="/usr/jdk/jdk1.5.0_01"
		OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"
		#PATH=/usr/ccs/bin:/opt/SUNWspro/bin:$PATH
		PATH=/usr/ccs/bin:/export/home/SUNWspro/bin:$PATH
		export PATH
		if [ "$PROC" = "" ];  then
		    PROC=`uname -p`
		fi
		case ${PROC} in
			"i386")
				defaultOSArch="x86"
				CC=cc
				;;
			"sparc")
				defaultOSArch="sparc"
				CC=cc
				;;
			*)
				echo "*** Unknown processor type <${PROC}>"
				;;
		esac
		;;
	*)
	echo "Unknown OS -- build aborted"
	;;
esac
export CC
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
    elif [ "$1" = "-java" ] && [ "$2" != "" ]; then
        javaHome="$2"
        shift
    else
        extraArgs="$extraArgs $1"
    fi
    shift
done

# Set up environment variables needed by the makefiles.
PROGRAM_OUTPUT="$programOutput"
DEFAULT_OS="$defaultOS"
DEFAULT_OS_ARCH="$defaultOSArch"
DEFAULT_WS="$defaultWS"
DEFAULT_JAVA=$defaultJava

if [ -n  $javaHome ]; then
	JAVA_HOME=$javaHome
	export JAVA_HOME
fi

LIBRARY_DIR="../../../org.eclipse.equinox.launcher/fragments/org.eclipse.equinox.launcher.$defaultWS.$defaultOS.$defaultOSArch"
OUTPUT_DIR="../../bin/$defaultWS/$defaultOS/$defaultOSArch"

export OUTPUT_DIR PROGRAM_OUTPUT DEFAULT_OS DEFAULT_OS_ARCH DEFAULT_WS DEFAULT_JAVA LIBRARY_DIR

# If the OS is supported (a makefile exists)
if [ "$makefile" != "" ]; then
	if [ "$extraArgs" != "" ]; then
		make -f $makefile $extraArgs
	else
		echo "Building $OS launcher. Defaults: -os $DEFAULT_OS -arch $DEFAULT_OS_ARCH -ws $DEFAULT_WS"
		make -f $makefile clean
		case x$CC in
		  x*gcc*) make -f $makefile all PICFLAG=-fpic ;;
		  *)      make -f $makefile all ;;
		esac
	fi
else
	echo "Unknown OS $OS -- build aborted"
fi
