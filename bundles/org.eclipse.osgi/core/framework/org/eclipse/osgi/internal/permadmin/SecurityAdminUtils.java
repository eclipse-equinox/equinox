/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.internal.permadmin;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import org.eclipse.osgi.framework.internal.core.Framework;
import org.osgi.framework.FrameworkEvent;
import org.osgi.service.condpermadmin.ConditionInfo;
import org.osgi.service.permissionadmin.PermissionInfo;

public class SecurityAdminUtils {
	public static final ConditionInfo[] EMPTY_COND_INFO = new ConditionInfo[0];
	public static final PermissionInfo[] EMPTY_PERM_INFO = new PermissionInfo[0];

	static PermissionInfo[] getPermissionInfos(URL resource, Framework framework) {
		if (resource == null)
			return null;
		PermissionInfo[] info = EMPTY_PERM_INFO;
		DataInputStream in = null;
		try {
			in = new DataInputStream(resource.openStream());
			ArrayList permissions = new ArrayList();
			BufferedReader reader;
			try {
				reader = new BufferedReader(new InputStreamReader(in, "UTF8")); //$NON-NLS-1$
			} catch (UnsupportedEncodingException e) {
				reader = new BufferedReader(new InputStreamReader(in));
			}

			while (true) {
				String line = reader.readLine();
				if (line == null) /* EOF */
					break;
				line = line.trim();
				if ((line.length() == 0) || line.startsWith("#") || line.startsWith("//")) /* comments *///$NON-NLS-1$ //$NON-NLS-2$
					continue;

				try {
					permissions.add(new PermissionInfo(line));
				} catch (IllegalArgumentException iae) {
					/* incorrectly encoded permission */
					if (framework != null)
						framework.publishFrameworkEvent(FrameworkEvent.ERROR, framework.getBundle(0), iae);
				}
			}
			int size = permissions.size();
			if (size > 0)
				info = (PermissionInfo[]) permissions.toArray(new PermissionInfo[size]);
		} catch (IOException e) {
			// do nothing
		} finally {
			try {
				if (in != null)
					in.close();
			} catch (IOException ee) {
				// do nothing
			}
		}
		return info;
	}
}
