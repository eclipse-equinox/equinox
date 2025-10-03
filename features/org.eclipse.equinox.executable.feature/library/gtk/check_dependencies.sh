#!/bin/bash
###############################################################################
# Copyright (c) 2020, 2025 Kichwa Coders Canada Inc and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
###############################################################################

set -eu
set -o pipefail

SCRIPT=$( basename "${BASH_SOURCE[0]}" )

###
# Check that executable/so ${FILE}
# use glibc symbols no greater than ${ALLOWED_GLIBC_VERSION} and depend on
# no libs other than ${ALLOWED_LIBS}
ARCH=$1; shift
FILE=$1; shift
ALLOWED_LIBS="$@"; shift

case "${ARCH}" in
    x86_64)
        ALLOWED_GLIBC_VERSION="2.7"
        ;;
    aarch64)
        # glibc 2.17 gives us compatibility with RHEL 7 and Debian 8.
        ALLOWED_GLIBC_VERSION="2.17"
        ;;
    *)
        # We don't enforce max version and library sets on other architectures because
        # 1. We build on native hardware for those platforms so we don't have
        #    ability to use docker to adjust dependency versions as easily
        # 2. The other platforms that are newer are generally faster moving
        #    and it is less likely to break users to have harder version
        #    requirements.
        # As we get bigger user base on these architectures we should start enforcing
        # upper bounds for them too.
        echo "We do not enforce glibc version or library dependencies for ${ARCH} architecture so far. All good."
        exit 0
        ;;
esac

# Check for permitted libraries using `readelf -d` looking for shared
# libraries that are listed as needed. e.g. lines like:
#  0x0000000000000001 (NEEDED)             Shared library: [libpthread.so.0]
readelf -d ${FILE} | grep -E '\(NEEDED\)' | while read needed; do
    needed=${needed//*Shared library: [/}
    needed=${needed//]*/}
    if [[ ! " ${ALLOWED_LIBS} " =~ " ${needed} " ]]; then
        echo "ERROR: $FILE has illegal dependency of ${needed}"
        exit 1
    fi
done

# The way the version check is done is that all symbol version info is extracted
# from relocations match @GLIBC_*, the versions are sorted with the max
# allowed version added to the list too. And then we check the last entry in
# the list to make sure it is == to max allowed version.
objdump -R ${FILE} | grep @GLIBC_ | while read version; do
    echo ${version//*@GLIBC_}
done > /tmp/version_check
echo ${ALLOWED_GLIBC_VERSION} >> /tmp/version_check
max_version_in_use=$(cat /tmp/version_check | sort --unique --version-sort | tail -n1)
if [ "$max_version_in_use" != "$ALLOWED_GLIBC_VERSION" ]; then
    echo "ERROR: $FILE has dependency on glibc greater than allowed version of ${ALLOWED_GLIBC_VERSION} for at least the following symbols"
    # This only lists greatest version number symbols
    objdump -R ${FILE} | grep @GLIBC_${max_version_in_use}
    exit 1
fi
