#!/bin/bash -xe

#*******************************************************************************
# Copyright (c) 2025, 2025 IBM Hannes Wellmann and others.
#
# This program and the accompanying materials
# are made available under the terms of the Eclipse Public License 2.0
# which accompanies this distribution, and is available at
# https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#
# Contributors:
#     Hannes Wellmann - initial API and implementation
#*******************************************************************************

# This script is called by the pipeline for preparing the next development cycle (this file's name is crucial!)
# and applies the changes required individually for Equinox.

sed -i features/org.eclipse.equinox.executable.feature/bin/cocoa/macosx/*/Eclipse.app/Contents/Info.plist \
	--expression "s|${PREVIOUS_RELEASE_VERSION}|${NEXT_RELEASE_VERSION}|g" \
	--expression "s|2002, 20[0-9]\+[0-9]\+.|2002, ${NEXT_RELEASE_YEAR}.|g"


git commit --all --message "Update version number in Mac's Eclipse.app for ${NEXT_RELEASE_VERSION}"
