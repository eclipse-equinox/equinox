###############################################################################
# Copyright (c) 2003, 2024 IBM Corporation and others.
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
 java.applet,\
 java.awt,\
 java.awt.datatransfer,\
 java.awt.event,\
 java.awt.image,\
 java.awt.peer,\
 java.beans,\
 java.io,\
 java.lang,\
 java.lang.reflect,\
 java.math,\
 java.net,\
 java.rmi,\
 java.rmi.dgc,\
 java.rmi.registry,\
 java.rmi.server,\
 java.security,\
 java.security.acl,\
 java.security.interfaces,\
 java.sql,\
 java.text,\
 java.text.resources,\
 java.util,\
 java.util.zip
org.osgi.framework.bootdelegation = \
 sun.*,\
 com.sun.*
org.osgi.framework.executionenvironment = \
 JRE-1.1
org.osgi.framework.system.capabilities = \
 osgi.ee; osgi.ee="JRE"; version:List<Version>="1.0, 1.1"
osgi.java.profile.name = JRE-1.1
org.eclipse.jdt.core.compiler.compliance=1.3
org.eclipse.jdt.core.compiler.source=1.3
org.eclipse.jdt.core.compiler.codegen.targetPlatform=1.1
org.eclipse.jdt.core.compiler.problem.assertIdentifier=ignore
org.eclipse.jdt.core.compiler.problem.enumIdentifier=ignore
