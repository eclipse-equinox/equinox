/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.container;

import org.eclipse.osgi.framework.report.ResolutionReport;
import org.eclipse.osgi.framework.report.ResolutionReportEntry;
import org.osgi.resource.Resource;

/**
 * @since 3.10
 */
public class ResolutionReportBuilder {
	public ResolutionReport build() {
		return new ResolutionReport() {
			@Override
			public boolean hasNext() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public ResolutionReportEntry next() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub

			}

			@Override
			public ResolutionReportEntry getResolutionReport(Resource resource) {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}
}
