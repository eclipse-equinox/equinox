#!/bin/sh
#*******************************************************************************
# Copyright (c) 2000, 2014 IBM Corporation and others.
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
EXEC_DIR=../../../../../rt.equinox.binaries/org.eclipse.equinox.executable
defaultJava=DEFAULT_JAVA_JNI
defaultJavaHome=""
javaHome=""
makefile=""
if [ "${CC}" = "" ]; then
	CC=cc
	export CC
fi

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
if [ "$defaultOS" = "" ];  then
    defaultOS=`uname -s`
fi
if [ "$defaultOSArch" = "" ];  then
	defaultOSArch=`uname -m`
fi


case $defaultOS in
	"Linux" | "linux")
		makefile="make_linux.mak"
		defaultOS="linux"
		case $defaultOSArch in
			"x86_64")
				defaultOSArch="x86_64"
				defaultJava=DEFAULT_JAVA_EXEC
				[ -d /bluebird/teamswt/swt-builddir/build/JRE/x64/jdk1.6.0_14 ] && defaultJavaHome="/bluebird/teamswt/swt-builddir/build/JRE/x64/jdk1.6.0_14"
				OUTPUT_DIR="$EXEC_DIR/bin/$defaultWS/$defaultOS/$defaultOSArch"
				;;
			i?86 | "x86")
				defaultOSArch="x86"
				[ -d /bluebird/teamswt/swt-builddir/build/JRE/x32/jdk1.6.0_14 ] && defaultJavaHome="/bluebird/teamswt/swt-builddir/build/JRE/x32/jdk1.6.0_14"
				OUTPUT_DIR="$EXEC_DIR/bin/$defaultWS/$defaultOS/$defaultOSArch"
				;;
			"ppc")
				defaultOSArch="ppc"
				defaultJava=DEFAULT_JAVA_EXEC
				[ -d /bluebird/teamswt/swt-builddir/JDKs/PPC/ibm-java2-ppc-50 ] && defaultJavaHome="/bluebird/teamswt/swt-builddir/JDKs/PPC/ibm-java2-ppc-50"
				OUTPUT_DIR="$EXEC_DIR/bin/$defaultWS/$defaultOS/$defaultOSArch"
				;;
			"ppc64")
				defaultOSArch="ppc64"
				defaultJava=DEFAULT_JAVA_EXEC
				[ -d /bluebird/teamswt/swt-builddir/JDKs/PPC64/ibm-java2-ppc64-50 ] && defaultJavaHome="/bluebird/teamswt/swt-builddir/JDKs/PPC64/ibm-java2-ppc64-50"
				OUTPUT_DIR="$EXEC_DIR/bin/$defaultWS/$defaultOS/$defaultOSArch"
				;;
			"ppc64le")
				defaultOSArch="ppc64le"
				defaultJava=DEFAULT_JAVA_EXEC
				[ -d /bluebird/teamswt/swt-builddir/JDKs/PPC64LE/ibm-java2-ppc64le-50 ] && defaultJavaHome="/bluebird/teamswt/swt-builddir/JDKs/PPC64LE/ibm-java2-ppc64le-50"
				OUTPUT_DIR="$EXEC_DIR/bin/$defaultWS/$defaultOS/$defaultOSArch"
				;;
			"s390")
				defaultOSArch="s390"
				defaultJava=DEFAULT_JAVA_EXEC
				OUTPUT_DIR="$EXEC_DIR/contributed/$defaultWS/$defaultOS/$defaultOSArch"
				;;
			"s390x")
				defaultOSArch="s390x"
				defaultJava=DEFAULT_JAVA_EXEC
				OUTPUT_DIR="$EXEC_DIR/contributed/$defaultWS/$defaultOS/$defaultOSArch"
				;;
			"ia64")
				defaultOSArch="ia64"
				defaultJava=DEFAULT_JAVA_EXEC
				OUTPUT_DIR="$EXEC_DIR/bin/$defaultWS/$defaultOS/$defaultOSArch"
				;;
			*)
				echo "*** Unknown MODEL <${MODEL}>"
				;;
		esac
		;;
	"AIX" | "aix")
		makefile="make_aix.mak"
		defaultOS="aix"
		if [ -z "$defaultOSArch" ]; then
			defaultOSArch="ppc64"
		fi
		[ -d /bluebird/teamswt/swt-builddir/JDKs/AIX/PPC64/j564/sdk ] && defaultJavaHome="/bluebird/teamswt/swt-builddir/JDKs/AIX/PPC64/j564/sdk"
	;;
	"HP-UX" | "hpux")
		makefile="make_hpux.mak"
		defaultOS="hpux"
		case $defaultOSArch in
			"ia64_32")
				PATH=$PATH:/opt/hp-gcc/bin:/opt/gtk2.6/bin
				PKG_CONFIG_PATH="/opt/gtk2.6/lib/pkgconfig"
				;;
			"ia64")
				PATH=$PATH:/opt/hp-gcc/bin:/opt/gtk_64bit/bin
				PKG_CONFIG_PATH="/opt/gtk_64bit/lib/hpux64/pkgconfig"
				;;
		esac
		export PATH PKG_CONFIG_PATH
		[ -d /opt/java1.5 ] && defaultJavaHome="/opt/java1.5"
	;;
	"SunOS" | "solaris")
		makefile="make_solaris.mak"
		defaultOS="solaris"
		OUTPUT_DIR="$EXEC_DIR/bin/$defaultWS/$defaultOS/$defaultOSArch"
		#PATH=/usr/ccs/bin:/opt/SUNWspro/bin:$PATH
		PATH=/usr/ccs/bin:/export/home/SUNWspro/bin:$PATH
		export PATH
		if [ "$PROC" = "" ];  then
		    PROC=`uname -p`
		fi
		case ${PROC} in
			"i386" | "x86")
				defaultOSArch="x86"
				[ -d /bluebird/teamswt/swt-builddir/build/JRE/Solaris_x86/jdk1.6.0_14 ] && defaultJavaHome="/bluebird/teamswt/swt-builddir/build/JRE/Solaris_x86/jdk1.6.0_14"
				CC=cc
				;;
			"sparc")
				defaultOSArch="sparc"
				[ -d /bluebird/teamswt/swt-builddir/build/JRE/SPARC/jdk1.6.0_14 ] && defaultJavaHome="/bluebird/teamswt/swt-builddir/build/JRE/SPARC/jdk1.6.0_14"
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


# Set up environment variables needed by the makefiles.
PROGRAM_OUTPUT="$programOutput"
DEFAULT_OS="$defaultOS"
DEFAULT_OS_ARCH="$defaultOSArch"
DEFAULT_WS="$defaultWS"
DEFAULT_JAVA=$defaultJava

origJavaHome=$JAVA_HOME
if [ -n  "$javaHome" ]; then
	JAVA_HOME=$javaHome
	export JAVA_HOME
elif [ -z "$JAVA_HOME" -a -n  "$defaultJavaHome" ]; then
	JAVA_HOME="$defaultJavaHome"
	export JAVA_HOME
fi

if [ "$defaultOSArch" = "ppc64" -o $defaultOSArch = "ppc64le" ];  then
	if [ "$defaultOS" = "aix" ];  then
		M_ARCH=-maix64
	else
		M_ARCH=-m64
	fi
	export M_ARCH
elif [ "$defaultOSArch" = "s390" ];  then
	M_ARCH=-m31
	export M_ARCH
elif [ "$defaultOSArch" = "ia64" ];  then
	M_ARCH=-mlp64
	export M_ARCH
fi

LIBRARY_DIR="$EXEC_DIR/../org.eclipse.equinox.launcher.$defaultWS.$defaultOS.$defaultOSArch"
OUTPUT_DIR="$EXEC_DIR/bin/$defaultWS/$defaultOS/$defaultOSArch"

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

#restore original JAVA_HOME
JAVA_HOME="$origJavaHome"
export JAVA_HOME
