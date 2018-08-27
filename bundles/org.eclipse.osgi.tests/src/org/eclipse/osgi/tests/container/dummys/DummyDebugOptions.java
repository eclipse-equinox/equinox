/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.osgi.tests.container.dummys;

import java.io.File;
import java.util.Map;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugTrace;

public class DummyDebugOptions implements DebugOptions {
	private final Map<String, String> options;

	public DummyDebugOptions(Map<String, String> options) {
		this.options = options;
	}

	@Override
	public boolean getBooleanOption(String option, boolean defaultValue) {
		String value = options.get(option);
		return value == null ? defaultValue : Boolean.parseBoolean(value);
	}

	@Override
	public String getOption(String option) {
		return options.get(option);
	}

	@Override
	public String getOption(String option, String defaultValue) {
		String value = options.get(option);
		return value == null ? defaultValue : value;
	}

	@Override
	public int getIntegerOption(String option, int defaultValue) {
		String value = options.get(option);
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	@Override
	public Map<String, String> getOptions() {
		return options;
	}

	@Override
	public void setOption(String option, String value) {
		options.put(option, value);
	}

	@Override
	public void setOptions(Map<String, String> options) {
		this.options.clear();
		this.options.putAll(options);
	}

	@Override
	public void removeOption(String option) {
		this.options.remove(option);
	}

	@Override
	public boolean isDebugEnabled() {
		return true;
	}

	@Override
	public void setDebugEnabled(boolean value) {
		// nothing
	}

	@Override
	public void setFile(File newFile) {
		// nothing
	}

	@Override
	public File getFile() {
		// nothing
		return null;
	}

	@Override
	public DebugTrace newDebugTrace(String bundleSymbolicName) {
		// nothing
		return null;
	}

	@Override
	public DebugTrace newDebugTrace(String bundleSymbolicName, Class<?> traceEntryClass) {
		// nothing
		return null;
	}

}
