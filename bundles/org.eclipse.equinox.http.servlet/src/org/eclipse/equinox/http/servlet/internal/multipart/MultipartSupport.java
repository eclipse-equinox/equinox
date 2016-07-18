/*******************************************************************************
 * Copyright (c) 2016 Raymond Augé and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 497271
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal.multipart;

import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

public interface MultipartSupport {

	public Map<String, Part> parseRequest(HttpServletRequest request) throws IOException, ServletException;

}