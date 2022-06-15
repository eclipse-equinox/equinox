###############################################################################
# Copyright (c) 2014, 2018 IBM Corporation and others.
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
 java.lang.annotation,\
 java.lang.invoke,\
 java.lang.ref,\
 java.lang.reflect,\
 java.math,\
 java.net,\
 java.nio,\
 java.nio.channels,\
 java.nio.channels.spi,\
 java.nio.charset,\
 java.nio.charset.spi,\
 java.nio.file,\
 java.nio.file.attribute,\
 java.nio.file.spi,\
 java.rmi,\
 java.rmi.activation,\
 java.rmi.dgc,\
 java.rmi.registry,\
 java.rmi.server,\
 java.security,\
 java.security.cert,\
 java.security.interfaces,\
 java.security.spec,\
 java.sql,\
 java.text,\
 java.text.spi,\
 java.time,\
 java.time.chrono,\
 java.time.format,\
 java.time.temporal,\
 java.time.zone,\
 java.util,\
 java.util.concurrent,\
 java.util.concurrent.atomic,\
 java.util.concurrent.locks,\
 java.util.function,\
 java.util.jar,\
 java.util.logging,\
 java.util.regex,\
 java.util.spi,\
 java.util.stream,\
 java.util.zip,\
 javax.crypto,\
 javax.crypto.interfaces,\
 javax.crypto.spec,\
 javax.net,\
 javax.net.ssl,\
 javax.rmi.ssl,\
 javax.script,\
 javax.security.auth,\
 javax.security.auth.callback,\
 javax.security.auth.login,\
 javax.security.auth.spi,\
 javax.security.auth.x500,\
 javax.security.cert,\
 javax.sql,\
 javax.transaction,\
 javax.transaction.xa,\
 javax.xml,\
 javax.xml.datatype,\
 javax.xml.namespace,\
 javax.xml.parsers,\
 javax.xml.stream,\
 javax.xml.stream.events,\
 javax.xml.stream.util,\
 javax.xml.transform,\
 javax.xml.transform.dom,\
 javax.xml.transform.sax,\
 javax.xml.transform.stax,\
 javax.xml.transform.stream,\
 javax.xml.validation,\
 javax.xml.xpath,\
 org.w3c.dom,\
 org.w3c.dom.bootstrap,\
 org.w3c.dom.events,\
 org.w3c.dom.ls,\
 org.xml.sax,\
 org.xml.sax.ext,\
 org.xml.sax.helpers
org.osgi.framework.executionenvironment = \
 OSGi/Minimum-1.0,\
 OSGi/Minimum-1.1,\
 OSGi/Minimum-1.2,\
 JavaSE/compact1-1.8,\
 JavaSE/compact2-1.8
org.osgi.framework.system.capabilities = \
 osgi.ee; osgi.ee="OSGi/Minimum"; version:List<Version>="1.0, 1.1, 1.2",\
 osgi.ee; osgi.ee="JavaSE/compact1"; version:List<Version>="1.8",\
 osgi.ee; osgi.ee="JavaSE/compact2"; version:List<Version>="1.8"
osgi.java.profile.name = JavaSE/compact2-1.8
org.eclipse.jdt.core.compiler.compliance=1.8
org.eclipse.jdt.core.compiler.source=1.8
org.eclipse.jdt.core.compiler.codegen.inlineJsrBytecode=enabled
org.eclipse.jdt.core.compiler.codegen.targetPlatform=1.8
org.eclipse.jdt.core.compiler.problem.assertIdentifier=error
org.eclipse.jdt.core.compiler.problem.enumIdentifier=error
