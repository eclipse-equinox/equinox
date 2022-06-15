###############################################################################
# Copyright (c) 2005, 2018 IBM Corporation and others.
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
###############################################################################
org.osgi.framework.system.packages = \
 java.io,\
 java.lang,\
 java.lang.ref,\
 java.lang.reflect,\
 java.math,\
 java.net,\
 java.security,\
 java.security.acl,\
 java.security.cert,\
 java.security.interfaces,\
 java.security.spec,\
 java.text,\
 java.util,\
 java.util.jar,\
 java.util.zip,\
 javax.microedition.io,\
 javax.microedition.pki,\
 javax.security.auth.x500
org.osgi.framework.bootdelegation = \
 javax.microedition.io,\
 javax.microedition.pki,\
 javax.security.auth.x500
org.osgi.framework.executionenvironment = \
 OSGi/Minimum-1.0,\
 OSGi/Minimum-1.1,\
 OSGi/Minimum-1.2,\
 CDC-1.0/Foundation-1.0,\
 CDC-1.1/Foundation-1.1
org.osgi.framework.system.capabilities = \
 osgi.ee; osgi.ee="OSGi/Minimum"; version:List<Version>="1.0, 1.1, 1.2",\
 osgi.ee; osgi.ee="CDC/Foundation"; version:List<Version>="1.0, 1.1"
osgi.java.profile.name = CDC-1.1/Foundation-1.1
org.eclipse.jdt.core.compiler.compliance=1.4
org.eclipse.jdt.core.compiler.source=1.3
org.eclipse.jdt.core.compiler.codegen.targetPlatform=1.2
org.eclipse.jdt.core.compiler.problem.assertIdentifier=warning
org.eclipse.jdt.core.compiler.problem.enumIdentifier=warning
