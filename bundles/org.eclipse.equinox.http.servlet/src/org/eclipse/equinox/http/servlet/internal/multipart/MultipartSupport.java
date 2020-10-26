/*******************************************************************************
 * Copyright (c) 2016, 2020 Raymond Augé and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Raymond Augé <raymond.auge@liferay.com> - Bug 497271
 *     Dirk Fauth <dirk.fauth@googlemail.com> - Bug 567831
 *******************************************************************************/
package org.eclipse.equinox.http.servlet.internal.multipart;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

public interface MultipartSupport {

	public List<Part> parseRequest(HttpServletRequest request) throws IOException, ServletException;

}