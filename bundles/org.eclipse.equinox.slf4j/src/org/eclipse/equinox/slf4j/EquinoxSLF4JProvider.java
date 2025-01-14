/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.slf4j;

import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public class EquinoxSLF4JProvider implements SLF4JServiceProvider {

	private IMarkerFactory markerFactory;
	private MDCAdapter mdcAdapter;
	private EquinoxLoggerFactory loggerFactory;

	@Override
	public void initialize() {
		loggerFactory = new EquinoxLoggerFactory();
		markerFactory = new BasicMarkerFactory();
		mdcAdapter = new BasicMDCAdapter();
	}

	@Override
	public ILoggerFactory getLoggerFactory() {
		return loggerFactory;
	}

	@Override
	public IMarkerFactory getMarkerFactory() {
		return markerFactory;
	}

	@Override
	public MDCAdapter getMDCAdapter() {
		return mdcAdapter;
	}

	@Override
	public String getRequestedApiVersion() {
		// It would be better to replace this by a compile time constant on the other
		// hand SLF4j currently only supports 2.0 anyways
		return "2.0.0";
	}
}
