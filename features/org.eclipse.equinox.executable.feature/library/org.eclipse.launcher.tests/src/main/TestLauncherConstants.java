/*******************************************************************************
 * Copyright (c) 2023 Eclipse Foundation, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Umair Sair - initial API and implementation
 *******************************************************************************/
package main;

public interface TestLauncherConstants {
	public static final String ARGS_PARAMETER = "-args"; //$NON-NLS-1$
	public static final String EXITDATA_PARAMETER = "-exitdata"; //$NON-NLS-1$
	public static final String EXITCODE_PARAMETER = "-exitcode"; //$NON-NLS-1$
	public static final String MULTILINE_ARG_VALUE_TERMINATOR = "---"; //$NON-NLS-1$
	
	public static final String PORT_ENV_KEY = "eclipse_test_port"; //$NON-NLS-1$
}
